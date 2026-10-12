package com.rpis.backend.controller;

import com.rpis.backend.dto.QuarterlyProgressReportRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.QuarterlyProgressReport;
import com.rpis.backend.service.QuarterlyProgressReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quarterly-reports")
@RequiredArgsConstructor
public class QuarterlyProgressReportController {

    private final QuarterlyProgressReportService service;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody QuarterlyProgressReportRequest request) {
        try {
            return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody QuarterlyProgressReportRequest request) {
        try {
            return ResponseEntity.ok(service.update(id, request));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuarterlyProgressReport> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getReport(id));
    }

    @GetMapping("/proposal/{proposalId}")
    public ResponseEntity<List<QuarterlyProgressReport>> getByProposal(@PathVariable Long proposalId) {
        return ResponseEntity.ok(service.getReportsByProposal(proposalId));
    }

    @GetMapping("/eligible-projects")
    public ResponseEntity<List<Proposal>> getEligibleProjects() {
        return ResponseEntity.ok(service.getEligibleProjects());
    }

    /**
     * Next required reporting stage for a proposal (Q1/Q2/Q3 quarterly stage or
     * the terminal stage), derived from that proposal's own submitted reports.
     */
    @GetMapping("/next-stage/{proposalId}")
    public ResponseEntity<?> getNextStage(@PathVariable Long proposalId) {
        try {
            return ResponseEntity.ok(service.getNextStage(proposalId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Attach the Project Workplan (stages 1-3) and/or Terminal Report (final
     * stage) file to a draft report before it is submitted.
     */
    @PostMapping("/{id}/documents")
    public ResponseEntity<?> uploadDocuments(
            @PathVariable Long id,
            @RequestParam(value = "workplan", required = false) MultipartFile workplan,
            @RequestParam(value = "terminalReport", required = false) MultipartFile terminalReport) {
        try {
            return ResponseEntity.ok(service.attachDocuments(id, workplan, terminalReport));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Download an attached report document. {@code docType} is {@code workplan} or
     * {@code terminal}. Access is restricted by the {@code /api/quarterly-reports/**}
     * GET rule to PROPONENT, RPS_ADMIN, RPS_STAFF and OVCRIGE.
     */
    @GetMapping("/{id}/documents/{docType}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, @PathVariable String docType) {
        try {
            QuarterlyProgressReportService.DocumentDownload download = service.loadDocument(id, docType);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + download.fileName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(download.resource());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
