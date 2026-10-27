package com.rpis.backend.service;

import com.rpis.backend.dto.EndorsementDecisionRequest;
import com.rpis.backend.dto.EndorsementEventResponse;
import com.rpis.backend.dto.EndorsementRequestCreateRequest;
import com.rpis.backend.dto.EndorsementRequestResponse;
import com.rpis.backend.model.Certification;
import com.rpis.backend.model.EndorsementLetterSettings;
import com.rpis.backend.model.EndorsementRequest;
import com.rpis.backend.model.EndorsementRequestEvent;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.EndorsementRequestEventRepository;
import com.rpis.backend.repository.EndorsementRequestRepository;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * The Dean / Department Chair endorsement workflow.
 *
 * <p>A Faculty Proponent raises a request with only the preliminary information, the
 * request is routed to the Dean authorized for its college, and that Dean approves
 * (which issues the official endorsement letter, files it in the Proponent's
 * Certifications section and notifies them) or rejects it with a reason.
 *
 * <h2>Identity and authorization</h2>
 * Every identity is resolved from the authenticated token through {@link CurrentUser}:
 * the Proponent of a request, the Dean who decides it and the actor of every audit
 * entry. No user id is ever accepted from the request body, so a Proponent can
 * neither submit on someone else's behalf nor approve their own request, and a Dean
 * can only reach the requests inside their authorized scope.
 *
 * <h2>Transaction safety</h2>
 * Approval takes a row-level write lock on the request
 * ({@link EndorsementRequestRepository#findByIdForUpdate}), re-checks that it is still
 * pending, and only then issues the letter. A repeated or concurrent approval is
 * refused, and the certification entry is keyed by the request, so no duplicate
 * letter or certification can be produced. The PDF and the signature snapshot are
 * written before the database work and removed again if the transaction fails, so a
 * failed generation can never leave a request marked approved without its letter.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EndorsementRequestService {

    /** The role that raises endorsement requests. */
    public static final String ROLE_PROPONENT = "PROPONENT";
    /** The Dean / Department Chair role. */
    public static final String ROLE_DEAN = "RPS_DEAN";
    /** The system administrator role. */
    public static final String ROLE_ADMIN = "RPS_ADMIN";
    /** The ORPS staff role. */
    public static final String ROLE_STAFF = "RPS_STAFF";

    /** Reference and document code format: MSUN-ORPS-END-<code>-<year>. */
    static final String REFERENCE_FORMAT = "MSUN-ORPS-END-%03d-%d";

    /** Where issued letters and their signature snapshots are stored. */
    static final String UPLOAD_DIR = "uploads/endorsements/";

    private static final Path UPLOAD_ROOT = Paths.get("uploads").toAbsolutePath().normalize();

    private final EndorsementRequestRepository requestRepository;
    private final EndorsementRequestEventRepository eventRepository;
    private final UserRepository userRepository;
    private final ProposalAnnouncementRepository announcementRepository;
    private final ProposalAnnouncementService announcementService;
    private final ProposalRepository proposalRepository;
    private final CertificationService certificationService;
    private final EndorsementLetterService letterService;
    private final EndorsementLetterSettingsService settingsService;
    private final UserSignatureService signatureService;
    private final NotificationService notificationService;
    private final CurrentUser currentUser;
    private final JdbcTemplate jdbcTemplate;

    /** Guards the one-off "create the sequence if it is missing" step. */
    private volatile boolean sequenceChecked;

    // ==================== PROPONENT: CREATE ====================

    /**
     * Creates an endorsement request for the authenticated Proponent and routes it to
     * the Dean authorized for the request's college. The request starts as
     * {@code PENDING}.
     */
    @Transactional
    public EndorsementRequestResponse create(EndorsementRequestCreateRequest request) {
        requireRole(ROLE_PROPONENT, "Only a Faculty Proponent may request an endorsement.");
        Long proponentId = currentUser.requireUserId();
        User proponent = userRepository.findById(proponentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account not found."));

        String proposalTitle = trimmed(request.getProposalTitle());
        if (proposalTitle == null) {
            throw new IllegalArgumentException("The proposal/program title is required.");
        }
        String college = trimmed(request.getCollege());
        if (college == null) {
            throw new IllegalArgumentException("The implementing college/department is required.");
        }
        if (request.getProposalAnnouncementId() == null) {
            throw new IllegalArgumentException("The Call for Proposal being applied to is required.");
        }
        ProposalAnnouncement announcement = requireEligibleAnnouncement(request.getProposalAnnouncementId());

        EndorsementRequest entity = new EndorsementRequest();
        entity.setReferenceNumber(nextReference());
        entity.setProponent(proponent);
        entity.setCollege(college);
        entity.setDepartment(trimmed(request.getDepartment()));
        entity.setProposalTitle(proposalTitle);
        entity.setProposalAnnouncementId(announcement.getId());
        entity.setResearchSummary(trimmed(request.getResearchSummary()));
        entity.setNotes(trimmed(request.getNotes()));
        entity.setStatus(EndorsementRequest.STATUS_PENDING);

        User dean = findDeanFor(college);
        entity.setAssignedDean(dean);

        EndorsementRequest saved = requestRepository.save(entity);
        audit(saved, EndorsementRequestEvent.TYPE_CREATED, proponent,
                "Request created for \"" + proposalTitle + "\" under " + announcement.getName() + ".");
        if (dean != null) {
            audit(saved, EndorsementRequestEvent.TYPE_ASSIGNED, proponent,
                    "Routed to " + dean.getName() + deanScopeNote(dean) + ".");
            notifyUser(dean, saved, "New endorsement request",
                    saved.getReferenceNumber() + " from " + proponent.getName()
                            + " is waiting for your endorsement.");
        } else {
            audit(saved, EndorsementRequestEvent.TYPE_ASSIGNED, proponent,
                    "No Dean is configured for " + college
                            + "; the request stays unassigned for any Dean to pick up.");
        }
        return toResponse(saved, announcement);
    }

    private String deanScopeNote(User dean) {
        String scope = dean.getAssignedCollege();
        if (scope == null || scope.isBlank()) {
            return " (no college configured, institution-wide fallback)";
        }
        return " (" + scope + ")";
    }

    /** The Dean authorized for a college, or the institution-wide fallback. */
    private User findDeanFor(String college) {
        List<User> scoped = userRepository
                .findByRoleAndAssignedCollegeIgnoreCaseAndStatusOrderByNameAsc(ROLE_DEAN, college,
                        "APPROVED");
        if (!scoped.isEmpty()) {
            return scoped.get(0);
        }
        List<User> institutionWide = userRepository
                .findByRoleAndAssignedCollegeIsNullAndStatusOrderByNameAsc(ROLE_DEAN, "APPROVED");
        return institutionWide.isEmpty() ? null : institutionWide.get(0);
    }

    /**
     * The selected Call for Proposal must exist and be one the Proponent may actually
     * answer right now - the same rule the Create Proposal workflow uses - so an
     * arbitrary announcement id is never accepted.
     */
    private ProposalAnnouncement requireEligibleAnnouncement(Long announcementId) {
        return announcementService.getAvailableAnnouncements().stream()
                .filter(call -> announcementId.equals(call.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "The selected Call for Proposal is not open for submissions."));
    }

    // ==================== READS ====================

    /** The authenticated Proponent's own requests, newest first. */
    public List<EndorsementRequestResponse> listMine() {
        requireRole(ROLE_PROPONENT, "Only a Faculty Proponent has their own endorsement requests.");
        Long proponentId = currentUser.requireUserId();
        return toResponses(requestRepository.findByProponentIdOrderByCreatedAtDesc(proponentId));
    }

    /**
     * The review list of the authenticated Dean - the requests routed to them plus the
     * unassigned ones inside their authorized scope - or the master list for the RPS
     * administrator / staff roles. An optional status filter is applied on top.
     */
    public List<EndorsementRequestResponse> listForReviewer(String status) {
        String role = currentUser.getRole();
        Long userId = currentUser.requireUserId();

        List<EndorsementRequest> requests;
        if (isAdminOrStaff(role)) {
            requests = new ArrayList<>(requestRepository.findAll());
        } else if (isDean(role)) {
            User dean = requireAccount(userId);
            requests = new ArrayList<>(
                    requestRepository.findByAssignedDeanIdOrderByCreatedAtDesc(userId));
            for (EndorsementRequest unassigned : requestRepository
                    .findByAssignedDeanIdIsNullOrderByCreatedAtDesc()) {
                if (inScope(dean, unassigned)) {
                    requests.add(unassigned);
                }
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to review endorsement requests.");
        }

        String wanted = trimmed(status);
        if (wanted != null) {
            requests.removeIf(request -> !wanted.equalsIgnoreCase(request.getStatus()));
        }
        requests.sort(java.util.Comparator.comparing(EndorsementRequest::getCreatedAt,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        return toResponses(requests);
    }

    /** One request, when the caller is allowed to see it. */
    public EndorsementRequestResponse getForCurrentUser(Long id) {
        return toResponse(requireVisible(id));
    }

    /** The audit trail of one request, for the caller allowed to see it. */
    public List<EndorsementEventResponse> eventsForCurrentUser(Long id) {
        requireVisible(id);
        List<EndorsementEventResponse> events = new ArrayList<>();
        for (EndorsementRequestEvent event : eventRepository
                .findByEndorsementRequestIdOrderByCreatedAtAsc(id)) {
            EndorsementEventResponse response = new EndorsementEventResponse();
            response.setEventType(event.getEventType());
            response.setActorId(event.getActorId());
            response.setActorName(event.getActorName());
            response.setActorRole(event.getActorRole());
            response.setDetail(event.getDetail());
            response.setCreatedAt(event.getCreatedAt());
            events.add(response);
        }
        return events;
    }

    /**
     * The stored official letter of a request, for an authorized caller. A Proponent
     * can only reach their own request, so one Proponent can never download another's
     * letter.
     */
    public EndorsementRequestService.StoredLetter loadLetter(Long id) {
        EndorsementRequest entity = requireVisible(id);
        if (!entity.hasIssuedLetter()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No endorsement letter has been issued for this request yet.");
        }
        Path resolved = UPLOAD_ROOT.resolve(entity.getLetterFilePath()).normalize();
        if (!resolved.startsWith(UPLOAD_ROOT) || !Files.isReadable(resolved)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "The stored endorsement letter is no longer available.");
        }
        String fileName = entity.getLetterFileName();
        if (fileName == null || fileName.isBlank()) {
            fileName = "endorsement-letter.pdf";
        }
        return new StoredLetter(resolved, fileName);
    }

    /** A stored endorsement letter: where it is and the name to offer. */
    public record StoredLetter(Path path, String fileName) {
    }

    // ==================== DEAN: APPROVE ====================

    /**
     * Approves a pending request and issues the official endorsement letter: the PDF
     * is rendered from the official letterhead, stored, recorded on the request, filed
     * in the Proponent's Certifications section and audited, all inside one
     * transaction.
     *
     * <p>The approving identity is the authenticated Dean - never a value taken from
     * the request - and the request is locked for the duration, so a second approval
     * (or a concurrent one) cannot issue a second letter.
     */
    @Transactional
    public EndorsementRequestResponse approve(Long id, EndorsementDecisionRequest request) {
        requireRole(ROLE_DEAN, "Only an authorized Dean may issue an endorsement.");
        Long deanId = currentUser.requireUserId();
        User dean = requireAccount(deanId);

        EndorsementRequest entity = requestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Endorsement request not found."));
        requireDeanScope(entity, dean);
        requirePending(entity, "approved");

        ProposalAnnouncement announcement = announcementRepository
                .findById(entity.getProposalAnnouncementId())
                .orElseThrow(() -> new IllegalStateException(
                        "The Call for Proposal of this request no longer exists."));
        // The IFP wording may only be used for a Call for Proposal that has an official
        // endorsement letter template.
        EndorsementLetterTemplates.requireImplemented(announcement.getFundingProgram());
        EndorsementLetterSettings settings = settingsService.requireComplete();

        String endorserName = firstNonBlank(trimmed(request.getEndorserName()), dean.getName());
        String designation = firstNonBlank(trimmed(request.getEndorserDesignation()),
                firstNonBlank(trimmed(dean.getDesignation()), "Dean"));
        LocalDate endorsementDate = request.getEndorsementDate() != null
                ? request.getEndorsementDate()
                : LocalDate.now();

        byte[] signature = readSignature(deanId);
        if (signature == null || signature.length == 0) {
            throw new IllegalArgumentException(
                    "Save your electronic signature in Manage Profile -> Electronic Signature "
                            + "before approving a request: the official letter is signed with it.");
        }

        String documentCode = entity.getReferenceNumber();
        EndorsementLetterService.GeneratedLetter letter = letterService.generate(
                new EndorsementLetterService.LetterContext(settings, documentCode, entity.getCollege(),
                        endorsementDate, entity.getProposalTitle(), entity.getProponent().getName(),
                        entity.getDepartment(), endorserName, designation, signature));

        StoredFile letterFile = storeFile(letter.content(), "endorsement-letter.pdf");
        StoredFile signatureFile = storeSignatureSnapshot(deanId, signature);
        try {
            return applyApproval(entity, dean, announcement, endorserName, designation,
                    endorsementDate, documentCode, letterFile, signatureFile);
        } catch (RuntimeException e) {
            // The database work rolls back with the transaction; the files written for
            // it must not survive, or an approved request could exist without its
            // letter (or the other way round).
            deleteQuietly(letterFile.storedPath());
            deleteQuietly(signatureFile.storedPath());
            throw e;
        }
    }

    /** The database half of an approval, inside the caller's transaction. */
    private EndorsementRequestResponse applyApproval(EndorsementRequest entity, User dean,
            ProposalAnnouncement announcement, String endorserName, String designation,
            LocalDate endorsementDate, String documentCode, StoredFile letterFile,
            StoredFile signatureFile) {

        entity.setStatus(EndorsementRequest.STATUS_APPROVED);
        entity.setDecidedByUserId(dean.getId());
        entity.setDecidedByName(endorserName);
        entity.setDecidedByDesignation(designation);
        entity.setDecidedAt(LocalDateTime.now());
        entity.setEndorsementDate(endorsementDate);
        entity.setRejectionReason(null);
        entity.setLetterDocumentCode(documentCode);
        entity.setLetterFileName(letterFile.originalName());
        entity.setLetterFilePath(letterFile.storedPath());
        entity.setLetterVersion(entity.getLetterVersion() == null ? 1 : entity.getLetterVersion());
        entity.setLetterGeneratedAt(LocalDateTime.now());
        entity.setSignatureFileName(signatureFile.originalName());
        entity.setSignatureFilePath(signatureFile.storedPath());

        EndorsementRequest saved = requestRepository.save(entity);
        audit(saved, EndorsementRequestEvent.TYPE_LETTER_GENERATED, dean,
                "Official endorsement letter " + documentCode + " generated (version "
                        + saved.getLetterVersion() + ").");
        audit(saved, EndorsementRequestEvent.TYPE_APPROVED, dean,
                "Approved by " + endorserName + " (" + designation + ").");

        Certification certification = certificationService.issueEndorsementLetter(saved);
        saved.setCertificationId(certification.getId());
        saved = requestRepository.save(saved);
        audit(saved, EndorsementRequestEvent.TYPE_CERTIFICATION_CREATED, dean,
                "The issued letter is available in " + saved.getProponent().getName()
                        + "'s Certifications section.");

        notifyUser(saved.getProponent(), saved, "Endorsement approved",
                saved.getReferenceNumber() + " was endorsed by " + endorserName
                        + ". The official endorsement letter is now in your Certifications.");
        return toResponse(saved, announcement);
    }

    // ==================== DEAN: REJECT ====================

    /**
     * Rejects a pending request. A reason is required and is stored and shown to the
     * Proponent. No endorsement letter and no Certifications entry are produced, so a
     * rejected request can never be treated as endorsed.
     */
    @Transactional
    public EndorsementRequestResponse reject(Long id, EndorsementDecisionRequest request) {
        requireRole(ROLE_DEAN, "Only an authorized Dean may decide an endorsement request.");
        Long deanId = currentUser.requireUserId();
        User dean = requireAccount(deanId);

        EndorsementRequest entity = requestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Endorsement request not found."));
        requireDeanScope(entity, dean);
        requirePending(entity, "rejected");

        String reason = trimmed(request.getRejectionReason());
        if (reason == null) {
            throw new IllegalArgumentException("A reason is required to reject an endorsement request.");
        }

        entity.setStatus(EndorsementRequest.STATUS_REJECTED);
        entity.setDecidedByUserId(deanId);
        entity.setDecidedByName(firstNonBlank(trimmed(request.getEndorserName()), dean.getName()));
        entity.setDecidedByDesignation(firstNonBlank(trimmed(request.getEndorserDesignation()),
                firstNonBlank(trimmed(dean.getDesignation()), "Dean")));
        entity.setDecidedAt(LocalDateTime.now());
        entity.setRejectionReason(reason);

        EndorsementRequest saved = requestRepository.save(entity);
        audit(saved, EndorsementRequestEvent.TYPE_REJECTED, dean,
                "Rejected by " + saved.getDecidedByName() + ": " + reason);
        notifyUser(saved.getProponent(), saved, "Endorsement request rejected",
                saved.getReferenceNumber() + " was not endorsed: " + reason);
        return toResponse(saved);
    }

    // ==================== DEAN / ADMIN: REISSUE ====================

    /**
     * Explicitly reissues an issued letter as a new version. The official record is
     * never silently overwritten: the new PDF is written to a new file, the version is
     * incremented, and the superseded file is left in place and named in the audit
     * trail. Only the Dean who issued the letter or an RPS administrator may do this.
     */
    @Transactional
    public EndorsementRequestResponse reissue(Long id, EndorsementDecisionRequest request) {
        Long actorId = currentUser.requireUserId();
        String role = currentUser.getRole();
        User actor = requireAccount(actorId);

        EndorsementRequest entity = requestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Endorsement request not found."));

        boolean administrator = ROLE_ADMIN.equalsIgnoreCase(role);
        boolean issuingDean = ROLE_DEAN.equalsIgnoreCase(role)
                && actorId.equals(entity.getDecidedByUserId());
        if (!administrator && !issuingDean) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the Dean who issued the letter or an RPS administrator may reissue it.");
        }
        if (!EndorsementRequest.STATUS_APPROVED.equals(entity.getStatus())) {
            throw new IllegalStateException(
                    "Only an approved endorsement letter can be reissued.");
        }

        ProposalAnnouncement announcement = announcementRepository
                .findById(entity.getProposalAnnouncementId())
                .orElseThrow(() -> new IllegalStateException(
                        "The Call for Proposal of this request no longer exists."));
        EndorsementLetterTemplates.requireImplemented(announcement.getFundingProgram());
        EndorsementLetterSettings settings = settingsService.requireComplete();

        Long signerId = entity.getDecidedByUserId();
        byte[] signature = signerId == null ? null : readSignature(signerId);
        if (signature == null || signature.length == 0) {
            throw new IllegalStateException(
                    "The signature the letter was signed with is no longer available, so the letter "
                            + "cannot be reissued. The Dean must save an electronic signature again.");
        }

        String endorserName = firstNonBlank(trimmed(request.getEndorserName()),
                entity.getDecidedByName());
        String designation = firstNonBlank(trimmed(request.getEndorserDesignation()),
                entity.getDecidedByDesignation());
        LocalDate endorsementDate = request.getEndorsementDate() != null
                ? request.getEndorsementDate()
                : entity.getEndorsementDate();
        int nextVersion = (entity.getLetterVersion() == null ? 1 : entity.getLetterVersion()) + 1;

        EndorsementLetterService.GeneratedLetter letter = letterService.generate(
                new EndorsementLetterService.LetterContext(settings, entity.getReferenceNumber(),
                        entity.getCollege(), endorsementDate, entity.getProposalTitle(),
                        entity.getProponent().getName(), entity.getDepartment(), endorserName,
                        designation, signature));

        StoredFile newFile = storeFile(letter.content(), "endorsement-letter.pdf");
        String supersededPath = entity.getLetterFilePath();
        Integer supersededVersion = entity.getLetterVersion();
        try {
            entity.setLetterFileName(newFile.originalName());
            entity.setLetterFilePath(newFile.storedPath());
            entity.setLetterVersion(nextVersion);
            entity.setLetterGeneratedAt(LocalDateTime.now());
            entity.setEndorsementDate(endorsementDate);
            entity.setDecidedByName(endorserName);
            entity.setDecidedByDesignation(designation);

            EndorsementRequest saved = requestRepository.save(entity);
            audit(saved, EndorsementRequestEvent.TYPE_LETTER_REISSUED, actor,
                    "Letter reissued as version " + nextVersion + " by " + actor.getName() + " ("
                            + role + "). Superseded version " + supersededVersion
                            + " remains stored at " + supersededPath + ".");
            certificationService.refreshDocument(saved);
            notifyUser(saved.getProponent(), saved, "Endorsement letter reissued",
                    "The endorsement letter of " + saved.getReferenceNumber()
                            + " was reissued (version " + nextVersion + ").");
            return toResponse(saved, announcement);
        } catch (RuntimeException e) {
            deleteQuietly(newFile.storedPath());
            throw e;
        }
    }

    // ==================== PROPONENT: CONTINUE TO SUBMISSION ====================

    /**
     * Links an approved endorsement to the proposal the Proponent continued to. The
     * association is the {@code proposals.endorsement_request_id} foreign key, never a
     * title match, and one endorsement can back at most one proposal.
     */
    @Transactional
    public EndorsementRequestResponse linkProposal(Long id, Long proposalId) {
        if (proposalId == null) {
            throw new IllegalArgumentException("A proposal id is required.");
        }
        Long userId = currentUser.requireUserId();
        String role = currentUser.getRole();
        EndorsementRequest entity = requestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Endorsement request not found."));

        boolean owner = entity.getProponent() != null
                && userId.equals(entity.getProponent().getId());
        if (!owner && !isAdminOrStaff(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to link this endorsement.");
        }
        if (proposalId.equals(entity.getProposalId())) {
            // Already linked to exactly this proposal: idempotent, nothing to do.
            return toResponse(entity);
        }
        if (entity.getProposalId() != null) {
            throw new IllegalStateException(
                    "This endorsement is already linked to another proposal.");
        }
        if (!EndorsementRequest.STATUS_APPROVED.equals(entity.getStatus())) {
            throw new IllegalStateException("Only an approved endorsement can be linked to a proposal.");
        }

        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Proposal not found."));
        if (proposal.getProponent() == null
                || !proposal.getProponent().getId().equals(entity.getProponent().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "The proposal belongs to another account.");
        }
        requestRepository.findByProposalId(proposalId).ifPresent(other -> {
            throw new IllegalStateException("That proposal is already linked to endorsement "
                    + other.getReferenceNumber() + ".");
        });

        entity.setProposalId(proposalId);
        proposal.setEndorsementRequestId(entity.getId());
        proposalRepository.save(proposal);
        EndorsementRequest saved = requestRepository.save(entity);
        certificationService.linkProposal(saved);

        String reference = proposal.getDocumentId() != null
                ? proposal.getDocumentId()
                : String.valueOf(proposalId);
        audit(saved, EndorsementRequestEvent.TYPE_PROPOSAL_LINKED, requireAccount(userId),
                "Linked to proposal " + reference + ".");
        return toResponse(saved);
    }

    // ==================== AUTHORIZATION ====================

    private void requireRole(String role, String message) {
        if (!role.equalsIgnoreCase(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private User requireAccount(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account not found."));
    }

    /**
     * The request, when the caller is allowed to see it: the owning Proponent, a Dean
     * inside whose scope it falls, or RPS staff / administrator.
     */
    private EndorsementRequest requireVisible(Long id) {
        EndorsementRequest entity = requestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Endorsement request not found."));
        String role = currentUser.getRole();
        Long userId = currentUser.requireUserId();

        if (isAdminOrStaff(role)) {
            return entity;
        }
        if (isDean(role)) {
            User dean = requireAccount(userId);
            boolean assignedToMe = entity.getAssignedDean() != null
                    && userId.equals(entity.getAssignedDean().getId());
            if (assignedToMe || inScope(dean, entity)) {
                return entity;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This endorsement request is outside your authorized scope.");
        }
        if (isProponent(role) && entity.getProponent() != null
                && userId.equals(entity.getProponent().getId())) {
            return entity;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You are not authorized to access this endorsement request.");
    }

    /** A Dean may only decide requests inside their authorized scope. */
    private void requireDeanScope(EndorsementRequest entity, User dean) {
        boolean assignedToMe = entity.getAssignedDean() != null
                && dean.getId().equals(entity.getAssignedDean().getId());
        if (!assignedToMe && !inScope(dean, entity)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This endorsement request is outside your authorized scope.");
        }
    }

    /**
     * True when the request falls inside the Dean's authorized college. A Dean with no
     * college configured is the institution-wide fallback and may decide any request;
     * that fallback is recorded in the audit trail when the request is routed.
     */
    private boolean inScope(User dean, EndorsementRequest entity) {
        String scope = dean.getAssignedCollege();
        if (scope == null || scope.isBlank()) {
            return true;
        }
        String college = entity.getCollege();
        return college != null && scope.trim().equalsIgnoreCase(college.trim());
    }

    private void requirePending(EndorsementRequest entity, String action) {
        if (!EndorsementRequest.STATUS_PENDING.equals(entity.getStatus())) {
            throw new IllegalStateException("This endorsement request is already "
                    + entity.getStatus().toLowerCase(Locale.ROOT)
                    + " and can no longer be " + action + ".");
        }
    }

    private static boolean isDean(String role) {
        return ROLE_DEAN.equalsIgnoreCase(role);
    }

    private static boolean isProponent(String role) {
        return ROLE_PROPONENT.equalsIgnoreCase(role);
    }

    private static boolean isAdminOrStaff(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role) || ROLE_STAFF.equalsIgnoreCase(role);
    }

    /** A trimmed value, or null when it is missing or blank. */
    private static String trimmed(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred == null ? fallback : preferred;
    }

    // ==================== REFERENCE NUMBER ====================

    /**
     * Allocates the next endorsement reference from the PostgreSQL sequence, so two
     * simultaneous requests can never receive the same reference.
     */
    private String nextReference() {
        ensureSequence();
        Long value = jdbcTemplate.queryForObject("SELECT nextval('endorsement_code_seq')", Long.class);
        long code = value == null ? 1L : value;
        return String.format(REFERENCE_FORMAT, code, LocalDate.now().getYear());
    }

    /**
     * Creates the sequence when it is missing. It is normally created by the
     * migration and by {@code EndorsementCodeSequenceInitializer}; this is the
     * belt-and-braces path for a database provisioned without either.
     */
    private void ensureSequence() {
        if (sequenceChecked) {
            return;
        }
        try {
            jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS endorsement_code_seq");
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "The endorsement reference sequence could not be prepared: " + e.getMessage(), e);
        }
        sequenceChecked = true;
    }

    // ==================== FILES ====================

    /**
     * Stores generated bytes under {@code uploads/endorsements/} with a generated
     * name, following the existing upload convention: the stored path is relative to
     * the uploads directory and the original name is kept for display.
     */
    private StoredFile storeFile(byte[] content, String originalName) {
        try {
            Path directory = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String storedName = UUID.randomUUID() + "_" + originalName;
            Path target = directory.resolve(storedName).normalize();
            if (!target.startsWith(directory)) {
                throw new IllegalStateException("The endorsement letter could not be stored.");
            }
            Files.write(target, content);
            String relative = UPLOAD_ROOT.relativize(target).toString().replace('\\', '/');
            return new StoredFile(originalName, relative);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "The endorsement letter could not be stored: " + e.getMessage(), e);
        }
    }

    /**
     * Copies the Dean's authorized signature into the endorsement upload directory, so
     * the issued letter keeps the exact image it was signed with even if the account
     * later replaces its signature.
     */
    private StoredFile storeSignatureSnapshot(Long deanId, byte[] signature) {
        String originalName = signatureNameOf(deanId);
        return storeFile(signature, originalName);
    }

    private String signatureNameOf(Long userId) {
        UserSignatureService.SignatureFile stored = signatureService.loadSignature(userId);
        if (stored == null || stored.fileName() == null || stored.fileName().isBlank()) {
            return "signature.png";
        }
        return stored.fileName();
    }

    /** The Dean's stored signature bytes, or null when none is saved. */
    private byte[] readSignature(Long userId) {
        UserSignatureService.SignatureFile stored = signatureService.loadSignature(userId);
        if (stored == null) {
            return null;
        }
        try {
            Resource resource = stored.resource();
            return resource.getInputStream().readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    private void deleteQuietly(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }
        try {
            Path resolved = UPLOAD_ROOT.resolve(storedPath).normalize();
            if (resolved.startsWith(UPLOAD_ROOT)) {
                Files.deleteIfExists(resolved);
            }
        } catch (IOException ignored) {
            // The transaction already failed; a leftover file is not worth masking it.
        }
    }

    /** A stored file: the name to display and the path relative to uploads. */
    private record StoredFile(String originalName, String storedPath) {
    }

    /** Display name of an account, for audit records. */
    public String accountName(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).map(User::getName).orElse(null);
    }

    // ==================== AUDIT & NOTIFICATIONS ====================

    /** Appends one entry to the request's audit trail. Rows are never modified. */
    private void audit(EndorsementRequest entity, String eventType, User actor, String detail) {
        EndorsementRequestEvent event = new EndorsementRequestEvent();
        event.setEndorsementRequestId(entity.getId());
        event.setEventType(eventType);
        if (actor != null) {
            event.setActorId(actor.getId());
            event.setActorName(actor.getName());
            event.setActorRole(actor.getRole());
        }
        event.setDetail(detail);
        eventRepository.save(event);
    }

    /**
     * Raises an in-app notification. A notification is a courtesy: it must never fail
     * an action that has already issued the official letter.
     */
    private void notifyUser(User recipient, EndorsementRequest entity, String title, String message) {
        if (recipient == null || recipient.getId() == null) {
            return;
        }
        try {
            notificationService.createNotification(recipient.getId(), message, title, "ENDORSEMENT",
                    null, entity.getProposalAnnouncementId());
        } catch (RuntimeException e) {
            log.warn("Endorsement notification for account {} could not be created: {}",
                    recipient.getId(), e.getMessage());
        }
    }

    // ==================== READ MODELS ====================

    private EndorsementRequestResponse toResponse(EndorsementRequest entity) {
        return toResponse(entity, null);
    }

    private EndorsementRequestResponse toResponse(EndorsementRequest entity,
            ProposalAnnouncement knownAnnouncement) {
        ProposalAnnouncement announcement = knownAnnouncement;
        if (announcement == null && entity.getProposalAnnouncementId() != null) {
            announcement = announcementRepository.findById(entity.getProposalAnnouncementId())
                    .orElse(null);
        }
        return buildResponse(entity, announcement);
    }

    /** Read models of a page of requests, with the calls loaded in one batch. */
    private List<EndorsementRequestResponse> toResponses(List<EndorsementRequest> requests) {
        Map<Long, ProposalAnnouncement> announcements = new HashMap<>();
        List<Long> ids = requests.stream()
                .map(EndorsementRequest::getProposalAnnouncementId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!ids.isEmpty()) {
            for (ProposalAnnouncement announcement : announcementRepository.findAllById(ids)) {
                announcements.put(announcement.getId(), announcement);
            }
        }
        List<EndorsementRequestResponse> responses = new ArrayList<>();
        for (EndorsementRequest request : requests) {
            responses.add(buildResponse(request,
                    announcements.get(request.getProposalAnnouncementId())));
        }
        return responses;
    }

    /** Builds the read model of one request. */
    private EndorsementRequestResponse buildResponse(EndorsementRequest entity,
            ProposalAnnouncement announcement) {
        EndorsementRequestResponse response = new EndorsementRequestResponse();
        response.setId(entity.getId());
        response.setReferenceNumber(entity.getReferenceNumber());
        response.setStatus(entity.getStatus());
        response.setProposalTitle(entity.getProposalTitle());
        response.setCollege(entity.getCollege());
        response.setDepartment(entity.getDepartment());
        response.setResearchSummary(entity.getResearchSummary());
        response.setNotes(entity.getNotes());
        response.setProposalAnnouncementId(entity.getProposalAnnouncementId());

        if (announcement != null) {
            response.setProposalAnnouncementName(announcement.getName());
            response.setFundingProgram(
                    EndorsementLetterTemplates.normalize(announcement.getFundingProgram()));
            response.setFundingProgramLabel(
                    EndorsementLetterTemplates.labelFor(announcement.getFundingProgram()));
            response.setLetterTemplateAvailable(
                    EndorsementLetterTemplates.isImplemented(announcement.getFundingProgram()));
        } else {
            response.setFundingProgram(EndorsementLetterTemplates.PROGRAM_IFP);
            response.setFundingProgramLabel(EndorsementLetterTemplates.UNSPECIFIED_LABEL);
            response.setLetterTemplateAvailable(false);
        }

        User proponent = entity.getProponent();
        if (proponent != null) {
            response.setProponentId(proponent.getId());
            response.setProponentName(proponent.getName());
            response.setProponentEmail(proponent.getEmail());
        }
        if (entity.getAssignedDean() != null) {
            response.setAssignedDeanId(entity.getAssignedDean().getId());
            response.setAssignedDeanName(entity.getAssignedDean().getName());
        }

        response.setCreatedAt(entity.getCreatedAt());
        response.setDecidedByUserId(entity.getDecidedByUserId());
        response.setDecidedByName(entity.getDecidedByName());
        response.setDecidedByDesignation(entity.getDecidedByDesignation());
        response.setDecidedAt(entity.getDecidedAt());
        response.setEndorsementDate(entity.getEndorsementDate());
        response.setRejectionReason(entity.getRejectionReason());
        response.setLetterDocumentCode(entity.getLetterDocumentCode());
        response.setLetterFileName(entity.getLetterFileName());
        response.setLetterVersion(entity.getLetterVersion());
        response.setHasLetter(entity.hasIssuedLetter());
        response.setHasSignature(entity.getSignatureFilePath() != null
                && !entity.getSignatureFilePath().isBlank());
        response.setProposalId(entity.getProposalId());
        response.setCertificationId(entity.getCertificationId());

        boolean pending = EndorsementRequest.STATUS_PENDING.equals(entity.getStatus());
        response.setCanApprove(pending && response.isLetterTemplateAvailable());
        response.setCanReject(pending);
        return response;
    }
}
