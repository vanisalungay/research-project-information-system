package com.rpis.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpis.backend.dto.QuarterlyProgressReportRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.QuarterlyProgressReport;
import com.rpis.backend.model.TerminalReportData;
import com.rpis.backend.model.TerminalReportSection;
import com.rpis.backend.model.User;
import com.rpis.backend.model.WorkplanData;
import com.rpis.backend.model.WorkplanObjective;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.QuarterlyProgressReportRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.service.ProposalService;
import com.rpis.backend.service.QuarterlyProgressReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for the project completion transition, exercised through the real
 * HTTP controller and the real service on PostgreSQL - the same path the frontend uses.
 *
 * <p>Reproduces the failure reported from the running application: a project with Q1,
 * Q2, Q3 and a Terminal Report submitted stayed APPROVED. The sequence below is exactly
 * what the form sends ({@code ProponentQuarterlyReportForm.vue}): each stage is created
 * or saved, the Terminal Report is first saved as a DRAFT, and the final Submit sends
 * {@code PUT /api/quarterly-reports/{id}} with {@code status = SUBMITTED}. That request
 * must move the proposal to COMPLETED, persist exactly one history row, and return the
 * updated proposal status in the response body.</p>
 *
 * <p>The tests run against the isolated {@code rpis_test} database only.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectCompletionWorkflowApiPostgresTest {

    @Autowired
    private QuarterlyProgressReportService reportService;
    @Autowired
    private ProposalService proposalService;
    @Autowired
    private ProposalRepository proposalRepository;
    @Autowired
    private QuarterlyProgressReportRepository reportRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Proposal proposal;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE proposal_status_history RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE quarterly_progress_reports RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        User proponent = new User();
        proponent.setName("Completion Proponent");
        proponent.setEmail("completion.proponent@msunaawan.edu.ph");
        proponent.setPassword("secret");
        proponent.setRole("PROPONENT");
        proponent.setStatus("APPROVED");
        User savedProponent = userRepository.saveAndFlush(proponent);

        Proposal p = new Proposal();
        p.setProjectTitle("Completion Workflow Project");
        p.setProponent(savedProponent);
        p.setStatus("APPROVED");
        p.setSoNumber("SO-2026-101");
        p.setSoUploadedAt(LocalDateTime.of(2026, 11, 1, 9, 0));
        p.setNeedsBudget(Boolean.FALSE);
        proposal = proposalRepository.saveAndFlush(p);

        mockMvc = MockMvcBuilders.standaloneSetup(new QuarterlyProgressReportController(
                reportService, proposalService, new CurrentUser())).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(savedProponent.getId(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_PROPONENT"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // Request bodies, shaped like the JSON the form sends
    // ------------------------------------------------------------------

    private QuarterlyProgressReportRequest quarterly(String period, String status) {
        QuarterlyProgressReportRequest request = new QuarterlyProgressReportRequest();
        request.setProposalId(proposal.getId());
        request.setReportType(QuarterlyProgressReportService.TYPE_QUARTERLY);
        request.setPeriod(period);
        request.setStatus(status);
        request.setYear(2026);
        WorkplanData workplan = new WorkplanData();
        workplan.setObjectives(new ArrayList<>(List.of(
                new WorkplanObjective("Complete the field survey for " + period,
                        new ArrayList<>(), "100%"))));
        request.setWorkplan(workplan);
        return request;
    }

    private TerminalReportData terminalContent() {
        TerminalReportData content = new TerminalReportData();
        content.setProjectTitle("Completion Workflow Project");
        content.setAbstractText("This project measured the completion workflow end to end.");
        TerminalReportSection introduction = new TerminalReportSection();
        introduction.setText("The project was implemented over three quarters.");
        content.setIntroduction(introduction);
        TerminalReportSection results = new TerminalReportSection();
        results.setText("All planned activities were completed and reported.");
        content.setResultsAndDiscussion(results);
        return content;
    }

    private QuarterlyProgressReportRequest terminal(String status) {
        QuarterlyProgressReportRequest request = new QuarterlyProgressReportRequest();
        request.setProposalId(proposal.getId());
        request.setReportType(QuarterlyProgressReportService.TYPE_TERMINAL);
        request.setPeriod("TERMINAL");
        request.setStatus(status);
        request.setYear(2026);
        request.setTerminalReport(terminalContent());
        return request;
    }

    // ------------------------------------------------------------------
    // Helpers: the controller calls the frontend makes
    // ------------------------------------------------------------------

    private ResultActions postReport(QuarterlyProgressReportRequest request) throws Exception {
        return mockMvc.perform(post("/api/quarterly-reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions putReport(Long id, QuarterlyProgressReportRequest request) throws Exception {
        return mockMvc.perform(put("/api/quarterly-reports/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private Long idOf(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private void submitQuarterly(String period) throws Exception {
        postReport(quarterly(period, "SUBMITTED")).andExpect(status().isCreated());
    }

    /** Forces the pending JPA changes to the database so the SQL checks see them. */
    private void flush() {
        proposalRepository.flush();
    }

    private String storedStatus() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM proposals WHERE id = ?", String.class, proposal.getId());
    }

    private int completionHistoryRows() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM proposal_status_history WHERE proposal_id = ? "
                        + "AND from_status = 'APPROVED' AND to_status = 'COMPLETED' "
                        + "AND source = 'APPLICATION'",
                Integer.class, proposal.getId());
        return count == null ? 0 : count;
    }

    // ------------------------------------------------------------------
    // The reported failure, through the exact endpoint the form calls
    // ------------------------------------------------------------------

    @Test
    void submittingTheTerminalDraftThroughTheFormCompletesTheProjectAndReturnsCompleted() throws Exception {
        submitQuarterly("Q1");
        submitQuarterly("Q2");
        submitQuarterly("Q3");

        MvcResult draft = postReport(terminal("DRAFT"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        Long terminalId = idOf(draft);
        flush();

        // A saved Terminal Report draft does not complete the project.
        assertEquals("APPROVED", storedStatus());

        // The Submit button sends the same endpoint the form uses.
        putReport(terminalId, terminal("SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.proposalStatus").value("COMPLETED"));
        flush();

        assertEquals("COMPLETED", storedStatus());
        assertEquals(1, completionHistoryRows(), "exactly one completion history row");
        assertEquals("SUBMITTED", jdbcTemplate.queryForObject(
                "SELECT status FROM quarterly_progress_reports WHERE id = ?",
                String.class, terminalId));
    }

    @Test
    void aTerminalReportSubmittedBeforeEveryQuarterlyReportIsSubmittedDoesNotCompleteTheProject()
            throws Exception {
        submitQuarterly("Q1");
        submitQuarterly("Q2");

        // The terminal row can only reach the form once it is the next stage, so it is
        // created directly to exercise the completion check with Q3 still missing.
        QuarterlyProgressReport earlyTerminal = new QuarterlyProgressReport();
        earlyTerminal.setProposalId(proposal.getId());
        earlyTerminal.setReportType(QuarterlyProgressReportService.TYPE_TERMINAL);
        earlyTerminal.setPeriod("TERMINAL");
        earlyTerminal.setYear(2026);
        earlyTerminal.setStatus("DRAFT");
        earlyTerminal.setTerminalReport(terminalContent());
        Long earlyTerminalId = reportRepository.saveAndFlush(earlyTerminal).getId();

        putReport(earlyTerminalId, terminal("SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposalStatus").value("APPROVED"));
        flush();

        assertEquals("APPROVED", storedStatus(), "a missing Q3 keeps the project approved");
        assertEquals(0, completionHistoryRows());

        // Submitting the still-missing Q3 then completes the project, because the
        // Terminal Report is already submitted and the check reads the stored stages.
        submitQuarterly("Q3");
        flush();
        assertEquals("COMPLETED", storedStatus());
        assertEquals(1, completionHistoryRows());
    }

    @Test
    void aTerminalDraftUpdateNeverCompletesTheProject() throws Exception {
        submitQuarterly("Q1");
        submitQuarterly("Q2");
        submitQuarterly("Q3");

        MvcResult draft = postReport(terminal("DRAFT"))
                .andExpect(status().isCreated())
                .andReturn();
        Long terminalId = idOf(draft);

        // Saving the draft again through the form's Save action must not complete it.
        putReport(terminalId, terminal("DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
        flush();

        assertEquals("APPROVED", storedStatus());
        assertEquals(0, completionHistoryRows());
    }

    @Test
    void aRepeatedTerminalSubmissionIsRejectedAndAddsNoSecondHistoryRow() throws Exception {
        submitQuarterly("Q1");
        submitQuarterly("Q2");
        submitQuarterly("Q3");
        Long terminalId = idOf(postReport(terminal("DRAFT"))
                .andExpect(status().isCreated()).andReturn());
        putReport(terminalId, terminal("SUBMITTED")).andExpect(status().isOk());
        flush();
        assertEquals(1, completionHistoryRows());

        putReport(terminalId, terminal("SUBMITTED"))
                .andExpect(status().isForbidden());

        flush();
        assertEquals("COMPLETED", storedStatus());
        assertEquals(1, completionHistoryRows(), "the repeat adds no second transition");
    }

    @Test
    void aCompletedProjectKeepsItsReportsAndCannotStartAnotherStage() throws Exception {
        submitQuarterly("Q1");
        submitQuarterly("Q2");
        submitQuarterly("Q3");
        Long terminalId = idOf(postReport(terminal("DRAFT"))
                .andExpect(status().isCreated()).andReturn());
        putReport(terminalId, terminal("SUBMITTED")).andExpect(status().isOk());
        flush();

        // The completed project's reports stay readable: all four rows are listed and
        // the Terminal Report document is still reachable.
        mockMvc.perform(get("/api/quarterly-reports/proposal/{id}", proposal.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
        mockMvc.perform(get("/api/quarterly-reports/{id}", terminalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.terminalReport.projectTitle")
                        .value("Completion Workflow Project"));

        // The stage resolves as complete rather than offering a fifth report.
        mockMvc.perform(get("/api/quarterly-reports/next-stage/{id}", proposal.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allStagesComplete").value(true));

        // Starting another reporting stage stays closed.
        postReport(quarterly("Q1", "DRAFT"))
                .andExpect(status().isForbidden());
    }
}
