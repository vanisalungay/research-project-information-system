package com.rpis.backend.service;

import com.rpis.backend.model.ActivityLog;
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
    private final InstitutionalCredentialsService credentialsService;
    private final ActivityLogService activityLogService;

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

        if (!InstitutionalCredentialsService.isInstitutionalAddress(user.getEmail())) {
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

    /**
     * Creates an account from the administrator's input - the person's name and role, plus the
     * Dean's own fields where they apply. The institutional address and a random temporary
     * password are generated here, on the server, so the browser never chooses them, and the
     * credentials are handed back exactly once for the administrator to pass on.
     */
    public com.rpis.backend.dto.CreatedAccountResponse createStaffAccountWithGeneratedCredentials(User user) {
        if (user == null || user.getName() == null || user.getName().isBlank()) {
            throw new IllegalArgumentException("A full name is required.");
        }

        String email = credentialsService.availableEmailFor(user.getName());
        boolean adjusted = !email.equalsIgnoreCase(
                InstitutionalCredentialsService.emailFor(user.getName()));
        String temporaryPassword = InstitutionalCredentialsService.temporaryPasswordFor(user.getName());

        user.setEmail(email);
        user.setPassword(temporaryPassword);
        user.setMustChangePassword(true);

        User saved = createStaffAccount(user);
        activityLogService.success(ActivityLog.ACTION_ACCOUNT_CREATED,
                ActivityLog.MODULE_USER_ACCOUNTS, "Created a new user account",
                saved.getName() + " (" + saved.getEmail() + ") was created with the role "
                        + saved.getRole() + ".");
        return com.rpis.backend.dto.CreatedAccountResponse.of(saved, email, temporaryPassword, adjusted);
    }

    /**
     * Changes the password of the signed-in account.
     *
     * <p>The current password is verified first, so a stolen session cannot be used to lock the
     * owner out of their own account, and the new one is validated and hashed with the same
     * BCrypt encoder every other password goes through. Only once the new hash is stored is the
     * must-change flag cleared, so an account is never left able to skip the change.
     *
     * @return the account, with its stored hash removed by the caller
     */
    public User changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Your current password is required.");
        }
        if (!passwordService.verify(user.getPassword(), currentPassword).matches()) {
            throw new IllegalArgumentException("Your current password is incorrect.");
        }

        validateNewPassword(newPassword, confirmPassword, currentPassword);

        user.setPassword(passwordService.hash(newPassword));
        user.setMustChangePassword(false);
        User saved = userRepository.save(user);
        // Only the fact that the password changed is recorded, never the password itself.
        activityLogService.success(ActivityLog.ACTION_PASSWORD_CHANGED,
                ActivityLog.MODULE_PROFILE, "Changed administrator password",
                "The password of " + saved.getName() + " (" + saved.getEmail() + ") was changed.");
        return saved;
    }

    /**
     * The password policy a new password must satisfy: long enough, mixed case, a digit and a
     * symbol, different from the current one, and typed the same way twice.
     */
    static void validateNewPassword(String newPassword, String confirmPassword, String currentPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("The new password must be at least 8 characters long.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("The new password and its confirmation do not match.");
        }
        if (newPassword.equals(currentPassword)) {
            throw new IllegalArgumentException("The new password must be different from the current one.");
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean symbol = false;
        for (char c : newPassword.toCharArray()) {
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isLowerCase(c)) lower = true;
            else if (Character.isDigit(c)) digit = true;
            else symbol = true;
        }
        if (!upper || !lower || !digit || !symbol) {
            throw new IllegalArgumentException("The new password must include an upper-case letter, a lower-case letter, a digit and a symbol.");
        }
    }

    /** Marks an account as holding a temporary password, so it must change it at first login. */
    public User requirePasswordChange(User user) {
        user.setMustChangePassword(true);
        return userRepository.save(user);
    }

    public User createStaffAccount(User user) {

        if (!InstitutionalCredentialsService.isInstitutionalAddress(user.getEmail())) {
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
        activityLogService.success(ActivityLog.ACTION_ACCOUNT_APPROVED,
                ActivityLog.MODULE_USER_ACCOUNTS, "Approved a user account",
                saved.getName() + " (" + saved.getEmail() + ") was approved and can sign in.");
        return saved;
    }

    public User rejectUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus("REJECTED");
        User saved = userRepository.save(user);
        emailService.sendRejectionEmail(saved.getEmail(), saved.getName());
        activityLogService.success(ActivityLog.ACTION_ACCOUNT_REJECTED,
                ActivityLog.MODULE_USER_ACCOUNTS, "Rejected a user account",
                saved.getName() + " (" + saved.getEmail() + ") was rejected.");
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
        if (!InstitutionalCredentialsService.isInstitutionalAddress(user.getEmail())) {
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

        User saved = userRepository.save(user);
        boolean ownProfile = saved.getId() != null
                && saved.getId().equals(activityLogService.currentActorId());
        activityLogService.success(
                ownProfile ? ActivityLog.ACTION_PROFILE_UPDATED : ActivityLog.ACTION_ACCOUNT_UPDATED,
                ownProfile ? ActivityLog.MODULE_PROFILE : ActivityLog.MODULE_USER_ACCOUNTS,
                ownProfile ? "Changed administrator profile information"
                        : "Updated an existing user account",
                saved.getName() + " (" + saved.getEmail() + ") was updated with the role "
                        + saved.getRole() + ".");
        return saved;
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
