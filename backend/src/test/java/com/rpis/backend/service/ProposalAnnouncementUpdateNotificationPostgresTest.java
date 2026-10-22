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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL-backed tests for the "Call for Proposals updated" notification: editing an
 * existing announcement that proponents already see as <em>Open</em> or <em>Closed</em>
 * notifies the approved proponents, while <em>Scheduled</em> (and <em>Draft</em>)
 * announcements do not, and the existing creation / activation notifications keep
 * working unchanged.
 * <p>
 * Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProposalAnnouncementUpdateNotificationPostgresTest {

    @Autowired
    private ProposalAnnouncementService service;
    @Autowired
    private ProposalAnnouncementRepository announcementRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User proponent;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE notifications RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposal_announcements RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        proponent = createUser("proponent@msunaawan.edu.ph", "PROPONENT", "APPROVED");
    }

    private User createUser(String email, String role, String status) {
        User user = new User();
        user.setName("Test " + role);
        user.setEmail(email);
        user.setPassword("secret");
        user.setRole(role);
        user.setStatus(status);
        return userRepository.saveAndFlush(user);
    }

    private static String daysFromToday(long days) {
        return LocalDate.now().plusDays(days).toString();
    }

    private ProposalAnnouncementRequest request(String name, String start, String end, String status) {
        ProposalAnnouncementRequest request = new ProposalAnnouncementRequest();
        request.setName(name);
        request.setStartDate(LocalDate.parse(start));
        request.setEndDate(LocalDate.parse(end));
        request.setStatus(status);
        return request;
    }

    /** A stored ACTIVE announcement whose window contains today, i.e. shown as Open. */
    private ProposalAnnouncementRequest openRequest(String name) {
        return request(name, daysFromToday(-1), daysFromToday(5), "ACTIVE");
    }

    /** A stored ACTIVE announcement starting in the future, i.e. shown as Scheduled. */
    private ProposalAnnouncementRequest scheduledRequest(String name) {
        return request(name, daysFromToday(10), daysFromToday(20), "ACTIVE");
    }

    private List<Notification> notifications() {
        return notificationRepository.findByUserOrderByCreatedAtDesc(proponent);
    }

    private boolean hasNotification(String title) {
        return notifications().stream().anyMatch(n -> title.equals(n.getTitle()));
    }

    // 1. Editing an existing Open announcement (deadline change) notifies proponents.
    @Test
    void editingAnOpenAnnouncementNotifiesProponents() {
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Open Call"));
        assertEquals(1, notifications().size(), "creating an open call notifies once");

        service.updateAnnouncement(created.getId(),
                request("Open Call", daysFromToday(-1), daysFromToday(12), "ACTIVE"));

        assertEquals(2, notifications().size());
        assertTrue(hasNotification("Submission Deadline Updated"));
        Notification latest = notifications().stream()
                .filter(n -> "Submission Deadline Updated".equals(n.getTitle()))
                .findFirst()
                .orElseThrow();
        assertTrue(latest.getMessage().contains("Open Call"));
        assertEquals("PROPOSAL_ANNOUNCEMENT", latest.getType());
        assertFalse(latest.getIsRead());

        // The edit itself still works exactly as before.
        ProposalAnnouncement saved = announcementRepository.findById(created.getId()).orElseThrow();
        assertEquals(LocalDate.parse(daysFromToday(12)), saved.getEndDate());
        assertEquals("ACTIVE", saved.getStatus());
    }

    // 2. Editing an existing Closed announcement notifies proponents.
    @Test
    void editingAClosedAnnouncementNotifiesProponents() {
        ProposalAnnouncement created = service.createAnnouncement(
                request("Closed Call", daysFromToday(-20), daysFromToday(-10), "CLOSED"));
        assertTrue(notifications().isEmpty(), "creating a closed call notifies nobody");

        service.updateAnnouncement(created.getId(),
                request("Closed Call (revised)", daysFromToday(-20), daysFromToday(-10), "CLOSED"));

        assertEquals(1, notifications().size());
        assertTrue(hasNotification("Call for Proposals Updated"));
    }

    // 3. Changing the name of an Open announcement notifies proponents.
    @Test
    void changingTheAnnouncementNameNotifiesProponents() {
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Original Name"));

        service.updateAnnouncement(created.getId(), openRequest("Renamed Call"));

        assertTrue(hasNotification("Call for Proposals Updated"));
        assertEquals(2, notifications().size());
    }

    // 4. Closing an Open call uses the specific "closed" notification.
    @Test
    void closingAnOpenAnnouncementSendsTheClosedNotification() {
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Closing Call"));

        service.updateAnnouncement(created.getId(),
                request("Closing Call", daysFromToday(-1), daysFromToday(5), "CLOSED"));

        assertTrue(hasNotification("Call for Proposals Closed"));
        assertFalse(hasNotification("Call for Proposals Updated"));
        assertEquals(2, notifications().size());
    }

    // 5. A Scheduled announcement is never eligible, even when its deadline changes.
    @Test
    void editingAScheduledAnnouncementSendsNoUpdateNotification() {
        ProposalAnnouncement created = service.createAnnouncement(scheduledRequest("Scheduled Call"));
        // Pre-existing behaviour: creating an ACTIVE announcement notifies proponents once.
        assertEquals(1, notifications().size());

        service.updateAnnouncement(created.getId(),
                request("Scheduled Call", daysFromToday(10), daysFromToday(30), "ACTIVE"));

        assertEquals(1, notifications().size(), "editing a scheduled call adds no update notification");
        assertFalse(hasNotification("Call for Proposals Updated"));
        assertFalse(hasNotification("Submission Deadline Updated"));
        assertFalse(hasNotification("Call for Proposals Closed"));
    }

    // 6. Scheduled -> Open is still an edit of a Scheduled announcement: no notification.
    @Test
    void changingAScheduledAnnouncementToOpenSendsNoUpdateNotification() {
        ProposalAnnouncement created = service.createAnnouncement(scheduledRequest("Future Call"));
        assertEquals(1, notifications().size());

        service.updateAnnouncement(created.getId(), openRequest("Future Call"));

        assertEquals(1, notifications().size(), "Scheduled -> Open adds no update notification");
        assertFalse(hasNotification("Call for Proposals Updated"));
        assertFalse(hasNotification("Submission Deadline Updated"));
    }

    // 7. Scheduled -> Closed must not send the new update notification either.
    @Test
    void changingAScheduledAnnouncementToClosedSendsNoUpdateNotification() {
        ProposalAnnouncement created = service.createAnnouncement(scheduledRequest("Future Call 2"));
        assertEquals(1, notifications().size());

        service.updateAnnouncement(created.getId(),
                request("Future Call 2", daysFromToday(10), daysFromToday(20), "CLOSED"));

        assertEquals(1, notifications().size(), "Scheduled -> Closed adds no update notification");
        assertFalse(hasNotification("Call for Proposals Closed"));
        assertFalse(hasNotification("Call for Proposals Updated"));
    }

    // 8. Creating a new announcement only sends the pre-existing "open" notification.
    @Test
    void creatingANewAnnouncementSendsOnlyTheExistingOpenNotification() {
        service.createAnnouncement(openRequest("Brand New Call"));

        assertEquals(1, notifications().size());
        assertTrue(hasNotification("New Proposal Announcement Open"));
        assertFalse(hasNotification("Call for Proposals Updated"));
    }

    // 9. A rejected update notifies nobody and changes nothing.
    @Test
    void aFailedUpdateSendsNoNotification() {
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Valid Call"));

        assertThrows(IllegalArgumentException.class, () -> service.updateAnnouncement(created.getId(),
                request("Valid Call", daysFromToday(5), daysFromToday(1), "ACTIVE")));

        assertEquals(1, notifications().size(), "only the creation notification exists");
        assertFalse(hasNotification("Call for Proposals Updated"));
        assertEquals(LocalDate.parse(daysFromToday(5)),
                announcementRepository.findById(created.getId()).orElseThrow().getEndDate());
    }

    // 10. Saving without changing anything must not send a misleading notification.
    @Test
    void anUnchangedSaveSendsNoUpdateNotification() {
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Unchanged Call"));
        assertEquals(1, notifications().size());

        // Exactly the same values again, as the Edit form sends when nothing was touched.
        service.updateAnnouncement(created.getId(), openRequest("Unchanged Call"));

        assertEquals(1, notifications().size(), "an unchanged save must not notify");
    }

    // 11. Every eligible proponent receives exactly one notification per update.
    @Test
    void eachEligibleProponentReceivesExactlyOneNotificationPerUpdate() {
        User second = createUser("proponent2@msunaawan.edu.ph", "PROPONENT", "APPROVED");
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Shared Call"));

        service.updateAnnouncement(created.getId(),
                request("Shared Call", daysFromToday(-1), daysFromToday(15), "ACTIVE"));

        assertEquals(1, notifications().stream()
                .filter(n -> "Submission Deadline Updated".equals(n.getTitle())).count());
        assertEquals(1, notificationRepository.findByUserOrderByCreatedAtDesc(second).stream()
                .filter(n -> "Submission Deadline Updated".equals(n.getTitle())).count());
    }

    // 12. Only the established recipient group (approved proponents) is notified.
    @Test
    void ineligibleAccountsDoNotReceiveTheUpdateNotification() {
        User pending = createUser("pending@msunaawan.edu.ph", "PROPONENT", "PENDING");
        User staff = createUser("staff@msunaawan.edu.ph", "RPS_ADMIN", "APPROVED");
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Eligibility Call"));

        service.updateAnnouncement(created.getId(),
                request("Eligibility Call", daysFromToday(-1), daysFromToday(9), "ACTIVE"));

        assertTrue(notificationRepository.findByUserOrderByCreatedAtDesc(pending).isEmpty());
        assertTrue(notificationRepository.findByUserOrderByCreatedAtDesc(staff).isEmpty());
        assertTrue(hasNotification("Submission Deadline Updated"),
                "the approved proponent is still notified");
    }

    // 13. The pre-existing "call is open" notification is preserved for Draft -> Active.
    @Test
    void activatingADraftAnnouncementStillSendsTheExistingOpenNotification() {
        ProposalAnnouncement created = service.createAnnouncement(
                request("Draft Call", daysFromToday(-1), daysFromToday(5), "DRAFT"));
        assertTrue(notifications().isEmpty());

        service.updateAnnouncement(created.getId(), openRequest("Draft Call"));

        assertEquals(1, notifications().size());
        assertTrue(hasNotification("New Proposal Announcement Open"));
        assertFalse(hasNotification("Call for Proposals Updated"));
    }

    // 14. The new notification is visible through the existing notification listing.
    @Test
    void theUpdateNotificationAppearsInTheExistingNotificationListing() {
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Listed Call"));

        service.updateAnnouncement(created.getId(),
                request("Listed Call", daysFromToday(-1), daysFromToday(20), "ACTIVE"));

        List<Notification> listed = notificationService.getNotificationsForUser(proponent.getId());
        assertEquals(2, listed.size());
        Notification update = listed.stream()
                .filter(n -> "Submission Deadline Updated".equals(n.getTitle()))
                .findFirst()
                .orElseThrow();
        assertEquals("PROPOSAL_ANNOUNCEMENT", update.getType());
        assertFalse(update.getIsRead());
        assertTrue(update.getCreatedAt() != null);
        assertTrue(update.getMessage().contains("Listed Call"));
    }

    // 15. Reopening a Closed call keeps the existing "call is open" notification and adds
    // the update notification (the eligibility table lists Closed -> Open as eligible,
    // and the pre-existing opening notification must be preserved).
    @Test
    void reopeningAClosedAnnouncementSendsBothNotifications() {
        ProposalAnnouncement created = service.createAnnouncement(
                request("Reopen Call", daysFromToday(-20), daysFromToday(-10), "CLOSED"));
        assertTrue(notifications().isEmpty());

        service.updateAnnouncement(created.getId(), openRequest("Reopen Call"));

        // Two distinct notifications, not duplicates: the pre-existing "call is open" one
        // plus the new update notification. Because the edit also moved the submission
        // window, the most specific update message (deadline) is the one that is sent.
        assertEquals(2, notifications().size());
        assertTrue(hasNotification("New Proposal Announcement Open"),
                "pre-existing opening notification is preserved");
        assertTrue(hasNotification("Submission Deadline Updated"),
                "the new update notification is added");
    }

    // 16. A start-date change of an Open call is a meaningful edit: the general
    //     "Call for Proposals Updated" notification is sent (no deadline change).
    @Test
    void changingTheStartDateOfAnOpenAnnouncementNotifiesProponents() {
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Window Call"));

        service.updateAnnouncement(created.getId(),
                request("Window Call", daysFromToday(-3), daysFromToday(5), "ACTIVE"));

        assertEquals(2, notifications().size());
        assertTrue(hasNotification("Call for Proposals Updated"));
        assertFalse(hasNotification("Submission Deadline Updated"));
    }

    // 17. Changing only the RIIDE priority-agenda selection of an Open call notifies.
    @Test
    void changingThePriorityAgendasOfAnOpenAnnouncementNotifiesProponents() {
        ProposalAnnouncementRequest create = openRequest("Agenda Call");
        create.setPriorityAgendas(List.of("dagat"));
        ProposalAnnouncement created = service.createAnnouncement(create);

        ProposalAnnouncementRequest update = openRequest("Agenda Call");
        update.setPriorityAgendas(List.of("dagat", "punla"));
        service.updateAnnouncement(created.getId(), update);

        assertEquals(2, notifications().size());
        assertTrue(hasNotification("Call for Proposals Updated"));
    }

    // 18. Changing only the reminder selection of an Open call notifies.
    @Test
    void changingTheReminderSelectionOfAnOpenAnnouncementNotifiesProponents() {
        ProposalAnnouncementRequest create = openRequest("Reminder Call");
        create.setReminderIds(List.of("research-plan"));
        ProposalAnnouncement created = service.createAnnouncement(create);

        ProposalAnnouncementRequest update = openRequest("Reminder Call");
        update.setReminderIds(List.of("research-plan", "collaboration"));
        service.updateAnnouncement(created.getId(), update);

        assertEquals(2, notifications().size());
        assertTrue(hasNotification("Call for Proposals Updated"));
    }

    // 19. Changing only the additional announcement text of a Closed call notifies.
    @Test
    void changingTheAdditionalTextOfAClosedAnnouncementNotifiesProponents() {
        ProposalAnnouncementRequest create =
                request("Closed Text Call", daysFromToday(-20), daysFromToday(-10), "CLOSED");
        create.setAdditionalAnnouncement("Original body.");
        ProposalAnnouncement created = service.createAnnouncement(create);
        assertTrue(notifications().isEmpty(), "creating a closed call notifies nobody");

        ProposalAnnouncementRequest update =
                request("Closed Text Call", daysFromToday(-20), daysFromToday(-10), "CLOSED");
        update.setAdditionalAnnouncement("Revised body with new instructions.");
        service.updateAnnouncement(created.getId(), update);

        assertEquals(1, notifications().size());
        assertTrue(hasNotification("Call for Proposals Updated"));
    }

    // 20. Clearing the additional announcement text is also a meaningful edit.
    @Test
    void clearingTheAdditionalTextOfAnOpenAnnouncementNotifiesProponents() {
        ProposalAnnouncementRequest create = openRequest("Cleared Text Call");
        create.setAdditionalAnnouncement("Remove me.");
        ProposalAnnouncement created = service.createAnnouncement(create);

        ProposalAnnouncementRequest update = openRequest("Cleared Text Call");
        update.setAdditionalAnnouncement("");
        service.updateAnnouncement(created.getId(), update);

        assertEquals(2, notifications().size());
        assertTrue(hasNotification("Call for Proposals Updated"));
        assertNull(
                announcementRepository.findById(created.getId()).orElseThrow().getAdditionalAnnouncement());
    }

}
