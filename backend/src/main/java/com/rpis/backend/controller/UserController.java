package com.rpis.backend.controller;

import com.rpis.backend.dto.DeanAccountRequest;
import com.rpis.backend.dto.DeanAssignmentResponse;
import com.rpis.backend.dto.DeanReplacementCreateRequest;
import com.rpis.backend.dto.DeanReplacementRequest;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.service.DeanAccountService;
import com.rpis.backend.service.UserService;
import com.rpis.backend.service.UserSignatureService;
import com.rpis.backend.util.EmailNormalizer;
import com.rpis.backend.util.PersonNameFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.rpis.backend.dto.GoogleLoginRequest;
import com.rpis.backend.service.TokenService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;
    private final UserSignatureService signatureService;
    private final DeanAccountService deanAccountService;
    private final CurrentUser currentUser;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {

        List<User> users = userService.getAllUsers();
        // The Dean's appointment status comes from the same appointment table Dean Role
        // Management reads, so the two screens report the same Active / Revoked state.
        deanAccountService.attachDeanStatuses(users);

        // Hide passwords before returning response
        users.forEach(user -> user.setPassword(null));

        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);

            // Hide password in response
            createdUser.setPassword(null);

            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            // A rejected registration - a weak password, a non-institutional address or a
            // duplicate account - is a client error, not a server failure. The message is
            // the one the registration form shows; it never contains the password.
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<List<User>> getPendingUsers() {
        List<User> users = userService.getPendingUsers();
        // Hide passwords before returning response
        users.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<User> approveUser(@PathVariable Long id) {
        User user = userService.approveUser(id);
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<User> rejectUser(@PathVariable Long id) {
        User user = userService.rejectUser(id);
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    /**
     * Authenticates an account and issues a bearer token.
     *
     * <p>The password is verified in exactly one place
     * ({@link UserService#login}). This method previously performed its own plain-text
     * comparison of the stored and submitted passwords before delegating, so there were
     * two comparison sites to keep correct - and both compared raw values.
     *
     * <p>The HTTP answers are unchanged: a pending or rejected account whose
     * credentials are correct still gets 403 with the same message it always did, and
     * everything else gets 401. A wrong password and an unknown e-mail address are
     * deliberately indistinguishable, so this endpoint cannot be used to enumerate
     * accounts.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody com.rpis.backend.dto.LoginRequest loginRequest) {

        UserService.LoginOutcome outcome = userService.login(
                loginRequest.getEmail(),
                loginRequest.getPassword(),
                loginRequest.getRole());

        if (outcome.status() == UserService.LoginStatus.PENDING) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Your account is pending RII ADMIN approval.");
        }
        if (outcome.status() == UserService.LoginStatus.REJECTED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Your account registration was rejected.");
        }

        User user = outcome.user();
        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email, password, or role.");
        }

        // The token carries the role the user selected. An account that holds several roles
        // gets a session for that one role, so switching roles on the sign-in page never
        // carries the other role's authority into the session.
        String selectedRole = EmailNormalizer.normalizeRole(loginRequest.getRole());
        User session = sessionView(user, selectedRole);
        // The account's current version is embedded so a later sign-out (which increments
        // it) invalidates this token everywhere.
        String token = tokenService.generateToken(
                user.getId(), selectedRole, user.getTokenVersion());
        session.setToken(token);

        return ResponseEntity.ok(session);
    }

    /**
     * The account as the browser should see this session: the same real account data, with
     * {@code role} set to the role selected at sign-in. The stored account keeps its
     * primary role, so a Proponent who is also a Dean signs in as whichever role was
     * selected and still appears in both role-specific lists. The stored password hash is
     * never copied to the response.
     */
    private static User sessionView(User account, String selectedRole) {
        User view = new User();
        view.setId(account.getId());
        view.setName(account.getName());
        view.setEmail(account.getEmail());
        view.setRole(selectedRole);
        view.setStatus(account.getStatus());
        view.setDepartmentOffice(account.getDepartmentOffice());
        view.setDateRegistered(account.getDateRegistered());
        view.setEmailVerified(account.getEmailVerified());
        view.setMustChangePassword(account.getMustChangePassword());
        view.setContactNumber(account.getContactNumber());
        view.setSpecialization(account.getSpecialization());
        view.setDesignation(account.getDesignation());
        view.setAssignedCollege(account.getAssignedCollege());
        view.setNotifyProposals(account.getNotifyProposals() == null
                ? new java.util.ArrayList<>()
                : new java.util.ArrayList<>(account.getNotifyProposals()));
        view.setNotifyDeadlines(account.getNotifyDeadlines() == null
                ? new java.util.ArrayList<>()
                : new java.util.ArrayList<>(account.getNotifyDeadlines()));
        view.setNotifyPreferencesConfigured(account.getNotifyPreferencesConfigured());
        view.setSignatureFileName(account.getSignatureFileName());
        // The role assignments travel with the session so the client can show every role
        // the account holds (the role selected for this token is the primary one here).
        for (String role : account.getRoles()) {
            view.assignRole(role);
        }
        view.setPassword(null);
        return view;
    }

    /**
     * Ends the current session.
     *
     * <p>The bearer token itself stays valid until it expires; what changes is the
     * account's token version, which every instance reads from the shared database.
     * Incrementing it makes the presented token - and every other token already
     * issued to this account - stop authenticating, so it cannot be replayed after
     * logout.
     *
     * <p>The response has no body: the client discards its stored session whether or
     * not this call succeeded, and repeating it is harmless.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        userService.revokeTokens(currentUser.requireUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request) {
        try {
            NetHttpTransport transport = new NetHttpTransport();
            GsonFactory jsonFactory = new GsonFactory();

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .setAudience(Collections.singletonList("YOUR_GOOGLE_CLIENT_ID"))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getToken());
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();

                if (!com.rpis.backend.service.InstitutionalCredentialsService.isInstitutionalAddress(email)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Only @msunaawan.edu.ph accounts are allowed.");
                }

                java.util.Optional<User> existingUserOpt = userService.findByEmail(email);
                String selectedRole = EmailNormalizer.normalizeRole(request.getRole());
                if (existingUserOpt.isPresent()) {
                    User existingUser = existingUserOpt.get();
                    if (selectedRole == null || !existingUser.hasRole(selectedRole)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body("Role mismatch for this user.");
                    }
                    if (existingUser.getStatus().equalsIgnoreCase("PENDING")) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body("Your account is pending admin approval.");
                    } else if (existingUser.getStatus().equalsIgnoreCase("REJECTED")) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body("Your account registration was rejected.");
                    }

                    User session = sessionView(existingUser, selectedRole);
                    // Generate and set token, bound to the account's current version.
                    String token = tokenService.generateToken(existingUser.getId(),
                            selectedRole, existingUser.getTokenVersion());
                    session.setToken(token);

                    return ResponseEntity.ok(session);
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
                }
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Google token.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Google Authentication Failed: " + e.getMessage());
        }
    }

    // The POST /seed route moved to DevSeedController: it is now opt-in, restricted
    // to RPS_ADMIN and refuses to write unless the target is the isolated test
    // database.

    /**
     * Creates an account with the role the administrator selected.
     *
     * <p>The Dean role is the single exception: it carries an endorsement scope, so it is
     * provisioned by {@link DeanAccountService} - the one implementation of the Dean
     * rules - which validates the payload, hashes the initial password, records the
     * account as approved and writes the assignment to the audit trail. That service also
     * refuses any caller who is not an RPS administrator, so sharing this route with the
     * Dean role does not let an RPS Staff account mint one. Every other role keeps going
     * through the general service, unchanged.
     */
    /**
     * Changes the signed-in account's password.
     *
     * <p>The account is taken from the authenticated principal, never from the body, so one
     * account can never change another's password. Nothing password-shaped is ever returned:
     * only a message.
     */
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody com.rpis.backend.dto.ChangePasswordRequest request) {
        try {
            userService.changePassword(
                    currentUser.requireUserId(),
                    request.getCurrentPassword(),
                    request.getNewPassword(),
                    request.getConfirmPassword());
            return ResponseEntity.ok(Map.of("message", "Your password has been changed."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/admin/create")
    public ResponseEntity<?> createStaffAccount(
            @RequestBody User user) {
        try {
            if (isDeanRole(user)) {
                return createDeanAccount(user);
            }
            // The address and the temporary password are generated on the server; the
            // credentials come back once, here, and nowhere else.
            com.rpis.backend.dto.CreatedAccountResponse created =
                    userService.createStaffAccountWithGeneratedCredentials(user);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** True when the submitted payload asks for the Dean role. */
    private static boolean isDeanRole(User user) {
        return user != null && UserRoles.DEAN.equalsIgnoreCase(user.getRole());
    }

    /**
     * Provisions a Dean through the existing Dean account service, so the standard Create
     * Account form and the Dean-specific validation, password hashing, college scope and
     * audit trail can never drift apart. No {@code userId} is set, so this always creates
     * a new account and never promotes an existing one by accident; the service itself
     * enforces that only an RPS administrator may reach it (403 otherwise).
     */
    private ResponseEntity<?> createDeanAccount(User user) {
        // The Dean keeps its own validation, hashing, college scope and audit trail; only the
        // credentials are generated here, the same way they are for every other role.
        if (user.getName() == null || user.getName().isBlank()) {
            throw new IllegalArgumentException("A full name is required.");
        }
        // Format first: the email and temporary password below are derived from this name.
        user.setName(PersonNameFormatter.format(user.getName()));
        // The Dean's own validation still runs on what the administrator sent, so a foreign
        // domain is refused with the reason the form always showed. The address the account is
        // created with is still the generated institutional one.
        if (user.getEmail() != null && !user.getEmail().isBlank()
                && !com.rpis.backend.service.InstitutionalCredentialsService
                        .isInstitutionalAddress(user.getEmail())) {
            throw new IllegalArgumentException("Only MSU Naawan email addresses are allowed.");
        }

        DeanAccountRequest request = new DeanAccountRequest();
        request.setName(user.getName());
        request.setDesignation(user.getDesignation());
        request.setAssignedCollege(user.getAssignedCollege());
        // The service generates the address and temporary password, or reuses the account
        // that already owns the person's institutional address instead of duplicating it.
        DeanAccountService.ProvisionedDean provisioned =
                deanAccountService.save(request, currentUser.requireUserId(), currentUser.getRole());
        User flagged = provisioned.reused()
                ? provisioned.user()
                : userService.requirePasswordChange(provisioned.user());
        return new ResponseEntity<>(
                com.rpis.backend.dto.CreatedAccountResponse.of(flagged, provisioned.email(),
                        provisioned.temporaryPassword(), provisioned.emailAdjusted(),
                        provisioned.reused()),
                HttpStatus.CREATED);
    }

    /**
     * Reads one account.
     *
     * <p>Data isolation: a caller who is not one of the privileged review/administrative
     * roles may only read their own account. The route used to be reachable by any
     * authenticated user with any id, so one account could enumerate every other
     * account's profile. The password is never serialized ({@code @JsonProperty(WRITE_ONLY)}).
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        currentUser.requireSelfOrPrivileged(id);
        return userService.getUserById(id)
                .map(user -> {
                    user.setPassword(null); // Hide password for safety
                    return ResponseEntity.ok(user);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== Dean / Department Chair accounts (RPS_ADMIN only) =====
    // The Dean is an ordinary RPIS account with the Dean role, so these routes extend
    // the existing account management instead of introducing a second account system.
    // The standard Create Account route (/admin/create) also accepts the Dean role, but
    // it delegates to DeanAccountService rather than provisioning a Dean itself, so there
    // is exactly one implementation of the Dean rules. SecurityConfig restricts these
    // routes to RPS_ADMIN and the service re-checks the role, so the Dean role can never
    // be granted by a caller who is not an administrator.

    /** The Dean accounts, for the administrator's list. */
    @GetMapping("/deans")
    public ResponseEntity<List<User>> getDeans() {
        List<User> deans = deanAccountService.listDeans();
        deans.forEach(dean -> dean.setPassword(null));
        return ResponseEntity.ok(deans);
    }

    /**
     * Creates a Dean account, or assigns the Dean role (with its designation and
     * college scope) to an existing account when {@code userId} is supplied.
     */
    @PostMapping("/deans")
    public ResponseEntity<?> saveDean(@RequestBody DeanAccountRequest request) {
        try {
            User dean = deanAccountService.save(request, currentUser.requireUserId(),
                    currentUser.getRole()).user();
            dean.setPassword(null);
            return new ResponseEntity<>(dean, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Revokes the Dean role; the account keeps working as a Faculty Proponent.
     *
     * <p>A Dean appointed to a college is refused here on purpose: the sitting Dean owns
     * the college's pending endorsement requests, so the administrator must use Replace
     * Dean instead. The reason is returned as a readable 400, which the Manage Dean modal
     * shows when a direct request bypasses its disabled Revoke button.</p>
     */
    @DeleteMapping("/deans/{id}")
    public ResponseEntity<?> revokeDean(@PathVariable Long id) {
        try {
            // `User.password` is WRITE_ONLY, so the hash is never serialized. Do not null
            // it after the service call: with an open EntityManager (and inside a
            // surrounding transaction) that marks the managed row dirty and the flush then
            // fails the not-null constraint, which is what made the Revoke action fail.
            return ResponseEntity.ok(deanAccountService.revoke(id, currentUser.requireUserId(),
                    currentUser.getRole()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * The Dean appointment history the Dean Role Management page shows: current and former
     * appointments alike, so a change of leadership is readable in one list.
     *
     * @param status {@code ACTIVE} (default), {@code REVOKED} or {@code ALL}
     */
    @GetMapping("/deans/assignments")
    public ResponseEntity<List<DeanAssignmentResponse>> getDeanAssignments(
            @RequestParam(value = "status", required = false, defaultValue = "ACTIVE") String status) {
        return ResponseEntity.ok(deanAccountService.listAssignments(status));
    }

    /** The accounts eligible to be appointed Dean - the replacement candidates. */
    @GetMapping("/deans/candidates")
    public ResponseEntity<List<User>> getDeanCandidates(
            @RequestParam(value = "college", required = false) String college) {
        // The password is never serialized (@JsonProperty WRITE_ONLY), so nothing is stripped;
        // clearing it on a managed entity would only dirty the row.
        return ResponseEntity.ok(deanAccountService.listCandidates(college));
    }

    /**
     * Replaces the sitting Dean of a college (or the institution-wide fallback) with a new
     * appointment. The previous Dean's appointment is closed as REVOKED and kept as
     * history; the incoming Dean's account is given the Dean role and a new ACTIVE
     * appointment, all in one transaction. Only an RPS administrator reaches this route.
     */
    @PostMapping("/deans/replace")
    public ResponseEntity<?> replaceDean(@RequestBody DeanReplacementRequest request) {
        try {
            DeanAssignmentResponse assignment = deanAccountService.replaceDean(request,
                    currentUser.requireUserId(), currentUser.getRole());
            return ResponseEntity.ok(assignment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Creates the incoming Dean of an existing ACTIVE appointment and completes the
     * replacement in one transaction (Dean Role Management -> Manage Dean -> Replace Dean).
     *
     * <p>The account is provisioned exactly like any other administrator-created account:
     * the institutional address and temporary password are generated here and returned once,
     * and the payload never chooses either. The college comes from the appointment being
     * replaced; a request that names a different college is refused by the service. Only an
     * RPS administrator reaches this route (SecurityConfig and the service).
     */
    @PostMapping("/deans/replacement")
    public ResponseEntity<?> createReplacementDean(
            @RequestBody DeanReplacementCreateRequest request) {
        try {
            String name = request != null ? request.getName() : null;
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("A full name is required.");
            }
            request.setName(PersonNameFormatter.format(name));
            DeanAccountService.ProvisionedDean provisioned =
                    deanAccountService.createReplacementDean(request,
                            currentUser.requireUserId(), currentUser.getRole());
            User flagged = provisioned.reused()
                    ? provisioned.user()
                    : userService.requirePasswordChange(provisioned.user());
            return new ResponseEntity<>(
                    com.rpis.backend.dto.CreatedAccountResponse.of(flagged, provisioned.email(),
                            provisioned.temporaryPassword(), provisioned.emailAdjusted(),
                            provisioned.reused()),
                    HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Updates an account.
     *
     * <p>Data isolation: a caller who is not one of the privileged review/administrative
     * roles may only update their own account, so a Proponent can no longer rewrite
     * another account's name, e-mail or password by changing the id in the URL. The
     * administrative account-management screen keeps working because the
     * administrative roles are privileged.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        currentUser.requireSelfOrPrivileged(id);
        try {
            User updated = userService.updateUser(id, user);
            updated.setPassword(null); // Hide password for safety
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            if ("User not found".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            // A password supplied here (the administrator's reset) is held to the shared
            // policy; the reason is a client error the reset modal can display.
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ===== Electronic signature (Manage Profile -> Electronic Signature) =====
    // Every route is scoped to the authenticated caller: the user id is resolved
    // from the bearer token (CurrentUser), never from the request, so one account
    // can never read, replace or delete another account's signature.

    /** Saves (or replaces) the caller's electronic signature. */
    @PostMapping("/me/signature")
    public ResponseEntity<?> saveMySignature(
            @RequestParam(value = "signature", required = false) MultipartFile signature) {
        try {
            User saved = signatureService.saveSignature(currentUser.requireUserId(), signature);
            return ResponseEntity.ok(Map.of(
                    "hasSignature", true,
                    "signatureFileName", saved.getSignatureFileName() == null
                            ? ""
                            : saved.getSignatureFileName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** Serves the caller's own signature image; 404 when none is saved yet. */
    @GetMapping("/me/signature")
    public ResponseEntity<Resource> getMySignature() {
        UserSignatureService.SignatureFile file =
                signatureService.loadSignature(currentUser.requireUserId());
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                // A replaced signature must never be served from a stale cache.
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.fileName() + "\"")
                .contentType(file.contentType())
                .body(file.resource());
    }

    /** Removes the caller's saved signature; the account keeps working without it. */
    @DeleteMapping("/me/signature")
    public ResponseEntity<?> deleteMySignature() {
        signatureService.deleteSignature(currentUser.requireUserId());
        return ResponseEntity.ok(Map.of("hasSignature", false));
    }
}