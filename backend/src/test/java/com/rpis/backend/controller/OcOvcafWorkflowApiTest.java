package com.rpis.backend.controller;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.OvcafValidationRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.TokenService;
import com.rpis.backend.storage.UploadStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end HTTP verification of the corrected OC -&gt; OVCAF workflow against the
 * isolated {@code rpis_test} database and the real Spring Security filter chain.
 *
 * <p>This is the acceptance test that a unit test cannot replace: it asserts the
 * actual database transition and the actual API responses of
 * <ol>
 *   <li>the Chancellor's Office recording an SO (and the file it stores);</li>
 *   <li>OVCAF seeing the proposal only after that record exists;</li>
 *   <li>OVCAF's own approval being recorded separately and advancing the proposal;</li>
 *   <li>the proposal becoming eligible for quarterly reporting only after the full
 *       chain, and a failed OC approval leaving everything unchanged.</li>
 * </ol>
 *
 * <p>Deliberately <em>not</em> {@code @Transactional}: each request must commit its own
 * transaction (or be rolled back by it) exactly as in production. The assertions then
 * read the committed database state, so a status or SO that only existed inside a test
 * transaction could never be mistaken for a persisted transition.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OcOvcafWorkflowApiTest {

    private static final byte[] PDF = "%PDF-1.4\n% special order\n".getBytes(StandardCharsets.US_ASCII);

    @Autowired private MockMvc mockMvc;
    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProposalRepository proposalRepository;
    @Autowired private OvcafValidationRepository validationRepository;
    @Autowired private UploadStorage uploadStorage;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String ocToken;
    private String ovcafToken;
    private String proponentToken;
    private User proponent;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        proponent = user("Maria Proponent", "maria.oc.workflow@msunaawan.edu.ph", "PROPONENT");
        User oc = user("Chancellor", "chancellor.oc.workflow@msunaawan.edu.ph", "OC");
        User ovcaf = user("OVCAF Reviewer", "ovcaf.oc.workflow@msunaawan.edu.ph", "OVCAF");

        proponentToken = tokenService.generateToken(proponent.getId(), "PROPONENT");
        ocToken = tokenService.generateToken(oc.getId(), "OC");
        ovcafToken = tokenService.generateToken(ovcaf.getId(), "OVCAF");
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

    private Proposal proposal(String status) {
        Proposal proposal = new Proposal();
        proposal.setDocumentId("TEST-OC-OVCAF-" + System.nanoTime());
        proposal.setProjectTitle("Controlled Workflow Proposal");
        proposal.setProjectLeader("Maria Proponent");
        proposal.setCollege("College of Science");
        proposal.setBeneficiariesFileName("Line-Item Budget.docx");
        proposal.setBeneficiariesFilePath("budget-files/test_line-item-budget.docx");
        proposal.setStatus(status);
        proposal.setProponent(proponent);
        return proposalRepository.saveAndFlush(proposal);
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
            approveRequest(Long id, MockMultipartFile file, String soNumber, String needsBudget) {
        var builder = multipart("/api/proposals/{id}/approve-with-so", id);
        if (file != null) {
            builder.file(file);
        }
        if (soNumber != null) {
            builder.param("soNumber", soNumber);
        }
        if (needsBudget != null) {
            builder.param("needsBudget", needsBudget);
        }
        // The endpoint is a PUT; multipart() builds a POST by default. The method is
        // overridden last because with(...) returns the base builder type.
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });
        return builder;
    }

    @Test
    void theFullOcToOvcafToReportingChainIsPersisted() throws Exception {
        Proposal proposal = proposal("FOR_OC_APPROVAL");
        MockMultipartFile file = new MockMultipartFile("soFile", "SO No. 42.pdf", "application/pdf", PDF);

        mockMvc.perform(approveRequest(proposal.getId(), file, "SO-2026-042", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposal.status").value("FOR_OVCAF_APPROVAL"))
                .andExpect(jsonPath("$.proposal.soNumber").value("SO-2026-042"));

        proposalRepository.flush();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, so_number, so_uploaded_at, so_file_name, so_file_path, needs_budget "
                        + "FROM proposals WHERE id = ?", proposal.getId());
        assertEquals("FOR_OVCAF_APPROVAL", row.get("status"));
        assertEquals("SO-2026-042", row.get("so_number"));
        assertNotNull(row.get("so_uploaded_at"), "the OC approval time must be persisted");
        assertEquals("SO No. 42.pdf", row.get("so_file_name"));
        assertEquals(Boolean.TRUE, row.get("needs_budget"));
        String storedPath = (String) row.get("so_file_path");
        assertNotNull(storedPath);
        assertTrue(uploadStorage.exists(storedPath), "the issued SO document must exist on disk");

        // OVCAF can see it only now.
        mockMvc.perform(get("/api/ovcaf/proposals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(proposal.getId().intValue())));
        mockMvc.perform(get("/api/ovcaf/proposals/{id}", proposal.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soNumber").value("SO-2026-042"))
                .andExpect(jsonPath("$.beneficiariesFilePath").value("budget-files/test_line-item-budget.docx"));

        // OVCAF records its own decision, which advances the proposal.
        String body = "{\"decision\":\"APPROVED_ENDORSED\",\"budgetComplete\":true,"
                + "\"budgetComputationCorrect\":true,\"supportingDocsComplete\":true,"
                + "\"expensesReasonable\":true,\"fundingSourceIdentified\":true,"
                + "\"complianceVerified\":true}";
        mockMvc.perform(post("/api/ovcaf/proposals/{id}/validate", proposal.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decision").value("APPROVED_ENDORSED"));

        proposalRepository.flush();
        Map<String, Object> after = jdbcTemplate.queryForMap(
                "SELECT status FROM proposals WHERE id = ?", proposal.getId());
        assertEquals("APPROVED", after.get("status"));
        assertTrue(validationRepository.findTopByProposalIdOrderByCreatedAtDesc(proposal.getId())
                        .orElseThrow().getBudgetEndorsed(),
                "OVCAF's own approval record must be persisted");

        // Only the complete chain makes quarterly reporting available.
        mockMvc.perform(get("/api/quarterly-reports/next-stage/{id}", proposal.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("Q1"));
    }

    @Test
    void aFailedOcApprovalChangesNothingAndDoesNotExposeTheProposal() throws Exception {
        Proposal proposal = proposal("FOR_OC_APPROVAL");
        MockMultipartFile file = new MockMultipartFile("soFile", "SO.pdf", "application/pdf", PDF);

        mockMvc.perform(approveRequest(proposal.getId(), file, "   ", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        proposalRepository.flush();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, so_number, so_uploaded_at, so_file_path FROM proposals WHERE id = ?",
                proposal.getId());
        assertEquals("FOR_OC_APPROVAL", row.get("status"), "a failed approval must not advance the status");
        assertNull(row.get("so_number"), "a failed approval must not create an SO record");
        assertNull(row.get("so_uploaded_at"));
        assertNull(row.get("so_file_path"));

        mockMvc.perform(get("/api/ovcaf/proposals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.not(hasItem(proposal.getId().intValue()))));
        mockMvc.perform(get("/api/ovcaf/proposals/{id}", proposal.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void ovcafCannotApproveAProposalWithoutTheOcApprovalAndSo() throws Exception {
        Proposal proposal = proposal("FOR_OVCAF_APPROVAL");
        proposal.setSoNumber(null);
        proposal.setSoUploadedAt(null);
        proposalRepository.saveAndFlush(proposal);

        mockMvc.perform(post("/api/ovcaf/proposals/{id}/validate", proposal.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED_ENDORSED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());

        proposalRepository.flush();
        assertEquals("FOR_OVCAF_APPROVAL", jdbcTemplate.queryForObject(
                "SELECT status FROM proposals WHERE id = ?", String.class, proposal.getId()));
        assertTrue(validationRepository.findByProposalId(proposal.getId()).isEmpty(),
                "a refused OVCAF approval must not create a validation record");
    }

    @Test
    void onlyTheChancellorCanApproveWithAnSo() throws Exception {
        Proposal proposal = proposal("FOR_OC_APPROVAL");

        mockMvc.perform(approveRequest(proposal.getId(), null, "SO-2026-099", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken))
                .andExpect(status().isForbidden());

        proposalRepository.flush();
        assertEquals("FOR_OC_APPROVAL", jdbcTemplate.queryForObject(
                "SELECT status FROM proposals WHERE id = ?", String.class, proposal.getId()));
        assertNull(jdbcTemplate.queryForObject(
                "SELECT so_number FROM proposals WHERE id = ?", String.class, proposal.getId()));
    }

    @Test
    void aDuplicateApprovalIsRefusedAndDoesNotOverwriteTheRecordedSo() throws Exception {
        Proposal proposal = proposal("FOR_OC_APPROVAL");
        MockMultipartFile file = new MockMultipartFile("soFile", "SO.pdf", "application/pdf", PDF);

        mockMvc.perform(approveRequest(proposal.getId(), file, "SO-2026-100", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken))
                .andExpect(status().isOk());

        mockMvc.perform(approveRequest(proposal.getId(), file, "SO-DIFFERENT", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());

        proposalRepository.flush();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT so_number, status FROM proposals WHERE id = ?", proposal.getId());
        assertEquals("SO-2026-100", row.get("so_number"));
        assertEquals("FOR_OVCAF_APPROVAL", row.get("status"));
    }
}
