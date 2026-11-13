package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalAnnouncementRequest;
import com.rpis.backend.model.Notification;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.NotificationRepository;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL-backed tests for proposal announcement creation, focused on the
 * removal of the date-overlap restriction. Overlapping announcements must be
 * allowed, while the existing field/date validation, status handling and
 * notification behaviour are preserved.
 * <p>
 * Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProposalAnnouncementServicePostgresTest {

    @Autowired
    private ProposalAnnouncementService service;
    @Autowired
    private ProposalAnnouncementRepository announcementRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User proponent;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposal_announcements RESTART IDENTITY CASCADE");

        User user = new User();
        user.setName("Test Proponent");
        user.setEmail("overlap.proponent@msunaawan.edu.ph");
        user.setPassword("secret");
        user.setRole("PROPONENT");
        user.setStatus("APPROVED");
        proponent = userRepository.saveAndFlush(user);
    }

    private ProposalAnnouncementRequest request(String name, String start, String end, String status) {
        ProposalAnnouncementRequest request = new ProposalAnnouncementRequest();
        request.setName(name);
        request.setStartDate(LocalDate.parse(start));
        request.setEndDate(LocalDate.parse(end));
        request.setStatus(status);
        return request;
    }

    // 1. Overlap with an existing Open announcement -> creation succeeds.
    @Test
    void overlappingWithAnExistingOpenAnnouncementIsAllowed() {
        service.createAnnouncement(request("Call for Proposal TESTINGG", "2026-10-01", "2026-10-31", "ACTIVE"));
        ProposalAnnouncement created = service.createAnnouncement(request("New Call", "2026-10-13", "2026-10-31", "ACTIVE"));

        assertNotNull(created.getId());
        assertEquals(2, announcementRepository.count());
    }

    // 2. Identical start and end dates -> both allowed.
    @Test
    void identicalStartAndEndDatesAreAllowed() {
        service.createAnnouncement(request("A", "2026-10-01", "2026-10-31", "DRAFT"));
        service.createAnnouncement(request("B", "2026-10-01", "2026-10-31", "DRAFT"));

        assertEquals(2, announcementRepository.count());
    }

    // 3. Partially overlapping ranges -> both allowed.
    @Test
    void partiallyOverlappingRangesAreAllowed() {
        service.createAnnouncement(request("A", "2026-10-01", "2026-10-20", "DRAFT"));
        service.createAnnouncement(request("B", "2026-10-15", "2026-10-31", "DRAFT"));

        assertEquals(2, announcementRepository.count());
    }

    // 4. Non-overlapping ranges -> still work.
    @Test
    void nonOverlappingRangesStillWork() {
        service.createAnnouncement(request("A", "2026-01-01", "2026-01-31", "DRAFT"));
        service.createAnnouncement(request("B", "2026-03-01", "2026-03-31", "DRAFT"));

        assertEquals(2, announcementRepository.count());
    }

    // 5. Invalid date range (end before start) -> still rejected.
    @Test
    void endDateBeforeStartDateIsStillRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createAnnouncement(request("Invalid", "2026-10-31", "2026-10-01", "DRAFT")));
        assertEquals(0, announcementRepository.count());
    }

    // 5b. Required fields are still validated (name required).
    @Test
    void requiredFieldsAreStillValidated() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createAnnouncement(request("", "2026-10-01", "2026-10-31", "DRAFT")));
    }

    // 6. Overlapping announcements keep their own status.
    @Test
    void overlappingAnnouncementsKeepTheirOwnStatus() {
        service.createAnnouncement(request("Open Call", "2026-10-01", "2026-10-31", "ACTIVE"));
        service.createAnnouncement(request("Draft Call", "2026-10-13", "2026-10-31", "DRAFT"));

        List<ProposalAnnouncement> all = announcementRepository.findAll();
        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(a -> "Open Call".equals(a.getName()) && "ACTIVE".equals(a.getStatus())));
        assertTrue(all.stream().anyMatch(a -> "Draft Call".equals(a.getName()) && "DRAFT".equals(a.getStatus())));
    }

    // 7. Saving with Open (ACTIVE) status still notifies proponents.
    @Test
    void savingWithOpenStatusStillNotifiesProponents() {
        service.createAnnouncement(request("Open Call", "2026-10-01", "2026-10-31", "ACTIVE"));

        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(proponent);
        assertEquals(1, notifications.size());
        assertEquals("PROPOSAL_ANNOUNCEMENT", notifications.get(0).getType());
    }

    @Test
    void savingWithDraftStatusDoesNotNotifyProponents() {
        service.createAnnouncement(request("Draft Call", "2026-10-01", "2026-10-31", "DRAFT"));

        assertTrue(notificationRepository.findByUserOrderByCreatedAtDesc(proponent).isEmpty());
    }

    // 9. The ORPS announcement list is ordered by Start Date descending (newest first).
    @Test
    void announcementListIsOrderedByStartDateDescending() {
        service.createAnnouncement(request("September Call", "2026-09-28", "2026-09-30", "DRAFT"));
        service.createAnnouncement(request("October Call", "2026-10-01", "2026-10-31", "DRAFT"));
        service.createAnnouncement(request("November Call", "2026-11-02", "2026-11-30", "DRAFT"));
        service.createAnnouncement(request("December Call", "2026-12-01", "2026-12-30", "DRAFT"));

        List<String> names = service.getAllAnnouncements().stream()
                .map(ProposalAnnouncement::getName)
                .toList();

        assertEquals(List.of("December Call", "November Call", "October Call", "September Call"), names);
    }

    // 10. Announcements sharing a Start Date keep a stable creation-order tie-break.
    @Test
    void announcementsWithTheSameStartDateUseCreationOrderAsTieBreaker() {
        service.createAnnouncement(request("First Same Start", "2026-10-14", "2026-10-29", "DRAFT"));
        service.createAnnouncement(request("Second Same Start", "2026-10-14", "2026-10-31", "DRAFT"));

        List<String> names = service.getAllAnnouncements().stream()
                .map(ProposalAnnouncement::getName)
                .toList();

        // Most recently created first, and repeating the read proves the order is stable.
        assertEquals(List.of("Second Same Start", "First Same Start"), names);
        assertEquals(names, service.getAllAnnouncements().stream()
                .map(ProposalAnnouncement::getName)
                .toList());
    }
}
