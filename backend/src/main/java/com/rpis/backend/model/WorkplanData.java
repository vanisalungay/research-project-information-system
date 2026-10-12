package com.rpis.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured "MSUN Internally Funded Project Workplan" attached to a quarterly
 * reporting stage (stages 1-3). Stored as JSON inside the {@code workplan_json}
 * column of the parent {@link QuarterlyProgressReport}.
 *
 * <p>Reporting stages 1-3 each carry their own workplan row/record, so one
 * quarter's workplan never overwrites another quarter's workplan.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkplanData {

    /** "As of" date in ISO form (yyyy-MM-dd), or null when not provided. */
    private String asOf;

    private String projectTitle;
    private String projectLeader;
    private String projectDuration;

    /** Numbered objective rows (OBJECTIVES / monthly activities / TOTAL). */
    private List<WorkplanObjective> objectives = new ArrayList<>();

    /** REMARKS (catch up plan, etc.). */
    private String remarks;
}
