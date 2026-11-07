package com.rpis.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpis.backend.model.EndorsementRequestEvent;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.PasswordService;
import com.rpis.backend.service.TokenService;
import com.rpis.backend.service.UserService;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The standard administrator account-creation route (POST /api/users/admin/create), which
 * the Create Account form on the User Accounts page uses for every role.
 *
 * <p>The Dean is created here like any other role. It used to be refused by
 * {@code UserService.createStaffAccount} and provisioned only by a separate screen, which
 * meant two account-creation flows and two places to keep the Dean rules correct. The route
 * now recognises the Dean role and delegates it to {@code DeanAccountService}, so the
 * validation, the BCrypt hashing, the college scope and the audit trail all come from that
 * one implementation - and it still refuses every caller who is not an RPS administrator.
 *
 * <p>Pinned down here: an administrator can create a Dean through the standard route,
 * nobody else can, every other role keeps working as before, the Dean's initial password is
 * never stored in the clear, the assignment is audited, and an existing account is neither
 * duplicated nor promoted.
 *
 * <p>Runs against the isolated {@code rpis_test} database only, and every test is
 * transactional, so the rows it creates are rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminAccountCreationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    /** The initial password an administrator chooses on the Create Account form. */
    private static final String PASSWORD = "initial-password-1234";

    /** The college a Dean is scoped to in these tests. */
    private static final String COLLEGE = "College of Fisheries and Marine Sciences (CFMS)";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
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
    private String staffToken;
    private String proponentToken;
    private String deanToken;

    @BeforeEach
    void setUp() throws Exception {
        requireTestDatabase();

        // The audit trail this class asserts on. Emptied so an assertion sees only the
        // events this test produced; the truncation belongs to the test transaction and is
        // rolled back with it.
        jdbcTemplate.execute("TRUNCATE TABLE endorsement_request_events RESTART IDENTITY CASCADE");

        adminId = account(UserRoles.RPS_ADMIN).getId();
        adminToken = tokenService.generateToken(adminId, UserRoles.RPS_ADMIN);
        staffToken = tokenFor(UserRoles.RPS_STAFF);
        proponentToken = tokenFor(UserRoles.PROPONENT);
        deanToken = tokenFor(UserRoles.DEAN);
    }

    /** Safety guard: this class must never run against the real database. */
    private void requireTestDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertTrue(url.contains("rpis_test"),
                    "Account creation tests must run against the isolated rpis_test database, "
                            + "but connected to: " + url);
        }
    }

    private String uniqueEmail(String prefix) {
        return prefix + ".create" + SEQUENCE.incrementAndGet() + "@msunaawan.edu.ph";
    }

    private User account(String role) {
        User user = new User();
        user.setName(role + " Account");
        user.setEmail(uniqueEmail(role.toLowerCase()));
        user.setPassword(passwordService.hash(PASSWORD));
        user.setRole(role);
        user.setStatus("APPROVED");
        return userRepository.saveAndFlush(user);
    }

    private String tokenFor(String role) {
        return tokenService.generateToken(account(role).getId(), role);
    }

    /** The payload the Create Account form sends for a role. */
    private Map<String, Object> payload(String email, String role) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "New " + role + " Account");
        body.put("email", email);
        body.put("password", PASSWORD);
        body.put("role", role);
        return body;
    }

    /** The payload the Create Account form sends when the Dean role is selected. */
    private Map<String, Object> deanPayload(String email) {
        return deanPayload(email, COLLEGE);
    }

    /** The Dean payload for a specific college (a college holds one sitting Dean). */
    private Map<String, Object> deanPayload(String email, String college) {
        Map<String, Object> body = payload(email, UserRoles.DEAN);
        body.put("designation", "Dean");
        body.put("assignedCollege", college);
        return body;
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder builder,
            String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    /**
     * Performs the create request, then detaches the persistence context.
     *
     * <p>The controller nulls the password on the entity it returns, because a response must
     * never carry it. Inside this test's transaction that entity is still managed, so the
     * null would be flushed as an UPDATE the moment Hibernate runs the next query - which is
     * exactly what the not-null constraint on {@code users.password} refuses. In a real
     * request the repository's own read-only transaction leaves the entity detached and
     * Hibernate never flushes that change, so detaching here reproduces the real behaviour
     * instead of the test transaction's.
     */
    private org.springframework.test.web.servlet.ResultActions create(String token,
            Map<String, Object> body) throws Exception {
        org.springframework.test.web.servlet.ResultActions result =
                mockMvc.perform(withToken(post("/api/users/admin/create"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)));
        entityManager.clear();
        return result;
    }

    /** The address the response reports for the account it just created. */
    private String responseEmail(org.springframework.test.web.servlet.MvcResult result)
            throws Exception {
        return responseText(result, "email");
    }

    /** Reads one string field of a create response body. */
    private String responseText(org.springframework.test.web.servlet.MvcResult result, String field)
            throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path(field).asText();
    }

    private String localPartOf(String email) {
        int at = email.indexOf(0x40);
        return at < 0 ? email : email.substring(0, at);
    }

    @Test
    void anAdministratorCreatesADeanThroughTheStandardAccountForm() throws Exception {
        String email = uniqueEmail("dean");

        var result = create(adminToken, deanPayload(email))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value(UserRoles.DEAN))
                .andExpect(jsonPath("$.user.status").value("APPROVED"))
                .andExpect(jsonPath("$.user.designation").value("Dean"))
                .andExpect(jsonPath("$.user.assignedCollege").value(COLLEGE))
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andReturn();

        // The form sends only the name and the role: the address comes from the response.
        String generatedEmail = responseEmail(result);
        assertTrue(generatedEmail.endsWith("@msunawaan.edu.ph"),
                "the generated address must use the institutional domain, was " + generatedEmail);
        User dean = userRepository.findByEmail(generatedEmail).orElseThrow();
        assertEquals(UserRoles.DEAN, dean.getRole(),
                "the account must be stored with the canonical Dean role");
        assertEquals("APPROVED", dean.getStatus(),
                "an account an administrator creates is usable at once");

        // The initial password goes through the Dean provisioning path, which hashes it, so
        // the column never holds a readable password.
        assertFalse(PASSWORD.equals(dean.getPassword()), "the password must not be stored raw");
        assertTrue(passwordService.isHashed(dean.getPassword()),
                "the stored password must be a hash");
        String temporaryPassword = responseText(result, "temporaryPassword");
        assertFalse(temporaryPassword.isBlank(),
                "the response must carry the generated temporary password");
        assertTrue(passwordService.verify(dean.getPassword(), temporaryPassword).matches(),
                "the temporary password the response returned must be the one that works");
        assertTrue(Boolean.TRUE.equals(dean.getMustChangePassword()),
                "a generated password must be changed at the first sign-in");
        assertEquals(com.rpis.backend.service.InstitutionalCredentialsService
                        .temporaryPasswordFor("New " + UserRoles.DEAN + " Account"),
                temporaryPassword,
                "the Dean must be issued the name-based temporary password");

        // And the assignment is in the audit trail, attributed to the administrator.
        Integer audited = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM endorsement_request_events WHERE actor_id = ? AND event_type = ?",
                Integer.class, adminId, EndorsementRequestEvent.TYPE_DEAN_ROLE_ASSIGNED);
        assertEquals(1, audited, "provisioning a Dean must be recorded in the audit trail");
    }

    @Test
    void onlyAnAdministratorMayProvisionADean() throws Exception {
        String email = uniqueEmail("dean-staff");

        // RPS Staff may create every other role through this route, but not a Dean: the Dean
        // carries an endorsement scope, and the service that provisions it is RPS_ADMIN only.
        create(staffToken, deanPayload(email)).andExpect(status().isForbidden());
        assertTrue(userRepository.findByEmail(email).isEmpty(),
                "a refused request must not leave an account behind");

        // No other role reaches the route at all.
        for (String token : new String[] { proponentToken, deanToken }) {
            create(token, deanPayload(uniqueEmail("dean-refused"))).andExpect(status().isForbidden());
        }
    }

    @Test
    void theDeanRoleIsStillRefusedByTheGeneralAccountService() {
        // Defence in depth: the controller routes the Dean role to the Dean service, and the
        // general service refuses it, so no future caller of that service can mint a Dean
        // without the administrator check the Dean service performs.
        User dean = new User();
        dean.setName("Bypass Attempt");
        dean.setEmail(uniqueEmail("bypass"));
        dean.setPassword(PASSWORD);
        dean.setRole(UserRoles.DEAN);

        assertThrows(IllegalArgumentException.class, () -> userService.createStaffAccount(dean));
        assertTrue(userRepository.findByEmail(dean.getEmail()).isEmpty(),
                "the refused account must not be stored");
    }

    @Test
    void everyOtherRoleStillCreatesThroughTheStandardAccountForm() throws Exception {
        for (String role : new String[] { UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF,
                UserRoles.OVCRIGE, UserRoles.OVCAF, UserRoles.REC, UserRoles.OC }) {
            String email = uniqueEmail(role.toLowerCase());

            var result = create(adminToken, payload(email, role))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.role").value(role))
                    .andExpect(jsonPath("$.mustChangePassword").value(true))
                    .andReturn();

            User created = userRepository.findByEmail(responseEmail(result)).orElseThrow();
            assertEquals(role, created.getRole(),
                    role + " must still be created through the standard form");
            assertTrue(passwordService.isHashed(created.getPassword()),
                    role + " must still get a hashed password");
            assertEquals(com.rpis.backend.service.InstitutionalCredentialsService
                            .temporaryPasswordFor("New " + role + " Account"),
                    responseText(result, "temporaryPassword"),
                    role + " must be issued the name-based temporary password");
            assertTrue(passwordService.verify(created.getPassword(),
                            responseText(result, "temporaryPassword")).matches(),
                    role + " must store a BCrypt hash of the password it returned");
        }
    }

    @Test
    void aDeanAccountIsListedWithTheOtherAccounts() throws Exception {
        String email = uniqueEmail("dean-listed");

        var result = create(adminToken, deanPayload(email)).andExpect(status().isCreated())
                .andReturn();
        String generatedEmail = responseEmail(result);

        // The account list the User Accounts page reads shows the Dean like any other
        // account, which is what makes it searchable by name, e-mail and role there.
        mockMvc.perform(withToken(get("/api/users"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == '" + generatedEmail + "')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.email == '" + generatedEmail + "')].role")
                        .value(hasItem(UserRoles.DEAN)));
    }

    @Test
    void aDeanOnAnotherDomainIsRefusedWithAReadableReason() throws Exception {
        // The Dean-specific validation still runs behind the standard form: the account is
        // refused, and the reason is what the form shows.
        create(adminToken, deanPayload("dean@example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("MSU Naawan")));
    }

    @Test
    void aDeanEmailThatAlreadyExistsIsNumberedInsteadOfDuplicated() throws Exception {
        // The address is generated from the name, so two faculty members with the same name
        // collide. The rule is to number the address rather than refuse the request, and an
        // account that already holds the plain address is left exactly as it was.
        var first = create(adminToken, deanPayload(uniqueEmail("dean-duplicate")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailAdjusted").value(false))
                .andReturn();
        String firstEmail = responseEmail(first);

        // A different college, so the second Dean does not collide with the first on the
        // one-active-Dean-per-college rule (the point here is the address numbering).
        var second = create(adminToken,
                deanPayload(uniqueEmail("dean-duplicate-2"),
                        "College of Environment and Life Sciences (CELS)"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailAdjusted").value(true))
                .andReturn();
        String secondEmail = responseEmail(second);

        assertFalse(firstEmail.equalsIgnoreCase(secondEmail),
                "a taken address must be numbered rather than reused");
        assertTrue(secondEmail.indexOf(0x40) > firstEmail.indexOf(0x40),
                "the number must be appended to the local part, before the domain");
        String suffix = localPartOf(secondEmail).substring(localPartOf(firstEmail).length());
        assertTrue(suffix.matches("\\D*\\d+"),
                "a number must be appended to the taken address, got: " + secondEmail);

        // Both accounts exist, and neither claim of another account's address is allowed.
        assertEquals(UserRoles.DEAN, userRepository.findByEmail(firstEmail).orElseThrow().getRole());
        assertEquals(UserRoles.DEAN, userRepository.findByEmail(secondEmail).orElseThrow().getRole());
    }

    @Test
    void aDeanCanBeCreatedForEachOfTheFiveColleges() throws Exception {
        // The Dean Role Management form offers the five colleges as the Dean's college. Each
        // choice must be stored on the account's assignedCollege while the account keeps the
        // DEAN system role and the generated-credentials flow is unchanged.
        for (String college : new String[] {
                "College of Fisheries and Marine Sciences (CFMS)",
                "College of Environment and Life Sciences (CELS)",
                "College of Education and Social Sciences (CESS)",
                "College of Business and Information Technology (CBIT)",
                "Integrated Development School (IDS)" }) {
            var result = create(adminToken, deanPayload(uniqueEmail("dean-college"), college))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.role").value(UserRoles.DEAN))
                    .andExpect(jsonPath("$.user.assignedCollege").value(college))
                    .andExpect(jsonPath("$.mustChangePassword").value(true))
                    .andReturn();

            User dean = userRepository.findByEmail(responseEmail(result)).orElseThrow();
            assertEquals(UserRoles.DEAN, dean.getRole(),
                    "a Dean created for " + college + " must keep the DEAN system role");
            assertEquals(college, dean.getAssignedCollege(),
                    "the selected college must be stored on the account");
            assertTrue(passwordService.isHashed(dean.getPassword()),
                    "the generated password must still be BCrypt-hashed");
        }
    }
}
