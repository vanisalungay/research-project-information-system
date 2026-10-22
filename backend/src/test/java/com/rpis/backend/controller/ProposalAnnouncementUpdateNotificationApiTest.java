package com.rpis.backend.controller;

import com.rpis.backend.model.Notification;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.NotificationRepository;
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
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level integration test for the "Call for Proposals updated" notification: an
 * ORPS (RPS) Admin or Staff member edits an existing announcement through the real
 * {@code PUT /api/proposal-announcements/{id}} endpoint, and the eligible proponents can
 * then read the resulting notification through the very endpoint their notification page
 * already calls ({@code GET /api/notifications}).
 * <p>
 * The service-level test ({@code ProposalAnnouncementUpdateNotificationPostgresTest})
 * proves the trigger rules; this test proves the full request path - controller, security
 * filter chain, persisted announcement, persisted notification and the notification
 * listing API - including that ineligible accounts never see the notification and that a
 * {@code Scheduled} announcement is excluded from the new behaviour.
 * <p>
 * Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProposalAnnouncementUpdateNotificationApiTest {

    private static final String UPDATE_TITLE = "Call for Proposals Updated";
    private static final String DEADLINE_TITLE = "Submission Deadline Updated";
    private static final String CLOSED_TITLE = "Call for Proposals Closed";
    private static final String OPEN_TITLE = "New Proposal Announcement Open";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User proponent;
    private User secondProponent;
    private User pendingProponent;
    private User rpsAdmin;
    private String proponentToken;
    private String secondProponentToken;
    private String pendingProponentToken;
    private String rpsAdminToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE notifications RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposal_announcements RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        proponent = createUser("proponent@msunaawan.edu.ph", "PROPONENT", "APPROVED");
        secondProponent = createUser("proponent2@msunaawan.edu.ph", "PROPONENT", "APPROVED");
        pendingProponent = createUser("pending@msunaawan.edu.ph", "PROPONENT", "PENDING");
        rpsAdmin = createUser("rpsadmin@msunaawan.edu.ph", "RPS_ADMIN", "APPROVED");

        proponentToken = tokenService.generateToken(proponent.getId(), proponent.getRole());
        secondProponentToken =
                tokenService.generateToken(secondProponent.getId(), secondProponent.getRole());
        pendingProponentToken =
                tokenService.generateToken(pendingProponent.getId(), pendingProponent.getRole());
        rpsAdminToken = tokenService.generateToken(rpsAdmin.getId(), rpsAdmin.getRole());
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

    /** The exact payload shape the Proposal Announcements edit form sends. */
    private static String announcementBody(String name, String start, String end, String status) {
        return "{\"name\":\"" + name + "\",\"startDate\":\"" + start + "\",\"endDate\":\"" + end
                + "\",\"status\":\"" + status + "\",\"priorityAgendas\":[],\"reminderIds\":[],"
                + "\"additionalAnnouncement\":\"\"}";
    }

    /** Creates an announcement through the real endpoint and returns its database id. */
    private Long createAnnouncement(String token, String name, String start, String end,
            String status) throws Exception {
        mockMvc.perform(post("/api/proposal-announcements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementBody(name, start, end, status)))
                .andExpect(status().isCreated());
        return jdbcTemplate.queryForObject(
                "SELECT id FROM proposal_announcements WHERE name = ?", Long.class, name);
    }

    /** An ACTIVE announcement whose window contains today, i.e. shown as Open. */
    private Long createOpenAnnouncement(String name) throws Exception {
        return createAnnouncement(rpsAdminToken, name, daysFromToday(-1), daysFromToday(5), "ACTIVE");
    }

    /** An ACTIVE announcement starting in the future, i.e. shown as Scheduled. */
    private Long createScheduledAnnouncement(String name) throws Exception {
        return createAnnouncement(rpsAdminToken, name, daysFromToday(10), daysFromToday(20), "ACTIVE");
    }

    private void updateAnnouncement(String token, Long id, String name, String start, String end,
            String status) throws Exception {
        mockMvc.perform(put("/api/proposal-announcements/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementBody(name, start, end, status)))
                .andExpect(status().isOk());
    }

    private List<Notification> notificationsOf(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    private boolean hasNotification(User user, String title) {
        return notificationsOf(user).stream().anyMatch(n -> title.equals(n.getTitle()));
    }

    // 1. Editing an Open call: the proponent reads the update notification from the
    //    existing listing endpoint, and the announcement update itself still succeeds.
    @Test
    void editingAnOpenCallNotifiesTheProponentThroughTheExistingNotificationEndpoint()
            throws Exception {
        Long id = createOpenAnnouncement("Open Call");
        assertTrue(hasNotification(proponent, OPEN_TITLE),
                "creating an open call still sends the pre-existing notification");
        assertEquals(id, notificationsOf(proponent).stream()
                        .filter(n -> OPEN_TITLE.equals(n.getTitle()))
                        .findFirst()
                        .orElseThrow()
                        .getProposalAnnouncementId(),
                "the published-call notification points at its own announcement");

        mockMvc.perform(put("/api/proposal-announcements/" + id)
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementBody("Open Call", daysFromToday(-1),
                                daysFromToday(15), "ACTIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endDate").value(daysFromToday(15)))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title").value(hasItem(DEADLINE_TITLE)))
                .andExpect(jsonPath("$[?(@.title == '" + DEADLINE_TITLE + "')].type")
                        .value(hasItem("PROPOSAL_ANNOUNCEMENT")))
                .andExpect(jsonPath("$[?(@.title == '" + DEADLINE_TITLE + "')].isRead")
                        .value(hasItem(false)))
                .andExpect(jsonPath("$[?(@.title == '" + DEADLINE_TITLE + "')].createdAt")
                        .value(hasItem(notNullValue())))
                .andExpect(jsonPath("$[?(@.title == '" + DEADLINE_TITLE + "')].message")
                        .value(hasItem(containsString("Open Call"))))
                // The reference the Proponent notification list navigates with: the exact
                // announcement this notification was raised for.
                .andExpect(jsonPath("$[?(@.title == '" + DEADLINE_TITLE + "')].proposalAnnouncementId")
                        .value(hasItem(id.intValue())));
    }

    // 2. Closing an Open call keeps the specific "closed" notification, visible to the
    //    proponent through the same endpoint.
    @Test
    void closingAnOpenCallNotifiesTheProponentThatTheCallWasClosed() throws Exception {
        Long id = createOpenAnnouncement("Closing Call");

        updateAnnouncement(rpsAdminToken, id, "Closing Call", daysFromToday(-1), daysFromToday(5),
                "CLOSED");

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title").value(hasItem(CLOSED_TITLE)));
        assertFalse(hasNotification(proponent, UPDATE_TITLE));
    }

    // 3. A Scheduled announcement is excluded from the new behaviour, even when the same
    //    edit changes its deadline (and it becomes Open).
    @Test
    void editingAScheduledCallNotifiesNobody() throws Exception {
        Long id = createScheduledAnnouncement("Scheduled Call");

        updateAnnouncement(rpsAdminToken, id, "Scheduled Call", daysFromToday(-1),
                daysFromToday(15), "ACTIVE");

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title").value(not(hasItem(UPDATE_TITLE))))
                .andExpect(jsonPath("$[*].title").value(not(hasItem(DEADLINE_TITLE))))
                .andExpect(jsonPath("$[*].title").value(not(hasItem(CLOSED_TITLE))));
    }


    // 4. Recipient correctness at the API level: every approved proponent sees the update
    //    notification exactly once, and staff / pending accounts never see it.
    @Test
    void onlyEligibleProponentsSeeTheUpdateNotification() throws Exception {
        Long id = createOpenAnnouncement("Shared Call");

        updateAnnouncement(rpsAdminToken, id, "Shared Call", daysFromToday(-1), daysFromToday(20),
                "ACTIVE");

        assertEquals(1, notificationsOf(proponent).stream()
                .filter(n -> DEADLINE_TITLE.equals(n.getTitle())).count());
        assertEquals(1, notificationsOf(secondProponent).stream()
                .filter(n -> DEADLINE_TITLE.equals(n.getTitle())).count());

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + rpsAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title").value(not(hasItem(DEADLINE_TITLE))));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + pendingProponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // 5. The update notification keeps the existing unread/read behaviour of the
    //    notification API.
    @Test
    void theProponentCanMarkTheUpdateNotificationAsRead() throws Exception {
        Long id = createOpenAnnouncement("Readable Call");

        updateAnnouncement(rpsAdminToken, id, "Readable Call", daysFromToday(-1),
                daysFromToday(25), "ACTIVE");

        Notification update = notificationsOf(proponent).stream()
                .filter(n -> DEADLINE_TITLE.equals(n.getTitle()))
                .findFirst()
                .orElseThrow();
        assertFalse(update.getIsRead());

        mockMvc.perform(put("/api/notifications/" + update.getId() + "/read")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + update.getId() + ")].isRead")
                        .value(hasItem(true)));
    }

    // 6. The notification listing stays protected: it is never readable anonymously.
    @Test
    void theNotificationListingRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
    }

}
