package com.rpis.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row of the Terminal Report summary sheet's "Objectives / Accomplishments" table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TerminalReportObjective {

    private String objective;
    private String accomplishment;
}
