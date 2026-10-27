package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.dto.SectionRemarkDTO;
import com.rpis.backend.model.*;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ProposalAnnouncementService proposalAnnouncementService;
    private final ProposalVersionService proposalVersionService;
    private final ProposalSectionRemarkService proposalSectionRemarkService;
    private final CurrentUser currentUser;
    private final CooperatingAgencyService cooperatingAgencyService;
    private final PriorityAgendaService priorityAgendaService;

    /**
     * Project durations (in months) a proponent may select for an internally funded
     * MSUN project. The RPIS only handles internally funded proposals, so every new
     * proposal and draft must use one of these values.
     */
    public static final Set<String> SUPPORTED_DURATIONS = Set.of("12", "24", "36");

    // ========================
    // TECHNOLOGY MAP (section 13 of the Create Proposal form)
    // ========================
    // The proponent uploads the technology map as an image. The same limits the form
    // enforces in the browser are enforced again here, so a request that bypasses the
    // form cannot store an oversized file or a non-image that merely has a .png /
    // .jpg name.

    /** Largest accepted Technology Map image: 10 MB (10 * 1024 * 1024 bytes). */
    public static final long TECHNOLOGY_MAP_MAX_BYTES = 10L * 1024 * 1024;

    /** Raster formats accepted for the Technology Map (ImageIO format names). */
    private static final Set<String> TECHNOLOGY_MAP_FORMATS = Set.of("png", "jpeg");

    /** Messages shown by the form as well, so both layers report the same reason. */
    public static final String TECHNOLOGY_MAP_SIZE_MESSAGE =
            "File size exceeds the 10 MB limit. Please upload a smaller image.";
    public static final String TECHNOLOGY_MAP_FORMAT_MESSAGE =
            "Invalid file format. Please upload a PNG or JPEG image.";

    /** Directory every uploaded document lives in (see {@code FileController}). */
    private static final String UPLOAD_ROOT = "uploads";

    /** Folder the Technology Map images are stored in, inside the uploads root. */
    private static final String TECHNOLOGY_MAP_DIR = "technology-maps";

    // ========================
    // LINE-ITEM BUDGET FILE (section 25 of the Create Proposal form)
    // ========================
    // The proponent uploads the filled-out line-item budget instead of entering the
    // budget details into the form. The same limits the form enforces in the browser
    // are enforced again here, so a request that bypasses the form cannot store an
    // oversized file or a document that merely has a .pdf / .docx name.

    /** Largest accepted Line-Item Budget file: 10 MB (10 * 1024 * 1024 bytes). */
    public static final long BUDGET_FILE_MAX_BYTES = 10L * 1024 * 1024;

    /** Document formats accepted for the Line-Item Budget. */
    private static final Set<String> BUDGET_FILE_FORMATS = Set.of("pdf", "docx");

    /** Media types an upload may declare for those formats. */
    private static final Set<String> BUDGET_FILE_MEDIA_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            // A .docx is an OOXML package (a ZIP archive), which some clients report
            // as a ZIP media type.
            "application/zip",
            "application/x-zip-compressed");

    /** Messages shown by the form as well, so both layers report the same reason. */
    public static final String BUDGET_FILE_SIZE_MESSAGE =
            "File size exceeds the 10 MB limit. Please upload a smaller file.";
    public static final String BUDGET_FILE_FORMAT_MESSAGE =
            "Invalid file format. Please upload a PDF or DOCX file.";

    /** Folder the Line-Item Budget documents are stored in, inside the uploads root. */
    private static final String BUDGET_FILE_DIR = "budget-files";

    /** "%PDF-": the header of every PDF document. */
    private static final byte[] PDF_HEADER = { 0x25, 0x50, 0x44, 0x46, 0x2d };

    /** "PK": the header of a ZIP archive, and therefore of an OOXML (.docx) package. */
    private static final byte[] ZIP_HEADER = { 0x50, 0x4b };

    // ========================
    // PROPOSAL ID AND REVISION FORMAT
    // ========================
    // Proposal ID: 3-digit continuous counter (001, 002, 003, ... 139)
    // Revision number: 2-digit format (00 = initial, 01 = first revision, 02 =
    // second, etc.)
    // Document format: MSUN-ORPS-DA-{proposalId}-{year}-REV{revisionNo}
    // Example: MSUN-ORPS-DA-001-2026-REV00

    /**
     * Allocate the next proposal code (3-digit continuous counter) from the
     * database sequence. The counter never resets across years and is safe under
     * concurrent submissions and multiple backend instances.
     */
    private String generateNextProposalCode() {
        Number next = proposalRepository.nextProposalCodeSequence();
        if (next == null) {
            throw new IllegalStateException(
                    "Unable to allocate the next proposal code: the proposal_code_seq sequence is unavailable.");
        }
        return String.format("%03d", next.longValue());
    }

    /**
     * Generate the document ID in format:
     * MSUN-ORPS-DA-{proposalCode}-{year}-REV{revisionNo}
     * 
     * @param proposalCode   3-digit proposal code (e.g., "001")
     * @param revisionNumber revision number (e.g., 0 for initial, 1 for first
     *                       revision)
     * @return formatted document ID
     */
    private String generateDocumentId(String proposalCode, int revisionNumber) {
        int year = Year.now().getValue();
        return String.format("MSUN-ORPS-DA-%s-%d-REV%02d", proposalCode, year, revisionNumber);
    }

    public List<Proposal> getAllProposals() {
        return proposalRepository.findAll();
    }

    public List<Proposal> getProposalsByStatus(String status) {
        return proposalRepository.findByStatus(status.toUpperCase());
    }

    public List<Proposal> getProposalsByProponent(Long proponentId) {
        User proponent = userRepository.findById(proponentId)
                .orElseThrow(() -> new IllegalArgumentException("Proponent not found: " + proponentId));
        return proposalRepository.findByProponent(proponent);
    }

    public List<Proposal> getProposalsByStatusIn(List<String> statuses) {
        return proposalRepository.findByStatusIn(statuses);
    }

    public Proposal getProposalById(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + id));
    }

    /**
     * Lists proposals for the caller, enforcing ownership server-side.
     * <p>
     * Only explicitly authorized staff/reviewer roles ({@link CurrentUser#isPrivileged()})
     * may see the university-wide set. Every other caller — a Faculty Proponent or
     * any unrecognized role — is scoped to their own proposals, so the endpoint
     * fails closed instead of leaking records. The {@code requestedProponentId}
     * supplied by the frontend is ignored for non-privileged callers.
     */
    public List<Proposal> getProposalsForCurrentUser(Long requestedProponentId, String status, List<String> statusIn) {
        String normalizedStatus = status != null ? status.toUpperCase() : null;
        List<String> normalizedStatuses = statusIn != null
                ? statusIn.stream().filter(s -> s != null && !s.isBlank()).map(String::toUpperCase).toList()
                : List.of();

        if (!currentUser.isPrivileged()) {
            // Own-records-only. requireUserId() fails with 401 when unauthenticated,
            // so an unidentified caller can never fall through to the full list.
            User me = userRepository.findById(currentUser.requireUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Proponent not found"));
            if (normalizedStatus != null) {
                return proposalRepository.findByProponentAndStatus(me, normalizedStatus);
            }
            if (!normalizedStatuses.isEmpty()) {
                return proposalRepository.findByProponentAndStatusIn(me, normalizedStatuses);
            }
            return proposalRepository.findByProponent(me);
        }

        // Privileged staff/reviewers keep the existing university-wide view.
        if (requestedProponentId != null) {
            return getProposalsByProponent(requestedProponentId);
        }
        if (normalizedStatus != null) {
            return getProposalsByStatus(normalizedStatus);
        }
        if (!normalizedStatuses.isEmpty()) {
            return getProposalsByStatusIn(normalizedStatuses);
        }
        return getAllProposals();
    }

    /**
     * Loads a single proposal, enforcing ownership for Proponents. Used by every
     * endpoint that returns proposal data (details, versions, reviews, ...) so a
     * Proponent cannot reach another user's proposal by changing the id.
     */
    public Proposal getProposalByIdForCurrentUser(Long id) {
        Proposal proposal = getProposalById(id);
        currentUser.requireAccessTo(proposal);
        return proposal;
    }

    @Transactional
    public Proposal saveProposal(ProposalRequest request, Long proposalId) {
        Proposal proposal;
        boolean isNewProposal = (proposalId == null);
        boolean isRevisionResubmission = false;
        String revisionRemarks = null;

        if (proposalId != null) {
            // Lock the row for the duration of the transaction so two simultaneous
            // revision resubmissions of the same proposal are serialized and cannot
            // allocate the same revision number / document id.
            proposal = proposalRepository.findByIdForUpdate(proposalId)
                    .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));
            // A Proponent may only modify a proposal they own.
            currentUser.requireAccessTo(proposal);
            // Check if this is a revision resubmission (proposal was returned and is now
            // being resubmitted)
            if (proposalVersionService.isRevisionStatus(proposal.getStatus())) {
                isRevisionResubmission = true;
                // Preserve the reviewer's return remarks before the row is overwritten, and
                // snapshot the previous submission so it is never lost from history.
                revisionRemarks = proposal.getRemarks();
                proposalVersionService.ensureBaseline(proposal);

                // Enforce the RPS-set revision deadline: the Proponent cannot submit the
                // revised proposal after the deadline.
                if (proposal.getRevisionDeadline() != null
                        && LocalDateTime.now().isAfter(proposal.getRevisionDeadline())) {
                    throw new IllegalStateException(
                            "The revision deadline has passed. Revision submission is no longer available.");
                }
            }
        } else {
            proposal = new Proposal();
            // The owner is always derived from the authenticated principal, never from
            // the (spoofable) request payload.
            proposal.setProponent(resolveOwnerForNewProposal(request));
        }

        // Proposal Announcement gatekeeper (server-side enforcement): new proposals and
        // final submissions are only accepted while a proposal announcement is active.
        // Revision resubmissions belong to the review lifecycle of an already
        // submitted proposal and remain allowed outside an active announcement.
        boolean isSubmission = request.getStatus() != null
                && "SUBMITTED".equalsIgnoreCase(request.getStatus());

        // The "Address *" / "Contact *" sections are validated again here, so a
        // request cannot bypass the form's own checks. Drafts are exempt: they are
        // saved while the proponent is still filling the form in.
        if (isSubmission) {
            validateAddressAndContact(request);
            validateCooperatingAgencies(request);
        }
        ProposalAnnouncement activeAnnouncement = null;
        // The Call for Proposal the proponent selected in step 1 of the Create Proposal
        // workflow. When it is sent, that selection wins over the implicit "currently
        // active" announcement, so a proposal can never end up under a different call
        // than the one the proponent chose.
        Long selectedAnnouncementId = request.getProposalAnnouncementId();
        boolean hasSelectedCall = selectedAnnouncementId != null;

        if ((isNewProposal || isSubmission) && !isRevisionResubmission && !hasSelectedCall) {
            activeAnnouncement = proposalAnnouncementService.getActiveAnnouncementOrNull();
            if (activeAnnouncement == null) {
                throw new IllegalStateException(
                        "Submissions are currently closed. There is no active proposal announcement at this time.");
            }
        }

        // Associate the proposal with the active submission window so RPS can later
        // enforce that announcements with submitted proposals cannot be deleted.
        if (activeAnnouncement != null) {
            proposal.setProposalAnnouncementId(activeAnnouncement.getId());
        }

        // The selected call is validated whenever it is actually used or changed -
        // creating the proposal, submitting it, or pointing an existing draft at another
        // call - so a closed, cancelled or not-yet-open call is refused instead of being
        // silently accepted. Re-saving a draft that already carries its (possibly closed)
        // call stays possible, exactly as before; the gatekeeper above still blocks the
        // final submission of such a draft.
        if (hasSelectedCall && !isRevisionResubmission
                && (isNewProposal || isSubmission
                        || !selectedAnnouncementId.equals(proposal.getProposalAnnouncementId()))) {
            proposal.setProposalAnnouncementId(
                    proposalAnnouncementService.requireOpenAnnouncement(selectedAnnouncementId).getId());
        }

        // Generate proposal code and document ID for new proposals
        if (isNewProposal) {
            String proposalCode = generateNextProposalCode();
            proposal.setProposalCode(proposalCode);
            proposal.setRevisionNumber(0);
            proposal.setDocumentId(generateDocumentId(proposalCode, 0));
        }
        // Increment revision number and regenerate document ID for revision
        // resubmissions
        else if (isRevisionResubmission) {
            int newRevisionNumber = (proposal.getRevisionNumber() != null ? proposal.getRevisionNumber() : 0) + 1;
            proposal.setRevisionNumber(newRevisionNumber);
            proposal.setDocumentId(generateDocumentId(proposal.getProposalCode(), newRevisionNumber));
        }

        // Owner reassignment is only permitted for privileged roles on an existing
        // proposal; a Proponent can never move their proposal to another user.
        if (!isNewProposal && currentUser.isPrivileged() && request.getProponentId() != null) {
            User proponent = userRepository.findById(request.getProponentId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Proponent not found: " + request.getProponentId()));
            proposal.setProponent(proponent);
        }

        // Standard properties
        proposal.setProgramTitle(request.getProgramTitle());
        proposal.setProjectTitle(request.getProjectTitle());
        proposal.setProjectLeader(request.getProjectLeader());
        proposal.setProjectLeaderSex(request.getProjectLeaderSex());
        // Project duration is selected by the proponent (12, 24 or 36 months). It is
        // validated server-side (not only by the frontend dropdown) so a modified API
        // request cannot persist an unsupported value on a new proposal or on a draft
        // that is still being prepared. Proposals already in the review lifecycle
        // (e.g. revision resubmissions) keep their existing duration handling.
        boolean applySelectedDuration = isNewProposal
                || proposal.getStatus() == null
                || "DRAFT".equalsIgnoreCase(proposal.getStatus());
        proposal.setDuration(applySelectedDuration
                ? validateSelectedDuration(request.getDuration())
                : request.getDuration());
        proposal.setCollege(request.getCollege());
        applyAddressAndContact(proposal, request);
        proposal.setCooperatingAgencies(request.getCooperatingAgencies());
        proposal.setResearchType(request.getResearchType());
        proposal.setInnovationGoals(request.getInnovationGoals());
        proposal.setSectorRelevance(request.getSectorRelevance());
        proposal.setSdg(request.getSdg() != null ? request.getSdg() : List.of());
        proposal.setExecutiveSummary(request.getExecutiveSummary());
        proposal.setRationale(request.getRationale());
        proposal.setFramework(request.getFramework());
        proposal.setObjectivesGeneral(request.getObjectivesGeneral());
        proposal.setObjectivesSpecific(request.getObjectivesSpecific());
        proposal.setReview(request.getReview());
        proposal.setReviewFileName(request.getReviewFileName());
        proposal.setMethodology(request.getMethodology());
        proposal.setRoadmapFileName(request.getRoadmapFileName());
        proposal.setTechnologyTrl(request.getTechnologyTrl());
        proposal.setOutputs(request.getOutputs());
        proposal.setOutcomes(request.getOutcomes());
        proposal.setImpactEconomic(request.getImpactEconomic());
        proposal.setImpactSocial(request.getImpactSocial());
        proposal.setBeneficiaries(request.getBeneficiaries());
        proposal.setBeneficiariesFileName(request.getBeneficiariesFileName());
        proposal.setSustainability(request.getSustainability());
        proposal.setGadScore(request.getGadScore());
        proposal.setGadFileName(request.getGadFileName());
        proposal.setRisks(request.getRisks());
        proposal.setLimitationsText(request.getLimitationsText());
        proposal.setOtherProjectsNumber(request.getOtherProjectsNumber());

        // Status
        if (request.getStatus() != null) {
            proposal.setStatus(request.getStatus().toUpperCase());
        } else if (proposal.getStatus() == null) {
            proposal.setStatus("DRAFT");
        }

        // Dates parsing
        try {
            if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                proposal.setStartDate(LocalDate.parse(request.getStartDate()));
            }
        } catch (DateTimeParseException e) {
            // Ignore format issues or log
        }
        try {
            if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                proposal.setEndDate(LocalDate.parse(request.getEndDate()));
            }
        } catch (DateTimeParseException e) {
            // Ignore
        }

        // Project period: the End Date is calculated by the system (the selected duration
        // added to the start date - see validateProjectPeriod). Enforced here as well as in
        // the form, so a modified API request cannot store another date. Only proposals that
        // are still being prepared are checked, exactly like the duration rule above.
        if (applySelectedDuration) {
            validateProjectPeriod(proposal.getStartDate(), proposal.getEndDate(), proposal.getDuration());
        }

        // Section 23 (Literature Cited). The form sends the plain-text rendering of
        // every reference and the structured entries behind it. A client that does not
        // send them (null) leaves the stored value untouched, so a partial update can
        // never wipe the literature cited.
        if (request.getReferencesText() != null) {
            proposal.setReferencesText(request.getReferencesText().trim());
        }
        if (request.getReferencesJson() != null) {
            proposal.setReferencesJson(request.getReferencesJson().trim());
        }

        // Legacy clients send the structured list of author/title/year instead: keep
        // formatting it as text (they do not send referencesText).
        if (request.getReferences() != null && request.getReferencesText() == null) {
            StringBuilder sb = new StringBuilder();
            for (ProposalRequest.ReferenceDTO ref : request.getReferences()) {
                if (ref.getAuthor() != null || ref.getTitle() != null || ref.getYear() != null) {
                    sb.append(ref.getAuthor()).append(". (")
                            .append(ref.getYear()).append("). ")
                            .append(ref.getTitle()).append(".\n");
                }
            }
            proposal.setReferencesText(sb.toString().trim());
        }

        // Priority Agendas (section 05): the keys the proposal already stores are
        // captured before the child list is cleared below. They are what lets a revision
        // resubmit an agenda that belongs to an older (now deactivated) framework
        // without ever letting a client introduce an unknown or inactive key.
        List<String> storedAgendaKeys = proposal.getPriorityAgendas().stream()
                .map(ProposalPriorityAgenda::getAgendaKey)
                .filter(Objects::nonNull)
                .toList();

        // Clear existing child lists to overwrite (cascade all + orphan removal takes
        // care of orphans). The limitations table is handled separately below: only
        // the forms that actually edit it send it, so clearing it here would silently
        // delete existing rows on every revision resubmission.
        proposal.getSites().clear();
        proposal.getLogFrames().clear();
        proposal.getPersonnel().clear();
        proposal.getBudget().clear();
        proposal.getOtherProjects().clear();
        proposal.getPriorityAgendas().clear();

        // Save parent proposal to generate ID if new
        proposal = proposalRepository.save(proposal);

        // Cooperating Agencies (section 02). The checklist sends the ids of the
        // agencies selected from the shared directory, so the selection is stored
        // relationally in proposal_cooperating_agencies while the text column every
        // existing screen reads is derived from the resolved names - the two can
        // therefore never drift apart. A client that predates the checklist (the
        // revision form before this change, integrations) sends only the text, which
        // was stored above and is left untouched.
        if (request.getCooperatingAgencyIds() != null) {
            List<CooperatingAgency> selectedAgencies = cooperatingAgencyService
                    .replaceSelection(proposal, request.getCooperatingAgencyIds());
            proposal.setCooperatingAgencies(CooperatingAgencyService.describeSelection(selectedAgencies));
        }

        // Sites
        if (request.getSites() != null) {
            for (ProposalRequest.SiteDTO s : request.getSites()) {
                ProposalSite site = new ProposalSite(null, proposal, s.getCountry(), s.getRegion(), s.getProvince(),
                        s.getDistrict(), s.getMunicipality(), s.getBarangay());
                proposal.getSites().add(site);
            }
        }

        // LogFrames
        if (request.getLogFrame() != null) {
            for (ProposalRequest.LogFrameDTO lf : request.getLogFrame()) {
                ProposalLogFrame logFrame = new ProposalLogFrame(null, proposal, lf.getOutcome(), lf.getOutput());
                proposal.getLogFrames().add(logFrame);
            }
        }

        // Personnel
        if (request.getPersonnel() != null) {
            for (ProposalRequest.PersonnelDTO p : request.getPersonnel()) {
                ProposalPersonnel personnel = new ProposalPersonnel(null, proposal, p.getPosition(), p.getTime(),
                        p.getResponsibilities());
                proposal.getPersonnel().add(personnel);
            }
        }

        // Budget
        if (request.getBudget() != null) {
            for (ProposalRequest.BudgetDTO b : request.getBudget()) {
                ProposalBudget budget = new ProposalBudget(null, proposal, b.getAgency(), b.getPs(), b.getMooe(),
                        b.getEo(), b.getTotal());
                proposal.getBudget().add(budget);
            }
        }

        // Other Projects
        if (request.getOtherProjects() != null) {
            for (ProposalRequest.OtherProjectDTO op : request.getOtherProjects()) {
                ProposalOtherProject otherProject = new ProposalOtherProject(null, proposal, op.getTitle(),
                        op.getAgency(), op.getInvolvement());
                proposal.getOtherProjects().add(otherProject);
            }
        }

        // Limitations table. Only touched when the request actually carries the table,
        // so a form that does not edit it (the revision form) can never wipe existing
        // rows — that previously caused silent data loss on every resubmission.
        if (request.getLimitationsTable() != null) {
            proposal.getLimitations().clear();
            for (ProposalRequest.LimitationDTO l : request.getLimitationsTable()) {
                ProposalLimitation limitation = new ProposalLimitation(null, proposal, l.getLimitation(),
                        l.getRemarks());
                proposal.getLimitations().add(limitation);
            }
        }

        // Priority Agendas (section 05). Every submitted key is validated against the
        // configured master data: the active framework's active agendas plus the agendas
        // this proposal already stores (so a revision of a proposal submitted under an
        // older framework keeps working). An unknown or inactive key is rejected with a
        // readable 400 instead of being persisted.
        if (request.getPriorityAgenda() != null) {
            Set<String> allowedAgendaKeys = priorityAgendaService.allowedProposalKeys(storedAgendaKeys);
            for (Map.Entry<String, ProposalRequest.PriorityAgendaDTO> entry : request.getPriorityAgenda().entrySet()) {
                String agendaKey = entry.getKey() == null ? "" : entry.getKey().trim();
                if (agendaKey.isEmpty()
                        || allowedAgendaKeys == null
                        || !allowedAgendaKeys.contains(agendaKey.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException(
                            "Unknown or inactive priority agenda: " + entry.getKey());
                }
                ProposalRequest.PriorityAgendaDTO val = entry.getValue();
                ProposalPriorityAgenda agenda = new ProposalPriorityAgenda(null, proposal, agendaKey,
                        val.getSelected(), val.getValue());
                proposal.getPriorityAgendas().add(agenda);
            }
        }

        // Save again to cascade save child collections
        proposal = proposalRepository.save(proposal);

        // Record this submission in the immutable version history (original submission
        // or a revision). Previous versions are preserved and never overwritten.
        if (proposalVersionService.isSubmissionStatus(proposal.getStatus())) {
            proposalVersionService.recordSubmission(proposal, isRevisionResubmission, revisionRemarks);
        }

        // A revision resubmission closes the open remark round: the feedback the
        // reviewer gave for the previous cycle becomes history and the next review
        // cycle starts a fresh round, so nothing is ever overwritten.
        if (isRevisionResubmission) {
            proposalSectionRemarkService.closeActiveRound(proposal);
        }

        // Send system notification for submissions
        notifySubscribedRoles(
                proposal,
                "Proposal Update: " + proposal.getProjectTitle());

        return proposal;
    }

    /**
     * Resolves the owner of a brand-new proposal. A Faculty Proponent is always the
     * authenticated caller; privileged roles may create a proposal on behalf of a
     * specified proponent.
     */
    private User resolveOwnerForNewProposal(ProposalRequest request) {
        if (!currentUser.isPrivileged()) {
            // Faculty Proponent (or any non-privileged role) always owns their own
            // submission; requireUserId() fails closed with 401 if unauthenticated.
            return userRepository.findById(currentUser.requireUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Proponent not found"));
        }
        if (request.getProponentId() != null) {
            return userRepository.findById(request.getProponentId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Proponent not found: " + request.getProponentId()));
        }
        return null;
    }

    /**
     * Validates a proponent-selected project duration. Only the supported internally
     * funded durations (12, 24 or 36 months) are accepted.
     */
    private String validateSelectedDuration(String duration) {
        String normalized = duration == null ? null : duration.trim();
        if (normalized == null || !SUPPORTED_DURATIONS.contains(normalized)) {
            throw new IllegalArgumentException("Project duration must be one of 12, 24 or 36 months.");
        }
        return normalized;
    }

    /**
     * The calculated End Date rule: the End Date is the start date plus the selected number
     * of months, keeping the day of the month and using the last valid day when the target
     * month is shorter (31 January + 1 month = 28 February). A 12-month project starting on
     * 1 October 2026 therefore ends on 1 October 2027.
     *
     * <p>The same rule calculates the field in the Create Proposal form
     * ({@code src/utils/projectDates.js}), so the form and the server always agree; a
     * modified API request cannot store a different date.</p>
     *
     * <p>Only the ordering rule is applied when a proposal carries a duration that is not one
     * of the supported options, because no end date can be derived from it.</p>
     */
    private void validateProjectPeriod(LocalDate startDate, LocalDate endDate, String duration) {
        if (startDate == null || endDate == null) {
            return;
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "The project end date cannot be earlier than the start date (" + startDate + ").");
        }
        Integer months = supportedDurationMonths(duration);
        if (months == null) {
            return;
        }
        LocalDate calculatedEndDate = startDate.plusMonths(months);
        if (!endDate.equals(calculatedEndDate)) {
            throw new IllegalArgumentException("The project end date is calculated from the duration and"
                    + " the start date: a " + months + "-month project starting " + startDate
                    + " must end on " + calculatedEndDate + ".");
        }
    }

    /** Whole months of a supported duration, or null when the value is not one of them. */
    private Integer supportedDurationMonths(String duration) {
        String normalized = duration == null ? null : duration.trim();
        if (normalized == null || !SUPPORTED_DURATIONS.contains(normalized)) {
            return null;
        }
        try {
            return Integer.valueOf(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Statuses that must only be set through the RPS revision workflow. */
    private static final Set<String> REVISION_STATUSES = Set.of(
            "RPS_RETURNED", "REVISION", "REC_REVISION", "PENDING_REVISION", "RETURNED_TO_RPS");

    @Transactional
    public Proposal updateProposalStatus(Long id, String status) {
        String upper = status != null ? status.toUpperCase() : null;
        if (upper != null && REVISION_STATUSES.contains(upper)) {
            throw new IllegalStateException(
                    "Revision statuses can only be set through the RPS revision workflow.");
        }
        Proposal proposal = getProposalById(id);
        currentUser.requireAccessTo(proposal);
        proposal.setStatus(upper);
        proposal = proposalRepository.save(proposal);

        // Dispatch notifications to the next user level and the proponent
        notifySubscribedRoles(proposal, null);

        return proposal;
    }

    /**
     * Save a proposal entity directly (used when fields are set manually on the
     * entity).
     * Also dispatches notifications based on the current status.
     */
    @Transactional
    public Proposal saveProposalDirect(Proposal proposal) {
        proposal = proposalRepository.save(proposal);
        notifySubscribedRoles(proposal, null);
        return proposal;
    }

    @Transactional
    public Proposal updateReviewerInfo(Long id, String reviewedBy, String reviewedByPosition) {
        Proposal proposal = getProposalById(id);
        currentUser.requireAccessTo(proposal);
        proposal.setReviewedBy(reviewedBy);
        proposal.setReviewedByPosition(reviewedByPosition);
        return proposalRepository.save(proposal);
    }

    @Transactional
    public Proposal returnForRevision(Long id, String remarks, String returnedByOffice,
                                      Long returnedByUserId, String returnedByName) {
        return returnForRevision(id, remarks, returnedByOffice, returnedByUserId, returnedByName, null);
    }

    /**
     * Returns a proposal for revision, optionally recording section-specific remarks.
     * The section remarks open a new revision round so any previous round's feedback
     * is retained as history instead of being overwritten.
     */
    @Transactional
    public Proposal returnForRevision(Long id, String remarks, String returnedByOffice,
                                      Long returnedByUserId, String returnedByName,
                                      List<SectionRemarkDTO> sectionRemarks) {
        Proposal proposal = getProposalById(id);
        currentUser.requireAccessTo(proposal);
        // A return from a reviewing office goes to RPS first, never directly to the
        // Proponent. RPS is the only party that can forward the request and set the
        // revision deadline.
        proposal.setStatus("RETURNED_TO_RPS");
        proposal.setRemarks(remarks);
        proposal.setReturnRemarks(remarks);
        proposal.setReturnedByOffice(returnedByOffice);
        proposal.setReturnedByUserId(returnedByUserId);
        proposal.setReturnedByName(returnedByName);
        proposal.setReturnedAt(LocalDateTime.now());
        // Reset any previous forward/deadline metadata so a fresh return restarts the flow.
        proposal.setRevisionDeadline(null);
        proposal.setRevisionForwardedAt(null);
        proposal.setRevisionForwardedByName(null);
        proposal.setRevisionForwardedByUserId(null);
        proposal.setRevisionNotes(null);
        proposal = proposalRepository.save(proposal);

        // Persist the per-section remarks for this revision round (if any).
        proposalSectionRemarkService.recordReturnRemarks(
                proposal, sectionRemarks, returnedByOffice, returnedByUserId, returnedByName);

        // Notify RPS to review and forward the revision request.
        notifySubscribedRoles(proposal, null);

        return proposal;
    }

    /**
     * RPS-only action: set the revision deadline and forward the revision request to
     * the Proponent. Also used when RPS itself returns a proposal during its own
     * review (RPS is the forwarder, so it records remarks + deadline in one step).
     */
    @Transactional
    public Proposal forwardRevisionToProponent(Long id, String deadline, String notes,
                                               Long forwardedByUserId, String forwardedByName,
                                               String remarks) {
        return forwardRevisionToProponent(id, deadline, notes, forwardedByUserId, forwardedByName,
                remarks, null);
    }

    /**
     * RPS-only action: set the revision deadline and forward the revision request to
     * the Proponent. Also used when RPS itself returns a proposal during its own
     * review (RPS is the forwarder, so it records remarks + deadline in one step).
     * Section-specific remarks added by RPS are appended to the round already opened
     * by the returning office, so both offices' feedback stays visible per section.
     */
    @Transactional
    public Proposal forwardRevisionToProponent(Long id, String deadline, String notes,
                                               Long forwardedByUserId, String forwardedByName,
                                               String remarks, List<SectionRemarkDTO> sectionRemarks) {
        Proposal proposal = getProposalById(id);
        currentUser.requireAccessTo(proposal);
        LocalDateTime deadlineLdt = parseDeadline(deadline);

        // If no office return was recorded yet (RPS returning directly), capture the
        // remarks + attribution here so the record is complete.
        if (proposal.getReturnRemarks() == null || proposal.getReturnRemarks().isBlank()) {
            proposal.setReturnRemarks(remarks);
            proposal.setReturnedByOffice("RPS");
            proposal.setReturnedByUserId(forwardedByUserId);
            proposal.setReturnedByName(forwardedByName);
            if (proposal.getReturnedAt() == null) {
                proposal.setReturnedAt(LocalDateTime.now());
            }
        }

        proposal.setStatus("RPS_RETURNED");
        proposal.setRevisionDeadline(deadlineLdt);
        proposal.setRevisionForwardedAt(LocalDateTime.now());
        proposal.setRevisionForwardedByUserId(forwardedByUserId);
        proposal.setRevisionForwardedByName(forwardedByName);
        proposal.setRevisionNotes(notes);

        // Keep the proponent-facing remarks in sync with the office's original remarks so
        // the revision history continues to capture them.
        proposal.setRemarks(proposal.getReturnRemarks());

        proposal = proposalRepository.save(proposal);

        // Append RPS's own per-section remarks (if any) to the open revision round so
        // the Proponent sees the office feedback and the RPS feedback together.
        proposalSectionRemarkService.recordForwardRemarks(
                proposal, sectionRemarks, "RPS", forwardedByUserId, forwardedByName);

        // Notify the Proponent of the revision request + deadline.
        if (proposal.getProponent() != null) {
            notificationService.createNotification(
                    proposal.getProponent().getId(),
                    "Your proposal \"" + proposal.getProjectTitle()
                            + "\" has been returned for revision. Deadline: "
                            + (deadlineLdt != null ? deadlineLdt.toLocalDate().toString() : "—"),
                    "Revision Required",
                    "REVISION",
                    proposal.getId());
        }

        return proposal;
    }

    private LocalDateTime parseDeadline(String deadline) {
        if (deadline == null || deadline.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(deadline);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(deadline).atTime(23, 59, 59);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    @Transactional
    public void deleteProposal(Long id) {
        Proposal proposal = getProposalById(id);
        currentUser.requireAccessTo(proposal);
        // The proposal's agency links go with it; the shared directory rows stay, so
        // the same agencies remain selectable on every other proposal.
        cooperatingAgencyService.deleteSelection(proposal.getId());
        proposalRepository.delete(proposal);
    }

    /**
     * Creates a brand-new DRAFT proposal by reusing the reusable content of an
     * existing proposal.
     * <p>
     * The copy is an independent record: it receives its own proposal code and
     * document id from the existing sequence allocation, its child rows are fresh
     * copies bound to the new proposal, and none of the original proposal's workflow
     * state (status, revision metadata, review remarks, approval/SO data, reports,
     * notifications or submission history) is carried over. Uploaded attachments are
     * intentionally not carried over either, because those files belong to the
     * original proposal and the new-proposal form cannot prefill them — the proponent
     * re-uploads them before submitting.
     * <p>
     * The original's status never decides whether it can be copied: the copy is a
     * private DRAFT that starts the normal submission workflow from scratch, so
     * reusing the content of a rejected, pending, under-review, approved or
     * returned-for-revision proposal is always allowed. Only authorization is
     * checked here - a proponent can only copy their own proposal.
     *
     * @param sourceId       id of the proposal to copy
     * @param announcementId id of the Call for Proposal the copy belongs to
     * @return the newly created DRAFT proposal
     */
    @Transactional
    public Proposal duplicateProposal(Long sourceId, Long announcementId) {
        Proposal source = getProposalById(sourceId);
        // Ownership is enforced server-side: a Proponent can never duplicate a
        // proposal they are not authorized to access by manipulating the request.
        currentUser.requireAccessTo(source);

        // A proposal whose proponent record was deleted has no owner, so a duplicate
        // could never be reached by anyone. Fail instead of creating an orphan record.
        if (source.getProponent() == null) {
            throw new IllegalStateException(
                    "This proposal has no proponent on record and cannot be duplicated.");
        }

        // The Call for Proposal the proponent selected in the picker: without it no copy
        // is created, and the call is validated with the same rule the submission
        // gatekeeper uses (it must exist and still be open for submissions).
        if (announcementId == null) {
            throw new IllegalArgumentException(
                    "Please select a call for proposal before making a copy.");
        }
        ProposalAnnouncement announcement = proposalAnnouncementService.requireOpenAnnouncement(announcementId);

        Proposal copy = new Proposal();
        // The duplicate belongs to the same proponent so it stays in their workspace.
        copy.setProponent(source.getProponent());
        // It belongs to the call the proponent selected, so it is listed under that call
        // in their own workspace and in the RPS views, and its submission is validated
        // against the same call.
        copy.setProposalAnnouncementId(announcement.getId());
        copy.setStatus("DRAFT");
        copy.setRevisionNumber(0);
        String proposalCode = generateNextProposalCode();
        copy.setProposalCode(proposalCode);
        copy.setDocumentId(generateDocumentId(proposalCode, 0));

        copyReusableContent(source, copy);

        // Two-step save mirrors saveProposal(): the parent is inserted first so the
        // child collections can be cascaded with the generated foreign key. Everything
        // happens in one transaction, so a failure leaves no partial duplicate behind.
        copy = proposalRepository.save(copy);
        copyChildCollections(source, copy);
        // The duplicate points at the same shared directory agencies as the original:
        // the agencies are referenced again, never copied and never removed from the
        // original proposal.
        cooperatingAgencyService.copySelection(source.getId(), copy);
        return proposalRepository.save(copy);
    }

    /**
     * Copies only the content fields that are meaningful to reuse. Workflow fields
     * (status, identifiers, revision/SO/approval metadata, remarks) are intentionally
     * excluded and set explicitly by {@link #duplicateProposal(Long, Long)}, and uploaded
     * attachment references (review/roadmap/beneficiaries/GAD file names) are left
     * blank because those files belong to the original proposal and must be uploaded
     * again on the copy.
     */
    /** Longest accepted value per structured address part (mirrors the columns). */
    static final int ADDRESS_PART_MAX_LENGTH = 120;
    /** Longest accepted contact type / contact value (mirrors the columns). */
    static final int CONTACT_TYPE_MAX_LENGTH = 30;
    static final int CONTACT_VALUE_MAX_LENGTH = 255;

    /** Contact types the Create Proposal form offers. */
    static final Set<String> CONTACT_TYPES = Set.of("CONTACT_NUMBER", "EMAIL_ADDRESS");

    /** General email shape; mirrors the frontend's contactDetails rules. */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    /**
     * A contact number may hold digits, spaces, dashes, dots, parentheses and a
     * single leading "+" (so a Philippine number keeps its leading zero and an
     * international one its country code).
     */
    private static final Pattern CONTACT_NUMBER_PATTERN = Pattern.compile("^\\+?[()\\d\\s.-]*$");

    /**
     * Server-side validation of the "Address *" and "Contact *" sections.
     * <p>
     * The form validates the same rules, but a request can also arrive from a
     * client that skips that validation, so the values are checked again before a
     * proposal is submitted: the address must be complete for the Philippine
     * administrative hierarchy (see {@link #validateAddressHierarchy}) and the
     * contact must match its selected type. Only what the client actually sends is
     * validated: a client that predates the split (the revision form) sends neither
     * section and its legacy {@code address} text keeps working unchanged.
     * <p>
     * The Address section is Philippine-only, so the country is not a question any
     * more: a request that sends the address parts without a country is valid and
     * the country is stored as the Philippines (see
     * {@link #addressCountryOrDefault}).
     */
    static void validateAddressAndContact(ProposalRequest request) {
        boolean addressSupplied = request.getAddressCountry() != null
                || request.getAddressRegion() != null
                || request.getAddressProvince() != null
                || request.getAddressDistrict() != null
                || request.getAddressMunicipality() != null
                || request.getAddressBarangay() != null;
        if (addressSupplied) {
            requireAddressPart(request.getAddressRegion(), "Region");
            requireAddressPart(request.getAddressMunicipality(), "Municipality / City");
            requireAddressPart(request.getAddressBarangay(), "Barangay");
            validateAddressHierarchy(request);
        }

        if (request.getContactType() == null && request.getContactValue() == null) {
            return;
        }
        String type = trimToNull(request.getContactType());
        String value = trimToNull(request.getContactValue());
        if (type == null) {
            throw new IllegalArgumentException("Contact type is required when a contact value is given.");
        }
        if (!CONTACT_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unsupported contact type: " + type);
        }
        if (value == null) {
            throw new IllegalArgumentException("Contact value is required for the selected contact type.");
        }
        if ("EMAIL_ADDRESS".equals(type) && !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Please provide a valid email address.");
        }
        if ("CONTACT_NUMBER".equals(type) && !isValidContactNumber(value)) {
            throw new IllegalArgumentException("Please provide a valid contact number.");
        }
    }

    /**
     * Server-side check of the "Cooperating Agencies" section (section 02): a
     * submitted proposal must name at least one agency from the shared directory.
     * The ids themselves are validated against the directory when the selection is
     * stored ({@link CooperatingAgencyService#replaceSelection}), so a request can
     * never point a proposal at an agency that does not exist.
     * <p>
     * A client that predates the checklist sends only the legacy text and keeps its
     * existing behaviour: the form still requires that text, and nothing here
     * overrides it.
     */
    static void validateCooperatingAgencies(ProposalRequest request) {
        if (request.getCooperatingAgencyIds() == null) {
            return;
        }
        boolean hasAgency = request.getCooperatingAgencyIds().stream().anyMatch(Objects::nonNull);
        if (!hasAgency) {
            throw new IllegalArgumentException("At least one cooperating agency must be selected.");
        }
    }

    private static void requireAddressPart(String value, String label) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(label + " is required for the address.");
        }
    }

    /**
     * The parts of the address must actually belong to each other: a province of
     * another region, a city of another province or a district of another area
     * would otherwise be stored as a valid-looking address.
     * <p>
     * The check runs against the same PSGC extract the form's dropdowns are built
     * from ({@link PhilippineAddressCatalog}). The Address section is
     * Philippine-only, so a request that omits the country is checked as a
     * Philippine address ({@link #addressCountryOrDefault}); a country that is sent
     * is still verified, and a country other than the Philippines has no structured
     * hierarchy in RPIS - its parts are typed in by hand - so it keeps the simpler
     * rule that a province or a district identifies the area.
     */
    static void validateAddressHierarchy(ProposalRequest request) {
        String country = addressCountryOrDefault(request);
        PhilippineAddressCatalog catalog = PhilippineAddressCatalog.shared();
        if (catalog != null && !catalog.isKnownCountry(country)) {
            throw new IllegalArgumentException("Unknown country: " + country + ".");
        }
        if (catalog == null || !catalog.isPhilippines(country)) {
            // Without the bundled hierarchy (or for a foreign address) only the
            // presence of an area level can be checked.
            if (isBlank(request.getAddressProvince()) && isBlank(request.getAddressDistrict())) {
                throw new IllegalArgumentException("Province or district is required for the address.");
            }
            return;
        }
        String problem = catalog.describeProblem(request.getAddressRegion(), request.getAddressProvince(),
                request.getAddressDistrict(), request.getAddressMunicipality());
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
    }

    /**
     * The country of the "Address *" section.
     * <p>
     * RPIS handles Philippine addresses only: the form has no country control, so a
     * request that sends the address parts without a country means the Philippines.
     * A country that is sent is used as it is, which keeps a proposal saved while
     * the control still existed editable.
     */
    static String addressCountryOrDefault(ProposalRequest request) {
        String country = request == null ? null : trimToNull(request.getAddressCountry());
        return country == null ? PhilippineAddressCatalog.PHILIPPINES : country;
    }

    /** 7-15 digits, an optional leading "+" and the usual separators. */
    static boolean isValidContactNumber(String value) {
        if (value == null || !CONTACT_NUMBER_PATTERN.matcher(value).matches()) {
            return false;
        }
        int digits = value.replaceAll("\\D", "").length();
        return digits >= 7 && digits <= 15;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Maps the "Address *" and "Contact *" sections of the Create Proposal form
     * (which replace the single "Address / Contact" field).
     * <p>
     * The structured columns are only written when the caller actually sends
     * them: a client that predates the split (the revision form, integrations)
     * sends no such values, and its {@code null} must not wipe an address the
     * proponent already saved. An empty string still clears a part, because the
     * form always sends what it currently holds.
     * <p>
     * The Address section is Philippine-only, so the country is never asked for: when
     * the address parts are supplied without one, the country is stored as the
     * Philippines (see {@link #addressCountryOrDefault}).
     * <p>
     * Whenever the structured values are supplied, the combined {@code address}
     * column is rewritten from them plus the contact, so the proposal details,
     * reviewer views, exports and printouts - which all read that single column -
     * keep showing the complete "Address / Contact" text. A client that sends
     * only the legacy text has it stored unchanged.
     */
    static void applyAddressAndContact(Proposal proposal, ProposalRequest request) {
        boolean addressPartsSupplied = request.getAddressCountry() != null
                || request.getAddressRegion() != null
                || request.getAddressProvince() != null
                || request.getAddressDistrict() != null
                || request.getAddressMunicipality() != null
                || request.getAddressBarangay() != null;
        boolean supplied = addressPartsSupplied
                || request.getContactType() != null
                || request.getContactValue() != null;

        if (request.getAddressCountry() != null) {
            proposal.setAddressCountry(truncate(request.getAddressCountry(), ADDRESS_PART_MAX_LENGTH));
        } else if (addressPartsSupplied) {
            // The Address section is Philippine-only and its form no longer sends a
            // country, so the parts are stored as a Philippine address: the structured
            // columns and the combined "Address / Contact" text stay complete.
            proposal.setAddressCountry(PhilippineAddressCatalog.PHILIPPINES);
        }
        if (request.getAddressRegion() != null) {
            proposal.setAddressRegion(truncate(request.getAddressRegion(), ADDRESS_PART_MAX_LENGTH));
        }
        if (request.getAddressProvince() != null) {
            proposal.setAddressProvince(truncate(request.getAddressProvince(), ADDRESS_PART_MAX_LENGTH));
        }
        if (request.getAddressDistrict() != null) {
            proposal.setAddressDistrict(truncate(request.getAddressDistrict(), ADDRESS_PART_MAX_LENGTH));
        }
        if (request.getAddressMunicipality() != null) {
            proposal.setAddressMunicipality(truncate(request.getAddressMunicipality(), ADDRESS_PART_MAX_LENGTH));
        }
        if (request.getAddressBarangay() != null) {
            proposal.setAddressBarangay(truncate(request.getAddressBarangay(), ADDRESS_PART_MAX_LENGTH));
        }
        if (request.getContactType() != null) {
            proposal.setContactType(truncate(request.getContactType(), CONTACT_TYPE_MAX_LENGTH));
        }
        if (request.getContactValue() != null) {
            proposal.setContactValue(truncate(request.getContactValue(), CONTACT_VALUE_MAX_LENGTH));
        }

        proposal.setAddress(supplied ? combineAddressAndContact(proposal) : request.getAddress());
    }

    /**
     * The combined "Address / Contact" text kept in {@code address}: narrowest
     * part first (the order the old free-text field asked for), then the contact.
     */
    static String combineAddressAndContact(Proposal proposal) {
        List<String> parts = new ArrayList<>();
        for (String part : List.of(
                nullToEmpty(proposal.getAddressBarangay()),
                nullToEmpty(proposal.getAddressMunicipality()),
                nullToEmpty(proposal.getAddressDistrict()),
                nullToEmpty(proposal.getAddressProvince()),
                nullToEmpty(proposal.getAddressRegion()),
                nullToEmpty(proposal.getAddressCountry()))) {
            if (!part.isBlank()) {
                parts.add(part.trim());
            }
        }
        String address = String.join(", ", parts);
        String contact = nullToEmpty(proposal.getContactValue()).trim();
        if (address.isEmpty()) {
            return contact;
        }
        return contact.isEmpty() ? address : address + " / " + contact;
    }

    /** Trims a value and caps it at the column length; {@code null} stays {@code null}. */
    static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    // ========================
    // TECHNOLOGY MAP UPLOAD (section 13 of the Create Proposal form)
    // ========================

    /**
     * Stores the Technology Map image of a proposal and records it on that proposal.
     * <p>
     * The image is attached to the caller's own proposal only (ownership is resolved
     * from the authenticated principal, never from the request), it must be a real
     * PNG/JPEG raster file of at most {@link #TECHNOLOGY_MAP_MAX_BYTES} bytes, and
     * the file is stored under a generated name - the proponent's own file name is
     * kept for display only. Replacing an image deletes the previous file, so a
     * proposal never accumulates orphaned uploads.
     */
    @Transactional
    public Proposal attachTechnologyMap(Long proposalId, MultipartFile file) {
        Proposal proposal = getProposalByIdForCurrentUser(proposalId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a Technology Map image first.");
        }
        if (file.getSize() > TECHNOLOGY_MAP_MAX_BYTES) {
            throw new IllegalArgumentException(TECHNOLOGY_MAP_SIZE_MESSAGE);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "The Technology Map image could not be read.");
        }

        String format = readTechnologyMapFormat(bytes);
        String extension = "jpeg".equals(format) ? "jpg" : format;
        String storedPath = writeTechnologyMap(bytes, extension);

        String previousPath = proposal.getRoadmapFilePath();
        proposal.setRoadmapFileName(technologyMapDisplayName(file, extension));
        proposal.setRoadmapFilePath(storedPath);
        Proposal saved = proposalRepository.save(proposal);

        deleteStoredTechnologyMap(previousPath);
        return saved;
    }

    /**
     * Reads the real image format of the uploaded bytes. A file that is not a
     * PNG/JPEG raster image has no usable reader and is rejected, so the stored
     * format can never be spoofed through the file name or the declared MIME type.
     */
    private String readTechnologyMapFormat(byte[] bytes) {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) {
                throw new IllegalArgumentException(TECHNOLOGY_MAP_FORMAT_MESSAGE);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException(TECHNOLOGY_MAP_FORMAT_MESSAGE);
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName() == null
                        ? ""
                        : reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!TECHNOLOGY_MAP_FORMATS.contains(format)) {
                    throw new IllegalArgumentException(TECHNOLOGY_MAP_FORMAT_MESSAGE);
                }
                return format;
            } catch (IOException e) {
                throw new IllegalArgumentException(TECHNOLOGY_MAP_FORMAT_MESSAGE);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(TECHNOLOGY_MAP_FORMAT_MESSAGE);
        }
    }

    /**
     * Stores the bytes under a generated name inside the uploads directory and
     * returns the path relative to it - the value the authorized download route
     * ({@code GET /api/files?path=...}) expects.
     */
    private String writeTechnologyMap(byte[] bytes, String extension) {
        try {
            Path uploadPath = Paths.get(UPLOAD_ROOT, TECHNOLOGY_MAP_DIR);
            Files.createDirectories(uploadPath);
            Path target = uploadPath.resolve(UUID.randomUUID() + "_technology-map." + extension);
            Files.write(target, bytes);
            return TECHNOLOGY_MAP_DIR + "/" + target.getFileName();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "The Technology Map image could not be stored.");
        }
    }

    /**
     * The proponent's own file name, kept for display only. Any directory part is
     * stripped and the length is capped to the column size, so a crafted upload can
     * never store a path-like value.
     */
    private String technologyMapDisplayName(MultipartFile file, String extension) {
        String fallback = "technology-map." + extension;
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return fallback;
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        if (name.isEmpty()) {
            return fallback;
        }
        return truncate(name, 255);
    }

    /**
     * Removes the file a previous upload of this proposal stored. Only paths inside
     * the Technology Map upload directory are touched, and a missing file (or a
     * leftover path from before this feature) is not an error.
     */
    private void deleteStoredTechnologyMap(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }
        Path uploadRoot = Paths.get(UPLOAD_ROOT, TECHNOLOGY_MAP_DIR).toAbsolutePath().normalize();
        // Stored paths are relative to the uploads directory ("technology-maps/x.png").
        Path resolved = Paths.get(UPLOAD_ROOT).resolve(storedPath).toAbsolutePath().normalize();
        if (!resolved.startsWith(uploadRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(resolved);
        } catch (IOException ignored) {
            // An orphaned file is not worth failing an otherwise successful upload for.
        }
    }

    // ========================
    // LINE-ITEM BUDGET UPLOAD (section 25 of the Create Proposal form)
    // ========================

    /**
     * Stores the filled-out Line-Item Budget of a proposal and records it on that
     * proposal.
     * <p>
     * The document is attached to the caller's own proposal only (ownership is
     * resolved from the authenticated principal, never from the request), it must be
     * a PDF or a DOCX of at most {@link #BUDGET_FILE_MAX_BYTES} bytes - decided from
     * the file name, the declared media type and the real bytes, so a renamed file
     * is rejected - and it is stored under a generated name while the proponent's own
     * file name is kept for display only. Replacing the file deletes the previous
     * one, so a proposal never accumulates orphaned uploads.
     */
    @Transactional
    public Proposal attachBudgetFile(Long proposalId, MultipartFile file) {
        Proposal proposal = getProposalByIdForCurrentUser(proposalId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a Line-Item Budget file first.");
        }
        if (file.getSize() > BUDGET_FILE_MAX_BYTES) {
            throw new IllegalArgumentException(BUDGET_FILE_SIZE_MESSAGE);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "The Line-Item Budget file could not be read.");
        }

        String format = readBudgetFileFormat(file, bytes);
        String storedPath = writeBudgetFile(bytes, format);

        String previousPath = proposal.getBeneficiariesFilePath();
        proposal.setBeneficiariesFileName(budgetFileDisplayName(file, format));
        proposal.setBeneficiariesFilePath(storedPath);
        Proposal saved = proposalRepository.save(proposal);

        deleteStoredBudgetFile(previousPath);
        return saved;
    }

    /**
     * The real format of an uploaded Line-Item Budget document: the extension must be
     * PDF or DOCX, a declared media type (when the client sends one) must belong to
     * that format, and the first bytes must be a PDF header or a ZIP (OOXML) header.
     * A document that only carries a ".pdf" name is therefore rejected.
     */
    private String readBudgetFileFormat(MultipartFile file, byte[] bytes) {
        String original = file.getOriginalFilename();
        String name = original == null ? "" : original.replace('\\', '/');
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
        if (!BUDGET_FILE_FORMATS.contains(extension)) {
            throw new IllegalArgumentException(BUDGET_FILE_FORMAT_MESSAGE);
        }

        String declared = file.getContentType() == null
                ? ""
                : file.getContentType().trim().toLowerCase(Locale.ROOT);
        boolean genericType = declared.isEmpty() || "application/octet-stream".equals(declared);
        if (!genericType && !BUDGET_FILE_MEDIA_TYPES.contains(declared)) {
            throw new IllegalArgumentException(BUDGET_FILE_FORMAT_MESSAGE);
        }

        String detected = detectBudgetFileFormat(bytes);
        if (!extension.equals(detected)) {
            throw new IllegalArgumentException(BUDGET_FILE_FORMAT_MESSAGE);
        }
        return detected;
    }

    /** The format of the first bytes: "pdf", "docx" (a ZIP / OOXML package) or "". */
    private String detectBudgetFileFormat(byte[] bytes) {
        if (startsWith(bytes, PDF_HEADER)) {
            return "pdf";
        }
        if (startsWith(bytes, ZIP_HEADER)) {
            return "docx";
        }
        return "";
    }

    private boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (bytes[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Stores the bytes under a generated name inside the uploads directory and
     * returns the path relative to it - the value the authorized download route
     * ({@code GET /api/files?path=...}) expects.
     */
    private String writeBudgetFile(byte[] bytes, String extension) {
        try {
            Path uploadPath = Paths.get(UPLOAD_ROOT, BUDGET_FILE_DIR);
            Files.createDirectories(uploadPath);
            Path target = uploadPath.resolve(UUID.randomUUID() + "_line-item-budget." + extension);
            Files.write(target, bytes);
            return BUDGET_FILE_DIR + "/" + target.getFileName();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "The Line-Item Budget file could not be stored.");
        }
    }

    /**
     * The proponent's own file name, kept for display only. Any directory part is
     * stripped and the length is capped to the column size, so a crafted upload can
     * never store a path-like value.
     */
    private String budgetFileDisplayName(MultipartFile file, String extension) {
        String fallback = "line-item-budget." + extension;
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return fallback;
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        if (name.isEmpty()) {
            return fallback;
        }
        return truncate(name, 255);
    }

    /**
     * Removes the file a previous upload of this proposal stored. Only paths inside
     * the Line-Item Budget upload directory are touched, and a missing file (or a
     * leftover path from before this feature) is not an error.
     */
    private void deleteStoredBudgetFile(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }
        Path uploadRoot = Paths.get(UPLOAD_ROOT, BUDGET_FILE_DIR).toAbsolutePath().normalize();
        // Stored paths are relative to the uploads directory ("budget-files/x.pdf").
        Path resolved = Paths.get(UPLOAD_ROOT).resolve(storedPath).toAbsolutePath().normalize();
        if (!resolved.startsWith(uploadRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(resolved);
        } catch (IOException ignored) {
            // An orphaned file is not worth failing an otherwise successful upload for.
        }
    }

    private void copyReusableContent(Proposal source, Proposal copy) {
        copy.setProgramTitle(source.getProgramTitle());
        copy.setProjectTitle(source.getProjectTitle());
        copy.setProjectLeader(source.getProjectLeader());
        copy.setProjectLeaderSex(source.getProjectLeaderSex());
        copy.setDuration(source.getDuration());
        copy.setStartDate(source.getStartDate());
        copy.setEndDate(source.getEndDate());
        copy.setCollege(source.getCollege());
        copy.setAddress(source.getAddress());
        copy.setAddressCountry(source.getAddressCountry());
        copy.setAddressRegion(source.getAddressRegion());
        copy.setAddressProvince(source.getAddressProvince());
        copy.setAddressDistrict(source.getAddressDistrict());
        copy.setAddressMunicipality(source.getAddressMunicipality());
        copy.setAddressBarangay(source.getAddressBarangay());
        copy.setContactType(source.getContactType());
        copy.setContactValue(source.getContactValue());
        copy.setCooperatingAgencies(source.getCooperatingAgencies());
        copy.setResearchType(source.getResearchType());
        copy.setInnovationGoals(source.getInnovationGoals());
        copy.setSectorRelevance(source.getSectorRelevance());
        copy.setSdg(source.getSdg() != null ? new ArrayList<>(source.getSdg()) : new ArrayList<>());
        copy.setExecutiveSummary(source.getExecutiveSummary());
        copy.setRationale(source.getRationale());
        copy.setFramework(source.getFramework());
        copy.setObjectivesGeneral(source.getObjectivesGeneral());
        copy.setObjectivesSpecific(source.getObjectivesSpecific());
        copy.setReview(source.getReview());
        copy.setMethodology(source.getMethodology());
        copy.setTechnologyTrl(source.getTechnologyTrl());
        copy.setOutputs(source.getOutputs());
        copy.setOutcomes(source.getOutcomes());
        copy.setImpactEconomic(source.getImpactEconomic());
        copy.setImpactSocial(source.getImpactSocial());
        copy.setBeneficiaries(source.getBeneficiaries());
        copy.setSustainability(source.getSustainability());
        copy.setGadScore(source.getGadScore());
        copy.setRisks(source.getRisks());
        copy.setLimitationsText(source.getLimitationsText());
        copy.setReferencesText(source.getReferencesText());
        copy.setReferencesJson(source.getReferencesJson());
        copy.setOtherProjectsNumber(source.getOtherProjectsNumber());
    }

    /**
     * Deep-copies the reusable child collections onto the duplicate. Every row is a
     * brand-new entity bound to the copy, so editing the duplicate can never mutate
     * the original proposal's related records.
     */
    private void copyChildCollections(Proposal source, Proposal copy) {
        for (ProposalSite s : source.getSites()) {
            copy.getSites().add(new ProposalSite(null, copy, s.getCountry(), s.getRegion(),
                    s.getProvince(), s.getDistrict(), s.getMunicipality(), s.getBarangay()));
        }
        for (ProposalLogFrame lf : source.getLogFrames()) {
            copy.getLogFrames().add(new ProposalLogFrame(null, copy, lf.getOutcome(), lf.getOutput()));
        }
        for (ProposalPersonnel p : source.getPersonnel()) {
            copy.getPersonnel().add(new ProposalPersonnel(null, copy, p.getPosition(),
                    p.getTimeDevoted(), p.getResponsibilities()));
        }
        for (ProposalBudget b : source.getBudget()) {
            copy.getBudget().add(new ProposalBudget(null, copy, b.getAgency(), b.getPs(),
                    b.getMooe(), b.getEo(), b.getTotal()));
        }
        for (ProposalOtherProject op : source.getOtherProjects()) {
            copy.getOtherProjects().add(new ProposalOtherProject(null, copy, op.getTitle(),
                    op.getAgency(), op.getInvolvement()));
        }
        for (ProposalPriorityAgenda pa : source.getPriorityAgendas()) {
            copy.getPriorityAgendas().add(new ProposalPriorityAgenda(null, copy, pa.getAgendaKey(),
                    pa.getSelected(), pa.getValue()));
        }
        for (ProposalLimitation l : source.getLimitations()) {
            copy.getLimitations().add(new ProposalLimitation(null, copy, l.getLimitation(),
                    l.getRemarks()));
        }
    }

    private void notifySubscribedRoles(Proposal proposal, String message) {
        List<User> allUsers = userRepository.findAll();
        String status = proposal.getStatus();

        for (User user : allUsers) {
            String role = user.getRole();

            switch (status) {
                // Newly submitted proposal -> notify RPS staff/admins to check
                case "SUBMITTED":
                    if ("RPS_ADMIN".equals(role) || "RPS_STAFF".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "New proposal submitted for review: \"" + proposal.getProjectTitle() + "\"",
                                "New Proposal Submitted",
                                "PROPOSAL_UPDATE",
                                proposal.getId());
                    }
                    break;

                // RPS endorsed it to OVCRIGE -> notify OVCRIGE to review & coordinate
                case "ENDORSED":
                    if ("OVCRIGE".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "A proposal has been endorsed to OVCRIGE: \"" + proposal.getProjectTitle() + "\"",
                                "Proposal Endorsed",
                                "ENDORSEMENT",
                                proposal.getId());
                    }
                    break;

                // OVCRIGE forwarded to REC -> notify REC to conduct technical/merit evaluation
                case "UNDER_REVIEW":
                    if ("REC".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "Proposal awaiting REC review and evaluation: \"" + proposal.getProjectTitle() + "\"",
                                "Review Request",
                                "REVIEW_REQUEST",
                                proposal.getId());
                    }
                    break;

                // REC approved & recommended -> notify OVCRIGE to prepare chancellor final
                // approval list
                case "REC_APPROVED":
                    if ("OVCRIGE".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "REC has evaluated and recommended proposal for approval: \""
                                        + proposal.getProjectTitle() + "\"",
                                "Proposal Approved by REC",
                                "APPROVAL",
                                proposal.getId());
                    }
                    break;

                // OVCRIGE forwarded to OVCAF -> notify OVCAF to review & endorse
                case "FOR_OVCAF_APPROVAL":
                    if ("OVCAF".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "A proposal is ready for your review and endorsement: \"" + proposal.getProjectTitle()
                                        + "\"",
                                "Proposal for OVCAF Review",
                                "ENDORSEMENT",
                                proposal.getId());
                    }
                    break;

                // OVCAF endorsed to OC -> notify OC (Chancellor) for final approval
                case "FOR_OC_APPROVAL":
                    if ("OC".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "A proposal is ready for your final approval: \"" + proposal.getProjectTitle() + "\"",
                                "Final Approval Required",
                                "APPROVAL",
                                proposal.getId());
                    }
                    break;

                // Returned by a reviewing office -> notify RPS to review and forward.
                case "RETURNED_TO_RPS":
                    if ("RPS_ADMIN".equals(role) || "RPS_STAFF".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "A proposal was returned for revision and awaits your review and forwarding: \""
                                        + proposal.getProjectTitle() + "\"",
                                "Revision Request Received",
                                "REVISION",
                                proposal.getId());
                    }
                    break;

                // REC returned for revision -> notify proponent to revise
                case "REC_REVISION":
                case "REVISION":
                    // RPS returned for revision -> notify proponent to revise
                case "RPS_RETURNED":
                    if (proposal.getProponent() != null && proposal.getProponent().getId().equals(user.getId())) {
                        notificationService.createNotification(
                                user.getId(),
                                "Your proposal \"" + proposal.getProjectTitle()
                                        + "\" has been returned for revision. Please check feedback.",
                                "Revision Required",
                                "REVISION",
                                proposal.getId());
                    }
                    break;

                // REC rejected or Chancellor rejected -> notify proponent
                case "REC_REJECTED":
                case "REJECTED":
                    if (proposal.getProponent() != null && proposal.getProponent().getId().equals(user.getId())) {
                        notificationService.createNotification(
                                user.getId(),
                                "Your proposal \"" + proposal.getProjectTitle() + "\" has been rejected.",
                                "Proposal Rejected",
                                "REJECTION",
                                proposal.getId());
                    }
                    break;

                // Final approved by Chancellor -> notify proponent & OVCAF (Finance) to
                // authorize budget
                case "APPROVED":
                    if (proposal.getProponent() != null && proposal.getProponent().getId().equals(user.getId())) {
                        notificationService.createNotification(
                                user.getId(),
                                "Congratulations! Your proposal \"" + proposal.getProjectTitle()
                                        + "\" has received final approval by the Chancellor.",
                                "Proposal Approved",
                                "APPROVAL",
                                proposal.getId());
                    } else if ("OVCAF".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "A proposal is approved and ready for budget endorsement: \""
                                        + proposal.getProjectTitle() + "\"",
                                "Budget Endorsement Required",
                                "FUNDING",
                                proposal.getId());
                    }
                    break;

                // Budget ready for release -> notify OVCAF (Finance)
                case "READY_FOR_RELEASE":
                    if ("OVCAF".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "Budget processing requested for proposal: \"" + proposal.getProjectTitle() + "\"",
                                "Budget Processing Request",
                                "FUNDING",
                                proposal.getId());
                    }
                    break;

                // Funds released -> notify proponent & RPS (RII/DARES) to monitor
                // implementation
                case "RELEASED":
                    if (proposal.getProponent() != null && proposal.getProponent().getId().equals(user.getId())) {
                        notificationService.createNotification(
                                user.getId(),
                                "Implementation funds have been successfully released for your project: \""
                                        + proposal.getProjectTitle() + "\"",
                                "Funds Released",
                                "FUNDING",
                                proposal.getId());
                    } else if ("RPS_ADMIN".equals(role) || "RPS_STAFF".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "Funds released. Ready for implementation monitoring: \"" + proposal.getProjectTitle()
                                        + "\"",
                                "Implementation Monitoring",
                                "PROPOSAL_UPDATE",
                                proposal.getId());
                    }
                    break;

                // Returned by OVCAF -> notify proponent & RPS
                case "RETURNED":
                    if (proposal.getProponent() != null && proposal.getProponent().getId().equals(user.getId())) {
                        notificationService.createNotification(
                                user.getId(),
                                "Budget release request for your proposal \"" + proposal.getProjectTitle()
                                        + "\" was returned.",
                                "Budget Request Returned",
                                "REVISION",
                                proposal.getId());
                    } else if ("RPS_ADMIN".equals(role) || "RPS_STAFF".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "Budget release was returned by Finance Office for proposal: \""
                                        + proposal.getProjectTitle() + "\"",
                                "Budget Request Returned",
                                "REVISION",
                                proposal.getId());
                    }
                    break;
            }
        }
    }
}
