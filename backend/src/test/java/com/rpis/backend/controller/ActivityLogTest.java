package com.rpis.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpis.backend.model.ActivityLog;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.ActivityLogRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.PasswordService;
import com.rpis.backend.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Activity Log of an RPS administrator.
 *
 * <p>Pinned down here: an entry is written by the backend when the operation really
 * happens, it names the administrator who performed it (never the affected account),
 * one administrator cannot read another's history, no other role reaches the API at
 * all, and the page's search, filters, pagination and details work.
 *
 * <p>Runs against the isolated {@code rpis_test} database. An entry is written in its
 * own transaction, so the table is cleared at the start of every test instead of
 * relying on the test transaction's rollback.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ActivityLogTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final String PASSWORD = "activity-log-pass-1A!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ActivityLogRepository activityLogRepository;
    @Autowired
    private PasswordService passwordService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DataSource dataSource;
    /**
     * The controller removes the password from the account it returns, which leaves a
     * dirty managed entity in this test's transaction. Detaching it before the next
     * query keeps that removal out of the database, exactly as the real request's
     * read-only repository transaction does.
     */
    @Autowired
    private EntityManager entityManager;

    private Long adminId;
    private String adminToken;
    private String otherAdminToken;
    private String staffToken;
    private String proponentToken;

    @BeforeEach
    void setUp() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().contains("rpis_test"),
                    "the activity log tests must run against the isolated rpis_test database");
        }

        // Recorded rows are committed by their own transaction, so clear what earlier
        // tests left behind and assert only on this test's own activity.
        jdbcTemplate.execute("DELETE FROM activity_logs");

        User admin = account(UserRoles.RPS_ADMIN, "Activity Admin");
        adminId = admin.getId();
        adminToken = tokenService.generateToken(adminId, UserRoles.RPS_ADMIN);
        otherAdminToken = tokenFor(UserRoles.RPS_ADMIN, "Other Admin");
        staffToken = tokenFor(UserRoles.RPS_STAFF, "Activity Staff");
        proponentToken = tokenFor(UserRoles.PROPONENT, "Activity Proponent");
    }

    @Test
    void creatingAnAccountRecordsAnActivityForTheAdministrator() throws Exception {
        mockMvc.perform(withToken(post("/api/users/admin/create"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Created By Admin", "role", UserRoles.RPS_STAFF))))
                .andExpect(status().isCreated());

        entityManager.clear();
        entityManager.clear();
        mockMvc.perform(withToken(get("/api/activity-logs"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].action")
                        .value(ActivityLog.ACTION_ACCOUNT_CREATED))
                .andExpect(jsonPath("$.content[0].module").value(ActivityLog.MODULE_USER_ACCOUNTS))
                .andExpect(jsonPath("$.content[0].description")
                        .value("Created a new user account"))
                .andExpect(jsonPath("$.content[0].status").value(ActivityLog.STATUS_SUCCESS))
                // The actor is recorded; the account that was created is only described.
                .andExpect(jsonPath("$.content[0].userName").value("Activity Admin"))
                .andExpect(jsonPath("$.content[0].userId").value(adminId.intValue()))
                .andExpect(jsonPath("$.content[0].details").exists())
                .andExpect(jsonPath("$.content[0].createdAt").exists());
    }

    @Test
    void updatingAnAccountRecordsAnActivity() throws Exception {
        User target = account(UserRoles.PROPONENT, "Updated Target");

        mockMvc.perform(withToken(put("/api/users/" + target.getId()), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Updated Target Renamed"))))
                .andExpect(status().isOk());

        entityManager.clear();
        entityManager.clear();
        mockMvc.perform(withToken(get("/api/activity-logs"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].action")
                        .value(ActivityLog.ACTION_ACCOUNT_UPDATED))
                .andExpect(jsonPath("$.content[0].description")
                        .value("Updated an existing user account"));
    }

    @Test
    void changingACallForProposalRecordsAnActivity() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Activity Log Test Call " + SEQUENCE.incrementAndGet());
        body.put("startDate", LocalDate.now().toString());
        body.put("endDate", LocalDate.now().plusDays(30).toString());
        body.put("status", "ACTIVE");

        mockMvc.perform(withToken(post("/api/proposal-announcements"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        entityManager.clear();
        entityManager.clear();
        mockMvc.perform(withToken(get("/api/activity-logs"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].action").value(ActivityLog.ACTION_CALL_CREATED))
                .andExpect(jsonPath("$.content[0].module")
                        .value(ActivityLog.MODULE_CALLS_FOR_PROPOSAL))
                .andExpect(jsonPath("$.content[0].description")
                        .value("Created a call for proposal"));
    }

    @Test
    void anEntryNamesTheActorAndNotTheAffectedAccount() throws Exception {
        mockMvc.perform(withToken(post("/api/users/admin/create"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "By Admin", "role", UserRoles.RPS_STAFF))))
                .andExpect(status().isCreated());

        // The other administrator did nothing, so their own history is empty: an entry
        // belongs to the actor, and the actor comes from the bearer token alone.
        entityManager.clear();
        entityManager.clear();
        mockMvc.perform(withToken(get("/api/activity-logs"), otherAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void anAdministratorCannotReadAnotherAdministratorsEntry() throws Exception {
        mockMvc.perform(withToken(post("/api/users/admin/create"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Owned By Admin", "role", UserRoles.RPS_STAFF))))
                .andExpect(status().isCreated());

        Long entryId = jdbcTemplate.queryForObject(
                "SELECT id FROM activity_logs WHERE user_id = ? ORDER BY id DESC LIMIT 1",
                Long.class, adminId);

        // The owner reaches it...
        entityManager.clear();
        entityManager.clear();
        mockMvc.perform(withToken(get("/api/activity-logs/" + entryId), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(entryId.intValue()));

        // ...and the other administrator is refused, without the endpoint even
        // confirming that the entry exists.
        entityManager.clear();
        mockMvc.perform(withToken(get("/api/activity-logs/" + entryId), otherAdminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void everyRoleOtherThanTheAdministratorIsRefused() throws Exception {
        for (String token : new String[] { staffToken, proponentToken }) {
            entityManager.clear();
        mockMvc.perform(withToken(get("/api/activity-logs"), token))
                    .andExpect(status().isForbidden());
            entityManager.clear();
        mockMvc.perform(withToken(get("/api/activity-logs/modules"), token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void searchFiltersPaginationAndDetailsWork() throws Exception {
        // Seeded through the repository: this test is about the page search, filters,
        // pagination and details. That a real action records an entry is covered by the
        // tests above, which go through the endpoint.
        for (int i = 0; i < 3; i++) {
            ActivityLog entry = new ActivityLog();
            entry.setUserId(adminId);
            entry.setUserName("Activity Admin");
            entry.setAction(ActivityLog.ACTION_ACCOUNT_CREATED);
            entry.setModule(ActivityLog.MODULE_USER_ACCOUNTS);
            entry.setDescription("Created a new user account");
            entry.setDetails("Paged Account " + i + " was created with the role RPS_STAFF.");
            entry.setStatus(ActivityLog.STATUS_SUCCESS);
            activityLogRepository.save(entry);
        }
        entityManager.flush();
        // Search by description.
        entityManager.clear();
        entityManager.clear();
        mockMvc.perform(withToken(get("/api/activity-logs").param("search", "created a new"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        // The module list offers the modules really used, and the module filter matches it.
        entityManager.clear();
        mockMvc.perform(withToken(get("/api/activity-logs/modules"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(ActivityLog.MODULE_USER_ACCOUNTS));
        entityManager.clear();
        mockMvc.perform(withToken(get("/api/activity-logs").param("module", "Calls for Proposal"),
                        adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // Status filter: everything recorded so far succeeded.
        entityManager.clear();
        mockMvc.perform(withToken(get("/api/activity-logs").param("status", "FAILED"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        entityManager.clear();
        entityManager.clear();
        mockMvc.perform(withToken(get("/api/activity-logs").param("status", "SUCCESS"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        // Pagination, newest first.
        MvcResult firstPage = mockMvc.perform(
                        withToken(get("/api/activity-logs").param("size", "2").param("page", "0"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andReturn();

        MvcResult secondPage = mockMvc.perform(
                        withToken(get("/api/activity-logs").param("size", "2").param("page", "1"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andReturn();

        assertTrue(readId(firstPage, 0) > readId(secondPage, 0),
                "the activity log must be ordered newest first");

        // Date range: today is inside it, yesterday is not.
        entityManager.clear();
        entityManager.clear();
        entityManager.clear();
        mockMvc.perform(withToken(get("/api/activity-logs").param("from", LocalDate.now().toString()), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
        mockMvc.perform(withToken(
                        get("/api/activity-logs").param("to", LocalDate.now().minusDays(1).toString()), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void notificationsStillWorkForTheOtherRoles() throws Exception {
        // Replacing the administrator's Notifications item changed nothing else: the other
        // roles still read their own notifications.
        entityManager.clear();
        mockMvc.perform(withToken(get("/api/notifications"), staffToken))
                .andExpect(status().isOk());
        entityManager.clear();
        mockMvc.perform(withToken(get("/api/notifications"), proponentToken))
                .andExpect(status().isOk());
    }

    private long readId(MvcResult result, int index) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("content").get(index).path("id").asLong();
    }

    private User account(String role, String name) {
        User user = new User();
        user.setName(name);
        user.setEmail("activity." + SEQUENCE.incrementAndGet() + "." + role.toLowerCase()
                + "@msunaawan.edu.ph");
        user.setPassword(passwordService.hash(PASSWORD));
        user.setRole(role);
        user.setStatus("APPROVED");
        return userRepository.saveAndFlush(user);
    }

    private String tokenFor(String role, String name) {
        return tokenService.generateToken(account(role, name).getId(), role);
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder builder,
            String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
