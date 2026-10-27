package com.rpis.backend.controller;

import com.rpis.backend.model.Notification;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.ProposalAnnouncementUpdate;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.NotificationRepository;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level tests for the restricted update of an existing Call for Proposal.
 * <p>
 * Creating a call still requires an announcement name, while updating an existing call
 * only accepts the submission window (start and end date) and the status: the name, the
 * RIIDE priority agenda, the announcement reminders and the additional announcement keep
 * their stored values and are never asked for again. A field the update does not carry
 * keeps the value already stored, so ORPS can save a change to just one of the permitted
 * fields.
 * <p>
 * Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProposalAnnouncementUpdateRestrictionApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProposalAnnouncementRepository announcementRepository;
    @Autowired
    private ProposalAnnouncementUpdateRepository updateRepository;
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
        jdbcTemplate.execute("TRUNCATE TABLE proposal_announcement_updates RESTART IDENTITY CASCADE");
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

    /** Creates a call through the real endpoint with a complete creation payload. */
    private Long createCall(String name, String start, String end, String status) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"startDate\":\"" + start + "\",\"endDate\":\""
                + end + "\",\"status\":\"" + status + "\""
                + ",\"priorityAgendas\":[\"dagat\"]"
                + ",\"reminderIds\":[\"research-plan\"]"
                + ",\"additionalAnnouncement\":\"Original body.\"}";
        mockMvc.perform(post("/api/proposal-announcements")
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
        return jdbcTemplate.queryForObject(
                "SELECT id FROM proposal_announcements WHERE name = ?", Long.class, name);
    }

    private void updateCall(Long id, String body) throws Exception {
        mockMvc.perform(put("/api/proposal-announcements/" + id)
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private ProposalAnnouncement stored(Long id) {
        return announcementRepository.findById(id).orElseThrow();
    }

    // 1. Creating a new call still requires an announcement name.
    @Test
    void creatingACallWithoutANameIsStillRejected() throws Exception {
        String body = "{\"startDate\":\"" + daysFromToday(-1) + "\",\"endDate\":\""
                + daysFromToday(5) + "\",\"status\":\"ACTIVE\"}";

        mockMvc.perform(post("/api/proposal-announcements")
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Announcement name is required."));

        assertEquals(0, announcementRepository.count(), "nothing was created");
    }

    // 2. A blank name is rejected on creation as well.
    @Test
    void creatingACallWithABlankNameIsStillRejected() throws Exception {
        String body = "{\"name\":\"   \",\"startDate\":\"" + daysFromToday(-1)
                + "\",\"endDate\":\"" + daysFromToday(5) + "\",\"status\":\"ACTIVE\"}";

        mockMvc.perform(post("/api/proposal-announcements")
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Announcement name is required."));
    }

    // 3. Only the start date may be sent: the stored deadline is kept and no name is asked.
    @Test
    void updatingOnlyTheStartDateSucceeds() throws Exception {
        Long id = createCall("Opening Call", daysFromToday(-1), daysFromToday(10), "ACTIVE");

        updateCall(id, "{\"startDate\":\"" + daysFromToday(-3) + "\"}");

        ProposalAnnouncement saved = stored(id);
        assertEquals(LocalDate.parse(daysFromToday(-3)), saved.getStartDate());
        assertEquals(LocalDate.parse(daysFromToday(10)), saved.getEndDate(),
                "the deadline the update did not carry is kept");
        assertEquals("Opening Call", saved.getName());
    }

    // 4. Only the end date may be sent: the stored opening date is kept.
    @Test
    void updatingOnlyTheEndDateSucceeds() throws Exception {
        Long id = createCall("Deadline Call", daysFromToday(-1), daysFromToday(10), "ACTIVE");

        updateCall(id, "{\"endDate\":\"" + daysFromToday(25) + "\"}");

        ProposalAnnouncement saved = stored(id);
        assertEquals(LocalDate.parse(daysFromToday(-1)), saved.getStartDate(),
                "the opening date the update did not carry is kept");
        assertEquals(LocalDate.parse(daysFromToday(25)), saved.getEndDate());
        assertEquals("Deadline Call", saved.getName());
    }

    // 5. Only the status may be sent: the window is kept and the stored status changes.
    @Test
    void updatingOnlyTheStatusSucceeds() throws Exception {
        Long id = createCall("Status Call", daysFromToday(-1), daysFromToday(10), "ACTIVE");

        updateCall(id, "{\"status\":\"CLOSED\"}");

        ProposalAnnouncement saved = stored(id);
        assertEquals("CLOSED", saved.getStatus());
        assertEquals(LocalDate.parse(daysFromToday(-1)), saved.getStartDate());
        assertEquals(LocalDate.parse(daysFromToday(10)), saved.getEndDate());
    }


    // 6. A restricted field can never be replaced or blanked through the update endpoint,
    //    and the announcement name is never required again.
    @Test
    void anUpdateCannotBlankOrReplaceTheNameOrTheRestrictedFields() throws Exception {
        Long id = createCall("Restricted Call", daysFromToday(-1), daysFromToday(5), "ACTIVE");

        String body = "{\"name\":\"\",\"startDate\":\"" + daysFromToday(-1)
                + "\",\"endDate\":\"" + daysFromToday(15) + "\",\"status\":\"ACTIVE\""
                + ",\"priorityAgendas\":[]"
                + ",\"reminderIds\":[]"
                + ",\"additionalAnnouncement\":\"\""
                + ",\"updateExplanation\":\"Should never be stored.\"}";
        updateCall(id, body);

        ProposalAnnouncement saved = stored(id);
        assertEquals("Restricted Call", saved.getName(), "the stored name is kept");
        assertEquals(List.of("dagat"), saved.getPriorityAgendas());
        assertEquals(List.of("research-plan"), saved.getReminderIds());
        assertEquals("Original body.", saved.getAdditionalAnnouncement());
        assertEquals(LocalDate.parse(daysFromToday(15)), saved.getEndDate(),
                "the permitted field did change");

        // A different name in the payload is ignored as well.
        updateCall(id, "{\"name\":\"Renamed Call\",\"endDate\":\"" + daysFromToday(18) + "\"}");
        assertEquals("Restricted Call", stored(id).getName());

        // The removed public explanation is never recorded.
        List<ProposalAnnouncementUpdate> history = updateRepository
                .findByAnnouncementIdOrderByCreatedAtAscIdAsc(id);
        assertTrue(history.stream().allMatch(update -> update.getExplanation() == null));
    }

    // 7. The window is still validated: a deadline before the opening date is rejected and
    //    nothing is stored.
    @Test
    void anUpdateRejectsAnInvalidWindow() throws Exception {
        Long id = createCall("Invalid Window Call", daysFromToday(-1), daysFromToday(10), "ACTIVE");

        mockMvc.perform(put("/api/proposal-announcements/" + id)
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endDate\":\"" + daysFromToday(-5) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("End date cannot be earlier than the start date."));

        assertEquals(LocalDate.parse(daysFromToday(10)), stored(id).getEndDate());
    }

    // 8. Unknown status values are still rejected.
    @Test
    void anUpdateRejectsAnUnknownStatus() throws Exception {
        Long id = createCall("Bad Status Call", daysFromToday(-1), daysFromToday(10), "ACTIVE");

        mockMvc.perform(put("/api/proposal-announcements/" + id)
                        .header("Authorization", "Bearer " + rpsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isBadRequest());

        assertEquals("ACTIVE", stored(id).getStatus());
    }

    // 9. A deadline extension still records the previous deadline and notifies proponents.
    @Test
    void extendingTheDeadlineStillRecordsThePreviousDeadline() throws Exception {
        Long id = createCall("Extended Call", daysFromToday(-1), daysFromToday(5), "ACTIVE");
        updateCall(id, "{\"endDate\":\"" + daysFromToday(20) + "\"}");

        List<ProposalAnnouncementUpdate> history = updateRepository
                .findByAnnouncementIdOrderByCreatedAtAscIdAsc(id);
        assertEquals(2, history.size(), "publication + one extension");
        assertEquals(ProposalAnnouncementUpdate.TYPE_DEADLINE_EXTENDED,
                history.get(1).getUpdateType());
        assertEquals(LocalDate.parse(daysFromToday(5)), history.get(1).getPreviousEndDate());
        assertEquals(LocalDate.parse(daysFromToday(20)), history.get(1).getNewEndDate());

        List<Notification> notifications =
                notificationRepository.findByUserOrderByCreatedAtDesc(proponent);
        assertTrue(notifications.stream()
                        .anyMatch(n -> "Call for Proposal Deadline Extended".equals(n.getTitle())),
                "the extension notification is still sent");
    }
}
