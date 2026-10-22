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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level test for {@code GET /api/proposal-announcements/{id}/details} - the
 * read-only Call for Proposals view a Proponent notification opens.
 * <p>
 * Proves that a proponent can read the announcement that was actually saved by
 * RPS/OVCRIGE (all user-facing fields), that draft announcements stay hidden, that
 * non-proponent and anonymous callers are rejected, and that merely opening an
 * announcement has no side effects (no notification is created).
 * <p>
 * Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProposalAnnouncementDetailsApiTest {

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
    private User rpsAdmin;
    private String proponentToken;
    private String rpsAdminToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE notifications RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposal_announcements RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        proponent = createUser("proponent@msunaawan.edu.ph", "PROPONENT", "APPROVED");
        rpsAdmin = createUser("rpsadmin@msunaawan.edu.ph", "RPS_ADMIN", "APPROVED");

        proponentToken = tokenService.generateToken(proponent.getId(), proponent.getRole());
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

    private static String announcementBody(String name, String start, String end, String status,
            String priorityAgendas, String reminderIds, String additionalAnnouncement) {
        return "{\"name\":\"" + name + "\",\"startDate\":\"" + start + "\",\"endDate\":\"" + end
                + "\",\"status\":\"" + status + "\",\"priorityAgendas\":" + priorityAgendas
                + ",\"reminderIds\":" + reminderIds
                + ",\"additionalAnnouncement\":\"" + additionalAnnouncement + "\"}";
    }

    /** Creates the announcement exactly as the RPS/OVCRIGE management page does. */
    private Long createAnnouncement(String name, String start, String end, String status,
            String priorityAgendas, String reminderIds, String additionalAnnouncement)
            throws Exception {
        mockMvc.perform(post("/api/proposal-announcements")
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementBody(name, start, end, status, priorityAgendas,
                                reminderIds, additionalAnnouncement)))
                .andExpect(status().isCreated());
        return jdbcTemplate.queryForObject(
                "SELECT id FROM proposal_announcements WHERE name = ?", Long.class, name);
    }

    private List<Notification> notificationsOf(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // 1. A proponent opens the announcement a notification refers to and sees exactly the
    //    details saved by RPS/OVCRIGE.
    @Test
    void aProponentReadsTheSavedAnnouncementDetails() throws Exception {
        Long id = createAnnouncement("Details Call", daysFromToday(-1), daysFromToday(5), "ACTIVE",
                "[\"punla\",\"dagat\"]", "[\"collaboration\",\"research-plan\"]",
                "Bring your best proposal.");

        mockMvc.perform(get("/api/proposal-announcements/" + id + "/details")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.name").value("Details Call"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startDate").value(daysFromToday(-1)))
                .andExpect(jsonPath("$.endDate").value(daysFromToday(5)))
                // Saved in canonical order, exactly as the management page stores it.
                .andExpect(jsonPath("$.priorityAgendas[0]").value("dagat"))
                .andExpect(jsonPath("$.priorityAgendas[1]").value("punla"))
                .andExpect(jsonPath("$.reminderIds[0]").value("research-plan"))
                .andExpect(jsonPath("$.reminderIds[1]").value("collaboration"))
                .andExpect(jsonPath("$.additionalAnnouncement").value("Bring your best proposal."));
    }

    // 2. A closed call stays readable, so an older "call has been closed" notification
    //    still opens its announcement.
    @Test
    void aClosedAnnouncementIsStillReadableByAProponent() throws Exception {
        Long id = createAnnouncement("Closed Call", daysFromToday(-20), daysFromToday(-10),
                "CLOSED", "[]", "[]", "");

        mockMvc.perform(get("/api/proposal-announcements/" + id + "/details")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Closed Call"))
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    // 3. Draft announcements are not published yet and must not be exposed.
    @Test
    void aDraftAnnouncementIsReportedAsNotFound() throws Exception {
        Long id = createAnnouncement("Draft Call", daysFromToday(-1), daysFromToday(5), "DRAFT",
                "[]", "[]", "");

        mockMvc.perform(get("/api/proposal-announcements/" + id + "/details")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isNotFound());
    }

    // 4. A missing announcement is a not-found, never fabricated details.
    @Test
    void anUnknownAnnouncementIsReportedAsNotFound() throws Exception {
        mockMvc.perform(get("/api/proposal-announcements/999999/details")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isNotFound());
    }

    // 5. The Proponent view is proponents only: staff keep their own management page and
    //    anonymous callers are rejected.
    @Test
    void staffAndAnonymousCallersCannotUseTheProponentDetailsEndpoint() throws Exception {
        Long id = createAnnouncement("Restricted Call", daysFromToday(-1), daysFromToday(5),
                "ACTIVE", "[]", "[]", "");

        mockMvc.perform(get("/api/proposal-announcements/" + id + "/details")
                        .header("Authorization", "Bearer " + rpsAdminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/proposal-announcements/" + id + "/details"))
                .andExpect(status().isForbidden());
    }

    // 6. Opening an announcement is read-only: it must not create notifications or change
    //    the announcement itself.
    @Test
    void openingTheAnnouncementHasNoSideEffects() throws Exception {
        Long id = createAnnouncement("Quiet Call", daysFromToday(-1), daysFromToday(5), "ACTIVE",
                "[]", "[]", "");
        int notificationsBefore = notificationsOf(proponent).size();

        mockMvc.perform(get("/api/proposal-announcements/" + id + "/details")
                        .header("Authorization", "Bearer " + proponentToken))
                .andExpect(status().isOk());

        assertEquals(notificationsBefore, notificationsOf(proponent).size(),
                "opening the announcement must not create notifications");
        assertEquals("ACTIVE", jdbcTemplate.queryForObject(
                "SELECT status FROM proposal_announcements WHERE id = ?", String.class, id));
        assertEquals(LocalDate.parse(daysFromToday(5)), jdbcTemplate.queryForObject(
                "SELECT end_date FROM proposal_announcements WHERE id = ?", LocalDate.class, id));
    }

}
