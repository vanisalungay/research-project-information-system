package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalReview;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.ProposalReviewRepository;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * An evaluation outcome must go through the same notification policy as the dedicated
 * workflow endpoints, so the Proponent and the next office receive one specific message
 * instead of a second generic "decision" row.
 */
class ProposalReviewServiceNotificationTest {

    private static final Long PROPOSAL_ID = 42L;
    private static final Long REVIEWER_ID = 5L;

    private ProposalRepository proposalRepository;
    private ProposalReviewRepository reviewRepository;
    private UserRepository userRepository;
    private ProposalNotificationPolicy proposalNotificationPolicy;
    private ProposalReviewService service;
    private Proposal proposal;
    private User reviewer;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        reviewRepository = mock(ProposalReviewRepository.class);
        userRepository = mock(UserRepository.class);
        proposalNotificationPolicy = mock(ProposalNotificationPolicy.class);
        service = new ProposalReviewService(reviewRepository, proposalRepository,
                userRepository, proposalNotificationPolicy,
                new com.rpis.backend.security.CurrentUser());

        User proponent = new User();
        proponent.setId(7L);
        proponent.setName("Proponent");
        proponent.setRole(UserRoles.PROPONENT);

        proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        proposal.setProjectTitle("Coastal study");
        proposal.setStatus("UNDER_REVIEW");
        proposal.setProponent(proponent);

        reviewer = new User();
        reviewer.setId(REVIEWER_ID);
        reviewer.setName("REC evaluator");
        reviewer.setRole(UserRoles.REC);

        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(REVIEWER_ID)).thenReturn(Optional.of(reviewer));
        when(reviewRepository.save(any(ProposalReview.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ProposalReview review(String decision) {
        ProposalReview input = new ProposalReview();
        input.setDecision(decision);
        if ("APPROVED".equals(decision)) {
            input.setSoundnessSectorNeedScore(4);
            input.setSoundnessEffectiveSolutionScore(4);
            input.setSoundnessReasonableBudgetScore(4);
            input.setSoundnessDoableWorkplanScore(4);
            input.setSuitabilityCostEffectiveScore(4);
            input.setSuitabilityPartnersIdentifiedScore(4);
            input.setSuitabilityCommercializationScore(4);
            input.setSuitabilityTimelyUtilizationScore(4);
            input.setSignificanceEconomicImpactScore(4);
            input.setSignificanceSocialImpactScore(4);
            input.setSignificanceEnvironmentalImpactScore(4);
            input.setSignificanceSustainabilityScore(4);
            input.setCompetenceAlignmentScore(4);
            input.setCompetenceCollaborationScore(4);
            input.setCompetenceDeliverablesUnderstandingScore(4);
            input.setOverallComments("The proposal meets the evaluation criteria.");
        }
        if ("REJECTED".equals(decision)) {
            input.setOverallComments("The proposal does not meet the evaluation criteria.");
        }
        return input;
    }

    @Test
    void anRecApprovalUsesTheSharedPolicyForTheProponentAndOvcrige() {
        service.submitReview(PROPOSAL_ID, REVIEWER_ID, review("APPROVED"));

        assertEquals("REC_APPROVED", proposal.getStatus());
        verify(proposalNotificationPolicy).notifyForStatus(proposal);
        verify(proposalNotificationPolicy, never()).notifyOvcrigeReturned(any(), any());
    }

    @Test
    void aReceRejectionIsHandedToOrpsInsteadOfNotifyingTheProponent() {
        service.submitReview(PROPOSAL_ID, REVIEWER_ID, review("REJECTED"));

        assertEquals("RETURNED_TO_RPS", proposal.getStatus());
        assertEquals("REJECTION", proposal.getReturnKind());
        assertEquals(UserRoles.REC, proposal.getReturnedByOffice());
        assertEquals("The proposal does not meet the evaluation criteria.",
                proposal.getReturnRemarks());
        verify(proposalNotificationPolicy).notifyRpsRejectionReturned(
                proposal, "The proposal does not meet the evaluation criteria.", UserRoles.REC);
        verify(proposalNotificationPolicy, never()).notifyForStatus(any());
    }

    @Test
    void anApprovalWithMissingScoresIsRefusedBeforeAnythingIsSaved() {
        ProposalReview input = new ProposalReview();
        input.setDecision("APPROVED");
        input.setOverallComments("Looks good.");

        org.springframework.web.server.ResponseStatusException error =
                org.junit.jupiter.api.Assertions.assertThrows(
                        org.springframework.web.server.ResponseStatusException.class,
                        () -> service.submitReview(PROPOSAL_ID, REVIEWER_ID, input));
        assertEquals(400, error.getStatusCode().value());
        assertEquals("UNDER_REVIEW", proposal.getStatus());
        verify(reviewRepository, never()).save(any(ProposalReview.class));
    }

    @Test
    void anApprovalWithoutACommentOrWithWhitespaceIsRefused() {
        ProposalReview missing = review("APPROVED");
        missing.setOverallComments(null);
        assertEquals(400, org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.submitReview(PROPOSAL_ID, REVIEWER_ID, missing))
                .getStatusCode().value());

        ProposalReview blank = review("APPROVED");
        blank.setOverallComments("   ");
        assertEquals(400, org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.submitReview(PROPOSAL_ID, REVIEWER_ID, blank))
                .getStatusCode().value());
    }

    @Test
    void aScoreOutsideOneToFiveIsRefused() {
        ProposalReview input = review("APPROVED");
        input.setSignificanceSustainabilityScore(6);

        assertEquals(400, org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.submitReview(PROPOSAL_ID, REVIEWER_ID, input))
                .getStatusCode().value());
    }

    @Test
    void aCompleteApprovalIsSavedAndMovesTheProposalToRecApproved() {
        service.submitReview(PROPOSAL_ID, REVIEWER_ID, review("APPROVED"));

        assertEquals("REC_APPROVED", proposal.getStatus());
        verify(reviewRepository).save(any(ProposalReview.class));
    }

    @Test
    void aRejectionWithoutRemarksIsRefused() {
        ProposalReview input = new ProposalReview();
        input.setDecision("REJECTED");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.submitReview(PROPOSAL_ID, REVIEWER_ID, input));
    }

    @Test
    void aReturnGoesThroughThePolicySoOnlyRpsIsNotified() {
        service.submitReview(PROPOSAL_ID, REVIEWER_ID, review("REVISION_REQUIRED"));

        assertEquals("RETURNED_TO_RPS", proposal.getStatus());
        verify(proposalNotificationPolicy).notifyForStatus(proposal);
    }
}
