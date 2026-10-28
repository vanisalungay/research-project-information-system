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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.rpis.backend.storage.LocalUploadStorage;

/**
 * Unit tests for the proponent-selected project duration rule. Only 12, 24 or 36
 * months are accepted; the rule is enforced in the service so a modified API
 * request cannot persist an unsupported value on a new proposal.
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
                announcementService, versionService, mock(ProposalSectionRemarkService.class),
                new CurrentUser(), mock(CooperatingAgencyService.class), mock(PriorityAgendaService.class), new LocalUploadStorage("uploads"));

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

    /** A draft proposal with a project period. */
    private ProposalRequest datedRequest(String duration, String startDate, String endDate) {
        ProposalRequest req = newProposalRequest(duration);
        req.setStartDate(startDate);
        req.setEndDate(endDate);
        return req;
    }

    @Test
    void newProposalSavesSelectedTwelveMonthDuration() {
        Proposal saved = service.saveProposal(newProposalRequest("12"), null);
        assertEquals("12", saved.getDuration());
    }

    @Test
    void newProposalSavesSelectedTwentyFourMonthDuration() {
        Proposal saved = service.saveProposal(newProposalRequest("24"), null);
        assertEquals("24", saved.getDuration());
    }

    @Test
    void newProposalSavesSelectedThirtySixMonthDuration() {
        Proposal saved = service.saveProposal(newProposalRequest("36"), null);
        assertEquals("36", saved.getDuration());
    }

    @Test
    void unsupportedDurationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.saveProposal(newProposalRequest("5"), null));
    }

    @Test
    void tamperedUnsupportedDurationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.saveProposal(newProposalRequest("999"), null));
    }

    // ===== Calculated End Date: start date + duration (the day of the month is kept) =====

    @Test
    void endDateIsTheStartDatePlusTheDuration() {
        // The examples of the requirement: 12 months from 1 October 2026 ends 1 October 2027.
        Proposal saved = service.saveProposal(datedRequest("12", "2026-10-01", "2027-10-01"), null);
        assertEquals(LocalDate.parse("2027-10-01"), saved.getEndDate());

        service.saveProposal(datedRequest("12", "2026-12-01", "2027-12-01"), null);
        service.saveProposal(datedRequest("24", "2026-10-01", "2028-10-01"), null);
        service.saveProposal(datedRequest("36", "2026-10-01", "2029-10-01"), null);
    }

    @Test
    void endDateThatIsNotTheCalculatedDateIsRejected() {
        // One day before the calculated end date is still not the calculated date.
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveProposal(datedRequest("12", "2026-10-01", "2027-09-30"), null));
        assertTrue(error.getMessage().contains("must end on 2027-10-01"), error.getMessage());

        // ... and neither is a day after it.
        assertThrows(IllegalArgumentException.class,
                () -> service.saveProposal(datedRequest("12", "2026-10-01", "2027-10-02"), null));
    }

    @Test
    void endDateEqualToTheStartDateIsRejectedForAMultiMonthDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> service.saveProposal(datedRequest("12", "2026-10-01", "2026-10-01"), null));
    }

    @Test
    void endDateEarlierThanTheStartDateIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveProposal(datedRequest("12", "2026-10-01", "2026-09-01"), null));
        assertTrue(error.getMessage().contains("cannot be earlier than the start date"),
                error.getMessage());
    }

    @Test
    void endDateUsesTheLastValidDayWhenTheTargetMonthIsShorter() {
        // 31 January + 12 months is 31 January of the next year (the day exists again) ...
        service.saveProposal(datedRequest("12", "2026-01-31", "2027-01-31"), null);
        // ... while a date the target month does not contain is clamped to its last day.
        assertThrows(IllegalArgumentException.class,
                () -> service.saveProposal(datedRequest("12", "2028-02-29", "2029-03-01"), null));
    }

    @Test
    void endDateRuleSkipsAMissingDate() {
        // A draft may be saved before both dates are known; no end date can be calculated yet.
        Proposal saved = service.saveProposal(datedRequest("12", "2026-10-01", ""), null);
        assertEquals(LocalDate.parse("2026-10-01"), saved.getStartDate());
        assertNull(saved.getEndDate());
    }
}
