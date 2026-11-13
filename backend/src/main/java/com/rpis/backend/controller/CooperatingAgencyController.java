package com.rpis.backend.controller;

import com.rpis.backend.dto.CooperatingAgencyRequest;
import com.rpis.backend.model.CooperatingAgency;
import com.rpis.backend.service.CooperatingAgencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * The shared cooperating agency directory used by section 02 of the Create
 * Proposal form.
 * <p>
 * Reading the directory requires an authenticated caller; adding an agency is an
 * administrative action and is restricted to the RPS roles in {@code SecurityConfig}
 * ({@code RPS_ADMIN} / {@code RPS_STAFF}, the roles that manage the master data).
 * A proponent therefore selects from the list and can never change it: an agency
 * that is missing is requested from an administrator.
 */
@RestController
@RequestMapping("/api/cooperating-agencies")
@RequiredArgsConstructor
public class CooperatingAgencyController {

    private final CooperatingAgencyService cooperatingAgencyService;

    /** The whole directory, for the Create Proposal checklist. */
    @GetMapping
    public ResponseEntity<List<CooperatingAgency>> getAgencies() {
        return ResponseEntity.ok(cooperatingAgencyService.getAllAgencies());
    }

    /**
     * Administrator-only: adds an agency that is not in the directory yet. The name
     * is validated and compared case-insensitively with the existing agencies; a
     * duplicate is answered with 409 and a message that can be shown as-is.
     * <p>
     * The role requirement lives in {@code SecurityConfig} (the proponent form never
     * calls this endpoint).
     */
    @PostMapping
    public ResponseEntity<CooperatingAgency> addAgency(@RequestBody CooperatingAgencyRequest request) {
        CooperatingAgency created = cooperatingAgencyService.addAgency(request != null ? request.getName() : null);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // ==================== ERROR MAPPING ====================

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
