package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalReview;
import com.rpis.backend.model.ProposalStatusHistory;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.ProposalReviewRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.util.ProposalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProposalReviewService {

    private final ProposalReviewRepository reviewRepository;
    private final ProposalRepository proposalRepository;
    private final UserRepository userRepository;
    private final ProposalNotificationPolicy proposalNotificationPolicy;
    private final com.rpis.backend.security.CurrentUser currentUser;

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

        // A multi-role account reviews as the role it selected for this session. The
        // account's primary role is only the fallback for a call that is not made by the
        // authenticated reviewer themselves.
        String reviewerRole = reviewerId.equals(currentUser.getUserId())
                && currentUser.getRole() != null
                ? currentUser.getRole()
                : reviewer.getRole();
        ProposalReview review = new ProposalReview();
        review.setProposal(proposal);
        review.setReviewer(reviewer);
        review.setReviewerRole(reviewerRole);

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
        String role = reviewerRole.toUpperCase();
        String previousStatus = ProposalStatus.normalize(proposal.getStatus());
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
            proposal.setReturnKind("REVISION");
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
        recordStatusHistory(proposal, previousStatus, targetStatus,
                reviewInput.getOverallComments(), reviewer, role);
        proposalRepository.save(proposal);

        // The one notification policy owns the recipients and wording of every status
        // transition. Using it here keeps an evaluation outcome consistent with the
        // dedicated workflow endpoints and prevents a second, generic "decision" message
        // from being written alongside it. The policy deliberately stays silent for a
        // return: that goes to RPS first, and the Proponent is told when RPS forwards it.
        proposalNotificationPolicy.notifyForStatus(proposal);

        return review;
    }

    /**
     * Appends the review decision to the proposal's audited status timeline. The row is
     * cascaded by the proposal save in the same transaction, so a failed submission
     * leaves neither a status change nor a history entry behind.
     */
    private void recordStatusHistory(Proposal proposal, String fromStatus, String toStatus,
                                     String remarks, User reviewer, String role) {
        String storedFrom = ProposalStatus.optionalSupportedStatus(fromStatus);
        String storedTo = ProposalStatus.requireSupportedStatus(toStatus);
        if (storedTo == null || storedTo.equals(storedFrom)) {
            return;
        }
        ProposalStatusHistory entry = new ProposalStatusHistory();
        entry.setProposal(proposal);
        entry.setFromStatus(storedFrom);
        entry.setToStatus(storedTo);
        entry.setActorUserId(reviewer.getId());
        entry.setActorRole(role);
        entry.setActorName(reviewer.getName());
        entry.setRemarks(remarks);
        entry.setSource(ProposalStatusHistory.SOURCE_APPLICATION);
        entry.setChangedAt(LocalDateTime.now());
        proposal.getStatusHistory().add(entry);
    }
}
