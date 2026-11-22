package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.model.GadChecklistData;
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
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
        service = new ProposalService(proposalRepository, userRepository, notificationService, new ProposalNotificationPolicy(notificationService),
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

    /** Every GAD checklist item answered, which is what a final submission requires. */
    private GadChecklistData completedChecklist() {
        GadChecklistData data = new GadChecklistData();
        Map<String, String> responses = new LinkedHashMap<>();
        for (String item : List.of("1.1", "1.2", "2.0", "3.1", "3.2", "4.0", "5.0",
                "6.1", "6.2", "6.3", "7.0", "8.0", "9.1", "9.2", "10.1", "10.2", "10.3")) {
            responses.put(item, "YES");
        }
        data.setResponses(responses);
        return data;
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
        when(proposalRepository.findByProponentAndStatusIn(me,
                List.of("SUBMITTED", "FOR REVIEW", "APPROVED")))
                .thenReturn(List.of(proposal(1L, 7L)));

        List<Proposal> result = service.getProposalsForCurrentUser(null, null, List.of("submitted", "APPROVED"));

        assertEquals(1, result.size());
        verify(proposalRepository).findByProponentAndStatusIn(me,
                List.of("SUBMITTED", "FOR REVIEW", "APPROVED"));
        verify(proposalRepository, never()).findByStatusIn(any());
    }

    @Test
    void aForReviewFilterAlsoReturnsLegacySubmittedRows() {
        User me = user(7L);
        authenticate(7L, "PROPONENT");
        when(userRepository.findById(7L)).thenReturn(Optional.of(me));
        when(proposalRepository.findByProponentAndStatusIn(me, List.of("FOR REVIEW", "SUBMITTED")))
                .thenReturn(List.of(proposal(1L, 7L)));

        List<Proposal> result = service.getProposalsForCurrentUser(null, "for review", null);

        assertEquals(1, result.size());
        verify(proposalRepository).findByProponentAndStatusIn(me, List.of("FOR REVIEW", "SUBMITTED"));
        verify(proposalRepository, never()).findByProponentAndStatus(any(), anyString());
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
    void submittedAtSortIsNewestFirstWithAStableIdTieBreakAndMissingLast() {
        authenticate(2L, "RPS_STAFF");
        Proposal oldest = proposal(5L, 7L);
        oldest.setSubmittedAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        Proposal newest = proposal(8L, 7L);
        newest.setSubmittedAt(LocalDateTime.of(2026, 11, 19, 15, 30));
        // The same timestamp must fall back to proposal id descending.
        Proposal tieLowId = proposal(6L, 7L);
        tieLowId.setSubmittedAt(LocalDateTime.of(2026, 11, 10, 12, 0));
        Proposal tieHighId = proposal(11L, 7L);
        tieHighId.setSubmittedAt(LocalDateTime.of(2026, 11, 10, 12, 0));
        // Never submitted: no timestamp, so it must sort after every dated record.
        Proposal missing = proposal(99L, 7L);
        when(proposalRepository.findAll())
                .thenReturn(List.of(missing, oldest, tieLowId, newest, tieHighId));

        List<Proposal> result = service.getProposalsForCurrentUser(null, null, null, true);

        assertEquals(List.of(8L, 11L, 6L, 5L, 99L),
                result.stream().map(Proposal::getId).toList());
    }

    @Test
    void eachListingCanSortByTheEventDateItRepresents() {
        authenticate(2L, "RPS_STAFF");
        Proposal oldEndorsement = proposal(2L, 7L);
        oldEndorsement.setEndorsedAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        oldEndorsement.setReturnedAt(LocalDateTime.of(2026, 11, 19, 9, 0));
        Proposal newEndorsement = proposal(9L, 7L);
        newEndorsement.setEndorsedAt(LocalDateTime.of(2026, 11, 18, 9, 0));
        newEndorsement.setReturnedAt(LocalDateTime.of(2026, 9, 1, 9, 0));
        when(proposalRepository.findAll())
                .thenReturn(List.of(oldEndorsement, newEndorsement));

        assertEquals(List.of(9L, 2L),
                service.getProposalsForCurrentUser(null, null, null, "endorsedAt")
                        .stream().map(Proposal::getId).toList());
        // The same two rows order differently by the return event: this proves the list
        // chooses its event date rather than reusing one arbitrary timestamp.
        assertEquals(List.of(2L, 9L),
                service.getProposalsForCurrentUser(null, null, null, "returnedAt")
                        .stream().map(Proposal::getId).toList());
    }

    @Test
    void anUnrecognisedSortLeavesTheDefaultOrderAlone() {
        authenticate(2L, "RPS_STAFF");
        Proposal first = proposal(3L, 7L);
        first.setSubmittedAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        Proposal second = proposal(4L, 7L);
        second.setSubmittedAt(LocalDateTime.of(2026, 11, 19, 15, 30));
        when(proposalRepository.findAll()).thenReturn(List.of(first, second));

        List<Proposal> result = service.getProposalsForCurrentUser(null, null, null, "not-a-column");

        assertEquals(List.of(3L, 4L), result.stream().map(Proposal::getId).toList());
    }

    @Test
    void endorsingAProposalRecordsTheEndorsementTimestamp() {
        authenticate(2L, "RPS_STAFF");
        Proposal submitted = proposal(5L, 7L);
        submitted.setStatus("FOR REVIEW");
        when(proposalRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(submitted));

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Proposal saved = service.endorseToOvcrige(5L);

        assertEquals("ENDORSED", saved.getStatus());
        assertNotNull(saved.getEndorsedAt(), "the endorsement event is recorded");
        assertTrue(saved.getEndorsedAt().isAfter(before));
    }

    @Test
    void theDefaultListOrderIsUnchangedWithoutTheSubmittedAtSort() {
        authenticate(2L, "RPS_STAFF");
        Proposal first = proposal(3L, 7L);
        first.setSubmittedAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        Proposal second = proposal(4L, 7L);
        second.setSubmittedAt(LocalDateTime.of(2026, 11, 19, 15, 30));
        when(proposalRepository.findAll()).thenReturn(List.of(first, second));

        List<Proposal> result = service.getProposalsForCurrentUser(null, null, null);

        assertEquals(List.of(3L, 4L), result.stream().map(Proposal::getId).toList());
    }

    @Test
    void submittingAProposalRecordsTheActualSubmissionTimestamp() {
        User me = user(7L);
        authenticate(7L, "PROPONENT");
        when(userRepository.findById(7L)).thenReturn(Optional.of(me));
        when(proposalRepository.nextProposalCodeSequence()).thenReturn(23L);
        ProposalAnnouncement announcement = new ProposalAnnouncement();
        announcement.setId(1L);
        announcement.setStatus(ProposalAnnouncement.STATUS_ACTIVE);
        announcement.setStartDate(LocalDate.now().minusDays(1));
        announcement.setEndDate(LocalDate.now().plusDays(1));
        when(announcementService.getActiveAnnouncementOrNull()).thenReturn(announcement);

        ProposalRequest request = new ProposalRequest();
        request.setProjectTitle("Submitted Proposal");
        request.setDuration("12");
        request.setStatus("SUBMITTED");
        // The server requires a complete GAD Checklist 2 on a final submission.
        request.setGadChecklist(completedChecklist());

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Proposal saved = service.saveProposal(request, null);

        assertNotNull(saved.getSubmittedAt(), "the submission event is recorded");
        assertTrue(saved.getSubmittedAt().isAfter(before));
        assertTrue(saved.getSubmittedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void savingAnAlreadySubmittedProposalKeepsItsOriginalSubmissionTimestamp() {
        User me = user(7L);
        authenticate(7L, "PROPONENT");
        Proposal existing = proposal(5L, 7L);
        existing.setStatus("FOR REVIEW");
        LocalDateTime original = LocalDateTime.of(2026, 9, 1, 10, 0);
        existing.setSubmittedAt(original);
        when(proposalRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(existing));
        ProposalAnnouncement announcement = new ProposalAnnouncement();
        announcement.setId(1L);
        announcement.setStatus(ProposalAnnouncement.STATUS_ACTIVE);
        announcement.setStartDate(LocalDate.now().minusDays(1));
        announcement.setEndDate(LocalDate.now().plusDays(1));
        when(announcementService.getActiveAnnouncementOrNull()).thenReturn(announcement);

        ProposalRequest request = new ProposalRequest();
        // A client that re-sends the same submitted status (an edit, a file upload).
        request.setStatus("FOR REVIEW");

        Proposal saved = service.saveProposal(request, 5L);

        assertEquals(original, saved.getSubmittedAt(),
                "a later save must never rewrite the original submission date");
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
