package com.rpis.backend.controller;

import com.rpis.backend.config.NotificationEmitterRegistry;
import com.rpis.backend.model.Notification;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.service.NotificationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationEmitterRegistry emitterRegistry;
    private final CurrentUser currentUser;

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(
            @RequestParam(required = false) Long userId) {
        // Identity is derived from the authenticated principal; the query param is
        // never trusted (prevents reading another user's notifications).
        return ResponseEntity.ok(notificationService.getNotificationsForUser(
                currentUser.requireUserId(), currentUser.getRole()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getNotificationsForUser(@PathVariable Long userId) {
        currentUser.requireSelf(userId);
        return ResponseEntity.ok(notificationService.getNotificationsForUser(
                userId, currentUser.getRole()));
    }

    /**
     * The live notification stream. The client authenticates with the ordinary Bearer
     * token (it reads the stream through fetch, because the browser's EventSource cannot
     * set an Authorization header).
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletResponse response) {
        if (!emitterRegistry.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Real-time notifications are disabled.");
        }
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        return emitterRegistry.register(currentUser.requireUserId());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsReadForCurrentUser(
                id, currentUser.requireUserId(), currentUser.getRole()));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestParam(required = false) Long userId) {
        notificationService.markAllAsRead(currentUser.requireUserId(), currentUser.getRole());
        return ResponseEntity.ok().build();
    }
}
