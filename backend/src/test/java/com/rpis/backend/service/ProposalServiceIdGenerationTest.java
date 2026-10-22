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
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that proposal codes are allocated from the atomic database sequence
 * (never by counting/MAX()+1) and that simultaneous submissions always receive
 * distinct sequential codes. The mocked repository models the atomic
 * {@code nextval('proposal_code_seq')} call with an {@link AtomicLong}.
 */
class ProposalServiceIdGenerationTest {

    private static final int CONCURRENT_SUBMISSIONS = 25;
    private static final int FIRST_SEQUENCE_VALUE = 21; // -> next allocation is 022

    private ProposalRepository proposalRepository;
    private ProposalService service;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        ProposalAnnouncementService announcementService = mock(ProposalAnnouncementService.class);
        ProposalVersionService versionService = mock(ProposalVersionService.class);
        service = new ProposalService(proposalRepository, userRepository, notificationService,
                announcementService, versionService, mock(ProposalSectionRemarkService.class),
                new CurrentUser(), mock(CooperatingAgencyService.class));

        AtomicLong sequence = new AtomicLong(FIRST_SEQUENCE_VALUE);
        when(proposalRepository.nextProposalCodeSequence()).thenAnswer(inv -> sequence.incrementAndGet());
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAll()).thenReturn(List.of());

        User proponent = new User();
        proponent.setId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(proponent));

        ProposalAnnouncement announcement = new ProposalAnnouncement();
        announcement.setId(1L);
        announcement.setStatus(ProposalAnnouncement.STATUS_ACTIVE);
        announcement.setStartDate(LocalDate.now().minusDays(1));
        announcement.setEndDate(LocalDate.now().plusDays(1));
        when(announcementService.getActiveAnnouncementOrNull()).thenReturn(announcement);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                5L, null, List.of(new SimpleGrantedAuthority("ROLE_PROPONENT"))));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private ProposalRequest newProposalRequest() {
        ProposalRequest request = new ProposalRequest();
        request.setProjectTitle("Concurrent Test Proposal");
        request.setDuration("12");
        request.setStatus("DRAFT");
        return request;
    }

    @Test
    void newProposalTakesItsCodeFromTheSequenceAndPreservesTheIdFormat() {
        Proposal saved = service.saveProposal(newProposalRequest(), null);

        assertEquals("022", saved.getProposalCode());
        assertEquals(0, saved.getRevisionNumber());
        assertEquals("MSUN-ORPS-DA-022-" + Year.now().getValue() + "-REV00", saved.getDocumentId());
    }

    @Test
    void simultaneousSubmissionsReceiveDistinctSequentialCodes() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_SUBMISSIONS);
        CountDownLatch startGate = new CountDownLatch(1);
        Set<String> codes = ConcurrentHashMap.newKeySet();

        try {
            List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < CONCURRENT_SUBMISSIONS; i++) {
                futures.add(pool.submit(() -> {
                    // SecurityContextHolder is thread-local; in production the token
                    // filter authenticates each request thread, so each worker thread
                    // here must authenticate itself the same way.
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                            5L, null, List.of(new SimpleGrantedAuthority("ROLE_PROPONENT"))));
                    try {
                        startGate.await();
                        codes.add(service.saveProposal(newProposalRequest(), null).getProposalCode());
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                    return null;
                }));
            }
            startGate.countDown();
            for (java.util.concurrent.Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(CONCURRENT_SUBMISSIONS, codes.size(), "every submission must receive a unique code");
        int first = FIRST_SEQUENCE_VALUE + 1;
        int last = FIRST_SEQUENCE_VALUE + CONCURRENT_SUBMISSIONS;
        for (int n = first; n <= last; n++) {
            assertTrue(codes.contains(String.format("%03d", n)),
                    "expected code " + String.format("%03d", n) + " in " + codes);
        }
    }
}
