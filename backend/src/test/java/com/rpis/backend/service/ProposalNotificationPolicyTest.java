package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.util.NotificationEvent;
import com.rpis.backend.util.NotificationTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The recipient rules of a proposal status transition. These are the rules that decide
 * whether OC, OVCRIGE, OVCAF, REC, RPS or the proponent is told about an action.
 */
class ProposalNotificationPolicyTest {

    private NotificationService notificationService;
    private ProposalNotificationPolicy policy;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        policy = new ProposalNotificationPolicy(notificationService);
    }

    private Proposal proposal(String status) {
        Proposal proposal = new Proposal();
        proposal.setId(42L);
        proposal.setProjectTitle("A test proposal");
        proposal.setStatus(status);
        return proposal;
    }

    private User proponent() {
        User user = new User();
        user.setId(7L);
        user.setName("Proponent");
        user.setRole(UserRoles.PROPONENT);
        return user;
    }

    @Test
    void theChancellorIsNotifiedWhenOvcrigeForwardsAProposal() {
        Proposal proposal = proposal("FOR_OC_APPROVAL");

        policy.notifyForStatus(proposal);

        verify(notificationService).notifyRoles(
                eq(List.of(UserRoles.OC)), anyString(), eq("Final Approval Required"),
                eq(NotificationTypes.APPROVAL), eq(42L), any(NotificationEvent.class));
    }

    @Test
    void recApprovalNotifiesOvcrigeAndTheProponent() {
        Proposal proposal = proposal("REC_APPROVED");
        proposal.setProponent(proponent());

        policy.notifyForStatus(proposal);

        verify(notificationService).notifyRoles(
                eq(List.of(UserRoles.OVCRIGE)), anyString(), eq("Proposal Approved by REC"),
                eq(NotificationTypes.APPROVAL), eq(42L), any(NotificationEvent.class));
        verify(notificationService).notifyUsers(
                eq(List.of(proposal.getProponent())), anyString(), eq("Proposal Approved by REC"),
                eq(NotificationTypes.APPROVAL), eq(42L), any(NotificationEvent.class),
                eq(UserRoles.PROPONENT));
    }

    @Test
    void theChancellorReturnDoesNotNotifyTheProponentOrClaimAnRecApproval() {
        Proposal proposal = proposal("REC_APPROVED");
        proposal.setProponent(proponent());

        policy.notifyOvcrigeReturned(proposal, "Please revise the budget.");

        verify(notificationService).notifyRoles(
                eq(List.of(UserRoles.OVCRIGE)), anyString(),
                eq("Proposal Returned by the Chancellor's Office"),
                eq(NotificationTypes.REVISION), eq(42L), any(NotificationEvent.class));
        verify(notificationService, never()).notifyUsers(any(), anyString(), anyString(), anyString(), any(), any(com.rpis.backend.util.NotificationEvent.class));
    }

    @Test
    void theOvcafReviewRequestUsesTheFundingTypeNotEndorsement() {
        policy.notifyForStatus(proposal("FOR_OVCAF_APPROVAL"));

        verify(notificationService).notifyRoles(
                eq(List.of(UserRoles.OVCAF)), anyString(), eq("Proposal for OVCAF Review"),
                eq(NotificationTypes.FUNDING), eq(42L), any(NotificationEvent.class));
    }

    @Test
    void rpsEndorsementUsesTheDedicatedProposalEndorsedType() {
        policy.notifyForStatus(proposal("ENDORSED"));

        verify(notificationService).notifyRoles(
                eq(List.of(UserRoles.OVCRIGE)), anyString(), eq("Proposal Endorsed"),
                eq(NotificationTypes.PROPOSAL_ENDORSED), eq(42L), any(NotificationEvent.class));
    }

    @Test
    void aReturnForRevisionGoesToBothRpsRoles() {
        policy.notifyForStatus(proposal("RETURNED_TO_RPS"));

        verify(notificationService).notifyRoles(
                eq(List.of(UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF)), anyString(),
                eq("Revision Request Received"), eq(NotificationTypes.REVISION), eq(42L), any(NotificationEvent.class));
    }

    @Test
    void aChancellorApprovalWithoutBudgetNotifiesOnlyTheProponent() {
        Proposal proposal = proposal("APPROVED");
        proposal.setProponent(proponent());
        proposal.setNeedsBudget(false);

        policy.notifyForStatus(proposal);

        verify(notificationService).notifyUsers(
                eq(List.of(proposal.getProponent())), anyString(),
                eq("Proposal Approved for Implementation"),
                eq(NotificationTypes.APPROVAL), eq(42L), any(NotificationEvent.class),
                eq(UserRoles.PROPONENT));
        verify(notificationService, never()).notifyRoles(any(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void anApprovalWithBudgetAlsoAsksOvcafToEndorse() {
        Proposal proposal = proposal("APPROVED");
        proposal.setProponent(proponent());
        proposal.setNeedsBudget(true);

        policy.notifyForStatus(proposal);

        verify(notificationService).notifyRoles(
                eq(List.of(UserRoles.OVCAF)), anyString(), eq("Budget Endorsement Required"),
                eq(NotificationTypes.FUNDING), eq(42L), any(NotificationEvent.class));
    }

    @Test
    void aDraftRaisesNothing() {
        Proposal proposal = proposal("DRAFT");

        policy.notifyForStatus(proposal);

        verifyNoInteractions(notificationService);
    }

    @Test
    void aResubmissionIsNotReportedAsANewSubmission() {
        policy.notifyForStatus(proposal("FOR REVIEW"), true);

        verify(notificationService).notifyRoles(
                eq(List.of(UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF)), anyString(),
                eq("Proposal Resubmitted"), eq(NotificationTypes.PROPOSAL_UPDATE), eq(42L), any(NotificationEvent.class));
    }

    @Test
    void submissionConfirmsToTheProponentAndKeepsTheResubmissionWording() {
        Proposal submitted = proposal("FOR REVIEW");
        submitted.setProponent(proponent());
        policy.notifyForStatus(submitted);
        verify(notificationService).notifyUsers(
                eq(List.of(submitted.getProponent())), anyString(), eq("Proposal Submitted"),
                eq(NotificationTypes.PROPOSAL_SUBMISSION), eq(42L), any(NotificationEvent.class),
                eq(UserRoles.PROPONENT));

        Proposal resubmitted = proposal("FOR REVIEW");
        resubmitted.setProponent(proponent());
        policy.notifyForStatus(resubmitted, true);
        verify(notificationService).notifyUsers(
                eq(List.of(resubmitted.getProponent())), anyString(), eq("Proposal Resubmitted"),
                eq(NotificationTypes.PROPOSAL_SUBMISSION), eq(42L), any(NotificationEvent.class),
                eq(UserRoles.PROPONENT));
    }

    @Test
    void theProponentIsToldAboutEveryForwardAndApprovalStage() {
        Proposal endorsed = proposal("ENDORSED");
        endorsed.setProponent(proponent());
        policy.notifyForStatus(endorsed);
        verify(notificationService).notifyUsers(
                eq(List.of(endorsed.getProponent())), anyString(),
                eq("Proposal Endorsed by RPS"), eq(NotificationTypes.PROPOSAL_ENDORSED),
                eq(42L), any(NotificationEvent.class), eq(UserRoles.PROPONENT));

        Proposal rec = proposal("UNDER_REVIEW");
        rec.setProponent(proponent());
        policy.notifyForStatus(rec);
        verify(notificationService).notifyUsers(
                eq(List.of(rec.getProponent())), anyString(),
                eq("Proposal Forwarded to REC"), eq(NotificationTypes.REVIEW_REQUEST),
                eq(42L), any(NotificationEvent.class), eq(UserRoles.PROPONENT));

        Proposal forwardedOc = proposal("FOR_OC_APPROVAL");
        forwardedOc.setProponent(proponent());
        policy.notifyForStatus(forwardedOc);
        verify(notificationService).notifyUsers(
                eq(List.of(forwardedOc.getProponent())), anyString(),
                eq("Proposal Forwarded to the Office of the Chancellor"),
                eq(NotificationTypes.APPROVAL), eq(42L), any(NotificationEvent.class),
                eq(UserRoles.PROPONENT));
    }

    @Test
    void anOvcApprovalTellsTheProponentWithoutPreemptingTheOcForward() {
        Proposal proposal = proposal("OVC_APPROVED");
        proposal.setProponent(proponent());

        policy.notifyForStatus(proposal);

        verify(notificationService).notifyUsers(
                eq(List.of(proposal.getProponent())), anyString(),
                eq("Proposal Approved by OVCRIGE"), eq(NotificationTypes.APPROVAL),
                eq(42L), any(NotificationEvent.class), eq(UserRoles.PROPONENT));
        verify(notificationService, never()).notifyRoles(
                eq(List.of(UserRoles.OC)), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void theChancellorApprovalMentionsTheSpecialOrderAndOvcafWhenABudgetIsRequired() {
        Proposal proposal = proposal("FOR_OVCAF_APPROVAL");
        proposal.setProponent(proponent());
        proposal.setSoNumber("SO-2026-001");

        policy.notifyForStatus(proposal);

        verify(notificationService).notifyUsers(
                eq(List.of(proposal.getProponent())),
                org.mockito.ArgumentMatchers.contains("Special Order SO-2026-001 has been issued"),
                eq("Proposal Approved by the Chancellor"), eq(NotificationTypes.APPROVAL),
                eq(42L), any(NotificationEvent.class), eq(UserRoles.PROPONENT));
    }

    @Test
    void aReturnedRevisionNamesTheRevisionAndCarriesTheRemarks() {
        Proposal proposal = proposal("RPS_RETURNED");
        proposal.setProponent(proponent());
        proposal.setRemarks("Please expand the methodology.");

        policy.notifyForStatus(proposal);

        verify(notificationService).notifyUsers(
                eq(List.of(proposal.getProponent())),
                org.mockito.ArgumentMatchers.contains("Please expand the methodology"),
                eq("Proposal Returned for Revision"), eq(NotificationTypes.REVISION),
                eq(42L), any(NotificationEvent.class), eq(UserRoles.PROPONENT));
    }
}
