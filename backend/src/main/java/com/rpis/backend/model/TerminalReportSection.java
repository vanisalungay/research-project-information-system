package com.rpis.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * One numbered body section of the Terminal Report (for example {@code 3. METHODOLOGY} of
 * the official format), holding the section text, its subsections and its tables.
 *
 * <p>Sections 1-7 follow the official MSUN-Naawan terminal report format; sections 8-10
 * (Problems Encountered, Output, Utilization) are the additional sections required by the
 * RPIS reporting requirements and are rendered in the same way.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TerminalReportSection {

    /** The section text, shown before the subsections. */
    private String text;

    /** Numbered subsections, in the order the proponent arranged them. */
    private List<TerminalReportSubsection> subsections = new ArrayList<>();

    /** Captioned tables of the section, in document order. */
    private List<TerminalReportTable> tables = new ArrayList<>();

    /** True when the section holds any text, in the section itself or in a subsection. */
    public boolean hasText() {
        if (text != null && !text.isBlank()) {
            return true;
        }
        if (subsections != null) {
            for (TerminalReportSubsection subsection : subsections) {
                if (subsection != null && subsection.hasText()) {
                    return true;
                }
            }
        }
        return false;
    }
}

