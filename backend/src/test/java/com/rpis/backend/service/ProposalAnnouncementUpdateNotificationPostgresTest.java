package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalAnnouncementRequest;
import com.rpis.backend.dto.ProposalAnnouncementUpdateRequest;
import com.rpis.backend.model.Notification;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.ProposalAnnouncementUpdate;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.NotificationRepository;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.ProposalAnnouncementUpdateRepository;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private ProposalAnnouncementUpdateRepository updateRepository;
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

    /** The same "MMMM d, yyyy" rendering the notification messages use. */
    private static String formattedDaysFromToday(long days) {
        return LocalDate.parse(daysFromToday(days))
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
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

    /**
     * The payload of an update: only the submission window and the status can be changed.
     * The name, priority agenda, reminders and additional announcement are not part of an
     * update any more, so this helper cannot send them.
     */
    private ProposalAnnouncementUpdateRequest updateRequest(String start, String end, String status) {
        ProposalAnnouncementUpdateRequest update = new ProposalAnnouncementUpdateRequest();
        update.setStartDate(LocalDate.parse(start));
        update.setEndDate(LocalDate.parse(end));
        update.setStatus(status);
        return update;
    }

    private List<Notification> notifications() {
        return notificationRepository.findByUserOrderByCreatedAtDesc(proponent);
    }

    private boolean hasNotification(String title) {
        return notifications().stream().anyMatch(n -> title.equals(n.getTitle()));
    }

    // 1. Extending the deadline of an existing Open announcement notifies proponents with
    // the specific "deadline extended" notification, carrying both real deadlines.
    @Test
    void extendingAnOpenAnnouncementNotifiesProponents() {
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Open Call"));
        assertEquals(1, notifications().size(), "creating an open call notifies once");

        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(-1), daysFromToday(12), "ACTIVE"));

        assertEquals(2, notifications().size());
        assertTrue(hasNotification("Call for Proposal Deadline Extended"));
        Notification latest = notifications().stream()
                .filter(n -> "Call for Proposal Deadline Extended".equals(n.getTitle()))
                .findFirst()
                .orElseThrow();
        assertTrue(latest.getMessage().contains("Open Call"));
        assertTrue(latest.getMessage().contains(formattedDaysFromToday(5)),
                "the previous deadline is named in the message");
        assertTrue(latest.getMessage().contains(formattedDaysFromToday(12)),
                "the new deadline is named in the message");
        assertEquals("PROPOSAL_ANNOUNCEMENT", latest.getType());
        assertFalse(latest.getIsRead());

        // The edit itself still works exactly as before.
        ProposalAnnouncement saved = announcementRepository.findById(created.getId()).orElseThrow();
        assertEquals(LocalDate.parse(daysFromToday(12)), saved.getEndDate());
        assertEquals("ACTIVE", saved.getStatus());
    }

    // 2. Editing an existing Closed announcement notifies proponents (its submission
    //    window is the only thing an update can change).
    @Test
    void editingAClosedAnnouncementNotifiesProponents() {
        ProposalAnnouncement created = service.createAnnouncement(
                request("Closed Call", daysFromToday(-20), daysFromToday(-10), "CLOSED"));
        assertTrue(notifications().isEmpty(), "creating a closed call notifies nobody");

        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(-20), daysFromToday(-2), "CLOSED"));

        assertEquals(1, notifications().size());
        assertTrue(hasNotification("Call for Proposal Deadline Extended"),
                "moving the deadline later is still announced as an extension");
        assertEquals("Closed Call",
                announcementRepository.findById(created.getId()).orElseThrow().getName(),
                "the name the call was published with is kept");
    }

    // 3. An update never changes the fields that are set when the call is created: the
    //    name, the RIIDE priority agenda, the announcement reminders and the additional
    //    announcement keep their stored values.
    @Test
    void anUpdateKeepsTheNameTheAgendaTheRemindersAndTheAdditionalAnnouncement() {
        ProposalAnnouncementRequest create = openRequest("Original Name");
        create.setPriorityAgendas(List.of("dagat"));
        create.setReminderIds(List.of("research-plan"));
        create.setAdditionalAnnouncement("Original body.");
        ProposalAnnouncement created = service.createAnnouncement(create);

        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(-1), daysFromToday(18), "ACTIVE"));

        ProposalAnnouncement saved = announcementRepository.findById(created.getId()).orElseThrow();
        assertEquals("Original Name", saved.getName());
        assertEquals(List.of("dagat"), saved.getPriorityAgendas());
        assertEquals(List.of("research-plan"), saved.getReminderIds());
        assertEquals("Original body.", saved.getAdditionalAnnouncement());
        assertEquals(LocalDate.parse(daysFromToday(18)), saved.getEndDate(),
                "the deadline itself is still updated");
    }

    // 4. Closing an Open call uses the specific "closed" notification.
    @Test
    void closingAnOpenAnnouncementSendsTheClosedNotification() {
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Closing Call"));

        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(-1), daysFromToday(5), "CLOSED"));

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
                updateRequest(daysFromToday(10), daysFromToday(30), "ACTIVE"));

        assertEquals(1, notifications().size(), "editing a scheduled call adds no update notification");
        assertFalse(hasNotification("Call for Proposals Updated"));
        assertFalse(hasNotification("Submission Deadline Updated"));
        assertFalse(hasNotification("Call for Proposals Closed"));
    }

    // 6. Scheduled -> Open notifies with the specific "now open" message.
    @Test
    void changingAScheduledAnnouncementToOpenSendsTheNowOpenNotification() {
        ProposalAnnouncement created = service.createAnnouncement(scheduledRequest("Future Call"));
        assertEquals(1, notifications().size());

        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(-1), daysFromToday(5), "ACTIVE"));

        assertEquals(2, notifications().size(), "Scheduled -> Open adds the now-open notification");
        assertTrue(hasNotification("Call for Proposal Now Open"));
        Notification nowOpen = notifications().stream()
                .filter(n -> "Call for Proposal Now Open".equals(n.getTitle()))
                .findFirst()
                .orElseThrow();
        assertTrue(nowOpen.getMessage().contains("Future Call"));
        assertTrue(nowOpen.getMessage().contains(formattedDaysFromToday(5)),
                "the real deadline is named in the message");
        assertFalse(hasNotification("Call for Proposals Updated"));
        assertFalse(hasNotification("Submission Deadline Updated"));
    }

    // 7. Scheduled -> Closed must not send the new update notification either.
    @Test
    void changingAScheduledAnnouncementToClosedSendsNoUpdateNotification() {
        ProposalAnnouncement created = service.createAnnouncement(scheduledRequest("Future Call 2"));
        assertEquals(1, notifications().size());

        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(10), daysFromToday(20), "CLOSED"));

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
                updateRequest(daysFromToday(5), daysFromToday(1), "ACTIVE")));

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
        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(-1), daysFromToday(5), "ACTIVE"));

        assertEquals(1, notifications().size(), "an unchanged save must not notify");
    }

    // 11. Every eligible proponent receives exactly one notification per update.
    @Test
    void eachEligibleProponentReceivesExactlyOneNotificationPerUpdate() {
        User second = createUser("proponent2@msunaawan.edu.ph", "PROPONENT", "APPROVED");
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Shared Call"));

        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(-1), daysFromToday(15), "ACTIVE"));

        assertEquals(1, notifications().stream()
                .filter(n -> "Call for Proposal Deadline Extended".equals(n.getTitle())).count());
        assertEquals(1, notificationRepository.findByUserOrderByCreatedAtDesc(second).stream()
                .filter(n -> "Call for Proposal Deadline Extended".equals(n.getTitle())).count());
    }

    // 12. Only the established recipient group (approved proponents) is notified.
    @Test
    void ineligibleAccountsDoNotReceiveTheUpdateNotification() {
        User pending = createUser("pending@msunaawan.edu.ph", "PROPONENT", "PENDING");
        User staff = createUser("staff@msunaawan.edu.ph", "RPS_ADMIN", "APPROVED");
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Eligibility Call"));

        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(-1), daysFromToday(9), "ACTIVE"));

        assertTrue(notificationRepository.findByUserOrderByCreatedAtDesc(pending).isEmpty());
        assertTrue(notificationRepository.findByUserOrderByCreatedAtDesc(staff).isEmpty());
        assertTrue(hasNotification("Call for Proposal Deadline Extended"),
                "the approved proponent is still notified");
    }

    // 13. The pre-existing "call is open" notification is preserved for Draft -> Active.
    @Test
    void activatingADraftAnnouncementStillSendsTheExistingOpenNotification() {
        ProposalAnnouncement created = service.createAnnouncement(
                request("Draft Call", daysFromToday(-1), daysFromToday(5), "DRAFT"));
        assertTrue(notifications().isEmpty());

        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(-1), daysFromToday(5), "ACTIVE"));

        assertEquals(1, notifications().size());
        assertTrue(hasNotification("New Proposal Announcement Open"));
        assertFalse(hasNotification("Call for Proposals Updated"));
    }

    // 14. The new notification is visible through the existing notification listing.
    @Test
    void theUpdateNotificationAppearsInTheExistingNotificationListing() {
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Listed Call"));

        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(-1), daysFromToday(20), "ACTIVE"));

        List<Notification> listed = notificationService.getNotificationsForUser(proponent.getId());
        assertEquals(2, listed.size());
        Notification update = listed.stream()
                .filter(n -> "Call for Proposal Deadline Extended".equals(n.getTitle()))
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

        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(-1), daysFromToday(5), "ACTIVE"));

        // Two distinct notifications, not duplicates: the pre-existing "call is open" one
        // plus the new update notification. Because the edit also moved the submission
        // window, the most specific update message (deadline) is the one that is sent.
        assertEquals(2, notifications().size());
        assertTrue(hasNotification("New Proposal Announcement Open"),
                "pre-existing opening notification is preserved");
        assertTrue(hasNotification("Call for Proposal Deadline Extended"),
                "the new update notification is added");
    }

    // 16. A start-date change of an Open call is a meaningful edit: the general
    //     "Call for Proposals Updated" notification is sent (no deadline change).
    @Test
    void changingTheStartDateOfAnOpenAnnouncementNotifiesProponents() {
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Window Call"));

        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(-3), daysFromToday(5), "ACTIVE"));

        assertEquals(2, notifications().size());
        assertTrue(hasNotification("Call for Proposals Updated"));
        assertFalse(hasNotification("Submission Deadline Updated"));
    }

    // 17. A start-date-only change is a meaningful update: the general "call updated"
    //     notification is sent (the deadline itself did not move).
    @Test
    void changingOnlyTheOpeningDateNotifiesWithTheGeneralUpdate() {
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Opening Call"));

        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(-4), daysFromToday(5), "ACTIVE"));

        assertEquals(2, notifications().size());
        assertTrue(hasNotification("Call for Proposals Updated"));
        assertFalse(hasNotification("Submission Deadline Updated"));
    }

    // 18. A start-date-only change records the window change in the update history.
    @Test
    void changingOnlyTheOpeningDateIsRecordedInTheUpdateHistory() {
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Recorded Opening Call"));

        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(-4), daysFromToday(5), "ACTIVE"));

        List<ProposalAnnouncementUpdate> history = updateRepository
                .findByAnnouncementIdOrderByCreatedAtAscIdAsc(created.getId());
        assertEquals(2, history.size(), "publication + the window change, never a duplicate");
        assertEquals(ProposalAnnouncementUpdate.TYPE_DETAILS_UPDATED, history.get(1).getUpdateType());
        assertEquals(LocalDate.parse(daysFromToday(-1)), history.get(1).getPreviousStartDate());
        assertEquals(LocalDate.parse(daysFromToday(-4)), history.get(1).getNewStartDate());
    }

    // 19. The update payload cannot carry the restricted fields at all, so a client cannot
    //     change the priority agenda, the reminders or the additional announcement.
    @Test
    void theUpdatePayloadOnlyCarriesTheWindowAndTheStatus() {
        ProposalAnnouncementRequest create = openRequest("Payload Call");
        create.setPriorityAgendas(List.of("dagat"));
        create.setReminderIds(List.of("research-plan"));
        create.setAdditionalAnnouncement("Original body.");
        ProposalAnnouncement created = service.createAnnouncement(create);
        int notificationsAfterCreate = notifications().size();

        // The same window and status again: nothing changed, so nothing is announced.
        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(-1), daysFromToday(5), "ACTIVE"));

        assertEquals(notificationsAfterCreate, notifications().size(),
                "an update that changes nothing notifies nobody");
        ProposalAnnouncement saved = announcementRepository.findById(created.getId()).orElseThrow();
        assertEquals(List.of("dagat"), saved.getPriorityAgendas());
        assertEquals(List.of("research-plan"), saved.getReminderIds());
        assertEquals("Original body.", saved.getAdditionalAnnouncement());
    }

    // 20. An update that omits the status keeps the stored one instead of falling back to
    //     the create-time default.
    @Test
    void anUpdateWithoutAStatusKeepsTheStoredStatus() {
        ProposalAnnouncement created = service.createAnnouncement(openRequest("Status Kept Call"));

        service.updateAnnouncement(created.getId(),
                updateRequest(daysFromToday(-1), daysFromToday(11), null));

        assertEquals("ACTIVE",
                announcementRepository.findById(created.getId()).orElseThrow().getStatus());
        assertTrue(hasNotification("Call for Proposal Deadline Extended"));
    }
}
