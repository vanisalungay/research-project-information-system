package com.rpis.backend.controller;

import com.rpis.backend.model.Notification;
import com.rpis.backend.model.ProposalAnnouncementUpdate;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.NotificationRepository;
import com.rpis.backend.repository.ProposalAnnouncementUpdateRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level test for the Call for Proposal update history as the Proponent Side sees it.
 * <p>
 * An RPS Admin publishes a call and then extends its deadline through the real
 * {@code POST/PUT /api/proposal-announcements} endpoints. The proponent view
 * ({@code GET /api/proposal-announcements/{id}/details}) must then show the recorded
 * history oldest first - the original deadline AND every extension - and the
 * notification listing ({@code GET /api/notifications}) must carry the call name and its
 * CURRENT status, so an officially extended call that is still open is never reported as
 * closed because its superseded deadline has passed.
 * <p>
 * Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProposalAnnouncementUpdateHistoryApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private ProposalAnnouncementUpdateRepository updateRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User proponent;
    private User rpsAdmin;
    private String proponentToken;
    private String rpsAdminToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE proposal_announcement_updates RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE notifications RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposal_announcements RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        proponent = createUser("proponent@msunaawan.edu.ph", "PROPONENT", "APPROVED", "Ana Proponent");
        rpsAdmin = createUser("rpsadmin@msunaawan.edu.ph", "RPS_ADMIN", "APPROVED", "Maria Santos");

        proponentToken = tokenService.generateToken(proponent.getId(), proponent.getRole());
        rpsAdminToken = tokenService.generateToken(rpsAdmin.getId(), rpsAdmin.getRole());
    }

    private User createUser(String email, String role, String status, String name) {
        User user = new User();
        user.setName(name);
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

    private static String announcementBody(String name, String start, String end, String status) {
        return "{\"name\":\"" + name + "\",\"startDate\":\"" + start + "\",\"endDate\":\"" + end
                + "\",\"status\":\"" + status + "\"}";
    }

    /** The payload an update sends: only the submission window and the status. */
    private static String updateBody(String start, String end, String status) {
        return "{\"startDate\":\"" + start + "\",\"endDate\":\"" + end
                + "\",\"status\":\"" + status + "\"}";
    }

    /** Publishes a call with a full creation payload (agenda, reminders, additional text). */
    private Long createFullAnnouncement(String name, String start, String end, String status,
            String priorityAgendas, String reminderIds, String additionalAnnouncement)
            throws Exception {
        String body = "{\"name\":\"" + name + "\",\"startDate\":\"" + start + "\",\"endDate\":\"" + end
                + "\",\"status\":\"" + status + "\",\"priorityAgendas\":" + priorityAgendas
                + ",\"reminderIds\":" + reminderIds + ",\"additionalAnnouncement\":\""
                + additionalAnnouncement + "\"}";
        mockMvc.perform(post("/api/proposal-announcements")
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
        return jdbcTemplate.queryForObject(
                "SELECT id FROM proposal_announcements WHERE name = ?", Long.class, name);
    }

    /** Publishes a call exactly as the RPS management page does and returns its id. */
    private Long createAnnouncement(String name, String start, String end, String status)
            throws Exception {
        mockMvc.perform(post("/api/proposal-announcements")
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementBody(name, start, end, status)))
                .andExpect(status().isCreated());
        return jdbcTemplate.queryForObject(
                "SELECT id FROM proposal_announcements WHERE name = ?", Long.class, name);
    }

    private List<Notification> notificationsOf(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // 1. Publishing a call records its publication together with the original deadline,
    //    and a call has no "previous" deadline to show yet.
    @Test
    void publishingACallRecordsTheOriginalDeadline() throws Exception {
        Long id = createAnnouncement("Published Call", daysFromToday(-1), daysFromToday(5), "ACTIVE");

        mockMvc.perform(get("/api/proposal-announcements/" + id + "/details")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updates.length()").value(1))
                .andExpect(jsonPath("$.updates[0].updateType").value("PUBLISHED"))
                .andExpect(jsonPath("$.updates[0].newEndDate").value(daysFromToday(5)))
                .andExpect(jsonPath("$.updates[0].previousEndDate").value(nullValue()))
                .andExpect(jsonPath("$.updates[0].effectiveDate").value(LocalDate.now().toString()));
    }

    // 2. Extending the deadline keeps the original deadline next to the new one instead of
    //    overwriting it; the update carries only the window and the status.
    @Test
    void extendingTheDeadlineKeepsTheOriginalDeadline() throws Exception {
        Long id = createAnnouncement("Extended Call", daysFromToday(-1), daysFromToday(5), "ACTIVE");

        mockMvc.perform(put("/api/proposal-announcements/" + id)
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(daysFromToday(-1), daysFromToday(20), "ACTIVE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/proposal-announcements/" + id + "/details")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk())
                // The current values follow the extension...
                .andExpect(jsonPath("$.endDate").value(daysFromToday(20)))
                // ...while the recorded history keeps both deadlines, oldest first.
                .andExpect(jsonPath("$.updates.length()").value(2))
                .andExpect(jsonPath("$.updates[0].updateType").value("PUBLISHED"))
                .andExpect(jsonPath("$.updates[0].newEndDate").value(daysFromToday(5)))
                .andExpect(jsonPath("$.updates[1].updateType").value("DEADLINE_EXTENDED"))
                .andExpect(jsonPath("$.updates[1].previousEndDate").value(daysFromToday(5)))
                .andExpect(jsonPath("$.updates[1].newEndDate").value(daysFromToday(20)))
                .andExpect(jsonPath("$.updates[1].actorName").value("Maria Santos"))
                .andExpect(jsonPath("$.updates[1].actorRole").value("RPS_ADMIN"));

        // The removed public explanation is not part of an update: nothing is recorded for it.
        List<ProposalAnnouncementUpdate> history = updateRepository
                .findByAnnouncementIdOrderByCreatedAtAscIdAsc(id);
        assertNull(history.get(1).getExplanation());
    }

    // 3. A second extension is appended, never overwritten: every step stays reviewable.
    @Test
    void aSecondExtensionKeepsEveryStep() throws Exception {
        Long id = createAnnouncement("Twice Extended Call", daysFromToday(-1), daysFromToday(5),
                "ACTIVE");

        mockMvc.perform(put("/api/proposal-announcements/" + id)
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementBody("Twice Extended Call", daysFromToday(-1),
                                daysFromToday(15), "ACTIVE")))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/proposal-announcements/" + id)
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementBody("Twice Extended Call", daysFromToday(-1),
                                daysFromToday(30), "ACTIVE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/proposal-announcements/" + id + "/details")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updates.length()").value(3))
                .andExpect(jsonPath("$.updates[0].newEndDate").value(daysFromToday(5)))
                .andExpect(jsonPath("$.updates[1].previousEndDate").value(daysFromToday(5)))
                .andExpect(jsonPath("$.updates[1].newEndDate").value(daysFromToday(15)))
                .andExpect(jsonPath("$.updates[2].previousEndDate").value(daysFromToday(15)))
                .andExpect(jsonPath("$.updates[2].newEndDate").value(daysFromToday(30)));
    }

    // 4. The proponent notification carries the call it is about and the call's CURRENT
    //    status: an extended call that is still open reads as OPEN, never as CLOSED,
    //    even though its superseded (original) deadline has already passed.
    @Test
    void theNotificationCarriesTheCallAndItsCurrentStatus() throws Exception {
        Long id = createAnnouncement("Status Call", daysFromToday(-10), daysFromToday(-5), "CLOSED");

        mockMvc.perform(put("/api/proposal-announcements/" + id)
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementBody("Status Call", daysFromToday(-10),
                                daysFromToday(6), "ACTIVE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].proposalAnnouncementId").value(hasItem(id.intValue())))
                .andExpect(jsonPath("$[*].proposalAnnouncementName").value(hasItem("Status Call")))
                .andExpect(jsonPath("$[*].proposalAnnouncementStatus").value(hasItem("OPEN")));
    }

    // 5. A published call whose window has not started yet is reported as SCHEDULED.
    @Test
    void aPublishedCallThatHasNotOpenedIsReportedAsScheduled() throws Exception {
        createAnnouncement("Scheduled Call", daysFromToday(7), daysFromToday(14), "ACTIVE");

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].proposalAnnouncementStatus").value(hasItem("SCHEDULED")));
    }

    // 6. A call that really is closed is reported as CLOSED, and the recorded status
    //    change stays part of the history.
    @Test
    void aClosedCallIsReportedAsClosed() throws Exception {
        Long id = createAnnouncement("Closing Call", daysFromToday(-1), daysFromToday(5), "ACTIVE");

        mockMvc.perform(put("/api/proposal-announcements/" + id)
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementBody("Closing Call", daysFromToday(-1),
                                daysFromToday(5), "CLOSED")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].proposalAnnouncementStatus").value(hasItem("CLOSED")));

        mockMvc.perform(get("/api/proposal-announcements/" + id + "/details")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.updates[*].updateType").value(hasItem("STATUS_CHANGED")));
    }

    // 7. A call published before the history existed keeps an empty history: nothing is
    //    fabricated for it, and the proponent view still opens it.
    @Test
    void aCallWithoutRecordedHistoryReportsAnEmptyHistory() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO proposal_announcements (name, start_date, end_date, status) VALUES (?, ?, ?, ?)",
                "Legacy Call", LocalDate.now().minusDays(1), LocalDate.now().plusDays(5), "ACTIVE");
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM proposal_announcements WHERE name = ?", Long.class, "Legacy Call");

        mockMvc.perform(get("/api/proposal-announcements/" + id + "/details")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updates.length()").value(0));
    }

    // 8. Every significant update notifies the proponent, and each notification points at
    //    the same call the recorded history belongs to.
    @Test
    void everySignificantUpdateNotifiesTheProponent() throws Exception {
        Long id = createAnnouncement("Notified Call", daysFromToday(-1), daysFromToday(5), "ACTIVE");

        mockMvc.perform(put("/api/proposal-announcements/" + id)
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementBody("Notified Call", daysFromToday(-1),
                                daysFromToday(12), "ACTIVE")))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/proposal-announcements/" + id)
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementBody("Notified Call", daysFromToday(-1),
                                daysFromToday(20), "ACTIVE")))
                .andExpect(status().isOk());

        List<Notification> notifications = notificationsOf(proponent);
        assertEquals(3, notifications.size(), "one notification per publication / extension");
        assertTrue(notifications.stream().allMatch(n -> id.equals(n.getProposalAnnouncementId())),
                "every notification opens the same call as the recorded history");
        assertEquals(2, notifications.stream()
                .filter(n -> "Call for Proposal Deadline Extended".equals(n.getTitle())).count());
    }

    // 9. Each extension notification names the deadline ITS OWN event replaced, so the
    //    second extension never repeats the first one's previous deadline and never skips
    //    the intermediate deadline. The stored events carry the same per-event values.
    @Test
    void eachExtensionNotificationNamesItsOwnPreviousDeadline() throws Exception {
        Long id = createAnnouncement("Chained Extension Call", daysFromToday(-1), daysFromToday(5),
                "ACTIVE");

        // First extension: today+5 -> today+12.
        mockMvc.perform(put("/api/proposal-announcements/" + id)
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementBody("Chained Extension Call", daysFromToday(-1),
                                daysFromToday(12), "ACTIVE")))
                .andExpect(status().isOk());
        // Second extension: today+12 -> today+20.
        mockMvc.perform(put("/api/proposal-announcements/" + id)
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementBody("Chained Extension Call", daysFromToday(-1),
                                daysFromToday(20), "ACTIVE")))
                .andExpect(status().isOk());

        List<String> messages = notificationsOf(proponent).stream()
                .filter(n -> "Call for Proposal Deadline Extended".equals(n.getTitle()))
                .map(Notification::getMessage)
                .toList();
        assertEquals(2, messages.size(), "one notification per extension");

        assertTrue(messages.stream().anyMatch(m -> m.contains("from "
                        + formattedDaysFromToday(5) + " to " + formattedDaysFromToday(12))),
                "the first extension names the deadline it replaced: " + messages);
        assertTrue(messages.stream().anyMatch(m -> m.contains("from "
                        + formattedDaysFromToday(12) + " to " + formattedDaysFromToday(20))),
                "the second extension names the deadline the first one set: " + messages);
        assertFalse(messages.stream().anyMatch(m -> m.contains("from "
                        + formattedDaysFromToday(5) + " to " + formattedDaysFromToday(20))),
                "no notification skips the intermediate deadline: " + messages);

        // The recorded extension events keep the very same previous/new deadlines.
        mockMvc.perform(get("/api/proposal-announcements/" + id + "/details")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updates[1].previousEndDate").value(daysFromToday(5)))
                .andExpect(jsonPath("$.updates[1].newEndDate").value(daysFromToday(12)))
                .andExpect(jsonPath("$.updates[2].previousEndDate").value(daysFromToday(12)))
                .andExpect(jsonPath("$.updates[2].newEndDate").value(daysFromToday(20)))
                // The current deadline is the extended one, so it must never be used as the
                // previous deadline of an extension event.
                .andExpect(jsonPath("$.endDate").value(daysFromToday(20)));
    }

    // 10. A client cannot change a restricted field through the update endpoint: the PUT
    //     body below carries a new name, priority agenda, reminders, additional text and
    //     the removed public explanation, and none of them is applied.
    @Test
    void anUpdateNeverChangesTheRestrictedFields() throws Exception {
        Long id = createFullAnnouncement("Restricted Call", daysFromToday(-1), daysFromToday(5),
                "ACTIVE", "[\"dagat\"]", "[\"research-plan\"]", "Original body.");

        String body = "{\"name\":\"Renamed Call\",\"startDate\":\"" + daysFromToday(-1)
                + "\",\"endDate\":\"" + daysFromToday(12) + "\",\"status\":\"ACTIVE\""
                + ",\"priorityAgendas\":[\"dagat\",\"punla\"]"
                + ",\"reminderIds\":[\"collaboration\"]"
                + ",\"additionalAnnouncement\":\"Replaced body.\""
                + ",\"updateExplanation\":\"Should never be stored.\"}";
        mockMvc.perform(put("/api/proposal-announcements/" + id)
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                // The permitted fields are applied...
                .andExpect(jsonPath("$.startDate").value(daysFromToday(-1)))
                .andExpect(jsonPath("$.endDate").value(daysFromToday(12)))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                // ...while every restricted field keeps the value it was created with.
                .andExpect(jsonPath("$.name").value("Restricted Call"))
                .andExpect(jsonPath("$.priorityAgendas.length()").value(1))
                .andExpect(jsonPath("$.priorityAgendas[0]").value("dagat"))
                .andExpect(jsonPath("$.reminderIds.length()").value(1))
                .andExpect(jsonPath("$.reminderIds[0]").value("research-plan"))
                .andExpect(jsonPath("$.additionalAnnouncement").value("Original body."));

        // The stored row agrees, and the removed explanation was never recorded.
        assertEquals("Restricted Call", jdbcTemplate.queryForObject(
                "SELECT name FROM proposal_announcements WHERE id = ?", String.class, id));
        List<ProposalAnnouncementUpdate> history = updateRepository
                .findByAnnouncementIdOrderByCreatedAtAscIdAsc(id);
        assertEquals(2, history.size(), "publication + the window change, never a duplicate");
        assertEquals("DEADLINE_EXTENDED", history.get(1).getUpdateType());
        assertNull(history.get(1).getExplanation());
    }
}
