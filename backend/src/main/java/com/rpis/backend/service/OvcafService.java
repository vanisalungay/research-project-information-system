package com.rpis.backend.service;

import com.rpis.backend.dto.OvcafProposalResponse;
import com.rpis.backend.dto.OvcafReportsSummaryResponse;
import com.rpis.backend.dto.OvcafValidationRequest;
import com.rpis.backend.model.OvcafValidation;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalBudget;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.OvcafValidationRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
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

    private final ProposalRepository proposalRepository;
    private final OvcafValidationRepository validationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Get all proposals that are pending OVCAF review
     * Statuses: FOR_OVCAF_APPROVAL, APPROVED (awaiting budget processing)
     */
    public List<OvcafProposalResponse> getProposalsForOvcaf() {
        List<String> ovcafStatuses = Arrays.asList(
                "FOR_OVCAF_APPROVAL",
                "APPROVED",
                "READY_FOR_RELEASE",
                "RELEASED");
        List<Proposal> proposals = proposalRepository.findByStatusIn(ovcafStatuses);
        return proposals.stream()
                // A proposal only becomes OVCAF's concern after the Office of the
                // Chancellor has recorded its final approval and Special Order. A record
                // that merely carries an OVCAF-adjacent status (for example one advanced
                // by hand while its OC approval failed) must not appear as received.
                .filter(OvcafService::hasCompletedOcApproval)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get proposals pending validation (FOR_OVCAF_APPROVAL status only)
     */
    public List<OvcafProposalResponse> getPendingValidationProposals() {
        List<Proposal> proposals = proposalRepository.findByStatus("FOR_OVCAF_APPROVAL");
        return proposals.stream()
                .filter(OvcafService::hasCompletedOcApproval)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
        return mapToResponse(proposal);
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

        User validator = null;
        if (request.getValidatedById() != null) {
            validator = userRepository.findById(request.getValidatedById())
                    .orElse(null);
        }

        OvcafValidation validation = new OvcafValidation();
        validation.setProposal(proposal);
        validation.setValidatedBy(validator);
        validation.setValidatedAt(LocalDateTime.now());
        validation.setDecision(request.getDecision());
        validation.setRemarks(request.getRemarks());

        // Set checklist values
        validation.setBudgetComplete(request.getBudgetComplete() != null ? request.getBudgetComplete() : false);
        validation.setBudgetComputationCorrect(
                request.getBudgetComputationCorrect() != null ? request.getBudgetComputationCorrect() : false);
        validation.setSupportingDocsComplete(
                request.getSupportingDocsComplete() != null ? request.getSupportingDocsComplete() : false);
        validation.setExpensesReasonable(
                request.getExpensesReasonable() != null ? request.getExpensesReasonable() : false);
        validation.setFundingSourceIdentified(
                request.getFundingSourceIdentified() != null ? request.getFundingSourceIdentified() : false);
        validation.setComplianceVerified(
                request.getComplianceVerified() != null ? request.getComplianceVerified() : false);

        // Update proposal status based on decision
        switch (request.getDecision()) {
            case "APPROVED_ENDORSED":
                // The budget is approved, but the project is not yet approved for
                // implementation: the budget release it requires has not been recorded.
                // READY_FOR_RELEASE is the existing intermediate state between the two,
                // and only releaseFunds() performs the final transition to APPROVED.
                // An implementation approval is also only valid for a project whose
                // Special Order has actually been issued by the Office of the Chancellor;
                // without the SO the proposal could never create the quarterly reports an
                // implementation-approved project must file.
                requireIssuedSpecialOrder(proposal);
                validation.setBudgetEndorsed(true);
                validation.setBudgetEndorsedAt(LocalDateTime.now());
                proposal.setStatus("READY_FOR_RELEASE");
                break;
            case "RETURNED_FOR_REVISION":
                proposal.setStatus("RETURNED");
                proposal.setRemarks(request.getRemarks());
                break;
            case "REJECTED":
                proposal.setStatus("REJECTED");
                proposal.setRemarks(request.getRemarks());
                break;
        }

        proposalRepository.save(proposal);
        OvcafValidation saved = validationRepository.save(validation);

        // Send notifications
        sendValidationNotifications(proposal, request.getDecision());

        return saved;
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
        if (validation.getDecision() == null) {
            // The endorsement is OVCAF's budget approval. Recording the decision keeps
            // the reporting-eligibility check (which requires an APPROVED_ENDORSED
            // record) consistent with the proposal's approved-for-implementation status.
            validation.setDecision("APPROVED_ENDORSED");
            validation.setValidatedAt(LocalDateTime.now());
        }
        if (endorsedById != null) {
            validation.setValidatedBy(userRepository.findById(endorsedById).orElse(null));
        }
        validationRepository.save(validation);

        // The budget is approved, not yet released: READY_FOR_RELEASE is the state the
        // proposal must be in before releaseFunds() records the release and moves it to
        // APPROVED (Implementation Approved).
        proposal.setStatus("READY_FOR_RELEASE");
        Proposal saved = proposalRepository.save(proposal);

        // Notify proponent that the budget is approved and the release is awaited.
        if (saved.getProponent() != null) {
            notificationService.createNotification(
                    saved.getProponent().getId(),
                    "The budget for your proposal \"" + saved.getProjectTitle()
                            + "\" has been approved by OVCAF and is awaiting fund release. "
                            + "The project becomes approved for implementation once the release is recorded.",
                    "Budget Approved",
                    "APPROVAL",
                    saved.getId());
        }
        // Notify RPS for monitoring
        notifyRole("RPS_ADMIN", saved,
                "Budget approved by OVCAF; awaiting fund release: \"" + saved.getProjectTitle() + "\"",
                "Budget Approved", "PROPOSAL_UPDATE");

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

        proposal.setStatus("READY_FOR_RELEASE");
        Proposal saved = proposalRepository.save(proposal);

        // Notify finance office
        notifyRole("OVCAF", saved, "Budget authorized. Ready for fund release: \"" + saved.getProjectTitle() + "\"",
                "Budget Authorized", "FUNDING");

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
        // Repeating the release of an already-approved project is harmless and must not
        // duplicate notifications; the first release is the recorded transition.
        if ("APPROVED".equals(status) && Boolean.TRUE.equals(validation.getFundsReleased())) {
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

        // Notify proponent and RPS
        if (saved.getProponent() != null) {
            notificationService.createNotification(
                    saved.getProponent().getId(),
                    "Funds have been released for your project: \"" + saved.getProjectTitle() + "\"",
                    "Funds Released",
                    "FUNDING",
                    saved.getId());
        }
        notifyRole("RPS_ADMIN", saved,
                "Funds released. Ready for implementation monitoring: \"" + saved.getProjectTitle() + "\"",
                "Implementation Monitoring", "PROPOSAL_UPDATE");

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

    private OvcafProposalResponse mapToResponse(Proposal proposal) {
        OvcafProposalResponse response = new OvcafProposalResponse();
        response.setId(proposal.getId());
        response.setDocumentId(proposal.getDocumentId());
        response.setProjectTitle(proposal.getProjectTitle());
        response.setProjectLeader(proposal.getProjectLeader());
        response.setCollege(proposal.getCollege());
        response.setStatus(proposal.getStatus());
        response.setDuration(proposal.getDuration());
        response.setStartDate(proposal.getStartDate());
        response.setEndDate(proposal.getEndDate());
        response.setExecutiveSummary(proposal.getExecutiveSummary());
        response.setRemarks(proposal.getRemarks());
        response.setCreatedAt(proposal.getCreatedAt());
        response.setUpdatedAt(proposal.getUpdatedAt());

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
                return "Returned";
            case "REJECTED":
                return "Rejected";
            default:
                return "Pending";
        }
    }

    private void sendValidationNotifications(Proposal proposal, String decision) {
        switch (decision) {
            case "APPROVED_ENDORSED":
                // The budget is approved; implementation approval follows the release.
                if (proposal.getProponent() != null) {
                    notificationService.createNotification(
                            proposal.getProponent().getId(),
                            "The budget for your proposal \"" + proposal.getProjectTitle()
                                    + "\" has been approved by OVCAF and is awaiting fund release. "
                                    + "The project becomes approved for implementation once the release is recorded.",
                            "Budget Approved",
                            "APPROVAL",
                            proposal.getId());
                }
                // Notify RPS that the budget cleared and the release is awaited.
                notifyRole("RPS_ADMIN", proposal,
                        "Budget approved by OVCAF; awaiting fund release: \"" + proposal.getProjectTitle() + "\"",
                        "Budget Approved", "PROPOSAL_UPDATE");
                break;
            case "RETURNED_FOR_REVISION":
                if (proposal.getProponent() != null) {
                    notificationService.createNotification(
                            proposal.getProponent().getId(),
                            "Your proposal \"" + proposal.getProjectTitle()
                                    + "\" has been returned by OVCAF for revision.",
                            "Budget Revision Required",
                            "REVISION",
                            proposal.getId());
                }
                notifyRole("RPS_ADMIN", proposal,
                        "Budget release was returned by OVCAF for proposal: \"" + proposal.getProjectTitle() + "\"",
                        "Budget Request Returned", "REVISION");
                break;
            case "REJECTED":
                if (proposal.getProponent() != null) {
                    notificationService.createNotification(
                            proposal.getProponent().getId(),
                            "Your proposal \"" + proposal.getProjectTitle() + "\" has been rejected by OVCAF.",
                            "Proposal Rejected",
                            "REJECTION",
                            proposal.getId());
                }
                break;
        }
    }

    private void notifyRole(String role, Proposal proposal, String message, String title, String type) {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (role.equals(user.getRole())) {
                notificationService.createNotification(user.getId(), message, title, type, proposal.getId());
            }
        }
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
