package com.rpis.backend.service;

import com.rpis.backend.dto.OvcafProposalFilterOptions;
import com.rpis.backend.dto.OvcafProposalPageResponse;
import com.rpis.backend.dto.OvcafProposalResponse;
import com.rpis.backend.dto.OvcafReportsSummaryResponse;
import com.rpis.backend.dto.OvcafValidationRequest;
import com.rpis.backend.model.OvcafValidation;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalBudget;
import com.rpis.backend.model.ProposalStatusHistory;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.OvcafValidationRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.ProposalStatusHistoryRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.util.NotificationEvent;
import com.rpis.backend.util.NotificationTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OvcafService {

    /**
     * Proposals OVCAF may open. A draft (or a proposal still in an earlier review stage)
     * must not be reachable through the OVCAF detail route, even by guessing its id.
     * The allow-list lives on {@link CurrentUser} so the proposal routes agree.
     */
    private static final Set<String> OVCAF_DETAIL_STATUSES = CurrentUser.OVCAF_VISIBLE_STATUSES;

    /** The statuses the proposals page lists, in workflow order. */
    private static final List<String> OVCAF_LIST_STATUSES = List.of(
            "FOR_OVCAF_APPROVAL", "READY_FOR_RELEASE", "APPROVED", "RELEASED");

    /** The six financial-validation checklist items and the label shown to the reviewer. */
    private static final Map<String, String> CHECKLIST_LABELS;
    static {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("budgetComplete", "Line-Item Budget is complete and matches all pages");
        labels.put("budgetComputationCorrect", "Budget computations are mathematically correct");
        labels.put("supportingDocsComplete", "All supporting financial files are attached and verified");
        labels.put("expensesReasonable", "Proposed expenses are reasonable and justified");
        labels.put("fundingSourceIdentified", "Funding source is officially identified and compliant");
        labels.put("complianceVerified",
                "Compliance with standard government/university funding guidelines verified");
        CHECKLIST_LABELS = Collections.unmodifiableMap(labels);
    }

    private static final String CHECKLIST_INCOMPLETE_MESSAGE =
            "Complete every financial validation checklist item before approving. Missing: ";
    private static final String REMARKS_REQUIRED_MESSAGE =
            "Remarks are required when returning or rejecting a proposal.";
    private static final int REMARKS_MAX_LENGTH = 2000;

    private final ProposalRepository proposalRepository;
    private final OvcafValidationRepository validationRepository;
    private final UserRepository userRepository;
    private final ProposalStatusHistoryRepository statusHistoryRepository;
    private final NotificationService notificationService;
    private final ProposalNotificationPolicy notificationPolicy;
    private final CurrentUser currentUser;

    /**
     * Get all proposals that are pending OVCAF review
     * Statuses: FOR_OVCAF_APPROVAL, APPROVED (awaiting budget processing)
     */
    public List<OvcafProposalResponse> getProposalsForOvcaf() {
        List<Proposal> proposals = loadOvcafVisibleProposals();
        Map<Long, LocalDateTime> receipts = receiptDates(proposals);
        proposals.sort(receivedAtComparator(receipts, true)
                .thenComparing(Proposal::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        return proposals.stream()
                .map(proposal -> mapToResponse(proposal, receivedAtOf(proposal, receipts)))
                .collect(Collectors.toList());
    }

    /**
     * Get proposals pending validation (FOR_OVCAF_APPROVAL status only)
     */
    public List<OvcafProposalResponse> getPendingValidationProposals() {
        List<Proposal> proposals = proposalRepository.findByStatus("FOR_OVCAF_APPROVAL").stream()
                .filter(OvcafService::hasCompletedOcApproval)
                .collect(Collectors.toCollection(ArrayList::new));
        Map<Long, LocalDateTime> receipts = receiptDates(proposals);
        proposals.sort(receivedAtComparator(receipts, true)
                .thenComparing(Proposal::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        return proposals.stream()
                .map(proposal -> mapToResponse(proposal, receivedAtOf(proposal, receipts)))
                .collect(Collectors.toList());
    }

    /**
     * One page of the OVCAF proposals page. Filtering, ordering and pagination happen on
     * the server so the page shows the same data regardless of client state.
     */
    @Transactional(readOnly = true)
    public OvcafProposalPageResponse getProposalPage(String search, String status, String college,
            String program, String sortBy, String sortDir, int page, int size) {
        List<Proposal> visible = loadOvcafVisibleProposals();
        Map<Long, LocalDateTime> receipts = receiptDates(visible);
        OvcafProposalFilterOptions options = filterOptions(visible);

        String keyword = trimToNull(search) == null ? null : search.trim().toLowerCase(Locale.ROOT);
        String statusFilter = trimToNull(status);
        String collegeFilter = trimToNull(college);
        String programFilter = trimToNull(program);

        List<Proposal> filtered = visible.stream()
                .filter(p -> statusFilter == null
                        || normaliseStatus(p.getStatus()).equals(normaliseStatus(statusFilter)))
                .filter(p -> collegeFilter == null || matches(p.getCollege(), collegeFilter))
                .filter(p -> programFilter == null || matches(p.getProgramTitle(), programFilter))
                .filter(p -> keyword == null || matchesSearch(p, keyword))
                .collect(Collectors.toCollection(ArrayList::new));

        boolean descending = !"asc".equalsIgnoreCase(trimToNull(sortDir) == null ? "desc" : sortDir.trim());
        filtered.sort(comparatorFor(sortBy, receipts, descending)
                .thenComparing(Proposal::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        int safeSize = Math.min(Math.max(size, 1), 100);
        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) safeSize));
        int safePage = Math.min(Math.max(page, 1), totalPages);
        int from = Math.min((safePage - 1) * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);

        List<OvcafProposalResponse> items = filtered.subList(from, to).stream()
                .map(proposal -> mapToResponse(proposal, receivedAtOf(proposal, receipts)))
                .collect(Collectors.toList());

        OvcafProposalPageResponse response = new OvcafProposalPageResponse();
        response.setItems(items);
        response.setPage(safePage);
        response.setSize(safeSize);
        response.setTotalItems(totalItems);
        response.setTotalPages(totalPages);
        response.setFilterOptions(options);
        return response;
    }

    /** The OVCAF-visible proposals, newest receipt first. */
    private List<Proposal> loadOvcafVisibleProposals() {
        return proposalRepository.findByStatusIn(OVCAF_LIST_STATUSES).stream()
                // A proposal only becomes OVCAF's concern after the Office of the
                // Chancellor has recorded its final approval and Special Order. A record
                // that merely carries an OVCAF-adjacent status (for example one advanced
                // by hand while its OC approval failed) must not appear as received.
                .filter(OvcafService::hasCompletedOcApproval)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** Real receipt events; the first (oldest) row per proposal wins. */
    private Map<Long, LocalDateTime> receiptDates(List<Proposal> proposals) {
        if (proposals.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = proposals.stream().map(Proposal::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, LocalDateTime> receipts = new HashMap<>();
        for (ProposalStatusHistory row : statusHistoryRepository.findOvcafReceiptEvents(ids)) {
            if (row.getProposal() != null && row.getProposal().getId() != null) {
                receipts.putIfAbsent(row.getProposal().getId(), row.getChangedAt());
            }
        }
        return receipts;
    }

    /** The actual moment OVCAF received the proposal; SO upload is the recorded fallback. */
    private static LocalDateTime receivedAtOf(Proposal proposal, Map<Long, LocalDateTime> receipts) {
        LocalDateTime event = proposal.getId() == null ? null : receipts.get(proposal.getId());
        return event != null ? event : proposal.getSoUploadedAt();
    }

    private LocalDateTime receivedAtFor(Proposal proposal) {
        if (proposal == null || proposal.getId() == null) {
            return proposal == null ? null : proposal.getSoUploadedAt();
        }
        List<ProposalStatusHistory> rows =
                statusHistoryRepository.findOvcafReceiptEvents(List.of(proposal.getId()));
        if (!rows.isEmpty()) {
            return rows.get(0).getChangedAt();
        }
        return proposal.getSoUploadedAt();
    }

    private static Comparator<Proposal> receivedAtComparator(Map<Long, LocalDateTime> receipts,
            boolean descending) {
        Comparator<LocalDateTime> order =
                descending ? Comparator.reverseOrder() : Comparator.naturalOrder();
        return Comparator.comparing(
                (Proposal p) -> receivedAtOf(p, receipts),
                Comparator.nullsLast(order));
    }

    private Comparator<Proposal> comparatorFor(String sortBy, Map<Long, LocalDateTime> receipts,
            boolean descending) {
        String key = trimToNull(sortBy) == null ? "receivedAt" : sortBy.trim();
        return switch (key) {
            case "documentId" -> textComparator(Proposal::getDocumentId, descending);
            case "projectTitle" -> textComparator(Proposal::getProjectTitle, descending);
            case "projectLeader" -> textComparator(Proposal::getProjectLeader, descending);
            case "college" -> textComparator(Proposal::getCollege, descending);
            case "status" -> textComparator(Proposal::getStatus, descending);
            default -> receivedAtComparator(receipts, descending);
        };
    }

    private static Comparator<Proposal> textComparator(java.util.function.Function<Proposal, String> value,
            boolean descending) {
        Comparator<String> order = descending ? Comparator.reverseOrder() : Comparator.naturalOrder();
        return Comparator.comparing(
                proposal -> {
                    String text = value.apply(proposal);
                    return text == null ? null : text.toLowerCase(Locale.ROOT);
                },
                Comparator.nullsLast(order));
    }

    private static boolean matchesSearch(Proposal proposal, String keyword) {
        return contains(proposal.getDocumentId(), keyword)
                || contains(proposal.getProposalCode(), keyword)
                || contains(proposal.getProjectTitle(), keyword)
                || contains(proposal.getProjectLeader(), keyword)
                || contains(proposal.getCollege(), keyword)
                || contains(proposal.getProgramTitle(), keyword);
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static boolean matches(String value, String expected) {
        return value != null && value.trim().equalsIgnoreCase(expected.trim());
    }

    private static OvcafProposalFilterOptions filterOptions(List<Proposal> proposals) {
        Set<String> statuses = new LinkedHashSet<>();
        for (String status : OVCAF_LIST_STATUSES) {
            if (proposals.stream().anyMatch(p -> status.equals(normaliseStatus(p.getStatus())))) {
                statuses.add(status);
            }
        }
        List<String> colleges = proposals.stream()
                .map(Proposal::getCollege)
                .map(OvcafService::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<String> programs = proposals.stream()
                .map(Proposal::getProgramTitle)
                .map(OvcafService::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return new OvcafProposalFilterOptions(new ArrayList<>(statuses), colleges, programs);
    }

    /**
     * Get complete proposal details for OVCAF review
     */
    public OvcafProposalResponse getProposalDetails(Long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));
        // Same visibility rule as the listing: a draft or a proposal that has not reached
        // OVCAF is reported as not found, so its content is never exposed through this route.
        if (proposal.getStatus() == null
                || !OVCAF_DETAIL_STATUSES.contains(proposal.getStatus().toUpperCase(Locale.ROOT))
                || !hasCompletedOcApproval(proposal)) {
            throw new IllegalArgumentException("Proposal not found: " + proposalId);
        }
        return mapToResponse(proposal, receivedAtFor(proposal));
    }

    /**
     * Submit validation decision for a proposal
     */
    @Transactional
    public OvcafValidation submitValidation(Long proposalId, OvcafValidationRequest request) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));

        // OVCAF's decision is a separate institutional act from the Chancellor's. It is
        // only valid for a proposal whose OC final approval and SO have been recorded,
        // and only while the proposal is actually awaiting budget evaluation - so a
        // repeated or stale request can never overwrite an earlier OVCAF record or
        // advance a proposal that has already moved on.
        requireCompletedOcApproval(proposal);
        requireAwaitingBudgetEvaluation(proposal);

        String decision = request.getDecision();
        if (!"APPROVED_ENDORSED".equals(decision)
                && !"RETURNED_FOR_REVISION".equals(decision)
                && !"REJECTED".equals(decision)) {
            throw new IllegalArgumentException(
                    "Unknown OVCAF decision. Use APPROVED_ENDORSED, RETURNED_FOR_REVISION or REJECTED.");
        }

        String remarks = normaliseRemarks(request.getRemarks());

        // The checklist certifies a budget that is being approved; it is not a
        // precondition for returning or rejecting an incomplete budget, which needs a
        // written reason instead. The backend enforces both rules independently of the
        // form.
        if ("APPROVED_ENDORSED".equals(decision)) {
            requireIssuedSpecialOrder(proposal);
            requireCompleteChecklist(request);
        } else {
            requireRemarks(remarks);
        }

        User validator = null;
        if (request.getValidatedById() != null) {
            validator = userRepository.findById(request.getValidatedById())
                    .orElse(null);
        }

        String fromStatus = normaliseStatus(proposal.getStatus());
        String returnedByName = actorName(validator, request.getValidatedById());

        OvcafValidation validation = new OvcafValidation();
        validation.setProposal(proposal);
        validation.setValidatedBy(validator);
        validation.setValidatedAt(LocalDateTime.now());
        validation.setDecision(request.getDecision());
        validation.setRemarks(remarks);

        // Set checklist values
        validation.setBudgetComplete(Boolean.TRUE.equals(request.getBudgetComplete()));
        validation.setBudgetComputationCorrect(Boolean.TRUE.equals(request.getBudgetComputationCorrect()));
        validation.setSupportingDocsComplete(Boolean.TRUE.equals(request.getSupportingDocsComplete()));
        validation.setExpensesReasonable(Boolean.TRUE.equals(request.getExpensesReasonable()));
        validation.setFundingSourceIdentified(Boolean.TRUE.equals(request.getFundingSourceIdentified()));
        validation.setComplianceVerified(Boolean.TRUE.equals(request.getComplianceVerified()));

        // Update proposal status based on decision
        switch (decision) {
            case "APPROVED_ENDORSED":
                // The budget is approved, but the project is not yet approved for
                // implementation: the budget release it requires has not been recorded.
                // READY_FOR_RELEASE is the existing intermediate state between the two,
                // and only releaseFunds() performs the final transition to APPROVED.
                validation.setBudgetEndorsed(true);
                validation.setBudgetEndorsedAt(LocalDateTime.now());
                proposal.setStatus("READY_FOR_RELEASE");
                break;
            case "RETURNED_FOR_REVISION":
                // The reviewing office returns the proposal to ORPS, which owns the
                // revision hand-off to the proponent. This is the same routing the
                // OVCRIGE return uses, so the record enters the existing RPS queue.
                proposal.setStatus("RETURNED_TO_RPS");
                proposal.setRemarks(remarks);
                applyReturnRouting(proposal, "REVISION", remarks, validator, request.getValidatedById(),
                        returnedByName);
                break;
            case "REJECTED":
                // A rejection is also a hand-off: ORPS records the final decision and
                // notifies the proponent. It is never a direct OVCAF -> Proponent move.
                proposal.setStatus("RETURNED_TO_RPS");
                proposal.setRemarks(remarks);
                applyReturnRouting(proposal, "REJECTION", remarks, validator, request.getValidatedById(),
                        returnedByName);
                break;
            default:
                break;
        }

        proposalRepository.save(proposal);
        OvcafValidation saved = validationRepository.save(validation);

        String toStatus = normaliseStatus(proposal.getStatus());
        if (!toStatus.equals(fromStatus)) {
            recordStatusChange(proposal, fromStatus, toStatus, remarks, validator,
                    request.getValidatedById());
        }

        // The notification recipients follow the hand-off: an approval tells the
        // proponent, ORPS and OC; a return/rejection tells ORPS only, because ORPS owns
        // the next step.
        switch (decision) {
            case "APPROVED_ENDORSED" -> notificationPolicy.notifyOvcafBudgetApproved(proposal);
            case "RETURNED_FOR_REVISION" -> notificationPolicy.notifyOvcafReturned(proposal, remarks);
            case "REJECTED" -> notificationPolicy.notifyRpsRejectionReturned(proposal, remarks, "OVCAF");
            default -> {
                // No decision, no notification.
            }
        }

        return saved;
    }

    /** Stores the office-return metadata the RPS revision/rejection workflow reads. */
    private void applyReturnRouting(Proposal proposal, String returnKind, String remarks,
            User actor, Long suppliedActorId, String actorName) {
        proposal.setReturnRemarks(remarks);
        proposal.setReturnedByOffice(UserRoles.OVCAF);
        proposal.setReturnKind(returnKind);
        proposal.setReturnedAt(LocalDateTime.now());
        Long actorId = actor != null ? actor.getId() : suppliedActorId;
        if (actorId == null) {
            actorId = currentUser.getUserId();
        }
        proposal.setReturnedByUserId(actorId);
        proposal.setReturnedByName(actorName);
    }

    /** The six-item checklist must be fully verified before a budget is approved. */
    private static void requireCompleteChecklist(OvcafValidationRequest request) {
        List<String> missing = new ArrayList<>();
        if (!Boolean.TRUE.equals(request.getBudgetComplete())) missing.add(CHECKLIST_LABELS.get("budgetComplete"));
        if (!Boolean.TRUE.equals(request.getBudgetComputationCorrect())) {
            missing.add(CHECKLIST_LABELS.get("budgetComputationCorrect"));
        }
        if (!Boolean.TRUE.equals(request.getSupportingDocsComplete())) {
            missing.add(CHECKLIST_LABELS.get("supportingDocsComplete"));
        }
        if (!Boolean.TRUE.equals(request.getExpensesReasonable())) missing.add(CHECKLIST_LABELS.get("expensesReasonable"));
        if (!Boolean.TRUE.equals(request.getFundingSourceIdentified())) {
            missing.add(CHECKLIST_LABELS.get("fundingSourceIdentified"));
        }
        if (!Boolean.TRUE.equals(request.getComplianceVerified())) missing.add(CHECKLIST_LABELS.get("complianceVerified"));
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(CHECKLIST_INCOMPLETE_MESSAGE + String.join("; ", missing) + ".");
        }
    }

    /** A return or rejection must state why. */
    private static void requireRemarks(String remarks) {
        if (remarks == null || remarks.isBlank()) {
            throw new IllegalArgumentException(REMARKS_REQUIRED_MESSAGE);
        }
    }

    private static String normaliseRemarks(String remarks) {
        String trimmed = trimToNull(remarks);
        if (trimmed == null) {
            return null;
        }
        return trimmed.length() > REMARKS_MAX_LENGTH
                ? trimmed.substring(0, REMARKS_MAX_LENGTH).trim()
                : trimmed;
    }

    /** The reviewer's name, with the authenticated account as the fallback. */
    private String actorName(User actor, Long suppliedActorId) {
        if (actor != null) {
            return actor.getName();
        }
        Long actorId = suppliedActorId != null ? suppliedActorId : currentUser.getUserId();
        if (actorId == null) {
            return null;
        }
        return userRepository.findById(actorId).map(User::getName).orElse(null);
    }

    /** Records one audited transition; the caller must already be in a transaction. */
    private void recordStatusChange(Proposal proposal, String fromStatus, String toStatus,
            String remarks, User actor, Long suppliedActorId) {
        ProposalStatusHistory entry = new ProposalStatusHistory();
        entry.setProposal(proposal);
        entry.setFromStatus(com.rpis.backend.util.ProposalStatus.optionalSupportedStatus(fromStatus));
        entry.setToStatus(com.rpis.backend.util.ProposalStatus.requireSupportedStatus(toStatus));
        Long actorId = actor != null ? actor.getId() : suppliedActorId;
        if (actorId == null) {
            actorId = currentUser.getUserId();
        }
        entry.setActorUserId(actorId);
        entry.setActorRole(UserRoles.OVCAF);
        entry.setActorName(actorName(actor, suppliedActorId));
        entry.setRemarks(remarks);
        entry.setSource(ProposalStatusHistory.SOURCE_APPLICATION);
        entry.setChangedAt(LocalDateTime.now());
        statusHistoryRepository.save(entry);
    }

    /**
     * Endorse budget - record OVCAF's budget approval and make the proposal ready for
     * the budget release that must precede implementation approval.
     */
    @Transactional
    public Proposal endorseBudget(Long proposalId, Long endorsedById) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));

        // Same rules as submitValidation: OVCAF may only act on a proposal whose OC
        // approval and SO are recorded, and only while it awaits budget evaluation.
        requireCompletedOcApproval(proposal);
        requireAwaitingBudgetEvaluation(proposal);

        String fromStatus = normaliseStatus(proposal.getStatus());

        // Get or create validation record
        OvcafValidation validation = validationRepository
                .findTopByProposalIdOrderByCreatedAtDesc(proposalId)
                .orElseGet(() -> {
                    OvcafValidation v = new OvcafValidation();
                    v.setProposal(proposal);
                    return v;
                });

        validation.setBudgetEndorsed(true);
        validation.setBudgetEndorsedAt(LocalDateTime.now());
        User endorser = null;
        if (validation.getDecision() == null) {
            // The endorsement is OVCAF's budget approval. Recording the decision keeps
            // the reporting-eligibility check (which requires an APPROVED_ENDORSED
            // record) consistent with the proposal's approved-for-implementation status.
            validation.setDecision("APPROVED_ENDORSED");
            validation.setValidatedAt(LocalDateTime.now());
        }
        if (endorsedById != null) {
            endorser = userRepository.findById(endorsedById).orElse(null);
            validation.setValidatedBy(endorser);
        }
        validationRepository.save(validation);

        // The budget is approved, not yet released: READY_FOR_RELEASE is the state the
        // proposal must be in before releaseFunds() records the release and moves it to
        // APPROVED (Implementation Approved).
        proposal.setStatus("READY_FOR_RELEASE");
        Proposal saved = proposalRepository.save(proposal);

        if (!"READY_FOR_RELEASE".equals(fromStatus)) {
            recordStatusChange(saved, fromStatus, "READY_FOR_RELEASE", validation.getRemarks(),
                    endorser, endorsedById);
        }

        // One approval event: proponent, ORPS and OC are told, and the dedupe key makes
        // a retried endorsement produce no second row.
        notificationPolicy.notifyOvcafBudgetApproved(saved);

        return saved;
    }

    /**
     * Authorize budget release
     */
    @Transactional
    public Proposal authorizeBudget(Long proposalId, Long authorizedById) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));
        requireCompletedOcApproval(proposal);

        // Authorization only applies to a proposal that is actually awaiting the
        // release. It must never move an Implementation Approved (APPROVED) project
        // backwards to READY_FOR_RELEASE.
        String status = normaliseStatus(proposal.getStatus());
        if (!"READY_FOR_RELEASE".equals(status)) {
            throw new IllegalStateException(
                    "This proposal is not awaiting fund release (current status: "
                            + (status.isEmpty() ? "none" : status) + ").");
        }

        OvcafValidation validation = validationRepository
                .findTopByProposalIdOrderByCreatedAtDesc(proposalId)
                .orElseGet(() -> {
                    OvcafValidation v = new OvcafValidation();
                    v.setProposal(proposal);
                    return v;
                });

        validation.setBudgetAuthorized(true);
        validation.setBudgetAuthorizedAt(LocalDateTime.now());
        validationRepository.save(validation);

        Proposal saved = proposalRepository.save(proposal);

        // Notify the finance office.
        notificationService.notifyRoles(List.of(UserRoles.OVCAF),
                "Budget authorized. Ready for fund release: \"" + saved.getProjectTitle() + "\"",
                "Budget Authorized", NotificationTypes.FUNDING, saved.getId(),
                NotificationEvent.STATUS_UPDATED);

        return saved;
    }

    /**
     * Release project funds.
     *
     * <p>This is the final step of the budget route: it may only be recorded after
     * OVCAF has approved the budget, and it is what moves the proposal to
     * {@code APPROVED} (Implementation Approved). Merely approving the budget never
     * does that on its own.</p>
     */
    @Transactional
    public Proposal releaseFunds(Long proposalId, Long releasedById) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));
        requireCompletedOcApproval(proposal);

        OvcafValidation validation = validationRepository
                .findTopByProposalIdOrderByCreatedAtDesc(proposalId)
                .orElseGet(() -> {
                    OvcafValidation v = new OvcafValidation();
                    v.setProposal(proposal);
                    return v;
                });

        // A release is never a substitute for OVCAF's budget decision: without the
        // recorded approval the proposal would be marked Implementation Approved while
        // its budget had never actually been approved.
        if (!Boolean.TRUE.equals(validation.getBudgetEndorsed())) {
            throw new IllegalStateException(
                    "OVCAF must approve the budget before funds can be released. Record the "
                            + "budget approval first; the proposal then becomes ready for release.");
        }

        String status = normaliseStatus(proposal.getStatus());
        if (!"READY_FOR_RELEASE".equals(status) && !"APPROVED".equals(status)) {
            throw new IllegalStateException(
                    "This proposal is not awaiting fund release (current status: "
                            + (status.isEmpty() ? "none" : status) + ").");
        }
        // A replay of the release is harmless. When the project is already approved the
        // only possible repair is to restore the release flags; the transition and its
        // notifications already happened and must not be repeated.
        if ("APPROVED".equals(status)) {
            if (!Boolean.TRUE.equals(validation.getFundsReleased())) {
                validation.setFundsReleased(true);
                validation.setFundsReleasedAt(LocalDateTime.now());
                validation.setReleasedBy(releasedById);
                validationRepository.save(validation);
            }
            return proposal;
        }

        validation.setFundsReleased(true);
        validation.setFundsReleasedAt(LocalDateTime.now());
        validation.setReleasedBy(releasedById);
        validationRepository.save(validation);

        // The budget release completes the approval chain: the proposal is now approved
        // for implementation. RELEASED is intentionally not the final status.
        proposal.setStatus("APPROVED");
        Proposal saved = proposalRepository.save(proposal);

        User releaser = releasedById == null ? null
                : userRepository.findById(releasedById).orElse(null);
        recordStatusChange(saved, "READY_FOR_RELEASE", "APPROVED", null, releaser, releasedById);

        // Notify proponent and RPS
        if (saved.getProponent() != null) {
            notificationService.createNotification(
                    saved.getProponent().getId(),
                    "Funds have been released for your project \"" + saved.getProjectTitle()
                            + "\". It is now approved for implementation.",
                    "Proposal Approved for Implementation",
                    NotificationTypes.APPROVAL,
                    saved.getId(), null, NotificationEvent.STATUS_UPDATED,
                    UserRoles.PROPONENT);
        }
        notifyRps(saved,
                "Funds released. Ready for implementation monitoring: \"" + saved.getProjectTitle() + "\"",
                "Implementation Monitoring", NotificationTypes.PROPOSAL_UPDATE);

        return saved;
    }

    /**
     * Get reports summary for OVCAF dashboard
     */
    public OvcafReportsSummaryResponse getReportsSummary() {
        OvcafReportsSummaryResponse response = new OvcafReportsSummaryResponse();

        // Get all OVCAF-related proposals
        List<String> ovcafStatuses = Arrays.asList(
                "FOR_OVCAF_APPROVAL",
                "APPROVED",
                "FOR_OC_APPROVAL",
                "READY_FOR_RELEASE",
                "RELEASED",
                "RETURNED_TO_RPS",
                "RETURNED",
                "REJECTED");
        // Only proposals with a completed OC approval + SO are OVCAF's to report on;
        // a record advanced without them is not counted as received from OC.
        List<Proposal> proposals = proposalRepository.findByStatusIn(ovcafStatuses).stream()
                .filter(OvcafService::hasCompletedOcApproval)
                .toList();

        // Calculate totals
        BigDecimal totalBudget = BigDecimal.ZERO;
        Map<String, Long> statusCounts = new HashMap<>();
        Map<String, BigDecimal> statusBudgets = new HashMap<>();
        Map<String, OvcafReportsSummaryResponse.CollegeSummary> collegeMap = new HashMap<>();

        // Initialize status counts
        statusCounts.put("Approved", 0L);
        statusCounts.put("Pending", 0L);
        statusCounts.put("Returned", 0L);
        statusCounts.put("Rejected", 0L);
        statusBudgets.put("Approved", BigDecimal.ZERO);
        statusBudgets.put("Pending", BigDecimal.ZERO);
        statusBudgets.put("Returned", BigDecimal.ZERO);
        statusBudgets.put("Rejected", BigDecimal.ZERO);

        for (Proposal p : proposals) {
            BigDecimal proposalBudget = calculateTotalBudget(p);
            totalBudget = totalBudget.add(proposalBudget);

            // Categorize by status
            String statusCategory = categorizeStatus(p.getStatus());
            statusCounts.merge(statusCategory, 1L, Long::sum);
            statusBudgets.merge(statusCategory, proposalBudget, BigDecimal::add);

            // College summary
            String college = p.getCollege() != null ? p.getCollege() : "Unknown";
            OvcafReportsSummaryResponse.CollegeSummary cs = collegeMap.computeIfAbsent(college, k -> {
                OvcafReportsSummaryResponse.CollegeSummary s = new OvcafReportsSummaryResponse.CollegeSummary();
                s.setCollege(college);
                s.setProposalCount(0);
                s.setTotalBudget(BigDecimal.ZERO);
                return s;
            });
            cs.setProposalCount(cs.getProposalCount() + 1);
            cs.setTotalBudget(cs.getTotalBudget().add(proposalBudget));
        }

        response.setTotalValidatedBudget(totalBudget);
        response.setTotalProposals(proposals.size());
        response.setAverageProjectBudget(proposals.isEmpty() ? BigDecimal.ZERO
                : totalBudget.divide(BigDecimal.valueOf(proposals.size()), 2, java.math.RoundingMode.HALF_UP));

        // Calculate endorsement rate
        long approvedCount = statusCounts.getOrDefault("Approved", 0L);
        long reviewedCount = approvedCount + statusCounts.getOrDefault("Returned", 0L)
                + statusCounts.getOrDefault("Rejected", 0L);
        response.setEndorsementRate(reviewedCount > 0 ? (double) approvedCount / reviewedCount * 100 : 0);

        response.setStatusCounts(statusCounts);
        response.setStatusBudgets(statusBudgets);
        response.setCollegeSummary(new ArrayList<>(collegeMap.values()));
        response.setApprovedCount(approvedCount);
        response.setPendingCount(statusCounts.getOrDefault("Pending", 0L));
        response.setReturnedCount(statusCounts.getOrDefault("Returned", 0L));
        response.setRejectedCount(statusCounts.getOrDefault("Rejected", 0L));

        return response;
    }

    // ========== Helper Methods ==========

    /**
     * True when the Office of the Chancellor has recorded its final approval and the
     * Special Order it issued on the proposal itself: an SO number and the time it
     * was recorded. Both are written in the same transaction as the OC approval, so
     * either one missing means the OC step did not complete. The workflow status is
     * deliberately not used as evidence - a proposal can carry an OVCAF-visible
     * status and still have no OC record (the historic inconsistent records).
     */
    static boolean hasCompletedOcApproval(Proposal proposal) {
        return proposal != null
                && trimToNull(proposal.getSoNumber()) != null
                && proposal.getSoUploadedAt() != null;
    }

    /**
     * OVCAF may only evaluate a proposal that is still awaiting its budget decision.
     * This is what makes a retry or a double submission harmless: it cannot create a
     * second validation record or advance an already-decided proposal.
     */
    private void requireAwaitingBudgetEvaluation(Proposal proposal) {
        String status = normaliseStatus(proposal.getStatus());
        if (!"FOR_OVCAF_APPROVAL".equals(status)) {
            throw new IllegalStateException(
                    "This proposal is not awaiting OVCAF budget evaluation "
                            + "(current status: " + (status.isEmpty() ? "none" : status) + ").");
        }
    }

    /**
     * Refuses any OVCAF action on a proposal whose OC final approval and SO issuance
     * are not complete. OVCAF approval is never a substitute for the Chancellor's.
     */
    private void requireCompletedOcApproval(Proposal proposal) {
        if (!hasCompletedOcApproval(proposal)) {
            throw new IllegalStateException(
                    "This proposal has no completed Office of the Chancellor final approval and "
                            + "Special Order (SO). It cannot be evaluated by OVCAF until the SO number "
                            + "and issuance are recorded by the Chancellor's Office.");
        }
    }

    /**
     * An implementation approval is only valid once the Office of the Chancellor has
     * issued the Special Order. Rejecting the approval here keeps every
     * "APPROVED for implementation" record consistent with the quarterly-reporting
     * rule (approved for implementation AND issued SO); the caller is told exactly
     * what is missing instead of the proposal being silently marked approved.
     */
    private void requireIssuedSpecialOrder(Proposal proposal) {
        String soNumber = proposal != null ? trimToNull(proposal.getSoNumber()) : null;
        if (soNumber == null) {
            throw new IllegalStateException(
                    "This proposal has no issued Special Order (SO). The Office of the Chancellor "
                            + "must record the SO number (Final Approval) before the proposal can be "
                            + "approved for implementation.");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** The same trim + upper-case comparison the rest of the workflow uses. */
    private static String normaliseStatus(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private OvcafProposalResponse mapToResponse(Proposal proposal, LocalDateTime receivedAt) {
        OvcafProposalResponse response = new OvcafProposalResponse();
        response.setId(proposal.getId());
        response.setDocumentId(proposal.getDocumentId());
        response.setProjectTitle(proposal.getProjectTitle());
        response.setProjectLeader(proposal.getProjectLeader());
        response.setCollege(proposal.getCollege());
        response.setProgramTitle(proposal.getProgramTitle());
        response.setStatus(proposal.getStatus());
        response.setDuration(proposal.getDuration());
        response.setStartDate(proposal.getStartDate());
        response.setEndDate(proposal.getEndDate());
        response.setExecutiveSummary(proposal.getExecutiveSummary());
        response.setRemarks(proposal.getRemarks());
        response.setCreatedAt(proposal.getCreatedAt());
        response.setUpdatedAt(proposal.getUpdatedAt());
        // The actual OVCAF receipt event (history), falling back to the OC SO timestamp.
        // It is never the draft's creation date.
        response.setReceivedAt(receivedAt);

        // Proponent info. The card and the details view must name the person typed into the
        // proposal form ("Proponent Name"), not the generic account that owns the record.
        // The form value is persisted as `projectLeader`; the account name is only a fallback
        // for records that predate the form field.
        if (proposal.getProponent() != null) {
            response.setProponentId(proposal.getProponent().getId());
        }
        String formProponent = trimToNull(proposal.getProjectLeader());
        if (formProponent != null) {
            response.setProponentName(formProponent);
        } else if (proposal.getProponent() != null) {
            response.setProponentName(proposal.getProponent().getName());
        }

        // Budget breakdown (legacy amount rows; not the section 25 source any more)
        List<ProposalBudget> budgets = proposal.getBudget();
        response.setBudgetBreakdown(budgets != null ? budgets : new ArrayList<>());
        response.setTotalBudget(calculateTotalBudget(proposal));

        // Section 25: the attached Line-Item Budget document, served by /api/files.
        response.setBeneficiariesFileName(proposal.getBeneficiariesFileName());
        response.setBeneficiariesFilePath(proposal.getBeneficiariesFilePath());

        // The Special Order already issued by OC, when there is one. Approval for
        // implementation is only valid after this exists.
        response.setSoNumber(proposal.getSoNumber());
        response.setSoFileName(proposal.getSoFileName());
        response.setSoUploadedAt(proposal.getSoUploadedAt());

        // Get latest validation if exists
        validationRepository.findTopByProposalIdOrderByCreatedAtDesc(proposal.getId())
                .ifPresent(validation -> {
                    response.setOvcafDecision(validation.getDecision());
                    response.setOvcafRemarks(validation.getRemarks());
                    response.setBudgetEndorsed(validation.getBudgetEndorsed());
                    response.setBudgetAuthorized(validation.getBudgetAuthorized());
                    response.setFundsReleased(validation.getFundsReleased());
                    response.setFundsReleasedAt(validation.getFundsReleasedAt());
                    response.setBudgetComplete(validation.getBudgetComplete());
                    response.setBudgetComputationCorrect(validation.getBudgetComputationCorrect());
                    response.setSupportingDocsComplete(validation.getSupportingDocsComplete());
                    response.setExpensesReasonable(validation.getExpensesReasonable());
                    response.setFundingSourceIdentified(validation.getFundingSourceIdentified());
                    response.setComplianceVerified(validation.getComplianceVerified());
                });

        return response;
    }

    private BigDecimal calculateTotalBudget(Proposal proposal) {
        if (proposal.getBudget() == null || proposal.getBudget().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return proposal.getBudget().stream()
                .map(b -> {
                    try {
                        return new BigDecimal(b.getTotal() != null ? b.getTotal() : "0");
                    } catch (NumberFormatException e) {
                        return BigDecimal.ZERO;
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String categorizeStatus(String status) {
        if (status == null)
            return "Pending";
        switch (status.toUpperCase()) {
            case "RELEASED":
            case "FOR_OC_APPROVAL":
            case "APPROVED":
                return "Approved";
            case "RETURNED":
            case "RETURNED_TO_RPS":
                return "Returned";
            case "REJECTED":
                return "Rejected";
            default:
                return "Pending";
        }
    }

    /** Notifies both ORPS roles; the service de-duplicates an account that holds both. */
    private void notifyRps(Proposal proposal, String message, String title, String type) {
        notificationService.notifyRoles(
                List.of(UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF),
                message, title, type, proposal.getId(), NotificationEvent.STATUS_UPDATED);
    }

    /**
     * Get validation history for a proposal
     */
    public List<OvcafValidation> getValidationHistory(Long proposalId) {
        // Verify proposal exists
        proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));
        return validationRepository.findByProposalId(proposalId);
    }
}
