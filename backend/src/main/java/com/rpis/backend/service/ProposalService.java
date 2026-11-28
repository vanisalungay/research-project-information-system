package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.dto.SectionRemarkDTO;
import com.rpis.backend.model.*;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.storage.UploadStorage;
import com.rpis.backend.util.NotificationEvent;
import com.rpis.backend.util.NotificationTypes;
import com.rpis.backend.util.PersonNameFormatter;
import com.rpis.backend.util.ProposalStatus;
import com.lowagie.text.pdf.PdfReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ProposalNotificationPolicy proposalNotificationPolicy;
    private final ProposalAnnouncementService proposalAnnouncementService;
    private final ProposalVersionService proposalVersionService;
    private final ProposalSectionRemarkService proposalSectionRemarkService;
    private final CurrentUser currentUser;
    private final CooperatingAgencyService cooperatingAgencyService;
    private final PriorityAgendaService priorityAgendaService;
    private final UploadStorage uploadStorage;

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
    // ENDORSEMENT DOCUMENT (Certification & Endorsement step - "Endorsed By")
    // ========================
    // The Dean's signed endorsement is uploaded as a PDF. The same limits the form
    // enforces in the browser are enforced again here, so a request that bypasses the
    // form cannot store an oversized document or an image that merely carries a .pdf
    // name, and the document is opened as well, so a corrupted PDF is refused too.

    /** Largest accepted endorsement document: 10 MB (10 * 1024 * 1024 bytes). */
    public static final long ENDORSEMENT_DOCUMENT_MAX_BYTES = 10L * 1024 * 1024;

    /** Media type an endorsement document may declare. */
    private static final Set<String> ENDORSEMENT_DOCUMENT_MEDIA_TYPES = Set.of("application/pdf");

    /** Messages shown by the form as well, so both layers report the same reason. */
    public static final String ENDORSEMENT_DOCUMENT_SIZE_MESSAGE =
            "File size exceeds the 10 MB limit. Please upload a smaller PDF file.";
    public static final String ENDORSEMENT_DOCUMENT_FORMAT_MESSAGE =
            "Invalid file format. Please upload the endorsement document as a PDF (.pdf) file.";
    public static final String ENDORSEMENT_DOCUMENT_EMPTY_MESSAGE =
            "Please choose the endorsement document (PDF) first.";

    /** Folder the endorsement documents are stored in, inside the uploads root. */
    private static final String ENDORSEMENT_DOCUMENT_DIR = "endorsement-documents";

    // ========================
    // SUBMITTED BY SIGNATURE (Certification & Endorsement step)
    // ========================
    // The proponent's signature shown in the "Submitted By" section is kept with the
    // proposal (not only on the account), so a reopened draft and a reviewer both see the
    // exact signature that was submitted. A PNG/JPEG image or a PDF is accepted.

    /** Largest accepted submitted signature: 10 MB (10 * 1024 * 1024 bytes). */
    public static final long SUBMITTED_BY_SIGNATURE_MAX_BYTES = 10L * 1024 * 1024;

    /** Raster formats accepted for the submitted signature (ImageIO format names). */
    private static final Set<String> SUBMITTED_BY_SIGNATURE_IMAGE_FORMATS = Set.of("png", "jpeg");

    /** Messages shown by the form as well, so both layers report the same reason. */
    public static final String SUBMITTED_BY_SIGNATURE_SIZE_MESSAGE =
            "File size exceeds the 10 MB limit. Please upload a smaller signature file.";
    public static final String SUBMITTED_BY_SIGNATURE_FORMAT_MESSAGE =
            "Invalid file format. Please upload a PNG, JPEG or PDF file.";
    public static final String SUBMITTED_BY_SIGNATURE_EMPTY_MESSAGE =
            "Please choose the signature file first.";

    /** Folder the submitted signatures are stored in, inside the uploads root. */
    private static final String SUBMITTED_BY_SIGNATURE_DIR = "submitted-by-signatures";

    // ========================
    // OC FINAL APPROVAL AND SPECIAL ORDER (Stage 1 of the OC -> OVCAF workflow)
    // ========================
    // The Office of the Chancellor records the issued Special Order (SO) and routes
    // the proposal to OVCAF. The upload is validated here - size, extension, declared
    // media type and the real bytes - so a renamed file or an oversized scan is
    // refused with a readable message instead of being stored or cut off by the
    // container. The document itself is optional (historic proposals carry only an SO
    // number); when one is supplied it is kept with the proposal.

    /** Folder the issued Special Order documents are stored in, inside the uploads root. */
    private static final String SPECIAL_ORDER_DIR = "special-orders";

    /** Document formats accepted for an issued Special Order. */
    private static final Set<String> SPECIAL_ORDER_FORMATS = Set.of("pdf", "doc", "docx");

    /** Media types an SO upload may declare for those formats. */
    private static final Set<String> SPECIAL_ORDER_MEDIA_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/zip",
            "application/x-zip-compressed");

    /** The OLE2 compound-document header shared by legacy Word (.doc) files. */
    private static final byte[] DOC_HEADER = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1 };

    /**
     * Largest accepted Special Order document, in megabytes. It matches the 10 MB
     * ceiling of the other form attachments and the size shown beside the upload
     * control; every deployment can override it without a code change. It is
     * deliberately below the servlet's multipart ceiling
     * ({@code spring.servlet.multipart.max-file-size}) so a file between this limit
     * and the container ceiling is answered here with a readable 400 instead of an
     * opaque 413.
     */
    @Value("${rpis.special-order.max-size-mb:10}")
    private long specialOrderMaxSizeMb = 10;

    public static final String SPECIAL_ORDER_NUMBER_REQUIRED_MESSAGE =
            "A Special Order (SO) number is required to approve this proposal.";
    public static final String SPECIAL_ORDER_NUMBER_TOO_LONG_MESSAGE =
            "The Special Order number must be 100 characters or fewer.";
    public static final String SPECIAL_ORDER_BUDGET_OPTION_REQUIRED_MESSAGE =
            "Select whether the proposal requires budget before approving it.";
    public static final String SPECIAL_ORDER_FORMAT_MESSAGE =
            "Invalid file format. Please upload the Special Order as a PDF, DOC or DOCX file.";
    public static final String SPECIAL_ORDER_EMPTY_MESSAGE =
            "The uploaded Special Order document is empty. Please choose a valid file.";
    public static final String SPECIAL_ORDER_NOT_AT_OC_MESSAGE =
            "Only a proposal awaiting the Chancellor's final approval can be approved. "
                    + "The proposal must be at FOR_OC_APPROVAL.";
    public static final String SPECIAL_ORDER_DRAFT_EMPTY_MESSAGE =
            "Enter the SO number or attach the SO document before saving a draft.";
    public static final String RETURN_TO_ORPS_NOT_AT_OC_MESSAGE =
            "Only a proposal awaiting the Chancellor's final approval can be returned to ORPS. "
                    + "The proposal must be at FOR_OC_APPROVAL.";

    // ========================
    // PERSONNEL "% TIME DEVOTED" (section 24 of the Create Proposal form)
    // ========================
    // The column holds a percentage. The form only accepts a number and shows the %
    // symbol beside the field, and the stored value is always the plain number between
    // 0 and 100 - so the symbol can never be stored twice, and a legacy "50 %" is
    // normalized instead of being kept as a string.

    /** Accepted range of the personnel percentage. */
    public static final int TIME_DEVOTED_MIN = 0;
    public static final int TIME_DEVOTED_MAX = 100;

    /** Message shown by the form as well, so both layers report the same reason. */
    public static final String TIME_DEVOTED_MESSAGE =
            "% Time Devoted must be a number between 0 and 100.";

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
        return getProposalsForCurrentUser(requestedProponentId, status, statusIn, (String) null);
    }

    /** Backwards-compatible shorthand for the original submission-date ordering. */
    public List<Proposal> getProposalsForCurrentUser(Long requestedProponentId, String status,
            List<String> statusIn, boolean sortBySubmittedAt) {
        return getProposalsForCurrentUser(requestedProponentId, status, statusIn,
                sortBySubmittedAt ? "submittedAt" : null);
    }

    /**
     * As above, optionally ordered by the workflow event date the caller names.
     * <p>
     * Sorting is server-side so every client gets the same order regardless of how
     * much of the list it fetched. The order is newest event first; records with no
     * date for that event sort last, and the proposal id (descending) is the stable
     * tie-breaker. It is opt-in (`sort=<field>`) so other callers keep the
     * repository's default order.
     */
    public List<Proposal> getProposalsForCurrentUser(Long requestedProponentId, String status,
            List<String> statusIn, String sort) {
        List<Proposal> proposals = new ArrayList<>(
                loadProposalsForCurrentUser(requestedProponentId, status, statusIn));
        Comparator<Proposal> ordering = orderingFor(sort);
        if (ordering != null) {
            proposals.sort(ordering);
        }
        return proposals;
    }

    /**
     * One page of the caller's authorized proposals, with the same search/status rules as
     * the list screens. The legacy {@code GET /api/proposals} array endpoint is untouched
     * for callers that still need the complete list; paginated screens use this method so
     * a large dataset is not transferred only to be sliced in the browser.
     */
    public com.rpis.backend.dto.ProposalPageResponse getProposalPage(Long requestedProponentId,
            String status, List<String> statusIn, String sort, String search, int page, int size) {
        List<Proposal> proposals = new ArrayList<>(
                getProposalsForCurrentUser(requestedProponentId, null, statusIn, sort));

        String exactStatus = ProposalStatus.normalize(status);
        if (exactStatus != null) {
            proposals = proposals.stream()
                    .filter(proposal -> exactStatus.equals(ProposalStatus.normalize(proposal.getStatus())))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        String keyword = search == null ? null : search.trim().toLowerCase(java.util.Locale.ROOT);
        if (keyword != null && !keyword.isEmpty()) {
            proposals = proposals.stream()
                    .filter(proposal -> matchesListSearch(proposal, keyword))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        int safeSize = Math.min(Math.max(size, 1), 100);
        int totalItems = proposals.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) safeSize));
        int safePage = Math.min(Math.max(page, 1), totalPages);
        int from = Math.min((safePage - 1) * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);

        com.rpis.backend.dto.ProposalPageResponse response =
                new com.rpis.backend.dto.ProposalPageResponse();
        response.setItems(new ArrayList<>(proposals.subList(from, to)));
        response.setPage(safePage);
        response.setSize(safeSize);
        response.setTotalItems(totalItems);
        response.setTotalPages(totalPages);
        return response;
    }

    private static boolean matchesListSearch(Proposal proposal, String keyword) {
        return containsIgnoreCase(proposal.getDocumentId(), keyword)
                || containsIgnoreCase(proposal.getProposalCode(), keyword)
                || containsIgnoreCase(proposal.getProjectTitle(), keyword)
                || containsIgnoreCase(proposal.getProjectLeader(), keyword)
                || containsIgnoreCase(proposal.getProgramTitle(), keyword);
    }

    private static boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(java.util.Locale.ROOT).contains(keyword);
    }

    /**
     * The supported list orderings, each keyed by the actual event timestamp it sorts by:
     * submission, RPS endorsement, the reviewing office's return, RPS's forward to the
     * proponent, the latest workflow change, or creation. An unrecognised value is ignored
     * instead of failing the list request.
     */
    private static Comparator<Proposal> orderingFor(String sort) {
        if (sort == null) {
            return null;
        }
        return switch (sort.trim()) {
            case "submittedAt" -> newestFirst(Proposal::getSubmittedAt);
            case "endorsedAt" -> newestFirst(Proposal::getEndorsedAt);
            case "returnedAt" -> newestFirst(Proposal::getReturnedAt);
            case "revisionForwardedAt" -> newestFirst(Proposal::getRevisionForwardedAt);
            case "updatedAt" -> newestFirst(Proposal::getUpdatedAt);
            case "createdAt" -> newestFirst(Proposal::getCreatedAt);
            default -> null;
        };
    }

    /**
     * Newest timestamp first; records with no timestamp sort last; the proposal id
     * (descending) breaks ties so equal timestamps keep a stable order.
     */
    private static Comparator<Proposal> newestFirst(
            java.util.function.Function<Proposal, LocalDateTime> timestamp) {
        return Comparator
                .comparing(timestamp, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Proposal::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private List<Proposal> loadProposalsForCurrentUser(Long requestedProponentId, String status,
            List<String> statusIn) {
        String normalizedStatus = ProposalStatus.normalize(status);
        List<String> normalizedStatuses = ProposalStatus.withForReviewAliases(statusIn);
        // A filter for either submission spelling must return both the current and the
        // legacy rows, so it is served by a statusIn query instead of an exact match.
        if (normalizedStatus != null && ProposalStatus.isForReview(normalizedStatus)) {
            normalizedStatuses = ProposalStatus.withForReviewAliases(List.of(normalizedStatus));
            normalizedStatus = null;
        }

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
            // A save on a proposal that is currently under revision is only a
            // resubmission when the request actually carries a submission status. A
            // draft save (Save Draft, or any request without a submission status) stores
            // the edits and leaves the revision state, its revision number and its
            // deadline untouched, so the Proponent can keep editing before submitting.
            if (proposalVersionService.isRevisionStatus(proposal.getStatus())
                    && ProposalStatus.isSubmissionRequest(request.getStatus())) {
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

        // The status the proposal had before this save. Notifications are raised only for
        // a real transition, so re-saving an already submitted draft (an attachment, a
        // late field edit) cannot send the "submitted" or any later notification twice.
        String previousStatus = ProposalStatus.normalize(proposal.getStatus());

        // Validate the requested status after the access/ownership checks but before any
        // write. An unsupported value (for example a typo sent by a client or integration)
        // is a request error, so it fails with the controller's readable 400 instead of
        // reaching PostgreSQL and appearing as a status foreign-key violation. Both
        // submission spellings are accepted and stored as FOR REVIEW.
        String requestedStatus = ProposalStatus.toStoredStatus(request.getStatus());
        if (requestedStatus != null) {
            ProposalStatus.requireSupportedStatus(requestedStatus);
        }

        // Proposal Announcement gatekeeper (server-side enforcement): new proposals and
        // final submissions are only accepted while a proposal announcement is active.
        // Revision resubmissions belong to the review lifecycle of an already
        // submitted proposal and remain allowed outside an active announcement.
        boolean isSubmission = ProposalStatus.isSubmissionRequest(request.getStatus());

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
        // Person names are stored in the one RPIS format (see PersonNameFormatter), so a
        // client that bypasses the browser cannot save an all-uppercase or lowercase name.
        proposal.setProjectLeader(PersonNameFormatter.format(request.getProjectLeader()));
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
        // Cooperating agencies: the legacy text is only written when the request carries
        // it. A null value means "this client does not edit the section" and preserves the
        // stored text; an explicitly sent empty string still clears it.
        if (request.getCooperatingAgencies() != null) {
            proposal.setCooperatingAgencies(request.getCooperatingAgencies());
        }
        proposal.setResearchType(request.getResearchType());
        proposal.setInnovationGoals(request.getInnovationGoals());
        proposal.setSectorRelevance(request.getSectorRelevance());
        // Only when sent: a partial save must not wipe the stored SDGs. An explicitly
        // empty list still clears them.
        if (request.getSdg() != null) {
            proposal.setSdg(request.getSdg());
        }
        proposal.setExecutiveSummary(request.getExecutiveSummary());
        proposal.setRationale(request.getRationale());
        proposal.setFramework(request.getFramework());
        proposal.setObjectivesGeneral(request.getObjectivesGeneral());
        proposal.setObjectivesSpecific(request.getObjectivesSpecific());
        proposal.setReview(request.getReview());
        // Uploaded-document names: null means "this save does not mention the document"
        // and preserves the stored reference; an explicit '' is the revision form's
        // Remove action and clears it. The bytes and stored paths are managed by the
        // dedicated upload endpoints, never by this DTO.
        if (request.getReviewFileName() != null) {
            proposal.setReviewFileName(request.getReviewFileName());
        }
        proposal.setMethodology(request.getMethodology());
        if (request.getRoadmapFileName() != null) {
            proposal.setRoadmapFileName(request.getRoadmapFileName());
        }
        proposal.setTechnologyTrl(request.getTechnologyTrl());
        proposal.setOutputs(request.getOutputs());
        proposal.setOutcomes(request.getOutcomes());
        proposal.setImpactEconomic(request.getImpactEconomic());
        proposal.setImpactSocial(request.getImpactSocial());
        proposal.setBeneficiaries(request.getBeneficiaries());
        if (request.getBeneficiariesFileName() != null) {
            proposal.setBeneficiariesFileName(request.getBeneficiariesFileName());
        }
        // The endorsement document of the Certification & Endorsement step ("Endorsed
        // By"): only its display name travels in the request, and only when the client
        // sends it, so a save that does not mention the document never blanks a stored
        // one (the bytes and the stored path are set by #attachEndorsementDocument).
        if (request.getEndorsementFileName() != null) {
            proposal.setEndorsementFileName(request.getEndorsementFileName());
        }
        proposal.setSustainability(request.getSustainability());

        // "Submitted By" / "Endorsed By" of the Certification & Endorsement step. Each
        // value is only written when the request carries it, so a partial save (the
        // revision form, an integration) preserves what was already stored; an explicit
        // empty string still clears a field the proponent emptied.
        if (request.getSubmittedByName() != null) {
            proposal.setSubmittedByName(PersonNameFormatter.format(request.getSubmittedByName()));
        }
        if (request.getSubmittedByDesignation() != null) {
            proposal.setSubmittedByDesignation(request.getSubmittedByDesignation());
        }
        if (request.getSubmittedByDate() != null) {
            proposal.setSubmittedByDate(request.getSubmittedByDate());
        }
        if (request.getEndorsedByName() != null) {
            proposal.setEndorsedByName(PersonNameFormatter.format(request.getEndorsedByName()));
        }
        if (request.getEndorsedByDesignation() != null) {
            proposal.setEndorsedByDesignation(request.getEndorsedByDesignation());
        }
        if (request.getEndorsedByDate() != null) {
            proposal.setEndorsedByDate(request.getEndorsedByDate());
        }

        // Section 19 - the interactive GAD Checklist 2. Only when the client sends one (a
        // legacy client that only sends the file leaves the stored checklist untouched). The
        // total, interpretation and completion state are RECALCULATED here from the responses,
        // never trusted from the client, and the total is mirrored onto the legacy gadScore
        // column so the existing screens and exports keep working. A checklist submitted with
        // a non-draft status must be complete.
        GadChecklistData incomingChecklist = request.getGadChecklist();
        boolean hadStoredChecklist = proposal.getGadChecklist() != null;
        if (incomingChecklist != null) {
            GadChecklistScoring.recalculate(incomingChecklist);
            boolean draft = request.getStatus() == null || "DRAFT".equalsIgnoreCase(request.getStatus());
            if (!draft && !Boolean.TRUE.equals(incomingChecklist.getCompleted())) {
                throw new IllegalArgumentException(
                        "Please select No, Partly or Yes for every GAD assessment item before submitting the proposal.");
            }
            proposal.setGadChecklist(incomingChecklist);
            // Mirror the derived total only when the checklist is actually in use: a legacy
            // proposal that only has a stored GAD score/file must not be zeroed by an empty
            // checklist object, while a checklist the proponent clears on purpose (it was
            // stored before) does update the score.
            boolean hasResponses = incomingChecklist.getResponses() != null
                    && incomingChecklist.getResponses().values().stream()
                            .anyMatch(value -> value != null && !value.isBlank());
            if (hasResponses || hadStoredChecklist) {
                proposal.setGadScore(incomingChecklist.getTotalScore());
            }
        } else if (request.getGadScore() != null) {
            proposal.setGadScore(request.getGadScore());
        }
        if (request.getGadFileName() != null) {
            proposal.setGadFileName(request.getGadFileName());
        }

        // Server-side GAD requirement of a final submission: a proposal that neither sends
        // nor already stores a completed checklist must carry a legacy GAD document. Old
        // proposals (legacy document) and revision resubmissions stay accepted. A proposal
        // that is already FOR REVIEW (or the legacy SUBMITTED spelling of the same state)
        // is not a new submission either - that is the state a losing thread sees when two
        // revision resubmissions race (the winner has already written the status), and
        // re-validating it there would replace the integrity violation the workflow relies
        // on with an unrelated content error.
        if (isSubmission && !isRevisionResubmission && incomingChecklist == null
                && !ProposalStatus.isForReview(proposal.getStatus())) {
            boolean storedChecklistComplete = proposal.getGadChecklist() != null
                    && Boolean.TRUE.equals(proposal.getGadChecklist().getCompleted());
            if (!storedChecklistComplete && isBlank(proposal.getGadFileName())) {
                throw new IllegalArgumentException(
                        "Please select No, Partly or Yes for every GAD assessment item before submitting the proposal.");
            }
        }
        // The legacy single-text plan is only written when the request actually carries it,
        // so a client that does not send the section (the RPS flows, an integration) can
        // never blank the stored plan. An empty string still clears it deliberately.
        if (request.getRisks() != null) {
            proposal.setRisks(request.getRisks());
        }
        // Section 21 - Risk Management Plan. The structured table is the authoritative
        // form of the plan: every complete entry is stored with its four fields kept
        // separate, and the legacy `risks` column above is filled with the readable
        // rendering of those entries so every screen, export and print view that predates
        // the table keeps working. The section is only touched when the request actually
        // carries it, so a client that does not edit it (the RPS forward flows, a legacy
        // client) can never wipe the stored rows, and an old proposal whose plan is only
        // hand-written text keeps that text as it is.
        if (request.getRiskManagement() != null) {
            List<RiskManagementEntry> riskEntries = validateRiskManagement(request.getRiskManagement(),
                    isSubmission);
            proposal.setRiskManagement(riskEntries);
            if (!riskEntries.isEmpty()) {
                proposal.setRisks(renderRiskManagement(riskEntries));
            }
        }
        proposal.setLimitationsText(request.getLimitationsText());
        proposal.setOtherProjectsNumber(request.getOtherProjectsNumber());

        // Status. Both submission spellings ("SUBMITTED" from the current client and the
        // stored "FOR REVIEW") are persisted as FOR REVIEW; every other supported status
        // is uppercased exactly as before. The value was validated above, so only a
        // workflow status can reach the entity.
        if (request.getStatus() != null) {
            if (requestedStatus != null) {
                proposal.setStatus(requestedStatus);
            }
        } else if (proposal.getStatus() == null) {
            proposal.setStatus(ProposalStatus.DRAFT);
        }

        // Record the actual submission event. It is written only when this save moves the
        // proposal into FOR REVIEW from a non-submitted state (a brand-new submission or a
        // revision resubmission), so editing an already submitted proposal, an attachment
        // upload, or a later review action never rewrites the original submission date.
        if (ProposalStatus.isForReview(proposal.getStatus())
                && !ProposalStatus.isForReview(previousStatus)) {
            proposal.setSubmittedAt(LocalDateTime.now());
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
        // without ever letting a client introduce an unknown or inactive key. The list is
        // only consulted when the request actually sends the section.
        List<String> storedAgendaKeys = request.getPriorityAgenda() != null
                ? proposal.getPriorityAgendas().stream()
                        .map(ProposalPriorityAgenda::getAgendaKey)
                        .filter(Objects::nonNull)
                        .toList()
                : List.of();

        // Clear existing child lists to overwrite (cascade all + orphan removal takes
        // care of orphans). Each list is cleared ONLY when the request carries it: a
        // client that does not edit a section (a partial save, the revision form, an
        // integration) must never delete rows it never mentioned. The limitations table
        // is handled separately below for the same reason.
        if (request.getSites() != null) {
            proposal.getSites().clear();
        }
        if (request.getLogFrame() != null) {
            proposal.getLogFrames().clear();
        }
        if (request.getPersonnel() != null) {
            proposal.getPersonnel().clear();
        }
        if (request.getBudget() != null) {
            proposal.getBudget().clear();
        }
        if (request.getOtherProjects() != null) {
            proposal.getOtherProjects().clear();
        }
        if (request.getPriorityAgenda() != null) {
            proposal.getPriorityAgendas().clear();
        }

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

        // Personnel (section 24). The "% Time Devoted" column holds a percentage, so
        // every value is normalized to the plain number the field stores - a legacy
        // "50 %" becomes "50" and a value outside 0-100 is capped - before it is written.
        // A value that is not a number at all (only a legacy record can hold one) is
        // rejected when the proposal is being submitted and kept as it is on a draft, so
        // an old draft can always still be saved.
        if (request.getPersonnel() != null) {
            int position = 0;
            for (ProposalRequest.PersonnelDTO p : request.getPersonnel()) {
                position++;
                String timeDevoted = normalizeTimeDevoted(p.getTime(), position, isSubmission);
                ProposalPersonnel personnel = new ProposalPersonnel(null, proposal, p.getPosition(), timeDevoted,
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

        // Notify only when this save actually moved the proposal to a new status. A
        // resubmission gets its own wording so it is not reported as a brand-new proposal.
        // Both submission spellings are the same workflow state.
        boolean statusChanged = requestedStatus != null
                && !requestedStatus.equals(previousStatus)
                && !(ProposalStatus.isForReview(requestedStatus)
                        && ProposalStatus.isForReview(previousStatus));
        if (statusChanged) {
            proposalNotificationPolicy.notifyForStatus(proposal, isRevisionResubmission);
        }

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
        String upper = ProposalStatus.normalize(status);
        if (upper == null) {
            throw new IllegalArgumentException("A proposal status is required.");
        }
        if (REVISION_STATUSES.contains(upper)) {
            throw new IllegalStateException(
                    "Revision statuses can only be set through the RPS revision workflow.");
        }
        // Reject an unknown status before loading or changing the proposal. The legacy
        // SUBMITTED spelling is accepted and stored as FOR REVIEW, exactly like a save.
        String storedStatus = ProposalStatus.requireSupportedStatus(upper);
        Proposal proposal = getProposalById(id);
        currentUser.requireAccessTo(proposal);
        String previousStatus = ProposalStatus.normalize(proposal.getStatus());
        if (storedStatus.equals(previousStatus)) {
            // A retried or repeated request must not create a second notification for the
            // same transition.
            return proposal;
        }
        // An implementation-approved project has already passed the financial evaluation.
        // OVCAF must not be able to move it back through the budget route with the generic
        // status endpoint; its financial decision is closed.
        if (UserRoles.OVCAF.equalsIgnoreCase(currentUser.getRole())
                && "APPROVED".equals(previousStatus)) {
            throw new IllegalStateException(
                    "This proposal is Implementation Approved; its financial evaluation is closed.");
        }
        proposal.setStatus(storedStatus);
        if ("ENDORSED".equals(storedStatus)) {
            // The generic transition is the same workflow event as the dedicated
            // endorsement endpoint, so the endorsement date is recorded here too.
            proposal.setEndorsedAt(LocalDateTime.now());
        }
        recordStatusChange(proposal, previousStatus, storedStatus, null);
        proposal = proposalRepository.save(proposal);

        // Dispatch notifications to the next user level and the proponent
        proposalNotificationPolicy.notifyForStatus(proposal);

        return proposal;
    }

    /** The roles that make up ORPS itself: they own the final rejection decision. */
    private static final Set<String> ORPS_ROLES = Set.of(
            UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF);

    /** A proposal that has already moved past evaluation can no longer be rejected. */
    private static final Set<String> NON_REJECTABLE_STATUSES = Set.of(
            "APPROVED", "RELEASED", "COMPLETED");

    /**
     * A reviewing office rejects a proposal.
     *
     * <p>Every rejection is centralized in this one method so no office can deliver a
     * rejection to the Proponent on its own:
     * <ul>
     *   <li>ORPS (RPS Admin / Staff) makes the final decision: the proposal becomes
     *       {@code REJECTED} and the Proponent is notified;</li>
     *   <li>OVCRIGE, REC, OVCAF and any other reviewing office hand the record to ORPS as
     *       {@code RETURNED_TO_RPS} with {@code return_kind='REJECTION'} and the rejecting
     *       office recorded. Only ORPS is notified; ORPS then forwards the decision
     *       through {@link #finalizeRejection(Long, String)}.</li>
     * </ul>
     *
     * <p>Remarks are mandatory. They are stored on the proposal, in the audited status
     * history and in the ORPS notification, so ORPS can review the rejection before
     * communicating it.
     */
    @Transactional
    public Proposal rejectProposal(Long proposalId, String remarks) {
        String role = currentUser.getRole();
        String office = role == null ? null : role.trim().toUpperCase(Locale.ROOT);
        String reason = remarks == null || remarks.isBlank() ? null : truncate(remarks.trim(), 2000);
        if (reason == null) {
            throw new IllegalArgumentException("Rejection remarks are required.");
        }
        if (office == null) {
            throw new IllegalStateException("A signed-in reviewing role is required to reject a proposal.");
        }
        return ORPS_ROLES.contains(office)
                ? rejectAsOrps(proposalId, reason)
                : rejectToOrpsFromOffice(proposalId, office, reason);
    }

    /** ORPS rejects a proposal outright; this is the decision the Proponent receives. */
    private Proposal rejectAsOrps(Long proposalId, String reason) {
        Proposal proposal = proposalRepository.findByIdForUpdate(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found."));
        currentUser.requireAccessTo(proposal);

        String previousStatus = ProposalStatus.normalize(proposal.getStatus());
        if ("REJECTED".equals(previousStatus)) {
            // Idempotent: a retried request must not notify the Proponent twice.
            return proposal;
        }
        if (REVISION_STATUSES.contains(previousStatus)) {
            throw new IllegalStateException(
                    "Revision statuses can only be set through the RPS revision workflow.");
        }
        if (NON_REJECTABLE_STATUSES.contains(previousStatus)) {
            throw new IllegalStateException(
                    "This proposal has already completed its evaluation and can no longer be rejected.");
        }

        proposal.setStatus("REJECTED");
        proposal.setRemarks(reason);
        recordStatusChange(proposal, previousStatus, "REJECTED", reason);

        Proposal saved = proposalRepository.save(proposal);
        proposalNotificationPolicy.notifyForStatus(saved);
        return saved;
    }

    /** A reviewing office routes its rejection to ORPS instead of notifying the Proponent. */
    private Proposal rejectToOrpsFromOffice(Long proposalId, String office, String reason) {
        Proposal proposal = proposalRepository.findByIdForUpdate(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found."));
        currentUser.requireAccessTo(proposal);

        String previousStatus = ProposalStatus.normalize(proposal.getStatus());
        boolean alreadyHandedOff = "RETURNED_TO_RPS".equals(previousStatus)
                && "REJECTION".equalsIgnoreCase(
                        proposal.getReturnKind() == null ? "" : proposal.getReturnKind().trim());
        if (alreadyHandedOff || "REJECTED".equals(previousStatus)) {
            // Idempotent: a repeated click must not create a second ORPS notification.
            return proposal;
        }
        if (NON_REJECTABLE_STATUSES.contains(previousStatus)) {
            throw new IllegalStateException(
                    "This proposal has already completed its evaluation and can no longer be rejected.");
        }

        Long actorId = currentUser.requireUserId();
        String actorName = userRepository.findById(actorId).map(User::getName).orElse(null);

        proposal.setStatus("RETURNED_TO_RPS");
        proposal.setRemarks(reason);
        proposal.setReturnRemarks(reason);
        proposal.setReturnedByOffice(office);
        proposal.setReturnKind("REJECTION");
        proposal.setReturnedByUserId(actorId);
        proposal.setReturnedByName(PersonNameFormatter.format(actorName));
        proposal.setReturnedAt(LocalDateTime.now());
        // A fresh rejection hand-off restarts the revision-forward metadata; ORPS owns
        // the next step and no deadline has been set yet.
        proposal.setRevisionDeadline(null);
        proposal.setRevisionForwardedAt(null);
        proposal.setRevisionForwardedByName(null);
        proposal.setRevisionForwardedByUserId(null);
        proposal.setRevisionNotes(null);

        recordStatusChange(proposal, previousStatus, "RETURNED_TO_RPS", reason);

        Proposal saved = proposalRepository.save(proposal);
        proposalNotificationPolicy.notifyRpsRejectionReturned(saved, reason, office);
        return saved;
    }

    /**
     * RPS endorses a submitted proposal to OVCRIGE.
     *
     * <p>The proposal row is locked for the duration of the transaction, so a repeated
     * click (or two concurrent requests) is serialized: the second request sees the
     * already stored {@code ENDORSED} status and returns without writing a second
     * notification. Only the existing submitted state ({@code FOR REVIEW}, or the legacy
     * {@code SUBMITTED} spelling) may be endorsed - any other status is refused instead of
     * silently advancing a proposal that is not awaiting RPS's decision. The notification
     * that tells OVCRIGE about the endorsement is written in the same transaction as the
     * status change, so a failure leaves neither a partial status nor a stray notification
     * behind.
     *
     * @param id the proposal the RPS has reviewed
     * @return the saved proposal, now at {@code ENDORSED}
     */
    @Transactional
    public Proposal endorseToOvcrige(Long id) {
        Proposal proposal = proposalRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + id));
        currentUser.requireAccessTo(proposal);
        String previousStatus = ProposalStatus.normalize(proposal.getStatus());
        if ("ENDORSED".equals(previousStatus)) {
            // A retried or repeated submission is a no-op, not a second transition.
            return proposal;
        }
        if (!ProposalStatus.isForReview(previousStatus)) {
            throw new IllegalArgumentException(
                    "Only a proposal submitted for RPS review can be endorsed to OVCRIGE; its current status is "
                            + (previousStatus == null ? "unknown" : previousStatus) + ".");
        }
        proposal.setStatus("ENDORSED");
        proposal.setEndorsedAt(LocalDateTime.now());
        recordStatusChange(proposal, previousStatus, "ENDORSED", null);
        Proposal saved = proposalRepository.save(proposal);
        proposalNotificationPolicy.notifyForStatus(saved);
        return saved;
    }

    /**
     * Save a proposal entity directly (used when fields are set manually on the
     * entity).
     * Also dispatches notifications based on the current status.
     *
     * <p>Note: the endorsement -> proposal association is <em>not</em> applied here.
     * It is owned by the dedicated endpoint
     * {@code PUT /api/endorsements/{id}/link} ({@code EndorsementRequestService.linkProposal}),
     * which validates the endorsement, records the audit event and updates the
     * certification. An earlier draft of this method also tried to link the
     * endorsement from the proposal payload; that code was never called and did not
     * compile (it referenced members that do not exist), so it has been removed
     * rather than duplicated here.
     */
    @Transactional
    public Proposal saveProposalDirect(Proposal proposal) {
        proposal = proposalRepository.save(proposal);
        proposalNotificationPolicy.notifyForStatus(proposal);
        return proposal;
    }

    /**
     * Stage 1 of the OC -&gt; OVCAF workflow: the Chancellor's Office records the
     * Special Order it has actually issued and hands the proposal to the next stage.
     *
     * <p>Everything that can be checked without side effects is checked first (SO
     * number, budget routing, document size/format). The proposal row is then locked
     * for the rest of the transaction and must still be at {@code FOR_OC_APPROVAL};
     * that check is what makes a retried or concurrent request harmless - it can
     * neither overwrite the first recorded SO nor advance a proposal a second time,
     * and an approval can never be recorded for a proposal that is not awaiting it.
     *
     * <p>An uploaded document is optional (records approved before document upload
     * existed carry only an SO number), but when one is supplied it is validated
     * before it is stored. The stored file is deleted again if the transaction rolls
     * back, including a commit-time failure, so a proposal can never keep a reference
     * to a file that was removed - and a failed request never leaves a stray upload
     * behind. When no document is supplied the previously recorded SO fields are
     * left untouched rather than blanked.
     *
     * @param proposalId      the proposal awaiting the Chancellor's final approval
     * @param soNumber        the SO number actually issued (required)
     * @param soFile          the issued SO document (optional)
     * @param needsBudget     whether OVCAF must still evaluate the budget (required)
     * @param chancellorNotes optional notes recorded with the approval
     * @return the saved proposal, now at {@code FOR_OVCAF_APPROVAL} or {@code APPROVED}
     */
    @Transactional
    public Proposal approveWithSpecialOrder(Long proposalId, String soNumber, MultipartFile soFile,
                                            Boolean needsBudget, String chancellorNotes) {
        return approveWithSpecialOrder(proposalId, soNumber, soFile, needsBudget, chancellorNotes, false);
    }

    /**
     * Finalizes the OC approval. {@code removeSoFile} discards an SO document saved
     * earlier as a draft, so a file the Chancellor's Office removed in the form is
     * not resurrected by the final approval.
     */
    @Transactional
    public Proposal approveWithSpecialOrder(Long proposalId, String soNumber, MultipartFile soFile,
                                            Boolean needsBudget, String chancellorNotes,
                                            boolean removeSoFile) {
        String normalisedSoNumber = soNumber == null ? "" : soNumber.trim();
        if (normalisedSoNumber.isEmpty()) {
            throw new IllegalArgumentException(SPECIAL_ORDER_NUMBER_REQUIRED_MESSAGE);
        }
        if (normalisedSoNumber.length() > 100) {
            throw new IllegalArgumentException(SPECIAL_ORDER_NUMBER_TOO_LONG_MESSAGE);
        }
        if (needsBudget == null) {
            throw new IllegalArgumentException(SPECIAL_ORDER_BUDGET_OPTION_REQUIRED_MESSAGE);
        }

        // Read and validate the document before the row lock is taken, so an invalid
        // or oversized upload never holds a database lock.
        byte[] soBytes = null;
        String soFormat = null;
        if (soFile != null && !soFile.isEmpty()) {
            if (soFile.getSize() > getSpecialOrderMaxBytes()) {
                throw new IllegalArgumentException(getSpecialOrderMaxSizeMessage());
            }
            try {
                soBytes = soFile.getBytes();
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "The Special Order document could not be read.");
            }
            soFormat = readSpecialOrderFileFormat(soFile, soBytes);
        }

        Proposal proposal = proposalRepository.findByIdForUpdate(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found."));
        String status = proposal.getStatus() == null
                ? ""
                : proposal.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!"FOR_OC_APPROVAL".equals(status)) {
            throw new IllegalStateException(SPECIAL_ORDER_NOT_AT_OC_MESSAGE);
        }

        // A document saved by an earlier SO draft, if any, is replaced or removed by
        // this approval. It is deleted only after the transaction commits.
        String previousPath = proposal.getSoFilePath();

        String storedPath = null;
        if (soBytes != null) {
            try {
                storedPath = uploadStorage.store(soBytes, SPECIAL_ORDER_DIR, "special-order." + soFormat);
            } catch (RuntimeException e) {
                // Surface the storage failure as an actionable server error; nothing has
                // been recorded on the proposal, so the previous state is intact.
                log.error("Could not store the Special Order document of proposal {}: {}",
                        proposalId, e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "The Special Order document could not be stored. Please try again.", e);
            }
            deleteStoredSpecialOrderOnRollback(storedPath);
        }

        try {
            proposal.setSoNumber(normalisedSoNumber);
            proposal.setNeedsBudget(needsBudget);
            proposal.setSoUploadedAt(LocalDateTime.now());
            if (storedPath != null) {
                proposal.setSoFileName(specialOrderDisplayName(soFile, soFormat));
                proposal.setSoFilePath(storedPath);
            } else if (removeSoFile) {
                proposal.setSoFileName(null);
                proposal.setSoFilePath(null);
            }
            String normalisedNotes = chancellorNotes != null && !chancellorNotes.isBlank()
                    ? truncate(chancellorNotes.trim(), 2000)
                    : null;
            if (normalisedNotes != null) {
                proposal.setRemarks(normalisedNotes);
            }
            // The budget routing decision is the Chancellor's: with budget the proposal
            // waits for OVCAF; without it, it is approved for immediate implementation.
            String targetStatus = needsBudget ? "FOR_OVCAF_APPROVAL" : "APPROVED";
            proposal.setStatus(targetStatus);
            // The transition is audited in the same transaction as the status change.
            recordStatusChange(proposal, "FOR_OC_APPROVAL", targetStatus, normalisedNotes);

            Proposal saved = proposalRepository.save(proposal);
            proposalNotificationPolicy.notifyForStatus(saved);
            if (previousPath != null && !previousPath.isBlank()
                    && (storedPath != null || removeSoFile)) {
                deleteStoredSpecialOrderAfterCommit(previousPath);
            }
            return saved;
        } catch (RuntimeException e) {
            // Compensates the filesystem write for a failure before commit. The rollback
            // synchronization above covers a failure during commit as well.
            if (storedPath != null) {
                uploadStorage.delete(storedPath);
            }
            throw e;
        }
    }

    /**
     * Saves the Special Order form as a draft without finalizing the approval: the
     * proposal's status is unchanged, no SO date is recorded and no notification is
     * sent. Only the fields entered in the form are written, so the form can be
     * reopened later and continue from where it was left.
     *
     * <p>At least an SO number or an SO document must be present (a budget choice or
     * notes alone cannot be distinguished from an untouched proposal). Because a
     * draft never sets {@code soUploadedAt}, the OVCAF workflow does not treat it as
     * a completed OC approval.
     *
     * @param proposalId      the proposal awaiting the Chancellor's final approval
     * @param soNumber        the SO number entered so far (optional)
     * @param soFile          an SO document to stage (optional)
     * @param needsBudget     the budget routing chosen so far (optional)
     * @param chancellorNotes notes entered so far (optional)
     * @param removeSoFile    whether a previously staged SO document must be removed
     * @return the saved proposal, still at {@code FOR_OC_APPROVAL}
     */
    @Transactional
    public Proposal saveSpecialOrderDraft(Long proposalId, String soNumber, MultipartFile soFile,
                                          Boolean needsBudget, String chancellorNotes,
                                          boolean removeSoFile) {
        String normalisedSoNumber = soNumber == null ? "" : soNumber.trim();
        if (normalisedSoNumber.length() > 100) {
            throw new IllegalArgumentException(SPECIAL_ORDER_NUMBER_TOO_LONG_MESSAGE);
        }

        // Validate the document before the row lock is taken, so an invalid or
        // oversized upload never holds a database lock.
        byte[] soBytes = null;
        String soFormat = null;
        if (soFile != null && !soFile.isEmpty()) {
            if (soFile.getSize() > getSpecialOrderMaxBytes()) {
                throw new IllegalArgumentException(getSpecialOrderMaxSizeMessage());
            }
            try {
                soBytes = soFile.getBytes();
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "The Special Order document could not be read.");
            }
            soFormat = readSpecialOrderFileFormat(soFile, soBytes);
        }

        Proposal proposal = proposalRepository.findByIdForUpdate(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found."));
        String status = proposal.getStatus() == null
                ? ""
                : proposal.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!"FOR_OC_APPROVAL".equals(status)) {
            throw new IllegalStateException(SPECIAL_ORDER_NOT_AT_OC_MESSAGE);
        }

        String previousPath = proposal.getSoFilePath();
        String previousFileName = proposal.getSoFileName();
        String previousSoNumber = proposal.getSoNumber() == null
                ? null
                : proposal.getSoNumber().trim();
        Boolean previousNeedsBudget = proposal.getNeedsBudget();
        boolean keepsStoredFile = previousPath != null && !previousPath.isBlank() && !removeSoFile;
        if (normalisedSoNumber.isEmpty() && soBytes == null && !keepsStoredFile) {
            throw new IllegalArgumentException(SPECIAL_ORDER_DRAFT_EMPTY_MESSAGE);
        }
        // Whether an earlier draft wrote the SO fields (and therefore owned `remarks`).
        // On a first save with no notes, remarks may still hold REC/return comments and
        // must not be wiped; on a later save, an empty notes box clears the draft notes.
        boolean draftExisted = (proposal.getSoNumber() != null && !proposal.getSoNumber().isBlank())
                || (previousPath != null && !previousPath.isBlank());

        String storedPath = null;
        if (soBytes != null) {
            try {
                storedPath = uploadStorage.store(soBytes, SPECIAL_ORDER_DIR, "special-order." + soFormat);
            } catch (RuntimeException e) {
                log.error("Could not store the draft Special Order document of proposal {}: {}",
                        proposalId, e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "The Special Order document could not be stored. Please try again.", e);
            }
            deleteStoredSpecialOrderOnRollback(storedPath);
        }

        try {
            String newSoNumber = normalisedSoNumber.isEmpty() ? null : normalisedSoNumber;
            // An omitted budget classification must not wipe an earlier choice; the SO
            // draft form may legitimately send only the notes or a file on a later save.
            Boolean newNeedsBudget = needsBudget != null ? needsBudget : previousNeedsBudget;
            String newFileName = previousFileName;
            if (storedPath != null) {
                newFileName = specialOrderDisplayName(soFile, soFormat);
            } else if (removeSoFile) {
                newFileName = null;
            }
            String newPath = storedPath != null ? storedPath : (removeSoFile ? null : previousPath);

            boolean numberChanged = !Objects.equals(previousSoNumber, newSoNumber);
            boolean budgetChanged = !Objects.equals(previousNeedsBudget, newNeedsBudget);
            boolean fileChanged = storedPath != null
                    || (removeSoFile && previousPath != null && !previousPath.isBlank());

            proposal.setSoNumber(newSoNumber);
            proposal.setNeedsBudget(newNeedsBudget);
            if (storedPath != null) {
                proposal.setSoFileName(newFileName);
                proposal.setSoFilePath(newPath);
            } else if (removeSoFile) {
                proposal.setSoFileName(null);
                proposal.setSoFilePath(null);
            }
            if (chancellorNotes != null && !chancellorNotes.isBlank()) {
                proposal.setRemarks(truncate(chancellorNotes.trim(), 2000));
            } else if (draftExisted) {
                proposal.setRemarks(null);
            }
            // Deliberately no soUploadedAt and no status change: the draft is not an OC
            // approval. It may still notify OVCAF that a meaningful draft changed.

            Proposal saved = proposalRepository.save(proposal);
            notifyOvcafOfMeaningfulDraftChange(saved, numberChanged, budgetChanged, fileChanged,
                    newNeedsBudget);
            if (previousPath != null && !previousPath.isBlank() && !previousPath.equals(storedPath)
                    && (storedPath != null || removeSoFile)) {
                deleteStoredSpecialOrderAfterCommit(previousPath);
            }
            return saved;
        } catch (RuntimeException e) {
            if (storedPath != null) {
                uploadStorage.delete(storedPath);
            }
            throw e;
        }
    }

    /**
     * Tells OVCAF about a Special Order draft only when something that matters changed:
     * the SO number, the document, or the budget routing. An identical re-save and a
     * notes-only edit produce nothing. OVCAF is skipped entirely when the proposal is
     * classified without a budget, because Finance has no action on it.
     */
    private void notifyOvcafOfMeaningfulDraftChange(Proposal saved, boolean numberChanged,
            boolean budgetChanged, boolean fileChanged, Boolean afterBudget) {
        if (!numberChanged && !budgetChanged && !fileChanged) {
            return;
        }
        // Only a proposal that is still routed to OVCAF needs the draft update. Switching
        // to "without budget" removes Finance from the route, so it is not a task for them.
        if (!Boolean.TRUE.equals(afterBudget)) {
            return;
        }

        String soNumber = saved.getSoNumber() == null || saved.getSoNumber().isBlank()
                ? "not set"
                : saved.getSoNumber();
        String routing = Boolean.TRUE.equals(saved.getNeedsBudget())
                ? "with budget"
                : Boolean.FALSE.equals(saved.getNeedsBudget())
                        ? "without budget"
                        : "budget classification not set";
        String document = saved.getSoFilePath() != null && !saved.getSoFilePath().isBlank()
                ? "document attached"
                : "no document attached";

        String message = "The Chancellor's Office updated the Special Order draft for \""
                + saved.getProjectTitle() + "\" (SO No. " + soNumber + "; " + routing + "; "
                + document + "). Final approval is still pending. The proposal becomes "
                + "available to OVCAF after the Chancellor's Office completes final approval.";

        NotificationEvent event = NotificationEvent.soDraftChanged(
                numberChanged || budgetChanged, fileChanged);
        notificationService.notifyRoles(List.of(UserRoles.OVCAF), message,
                "Special Order Draft Updated", NotificationTypes.SPECIAL_ORDER_DRAFT,
                saved.getId(), event);
    }

    /** Largest accepted Special Order document in bytes. */
    public long getSpecialOrderMaxBytes() {
        long megabytes = specialOrderMaxSizeMb > 0 ? specialOrderMaxSizeMb : 10;
        return megabytes * 1024 * 1024;
    }

    /** The readable reason shown when the SO document exceeds the configured limit. */
    public String getSpecialOrderMaxSizeMessage() {
        long megabytes = specialOrderMaxSizeMb > 0 ? specialOrderMaxSizeMb : 10;
        return "The Special Order document must be " + megabytes + " MB or smaller.";
    }

    /**
     * Deletes a just-stored SO document when the surrounding transaction does not
     * commit. The filesystem does not participate in the transaction, so without this
     * a commit-time failure (a constraint raised while flushing, for example) would
     * leave an orphaned upload behind.
     */
    private void deleteStoredSpecialOrderOnRollback(String storedPath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    uploadStorage.delete(storedPath);
                }
            }
        });
    }

    /**
     * Deletes an SO document that a committed transaction replaced or removed. The
     * row is updated only if the transaction commits, so deleting before that point
     * could leave the database pointing at a file that a rollback still needs.
     */
    private void deleteStoredSpecialOrderAfterCommit(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }
        Runnable delete = () -> {
            try {
                uploadStorage.delete(storedPath);
            } catch (RuntimeException e) {
                // The approval/draft is already committed; an orphaned old file must
                // not turn a successful request into an error.
                log.warn("Could not delete the replaced Special Order document {}: {}",
                        storedPath, e.getMessage());
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            delete.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                delete.run();
            }
        });
    }

    /**
     * The real format of an uploaded Special Order document: the extension must be
     * PDF, DOC or DOCX, a declared media type (when the client sends one) must belong
     * to that format, and the first bytes must carry the matching signature. A file
     * that merely has a {@code .pdf} name is therefore rejected.
     */
    private String readSpecialOrderFileFormat(MultipartFile file, byte[] bytes) {
        String original = file.getOriginalFilename();
        String name = original == null ? "" : original.replace('\\', '/');
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
        if (!SPECIAL_ORDER_FORMATS.contains(extension)) {
            throw new IllegalArgumentException(SPECIAL_ORDER_FORMAT_MESSAGE);
        }

        String declared = file.getContentType() == null
                ? ""
                : file.getContentType().trim().toLowerCase(Locale.ROOT);
        boolean genericType = declared.isEmpty() || "application/octet-stream".equals(declared);
        if (!genericType && !SPECIAL_ORDER_MEDIA_TYPES.contains(declared)) {
            throw new IllegalArgumentException(SPECIAL_ORDER_FORMAT_MESSAGE);
        }

        String detected;
        if (startsWith(bytes, PDF_HEADER)) {
            detected = "pdf";
        } else if (startsWith(bytes, DOC_HEADER)) {
            detected = "doc";
        } else if (startsWith(bytes, ZIP_HEADER)) {
            detected = "docx";
        } else {
            throw new IllegalArgumentException(SPECIAL_ORDER_FORMAT_MESSAGE);
        }
        if (!extension.equals(detected)) {
            throw new IllegalArgumentException(SPECIAL_ORDER_FORMAT_MESSAGE);
        }
        return detected;
    }

    /**
     * The Chancellor's own file name, kept for display only: any directory part is
     * stripped and the length is capped to the column size.
     */
    private String specialOrderDisplayName(MultipartFile file, String extension) {
        String fallback = "special-order." + extension;
        String original = file == null ? null : file.getOriginalFilename();
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

    @Transactional
    public Proposal updateReviewerInfo(Long id, String reviewedBy, String reviewedByPosition) {
        Proposal proposal = getProposalById(id);
        currentUser.requireAccessTo(proposal);
        proposal.setReviewedBy(PersonNameFormatter.format(reviewedBy));
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
        String previousStatus = ProposalStatus.normalize(proposal.getStatus());
        // A return from a reviewing office goes to RPS first, never directly to the
        // Proponent. RPS is the only party that can forward the request and set the
        // revision deadline.
        proposal.setStatus("RETURNED_TO_RPS");
        proposal.setRemarks(remarks);
        proposal.setReturnRemarks(remarks);
        proposal.setReturnedByOffice(returnedByOffice);
        proposal.setReturnKind("REVISION");
        proposal.setReturnedByUserId(returnedByUserId);
        proposal.setReturnedByName(PersonNameFormatter.format(returnedByName));
        proposal.setReturnedAt(LocalDateTime.now());
        // Reset any previous forward/deadline metadata so a fresh return restarts the flow.
        proposal.setRevisionDeadline(null);
        proposal.setRevisionForwardedAt(null);
        proposal.setRevisionForwardedByName(null);
        proposal.setRevisionForwardedByUserId(null);
        proposal.setRevisionNotes(null);
        recordStatusChange(proposal, previousStatus, "RETURNED_TO_RPS", remarks);
        proposal = proposalRepository.save(proposal);

        // Persist the per-section remarks for this revision round (if any).
        proposalSectionRemarkService.recordReturnRemarks(
                proposal, sectionRemarks, returnedByOffice, returnedByUserId, returnedByName);

        // Notify RPS to review and forward the revision request, and tell the Proponent
        // their proposal was returned for revision (the office and its remarks travel
        // with the message). RPS forwarding later adds the deadline as a second event.
        proposalNotificationPolicy.notifyForStatus(proposal);
        proposalNotificationPolicy.notifyProponentRevisionReturned(proposal, returnedByOffice);

        return proposal;
    }

    /**
     * The Chancellor's Office returns a proposal to ORPS (RPS) instead of deciding it.
     *
     * <p>The destination is ORPS, the office that owns the proposal's processing, using
     * the existing {@code RETURNED_TO_RPS} state that every reviewing office already uses
     * to hand a record back. ORPS reviews the remarks and forwards the revision to the
     * Proponent through the established workflow. The transition is recorded in
     * {@code proposal_status_history} with its actor, remarks and time. ORPS is notified
     * through the existing RETURNED_TO_RPS recipient rule, and the Proponent is told the
     * proposal was returned for revision (naming the office and its remarks) so they are
     * not left waiting for ORPS to forward it.
     */
    @Transactional
    public Proposal returnToOrps(Long proposalId, String remarks) {
        Proposal proposal = proposalRepository.findByIdForUpdate(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found."));

        String status = proposal.getStatus() == null
                ? ""
                : proposal.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!"FOR_OC_APPROVAL".equals(status)) {
            throw new IllegalStateException(RETURN_TO_ORPS_NOT_AT_OC_MESSAGE);
        }

        String normalisedRemarks = remarks == null || remarks.isBlank()
                ? null
                : truncate(remarks.trim(), 2000);
        Long actorId = currentUser.requireUserId();
        String actorName = userRepository.findById(actorId).map(User::getName).orElse(null);

        proposal.setStatus("RETURNED_TO_RPS");
        proposal.setReturnKind("REVISION");
        proposal.setReturnedByOffice("OC");
        proposal.setReturnedByUserId(actorId);
        proposal.setReturnedByName(PersonNameFormatter.format(actorName));
        proposal.setReturnedAt(LocalDateTime.now());
        if (normalisedRemarks != null) {
            proposal.setRemarks(normalisedRemarks);
            proposal.setReturnRemarks(normalisedRemarks);
        }
        // A fresh return restarts the revision-forward metadata; ORPS sets the deadline.
        proposal.setRevisionDeadline(null);
        proposal.setRevisionForwardedAt(null);
        proposal.setRevisionForwardedByName(null);
        proposal.setRevisionForwardedByUserId(null);
        proposal.setRevisionNotes(null);
        recordStatusChange(proposal, "FOR_OC_APPROVAL", "RETURNED_TO_RPS", normalisedRemarks);

        Proposal saved = proposalRepository.save(proposal);
        // RETURNED_TO_RPS tells ORPS it has work to review; the Proponent is told their
        // proposal was returned for revision by the Chancellor's Office. This is not an
        // REC recommendation, so the message never says the proposal was approved.
        proposalNotificationPolicy.notifyForStatus(saved);
        proposalNotificationPolicy.notifyProponentRevisionReturned(saved, "OC");
        return saved;
    }

    /**
     * The statuses in which OVCRIGE still owns the proposal and may reject it back to
     * ORPS. Once a proposal has been forwarded to OVCAF or the Chancellor's Office it
     * has left OVCRIGE's stage and can no longer be rejected from this action.
     */
    private static final Set<String> OVCRIGE_REJECTABLE_STATUSES = Set.of(
            "ENDORSED", "UNDER_REVIEW", "REC_APPROVED", "OVC_APPROVED");

    /**
     * OVCRIGE rejects a proposal by returning it to ORPS (RPS), not by closing it.
     *
     * <p>The target is the existing {@code RETURNED_TO_RPS} state - the same state a
     * reviewing office uses to hand a record back to ORPS. The rejection reason is
     * stored in both {@code remarks} and {@code return_remarks}, the transition is
     * written to {@code proposal_status_history}, and only ORPS is notified. The
     * Proponent is deliberately not notified here: ORPS reviews the rejection and
     * forwards the final decision through {@link #finalizeRejection(Long, String)}.
     *
     * <p>The row is locked so a repeated click (or two concurrent requests) cannot
     * create a second history row or a duplicate ORPS notification.
     */
    @Transactional
    public Proposal rejectToRps(Long proposalId, String remarks) {
        Proposal proposal = proposalRepository.findByIdForUpdate(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found."));
        currentUser.requireAccessTo(proposal);

        String previousStatus = ProposalStatus.normalize(proposal.getStatus());
        if ("RETURNED_TO_RPS".equals(previousStatus)) {
            // A retried request must not produce a second ORPS notification.
            return proposal;
        }
        if (previousStatus == null || !OVCRIGE_REJECTABLE_STATUSES.contains(previousStatus)) {
            throw new IllegalStateException(
                    "Only a proposal still in OVCRIGE review can be rejected and returned to ORPS.");
        }

        String reason = remarks == null || remarks.isBlank() ? null : truncate(remarks.trim(), 2000);
        Long actorId = currentUser.requireUserId();
        String actorName = userRepository.findById(actorId).map(User::getName).orElse(null);

        proposal.setStatus("RETURNED_TO_RPS");
        if (reason != null) {
            proposal.setRemarks(reason);
            proposal.setReturnRemarks(reason);
        }
        proposal.setReturnedByOffice("OVCRIGE");
        proposal.setReturnKind("REJECTION");
        proposal.setReturnedByUserId(actorId);
        proposal.setReturnedByName(PersonNameFormatter.format(actorName));
        proposal.setReturnedAt(LocalDateTime.now());
        // A fresh return restarts the revision-forward metadata; no deadline is set here.
        proposal.setRevisionDeadline(null);
        proposal.setRevisionForwardedAt(null);
        proposal.setRevisionForwardedByName(null);
        proposal.setRevisionForwardedByUserId(null);
        proposal.setRevisionNotes(null);

        recordStatusChange(proposal, previousStatus, "RETURNED_TO_RPS", reason);

        Proposal saved = proposalRepository.save(proposal);
        proposalNotificationPolicy.notifyRpsRejectionReturned(saved, reason);
        return saved;
    }

    /**
     * ORPS completes an OVCRIGE rejection by closing the proposal and informing the
     * Proponent. Only a proposal returned to ORPS by OVCRIGE can be finalized this way;
     * a revision return continues through the existing forward-revision workflow.
     *
     * <p>The OVCRIGE reason is preserved in {@code return_remarks}; an optional ORPS
     * note is appended to {@code remarks} so the Proponent's notification carries the
     * full explanation. The transition is recorded in the proposal's status history.
     */
    @Transactional
    public Proposal finalizeRejection(Long proposalId, String remarks) {
        Proposal proposal = proposalRepository.findByIdForUpdate(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found."));
        currentUser.requireAccessTo(proposal);

        String previousStatus = ProposalStatus.normalize(proposal.getStatus());
        if ("REJECTED".equals(previousStatus)) {
            // Idempotent: a retried ORPS confirmation must not notify the Proponent twice.
            return proposal;
        }
        String returnKind = proposal.getReturnKind() == null
                ? null
                : proposal.getReturnKind().trim();
        if (!"RETURNED_TO_RPS".equals(previousStatus)
                || !"REJECTION".equalsIgnoreCase(returnKind)) {
            throw new IllegalStateException(
                    "This proposal is not awaiting ORPS processing of a reviewing office rejection.");
        }

        String note = remarks == null || remarks.isBlank() ? null : truncate(remarks.trim(), 2000);
        String ovcrigeReason = proposal.getReturnRemarks();
        if (ovcrigeReason == null || ovcrigeReason.isBlank()) {
            ovcrigeReason = proposal.getRemarks();
        }
        String finalRemarks;
        if (ovcrigeReason == null || ovcrigeReason.isBlank()) {
            finalRemarks = note;
        } else if (note == null) {
            finalRemarks = ovcrigeReason;
        } else {
            finalRemarks = ovcrigeReason + "\n\nORPS note: " + note;
        }

        proposal.setStatus("REJECTED");
        if (finalRemarks != null) {
            proposal.setRemarks(finalRemarks);
        }
        recordStatusChange(proposal, previousStatus, "REJECTED", finalRemarks);

        Proposal saved = proposalRepository.save(proposal);
        // The existing REJECTED rule notifies the Proponent only; ORPS is the sender.
        proposalNotificationPolicy.notifyForStatus(saved);
        return saved;
    }

    /**
     * Appends one audited status transition to the proposal. The row is cascaded by
     * the proposal's own save, so it commits (or rolls back) with the status change.
     */
    private void recordStatusChange(Proposal proposal, String fromStatus, String toStatus, String remarks) {
        // History rows carry the same codes as proposals.status, so they are validated by
        // the same rule. A baseline-style empty from_status stays NULL.
        String storedFromStatus = ProposalStatus.optionalSupportedStatus(fromStatus);
        String storedToStatus = ProposalStatus.requireSupportedStatus(toStatus);

        Long actorUserId = currentUser.getUserId();
        String actorRole = currentUser.getRole();
        String actorName = null;
        if (actorUserId != null) {
            actorName = userRepository.findById(actorUserId)
                    .map(User::getName)
                    .orElse(null);
        }

        ProposalStatusHistory entry = new ProposalStatusHistory();
        entry.setProposal(proposal);
        entry.setFromStatus(storedFromStatus);
        entry.setToStatus(storedToStatus);
        entry.setActorUserId(actorUserId);
        entry.setActorRole(actorRole);
        entry.setActorName(actorName);
        entry.setRemarks(remarks);
        entry.setSource(ProposalStatusHistory.SOURCE_APPLICATION);
        entry.setChangedAt(LocalDateTime.now());
        proposal.getStatusHistory().add(entry);
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
        if ("REJECTION".equalsIgnoreCase(proposal.getReturnKind())) {
            throw new IllegalStateException(
                    "This proposal was rejected by a reviewing office and must be finalized, not forwarded as a revision.");
        }
        String previousStatus = ProposalStatus.normalize(proposal.getStatus());
        LocalDateTime deadlineLdt = parseDeadline(deadline);

        // If no office return was recorded yet (RPS returning directly), capture the
        // remarks + attribution here so the record is complete.
        if (proposal.getReturnRemarks() == null || proposal.getReturnRemarks().isBlank()) {
            proposal.setReturnRemarks(remarks);
            proposal.setReturnedByOffice("RPS");
            proposal.setReturnKind("REVISION");
            proposal.setReturnedByUserId(forwardedByUserId);
            proposal.setReturnedByName(PersonNameFormatter.format(forwardedByName));
            if (proposal.getReturnedAt() == null) {
                proposal.setReturnedAt(LocalDateTime.now());
            }
        }

        proposal.setStatus("RPS_RETURNED");
        proposal.setRevisionDeadline(deadlineLdt);
        proposal.setRevisionForwardedAt(LocalDateTime.now());
        proposal.setRevisionForwardedByUserId(forwardedByUserId);
        proposal.setRevisionForwardedByName(PersonNameFormatter.format(forwardedByName));
        proposal.setRevisionNotes(notes);

        // Keep the proponent-facing remarks in sync with the office's original remarks so
        // the revision history continues to capture them.
        proposal.setRemarks(proposal.getReturnRemarks());
        recordStatusChange(proposal, previousStatus, "RPS_RETURNED", proposal.getRemarks());

        proposal = proposalRepository.save(proposal);

        // Append RPS's own per-section remarks (if any) to the open revision round so
        // the Proponent sees the office feedback and the RPS feedback together.
        proposalSectionRemarkService.recordForwardRemarks(
                proposal, sectionRemarks, "RPS", forwardedByUserId, forwardedByName);

        // Notify the Proponent of the revision request, the deadline and the feedback.
        // The same deadline is the idempotency key: pressing Forward twice cannot write
        // the same revision request again, while setting a new deadline is a new event.
        if (proposal.getProponent() != null) {
            String deadlineText = deadlineLdt != null
                    ? deadlineLdt.toLocalDate().toString()
                    : "not set";
            String feedback = proposal.getRemarks() == null || proposal.getRemarks().isBlank()
                    ? ""
                    : " Remarks: " + proposal.getRemarks().trim();
            notificationService.createNotification(
                    proposal.getProponent().getId(),
                    "Your proposal \"" + proposal.getProjectTitle()
                            + "\" has been returned for revision. Please review the remarks and "
                            + "make the necessary corrections. Deadline: " + deadlineText + "."
                            + feedback,
                    "Proposal Returned for Revision",
                    NotificationTypes.REVISION,
                    proposal.getId(), null,
                    NotificationEvent.STATUS_UPDATED.withDedupeKey(
                            "proposal:" + proposal.getId() + ":revision-forwarded:" + deadlineText),
                    "PROPONENT");
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

        validateSiteCountries(request);

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
     * The three levels a risk entry may carry in its {@code likelihood} and
     * {@code impact} columns. The values are matched case-insensitively and stored in
     * their canonical form, so a client can never persist free text ("maybe", "very high")
     * in a column the form renders as a High / Medium / Low dropdown.
     */
    public static final List<String> RISK_LEVELS = List.of("High", "Medium", "Low");

    /**
     * Validates and normalizes the structured Risk Management Plan (section 21) of a
     * request.
     * <ul>
     *   <li>A row whose four fields are all empty carries no information (the form seeds
     *       one empty row for a new proposal) and is dropped instead of being stored as an
     *       empty entry.</li>
     *   <li>{@code likelihood} and {@code impact} must be High, Medium or Low. An
     *       unsupported value is rejected with the row number, so a modified API request
     *       cannot store what the dropdowns never offer.</li>
     *   <li>Every field of a row that is being <em>submitted</em> must be filled in:
     *       whitespace-only text counts as empty. A draft is saved while the proponent is
     *       still filling the table in, so a partial row is kept there (exactly like the
     *       other required sections of the form, which the backend also only enforces on
     *       submission).</li>
     * </ul>
     */
    static List<RiskManagementEntry> validateRiskManagement(List<ProposalRequest.RiskManagementDTO> rows,
            boolean submission) {
        List<RiskManagementEntry> entries = new ArrayList<>();
        if (rows == null) {
            return entries;
        }
        for (int i = 0; i < rows.size(); i++) {
            ProposalRequest.RiskManagementDTO row = rows.get(i);
            String identifiedRisks = row == null ? null : trimToNull(row.getIdentifiedRisks());
            String likelihood = row == null ? null : trimToNull(row.getLikelihood());
            String impact = row == null ? null : trimToNull(row.getImpact());
            String mitigationMeasure = row == null ? null : trimToNull(row.getMitigationMeasure());

            if (identifiedRisks == null && likelihood == null && impact == null && mitigationMeasure == null) {
                continue;
            }

            if (submission) {
                requireRiskField(identifiedRisks, i, "the identified risk is required");
                requireRiskField(likelihood, i, "the likelihood is required");
                requireRiskField(impact, i, "the impact is required");
                requireRiskField(mitigationMeasure, i, "the mitigation measure is required");
            }

            entries.add(new RiskManagementEntry(identifiedRisks,
                    normalizeRiskLevel(likelihood, i, "likelihood"),
                    normalizeRiskLevel(impact, i, "impact"),
                    mitigationMeasure));
        }
        return entries;
    }

    private static void requireRiskField(String value, int index, String message) {
        if (value == null) {
            throw new IllegalArgumentException("Risk entry #" + (index + 1) + ": " + message + ".");
        }
    }

    /** Canonical High / Medium / Low, or a 400 naming the row and the column. */
    static String normalizeRiskLevel(String value, int index, String field) {
        if (value == null) {
            return null;
        }
        String candidate = value.trim();
        for (String level : RISK_LEVELS) {
            if (level.equalsIgnoreCase(candidate)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Risk entry #" + (index + 1) + ": " + field
                + " must be High, Medium or Low.");
    }

    /**
     * The readable rendering of the structured Risk Management Plan, one entry per line.
     * It is what the legacy {@code risks} column of the proposal holds while structured
     * entries exist, so every screen and export that predates the table still shows the
     * plan instead of an empty field.
     */
    static String renderRiskManagement(List<RiskManagementEntry> entries) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            RiskManagementEntry entry = entries.get(i);
            if (i > 0) {
                text.append('\n');
            }
            text.append(i + 1).append(". ").append(riskText(entry.getIdentifiedRisks()));
            List<String> levels = new ArrayList<>();
            if (!isBlank(entry.getLikelihood())) {
                levels.add("Likelihood: " + entry.getLikelihood());
            }
            if (!isBlank(entry.getImpact())) {
                levels.add("Impact: " + entry.getImpact());
            }
            if (!levels.isEmpty()) {
                text.append(" (").append(String.join(", ", levels)).append(')');
            }
            if (!isBlank(entry.getMitigationMeasure())) {
                text.append("\n   Mitigation: ").append(entry.getMitigationMeasure());
            }
        }
        return text.toString();
    }

    private static String riskText(String value) {
        return value == null ? "" : value;
    }

    /**
     * Normalizes one "% Time Devoted" value (section 24) to the plain number the column
     * stores.
     *
     * <ul>
     *   <li>A blank value stays blank, so an empty row is not turned into "0".</li>
     *   <li>A percentage symbol, spacing and a trailing decimal point are removed, so a
     *       legacy "50 %" becomes "50" and the symbol can never be stored twice.</li>
     *   <li>A value outside 0-100 is capped, so the column can never hold a negative
     *       percentage or more than 100% even when a client bypasses the form.</li>
     *   <li>A value that is not a number at all is rejected while the proposal is being
     *       submitted (the message names the row). A draft keeps it exactly as it was,
     *       because only a legacy record can hold such a value and saving an old draft
     *       must never fail or silently rewrite it.</li>
     * </ul>
     */
    static String normalizeTimeDevoted(String value, int position, boolean submission) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            // "" and null both mean "nothing entered"; the value is kept as it came.
            return value;
        }

        String digits = trimmed.replace("%", "").replaceAll("\\s", "");
        // "50." is a value still being typed; it is the number 50.
        if (digits.endsWith(".")) {
            digits = digits.substring(0, digits.length() - 1);
        }
        if (digits.isEmpty()) {
            return value;
        }

        BigDecimal number;
        try {
            number = new BigDecimal(digits);
        } catch (NumberFormatException e) {
            if (submission) {
                throw new IllegalArgumentException(
                        "Personnel entry #" + position + ": " + TIME_DEVOTED_MESSAGE);
            }
            return trimmed;
        }

        BigDecimal capped = number
                .max(BigDecimal.valueOf(TIME_DEVOTED_MIN))
                .min(BigDecimal.valueOf(TIME_DEVOTED_MAX));
        // Canonical form: "50", "12.5" - never "50.00" and never with a % symbol.
        return capped.stripTrailingZeros().toPlainString();
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
     * Sites of Implementation are Philippine-only.
     *
     * <p>The form offers only the Philippines, but a request can arrive from a client
     * that skips the browser, so every submitted site is checked again before the
     * proposal is accepted. A blank country means the Philippines, exactly like the
     * Address section. No other site part is validated here: the existing location
     * data drives the dropdowns, and adding new rules to unrelated site fields would
     * change their requirements.</p>
     */
    static void validateSiteCountries(ProposalRequest request) {
        if (request == null || request.getSites() == null) {
            return;
        }
        int position = 1;
        for (ProposalRequest.SiteDTO site : request.getSites()) {
            String country = trimToNull(site == null ? null : site.getCountry());
            if (country != null && !PhilippineAddressCatalog.PHILIPPINES.equalsIgnoreCase(country)) {
                throw new IllegalArgumentException(
                        "Site " + position + ": only the Philippines is accepted as the country.");
            }
            position += 1;
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
        // The storage layer generates the stored name and returns the path relative to
        // its root - the value the authorized download route expects.
        return uploadStorage.store(bytes, TECHNOLOGY_MAP_DIR, "technology-map." + extension);
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
        // The storage layer only ever removes a path inside its own root, so a corrupted
        // column value cannot delete anything else.
        uploadStorage.delete(storedPath);
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
        // The storage layer generates the stored name and returns the path relative to
        // its root - the value the authorized download route expects.
        return uploadStorage.store(bytes, BUDGET_FILE_DIR, "line-item-budget." + extension);
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
        // The storage layer only ever removes a path inside its own root, so a corrupted
        // column value cannot delete anything else.
        uploadStorage.delete(storedPath);
    }

    /**
     * Attaches the endorsement document of the Certification &amp; Endorsement step
     * ("Endorsed By") to a proposal.
     * <p>
     * The document is attached to the caller's own proposal only (ownership is resolved
     * from the authenticated principal, never from the request), it must be a PDF of at
     * most {@link #ENDORSEMENT_DOCUMENT_MAX_BYTES} bytes - decided from the file name,
     * the declared media type, the real bytes and by opening the document, so an image
     * renamed to {@code .pdf} and a corrupted PDF are both rejected - and it is stored
     * under a generated name while the proponent's own file name is kept for display
     * only. Replacing the document deletes the previous one, so a proposal never
     * accumulates orphaned uploads.
     */
    @Transactional
    public Proposal attachEndorsementDocument(Long proposalId, MultipartFile file) {
        Proposal proposal = getProposalByIdForCurrentUser(proposalId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(ENDORSEMENT_DOCUMENT_EMPTY_MESSAGE);
        }
        if (file.getSize() > ENDORSEMENT_DOCUMENT_MAX_BYTES) {
            throw new IllegalArgumentException(ENDORSEMENT_DOCUMENT_SIZE_MESSAGE);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "The endorsement document could not be read.");
        }

        requireEndorsementPdf(file, bytes);
        String storedPath = uploadStorage.store(bytes, ENDORSEMENT_DOCUMENT_DIR,
                "endorsement-document.pdf");

        String previousPath = proposal.getEndorsementFilePath();
        proposal.setEndorsementFileName(endorsementDocumentDisplayName(file));
        proposal.setEndorsementFilePath(storedPath);
        Proposal saved = proposalRepository.save(proposal);

        deleteStoredEndorsementDocument(previousPath);
        return saved;
    }

    /**
     * Verifies that an uploaded endorsement document really is a PDF: the extension must
     * be {@code .pdf}, a declared media type (when the client sends one) must be
     * {@code application/pdf}, the first bytes must be the PDF header, and the document
     * must open with at least one page - which is what rejects a truncated or corrupted
     * file that merely starts with "%PDF-".
     */
    private void requireEndorsementPdf(MultipartFile file, byte[] bytes) {
        String original = file.getOriginalFilename();
        String name = original == null ? "" : original.replace('\\', '/');
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
        if (!"pdf".equals(extension)) {
            throw new IllegalArgumentException(ENDORSEMENT_DOCUMENT_FORMAT_MESSAGE);
        }

        String declared = file.getContentType() == null
                ? ""
                : file.getContentType().trim().toLowerCase(Locale.ROOT);
        boolean genericType = declared.isEmpty() || "application/octet-stream".equals(declared);
        if (!genericType && !ENDORSEMENT_DOCUMENT_MEDIA_TYPES.contains(declared)) {
            throw new IllegalArgumentException(ENDORSEMENT_DOCUMENT_FORMAT_MESSAGE);
        }

        if (!startsWith(bytes, PDF_HEADER)) {
            throw new IllegalArgumentException(ENDORSEMENT_DOCUMENT_FORMAT_MESSAGE);
        }

        try {
            PdfReader reader = new PdfReader(bytes);
            int pages = reader.getNumberOfPages();
            reader.close();
            if (pages < 1) {
                throw new IllegalArgumentException(ENDORSEMENT_DOCUMENT_FORMAT_MESSAGE);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            // Not a readable PDF document: refused instead of stored.
            throw new IllegalArgumentException(ENDORSEMENT_DOCUMENT_FORMAT_MESSAGE);
        }
    }

    /**
     * The proponent's own file name, kept for display only. Any directory part is
     * stripped and the length is capped to the column size, so a crafted upload can
     * never store a path-like value.
     */
    private String endorsementDocumentDisplayName(MultipartFile file) {
        String fallback = "endorsement-document.pdf";
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
     * Removes the endorsement document a previous upload stored. Only a path inside the
     * storage root is ever touched, and a missing file (or a path left over from before
     * this feature) is not an error - so historical records are never affected.
     */
    private void deleteStoredEndorsementDocument(String storedPath) {
        uploadStorage.delete(storedPath);
    }

    /**
     * Attaches the proponent's own signature of the Certification &amp; Endorsement step
     * ("Submitted By") to a proposal.
     * <p>
     * The file is attached to the caller's own proposal only (ownership is resolved from
     * the authenticated principal, never from the request), it must be a readable PNG or
     * JPEG image, or a valid PDF, of at most {@link #SUBMITTED_BY_SIGNATURE_MAX_BYTES}
     * bytes - decided from the real bytes, never only from the file name - and it is
     * stored under a generated name while the proponent's own file name is kept for
     * display only. Replacing the signature deletes only the previous submitted-signature
     * file; the endorsement PDF and every other attachment are untouched.
     */
    @Transactional
    public Proposal attachSubmittedBySignature(Long proposalId, MultipartFile file) {
        Proposal proposal = getProposalByIdForCurrentUser(proposalId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(SUBMITTED_BY_SIGNATURE_EMPTY_MESSAGE);
        }
        if (file.getSize() > SUBMITTED_BY_SIGNATURE_MAX_BYTES) {
            throw new IllegalArgumentException(SUBMITTED_BY_SIGNATURE_SIZE_MESSAGE);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "The signature file could not be read.");
        }

        String extension = requireSubmittedBySignature(file, bytes);
        String storedPath = uploadStorage.store(bytes, SUBMITTED_BY_SIGNATURE_DIR,
                "signature." + extension);

        String previousPath = proposal.getSubmittedByFilePath();
        proposal.setSubmittedByFileName(submittedBySignatureDisplayName(file, extension));
        proposal.setSubmittedByFilePath(storedPath);
        Proposal saved = proposalRepository.save(proposal);

        deleteStoredSubmittedBySignature(previousPath);
        return saved;
    }

    /**
     * Verifies the real format of an uploaded signature and returns the stored extension.
     * A {@code .pdf} name is checked as a PDF (header and readable pages); every other
     * name is accepted only when the bytes are a readable PNG/JPEG raster image, so an
     * image renamed to {@code .pdf} - or any other document - is refused.
     */
    private String requireSubmittedBySignature(MultipartFile file, byte[] bytes) {
        String original = file.getOriginalFilename();
        String name = original == null ? "" : original.replace('\\', '/');
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).trim().toLowerCase(Locale.ROOT);

        if ("pdf".equals(extension)) {
            requireEndorsementPdf(file, bytes);
            return "pdf";
        }

        String imageFormat;
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) {
                throw new IllegalArgumentException(SUBMITTED_BY_SIGNATURE_FORMAT_MESSAGE);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException(SUBMITTED_BY_SIGNATURE_FORMAT_MESSAGE);
            }
            ImageReader reader = readers.next();
            try {
                imageFormat = reader.getFormatName() == null
                        ? ""
                        : reader.getFormatName().toLowerCase(Locale.ROOT);
            } catch (IOException e) {
                throw new IllegalArgumentException(SUBMITTED_BY_SIGNATURE_FORMAT_MESSAGE);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(SUBMITTED_BY_SIGNATURE_FORMAT_MESSAGE);
        }
        if (!SUBMITTED_BY_SIGNATURE_IMAGE_FORMATS.contains(imageFormat)) {
            throw new IllegalArgumentException(SUBMITTED_BY_SIGNATURE_FORMAT_MESSAGE);
        }
        return "jpeg".equals(imageFormat) ? "jpg" : imageFormat;
    }

    /** The proponent's own file name, kept for display only (directory part stripped). */
    private String submittedBySignatureDisplayName(MultipartFile file, String extension) {
        String fallback = "signature." + extension;
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
     * Removes the submitted signature a previous upload stored. Only a path inside the
     * storage root is ever touched, and a missing file (or a path left over from before
     * this feature) is not an error.
     */
    private void deleteStoredSubmittedBySignature(String storedPath) {
        uploadStorage.delete(storedPath);
    }

    /**
     * Copies the reusable proposal content onto a duplicate. Uploaded documents (the
     * technology map, the Line-Item Budget, the endorsement PDF and the submitted
     * signature) and the certification fields are deliberately NOT copied: the existing
     * "Make a Copy" flow starts a fresh draft whose files are uploaded again, and a copy
     * must never silently point at another proposal's stored file.
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
        // Section 19: the GAD Checklist 2 is duplicated map by map (a new object with new
        // maps), so editing the copy can never change the original proposal's responses or
        // comments. The derived totals are copied as they stood; the backend recalculates
        // them whenever the copy is saved.
        if (source.getGadChecklist() != null) {
            GadChecklistData sourceChecklist = source.getGadChecklist();
            GadChecklistData copiedChecklist = new GadChecklistData();
            copiedChecklist.setResponses(sourceChecklist.getResponses() == null
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(sourceChecklist.getResponses()));
            copiedChecklist.setComments(sourceChecklist.getComments() == null
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(sourceChecklist.getComments()));
            copiedChecklist.setTotalScore(sourceChecklist.getTotalScore());
            copiedChecklist.setInterpretation(sourceChecklist.getInterpretation());
            copiedChecklist.setCompleted(sourceChecklist.getCompleted());
            copy.setGadChecklist(copiedChecklist);
        }
        copy.setRisks(source.getRisks());
        // Section 21: the structured Risk Management Plan is duplicated entry by entry, so
        // editing the copy can never change the original proposal's plan.
        List<RiskManagementEntry> copiedRiskEntries = new ArrayList<>();
        if (source.getRiskManagement() != null) {
            for (RiskManagementEntry entry : source.getRiskManagement()) {
                copiedRiskEntries.add(new RiskManagementEntry(entry.getIdentifiedRisks(),
                        entry.getLikelihood(), entry.getImpact(), entry.getMitigationMeasure()));
            }
        }
        copy.setRiskManagement(copiedRiskEntries);
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

}
