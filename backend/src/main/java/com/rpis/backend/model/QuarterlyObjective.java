package com.rpis.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single row of the "A. Actual accomplishment of the project (via-a-vis the
 * objectives)" table of a Quarterly Progress Report. Stored as JSON inside the
 * {@code objectives_json} column of the parent report.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuarterlyObjective {

    private String objective;
    private String target;
    private String actual;
    private String percentagePeriod;
    private String percentageCumulative;
}
