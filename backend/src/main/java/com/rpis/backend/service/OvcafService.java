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
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get proposals pending validation (FOR_OVCAF_APPROVAL status only)
     */
    public List<OvcafProposalResponse> getPendingValidationProposals() {
        List<Proposal> proposals = proposalRepository.findByStatus("FOR_OVCAF_APPROVAL");
        return proposals.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get complete proposal details for OVCAF review
     */
    public OvcafProposalResponse getProposalDetails(Long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));
        return mapToResponse(proposal);
    }

    /**
     * Submit validation decision for a proposal
     */
    @Transactional
    public OvcafValidation submitValidation(Long proposalId, OvcafValidationRequest request) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));

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
                // Budget endorsed, ready for OC approval or direct implementation
                validation.setBudgetEndorsed(true);
                validation.setBudgetEndorsedAt(LocalDateTime.now());
                proposal.setStatus("FOR_OC_APPROVAL");
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
     * Endorse budget - forward to OC for final approval
     */
    @Transactional
    public Proposal endorseBudget(Long proposalId, Long endorsedById) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));

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
        if (endorsedById != null) {
            validation.setValidatedBy(userRepository.findById(endorsedById).orElse(null));
        }
        validationRepository.save(validation);

        proposal.setStatus("FOR_OC_APPROVAL");
        Proposal saved = proposalRepository.save(proposal);

        // Notify OC
        notifyRole("OC", saved, "A proposal has been endorsed by OVCAF and requires your final approval: \""
                + saved.getProjectTitle() + "\"", "Budget Endorsed", "ENDORSEMENT");

        return saved;
    }

    /**
     * Authorize budget release
     */
    @Transactional
    public Proposal authorizeBudget(Long proposalId, Long authorizedById) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));

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
     * Release project funds
     */
    @Transactional
    public Proposal releaseFunds(Long proposalId, Long releasedById) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));

        OvcafValidation validation = validationRepository
                .findTopByProposalIdOrderByCreatedAtDesc(proposalId)
                .orElseGet(() -> {
                    OvcafValidation v = new OvcafValidation();
                    v.setProposal(proposal);
                    return v;
                });

        validation.setFundsReleased(true);
        validation.setFundsReleasedAt(LocalDateTime.now());
        validation.setReleasedBy(releasedById);
        validationRepository.save(validation);

        proposal.setStatus("RELEASED");
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
        List<Proposal> proposals = proposalRepository.findByStatusIn(ovcafStatuses);

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

    private OvcafProposalResponse mapToResponse(Proposal proposal) {
        OvcafProposalResponse response = new OvcafProposalResponse();
        response.setId(proposal.getId());
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

        // Proponent info
        if (proposal.getProponent() != null) {
            response.setProponentName(proposal.getProponent().getName());
            response.setProponentId(proposal.getProponent().getId());
        }

        // Budget breakdown
        List<ProposalBudget> budgets = proposal.getBudget();
        response.setBudgetBreakdown(budgets != null ? budgets : new ArrayList<>());
        response.setTotalBudget(calculateTotalBudget(proposal));

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
                notifyRole("OC", proposal,
                        "A proposal has been endorsed by OVCAF: \"" + proposal.getProjectTitle() + "\"",
                        "Budget Endorsed", "ENDORSEMENT");
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
