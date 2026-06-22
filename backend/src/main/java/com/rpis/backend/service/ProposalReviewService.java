package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalReview;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.ProposalReviewRepository;
import com.rpis.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProposalReviewService {

    private final ProposalReviewRepository reviewRepository;
    private final ProposalRepository proposalRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public List<ProposalReview> getReviewsForProposal(Long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));
        return reviewRepository.findByProposal(proposal);
    }

    @Transactional
    public ProposalReview submitReview(Long proposalId, Long reviewerId, ProposalReview reviewInput) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + proposalId));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found: " + reviewerId));

        ProposalReview review = new ProposalReview();
        review.setProposal(proposal);
        review.setReviewer(reviewer);
        review.setReviewerRole(reviewer.getRole());

        // Copy scores
        review.setSoundnessSectorNeedScore(reviewInput.getSoundnessSectorNeedScore());
        review.setSoundnessEffectiveSolutionScore(reviewInput.getSoundnessEffectiveSolutionScore());
        review.setSoundnessReasonableBudgetScore(reviewInput.getSoundnessReasonableBudgetScore());
        review.setSoundnessDoableWorkplanScore(reviewInput.getSoundnessDoableWorkplanScore());

        review.setSuitabilityCostEffectiveScore(reviewInput.getSuitabilityCostEffectiveScore());
        review.setSuitabilityPartnersIdentifiedScore(reviewInput.getSuitabilityPartnersIdentifiedScore());
        review.setSuitabilityCommercializationScore(reviewInput.getSuitabilityCommercializationScore());
        review.setSuitabilityTimelyUtilizationScore(reviewInput.getSuitabilityTimelyUtilizationScore());

        review.setSignificanceEconomicImpactScore(reviewInput.getSignificanceEconomicImpactScore());
        review.setSignificanceSocialImpactScore(reviewInput.getSignificanceSocialImpactScore());
        review.setSignificanceEnvironmentalImpactScore(reviewInput.getSignificanceEnvironmentalImpactScore());
        review.setSignificanceSustainabilityScore(reviewInput.getSignificanceSustainabilityScore());

        review.setCompetenceAlignmentScore(reviewInput.getCompetenceAlignmentScore());
        review.setCompetenceCollaborationScore(reviewInput.getCompetenceCollaborationScore());
        review.setCompetenceDeliverablesUnderstandingScore(reviewInput.getCompetenceDeliverablesUnderstandingScore());

        review.setOverallComments(reviewInput.getOverallComments());
        review.setDecision(reviewInput.getDecision());

        review = reviewRepository.save(review);

        // Update Proposal Status based on Review Decision
        String decision = reviewInput.getDecision().toUpperCase();
        String role = reviewer.getRole().toUpperCase();
        String targetStatus = proposal.getStatus();

        if ("REJECTED".equals(decision)) {
            targetStatus = "REJECTED";
        } else if ("REVISION_REQUIRED".equals(decision) || "REVISION".equals(decision)) {
            targetStatus = "PENDING_REVISION";
        } else if ("APPROVED".equals(decision)) {
            if ("REC".equals(role)) {
                targetStatus = "REC_APPROVED";
            } else if ("OVCRIGE".equals(role) || "OVC".equals(role)) {
                targetStatus = "OVC_APPROVED";
            } else if ("OC".equals(role)) {
                targetStatus = "APPROVED";
            } else {
                targetStatus = "APPROVED";
            }
        }

        proposal.setStatus(targetStatus);
        proposalRepository.save(proposal);

        // Notify Proponent
        if (proposal.getProponent() != null) {
            notificationService.createNotification(
                    proposal.getProponent().getId(),
                    "A review decision has been submitted on your proposal \"" + proposal.getProjectTitle() +
                            "\". Role: " + role + ", Decision: " + decision + ", Overall Proposal Status: " + targetStatus
            );
        }

        // Notify relevant reviewers or roles for next step
        if ("REC_APPROVED".equals(targetStatus)) {
            notifyRole("OVCRIGE", "Proposal \"" + proposal.getProjectTitle() + "\" was approved by REC and is ready for OVC review.");
        } else if ("OVC_APPROVED".equals(targetStatus)) {
            notifyRole("OC", "Proposal \"" + proposal.getProjectTitle() + "\" was approved by OVC and is ready for OC final approval.");
        }

        return review;
    }

    private void notifyRole(String role, String message) {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (role.equalsIgnoreCase(user.getRole())) {
                notificationService.createNotification(user.getId(), message);
            }
        }
    }
}
