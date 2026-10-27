package com.rpis.backend.controller;

import com.rpis.backend.dto.ProposalAnnouncementRequest;
import com.rpis.backend.dto.ProposalAnnouncementUpdateRequest;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.security.CurrentUser;
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
    private final CurrentUser currentUser;

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

    /**
     * The Calls for Proposal a proponent can currently respond to (status ACTIVE and
     * today inside the submission window). An empty list means there is nothing open
     * right now - the Create Proposal workflow shows its empty state in that case.
     * <p>
     * Drafts, scheduled, closed and past-deadline calls are never returned, so the
     * selection page cannot offer a call that is not genuinely open.
     */
    @GetMapping("/open")
    public ResponseEntity<List<ProposalAnnouncement>> getOpenAnnouncements() {
        return ResponseEntity.ok(announcementService.getOpenAnnouncements());
    }

    /**
     * The Calls for Proposal the first step of the Create Proposal workflow shows: the
     * published calls that are open for submissions right now, followed by the
     * published calls that open later (scheduled), ordered by their opening date.
     * <p>
     * Only status ACTIVE calls are included, and a call whose deadline has passed is
     * never returned: drafts, unpublished, cancelled/closed and expired calls stay
     * hidden. The response is empty when there is nothing open or upcoming.
     */
    @GetMapping("/available")
    public ResponseEntity<List<ProposalAnnouncement>> getAvailableAnnouncements() {
        return ResponseEntity.ok(announcementService.getAvailableAnnouncements());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProposalAnnouncement> getAnnouncementById(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.getAnnouncementById(id));
    }

    /**
     * Proponent-facing Call for Proposals details, opened from an announcement
     * notification. Read-only; draft announcements are reported as not found.
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<ProposalAnnouncement> getAnnouncementDetails(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.getAnnouncementForProponents(id));
    }

    // ==================== CREATE / UPDATE / DELETE ====================

    @PostMapping
    public ResponseEntity<ProposalAnnouncement> createAnnouncement(@RequestBody ProposalAnnouncementRequest request) {
        // The authenticated administrator is recorded on the update history when the call
        // is created already published.
        ProposalAnnouncement created =
                announcementService.createAnnouncement(request, currentUser.getUserId());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Updates an existing Call for Proposal. Only the submission window (start and end
     * date) and the status can be changed - see
     * {@link com.rpis.backend.dto.ProposalAnnouncementUpdateRequest}, which is the
     * allow-list of the update: the name, priority agenda, announcement reminders and
     * additional announcement are not part of the request and keep their stored values.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProposalAnnouncement> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody ProposalAnnouncementUpdateRequest request) {
        return ResponseEntity.ok(
                announcementService.updateAnnouncement(id, request, currentUser.getUserId()));
    }

    /**
     * Explicit activation action: sets the announcement to ACTIVE and notifies all
     * proponents.
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<ProposalAnnouncement> activateAnnouncement(@PathVariable Long id) {
        return ResponseEntity.ok(
                announcementService.activateAnnouncement(id, currentUser.getUserId()));
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
