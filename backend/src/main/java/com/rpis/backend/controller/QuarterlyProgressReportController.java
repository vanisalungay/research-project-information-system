package com.rpis.backend.controller;

import com.rpis.backend.dto.QuarterlyProgressReportRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.QuarterlyProgressReport;
import com.rpis.backend.service.QuarterlyProgressReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
