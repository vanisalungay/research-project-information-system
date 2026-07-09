package com.rpis.backend.controller;

import com.rpis.backend.dto.OvcafProposalResponse;
import com.rpis.backend.dto.OvcafReportsSummaryResponse;
import com.rpis.backend.dto.OvcafValidationRequest;
import com.rpis.backend.model.OvcafValidation;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.service.OvcafService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ovcaf")
@RequiredArgsConstructor
public class OvcafController {

    private final OvcafService ovcafService;

    /**
     * Get all proposals for OVCAF review
     */
    @GetMapping("/proposals")
    public ResponseEntity<List<OvcafProposalResponse>> getProposalsForOvcaf() {
        return ResponseEntity.ok(ovcafService.getProposalsForOvcaf());
    }

    /**
     * Get proposals pending validation only
     */
    @GetMapping("/proposals/pending")
    public ResponseEntity<List<OvcafProposalResponse>> getPendingValidationProposals() {
        return ResponseEntity.ok(ovcafService.getPendingValidationProposals());
    }

    /**
     * Get complete proposal details for OVCAF review
     */
    @GetMapping("/proposals/{id}")
    public ResponseEntity<OvcafProposalResponse> getProposalDetails(@PathVariable Long id) {
        try {
            OvcafProposalResponse response = ovcafService.getProposalDetails(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Submit validation decision for a proposal
     */
    @PostMapping("/proposals/{id}/validate")
    public ResponseEntity<OvcafValidation> submitValidation(
            @PathVariable Long id,
            @RequestBody OvcafValidationRequest request) {
        try {
            OvcafValidation validation = ovcafService.submitValidation(id, request);
            return new ResponseEntity<>(validation, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Endorse budget - forward to OC for final approval
     */
    @PutMapping("/proposals/{id}/endorse")
    public ResponseEntity<Proposal> endorseBudget(
            @PathVariable Long id,
            @RequestParam(required = false) Long endorsedById) {
        try {
            Proposal proposal = ovcafService.endorseBudget(id, endorsedById);
            return ResponseEntity.ok(proposal);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Authorize budget release
     */
    @PutMapping("/proposals/{id}/authorize")
    public ResponseEntity<Proposal> authorizeBudget(
            @PathVariable Long id,
            @RequestParam(required = false) Long authorizedById) {
        try {
            Proposal proposal = ovcafService.authorizeBudget(id, authorizedById);
            return ResponseEntity.ok(proposal);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Release project funds
     */
    @PutMapping("/proposals/{id}/release")
    public ResponseEntity<Proposal> releaseFunds(
            @PathVariable Long id,
            @RequestParam(required = false) Long releasedById) {
        try {
            Proposal proposal = ovcafService.releaseFunds(id, releasedById);
            return ResponseEntity.ok(proposal);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get reports summary for OVCAF dashboard
     */
    @GetMapping("/reports/summary")
    public ResponseEntity<OvcafReportsSummaryResponse> getReportsSummary() {
        return ResponseEntity.ok(ovcafService.getReportsSummary());
    }

    /**
     * Get validation history for a proposal
     */
    @GetMapping("/proposals/{id}/validations")
    public ResponseEntity<List<OvcafValidation>> getValidationHistory(@PathVariable Long id) {
        try {
            List<OvcafValidation> history = ovcafService.getValidationHistory(id);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
