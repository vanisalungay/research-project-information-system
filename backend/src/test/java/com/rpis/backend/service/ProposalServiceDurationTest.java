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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the fixed 12-month duration rule that applies to new internally
 * funded MSUN proposals. The rule is enforced in the service so a modified API
 * request cannot persist a different duration.
 */
class ProposalServiceDurationTest {

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
                announcementService, versionService, new CurrentUser());

        when(proposalRepository.nextProposalCodeSequence()).thenReturn(1L);
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));

        ProposalAnnouncement announcement = new ProposalAnnouncement();
        announcement.setId(1L);
        announcement.setName("2026 Call for Proposals");
        announcement.setStartDate(LocalDate.now().minusDays(1));
        announcement.setEndDate(LocalDate.now().plusDays(1));
        announcement.setStatus(ProposalAnnouncement.STATUS_ACTIVE);
        when(announcementService.getActiveAnnouncementOrNull()).thenReturn(announcement);

        User proponent = new User();
        proponent.setId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(proponent));

        // The duration rule is enforced regardless of who creates the proposal; run
        // as an authorized staff role so the proposal is created on behalf of
        // proponent 5.
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                2L, null, List.of(new SimpleGrantedAuthority("ROLE_RPS_STAFF"))));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private ProposalRequest newProposalRequest(String duration) {
        ProposalRequest req = new ProposalRequest();
        req.setProponentId(5L);
        req.setProjectTitle("Internally Funded Test Project");
        req.setDuration(duration);
        req.setStatus("DRAFT");
        return req;
    }

    @Test
    void newInternallyFundedProposalAlwaysSavesTwelveMonths() {
        Proposal saved = service.saveProposal(newProposalRequest("5"), null);
        assertEquals("12", saved.getDuration());
    }

    @Test
    void tamperedDurationOnANewProposalIsIgnored() {
        Proposal saved = service.saveProposal(newProposalRequest("999"), null);
        assertEquals("12", saved.getDuration());
    }
}
