package com.rpis.backend.controller;

import com.rpis.backend.dto.OvcafQuarterlyReportPageResponse;
import com.rpis.backend.service.OvcafQuarterlyReportService;
import com.rpis.backend.service.QuarterlyProgressReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Read-only quarterly reporting for OVCAF. The {@code /api/ovcaf/**} security rule
 * restricts every route here to the OVCAF role; no write endpoint exists.
 */
@RestController
@RequestMapping("/api/ovcaf/quarterly-reports")
@RequiredArgsConstructor
public class OvcafQuarterlyReportController {

    private final OvcafQuarterlyReportService reportService;

    @GetMapping
    public ResponseEntity<OvcafQuarterlyReportPageResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String college,
            @RequestParam(required = false) String program,
            @RequestParam(required = false) Long proposalId,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reportService.list(
                search, period, college, program, proposalId, sortBy, sortDir, page, size));
    }

    /** The attached workplan / terminal-report file of an authorized submitted report. */
    @GetMapping("/{id}/documents/{docType}")
    public ResponseEntity<InputStreamResource> downloadDocument(@PathVariable Long id,
            @PathVariable String docType) {
        try {
            QuarterlyProgressReportService.DocumentDownload download =
                    reportService.loadDocument(id, docType);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + download.fileName() + "\"")
                    .contentLength(download.stored().size())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(download.stored().content()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
