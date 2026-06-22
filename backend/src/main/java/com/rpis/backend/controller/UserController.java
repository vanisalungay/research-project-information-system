package com.rpis.backend.controller;

import com.rpis.backend.model.User;
import com.rpis.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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

            // Hide password before sending response
            user.setPassword(null);

            return ResponseEntity.ok(user);

        } else {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email, password, or role.");
        }
    }

    @PostMapping("/seed")
    public ResponseEntity<String> seedUsers() {

        userService.seedUsers();

        return ResponseEntity.ok(
                "Database seeded with temporary user accounts.");
    }
}