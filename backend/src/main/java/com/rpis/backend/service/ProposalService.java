package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.dto.SectionRemarkDTO;
import com.rpis.backend.model.*;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * Project durations (in months) a proponent may select for an internally funded
     * MSUN project. The RPIS only handles internally funded proposals, so every new
     * proposal and draft must use one of these values.
     */
    public static final Set<String> SUPPORTED_DURATIONS = Set.of("12", "24", "36");

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
        ProposalAnnouncement activeAnnouncement = null;
        if ((isNewProposal || isSubmission) && !isRevisionResubmission) {
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
        proposal.setAddress(request.getAddress());
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

        // References formatting as text
        if (request.getReferences() != null) {
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

        // Priority Agendas
        if (request.getPriorityAgenda() != null) {
            for (Map.Entry<String, ProposalRequest.PriorityAgendaDTO> entry : request.getPriorityAgenda().entrySet()) {
                ProposalRequest.PriorityAgendaDTO val = entry.getValue();
                ProposalPriorityAgenda agenda = new ProposalPriorityAgenda(null, proposal, entry.getKey(),
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
        proposalRepository.delete(proposal);
    }

    /**
     * Statuses whose proposal content may be reused as the starting point for a new
     * proposal. Only proposals that already completed the review/approval lifecycle
     * are eligible, so the duplicate action can never be used to bypass the normal
     * validation, review or submission process.
     */
    public static final Set<String> DUPLICABLE_STATUSES = Set.of(
            "APPROVED", "IMPLEMENTATION", "RELEASED", "COMPLETED");

    /** True when the given proposal status allows creating a duplicate draft from it. */
    public static boolean isDuplicableStatus(String status) {
        return status != null && DUPLICABLE_STATUSES.contains(status.toUpperCase());
    }

    /**
     * Creates a brand-new DRAFT proposal by reusing the reusable content of an
     * eligible (completed/approved) proposal.
     * <p>
     * The duplicate is an independent record: it receives its own proposal code and
     * document id from the existing sequence allocation, its child rows are fresh
     * copies bound to the new proposal, and none of the original proposal's workflow
     * state (status, revision metadata, review remarks, approval/SO data, reports,
     * notifications or submission history) is carried over.
     *
     * @param sourceId id of the proposal to copy
     * @return the newly created DRAFT proposal
     */
    @Transactional
    public Proposal duplicateProposal(Long sourceId) {
        Proposal source = getProposalById(sourceId);
        // Ownership is enforced server-side: a Proponent can never duplicate a
        // proposal they are not authorized to access by manipulating the request.
        currentUser.requireAccessTo(source);

        if (!isDuplicableStatus(source.getStatus())) {
            throw new IllegalStateException(
                    "Only completed or approved proposals can be duplicated. Current status: "
                            + source.getStatus());
        }

        // A proposal whose proponent record was deleted has no owner, so a duplicate
        // could never be reached by anyone. Fail instead of creating an orphan record.
        if (source.getProponent() == null) {
            throw new IllegalStateException(
                    "This proposal has no proponent on record and cannot be duplicated.");
        }

        Proposal copy = new Proposal();
        // The duplicate belongs to the same proponent so it stays in their workspace.
        copy.setProponent(source.getProponent());
        // A duplicate is a private draft: it is not attached to a submission window and
        // only becomes part of an announcement when the proponent actually submits it,
        // which keeps the normal gatekeeper/validation flow in force.
        copy.setProposalAnnouncementId(null);
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
        return proposalRepository.save(copy);
    }

    /**
     * Copies only the content fields that are meaningful to reuse. Workflow fields
     * (status, identifiers, revision/SO/approval metadata, remarks) are intentionally
     * excluded and set explicitly by {@link #duplicateProposal(Long)}.
     */
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
        copy.setReviewFileName(source.getReviewFileName());
        copy.setMethodology(source.getMethodology());
        copy.setRoadmapFileName(source.getRoadmapFileName());
        copy.setTechnologyTrl(source.getTechnologyTrl());
        copy.setOutputs(source.getOutputs());
        copy.setOutcomes(source.getOutcomes());
        copy.setImpactEconomic(source.getImpactEconomic());
        copy.setImpactSocial(source.getImpactSocial());
        copy.setBeneficiaries(source.getBeneficiaries());
        copy.setBeneficiariesFileName(source.getBeneficiariesFileName());
        copy.setSustainability(source.getSustainability());
        copy.setGadScore(source.getGadScore());
        copy.setGadFileName(source.getGadFileName());
        copy.setRisks(source.getRisks());
        copy.setLimitationsText(source.getLimitationsText());
        copy.setReferencesText(source.getReferencesText());
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
