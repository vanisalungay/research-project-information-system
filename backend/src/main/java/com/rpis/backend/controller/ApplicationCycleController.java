package com.rpis.backend.controller;

import com.rpis.backend.dto.ApplicationCycleRequest;
import com.rpis.backend.model.ApplicationCycle;
import com.rpis.backend.service.ApplicationCycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/application-cycles")
@RequiredArgsConstructor
public class ApplicationCycleController {

    private final ApplicationCycleService cycleService;

    // ==================== READ ====================

    @GetMapping
    public ResponseEntity<List<ApplicationCycle>> getAllCycles() {
        return ResponseEntity.ok(cycleService.getAllCycles());
    }

    /**
     * Proponent pre-check gatekeeper endpoint: returns the cycle currently
     * accepting submissions (status ACTIVE and today within its date range).
     * Responds 404 when no cycle is active.
     */
    @GetMapping("/active")
    public ResponseEntity<ApplicationCycle> getActiveCycle() {
        return ResponseEntity.ok(cycleService.getActiveCycle());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationCycle> getCycleById(@PathVariable Long id) {
        return ResponseEntity.ok(cycleService.getCycleById(id));
    }

    // ==================== CREATE / UPDATE / DELETE ====================

    @PostMapping
    public ResponseEntity<ApplicationCycle> createCycle(@RequestBody ApplicationCycleRequest request) {
        ApplicationCycle created = cycleService.createCycle(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationCycle> updateCycle(
            @PathVariable Long id,
            @RequestBody ApplicationCycleRequest request) {
        return ResponseEntity.ok(cycleService.updateCycle(id, request));
    }

    /**
     * Explicit activation action: sets the cycle to ACTIVE and notifies all
     * proponents.
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<ApplicationCycle> activateCycle(@PathVariable Long id) {
        return ResponseEntity.ok(cycleService.activateCycle(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCycle(@PathVariable Long id) {
        cycleService.deleteCycle(id);
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
