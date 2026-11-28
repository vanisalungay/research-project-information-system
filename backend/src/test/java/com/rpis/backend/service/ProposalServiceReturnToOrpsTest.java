package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalStatusHistory;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.storage.UploadStorage;
import com.rpis.backend.util.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The Chancellor's Office returning a proposal to ORPS instead of deciding it: a real,
 * audited hand-off to the existing {@code RETURNED_TO_RPS} state that every reviewing
 * office already uses. The tests pin down what the workflow promises - one transition,
 * recorded with its actor and remarks, no invented status or Special Order change, ORPS
 * notified, and a refusal once the record is no longer awaiting the Chancellor.
 */
class ProposalServiceReturnToOrpsTest {

    private static final Long PROPOSAL_ID = 77L;
    private static final Long ACTOR_ID = 7L;

    private ProposalRepository proposalRepository;
    private UserRepository userRepository;
    private NotificationService notificationService;
    private UploadStorage uploadStorage;
    private CurrentUser currentUser;
    private ProposalService service;
    private Proposal proposal;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        userRepository = mock(UserRepository.class);
        notificationService = mock(NotificationService.class);
        uploadStorage = mock(UploadStorage.class);
        currentUser = mock(CurrentUser.class);

        service = new ProposalService(proposalRepository,
                userRepository,
                notificationService,
                new ProposalNotificationPolicy(notificationService),
                mock(ProposalAnnouncementService.class),
                mock(ProposalVersionService.class),
                mock(ProposalSectionRemarkService.class),
                currentUser,
                mock(CooperatingAgencyService.class),
                mock(PriorityAgendaService.class),
                uploadStorage);

        proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        proposal.setProjectTitle("Coastal Research");
        proposal.setStatus("FOR_OC_APPROVAL");

        when(proposalRepository.findByIdForUpdate(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));

        when(currentUser.getUserId()).thenReturn(ACTOR_ID);
        when(currentUser.requireUserId()).thenReturn(ACTOR_ID);
        when(currentUser.getRole()).thenReturn("OC");
        User actor = new User();
        actor.setId(ACTOR_ID);
        actor.setName("The Chancellor");
        actor.setRole("OC");
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(actor));
        when(userRepository.findByRoleIn(any())).thenReturn(List.of());
    }

    @Test
    void returningMovesTheProposalToOrpsAndRecordsTheTransition() {
        Proposal saved = service.returnToOrps(PROPOSAL_ID, "  Please re-check the budget routing.  ");

        assertEquals("RETURNED_TO_RPS", saved.getStatus());
        assertEquals("REVISION", saved.getReturnKind());
        assertEquals("OC", saved.getReturnedByOffice());
        assertEquals(ACTOR_ID, saved.getReturnedByUserId());
        assertNotNull(saved.getReturnedAt());
        assertEquals("Please re-check the budget routing.", saved.getRemarks(),
                "the remarks are trimmed and stored on the proposal");
        assertEquals("Please re-check the budget routing.", saved.getReturnRemarks());

        assertEquals(1, saved.getStatusHistory().size(), "exactly one transition is recorded");
        ProposalStatusHistory entry = saved.getStatusHistory().get(0);
        assertEquals("FOR_OC_APPROVAL", entry.getFromStatus());
        assertEquals("RETURNED_TO_RPS", entry.getToStatus());
        assertEquals(ACTOR_ID, entry.getActorUserId(), "the actor is recorded");
        assertEquals("OC", entry.getActorRole());
        assertEquals("The Chancellor", entry.getActorName());
        assertEquals("Please re-check the budget routing.", entry.getRemarks());
        assertEquals(ProposalStatusHistory.SOURCE_APPLICATION, entry.getSource());
        assertNotNull(entry.getChangedAt());
        assertEquals(saved, entry.getProposal(), "the row belongs to the saved proposal");
        verify(proposalRepository, times(1)).save(proposal);
    }

    @Test
    void returningWithoutRemarksInventNoText() {
        proposal.setRemarks("REC reviewer note");

        Proposal saved = service.returnToOrps(PROPOSAL_ID, "   ");

        assertEquals("RETURNED_TO_RPS", saved.getStatus());
        assertEquals("REC reviewer note", saved.getRemarks(),
                "an existing note is not replaced by a blank one");
        assertNull(saved.getStatusHistory().get(0).getRemarks());
    }

    @Test
    void aProposalThatIsNotAwaitingTheChancellorIsRefused() {
        proposal.setStatus("FOR_OVCAF_APPROVAL");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.returnToOrps(PROPOSAL_ID, "Too early"));

        assertTrue(error.getMessage().contains("FOR_OC_APPROVAL"));
        assertEquals("FOR_OVCAF_APPROVAL", proposal.getStatus(), "the refused record is unchanged");
        assertTrue(proposal.getStatusHistory().isEmpty(), "a refused transition is not recorded");
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void aSecondReturnIsRefusedBecauseTheProposalHasLeftTheChancellor() {
        service.returnToOrps(PROPOSAL_ID, null);

        assertThrows(IllegalStateException.class, () -> service.returnToOrps(PROPOSAL_ID, null));

        assertEquals("RETURNED_TO_RPS", proposal.getStatus());
        assertEquals(1, proposal.getStatusHistory().size(), "the first transition is not duplicated");
        verify(proposalRepository, times(1)).save(proposal);
    }

    @Test
    void aMissingProposalIsReportedAsNotFound() {
        when(proposalRepository.findByIdForUpdate(eq(404L))).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.returnToOrps(404L, null));

        assertEquals(404, error.getStatusCode().value());
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void orpsIsNotifiedThroughTheExistingReturnedToRpsRule() {
        service.returnToOrps(PROPOSAL_ID, "Returned");

        verify(notificationService, times(1)).notifyRoles(
                eq(List.of("RPS_ADMIN", "RPS_STAFF")), anyString(),
                eq("Revision Request Received"), eq("REVISION"), eq(PROPOSAL_ID),
                any(NotificationEvent.class));
        verify(notificationService, never()).notifyUsers(any(), anyString(), anyString(),
                anyString(), any(), any(NotificationEvent.class));
    }

    @Test
    void theProponentIsToldTheProposalWasReturnedByTheChancellor() {
        User proponent = new User();
        proponent.setId(21L);
        proponent.setName("Returned Proponent");
        proponent.setRole("PROPONENT");
        proposal.setProponent(proponent);

        service.returnToOrps(PROPOSAL_ID, "Please revise the budget.");

        verify(notificationService, times(1)).notifyUsers(
                eq(List.of(proponent)),
                contains("returned for revision by OC"),
                eq("Proposal Returned for Revision"), eq("REVISION"), eq(PROPOSAL_ID),
                any(NotificationEvent.class), eq("PROPONENT"));
    }

    @Test
    void returningChangesNoSpecialOrderFieldAndTouchesNoFile() {
        proposal.setSoNumber("SO-2026-010");
        proposal.setSoFileName("SO.pdf");
        proposal.setSoFilePath("special-orders/stored_so.pdf");
        proposal.setNeedsBudget(true);

        Proposal saved = service.returnToOrps(PROPOSAL_ID, null);

        assertEquals("SO-2026-010", saved.getSoNumber());
        assertEquals("SO.pdf", saved.getSoFileName());
        assertEquals("special-orders/stored_so.pdf", saved.getSoFilePath());
        assertEquals(Boolean.TRUE, saved.getNeedsBudget());
        verify(uploadStorage, never()).store(any(byte[].class), anyString(), anyString());
        verify(uploadStorage, never()).delete(anyString());
    }
}
