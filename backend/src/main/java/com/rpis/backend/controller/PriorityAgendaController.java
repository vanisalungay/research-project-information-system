package com.rpis.backend.controller;

import com.rpis.backend.dto.ActivePriorityAgendaResponse;
import com.rpis.backend.dto.PriorityAgendaItemRequest;
import com.rpis.backend.dto.PriorityAgendaSetRequest;
import com.rpis.backend.model.PriorityAgenda;
import com.rpis.backend.model.PriorityAgendaSet;
import com.rpis.backend.service.PriorityAgendaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Priority Agenda master data (section 05 of the Create Proposal form).
 * <p>
 * Two read endpoints serve every screen that has to display agenda data - the
 * Proponent form and the historical labels of a saved proposal - and the rest is
 * administrator-only. The role requirement lives in {@code SecurityConfig}, so a
 * Proponent can never create, edit, activate or deactivate an agenda even by calling
 * the API directly; the frontend is never the only guard.
 */
@RestController
@RequestMapping("/api/priority-agendas")
@RequiredArgsConstructor
public class PriorityAgendaController {

    private final PriorityAgendaService priorityAgendaService;

    // ==================== READ (any authenticated caller) ====================

    /**
     * The configuration new proposals select from: the active framework (name and
     * coverage period) and its active agenda items. A successful response with an
     * empty agenda list means "nothing is configured yet" - it is deliberately not an
     * error, so the form can show its empty state instead of a failure.
     */
    @GetMapping("/active")
    public ResponseEntity<ActivePriorityAgendaResponse> getActiveConfiguration() {
        return ResponseEntity.ok(priorityAgendaService.getActiveConfiguration());
    }

    /**
     * Resolves the display metadata of the given stable keys, including keys of a
     * deactivated set or item, so a proposal submitted under an agenda that is no
     * longer offered still shows the agenda name it was submitted under. Unknown keys
     * are simply absent from the response.
     */
    @GetMapping("/lookup")
    public ResponseEntity<List<ActivePriorityAgendaResponse.Item>> lookup(
            @RequestParam(name = "keys", required = false) List<String> keys) {
        return ResponseEntity.ok(priorityAgendaService.lookup(keys));
    }

    // ==================== READ (administrator) ====================

    /** Every set with its items (active and inactive), the active set first. */
    @GetMapping("/sets")
    public ResponseEntity<List<PriorityAgendaSet>> getSets() {
        return ResponseEntity.ok(priorityAgendaService.getAllSets());
    }

    @GetMapping("/sets/{id}")
    public ResponseEntity<PriorityAgendaSet> getSet(@PathVariable Long id) {
        return ResponseEntity.ok(priorityAgendaService.getSet(id));
    }

    // ==================== WRITE (administrator) ====================

    @PostMapping("/sets")
    public ResponseEntity<PriorityAgendaSet> createSet(@RequestBody PriorityAgendaSetRequest request) {
        return new ResponseEntity<>(priorityAgendaService.createSet(request), HttpStatus.CREATED);
    }

    @PutMapping("/sets/{id}")
    public ResponseEntity<PriorityAgendaSet> updateSet(
            @PathVariable Long id, @RequestBody PriorityAgendaSetRequest request) {
        return ResponseEntity.ok(priorityAgendaService.updateSet(id, request));
    }

    /**
     * Activates one set for new submissions and deactivates the previous one in the
     * same transaction. Nothing is deleted, so proposals submitted under the previous
     * set keep their historical selection.
     */
    @PutMapping("/sets/{id}/activate")
    public ResponseEntity<PriorityAgendaSet> activateSet(@PathVariable Long id) {
        return ResponseEntity.ok(priorityAgendaService.activateSet(id));
    }

    @PostMapping("/sets/{id}/items")
    public ResponseEntity<PriorityAgenda> addItem(
            @PathVariable Long id, @RequestBody PriorityAgendaItemRequest request) {
        return new ResponseEntity<>(priorityAgendaService.addItem(id, request), HttpStatus.CREATED);
    }

    /** Edits an item and/or activates / deactivates it. The stable key is immutable. */
    @PutMapping("/items/{id}")
    public ResponseEntity<PriorityAgenda> updateItem(
            @PathVariable Long id, @RequestBody PriorityAgendaItemRequest request) {
        return ResponseEntity.ok(priorityAgendaService.updateItem(id, request));
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
