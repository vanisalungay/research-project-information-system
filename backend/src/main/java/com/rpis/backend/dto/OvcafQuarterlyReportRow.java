package com.rpis.backend.dto;

import com.rpis.backend.model.QuarterlyProgressReport;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * One submitted quarterly report as OVCAF sees it: the report itself plus the project
 * identity the table needs. The full report payload is included so the shared
 * read-only viewer can render it without a second request.
 */
@Data
public class OvcafQuarterlyReportRow {

    private Long id;
    private Long proposalId;
    private String documentId;
    private String projectTitle;
    private String projectLeader;
    private String college;
    private String programTitle;
    private String proposalStatus;
    private LocalDateTime submittedAt;
    private String period;
    private Integer year;
    private String status;
    private String reportType;
    private QuarterlyProgressReport report;
}
