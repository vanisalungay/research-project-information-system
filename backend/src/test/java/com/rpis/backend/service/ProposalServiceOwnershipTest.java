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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.rpis.backend.storage.LocalUploadStorage;

/**
 * Verifies that proposal data is isolated per authenticated Proponent, that the
 * frontend-supplied owner id is never trusted, and that authorized staff keep
 * their existing university-wide access.
 */
class ProposalServiceOwnershipTest {

    private ProposalRepository proposalRepository;
    private UserRepository userRepository;
    private ProposalAnnouncementService announcementService;
    private ProposalService service;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        userRepository = mock(UserRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        announcementService = mock(ProposalAnnouncementService.class);
        ProposalVersionService versionService = mock(ProposalVersionService.class);
        service = new ProposalService(proposalRepository, userRepository, notificationService,
                announcementService, versionService, mock(ProposalSectionRemarkService.class),
                new CurrentUser(), mock(CooperatingAgencyService.class), mock(PriorityAgendaService.class), new LocalUploadStorage("uploads"));

        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAll()).thenReturn(List.of());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("User " + id);
        user.setRole("PROPONENT");
        return user;
    }

    private Proposal proposal(Long id, Long ownerId) {
        Proposal proposal = new Proposal();
        proposal.setId(id);
        proposal.setProponent(user(ownerId));
        proposal.setStatus("SUBMITTED");
        return proposal;
    }

    @Test
    void newProponentWithNoProposalsReceivesAnEmptyList() {
        User me = user(7L);
        authenticate(7L, "PROPONENT");
        when(userRepository.findById(7L)).thenReturn(Optional.of(me));
        when(proposalRepository.findByProponent(me)).thenReturn(List.of());

        List<Proposal> result = service.getProposalsForCurrentUser(null, null, null);

        assertEquals(List.of(), result);
        verify(proposalRepository).findByProponent(me);
        verify(proposalRepository, never()).findAll();
    }

    @Test
    void proponentListIgnoresAForeignProponentIdSuppliedByTheFrontend() {
        User me = user(7L);
        authenticate(7L, "PROPONENT");
        when(userRepository.findById(7L)).thenReturn(Optional.of(me));
        when(proposalRepository.findByProponent(me)).thenReturn(List.of(proposal(1L, 7L)));

        // The caller tries to read proponent 99's proposals via the query param.
        List<Proposal> result = service.getProposalsForCurrentUser(99L, null, null);

        assertEquals(1, result.size());
        verify(proposalRepository).findByProponent(me);
        verify(userRepository, never()).findById(99L);
    }

    @Test
    void proponentStatusFilterIsScopedToTheirOwnRecords() {
        User me = user(7L);
        authenticate(7L, "PROPONENT");
        when(userRepository.findById(7L)).thenReturn(Optional.of(me));
        when(proposalRepository.findByProponentAndStatusIn(me, List.of("SUBMITTED", "APPROVED")))
                .thenReturn(List.of(proposal(1L, 7L)));

        List<Proposal> result = service.getProposalsForCurrentUser(null, null, List.of("submitted", "APPROVED"));

        assertEquals(1, result.size());
        verify(proposalRepository).findByProponentAndStatusIn(me, List.of("SUBMITTED", "APPROVED"));
        verify(proposalRepository, never()).findByStatusIn(any());
    }

    @Test
    void staffRetainTheUniversityWideProposalView() {
        authenticate(2L, "RPS_STAFF");
        List<Proposal> all = List.of(proposal(1L, 7L), proposal(2L, 8L));
        when(proposalRepository.findAll()).thenReturn(all);

        List<Proposal> result = service.getProposalsForCurrentUser(null, null, null);

        assertEquals(all, result);
        verify(proposalRepository).findAll();
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void unrecognizedRoleFailsClosedToOwnRecordsOnly() {
        User me = user(7L);
        authenticate(7L, "SOME_UNKNOWN_ROLE");
        when(userRepository.findById(7L)).thenReturn(Optional.of(me));
        when(proposalRepository.findByProponent(me)).thenReturn(List.of());

        List<Proposal> result = service.getProposalsForCurrentUser(null, null, null);

        assertEquals(List.of(), result);
        verify(proposalRepository).findByProponent(me);
        verify(proposalRepository, never()).findAll();
    }

    @Test
    void proponentCannotReadAnotherUsersProposalById() {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal(1L, 8L)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getProposalByIdForCurrentUser(1L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void proponentCanReadTheirOwnProposalById() {
        authenticate(7L, "PROPONENT");
        Proposal mine = proposal(1L, 7L);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(mine));

        assertEquals(mine, service.getProposalByIdForCurrentUser(1L));
    }

    @Test
    void submissionBindsTheAuthenticatedProponentAndIgnoresThePayloadOwner() {
        User me = user(7L);
        authenticate(7L, "PROPONENT");
        when(userRepository.findById(7L)).thenReturn(Optional.of(me));
        when(proposalRepository.nextProposalCodeSequence()).thenReturn(22L);

        ProposalAnnouncement announcement = new ProposalAnnouncement();
        announcement.setId(1L);
        announcement.setStatus(ProposalAnnouncement.STATUS_ACTIVE);
        announcement.setStartDate(LocalDate.now().minusDays(1));
        announcement.setEndDate(LocalDate.now().plusDays(1));
        when(announcementService.getActiveAnnouncementOrNull()).thenReturn(announcement);

        ProposalRequest request = new ProposalRequest();
        request.setProjectTitle("My Proposal");
        request.setDuration("12");
        request.setStatus("DRAFT");
        request.setProponentId(99L); // spoofed owner

        Proposal saved = service.saveProposal(request, null);

        assertEquals(7L, saved.getProponent().getId());
        verify(userRepository, never()).findById(99L);
    }
}
