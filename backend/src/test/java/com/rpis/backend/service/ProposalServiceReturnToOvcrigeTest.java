package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalStatusHistory;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.storage.UploadStorage;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The Chancellor's Office returning a proposal to OVCRIGE: a real, audited status
 * transition to the existing OVCRIGE-owned state {@code REC_APPROVED}. The tests pin
 * down what the workflow promises - one transition, recorded with its actor and
 * remarks, no invented status or Special Order change, and a refusal once the record
 * is no longer awaiting the Chancellor.
 */
class ProposalServiceReturnToOvcrigeTest {

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
        when(currentUser.getRole()).thenReturn("OC");
        User actor = new User();
        actor.setId(ACTOR_ID);
        actor.setName("The Chancellor");
        actor.setRole("OC");
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(actor));
        when(userRepository.findByRoleIn(any())).thenReturn(List.of());
    }

    @Test
    void returningMovesTheProposalToTheOvcrigeOwnedStateAndRecordsTheTransition() {
        Proposal saved = service.returnToOvcrige(PROPOSAL_ID, "  Please re-check the budget routing.  ");

        assertEquals("REC_APPROVED", saved.getStatus());
        assertEquals("Please re-check the budget routing.", saved.getRemarks(),
                "the remarks are trimmed and stored on the proposal");

        assertEquals(1, saved.getStatusHistory().size(), "exactly one transition is recorded");
        ProposalStatusHistory entry = saved.getStatusHistory().get(0);
        assertEquals("FOR_OC_APPROVAL", entry.getFromStatus());
        assertEquals("REC_APPROVED", entry.getToStatus());
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

        Proposal saved = service.returnToOvcrige(PROPOSAL_ID, "   ");

        assertEquals("REC_APPROVED", saved.getStatus());
        assertEquals("REC reviewer note", saved.getRemarks(),
                "an existing note is not replaced by a blank one");
        assertNull(saved.getStatusHistory().get(0).getRemarks());
    }

    @Test
    void aProposalThatIsNotAwaitingTheChancellorIsRefused() {
        proposal.setStatus("FOR_OVCAF_APPROVAL");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.returnToOvcrige(PROPOSAL_ID, "Too early"));

        assertTrue(error.getMessage().contains("FOR_OC_APPROVAL"));
        assertEquals("FOR_OVCAF_APPROVAL", proposal.getStatus(), "the refused record is unchanged");
        assertTrue(proposal.getStatusHistory().isEmpty(), "a refused transition is not recorded");
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void aSecondReturnIsRefusedBecauseTheProposalHasLeftTheChancellor() {
        service.returnToOvcrige(PROPOSAL_ID, null);

        assertThrows(IllegalStateException.class, () -> service.returnToOvcrige(PROPOSAL_ID, null));

        assertEquals("REC_APPROVED", proposal.getStatus());
        assertEquals(1, proposal.getStatusHistory().size(), "the first transition is not duplicated");
        verify(proposalRepository, times(1)).save(proposal);
    }

    @Test
    void aMissingProposalIsReportedAsNotFound() {
        when(proposalRepository.findByIdForUpdate(eq(404L))).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.returnToOvcrige(404L, null));

        assertEquals(404, error.getStatusCode().value());
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void theOvcrigeOfficeIsNotifiedThroughTheExistingRecApprovedRecipients() {
        User ovcrige = new User();
        ovcrige.setId(11L);
        ovcrige.setRole("OVCRIGE");
        when(userRepository.findByRoleIn(List.of("OVCRIGE"))).thenReturn(List.of(ovcrige));

        service.returnToOvcrige(PROPOSAL_ID, "Returned");

        verify(notificationService, times(1)).createNotification(
                eq(11L), anyString(), anyString(), anyString(), eq(PROPOSAL_ID));
    }

    @Test
    void returningChangesNoSpecialOrderFieldAndTouchesNoFile() {
        proposal.setSoNumber("SO-2026-010");
        proposal.setSoFileName("SO.pdf");
        proposal.setSoFilePath("special-orders/stored_so.pdf");
        proposal.setNeedsBudget(true);

        Proposal saved = service.returnToOvcrige(PROPOSAL_ID, null);

        assertEquals("SO-2026-010", saved.getSoNumber());
        assertEquals("SO.pdf", saved.getSoFileName());
        assertEquals("special-orders/stored_so.pdf", saved.getSoFilePath());
        assertEquals(Boolean.TRUE, saved.getNeedsBudget());
        verify(uploadStorage, never()).store(any(byte[].class), anyString(), anyString());
        verify(uploadStorage, never()).delete(anyString());
    }
}
