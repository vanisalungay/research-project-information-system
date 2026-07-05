package com.rpis.backend.controller;

import com.rpis.backend.model.ProjectReport;
import com.rpis.backend.repository.ProjectReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ProjectReportRepository reportRepository;
    
    private static final String UPLOAD_DIR = "uploads/reports/";

    /**
     * Upload project reports (Quarterly Progress and/or Financial)
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadReports(
            @RequestParam("proposalId") Long proposalId,
            @RequestParam("period") String period,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "quarterlyReport", required = false) MultipartFile quarterlyReport,
            @RequestParam(value = "financialReport", required = false) MultipartFile financialReport) {
        
        try {
            // Ensure upload directory exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Map<String, Object> response = new HashMap<>();
            
            // Save Quarterly Progress Report
            if (quarterlyReport != null && !quarterlyReport.isEmpty()) {
                ProjectReport quarterly = saveReport(proposalId, "QUARTERLY_PROGRESS", period, 
                        quarterlyReport, remarks);
                response.put("quarterlyReportId", quarterly.getId());
            }
            
            // Save Financial Report
            if (financialReport != null && !financialReport.isEmpty()) {
                ProjectReport financial = saveReport(proposalId, "FINANCIAL", period, 
                        financialReport, remarks);
                response.put("financialReportId", financial.getId());
            }
            
            response.put("message", "Reports uploaded successfully");
            response.put("proposalId", proposalId);
            response.put("period", period);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload reports: " + e.getMessage()));
        }
    }

    /**
     * Get all reports for a proposal
     */
    @GetMapping("/proposal/{proposalId}")
    public ResponseEntity<List<ProjectReport>> getReportsByProposal(@PathVariable Long proposalId) {
        return ResponseEntity.ok(reportRepository.findByProposalId(proposalId));
    }

    /**
     * Get reports by type for a proposal
     */
    @GetMapping("/proposal/{proposalId}/type/{reportType}")
    public ResponseEntity<List<ProjectReport>> getReportsByType(
            @PathVariable Long proposalId, 
            @PathVariable String reportType) {
        return ResponseEntity.ok(reportRepository.findByProposalIdAndReportType(proposalId, reportType));
    }

    /**
     * Get all pending reports (for RPS review)
     */
    @GetMapping("/pending")
    public ResponseEntity<List<ProjectReport>> getPendingReports() {
        return ResponseEntity.ok(reportRepository.findByReviewStatus("PENDING"));
    }

    /**
     * Review a report (for RPS)
     */
    @PutMapping("/{reportId}/review")
    public ResponseEntity<ProjectReport> reviewReport(
            @PathVariable Long reportId,
            @RequestParam Long reviewerId,
            @RequestParam String status, // APPROVED or RETURNED
            @RequestParam(required = false) String comments) {
        
        return reportRepository.findById(reportId)
                .map(report -> {
                    report.setReviewedBy(reviewerId);
                    report.setReviewedAt(LocalDateTime.now());
                    report.setReviewStatus(status);
                    report.setReviewComments(comments);
                    return ResponseEntity.ok(reportRepository.save(report));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a report
     */
    @DeleteMapping("/{reportId}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long reportId) {
        if (reportRepository.existsById(reportId)) {
            reportRepository.deleteById(reportId);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Helper method to save a report
     */
    private ProjectReport saveReport(Long proposalId, String reportType, String period,
            MultipartFile file, String remarks) throws IOException {
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
        String uniqueFilename = UUID.randomUUID().toString() + "_" + reportType.toLowerCase() + extension;
        
        // Save file
        Path filePath = Paths.get(UPLOAD_DIR, uniqueFilename);
        Files.write(filePath, file.getBytes());
        
        // Create report record
        ProjectReport report = new ProjectReport();
        report.setProposalId(proposalId);
        report.setReportType(reportType);
        report.setPeriod(period);
        report.setFileName(originalFilename);
        report.setFilePath(filePath.toString());
        report.setRemarks(remarks);
        
        return reportRepository.save(report);
    }
}