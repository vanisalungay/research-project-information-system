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
 * PostgreSQL-backed tests for the Calls for Proposal a proponent may respond to
 * (step 1 of the Create Proposal workflow).
 * <p>
 * The rule is the one the rest of RPIS already uses: a call is open when its stored
 * status is ACTIVE and today falls inside [startDate, endDate] (inclusive). Drafts,
 * scheduled calls (start date still ahead), CLOSED calls and calls whose deadline
 * has passed are never offered, and a call that is not open cannot be selected for a
 * proposal.
 * <p>
 * Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProposalAnnouncementOpenCallsPostgresTest {

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

    private List<String> openNames() {
        return service.getOpenAnnouncements().stream().map(ProposalAnnouncement::getName).toList();
    }

    @Test
    void onlyCallsThatAreActiveAndInsideTheirWindowAreOffered() {
        LocalDate today = LocalDate.now();
        save("Draft call", ProposalAnnouncement.STATUS_DRAFT, today.minusDays(5), today.plusDays(5));
        save("Scheduled call", ProposalAnnouncement.STATUS_ACTIVE, today.plusDays(1), today.plusDays(10));
        save("Closed call", ProposalAnnouncement.STATUS_CLOSED, today.minusDays(10), today.plusDays(10));
        save("Past deadline call", ProposalAnnouncement.STATUS_ACTIVE, today.minusDays(10), today.minusDays(1));
        save("Open call", ProposalAnnouncement.STATUS_ACTIVE, today.minusDays(2), today.plusDays(3));

        assertEquals(List.of("Open call"), openNames());
    }

    @Test
    void theFirstAndTheLastDayOfTheWindowAreStillOpen() {
        LocalDate today = LocalDate.now();
        // Inclusive on both ends, exactly like the submission gatekeeper.
        save("Opens today", ProposalAnnouncement.STATUS_ACTIVE, today, today.plusDays(4));
        save("Closes today", ProposalAnnouncement.STATUS_ACTIVE, today.minusDays(4), today);

        List<String> open = openNames();

        assertTrue(open.contains("Opens today"), "the first day of the window must be open");
        assertTrue(open.contains("Closes today"), "the last day of the window must be open");
    }

    @Test
    void severalOpenCallsAreReturnedNewestWindowFirst() {
        LocalDate today = LocalDate.now();
        save("Earlier window", ProposalAnnouncement.STATUS_ACTIVE, today.minusDays(9), today.plusDays(1));
        save("Later window", ProposalAnnouncement.STATUS_ACTIVE, today.minusDays(1), today.plusDays(9));

        assertEquals(List.of("Later window", "Earlier window"), openNames());
    }

    @Test
    void nothingIsOfferedWhenNoCallIsOpen() {
        LocalDate today = LocalDate.now();
        save("Draft call", ProposalAnnouncement.STATUS_DRAFT, today, today.plusDays(3));
        save("Past deadline call", ProposalAnnouncement.STATUS_ACTIVE, today.minusDays(9), today.minusDays(2));

        assertEquals(List.of(), openNames());
    }

    @Test
    void anOpenCallCanBeSelectedForAProposal() {
        LocalDate today = LocalDate.now();
        ProposalAnnouncement open = save("Open call", ProposalAnnouncement.STATUS_ACTIVE,
                today.minusDays(1), today.plusDays(1));

        assertEquals(open.getId(), service.requireOpenAnnouncement(open.getId()).getId());
    }

    @Test
    void aCallThatIsNotOpenCannotBeSelectedForAProposal() {
        LocalDate today = LocalDate.now();
        Long draft = save("Draft call", ProposalAnnouncement.STATUS_DRAFT, today, today.plusDays(3)).getId();
        Long scheduled = save("Scheduled call", ProposalAnnouncement.STATUS_ACTIVE,
                today.plusDays(1), today.plusDays(5)).getId();
        Long closed = save("Closed call", ProposalAnnouncement.STATUS_CLOSED,
                today.minusDays(5), today.plusDays(5)).getId();
        Long pastDeadline = save("Past deadline call", ProposalAnnouncement.STATUS_ACTIVE,
                today.minusDays(5), today.minusDays(1)).getId();

        for (Long id : List.of(draft, scheduled, closed, pastDeadline)) {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> service.requireOpenAnnouncement(id),
                    "call " + id + " must not be selectable");
            assertEquals("The selected Call for Proposal is no longer open for submissions. "
                    + "Please select an open call for proposal.", error.getMessage());
        }
    }

    @Test
    void anUnknownCallIsReportedAsNotFound() {
        assertThrows(IllegalArgumentException.class, () -> service.requireOpenAnnouncement(9999L));
    }
}
