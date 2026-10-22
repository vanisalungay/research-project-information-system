package com.rpis.backend.service;

import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL-backed tests for the list behind the first step of the Create Proposal
 * workflow (GET /api/proposal-announcements/available): the published calls that are
 * open for submissions right now plus the published calls that open later
 * (scheduled).
 * <p>
 * The rules are the ones the rest of RPIS already uses: only status ACTIVE calls are
 * shown, a call is Open when today is inside [startDate, endDate] (inclusive) and
 * Scheduled when its start date is still ahead. Drafts, CLOSED calls and calls whose
 * deadline has passed are never returned, and the order is "open calls first, then
 * the upcoming ones by opening date".
 * <p>
 * Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProposalAnnouncementAvailableCallsPostgresTest {

    @Autowired
    private ProposalAnnouncementService service;
    @Autowired
    private ProposalAnnouncementRepository announcementRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE proposal_announcements RESTART IDENTITY CASCADE");
    }

    private ProposalAnnouncement save(String name, String status, LocalDate start, LocalDate end) {
        ProposalAnnouncement announcement = new ProposalAnnouncement();
        announcement.setName(name);
        announcement.setStatus(status);
        announcement.setStartDate(start);
        announcement.setEndDate(end);
        return announcementRepository.saveAndFlush(announcement);
    }

    private List<String> availableNames() {
        return service.getAvailableAnnouncements().stream()
                .map(ProposalAnnouncement::getName)
                .toList();
    }

    /** The display status the page shows for a saved call (Draft/Scheduled/Open/Closed). */
    private String statusOf(String name) {
        ProposalAnnouncement announcement = announcementRepository.findAll().stream()
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .orElseThrow();
        LocalDate today = LocalDate.now();
        if (announcement.getStartDate().isAfter(today)) {
            return "Scheduled";
        }
        return announcement.getEndDate().isBefore(today) ? "Closed" : "Open";
    }

    @Test
    void openAndUpcomingCallsAreListedWhileDraftsClosedAndExpiredCallsAreNot() {
        LocalDate today = LocalDate.now();
        save("Open call", ProposalAnnouncement.STATUS_ACTIVE, today.minusDays(2), today.plusDays(3));
        save("Upcoming call", ProposalAnnouncement.STATUS_ACTIVE, today.plusDays(4), today.plusDays(9));
        save("Draft call", ProposalAnnouncement.STATUS_DRAFT, today.minusDays(2), today.plusDays(3));
        save("Draft upcoming call", ProposalAnnouncement.STATUS_DRAFT, today.plusDays(4), today.plusDays(9));
        save("Closed call", ProposalAnnouncement.STATUS_CLOSED, today.minusDays(2), today.plusDays(3));
        save("Expired call", ProposalAnnouncement.STATUS_ACTIVE, today.minusDays(9), today.minusDays(2));

        assertEquals(List.of("Open call", "Upcoming call"), availableNames());
    }

    @Test
    void aCallIsOpenOnItsFirstDayAndScheduledTheDayBefore() {
        LocalDate today = LocalDate.now();
        save("Starts today", ProposalAnnouncement.STATUS_ACTIVE, today, today.plusDays(3));
        save("Starts tomorrow", ProposalAnnouncement.STATUS_ACTIVE, today.plusDays(1), today.plusDays(4));

        // Both are shown, but only the one whose window has started is open.
        assertEquals(List.of("Starts today", "Starts tomorrow"), availableNames());
        assertEquals("Open", statusOf("Starts today"));
        assertEquals("Scheduled", statusOf("Starts tomorrow"));
    }

    @Test
    void theLastDayOfTheWindowIsStillOpen() {
        LocalDate today = LocalDate.now();
        save("Closes today", ProposalAnnouncement.STATUS_ACTIVE, today.minusDays(4), today);

        assertEquals(List.of("Closes today"), availableNames());
        assertEquals("Open", statusOf("Closes today"));
    }

    @Test
    void aCallWhoseDeadlineHasPassedIsNoLongerListed() {
        LocalDate today = LocalDate.now();
        save("Ended yesterday", ProposalAnnouncement.STATUS_ACTIVE, today.minusDays(9), today.minusDays(1));

        assertEquals(List.of(), availableNames());
    }

    @Test
    void openCallsComeFirstAndUpcomingCallsFollowByOpeningDate() {
        LocalDate today = LocalDate.now();
        save("Upcoming later", ProposalAnnouncement.STATUS_ACTIVE, today.plusDays(20), today.plusDays(25));
        save("Upcoming sooner", ProposalAnnouncement.STATUS_ACTIVE, today.plusDays(5), today.plusDays(10));
        save("Open newer window", ProposalAnnouncement.STATUS_ACTIVE, today.minusDays(1), today.plusDays(2));
        save("Open older window", ProposalAnnouncement.STATUS_ACTIVE, today.minusDays(10), today.plusDays(30));

        assertEquals(
                List.of("Open newer window", "Open older window", "Upcoming sooner", "Upcoming later"),
                availableNames());
    }

    @Test
    void nothingIsListedWhenNoCallIsOpenOrUpcoming() {
        LocalDate today = LocalDate.now();
        save("Draft call", ProposalAnnouncement.STATUS_DRAFT, today, today.plusDays(3));
        save("Expired call", ProposalAnnouncement.STATUS_ACTIVE, today.minusDays(9), today.minusDays(2));

        assertEquals(List.of(), availableNames());
    }

    @Test
    void anUpcomingCallIsListedButCannotBeSelectedForAProposalYet() {
        LocalDate today = LocalDate.now();
        ProposalAnnouncement upcoming = save("Upcoming call", ProposalAnnouncement.STATUS_ACTIVE,
                today.plusDays(2), today.plusDays(7));

        assertTrue(service.getAvailableAnnouncements().stream()
                .anyMatch(a -> a.getId().equals(upcoming.getId())));
        // The submission path refuses it: the button is disabled in the UI and the
        // server-side gatekeeper is the authoritative check.
        assertThrows(IllegalStateException.class,
                () -> service.requireOpenAnnouncement(upcoming.getId()));
    }
}
