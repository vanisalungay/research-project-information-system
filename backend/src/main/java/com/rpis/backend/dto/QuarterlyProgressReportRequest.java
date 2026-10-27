package com.rpis.backend.dto;

import com.rpis.backend.model.QuarterlyObjective;
import com.rpis.backend.model.QuarterlyOutput;
import com.rpis.backend.model.TerminalReportData;
import com.rpis.backend.model.WorkplanData;
import lombok.Data;

import java.util.List;

@Data
public class QuarterlyProgressReportRequest {

    private Long proposalId;
    private String period; // Q1, Q2, Q3, Q4, TERMINAL
    private Integer year;
    private String status; // DRAFT or SUBMITTED
    // QUARTERLY_PROGRESS (stages 1-3) or TERMINAL_REPORT (final stage). Optional:
    // the backend derives/validates the required next stage when omitted.
    private String reportType;

    private String projectTitle;
    private String projectLeader;
    private String leaderGender;
    private String baseStation;
    private String sitesOfImplementation;
    private String projectDuration;
    private String projectStartDate; // ISO date string (yyyy-MM-dd)
    private String projectEndDate;   // ISO date string (yyyy-MM-dd)

    private String catchUpPlan;
    private String problemsConcerns;
    private String suggestedSolutions;

    private List<QuarterlyObjective> objectives;
    private List<QuarterlyOutput> outputs;

    // Structured Project Workplan (reporting stages 1-3). Optional: omitted for
    // the terminal stage, which has no workplan.
    private WorkplanData workplan;

    // Structured Terminal Report content (reporting stage 4). Optional: omitted for
    // stages 1-3, and for a terminal submission that only carries a legacy uploaded
    // file.
    private TerminalReportData terminalReport;

    /**
     * The report the proponent chose as the starting point of this one ("Create a Copy").
     * Optional and only honoured when a brand-new record is created: the service stores it
     * for traceability and only when it is a report of the same project.
     */
    private Long copiedFromReportId;
}
