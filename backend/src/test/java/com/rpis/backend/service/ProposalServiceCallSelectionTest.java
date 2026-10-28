package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.rpis.backend.storage.LocalUploadStorage;

/**
 * Covers the Call for Proposal selection of the Create Proposal workflow (step 1):
 * a proposal created from a selected call is attached to that exact call, the
 * selection is re-validated whenever it is used or changed, and the previous
 * behaviour (no selection sent - the currently active call is used) is preserved.
 */
class ProposalServiceCallSelectionTest {

    private static final Long OPEN_CALL_ID = 5L;
    private static final Long OTHER_OPEN_CALL_ID = 9L;
    private static final Long PROPOSAL_ID = 42L;
    private static final Long PROPONENT_ID = 7L;

    private ProposalRepository proposalRepository;
    private UserRepository userRepository;
    private ProposalAnnouncementService announcementService;
    private ProposalService service;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        userRepository = mock(UserRepository.class);
        announcementService = mock(ProposalAnnouncementService.class);
        service = new ProposalService(proposalRepository, userRepository,
                mock(NotificationService.class), announcementService,
                mock(ProposalVersionService.class), mock(ProposalSectionRemarkService.class),
                new CurrentUser(), mock(CooperatingAgencyService.class), mock(PriorityAgendaService.class), new LocalUploadStorage("uploads"));

        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(proposalRepository.nextProposalCodeSequence()).thenReturn(22L);
        when(userRepository.findById(PROPONENT_ID)).thenReturn(Optional.of(proponent()));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                PROPONENT_ID, null, List.of(new SimpleGrantedAuthority("ROLE_PROPONENT"))));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User proponent() {
        User user = new User();
        user.setId(PROPONENT_ID);
        user.setName("Call Selection Proponent");
        user.setRole("PROPONENT");
        return user;
    }

    private ProposalAnnouncement announcement(Long id, String status, LocalDate start, LocalDate end) {
        ProposalAnnouncement announcement = new ProposalAnnouncement();
        announcement.setId(id);
        announcement.setName("Call " + id);
        announcement.setStatus(status);
        announcement.setStartDate(start);
        announcement.setEndDate(end);
        return announcement;
    }

    /** An announcement that is open today: ACTIVE and today inside the window. */
    private ProposalAnnouncement openAnnouncement(Long id) {
        return announcement(id, ProposalAnnouncement.STATUS_ACTIVE,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(5));
    }

    private ProposalRequest draftRequest(Long selectedCallId) {
        ProposalRequest request = new ProposalRequest();
        request.setProjectTitle("Proposal under a selected call");
        request.setDuration("12");
        request.setStatus("DRAFT");
        request.setProposalAnnouncementId(selectedCallId);
        return request;
    }

    private Proposal existingDraft(Long storedCallId) {
        Proposal proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        proposal.setProponent(proponent());
        proposal.setStatus("DRAFT");
        proposal.setProposalAnnouncementId(storedCallId);
        when(proposalRepository.findByIdForUpdate(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        return proposal;
    }

    @Test
    void aNewProposalIsAttachedToTheSelectedCall() {
        when(announcementService.requireOpenAnnouncement(OPEN_CALL_ID))
                .thenReturn(openAnnouncement(OPEN_CALL_ID));

        Proposal saved = service.saveProposal(draftRequest(OPEN_CALL_ID), null);

        assertEquals(OPEN_CALL_ID, saved.getProposalAnnouncementId());
    }

    @Test
    void theSelectedCallWinsOverTheImplicitActiveAnnouncement() {
        // Another call may be the "active" one, but the proponent selected a specific
        // call: the proposal must belong to the selected call, never to another one.
        when(announcementService.requireOpenAnnouncement(OPEN_CALL_ID))
                .thenReturn(openAnnouncement(OPEN_CALL_ID));

        Proposal saved = service.saveProposal(draftRequest(OPEN_CALL_ID), null);

        assertEquals(OPEN_CALL_ID, saved.getProposalAnnouncementId());
        verify(announcementService, never()).getActiveAnnouncementOrNull();
        verify(announcementService, never()).requireOpenAnnouncement(OTHER_OPEN_CALL_ID);
    }

    @Test
    void withoutASelectionTheActiveAnnouncementIsStillUsed() {
        // The pre-existing behaviour for clients that do not send a selection.
        when(announcementService.getActiveAnnouncementOrNull())
                .thenReturn(openAnnouncement(OTHER_OPEN_CALL_ID));

        Proposal saved = service.saveProposal(draftRequest(null), null);

        assertEquals(OTHER_OPEN_CALL_ID, saved.getProposalAnnouncementId());
        verify(announcementService, never()).requireOpenAnnouncement(any());
    }

    @Test
    void aCallThatIsNoLongerOpenIsRefusedAndNothingIsSaved() {
        when(announcementService.requireOpenAnnouncement(OPEN_CALL_ID))
                .thenThrow(new IllegalStateException(
                        "The selected Call for Proposal is no longer open for submissions. "
                                + "Please select an open call for proposal."));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.saveProposal(draftRequest(OPEN_CALL_ID), null));

        assertEquals("The selected Call for Proposal is no longer open for submissions. "
                + "Please select an open call for proposal.", error.getMessage());
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void reSavingADraftKeepsItsCallWithoutRevalidatingIt() {
        // A draft whose call has closed must stay editable: the selection is unchanged,
        // so it is not validated again (only the final submission is blocked).
        Proposal draft = existingDraft(OPEN_CALL_ID);

        Proposal saved = service.saveProposal(draftRequest(OPEN_CALL_ID), PROPOSAL_ID);

        assertEquals(OPEN_CALL_ID, saved.getProposalAnnouncementId());
        assertEquals(draft, saved);
        verify(announcementService, never()).requireOpenAnnouncement(any());
    }

    @Test
    void changingTheCallOfADraftValidatesTheNewSelection() {
        existingDraft(OPEN_CALL_ID);
        when(announcementService.requireOpenAnnouncement(OTHER_OPEN_CALL_ID))
                .thenReturn(openAnnouncement(OTHER_OPEN_CALL_ID));

        Proposal saved = service.saveProposal(draftRequest(OTHER_OPEN_CALL_ID), PROPOSAL_ID);

        assertEquals(OTHER_OPEN_CALL_ID, saved.getProposalAnnouncementId());
        verify(announcementService).requireOpenAnnouncement(OTHER_OPEN_CALL_ID);
    }

    @Test
    void anUnknownCallIsReported() {
        when(announcementService.requireOpenAnnouncement(404L))
                .thenThrow(new IllegalArgumentException("Call for Proposal not found: 404"));

        assertThrows(IllegalArgumentException.class,
                () -> service.saveProposal(draftRequest(404L), null));
        verify(proposalRepository, never()).save(any(Proposal.class));
    }
}
