package com.rpis.backend.service;

import com.rpis.backend.dto.QuarterlyProgressReportRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.QuarterlyProgressReport;
import com.rpis.backend.model.TerminalReportAppendix;
import com.rpis.backend.model.TerminalReportData;
import com.rpis.backend.model.TerminalReportEntry;
import com.rpis.backend.model.TerminalReportObjective;
import com.rpis.backend.model.TerminalReportReference;
import com.rpis.backend.model.TerminalReportSection;
import com.rpis.backend.model.TerminalReportSubsection;
import com.rpis.backend.model.TerminalReportTable;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.QuarterlyProgressReportRepository;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL-backed tests for the structured Terminal Report (reporting stage 4).
 *
 * <p>The Terminal Report is completed in RPIS instead of being uploaded as a file, using the
 * structure of the official MSUN-Naawan terminal report format (cover page, summary sheet,
 * abstract with keywords, table of contents and lists, numbered body sections with
 * subsections and captioned tables, literature cited, appendix) plus the additional RPIS
 * sections. The content is stored on the same report row, a draft can be saved while
 * incomplete and reopened with every section intact, submitting requires the core narrative
 * (report title, abstract, introduction, results and discussion) and the one-page abstract
 * limit, and a legacy uploaded terminal report file remains a valid way to satisfy the
 * stage.</p>
 *
 * <p>Runs against the isolated {@code rpis_test} database only.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TerminalReportContentPostgresTest {

    @Autowired
    private QuarterlyProgressReportService service;
    @Autowired
    private QuarterlyProgressReportRepository reportRepository;
    @Autowired
    private ProposalRepository proposalRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Proposal proposal;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE quarterly_progress_reports RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        User proponent = new User();
        proponent.setName("Terminal Proponent");
        proponent.setEmail("terminal.proponent@msunaawan.edu.ph");
        proponent.setPassword("secret");
        proponent.setRole("PROPONENT");
        proponent.setStatus("APPROVED");
        User saved = userRepository.saveAndFlush(proponent);

        Proposal p = new Proposal();
        p.setProjectTitle("Fish Landing Survey");
        p.setProponent(saved);
        p.setStatus("APPROVED");
        p.setSoNumber("SO-2026-001");
        proposal = proposalRepository.saveAndFlush(p);

        // Stages 1-3 already submitted, so the next required stage is the Terminal Report.
        for (String period : List.of("Q1", "Q2", "Q3")) {
            QuarterlyProgressReport q = new QuarterlyProgressReport();
            q.setProposalId(proposal.getId());
            q.setReportType(QuarterlyProgressReportService.TYPE_QUARTERLY);
            q.setPeriod(period);
            q.setYear(2026);
            q.setStatus("SUBMITTED");
            reportRepository.saveAndFlush(q);
        }
    }

    private QuarterlyProgressReportRequest request(TerminalReportData content, String status) {
        QuarterlyProgressReportRequest request = new QuarterlyProgressReportRequest();
        request.setProposalId(proposal.getId());
        request.setReportType(QuarterlyProgressReportService.TYPE_TERMINAL);
        request.setPeriod("TERMINAL");
        request.setStatus(status);
        request.setTerminalReport(content);
        return request;
    }

    private TerminalReportSection section(String text, TerminalReportSubsection... subsections) {
        TerminalReportSection section = new TerminalReportSection();
        section.setText(text);
        section.setSubsections(new java.util.ArrayList<>(
                subsections == null ? List.<TerminalReportSubsection>of() : List.of(subsections)));
        return section;
    }

    private TerminalReportSection subsection(String heading, String text) {
        return section(null, new TerminalReportSubsection(heading, text));
    }

    /** A fully populated structured Terminal Report, following the official format. */
    private TerminalReportData terminalReport() {
        TerminalReportData data = new TerminalReportData();
        // Cover page
        data.setProgramTitle("Fisheries Program");
        data.setProjectTitle("Fish Landing Survey");
        data.setStudyTitle("Market Site Study");
        data.setProjectTeam("Juan Dela Cruz, Maria Santos");
        data.setCollaborators("MSU Naawan - College of Marine Sciences");
        data.setImplementingAgencies("MSU at Naawan");
        data.setFundingAgencies("MSU at Naawan");
        data.setSubmissionMonthYear("MARCH 2027");
        // Summary sheet
        data.setSummaryRows(List.of(
                new TerminalReportEntry("Program Title", "Fisheries Program", ""),
                new TerminalReportEntry("Project Title", "Fish Landing Survey", ""),
                new TerminalReportEntry("Project Leader/Gender", "Juan Dela Cruz / Male", "")));
        data.setSummaryAccomplishmentsTitle("Major accomplishments of the project");
        data.setSummaryObjectives(List.of(new TerminalReportObjective(
                "Measure the change in fish landings.", "Documented a 24.43% decline.")));
        data.setSummaryTotalBudget("PHP 25,000.00");
        data.setSummarySourceOfFunds("MSU-Naawan Research Division - Internally Funded Research");
        // Preliminary pages
        data.setAbstractText("This study assessed fish landing practices in Naawan.");
        data.setKeywords("fish landings, catch per unit effort, Naawan");
        data.setAcknowledgment("We thank the landing site staff.");
        data.setTableOfContents(List.of(new TerminalReportEntry("1. INTRODUCTION", "", "1")));
        data.setListOfTables(
                List.of(new TerminalReportEntry("Table 1. Catch per unit effort", "", "5")));
        data.setListOfFigures(List.of(new TerminalReportEntry("Figure 1. Map of sites", "", "3")));
        data.setListOfGraphs(List.of(new TerminalReportEntry("Graph 1. Monthly catch", "", "6")));
        data.setListOfAppendices(List.of(new TerminalReportEntry("Appendix 1", "Survey form", "")));
        data.setListOfAcronyms(
                List.of(new TerminalReportEntry("CPUE", "Catch per unit effort", "")));
        // Body sections
        data.setIntroduction(section("Fish landing surveys document catch composition."));
        data.setReviewOfLiterature(subsection("Catch composition", "Earlier work reported this."));
        data.setMethodology(subsection("Study design", "Monthly sampling at two landing sites."));
        data.setResultsAndDiscussion(section("A total of 24 species were recorded."));
        data.getResultsAndDiscussion().setTables(new java.util.ArrayList<>(List.of(table())));
        data.setConclusions(subsection("Conclusions", "Landing practices are stable."));
        data.setLiteratureCited(List.of(new TerminalReportReference(
                "Dela Cruz, J.", "2020", "Landing survey methods", "2nd", "Naawan", "", "12-18")));
        data.setAppendices(List.of(new TerminalReportAppendix(
                "Landing Survey Form", "The survey form used during data collection.")));
        data.setAppendixTables(new java.util.ArrayList<>(List.of(table())));
        data.setProblems(subsection("Technical problems encountered", "Two trips were cancelled."));
        data.setOutputs(section(null, null));
        data.getOutputs().setTables(new java.util.ArrayList<>(List.of(table())));
        data.setUtilization(section(null, null));
        data.getUtilization().setTables(new java.util.ArrayList<>(List.of(table())));
        return data;
    }

    /** A captioned table with a header row and one data row. */
    private TerminalReportTable table() {
        TerminalReportTable table = new TerminalReportTable();
        table.setCaption("Catch per unit effort by site");
        table.setNote("Source: landing site records.");
        table.setColumns(new java.util.ArrayList<>(List.of("Site", "2019", "2020")));
        table.setRows(new java.util.ArrayList<>(List.of(
                new java.util.ArrayList<>(List.of("Poblacion", "100.00", "75.57")))));
        return table;
    }

    /** The core narrative only, as a minimum valid submission. */
    private TerminalReportData coreContent() {
        TerminalReportData data = new TerminalReportData();
        data.setProjectTitle("Fish Landing Survey");
        data.setAbstractText("This study assessed fish landing practices in Naawan.");
        data.setIntroduction(section("Fish landing surveys document catch composition."));
        data.setResultsAndDiscussion(subsection("Landings", "A total of 24 species were recorded."));
        return data;
    }


    // 1. A terminal draft stores the structured report on its own row.
    @Test
    void aTerminalDraftStoresTheStructuredReport() {
        QuarterlyProgressReport saved = service.create(request(terminalReport(), "DRAFT"));

        assertEquals(QuarterlyProgressReportService.TYPE_TERMINAL, saved.getReportType());
        assertEquals("TERMINAL", saved.getPeriod());
        assertEquals("DRAFT", saved.getStatus());
        assertNull(saved.getSubmittedAt(), "a draft is not a submission");
        assertEquals(proposal.getId(), saved.getProposalId());
        assertNotNull(saved.getTerminalReport());
    }

    // 2. Every section, including the numbered subsections and the tables, survives a reload.
    @Test
    void everySectionIncludingSubsectionsAndTablesIsPreserved() {
        QuarterlyProgressReport saved = service.create(request(terminalReport(), "DRAFT"));

        TerminalReportData reloaded = reportRepository.findById(saved.getId()).orElseThrow()
                .getTerminalReport();
        assertEquals("Fisheries Program", reloaded.getProgramTitle());
        assertEquals("MARCH 2027", reloaded.getSubmissionMonthYear());
        assertEquals("PHP 25,000.00", reloaded.getSummaryTotalBudget());
        assertEquals(3, reloaded.getSummaryRows().size());
        assertEquals("Project Leader/Gender", reloaded.getSummaryRows().get(2).getLabel());
        assertEquals("Documented a 24.43% decline.",
                reloaded.getSummaryObjectives().get(0).getAccomplishment());
        assertEquals("This study assessed fish landing practices in Naawan.",
                reloaded.getAbstractText());
        assertEquals("fish landings, catch per unit effort, Naawan", reloaded.getKeywords());
        assertEquals("1. INTRODUCTION", reloaded.getTableOfContents().get(0).getLabel());
        assertEquals("1", reloaded.getTableOfContents().get(0).getPage());
        assertEquals("CPUE", reloaded.getListOfAcronyms().get(0).getLabel());
        assertEquals("Catch per unit effort", reloaded.getListOfAcronyms().get(0).getDetail());
        assertEquals("Fish landing surveys document catch composition.",
                reloaded.getIntroduction().getText());
        assertEquals("Catch composition",
                reloaded.getReviewOfLiterature().getSubsections().get(0).getHeading());
        assertEquals("Monthly sampling at two landing sites.",
                reloaded.getMethodology().getSubsections().get(0).getText());
        TerminalReportTable table = reloaded.getResultsAndDiscussion().getTables().get(0);
        assertEquals("Catch per unit effort by site", table.getCaption());
        assertEquals(List.of("Site", "2019", "2020"), table.getColumns());
        assertEquals("75.57", table.getRows().get(0).get(2));
        assertEquals("Dela Cruz, J.", reloaded.getLiteratureCited().get(0).getAuthor());
        assertEquals("Landing Survey Form", reloaded.getAppendices().get(0).getTitle());
        assertEquals(1, reloaded.getAppendixTables().size());
        assertEquals("Two trips were cancelled.",
                reloaded.getProblems().getSubsections().get(0).getText());
        assertEquals("Catch per unit effort by site",
                reloaded.getOutputs().getTables().get(0).getCaption());
    }

    // 3. A saved draft can be reopened and updated, and an update that does not carry the
    //    content never wipes what the proponent already entered.
    @Test
    void aDraftCanBeReopenedAndIsNeverWipedByAnUnrelatedUpdate() {
        QuarterlyProgressReport draft = service.create(request(terminalReport(), "DRAFT"));

        // An update without the terminal report (for example attaching a file only).
        QuarterlyProgressReportRequest attachOnly = new QuarterlyProgressReportRequest();
        attachOnly.setProposalId(proposal.getId());
        attachOnly.setStatus("DRAFT");
        service.update(draft.getId(), attachOnly);

        TerminalReportData afterAttachOnly = reportRepository.findById(draft.getId()).orElseThrow()
                .getTerminalReport();
        assertNotNull(afterAttachOnly);
        assertEquals("Fisheries Program", afterAttachOnly.getProgramTitle());

        // The proponent reopens the draft, changes a section and saves again.
        TerminalReportData edited = terminalReport();
        edited.getResultsAndDiscussion().setText("A total of 31 species were recorded.");
        edited.getConclusions().getSubsections().get(0)
                .setText("Landing practices are stable across the year.");
        service.update(draft.getId(), request(edited, "DRAFT"));

        TerminalReportData reloaded = reportRepository.findById(draft.getId()).orElseThrow()
                .getTerminalReport();
        assertEquals("A total of 31 species were recorded.",
                reloaded.getResultsAndDiscussion().getText());
        assertEquals("Landing practices are stable across the year.",
                reloaded.getConclusions().getSubsections().get(0).getText());
        assertEquals("This study assessed fish landing practices in Naawan.",
                reloaded.getAbstractText(), "the untouched sections stay as they were");
    }

    // 4. Submitting without any Terminal Report content is rejected.
    @Test
    void submittingWithoutContentIsRejected() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.create(request(null, "SUBMITTED")));

        assertTrue(ex.getMessage().toLowerCase().contains("terminal report"),
                "the message names the Terminal Report form: " + ex.getMessage());
        assertEquals(0, reportRepository.findByProposalIdOrderByCreatedAtAsc(proposal.getId())
                .stream()
                .filter(r -> QuarterlyProgressReportService.TYPE_TERMINAL.equals(r.getReportType()))
                .count(), "no terminal row is created for a rejected submission");
    }

    // 5. A report written as numbered subsections (the official format) satisfies the
    //    submission gate, and the optional sections may stay empty.
    @Test
    void submittingSubsectionWrittenSectionsIsAccepted() {
        TerminalReportData content = coreContent();
        // The introduction and the results are written as subsections, as in the sample.
        content.setIntroduction(subsection("Background", "Fish landing surveys document catch."));
        content.setResultsAndDiscussion(subsection("Landings", "24 species were recorded."));

        QuarterlyProgressReport submitted = service.create(request(content, "SUBMITTED"));

        assertEquals("SUBMITTED", submitted.getStatus());
        assertNotNull(submitted.getSubmittedAt());
        assertEquals(proposal.getProponent().getId(), submitted.getSubmittedBy());
        assertNull(submitted.getTerminalReport().getReviewOfLiterature().getText());
        assertTrue(submitted.getTerminalReport().getOutputs().getTables().isEmpty());
        assertTrue(submitted.getTerminalReport().getLiteratureCited().isEmpty());
    }

    // 6. The abstract may not exceed the one-page limit when submitting, but a draft with
    //    the same text still saves.
    @Test
    void anAbstractOverTheLimitIsRejectedOnSubmissionOnly() {
        TerminalReportData tooLong = coreContent();
        tooLong.setAbstractText(String.join(" ", Collections.nCopies(351, "word")));

        service.create(request(tooLong, "DRAFT"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.create(request(tooLong, "SUBMITTED")));
        assertTrue(ex.getMessage().contains("350"), ex.getMessage());
        assertTrue(ex.getMessage().contains("351"),
                "the actual count is reported: " + ex.getMessage());
    }

    // 7. A submitted report can no longer be edited, and a legacy uploaded file still
    //    satisfies the stage.
    @Test
    void aSubmittedReportIsImmutableAndALegacyFileStillSubmits() {
        QuarterlyProgressReport submitted = service.create(request(coreContent(), "SUBMITTED"));

        assertThrows(IllegalStateException.class,
                () -> service.update(submitted.getId(), request(coreContent(), "DRAFT")));

        // A legacy row (created before the in-system form) with an uploaded file only.
        QuarterlyProgressReport legacy = new QuarterlyProgressReport();
        legacy.setProposalId(proposal.getId());
        legacy.setReportType(QuarterlyProgressReportService.TYPE_TERMINAL);
        legacy.setPeriod("TERMINAL");
        legacy.setYear(2026);
        legacy.setStatus("DRAFT");
        legacy.setTerminalReportFileName("terminal-report.pdf");
        legacy.setTerminalReportFilePath("uploads/reports/legacy_terminal-report.pdf");
        QuarterlyProgressReport savedLegacy = reportRepository.saveAndFlush(legacy);

        QuarterlyProgressReportRequest submitLegacy = new QuarterlyProgressReportRequest();
        submitLegacy.setProposalId(proposal.getId());
        submitLegacy.setStatus("SUBMITTED");
        service.update(savedLegacy.getId(), submitLegacy);

        QuarterlyProgressReport reloaded = reportRepository.findById(savedLegacy.getId()).orElseThrow();
        assertEquals("SUBMITTED", reloaded.getStatus());
        assertNull(reloaded.getTerminalReport(), "the legacy row keeps its file-only shape");
        assertFalse(reloaded.getTerminalReportFilePath().isBlank());
    }
}

