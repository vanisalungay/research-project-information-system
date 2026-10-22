package com.rpis.backend.controller;

import com.rpis.backend.model.User;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.service.UserService;
import com.rpis.backend.service.UserSignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final CurrentUser currentUser;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {

        List<User> users = userService.getAllUsers();

        // Hide passwords before returning response
        users.forEach(user -> user.setPassword(null));

        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {

        User createdUser = userService.createUser(user);

        // Hide password in response
        createdUser.setPassword(null);

        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
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

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody com.rpis.backend.dto.LoginRequest loginRequest) {

        java.util.Optional<User> existingUserOpt = userService.findByEmail(loginRequest.getEmail());
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.getPassword().equals(loginRequest.getPassword()) &&
                    existingUser.getRole().equalsIgnoreCase(loginRequest.getRole())) {

                if (existingUser.getStatus().equalsIgnoreCase("PENDING")) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Your account is pending RII ADMIN approval.");
                } else if (existingUser.getStatus().equalsIgnoreCase("REJECTED")) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Your account registration was rejected.");
                }
            }
        }

        User user = userService.login(
                loginRequest.getEmail(),
                loginRequest.getPassword(),
                loginRequest.getRole());

        if (user != null) {

            // Generate and set token
            String token = tokenService.generateToken(user.getId(), user.getRole());
            user.setToken(token);

            // Hide password before sending response
            user.setPassword(null);

            return ResponseEntity.ok(user);

        } else {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email, password, or role.");
        }
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

                if (!email.endsWith("@msunaawan.edu.ph")) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Only @msunaawan.edu.ph accounts are allowed.");
                }

                java.util.Optional<User> existingUserOpt = userService.findByEmail(email);
                if (existingUserOpt.isPresent()) {
                    User existingUser = existingUserOpt.get();
                    if (!existingUser.getRole().equalsIgnoreCase(request.getRole())) {
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

                    existingUser.setPassword(null);
                    
                    // Generate and set token
                    String token = tokenService.generateToken(existingUser.getId(), existingUser.getRole());
                    existingUser.setToken(token);
                    
                    return ResponseEntity.ok(existingUser);
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

    @PostMapping("/seed")
    public ResponseEntity<String> seedUsers() {

        userService.seedUsers();

        return ResponseEntity.ok(
                "Database seeded with temporary user accounts.");
    }

    @PostMapping("/admin/create")
    public ResponseEntity<?> createStaffAccount(
            @RequestBody User user) {
        try {
            User createdUser = userService.createStaffAccount(user);
            createdUser.setPassword(null);
            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> {
                    user.setPassword(null); // Hide password for safety
                    return ResponseEntity.ok(user);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            User updated = userService.updateUser(id, user);
            updated.setPassword(null); // Hide password for safety
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
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