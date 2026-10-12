package com.rpis.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A single objective row of the "MSUN Internally Funded Project Workplan".
 * Stored as JSON inside the {@code workplan_json} column of the parent
 * quarterly progress report (same mechanism as the report's objectives/outputs).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkplanObjective {

    /** Objective description (leftmost OBJECTIVES column). */
    private String objective;

    /** Planned activity/accomplishment for each month, in order Jan..Dec. */
    private List<String> months = new ArrayList<>();

    /**
     * Total accomplishment for the objective. Entered manually — the official
     * workplan format does not define an automatic calculation rule.
     */
    private String total;
}
