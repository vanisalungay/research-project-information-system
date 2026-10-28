package com.rpis.backend.controller;

import com.rpis.backend.dto.EndorsementDecisionRequest;
import com.rpis.backend.dto.EndorsementEventResponse;
import com.rpis.backend.dto.EndorsementLetterSettingsRequest;
import com.rpis.backend.dto.EndorsementLetterSettingsResponse;
import com.rpis.backend.dto.EndorsementLinkRequest;
import com.rpis.backend.dto.EndorsementRequestCreateRequest;
import com.rpis.backend.dto.EndorsementRequestResponse;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.service.EndorsementLetterSettingsService;
import com.rpis.backend.service.EndorsementRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * The Dean endorsement workflow endpoints.
 *
 * <p>Route-level authorization is declared in {@code SecurityConfig} and the business
 * rules - ownership, Dean scope, valid status transitions, template availability -
 * are enforced in {@link EndorsementRequestService}, so a hand-crafted request cannot
 * bypass what the UI shows.
 */
@RestController
@RequestMapping("/api/endorsements")
@RequiredArgsConstructor
public class EndorsementController {

    private final EndorsementRequestService endorsementService;
    private final EndorsementLetterSettingsService settingsService;
    private final CurrentUser currentUser;

    // ==================== PROPONENT ====================

    /** Raises a Request for Endorsement for the authenticated Proponent. */
    @PostMapping
    public ResponseEntity<EndorsementRequestResponse> create(
            @RequestBody EndorsementRequestCreateRequest request) {
        return new ResponseEntity<>(endorsementService.create(request), HttpStatus.CREATED);
    }

    /**
     * The requests the authenticated caller may see: their own for a Proponent, the
     * requests inside their scope for a Dean, and the master list for RPS staff /
     * administrator. An optional {@code status} filter (PENDING, APPROVED, REJECTED)
     * narrows the list.
     */
    @GetMapping
    public ResponseEntity<List<EndorsementRequestResponse>> list(
            @RequestParam(required = false) String status) {
        if (UserRoles.PROPONENT.equalsIgnoreCase(currentUser.getRole())) {
            return ResponseEntity.ok(endorsementService.listMine());
        }
        return ResponseEntity.ok(endorsementService.listForReviewer(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EndorsementRequestResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(endorsementService.getForCurrentUser(id));
    }

    /** The audit trail of one request, for an authorized caller. */
    @GetMapping("/{id}/events")
    public ResponseEntity<List<EndorsementEventResponse>> events(@PathVariable Long id) {
        return ResponseEntity.ok(endorsementService.eventsForCurrentUser(id));
    }

    // ==================== DEAN ====================

    /**
     * Approves a pending request and issues the official endorsement letter. The
     * approving identity is the authenticated Dean.
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<EndorsementRequestResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) EndorsementDecisionRequest request) {
        return ResponseEntity.ok(endorsementService.approve(id, decision(request)));
    }

    /** Rejects a pending request; a reason is required. */
    @PutMapping("/{id}/reject")
    public ResponseEntity<EndorsementRequestResponse> reject(
            @PathVariable Long id,
            @RequestBody(required = false) EndorsementDecisionRequest request) {
        return ResponseEntity.ok(endorsementService.reject(id, decision(request)));
    }

    /**
     * Explicitly reissues an issued letter as a new version. Never a silent
     * overwrite: the previous file is preserved and named in the audit trail.
     */
    @PutMapping("/{id}/reissue")
    public ResponseEntity<EndorsementRequestResponse> reissue(
            @PathVariable Long id,
            @RequestBody(required = false) EndorsementDecisionRequest request) {
        return ResponseEntity.ok(endorsementService.reissue(id, decision(request)));
    }

    private static EndorsementDecisionRequest decision(EndorsementDecisionRequest request) {
        return request == null ? new EndorsementDecisionRequest() : request;
    }

    // ==================== DOCUMENT ====================

    /**
     * The issued official letter. The same stored PDF is served whether it is
     * previewed in the app ({@code download} absent) or downloaded
     * ({@code download=true}), so the two are always the same document.
     */
    @GetMapping("/{id}/document")
    public ResponseEntity<InputStreamResource> document(
            @PathVariable Long id,
            @RequestParam(name = "download", defaultValue = "false") boolean download) {
        EndorsementRequestService.StoredLetter letter = endorsementService.loadLetter(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, (download ? "attachment" : "inline")
                        + "; filename=\"" + letter.fileName() + "\"")
                .contentLength(letter.stored().size())
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(letter.stored().content()));
    }

    // ==================== CONTINUE TO PROPOSAL SUBMISSION ====================

    /** Links an approved endorsement to the proposal the Proponent continued to. */
    @PutMapping("/{id}/proposal")
    public ResponseEntity<EndorsementRequestResponse> linkProposal(
            @PathVariable Long id,
            @RequestBody EndorsementLinkRequest request) {
        return ResponseEntity.ok(
                endorsementService.linkProposal(id, request == null ? null : request.getProposalId()));
    }

    // ==================== LETTER SETTINGS ====================

    /**
     * The institutional facts the letter is addressed with. Readable by any
     * authenticated user (the UI shows them), writable only by an RPS_ADMIN.
     */
    @GetMapping("/letter-settings")
    public ResponseEntity<EndorsementLetterSettingsResponse> letterSettings() {
        return ResponseEntity.ok(settingsService.toResponse(settingsService.getSettings()));
    }

    @PutMapping("/letter-settings")
    public ResponseEntity<EndorsementLetterSettingsResponse> updateLetterSettings(
            @RequestBody EndorsementLetterSettingsRequest request) {
        Long actorId = currentUser.requireUserId();
        return ResponseEntity.ok(settingsService.toResponse(
                settingsService.update(request, actorId, endorsementService.accountName(actorId))));
    }

    // ==================== ERROR MAPPING ====================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}
