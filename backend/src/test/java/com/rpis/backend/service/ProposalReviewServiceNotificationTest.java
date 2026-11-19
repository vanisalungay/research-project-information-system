package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalReview;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.util.NotificationEvent;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.ProposalReviewRepository;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Evaluation submission must produce the same typed notifications as the status-driven
 * workflow, and a return must go to ORPS instead of the proponent.
 */
class ProposalReviewServiceNotificationTest {

    private static final Long PROPOSAL_ID = 42L;
    private static final Long REVIEWER_ID = 5L;

    private ProposalRepository proposalRepository;
    private ProposalReviewRepository reviewRepository;
    private UserRepository userRepository;
    private NotificationService notificationService;
    private ProposalReviewService service;
    private Proposal proposal;
    private User reviewer;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        reviewRepository = mock(ProposalReviewRepository.class);
        userRepository = mock(UserRepository.class);
        notificationService = mock(NotificationService.class);
        service = new ProposalReviewService(reviewRepository, proposalRepository,
                userRepository, notificationService);

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
        return input;
    }

    @Test
    void anRecApprovalNotifiesTheProponentAndOvcrigeWithTypes() {
        service.submitReview(PROPOSAL_ID, REVIEWER_ID, review("APPROVED"));

        verify(notificationService).notifyUsers(
                eq(List.of(proposal.getProponent())), anyString(), eq("Proposal Evaluated"),
                eq("APPROVAL"), eq(PROPOSAL_ID), any(NotificationEvent.class));
        verify(notificationService).notifyRoles(
                eq(List.of(UserRoles.OVCRIGE)), anyString(), eq("Proposal Approved by REC"),
                eq("APPROVAL"), eq(PROPOSAL_ID), any(NotificationEvent.class));
        verify(userRepository, never()).findAll();
    }

    @Test
    void aRejectionNotifiesOnlyTheProponent() {
        service.submitReview(PROPOSAL_ID, REVIEWER_ID, review("REJECTED"));

        verify(notificationService).notifyUsers(
                eq(List.of(proposal.getProponent())), anyString(), eq("Proposal Rejected"),
                eq("REJECTION"), eq(PROPOSAL_ID), any(NotificationEvent.class));
        verify(notificationService, never()).notifyRoles(any(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void aReturnGoesToBothRpsRolesAndNotTheProponent() {
        service.submitReview(PROPOSAL_ID, REVIEWER_ID, review("REVISION_REQUIRED"));

        verify(notificationService).notifyRoles(
                eq(List.of(UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF)), anyString(),
                eq("Revision Request Received"), eq("REVISION"), eq(PROPOSAL_ID), any(NotificationEvent.class));
        verify(notificationService, never()).notifyUsers(any(), anyString(), anyString(), anyString(), any(), any(com.rpis.backend.util.NotificationEvent.class));
    }
}
