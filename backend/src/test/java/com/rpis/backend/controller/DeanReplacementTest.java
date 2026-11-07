package com.rpis.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpis.backend.model.DeanAssignment;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.DeanAssignmentRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.PasswordService;
import com.rpis.backend.service.TokenService;
import jakarta.persistence.EntityManager;
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
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dean succession (Manage Dean Role), exercised through the real HTTP layer.
 *
 * <p>Pins the behaviour the replacement feature exists for: replacing the sitting Dean of a
 * college closes the outgoing appointment as {@code REVOKED} (keeping the college, the
 * designation and the dates) and opens a new {@code ACTIVE} appointment for the incoming
 * Dean, in one transaction - the previous Dean's account and its history are never deleted,
 * and a college can never hold two active Deans or none.
 *
 * <p>Runs against the isolated {@code rpis_test} database only; every test is transactional.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DeanReplacementTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    /** A college the RPIS knows (matches the Create Proposal college list). */
    private static final String COLLEGE = "College of Business and Information Technology (CBIT)";

    private static final String PASSWORD = "doctorate-pass-2026";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DeanAssignmentRepository deanAssignmentRepository;
    @Autowired
    private PasswordService passwordService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private EntityManager entityManager;

    private Long adminId;
    private String adminToken;
    private String proponentToken;

    @BeforeEach
    void setUp() {
        requireTestDatabase();
        jdbcTemplate.execute("TRUNCATE TABLE dean_assignments RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE endorsement_request_events RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        adminId = account(UserRoles.RPS_ADMIN).getId();
        adminToken = tokenService.generateToken(adminId, UserRoles.RPS_ADMIN);
        proponentToken = tokenFor(UserRoles.PROPONENT);
    }

    /** Safety guard: this class must never run against the real database. */
    private void requireTestDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertTrue(url.contains("rpis_test"),
                    "Dean replacement tests must run against the isolated rpis_test database, "
                            + "but connected to: " + url);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private User account(String role) {
        return account(role, "APPROVED", null);
    }

    private User account(String role, String status, String college) {
        User user = new User();
        user.setName(role + " Account " + SEQUENCE.incrementAndGet());
        user.setEmail(role.toLowerCase() + ".replacement" + SEQUENCE.get() + "@msunaawan.edu.ph");
        user.setPassword(passwordService.hash(PASSWORD));
        user.setRole(role);
        user.setStatus(status);
        user.setAssignedCollege(college);
        return userRepository.saveAndFlush(user);
    }

    /** An eligible replacement: an approved account that holds the DEAN role with no assignment. */
    private User deanCandidate() {
        return account(UserRoles.DEAN);
    }

    private String tokenFor(String role) {
        return tokenService.generateToken(account(role).getId(), role);
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder builder,
            String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    /** The specification's scenario: Dean A -> Dean B, A kept as REVOKED history. */
    @Test
    void replacingADeanKeepsThePreviousAppointmentAsHistory() throws Exception {
        User deanA = createDean(COLLEGE);
        User deanB = deanCandidate();

        mockMvc.perform(withToken(post("/api/users/deans/replace"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", deanB.getId(),
                                "assignedCollege", COLLEGE,
                                "designation", "Dean"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(DeanAssignment.STATUS_ACTIVE))
                .andExpect(jsonPath("$.assignedCollege").value(COLLEGE));
        // Flush the pending role/appointment writes before detaching, so the assertions read
        // what the service persisted rather than the state before the call.
        entityManager.flush();
        entityManager.clear();

        // The replacement is now the sitting Dean.
        User reloadedB = userRepository.findById(deanB.getId()).orElseThrow();
        assertEquals(UserRoles.DEAN, reloadedB.getRole(), "the replacement must hold the Dean role");
        assertEquals(COLLEGE, reloadedB.getAssignedCollege());
        assertEquals("Dean", reloadedB.getDesignation());

        // The previous Dean keeps the account but loses the Dean scope; nothing is deleted.
        User reloadedA = userRepository.findById(deanA.getId()).orElseThrow();
        assertEquals(UserRoles.PROPONENT, reloadedA.getRole(),
                "the outgoing Dean becomes a Faculty Proponent again");
        assertNotNull(reloadedA.getEmail(), "the previous Dean's account is never deleted");

        // Exactly one ACTIVE appointment (Dean B) and one REVOKED appointment (Dean A).
        List<DeanAssignment> active = activeFor(COLLEGE);
        assertEquals(1, active.size(), "a college must hold exactly one active Dean");
        assertEquals(deanB.getId(), active.get(0).getUserId());

        List<DeanAssignment> revoked = revokedFor(COLLEGE);
        assertEquals(1, revoked.size(), "the previous appointment is kept as history");
        DeanAssignment former = revoked.get(0);
        assertEquals(deanA.getId(), former.getUserId());
        assertEquals(COLLEGE, former.getAssignedCollege(), "the previous college is preserved");
        assertEquals("Dean", former.getDesignation(), "the previous designation is preserved");
        assertNotNull(former.getDateRevoked(), "the revocation is dated");
        assertEquals(adminId, former.getRevokedBy(),
                "the administrator who replaced the Dean is recorded");

        // The succession is in the audit trail.
        Integer replaced = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM endorsement_request_events WHERE event_type = ?",
                Integer.class, "DEAN_ROLE_REPLACED");
        assertEquals(1, replaced, "the replacement is recorded in the audit trail");
    }

    @Test
    void aCollegeCanNeverHoldTwoActiveDeans() throws Exception {
        createDean(COLLEGE); // the sitting Dean
        User other = account(UserRoles.PROPONENT);

        // Assigning a different account to the same college through the plain save route is
        // refused: a college keeps one Dean, and replacement is the way to change them.
        mockMvc.perform(withToken(post("/api/users/deans"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", other.getId(),
                                "assignedCollege", COLLEGE))))
                .andExpect(status().isBadRequest());
        entityManager.clear();

        assertEquals(1, activeFor(COLLEGE).size(),
                "the refused request must not create a second active Dean");
        assertFalse(UserRoles.DEAN.equalsIgnoreCase(
                        userRepository.findById(other.getId()).orElseThrow().getRole()),
                "the refused promotion must be rolled back");
    }

    @Test
    void aFailedReplacementLeavesNoPartialUpdate() throws Exception {
        User deanA = createDean(COLLEGE);
        User pending = account(UserRoles.PROPONENT, "PENDING", null);

        // The candidate is not an approved account, so the replacement is refused...
        mockMvc.perform(withToken(post("/api/users/deans/replace"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", pending.getId(),
                                "assignedCollege", COLLEGE,
                                "designation", "Dean"))))
                .andExpect(status().isBadRequest());
        entityManager.clear();

        // ...and nothing changed: Dean A is still the active Dean of the college.
        assertEquals(UserRoles.DEAN,
                userRepository.findById(deanA.getId()).orElseThrow().getRole());
        List<DeanAssignment> active = activeFor(COLLEGE);
        assertEquals(1, active.size());
        assertEquals(deanA.getId(), active.get(0).getUserId());
        assertTrue(revokedFor(COLLEGE).isEmpty(), "no appointment was partially revoked");
    }

    /** Creates a sitting Dean for a college through the standard account-creation route. */
    @Test
    void onlyAnAdministratorMayReplaceADean() throws Exception {
        User deanA = createDean(COLLEGE);
        User candidate = account(UserRoles.PROPONENT);

        mockMvc.perform(withToken(post("/api/users/deans/replace"), proponentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", candidate.getId(),
                                "assignedCollege", COLLEGE))))
                .andExpect(status().isForbidden());
        entityManager.clear();

        assertEquals(UserRoles.DEAN,
                userRepository.findById(deanA.getId()).orElseThrow().getRole(),
                "a refused caller must not be able to replace the Dean");
    }

    @Test
    void theDeanRoleManagementListShowsCurrentAndFormerAssignments() throws Exception {
        User deanA = createDean(COLLEGE);
        User deanB = deanCandidate();
        mockMvc.perform(withToken(post("/api/users/deans/replace"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", deanB.getId(),
                                "assignedCollege", COLLEGE,
                                "designation", "Dean"))))
                .andExpect(status().isOk());

        // Default ("ACTIVE") view: only the sitting Dean.
        String activeBody = mockMvc.perform(withToken(get("/api/users/deans/assignments"), adminToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(activeBody.contains("\"status\":\"ACTIVE\""), "the current Dean is listed");
        assertFalse(activeBody.contains("\"status\":\"REVOKED\""), "the default view hides history");

        // "ALL" view: current and former, the former still visible with its dates.
        String allBody = mockMvc
                .perform(withToken(get("/api/users/deans/assignments?status=ALL"), adminToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(allBody.contains("\"status\":\"ACTIVE\""), "the current appointment is listed");
        assertTrue(allBody.contains("\"status\":\"REVOKED\""), "the former appointment is listed");
        assertTrue(allBody.contains("\"dateRevoked\":\""),
                "the former appointment carries a revocation date");
        assertTrue(allBody.contains(deanA.getEmail()), "the former Dean is still visible by email");

        // "REVOKED" view: the history only.
        String revokedBody = mockMvc
                .perform(withToken(get("/api/users/deans/assignments?status=REVOKED"), adminToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(revokedBody.contains("\"status\":\"REVOKED\""));
        assertFalse(revokedBody.contains("\"status\":\"ACTIVE\""));
    }

    @Test
    void onlyUnassignedDeanAccountsAreEligibleReplacementCandidates() throws Exception {
        User sitting = createDean(COLLEGE); // the outgoing DEAN (assigned COLLEGE)
        User assignedElsewhere = createDean("College of Education and Social Sciences (CESS)");
        User available = account(UserRoles.DEAN); // a DEAN with no appointment -> eligible
        User proponent = account(UserRoles.PROPONENT);
        User office = account(UserRoles.RPS_STAFF);

        // The candidate list contains DEAN accounts only, and never the outgoing Dean.
        String body = mockMvc.perform(withToken(get("/api/users/deans/candidates"), adminToken)
                        .param("college", COLLEGE))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains(available.getEmail()), "an unassigned DEAN account is a candidate");
        assertFalse(body.contains(sitting.getEmail()), "the current Dean must be excluded");
        assertFalse(body.contains(assignedElsewhere.getEmail()),
                "a Dean already assigned elsewhere must be excluded");
        assertFalse(body.contains(proponent.getEmail()), "a non-DEAN account must be excluded");
        assertFalse(body.contains(office.getEmail()), "an unrelated role must be excluded");

        // A non-DEAN account cannot become the replacement, even through a direct API call.
        for (Long id : new Long[] { proponent.getId(), office.getId() }) {
            mockMvc.perform(withToken(post("/api/users/deans/replace"), adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "userId", id,
                                    "assignedCollege", COLLEGE,
                                    "designation", "Dean"))))
                    .andExpect(status().isBadRequest());
        }
        entityManager.clear();

        // Nothing changed: the sitting Dean is still the active one (validation precedes writes).
        List<DeanAssignment> active = activeFor(COLLEGE);
        assertEquals(1, active.size(), "an invalid replacement must not change the assignment");
        assertEquals(sitting.getId(), active.get(0).getUserId());
    }

    private User createDean(String college) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Dean Villanueva");
        body.put("role", UserRoles.DEAN);
        body.put("designation", "Dean");
        body.put("assignedCollege", college);

        MvcResult result = mockMvc.perform(withToken(post("/api/users/admin/create"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        entityManager.clear();
        long id = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("user").path("id").asLong();
        return userRepository.findById(id).orElseThrow();
    }

    private List<DeanAssignment> activeFor(String college) {
        return deanAssignmentRepository
                .findByStatusAndAssignedCollegeIgnoreCase(DeanAssignment.STATUS_ACTIVE, college);
    }

    private List<DeanAssignment> revokedFor(String college) {
        return deanAssignmentRepository
                .findByStatusAndAssignedCollegeIgnoreCase(DeanAssignment.STATUS_REVOKED, college);
    }
}
