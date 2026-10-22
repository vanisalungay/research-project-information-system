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
 * Both endpoints require an authenticated caller (see {@code SecurityConfig}):
 * a proponent may read the directory and add a missing agency, and an anonymous
 * caller can do neither. Adding an agency is not an administrative privilege -
 * it only appends one reusable row to a lookup list - so no extra role is needed
 * or granted.
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
     * Adds an agency that is not in the directory yet. The name is validated and
     * compared case-insensitively with the existing agencies; a duplicate is
     * answered with 409 and a message the form can show as-is.
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
