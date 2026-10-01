package com.rpis.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single row of the "C. Expected Outputs / 6Ps" table of a Quarterly Progress
 * Report. Stored as JSON inside the {@code outputs_json} column of the parent
 * report.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuarterlyOutput {

    private String outputType; // Publications, Patents/IP, Products, People Services, Partnerships, Policy
    private String expectedOutput;
    private String actualOutput;
}
