package com.rpis.backend.controller;

import com.rpis.backend.model.Notification;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(
            @RequestParam(required = false) Long userId) {
        // Identity is derived from the authenticated principal; the query param is
        // never trusted (prevents reading another user's notifications).
        return ResponseEntity.ok(notificationService.getNotificationsForUser(currentUser.requireUserId()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getNotificationsForUser(@PathVariable Long userId) {
        currentUser.requireSelf(userId);
        return ResponseEntity.ok(notificationService.getNotificationsForUser(userId));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsReadForCurrentUser(
                id, currentUser.requireUserId(), currentUser.isProponent()));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestParam(required = false) Long userId) {
        notificationService.markAllAsRead(currentUser.requireUserId());
        return ResponseEntity.ok().build();
    }
}
