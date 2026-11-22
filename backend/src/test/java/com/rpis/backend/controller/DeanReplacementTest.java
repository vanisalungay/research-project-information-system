package com.rpis.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpis.backend.model.DeanAssignment;
import com.rpis.backend.model.EndorsementRequest;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.DeanAssignmentRepository;
import com.rpis.backend.repository.EndorsementRequestRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private EndorsementRequestRepository endorsementRequestRepository;
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
        // The real schema enforces one ACTIVE appointment per college. Hibernate builds the
        // test schema from the entities and cannot express this partial index, so add it
        // here: the succession must flush the outgoing closes before inserting the new
        // ACTIVE row, and this index is what catches it when that ordering is wrong.
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_dean_assignments_active_college "
                + "ON dean_assignments (COALESCE(assigned_college, '')) WHERE status = 'ACTIVE'");

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
                                "assignmentId", activeFor(COLLEGE).get(0).getId(),
                                "userId", deanB.getId()))))
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

        // The previous Dean keeps the account but loses the active Dean role; nothing is
        // deleted and no Proponent role is invented for a Dean-only account.
        User reloadedA = userRepository.findById(deanA.getId()).orElseThrow();
        assertEquals(UserRoles.DEAN, reloadedA.getRole(),
                "the primary role records that the account was created as a Dean");
        assertFalse(reloadedA.hasRole(UserRoles.DEAN), "the active Dean role is removed");
        assertFalse(reloadedA.hasRole(UserRoles.PROPONENT),
                "a revoked Dean is not silently made a Proponent");
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

    /**
     * A person who already has a Proponent account is appointed Dean by reusing that
     * account. The same address and password keep working, the account keeps the Proponent
     * role, and only one account exists for the person.
     */
    @Test
    void replacingWithAnExistingProponentAddsTheDeanRoleToThatAccount() throws Exception {
        createDean(COLLEGE);
        Long assignmentId = activeFor(COLLEGE).get(0).getId();

        User proponent = new User();
        proponent.setName("Vanisa Lungay");
        proponent.setEmail("vanisa.lungay@msunaawan.edu.ph");
        proponent.setPassword(passwordService.hash(PASSWORD));
        proponent.setRole(UserRoles.PROPONENT);
        proponent.assignRole(UserRoles.PROPONENT);
        proponent.setStatus("APPROVED");
        User existing = userRepository.saveAndFlush(proponent);
        entityManager.clear();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("assignmentId", assignmentId);
        body.put("name", "Vanisa Lungay");
        body.put("designation", "Dean");
        body.put("assignedCollege", COLLEGE);

        MvcResult result = mockMvc.perform(withToken(post("/api/users/deans/replacement"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountReused").value(true))
                .andExpect(jsonPath("$.user.id").value(existing.getId()))
                .andExpect(jsonPath("$.email").value("vanisa.lungay@msunaawan.edu.ph"))
                .andExpect(jsonPath("$.user.roles[0]").value(UserRoles.PROPONENT))
                .andExpect(jsonPath("$.user.roles[1]").value(UserRoles.DEAN))
                .andReturn();
        flushRestoringPassword(existing.getId());

        assertTrue(objectMapper.readTree(result.getResponse().getContentAsString())
                        .path("temporaryPassword").isNull(),
                "a reused account keeps its password");

        User promoted = userRepository.findById(existing.getId()).orElseThrow();
        assertTrue(promoted.hasRole(UserRoles.PROPONENT),
                "the existing Proponent role is preserved");
        assertTrue(promoted.hasRole(UserRoles.DEAN), "the Dean role is added");
        assertTrue(passwordService.verify(promoted.getPassword(), PASSWORD).matches(),
                "the existing password must keep working");

        assertEquals(1L, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM users WHERE LOWER(email) = ?",
                        Long.class, "vanisa.lungay@msunaawan.edu.ph"),
                "the person must not be duplicated");
        List<DeanAssignment> active = activeFor(COLLEGE);
        assertEquals(1, active.size());
        assertEquals(existing.getId(), active.get(0).getUserId());
    }

    /**
     * Both administrator screens read the same account. A rename in User Account Management
     * must therefore be visible in the Dean Role Management list immediately, instead of the
     * appointment's old snapshot being shown there.
     */
    @Test
    void deanRoleManagementShowsTheAccountsCurrentName() throws Exception {
        User dean = createDean(COLLEGE);
        Long assignmentId = activeFor(COLLEGE).get(0).getId();

        mockMvc.perform(withToken(put("/api/users/" + dean.getId()), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Renamed Dean"))))
                .andExpect(status().isOk());
        flushRestoringPassword(dean.getId());

        MvcResult result = mockMvc.perform(withToken(
                        get("/api/users/deans/assignments").param("status", "ALL"), adminToken))
                .andExpect(status().isOk())
                .andReturn();
        var rows = objectMapper.readTree(result.getResponse().getContentAsString());
        String listedName = null;
        boolean listed = false;
        for (var node : rows) {
            if (node.path("id").asLong() == assignmentId) {
                listed = true;
                listedName = node.path("name").asText();
            }
        }
        assertTrue(listed, "the appointment must be listed");
        assertEquals("Renamed Dean", listedName,
                "Dean Role Management must show the account's current name");
    }

    /**
     * A Dean-only account whose appointment is revoked stays a Dean in the record: the
     * account list reports Dean (Revoked), not Proponent, and no Proponent assignment is
     * written. The Dean only disappears from the active role while its appointment
     * remains visible as history.
     */
    @Test
    void aRevokedDeanOnlyAccountKeepsItsDeanHistoryAndGetsNoProponentRole() throws Exception {
        User dean = createDean(null);
        mockMvc.perform(withToken(delete("/api/users/deans/" + dean.getId()), adminToken))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        MvcResult result = mockMvc.perform(withToken(get("/api/users"), adminToken))
                .andExpect(status().isOk())
                .andReturn();
        var users = objectMapper.readTree(result.getResponse().getContentAsString());
        var listed = users.findParents("email").stream()
                .filter(node -> node.path("id").asLong() == dean.getId())
                .findFirst()
                .orElseThrow();

        assertEquals(UserRoles.DEAN, listed.path("role").asText(),
                "the primary role remains the Dean role");
        assertEquals(0, listed.path("roles").size(),
                "the active role list is empty - no Proponent was assigned");
        assertEquals(1, listed.path("revokedRoles").size());
        assertEquals(UserRoles.DEAN, listed.path("revokedRoles").get(0).asText(),
                "the revoked Dean role stays visible in the account record");
        assertEquals(DeanAssignment.STATUS_REVOKED, listed.path("deanStatus").asText());

        assertEquals(0L, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role = ?",
                        Long.class, dean.getId(), UserRoles.PROPONENT),
                "no Proponent assignment may be written");
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
                                "assignmentId", activeFor(COLLEGE).get(0).getId(),
                                "userId", pending.getId()))))
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
                                "assignmentId", activeFor(COLLEGE).get(0).getId(),
                                "userId", candidate.getId()))))
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
                                "assignmentId", activeFor(COLLEGE).get(0).getId(),
                                "userId", deanB.getId()))))
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
                                    "assignmentId", activeFor(COLLEGE).get(0).getId(),
                                    "userId", id))))
                    .andExpect(status().isBadRequest());
        }
        entityManager.clear();

        // Nothing changed: the sitting Dean is still the active one (validation precedes writes).
        List<DeanAssignment> active = activeFor(COLLEGE);
        assertEquals(1, active.size(), "an invalid replacement must not change the assignment");
        assertEquals(sitting.getId(), active.get(0).getUserId());
    }

    @Test
    void replacingKeepsTheAppointmentCollegeAndDesignationEvenIfTheBrowserSendsOthers()
            throws Exception {
        User deanA = createDean(COLLEGE);
        User deanB = deanCandidate();

        // The request body may still carry old field names, but the service reads the
        // college and designation from the stored appointment, not from the client.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("assignmentId", activeFor(COLLEGE).get(0).getId());
        body.put("userId", deanB.getId());
        body.put("assignedCollege", "College of Fisheries and Marine Sciences (CFMS)");
        body.put("designation", "Department Chair");

        mockMvc.perform(withToken(post("/api/users/deans/replace"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedCollege").value(COLLEGE))
                .andExpect(jsonPath("$.designation").value("Dean"));
        entityManager.flush();
        entityManager.clear();

        User replacement = userRepository.findById(deanB.getId()).orElseThrow();
        assertEquals(COLLEGE, replacement.getAssignedCollege(),
                "the college is the replaced appointment's college");
        assertEquals("Dean", replacement.getDesignation(),
                "the designation is the replaced appointment's designation");
        assertNotNull(userRepository.findById(deanA.getId()).orElseThrow().getEmail(),
                "the outgoing account is kept");
    }

    @Test
    void aReplacementMustIdentifyTheAppointmentBeingReplaced() throws Exception {
        createDean(COLLEGE);
        User replacement = deanCandidate();

        mockMvc.perform(withToken(post("/api/users/deans/replace"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", replacement.getId()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anInstitutionWideDeanCanBeRevokedWithoutDeletingTheAccount() throws Exception {
        User dean = createDean(null);
        Long assignmentId = activeInstitutionWide().get(0).getId();

        mockMvc.perform(withToken(delete("/api/users/deans/" + dean.getId()), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(UserRoles.DEAN));
        entityManager.flush();
        entityManager.clear();

        User revoked = userRepository.findById(dean.getId()).orElseThrow();
        assertEquals(UserRoles.DEAN, revoked.getRole(),
                "the primary role remains the historical Dean role");
        assertFalse(revoked.hasRole(UserRoles.PROPONENT),
                "revoking a Dean does not assign the Proponent role");
        assertNull(revoked.getAssignedCollege(), "the Dean scope is removed");
        assertNull(revoked.getDesignation(), "the Dean designation is removed");
        assertNotNull(revoked.getEmail(), "the account and its data are not deleted");

        assertTrue(activeInstitutionWide().isEmpty(), "no active appointment remains");
        List<DeanAssignment> history = revokedInstitutionWide();
        assertEquals(1, history.size(), "the appointment is kept as history");
        assertEquals(assignmentId, history.get(0).getId());
        assertEquals(dean.getId(), history.get(0).getUserId());
        assertNotNull(history.get(0).getDateRevoked(), "the revocation is dated");
        assertEquals(adminId, history.get(0).getRevokedBy(),
                "the acting administrator is recorded");
    }

    @Test
    void aCollegeDeanCannotBeRevokedWithoutAReplacement() throws Exception {
        User dean = createDean(COLLEGE);
        Long assignmentId = activeFor(COLLEGE).get(0).getId();

        mockMvc.perform(withToken(delete("/api/users/deans/" + dean.getId()), adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString(
                        "cannot be revoked yet")));
        entityManager.clear();

        User stillDean = userRepository.findById(dean.getId()).orElseThrow();
        assertEquals(UserRoles.DEAN, stillDean.getRole(), "nothing is revoked");
        assertEquals(COLLEGE, stillDean.getAssignedCollege());
        assertEquals(1, activeFor(COLLEGE).size());
        assertEquals(assignmentId, activeFor(COLLEGE).get(0).getId());
    }

    /**
     * The succession owns the outgoing Dean's pending work: every PENDING endorsement
     * request routed to them moves to the replacement, while a decided request keeps its
     * historical Dean and every other field is untouched.
     */
    @Test
    void replacingADeanTransfersTheOutgoingDeansPendingEndorsements() throws Exception {
        User deanA = createDean(COLLEGE);
        User deanB = deanCandidate();
        EndorsementRequest pending = endorsement(deanA, EndorsementRequest.STATUS_PENDING);
        EndorsementRequest approved = endorsement(deanA, EndorsementRequest.STATUS_APPROVED);

        mockMvc.perform(withToken(post("/api/users/deans/replace"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "assignmentId", activeFor(COLLEGE).get(0).getId(),
                                "userId", deanB.getId()))))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        EndorsementRequest moved = endorsementRequestRepository.findById(pending.getId())
                .orElseThrow();
        assertEquals(deanB.getId(), moved.getAssignedDean().getId(),
                "the pending request is transferred to the replacement Dean");
        assertEquals(EndorsementRequest.STATUS_PENDING, moved.getStatus(),
                "the status is preserved");
        assertEquals(COLLEGE, moved.getCollege(), "the college is preserved");
        assertNotNull(moved.getCreatedAt(), "the submission date is preserved");

        EndorsementRequest untouched = endorsementRequestRepository.findById(approved.getId())
                .orElseThrow();
        assertEquals(deanA.getId(), untouched.getAssignedDean().getId(),
                "a decided request keeps its historical Dean");
        assertEquals(EndorsementRequest.STATUS_APPROVED, untouched.getStatus(),
                "a decided request keeps its status");
    }

    @Test
    void revokingADeanRequiresAnAdministrator() throws Exception {
        User dean = createDean(COLLEGE);

        mockMvc.perform(withToken(delete("/api/users/deans/" + dean.getId()), proponentToken))
                .andExpect(status().isForbidden());
        entityManager.clear();

        assertEquals(UserRoles.DEAN,
                userRepository.findById(dean.getId()).orElseThrow().getRole(),
                "a refused caller cannot revoke the appointment");
        assertEquals(1, activeFor(COLLEGE).size(),
                "the appointment is still active");
    }

    /**
     * Manage Dean -> Replace Dean: the Create Dean Account form creates the incoming Dean
     * and the succession is completed in the same call, so the account is never left as a
     * second Dean beside the sitting one.
     */
    @Test
    void creatingTheReplacementDeanCompletesTheSuccessionInOneStep() throws Exception {
        User outgoing = createDean(COLLEGE);
        Long assignmentId = activeFor(COLLEGE).get(0).getId();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("assignmentId", assignmentId);
        body.put("name", "Incoming Person");
        body.put("designation", "Dean");
        body.put("assignedCollege", COLLEGE);

        MvcResult result = mockMvc.perform(
                        withToken(post("/api/users/deans/replacement"), adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").isNotEmpty())
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value(UserRoles.DEAN))
                .andExpect(jsonPath("$.user.assignedCollege").value(COLLEGE))
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode created =
                objectMapper.readTree(result.getResponse().getContentAsString());
        long incomingId = created.path("user").path("id").asLong();

        // CreatedAccountResponse.of nulls the password on the returned entity. Inside this
        // transactional test that entity is still managed, so flushing it as-is would hit the
        // not-null column. Restore its hash first, then flush the whole succession and detach.
        User managedIncoming = entityManager.find(User.class, incomingId);
        managedIncoming.setPassword(passwordService.hash(
                created.path("temporaryPassword").asText()));
        entityManager.flush();
        entityManager.clear();

        User incoming = userRepository.findById(incomingId).orElseThrow();
        assertEquals(UserRoles.DEAN, incoming.getRole(),
                "the newly created account is the Dean");
        assertEquals(COLLEGE, incoming.getAssignedCollege(), "appointed to the same college");
        assertEquals("Dean", incoming.getDesignation());
        assertNotNull(incoming.getEmail(), "the account has an institutional address");
        assertTrue(Boolean.TRUE.equals(incoming.getMustChangePassword()),
                "the generated password must be changed at first login");

        // Exactly one ACTIVE appointment, held by the new Dean.
        List<DeanAssignment> active = activeFor(COLLEGE);
        assertEquals(1, active.size(), "a college must hold exactly one active Dean");
        assertEquals(incomingId, active.get(0).getUserId());

        // The outgoing appointment is history, and the outgoing account is preserved.
        List<DeanAssignment> revoked = revokedFor(COLLEGE);
        assertEquals(1, revoked.size(), "the previous appointment is kept as history");
        assertEquals(assignmentId, revoked.get(0).getId());
        assertEquals(outgoing.getId(), revoked.get(0).getUserId());
        assertNotNull(revoked.get(0).getDateRevoked(), "the revocation is dated");

        User former = userRepository.findById(outgoing.getId()).orElseThrow();
        assertEquals(UserRoles.DEAN, former.getRole(),
                "the primary role records the account's Dean origin");
        assertFalse(former.hasRole(UserRoles.DEAN), "the active Dean role is removed");
        assertFalse(former.hasRole(UserRoles.PROPONENT),
                "no Proponent role is invented for the outgoing Dean");
        assertNotNull(former.getEmail(), "the outgoing account is never deleted");

        Integer replaced = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM endorsement_request_events WHERE event_type = ?",
                Integer.class, "DEAN_ROLE_REPLACED");
        assertEquals(1, replaced, "the succession is recorded in the audit trail");
    }

    /** The replacement form cannot appoint the new Dean to a different college. */
    @Test
    void aCreatedReplacementCannotBeAssignedToAnotherCollege() throws Exception {
        User sitting = createDean(COLLEGE);
        Long assignmentId = activeFor(COLLEGE).get(0).getId();
        long usersBefore = userRepository.count();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("assignmentId", assignmentId);
        body.put("name", "Wrong College Person");
        body.put("designation", "Dean");
        body.put("assignedCollege", "College of Fisheries and Marine Sciences (CFMS)");

        mockMvc.perform(withToken(post("/api/users/deans/replacement"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        entityManager.clear();

        assertEquals(usersBefore, userRepository.count(),
                "the refused request creates no account");
        List<DeanAssignment> active = activeFor(COLLEGE);
        assertEquals(1, active.size(), "the sitting Dean is untouched");
        assertEquals(sitting.getId(), active.get(0).getUserId());
    }

    /** Creating the replacement is an administrator operation, like every Dean route. */
    @Test
    void onlyAnAdministratorMayCreateAReplacementDean() throws Exception {
        createDean(COLLEGE);
        Long assignmentId = activeFor(COLLEGE).get(0).getId();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("assignmentId", assignmentId);
        body.put("name", "Unauthorized Person");
        body.put("designation", "Dean");
        body.put("assignedCollege", COLLEGE);

        mockMvc.perform(withToken(post("/api/users/deans/replacement"), proponentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
        entityManager.clear();

        assertEquals(1, activeFor(COLLEGE).size(),
                "a refused caller cannot create or appoint a Dean");
    }

    private User createDean(String college) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        // A distinct name per Dean: the same institutional address is now reused (the same
        // person), so creating two Deans for two colleges needs two different people. The
        // number sits in the given names so the surname stays a word (a digit-only surname
        // would derive a temporary password shorter than the minimum).
        body.put("name", "Dean" + SEQUENCE.incrementAndGet() + " Villanueva");
        body.put("role", UserRoles.DEAN);
        body.put("designation", "Dean");
        body.put("assignedCollege", college);

        MvcResult result = mockMvc.perform(withToken(post("/api/users/admin/create"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        // Flush the new role assignment (an element collection, not an IDENTITY insert)
        // before detaching; otherwise the following request would see no active role.
        entityManager.flush();
        entityManager.clear();
        long id = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("user").path("id").asLong();
        return userRepository.findById(id).orElseThrow();
    }

    /**
     * Restores the managed account's password after a response nulled it, then flushes.
     * Only needed inside this test transaction; a real request's entity is already detached.
     */
    private void flushRestoringPassword(Long userId) {
        String stored = jdbcTemplate.queryForObject(
                "SELECT password FROM users WHERE id = ?", String.class, userId);
        User managed = entityManager.find(User.class, userId);
        if (managed != null) {
            managed.setPassword(stored);
        }
        entityManager.flush();
        entityManager.clear();
    }

    private List<DeanAssignment> activeFor(String college) {
        return deanAssignmentRepository
                .findByStatusAndAssignedCollegeIgnoreCase(DeanAssignment.STATUS_ACTIVE, college);
    }

    private List<DeanAssignment> revokedFor(String college) {
        return deanAssignmentRepository
                .findByStatusAndAssignedCollegeIgnoreCase(DeanAssignment.STATUS_REVOKED, college);
    }

    private List<DeanAssignment> activeInstitutionWide() {
        return deanAssignmentRepository
                .findByStatusAndAssignedCollegeIsNull(DeanAssignment.STATUS_ACTIVE);
    }

    private List<DeanAssignment> revokedInstitutionWide() {
        return deanAssignmentRepository
                .findByStatusAndAssignedCollegeIsNull(DeanAssignment.STATUS_REVOKED);
    }

    /** An endorsement routed to a Dean for the test college. */
    private EndorsementRequest endorsement(User dean, String status) {
        EndorsementRequest request = new EndorsementRequest();
        request.setReferenceNumber("MSUN-ORPS-END-TEST-" + SEQUENCE.incrementAndGet());
        request.setProponent(account(UserRoles.PROPONENT));
        request.setAssignedDean(dean);
        request.setCollege(COLLEGE);
        request.setProjectTitle("Pending Endorsement " + SEQUENCE.get());
        request.setProposalAnnouncementId(1L);
        request.setStatus(status);
        return endorsementRequestRepository.saveAndFlush(request);
    }
}
