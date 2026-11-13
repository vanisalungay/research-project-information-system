package com.rpis.backend.service;

import com.rpis.backend.dto.DeanAccountRequest;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Database-backed tests for password handling.
 *
 * <p>Covers the paths that used to store or compare plain text: registration, staff
 * creation, the Dean account created by an administrator, the profile password change,
 * login, and the two migration paths for accounts that predate hashing.
 *
 * <p>Runs against the isolated {@code rpis_test} database only, and every test is
 * transactional so the rows it creates are rolled back.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PasswordSecurityPostgresTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private UserService userService;
    @Autowired
    private PasswordService passwordService;
    @Autowired
    private PasswordMigrationService passwordMigrationService;
    @Autowired
    private DeanAccountService deanAccountService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DataSource dataSource;

    /** Safety guard: these tests must never run against a real database. */
    private void requireTestDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertTrue(url.contains("rpis_test"),
                    "Password tests must run against the isolated rpis_test database, but connected to: " + url);
        }
    }

    private String uniqueEmail(String prefix) {
        return prefix + ".pw" + SEQUENCE.incrementAndGet() + "@msunaawan.edu.ph";
    }

    private User newUser(String email, String password, String role, String status) {
        User user = new User();
        user.setName("Password Test");
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    /**
     * Reads the stored column directly, so the assertions see what is really there.
     *
     * <p>The flush is required, not cosmetic: these tests are transactional and the
     * service updates a <em>managed</em> entity, so Hibernate defers the UPDATE until
     * the transaction ends. A raw JDBC read would otherwise see the pre-change value and
     * the assertion would be testing the persistence context rather than the database.
     */
    private String storedPassword(Long userId) {
        userRepository.flush();
        return jdbcTemplate.queryForObject(
                "SELECT password FROM users WHERE id = ?", String.class, userId);
    }

    // ------------------------------------------------------------------ writes

    @Test
    void registrationStoresAHashAndNeverThePlainPassword() throws Exception {
        requireTestDatabase();
        String raw = "proponentpassword1234";

        User created = userService.createUser(newUser(uniqueEmail("register"), raw, "PROPONENT", "PENDING"));

        String stored = storedPassword(created.getId());
        assertFalse(raw.equals(stored), "the stored value must not be the plain password");
        assertTrue(passwordService.isHashed(stored), "the column must hold a BCrypt hash");
        assertTrue(passwordService.verify(stored, raw).matches());
    }

    @Test
    void staffAccountCreationStoresAHash() throws Exception {
        requireTestDatabase();
        String raw = "rpsstaffpassword1234";

        User created = userService.createStaffAccount(newUser(uniqueEmail("staff"), raw, "RPS_STAFF", "APPROVED"));

        String stored = storedPassword(created.getId());
        assertFalse(raw.equals(stored), "the stored value must not be the plain password");
        assertTrue(passwordService.isHashed(stored));
    }

    @Test
    void theAdministratorCreatedDeanAccountStoresAHash() throws Exception {
        requireTestDatabase();
        String raw = "deanpassword1234";

        User admin = userRepository.save(newUser(uniqueEmail("admin"), "irrelevant", "RPS_ADMIN", "APPROVED"));

        DeanAccountRequest request = new DeanAccountRequest();
        request.setName("Dean Password Test");
        request.setEmail(uniqueEmail("dean"));
        request.setPassword(raw);
        request.setAssignedCollege("College of Science and Technology");

        User dean = deanAccountService.save(request, admin.getId(), "RPS_ADMIN");

        String stored = storedPassword(dean.getId());
        assertFalse(raw.equals(stored), "the stored value must not be the plain password");
        assertTrue(passwordService.isHashed(stored));
        assertTrue(passwordService.verify(stored, raw).matches());
    }

    @Test
    void aPasswordChangeStoresANewHashAndInvalidatesTheOldPassword() throws Exception {
        requireTestDatabase();
        User created = userService.createStaffAccount(
                newUser(uniqueEmail("change"), "originalpassword", "RPS_STAFF", "APPROVED"));

        User update = new User();
        update.setPassword("replacementpassword");
        userService.updateUser(created.getId(), update);

        String stored = storedPassword(created.getId());
        assertTrue(passwordService.isHashed(stored));
        assertTrue(passwordService.verify(stored, "replacementpassword").matches(),
                "the new password must work");
        assertFalse(passwordService.verify(stored, "originalpassword").matches(),
                "the old password must stop working");
    }

    @Test
    void aPartialProfileUpdateLeavesTheStoredPasswordAlone() throws Exception {
        requireTestDatabase();
        User created = userService.createStaffAccount(
                newUser(uniqueEmail("partial"), "untouchedpassword", "RPS_STAFF", "APPROVED"));
        String before = storedPassword(created.getId());

        User update = new User();
        update.setName("Renamed Account");
        userService.updateUser(created.getId(), update);

        assertEquals(before, storedPassword(created.getId()),
                "an update that does not carry a password must not re-hash it");
    }

    // ------------------------------------------------------------------- login

    @Test
    void loginSucceedsWithTheRightPasswordAndFailsWithAWrongOne() throws Exception {
        requireTestDatabase();
        String email = uniqueEmail("login");
        String raw = "correctpassword1234";
        userService.createStaffAccount(newUser(email, raw, "RPS_STAFF", "APPROVED"));

        UserService.LoginOutcome ok = userService.login(email, raw, "RPS_STAFF");
        assertEquals(UserService.LoginStatus.OK, ok.status());
        assertNotNull(ok.user());

        UserService.LoginOutcome wrong = userService.login(email, "not-the-password", "RPS_STAFF");
        assertEquals(UserService.LoginStatus.INVALID_CREDENTIALS, wrong.status());
        assertNull(wrong.user());
    }

    @Test
    void loginRequiresTheMatchingRole() throws Exception {
        requireTestDatabase();
        String email = uniqueEmail("role");
        String raw = "rolepassword1234";
        userService.createStaffAccount(newUser(email, raw, "RPS_STAFF", "APPROVED"));

        assertEquals(UserService.LoginStatus.INVALID_CREDENTIALS,
                userService.login(email, raw, "OVCRIGE").status(),
                "a correct password with the wrong role must not authenticate");
    }

    @Test
    void anUnknownEmailIsIndistinguishableFromAWrongPassword() throws Exception {
        requireTestDatabase();
        String raw = "somepassword1234";
        userService.createStaffAccount(newUser(uniqueEmail("known"), raw, "RPS_STAFF", "APPROVED"));

        UserService.LoginOutcome unknown = userService.login(uniqueEmail("nobody"), raw, "RPS_STAFF");

        // The same status as a wrong password, so the endpoint cannot be used to work
        // out which e-mail addresses have accounts.
        assertEquals(UserService.LoginStatus.INVALID_CREDENTIALS, unknown.status());
        assertNull(unknown.user());
    }

    @Test
    void aPendingOrRejectedAccountIsReportedOnlyWhenThePasswordIsCorrect() throws Exception {
        requireTestDatabase();
        String raw = "pendingpassword1234";
        String pendingEmail = uniqueEmail("pending");
        userService.createUser(newUser(pendingEmail, raw, "PROPONENT", "PENDING"));

        assertEquals(UserService.LoginStatus.PENDING,
                userService.login(pendingEmail, raw, "PROPONENT").status());
        // With the wrong password the account state must not leak.
        assertEquals(UserService.LoginStatus.INVALID_CREDENTIALS,
                userService.login(pendingEmail, "wrong", "PROPONENT").status());

        String rejectedEmail = uniqueEmail("rejected");
        User rejected = userService.createUser(newUser(rejectedEmail, raw, "PROPONENT", "PENDING"));
        rejected.setStatus("REJECTED");
        userRepository.save(rejected);

        assertEquals(UserService.LoginStatus.REJECTED,
                userService.login(rejectedEmail, raw, "PROPONENT").status());
        assertEquals(UserService.LoginStatus.INVALID_CREDENTIALS,
                userService.login(rejectedEmail, "wrong", "PROPONENT").status());
    }

    // --------------------------------------------------------------- migration

    @Test
    void aLegacyPlainTextAccountIsUpgradedOnItsNextSuccessfulLogin() throws Exception {
        requireTestDatabase();
        String email = uniqueEmail("legacy-login");
        String raw = "legacypassword1234";
        User legacy = userRepository.saveAndFlush(newUser(email, raw, "RPS_STAFF", "APPROVED"));

        // Prove the fixture really is the old shape.
        assertEquals(raw, storedPassword(legacy.getId()), "the fixture must start as plain text");

        UserService.LoginOutcome outcome = userService.login(email, raw, "RPS_STAFF");

        assertEquals(UserService.LoginStatus.OK, outcome.status(),
                "an existing account must never be locked out by the change");
        String stored = storedPassword(legacy.getId());
        assertTrue(passwordService.isHashed(stored), "the row must be hashed after the sign-in");
        assertTrue(passwordService.verify(stored, raw).matches(), "the same password must still work");
    }

    @Test
    void theMigrationPassConvertsPlainTextRowsAndIsIdempotent() throws Exception {
        requireTestDatabase();
        String raw = "migratepassword1234";
        User legacy = userRepository.saveAndFlush(
                newUser(uniqueEmail("legacy-migrate"), raw, "RPS_STAFF", "APPROVED"));

        int converted = passwordMigrationService.migratePlainTextPasswords();

        assertTrue(converted >= 1, "at least the row this test created must be converted");
        String stored = storedPassword(legacy.getId());
        assertTrue(passwordService.isHashed(stored));
        assertTrue(passwordService.verify(stored, raw).matches());

        // Running it again must change nothing: an already-hashed row is skipped, so the
        // value is neither re-hashed nor corrupted.
        String afterFirst = storedPassword(legacy.getId());
        passwordMigrationService.migratePlainTextPasswords();
        assertEquals(afterFirst, storedPassword(legacy.getId()),
                "a second pass must not touch an already-hashed password");
    }

    @Test
    void anAlreadyHashedRowIsNeverDoubleHashed() throws Exception {
        requireTestDatabase();
        User created = userService.createStaffAccount(
                newUser(uniqueEmail("already-hashed"), "hashmeplease1234", "RPS_STAFF", "APPROVED"));
        String before = storedPassword(created.getId());

        passwordMigrationService.migratePlainTextPasswords();

        assertEquals(before, storedPassword(created.getId()),
                "a BCrypt hash must be recognised and left untouched");
    }

    @Test
    void afterMigrationNoAccountIsLeftWithAPlainTextPassword() throws Exception {
        requireTestDatabase();
        userRepository.saveAndFlush(
                newUser(uniqueEmail("leftover"), "leftoverpassword1234", "RPS_STAFF", "APPROVED"));

        passwordMigrationService.migratePlainTextPasswords();

        // The operator-facing verification, run as SQL so it inspects the column rather
        // than the entities in the persistence context.
        userRepository.flush();
        Integer plainText = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE password IS NOT NULL AND password <> '' "
                        + "AND password NOT LIKE '$2a$%' AND password NOT LIKE '$2b$%' "
                        + "AND password NOT LIKE '$2y$%'",
                Integer.class);

        assertEquals(0, plainText,
                "no account may be left with a readable password after the migration");
    }

    @Test
    void aSuccessfulLoginNeverLeavesAPlainTextPasswordBehind() throws Exception {
        requireTestDatabase();
        String raw = "loginleftover1234";
        String email = uniqueEmail("login-leftover");
        userRepository.saveAndFlush(newUser(email, raw, "RPS_STAFF", "APPROVED"));

        userService.login(email, raw, "RPS_STAFF");

        // The upgrade writes to a managed entity, so flush before inspecting the column.
        userRepository.flush();
        Integer plainText = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ? AND password = ?",
                Integer.class, email, raw);

        assertEquals(0, plainText, "the sign-in must have replaced the plain-text value");
    }
}


