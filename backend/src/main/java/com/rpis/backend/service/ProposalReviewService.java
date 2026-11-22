package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalReview;
import com.rpis.backend.model.ProposalStatusHistory;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.ProposalReviewRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.util.ProposalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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

    /**
     * The shared evaluation rule.
     *
     * <p>Every supplied score must be a whole number from 1 to 5 (the ranges the form
     * offers). A REC approval additionally requires all 15 criteria and a non-blank
     * overall comment; whitespace-only comments are rejected. The scoring system itself
     * - criteria, ranges, weights and the resulting workflow - is unchanged.
     *
     * <p>The message is a 400 reason so the REC form can show exactly what is missing.
     */
    private void validateEvaluation(ProposalReview input, String reviewerRole) {
        List<Integer> scores = Arrays.asList(
                input.getSoundnessSectorNeedScore(),
                input.getSoundnessEffectiveSolutionScore(),
                input.getSoundnessReasonableBudgetScore(),
                input.getSoundnessDoableWorkplanScore(),
                input.getSuitabilityCostEffectiveScore(),
                input.getSuitabilityPartnersIdentifiedScore(),
                input.getSuitabilityCommercializationScore(),
                input.getSuitabilityTimelyUtilizationScore(),
                input.getSignificanceEconomicImpactScore(),
                input.getSignificanceSocialImpactScore(),
                input.getSignificanceEnvironmentalImpactScore(),
                input.getSignificanceSustainabilityScore(),
                input.getCompetenceAlignmentScore(),
                input.getCompetenceCollaborationScore(),
                input.getCompetenceDeliverablesUnderstandingScore());

        for (Integer score : scores) {
            if (score != null && (score < 1 || score > 5)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Every evaluation score must be a whole number from 1 to 5.");
            }
        }

        String decision = input.getDecision() == null
                ? ""
                : input.getDecision().trim().toUpperCase(Locale.ROOT);
        boolean recApproval = "APPROVED".equals(decision)
                && UserRoles.REC.equalsIgnoreCase(reviewerRole == null ? "" : reviewerRole.trim());
        if (!recApproval) {
            return;
        }

        long missing = scores.stream().filter(Objects::isNull).count();
        if (missing > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Complete all " + scores.size() + " evaluation criteria before approving this"
                            + " proposal. " + missing + " score" + (missing == 1 ? " is" : "s are")
                            + " still missing.");
        }
        if (input.getOverallComments() == null || input.getOverallComments().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "An overall comment is required before approving this proposal.");
        }
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
        // A REC approval is accepted only with a complete, valid evaluation and a real
        // comment. This runs before anything is saved, so a direct API request cannot
        // bypass the form and leave an incomplete approval on record.
        validateEvaluation(reviewInput, reviewerRole);

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
        boolean rejectionHandoffToOrps = false;

        if ("REJECTED".equals(decision)) {
            String comments = reviewInput.getOverallComments();
            if (comments == null || comments.isBlank()) {
                throw new IllegalArgumentException("Rejection remarks are required.");
            }
            // Every rejection is centralized at ORPS. Only an ORPS reviewer (RPS Admin or
            // Staff) makes the final decision; any other evaluating office routes the
            // rejection to ORPS, which reviews it and informs the Proponent.
            if (UserRoles.RPS_ADMIN.equalsIgnoreCase(role)
                    || UserRoles.RPS_STAFF.equalsIgnoreCase(role)) {
                targetStatus = "REJECTED";
            } else {
                rejectionHandoffToOrps = true;
                targetStatus = "RETURNED_TO_RPS";
                proposal.setReturnRemarks(comments);
                proposal.setReturnedByOffice(role);
                proposal.setReturnKind("REJECTION");
                proposal.setReturnedByUserId(reviewer.getId());
                proposal.setReturnedByName(reviewer.getName());
                proposal.setReturnedAt(java.time.LocalDateTime.now());
                proposal.setRevisionDeadline(null);
                proposal.setRevisionForwardedAt(null);
                proposal.setRevisionForwardedByName(null);
                proposal.setRevisionForwardedByUserId(null);
                proposal.setRevisionNotes(null);
            }
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
        if (returnedForRevision || rejectionHandoffToOrps || "REJECTED".equals(targetStatus)) {
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
        if (rejectionHandoffToOrps) {
            // The office rejection is delivered to ORPS, never to the Proponent directly.
            proposalNotificationPolicy.notifyRpsRejectionReturned(
                    proposal, reviewInput.getOverallComments(), role);
        } else {
            proposalNotificationPolicy.notifyForStatus(proposal);
        }

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
