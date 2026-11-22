package com.rpis.backend.controller;

import com.rpis.backend.model.OvcafValidation;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.QuarterlyProgressReport;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.OvcafValidationRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.QuarterlyProgressReportRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OVCAF Quarterly Reports: only submitted reports of implementation-approved,
 * budget-processed projects are visible, and the office has no write access.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OvcafQuarterlyReportsApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProposalRepository proposalRepository;
    @Autowired private OvcafValidationRepository validationRepository;
    @Autowired private QuarterlyProgressReportRepository reportRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String ovcafToken;
    private String rpsToken;
    private User proponent;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE quarterly_progress_reports RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        proponent = user("Report Proponent", "report.proponent@msunaawan.edu.ph", "PROPONENT");
        User ovcaf = user("Report OVCAF", "report.ovcaf@msunaawan.edu.ph", "OVCAF");
        User rps = user("Report RPS", "report.rps@msunaawan.edu.ph", "RPS_STAFF");
        ovcafToken = tokenService.generateToken(ovcaf.getId(), "OVCAF");
        rpsToken = tokenService.generateToken(rps.getId(), "RPS_STAFF");
    }

    private User user(String name, String email, String role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("secret");
        user.setRole(role);
        user.setStatus("APPROVED");
        return userRepository.saveAndFlush(user);
    }

    private Proposal proposal(String documentId, String title, String college, String program,
            boolean needsBudget) {
        Proposal proposal = new Proposal();
        proposal.setDocumentId(documentId);
        proposal.setProjectTitle(title);
        proposal.setProjectLeader("Leader " + title);
        proposal.setCollege(college);
        proposal.setProgramTitle(program);
        proposal.setStatus("APPROVED");
        proposal.setSoNumber("SO-" + documentId);
        proposal.setSoUploadedAt(LocalDateTime.of(2026, 1, 1, 8, 0));
        proposal.setNeedsBudget(needsBudget);
        proposal.setProponent(proponent);
        return proposalRepository.saveAndFlush(proposal);
    }

    private void budgetEndorsement(Proposal proposal) {
        OvcafValidation validation = new OvcafValidation();
        validation.setProposal(proposal);
        validation.setDecision("APPROVED_ENDORSED");
        validation.setBudgetEndorsed(true);
        validation.setValidatedAt(LocalDateTime.of(2026, 1, 2, 8, 0));
        validationRepository.saveAndFlush(validation);
    }

    private QuarterlyProgressReport report(Proposal proposal, String period, String status,
            LocalDateTime submittedAt) {
        QuarterlyProgressReport report = new QuarterlyProgressReport();
        report.setProposalId(proposal.getId());
        report.setPeriod(period);
        report.setYear(2026);
        report.setStatus(status);
        report.setReportType("QUARTERLY_PROGRESS");
        report.setSubmittedAt(submittedAt);
        return reportRepository.saveAndFlush(report);
    }

    @Test
    void onlySubmittedReportsOfBudgetProcessedProjectsAreVisible() throws Exception {
        Proposal eligible = proposal("QR-A", "Mangrove Project", "College of Science", "DAGAT", true);
        budgetEndorsement(eligible);
        report(eligible, "Q1", "SUBMITTED", LocalDateTime.of(2026, 2, 1, 8, 0));
        report(eligible, "Q2", "DRAFT", LocalDateTime.of(2026, 3, 1, 8, 0));

        Proposal noBudget = proposal("QR-B", "No Budget Project", "College of Arts", "PUNLA", false);
        report(noBudget, "Q1", "SUBMITTED", LocalDateTime.of(2026, 2, 2, 8, 0));

        Proposal notEndorsed = proposal("QR-C", "Pending Budget Project", "College of Science", "DAGAT", true);
        report(notEndorsed, "Q1", "SUBMITTED", LocalDateTime.of(2026, 2, 3, 8, 0));

        mockMvc.perform(get("/api/ovcaf/quarterly-reports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].documentId").value("QR-A"))
                .andExpect(jsonPath("$.items[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.items[0].report.period").value("Q1"))
                .andExpect(jsonPath("$.items[*].documentId", not(hasItem("QR-B"))))
                .andExpect(jsonPath("$.items[*].documentId", not(hasItem("QR-C"))));
    }

    @Test
    void filtersAndNewestFirstOrderingUseTheSubmittedTimestamp() throws Exception {
        Proposal a = proposal("QR-D", "Alpha Project", "College of Science", "DAGAT", true);
        budgetEndorsement(a);
        report(a, "Q1", "SUBMITTED", LocalDateTime.of(2026, 2, 1, 8, 0));

        Proposal b = proposal("QR-E", "Beta Project", "College of Engineering", "PUNLA", true);
        budgetEndorsement(b);
        report(b, "Q2", "SUBMITTED", LocalDateTime.of(2026, 4, 1, 8, 0));

        mockMvc.perform(get("/api/ovcaf/quarterly-reports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].documentId").value("QR-E"))
                .andExpect(jsonPath("$.items[1].documentId").value("QR-D"))
                .andExpect(jsonPath("$.filterOptions.periods", hasItem("Q1")))
                .andExpect(jsonPath("$.filterOptions.periods", hasItem("Q2")));

        mockMvc.perform(get("/api/ovcaf/quarterly-reports")
                        .param("college", "College of Science")
                        .param("period", "Q1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].documentId").value("QR-D"));
    }

    @Test
    void theOfficeHasNoWriteAccessAndOtherRolesCannotRead() throws Exception {
        Proposal eligible = proposal("QR-F", "Read Only Project", "College of Science", "DAGAT", true);
        budgetEndorsement(eligible);
        report(eligible, "Q1", "SUBMITTED", LocalDateTime.of(2026, 2, 1, 8, 0));

        mockMvc.perform(get("/api/ovcaf/quarterly-reports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + rpsToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/ovcaf/quarterly-reports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isMethodNotAllowed());
    }
}
