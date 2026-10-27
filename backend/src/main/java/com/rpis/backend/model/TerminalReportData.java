package com.rpis.backend.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured "MSUN Internally Funded Project Terminal Report" (reporting stage 4) completed
 * in RPIS.
 *
 * <p>Stored as JSON inside the {@code terminal_report_json} column of the parent
 * {@link QuarterlyProgressReport}, exactly like the structured Project Workplan
 * ({@code workplan_json}) of stages 1-3.</p>
 *
 * <p>The structure follows the official MSUN-Naawan terminal report format shipped with
 * RPIS: a cover page, the front matter (summary sheet, acknowledgment, abstract with
 * keywords, table of contents and the lists of tables, figures, graphs, appendices and
 * acronyms), the numbered body sections 1-7 (Introduction, Review of Related Literature,
 * Methodology, Results and Discussion, Conclusions and Recommendations, Literature Cited,
 * Appendix) and the additional sections 8-10 required by RPIS (Problems Encountered,
 * Output, Utilization). Body sections carry their own subsections and captioned tables.</p>
 *
 * <p>Only what the proponent entered is stored. The system prefills project information it
 * already holds (project title, leader and gender, agency, address, cooperating agency) but
 * never invents institutional names, budget figures, tables or results.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TerminalReportData {

    // ===== Cover page =====
    private String programTitle;
    private String projectTitle;
    /** Study title, when the terminal report covers a specific study. */
    private String studyTitle;
    /** Project team, printed as the authors of the report on the cover page. */
    private String projectTeam;
    private String collaborators;
    private String implementingAgencies;
    private String fundingAgencies;
    /** Month and year of submission, e.g. "MARCH 2023". */
    private String submissionMonthYear;

    // ===== Summary sheet =====
    /** "Project Information" table rows (label / detail). */
    private List<TerminalReportEntry> summaryRows = new ArrayList<>();
    /** Caption printed above the objectives table of the summary sheet. */
    private String summaryAccomplishmentsTitle;
    /** "Objectives / Accomplishments" rows. */
    private List<TerminalReportObjective> summaryObjectives = new ArrayList<>();
    /** "Financial Information" table. */
    private String summaryTotalBudget;
    private String summarySourceOfFunds;

    // ===== Preliminary pages =====
    /** Abstract (one page, single spaced, at most 350 words). */
    private String abstractText;
    private String keywords;
    private String acknowledgment;
    private List<TerminalReportEntry> tableOfContents = new ArrayList<>();
    private List<TerminalReportEntry> listOfTables = new ArrayList<>();
    private List<TerminalReportEntry> listOfFigures = new ArrayList<>();
    private List<TerminalReportEntry> listOfGraphs = new ArrayList<>();
    private List<TerminalReportEntry> listOfAppendices = new ArrayList<>();
    private List<TerminalReportEntry> listOfAcronyms = new ArrayList<>();

    // ===== Body sections 1-5 (text, subsections, tables) =====
    @JsonDeserialize(using = TerminalReportSectionDeserializer.class)
    private TerminalReportSection introduction = new TerminalReportSection();
    @JsonDeserialize(using = TerminalReportSectionDeserializer.class)
    private TerminalReportSection reviewOfLiterature = new TerminalReportSection();
    @JsonDeserialize(using = TerminalReportSectionDeserializer.class)
    private TerminalReportSection methodology = new TerminalReportSection();
    @JsonDeserialize(using = TerminalReportSectionDeserializer.class)
    private TerminalReportSection resultsAndDiscussion = new TerminalReportSection();
    @JsonDeserialize(using = TerminalReportSectionDeserializer.class)
    private TerminalReportSection conclusions = new TerminalReportSection();

    // ===== Body section 6: Literature Cited =====
    private List<TerminalReportReference> literatureCited = new ArrayList<>();

    // ===== Body section 7: Appendix =====
    private List<TerminalReportAppendix> appendices = new ArrayList<>();
    /** Tables of the appendix (numbered Table A1, A2 ... by the document renderer). */
    private List<TerminalReportTable> appendixTables = new ArrayList<>();

    // ===== Body sections 8-10 (RPIS reporting requirements) =====
    @JsonDeserialize(using = TerminalReportSectionDeserializer.class)
    private TerminalReportSection problems = new TerminalReportSection();
    @JsonDeserialize(using = TerminalReportSectionDeserializer.class)
    private TerminalReportSection outputs = new TerminalReportSection();
    @JsonDeserialize(using = TerminalReportSectionDeserializer.class)
    private TerminalReportSection utilization = new TerminalReportSection();
}
