package com.rpis.backend.controller;

import com.rpis.backend.dto.ProposalAnnouncementRequest;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.service.ProposalAnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/proposal-announcements")
@RequiredArgsConstructor
public class ProposalAnnouncementController {

    private final ProposalAnnouncementService announcementService;

    // ==================== READ ====================

    @GetMapping
    public ResponseEntity<List<ProposalAnnouncement>> getAllAnnouncements() {
        return ResponseEntity.ok(announcementService.getAllAnnouncements());
    }

    /**
     * Proponent pre-check gatekeeper endpoint: returns the announcement currently
     * accepting submissions (status ACTIVE and today within its date range).
     * Responds 404 when no announcement is active.
     */
    @GetMapping("/active")
    public ResponseEntity<ProposalAnnouncement> getActiveAnnouncement() {
        return ResponseEntity.ok(announcementService.getActiveAnnouncement());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProposalAnnouncement> getAnnouncementById(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.getAnnouncementById(id));
    }

    // ==================== CREATE / UPDATE / DELETE ====================

    @PostMapping
    public ResponseEntity<ProposalAnnouncement> createAnnouncement(@RequestBody ProposalAnnouncementRequest request) {
        ProposalAnnouncement created = announcementService.createAnnouncement(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProposalAnnouncement> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody ProposalAnnouncementRequest request) {
        return ResponseEntity.ok(announcementService.updateAnnouncement(id, request));
    }

    /**
     * Explicit activation action: sets the announcement to ACTIVE and notifies all
     * proponents.
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<ProposalAnnouncement> activateAnnouncement(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.activateAnnouncement(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== ERROR MAPPING ====================

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }
}
