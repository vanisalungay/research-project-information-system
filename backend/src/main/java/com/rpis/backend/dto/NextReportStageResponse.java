package com.rpis.backend.dto;

import lombok.Data;

/**
 * Describes the next required reporting stage for a single proposal, derived
 * from that proposal's own submitted reports. Reporting stages are per-proposal
 * (one proposal's reports never affect another proposal's stage).
 *
 * <p>Sequence: stages 1-3 are {@code QUARTERLY_PROGRESS} (Q1, Q2, Q3) each
 * requiring a Project Workplan alongside the quarterly progress report; the
 * fourth and final stage is {@code TERMINAL_REPORT}.</p>
 */
@Data
public class NextReportStageResponse {

    private Long proposalId;

    /** QUARTERLY_PROGRESS or TERMINAL_REPORT. Null when all stages are complete. */
    private String reportType;

    /** Q1, Q2, Q3 or TERMINAL. Null when all stages are complete. */
    private String period;

    /** True when the stage requires a Project Workplan (stages 1-3). */
    private boolean requiresWorkplan;

    /** True when the stage requires a Terminal Report upload (final stage). */
    private boolean requiresTerminalReport;

    /** Id of an existing DRAFT report for this stage, when one already exists. */
    private Long draftReportId;

    /** True when every required reporting stage has been submitted. */
    private boolean allStagesComplete;
}
