package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalReview;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.ProposalReviewRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.util.NotificationEvent;
import com.rpis.backend.util.NotificationTypes;
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
        boolean returnedForRevision = false;

        if ("REJECTED".equals(decision)) {
            targetStatus = "REJECTED";
        } else if ("REVISION_REQUIRED".equals(decision) || "REVISION".equals(decision)) {
            // A return for revision always goes to RPS first, never directly to the Proponent.
            returnedForRevision = true;
            targetStatus = "RETURNED_TO_RPS";
            proposal.setReturnRemarks(reviewInput.getOverallComments());
            proposal.setReturnedByOffice(role);
            proposal.setReturnedByUserId(reviewer.getId());
            proposal.setReturnedByName(reviewer.getName());
            proposal.setReturnedAt(java.time.LocalDateTime.now());
            proposal.setRevisionDeadline(null);
            proposal.setRevisionForwardedAt(null);
            proposal.setRevisionForwardedByName(null);
            proposal.setRevisionForwardedByUserId(null);
            proposal.setRevisionNotes(null);
        } else if ("APPROVED".equals(decision)) {
            if ("REC".equals(role)) {
                targetStatus = "REC_APPROVED";
            } else if ("OVCRIGE".equals(role)) {
                targetStatus = "OVC_APPROVED";
            } else {
                targetStatus = "APPROVED";
            }
        }

        proposal.setStatus(targetStatus);
        if (returnedForRevision) {
            proposal.setRemarks(reviewInput.getOverallComments());
        }
        proposalRepository.save(proposal);

        String title = proposal.getProjectTitle();

        // The proponent is told the outcome of an evaluation unless the proposal was
        // returned for revision - a return goes to ORPS first, who forwards it with the
        // deadline, so the proponent learns about it from that message instead.
        if (!returnedForRevision && proposal.getProponent() != null) {
            String type = "REJECTED".equals(decision)
                    ? NotificationTypes.REJECTION
                    : NotificationTypes.APPROVAL;
            String notificationTitle = "REJECTED".equals(decision)
                    ? "Proposal Rejected"
                    : "Proposal Evaluated";
            notificationService.notifyUsers(List.of(proposal.getProponent()),
                    "A review decision has been submitted on your proposal \"" + title
                            + "\". Decision: " + decision + ".",
                    notificationTitle, type, proposalId, NotificationEvent.STATUS_UPDATED);
        }

        // Notify the office that owns the next step. Each role is resolved by query, so
        // the whole user table is never loaded.
        if ("REC_APPROVED".equals(targetStatus)) {
            notificationService.notifyRoles(List.of(UserRoles.OVCRIGE),
                    "Proposal \"" + title + "\" was approved by REC and is ready for OVC review.",
                    "Proposal Approved by REC", NotificationTypes.APPROVAL, proposalId,
                    NotificationEvent.STATUS_UPDATED);
        } else if ("OVC_APPROVED".equals(targetStatus)) {
            notificationService.notifyRoles(List.of(UserRoles.OC),
                    "Proposal \"" + title + "\" was approved by OVC and is ready for OC final approval.",
                    "Final Approval Required", NotificationTypes.APPROVAL, proposalId,
                    NotificationEvent.STATUS_UPDATED);
        } else if (returnedForRevision) {
            notificationService.notifyRoles(
                    List.of(UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF),
                    "Proposal \"" + title + "\" was returned for revision by " + role
                            + " and awaits RPS review.",
                    "Revision Request Received", NotificationTypes.REVISION, proposalId,
                    NotificationEvent.STATUS_UPDATED);
        }

        return review;
    }
}
