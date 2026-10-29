package com.rpis.backend.service;

import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordService passwordService;

    /**
     * Why an authentication attempt ended the way it did.
     *
     * <p>Exists so the controller can answer exactly as it did before (403 with a
     * specific message for a pending or rejected account, 401 for anything else)
     * while the password is verified in one place only.
     */
    public enum LoginStatus {
        OK, INVALID_CREDENTIALS, PENDING, REJECTED
    }

    /**
     * The result of an authentication attempt.
     *
     * @param user   the authenticated account, or null when it did not authenticate
     * @param status why the attempt ended as it did
     */
    public record LoginOutcome(User user, LoginStatus status) {
        static LoginOutcome refused(LoginStatus status) {
            return new LoginOutcome(null, status);
        }
    }

    /**
     * Whether the fixed set of demonstration accounts may be created. Off by
     * default: these accounts have published passwords, so they must never appear
     * in a real database. {@code application-dev.properties} turns it on for local
     * work, and the seeder additionally refuses to run against anything but the
     * isolated test database when it is triggered through the API.
     */
    @Value("${rpis.seed.default-users.enabled:false}")
    private boolean defaultUsersSeedingEnabled;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public java.util.Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getPendingUsers() {
        return userRepository.findByStatus("PENDING");
    }

    public User createUser(User user) {

        if (!user.getEmail().toLowerCase().endsWith("@msunaawan.edu.ph")) {
            throw new IllegalArgumentException(
                    "Only MSU Naawan email addresses are allowed.");
        }

        // The password arrives as plain text over the wire (as it must, to be usable)
        // and is hashed here, so nothing but a BCrypt hash is ever stored.
        user.setPassword(passwordService.hash(user.getPassword()));

        user.setStatus("PENDING");
        user.setDateRegistered(java.time.LocalDate.now());
        user.setEmailVerified(false);

        return userRepository.save(user);
    }

    public User createStaffAccount(User user) {

        if (!user.getEmail().toLowerCase().endsWith("@msunaawan.edu.ph")) {
            throw new IllegalArgumentException(
                    "Only MSU Naawan email addresses are allowed.");
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists.");
        }

        // The Dean role is deliberately NOT created by this general service: it carries an
        // endorsement scope, so it is provisioned by DeanAccountService, which validates
        // it, hashes the password, writes the audit entry and refuses any caller who is
        // not an RPS administrator. UserController delegates the Dean role there, so this
        // guard is the backstop that keeps any other caller of this method from minting a
        // Dean. Everything else keeps working as before.
        if (UserRoles.DEAN.equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException(
                    "The Dean role is provisioned by the Dean account service, which only an "
                            + "RPS administrator may use.");
        }

        user.setPassword(passwordService.hash(user.getPassword()));
        user.setStatus("APPROVED");
        user.setEmailVerified(true);
        user.setDateRegistered(java.time.LocalDate.now());

        return userRepository.save(user);
    }

    public User approveUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus("APPROVED");
        User saved = userRepository.save(user);
        emailService.sendApprovalEmail(saved.getEmail(), saved.getName());
        return saved;
    }

    public User rejectUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus("REJECTED");
        User saved = userRepository.save(user);
        emailService.sendRejectionEmail(saved.getEmail(), saved.getName());
        return saved;
    }

    /**
     * Authenticates an account and reports why an attempt failed.
     *
     * <p>This is the only place in the application that verifies a password. It
     * previously compared plain text, and the controller performed a second plain-text
     * comparison of its own; both are gone.
     *
     * <p>A row whose stored password predates hashing is verified directly and then
     * <strong>upgraded in place</strong>, so an existing account keeps working and
     * stops being plain text from its very next successful sign-in. That is the second
     * of the two migration paths (the first is {@link PasswordMigrationService}), and
     * either one alone is enough to avoid locking anybody out.
     *
     * <p>No branch of this method logs the submitted password, the stored value or the
     * account's existence.
     */
    public LoginOutcome login(String email, String password, String role) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Deliberately indistinguishable from a wrong password: the caller answers
            // 401 for both, so the endpoint cannot be used to discover which e-mail
            // addresses have accounts. The previous version logged "no account for the
            // supplied e-mail address", which leaked exactly that.
            return LoginOutcome.refused(LoginStatus.INVALID_CREDENTIALS);
        }
        if (!user.getEmail().toLowerCase().endsWith("@msunaawan.edu.ph")) {
            return LoginOutcome.refused(LoginStatus.INVALID_CREDENTIALS);
        }

        boolean roleMatches = user.getRole() != null && user.getRole().equalsIgnoreCase(role);
        PasswordService.Verification verification = passwordService.verify(user.getPassword(), password);

        if (!verification.matches() || !roleMatches) {
            return LoginOutcome.refused(LoginStatus.INVALID_CREDENTIALS);
        }

        // Credentials are correct, so the account state can be reported honestly.
        if ("PENDING".equalsIgnoreCase(user.getStatus())) {
            return LoginOutcome.refused(LoginStatus.PENDING);
        }
        if ("REJECTED".equalsIgnoreCase(user.getStatus())) {
            return LoginOutcome.refused(LoginStatus.REJECTED);
        }
        if (!"APPROVED".equalsIgnoreCase(user.getStatus())) {
            return LoginOutcome.refused(LoginStatus.INVALID_CREDENTIALS);
        }

        if (verification.legacy()) {
            // The password was still plain text. Replace it now, before the token is
            // issued, so a successful sign-in always leaves the account hashed.
            upgradeLegacyPassword(user, password);
        }

        return new LoginOutcome(user, LoginStatus.OK);
    }

    /**
     * Replaces a legacy plain-text password with a BCrypt hash.
     *
     * <p>A failure here must not block the sign-in: the credentials were correct, and
     * refusing them would lock the user out of an account they just proved they own.
     * The next sign-in simply tries again.
     */
    private void upgradeLegacyPassword(User user, String rawPassword) {
        try {
            user.setPassword(passwordService.hash(rawPassword));
            userRepository.save(user);
            log.info("Upgraded account {} to a hashed password.", user.getId());
        } catch (Exception e) {
            log.warn("Could not upgrade account {} to a hashed password: {}",
                    user.getId(), e.getMessage());
        }
    }

    /**
     * Creates the fixed set of demonstration accounts.
     *
     * <p>This is <strong>synthetic seed data</strong> with published passwords, so
     * it is opt-in: it runs only when {@code rpis.seed.default-users.enabled} is
     * true (see {@code application-dev.properties}). It previously ran on every
     * start-up of every instance and wrote every existing account back to the
     * database, which is both a startup write storm on a shared database and a way
     * to introduce known-password accounts into production.
     *
     * @throws IllegalStateException when called while disabled
     */
    public void seedUsers() {
        if (!defaultUsersSeedingEnabled) {
            throw new IllegalStateException(
                    "Default demo accounts are disabled (rpis.seed.default-users.enabled=false). "
                            + "Enable it only for local development.");
        }

        createIfNotExist(
                "Proponent User",
                "proponent@msunaawan.edu.ph",
                "proponentpassword1234",
                "PROPONENT");

        createIfNotExist(
                "RPS Staff",
                "rpsstaff@msunaawan.edu.ph",
                "rpsstaffpassword1234",
                "RPS_STAFF");

        createIfNotExist(
                "RPS Admin",
                "rpsadmin@msunaawan.edu.ph",
                "rpsadminpassword1234",
                "RPS_ADMIN");

        createIfNotExist(
                "OVCRIGE User",
                "ovcrige@msunaawan.edu.ph",
                "ovcrigepassword1234",
                "OVCRIGE");

        createIfNotExist(
                "REC User",
                "rec@msunaawan.edu.ph",
                "recpassword1234",
                "REC");

        createIfNotExist(
                "OVCAF User",
                "ovcaf@msunaawan.edu.ph",
                "ovcafpassword1234",
                "OVCAF");

        createIfNotExist(
                "OC User",
                "oc@msunaawan.edu.ph",
                "ocpassword1234",
                "OC");

        // Backfill the bookkeeping fields on accounts that predate them. Only rows
        // that are actually missing a value are written, so a start-up against a
        // populated database performs no writes at all instead of rewriting every
        // account (the previous behaviour).
        List<User> allUsers = userRepository.findAll();
        List<User> needingBackfill = allUsers.stream()
                .filter(u -> u.getStatus() == null
                        || u.getEmailVerified() == null
                        || u.getDateRegistered() == null)
                .toList();

        if (needingBackfill.isEmpty()) {
            log.info("Account bookkeeping is complete; nothing to backfill.");
            return;
        }

        for (User user : needingBackfill) {
            if (user.getStatus() == null) {
                user.setStatus("APPROVED");
            }
            if (user.getEmailVerified() == null) {
                user.setEmailVerified(true);
            }
            if (user.getDateRegistered() == null) {
                user.setDateRegistered(java.time.LocalDate.now());
            }
        }
        userRepository.saveAll(needingBackfill);
        log.info("Backfilled bookkeeping fields on {} account(s).", needingBackfill.size());
    }

    public java.util.Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User updateUser(Long id, User updatedUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (updatedUser.getName() != null && !updatedUser.getName().trim().isEmpty()) {
            user.setName(updatedUser.getName());
        }
        if (updatedUser.getEmail() != null && !updatedUser.getEmail().trim().isEmpty()) {
            user.setEmail(updatedUser.getEmail());
        }
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().trim().isEmpty()) {
            // A password change always stores a fresh hash. The raw value is never
            // written to the column, and it is not logged.
            user.setPassword(passwordService.hash(updatedUser.getPassword()));
        }
        if (updatedUser.getDepartmentOffice() != null) {
            user.setDepartmentOffice(updatedUser.getDepartmentOffice());
        }
        if (updatedUser.getContactNumber() != null) {
            user.setContactNumber(updatedUser.getContactNumber());
        }
        if (updatedUser.getSpecialization() != null) {
            user.setSpecialization(updatedUser.getSpecialization());
        }
        // Notification Routing preferences (Manage Profile -> Notifications). Null
        // means "this client does not manage them" and leaves the stored selection
        // untouched; an empty list is a real value (the user cleared every box).
        if (updatedUser.getNotifyProposals() != null) {
            user.setNotifyProposals(updatedUser.getNotifyProposals());
        }
        if (updatedUser.getNotifyDeadlines() != null) {
            user.setNotifyDeadlines(updatedUser.getNotifyDeadlines());
        }

        return userRepository.save(user);
    }

    private void createIfNotExist(String name, String email, String password, String role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            // The demo accounts' published passwords are hashed like any other, so even
            // the development seed data never puts a readable password in the column.
            user.setPassword(passwordService.hash(password));
            user.setRole(role);
            user.setStatus("APPROVED");
            userRepository.save(user);
        }
    }
}
