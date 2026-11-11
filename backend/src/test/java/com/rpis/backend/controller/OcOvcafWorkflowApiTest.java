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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
            draftRequest(Long id, MockMultipartFile file, String soNumber, String needsBudget,
                         String notes) {
        var builder = multipart("/api/proposals/{id}/special-order-draft", id);
        if (file != null) {
            builder.file(file);
        }
        if (soNumber != null) {
            builder.param("soNumber", soNumber);
        }
        if (needsBudget != null) {
            builder.param("needsBudget", needsBudget);
        }
        if (notes != null) {
            builder.param("chancellorNotes", notes);
        }
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });
        return builder;
    }

    /**
     * A draft is staged for the record but never counts as the OC approval: the
     * status stays FOR_OC_APPROVAL, no SO date is written and OVCAF cannot see the
     * proposal. Finalizing afterwards issues the same staged record exactly once.
     */
    @Test
    void aDraftIsPersistedWithoutCompletingTheOcApprovalAndCanThenBeFinalized() throws Exception {
        Proposal proposal = proposal("FOR_OC_APPROVAL");
        MockMultipartFile file = new MockMultipartFile("soFile", "Draft SO.pdf", "application/pdf", PDF);

        mockMvc.perform(draftRequest(proposal.getId(), file, "SO-DRAFT-1", "true", "draft notes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposal.status").value("FOR_OC_APPROVAL"))
                .andExpect(jsonPath("$.proposal.soNumber").value("SO-DRAFT-1"));

        proposalRepository.flush();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, so_number, so_uploaded_at, so_file_name, so_file_path, needs_budget, remarks "
                        + "FROM proposals WHERE id = ?", proposal.getId());
        assertEquals("FOR_OC_APPROVAL", row.get("status"), "a draft must not advance the status");
        assertEquals("SO-DRAFT-1", row.get("so_number"));
        assertNull(row.get("so_uploaded_at"), "a draft must never record an issued date");
        assertEquals("Draft SO.pdf", row.get("so_file_name"));
        assertEquals(Boolean.TRUE, row.get("needs_budget"));
        assertEquals("draft notes", row.get("remarks"));
        String draftPath = (String) row.get("so_file_path");
        assertNotNull(draftPath);
        assertTrue(uploadStorage.exists(draftPath), "the staged document must exist on disk");

        // The draft satisfies none of OVCAF's gates.
        mockMvc.perform(get("/api/ovcaf/proposals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id",
                        org.hamcrest.Matchers.not(hasItem(proposal.getId().intValue()))));
        mockMvc.perform(get("/api/ovcaf/proposals/{id}", proposal.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isNotFound());

        // Finalizing issues the staged SO and keeps its document.
        mockMvc.perform(approveRequest(proposal.getId(), null, "SO-DRAFT-1", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposal.status").value("FOR_OVCAF_APPROVAL"));

        proposalRepository.flush();
        Map<String, Object> finalized = jdbcTemplate.queryForMap(
                "SELECT status, so_number, so_uploaded_at, so_file_path FROM proposals WHERE id = ?",
                proposal.getId());
        assertEquals("FOR_OVCAF_APPROVAL", finalized.get("status"));
        assertEquals("SO-DRAFT-1", finalized.get("so_number"));
        assertNotNull(finalized.get("so_uploaded_at"), "final approval records the issued date");
        assertEquals(draftPath, finalized.get("so_file_path"),
                "final approval must keep the staged document");
        assertTrue(uploadStorage.exists(draftPath));
    }

    @Test
    void onlyTheChancellorCanSaveADraft() throws Exception {
        Proposal proposal = proposal("FOR_OC_APPROVAL");

        mockMvc.perform(draftRequest(proposal.getId(), null, "SO-DRAFT-2", null, null)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken))
                .andExpect(status().isForbidden());

        proposalRepository.flush();
        assertNull(jdbcTemplate.queryForObject(
                "SELECT so_number FROM proposals WHERE id = ?", String.class, proposal.getId()));
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

    /**
     * A proposal has exactly one budget classification. A request that submits both
     * values is the ambiguous state the workflow forbids, so it is rejected before
     * anything is written - the server must never silently keep the first value.
     */
    @Test
    void aRequestThatSubmitsBothBudgetClassificationsIsRejected() throws Exception {
        Proposal proposal = proposal("FOR_OC_APPROVAL");

        mockMvc.perform(approveRequest(proposal.getId(), null, "SO-2026-070", "true")
                        .param("needsBudget", "false")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("exactly one budget classification")));

        proposalRepository.flush();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, needs_budget, so_number FROM proposals WHERE id = ?", proposal.getId());
        assertEquals("FOR_OC_APPROVAL", row.get("status"), "a rejected classification must not advance the status");
        assertNull(row.get("needs_budget"), "a rejected classification must not be stored");
        assertNull(row.get("so_number"));
    }

    /** An unrecognised classification is not a valid value and is refused. */
    @Test
    void anInvalidBudgetClassificationIsRejected() throws Exception {
        Proposal proposal = proposal("FOR_OC_APPROVAL");

        mockMvc.perform(approveRequest(proposal.getId(), null, "SO-2026-071", "maybe")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("WITH BUDGET")));

        assertEquals("FOR_OC_APPROVAL", jdbcTemplate.queryForObject(
                "SELECT status FROM proposals WHERE id = ?", String.class, proposal.getId()));
    }

    /** Approval cannot run without a classification at all (checked on both layers). */
    @Test
    void approvalWithoutABudgetClassificationIsRejected() throws Exception {
        Proposal proposal = proposal("FOR_OC_APPROVAL");

        mockMvc.perform(approveRequest(proposal.getId(), null, "SO-2026-072", null)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        proposalRepository.flush();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, needs_budget, so_number FROM proposals WHERE id = ?", proposal.getId());
        assertEquals("FOR_OC_APPROVAL", row.get("status"));
        assertNull(row.get("needs_budget"));
        assertNull(row.get("so_number"));
    }

    /** The same two guards apply to the draft endpoint, which can store the classification. */
    @Test
    void aDraftThatSubmitsBothBudgetClassificationsIsRejected() throws Exception {
        Proposal proposal = proposal("FOR_OC_APPROVAL");

        mockMvc.perform(draftRequest(proposal.getId(), null, "SO-DRAFT-070", "true", null)
                        .param("needsBudget", "false")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("exactly one budget classification")));

        proposalRepository.flush();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, needs_budget, so_number FROM proposals WHERE id = ?", proposal.getId());
        assertEquals("FOR_OC_APPROVAL", row.get("status"));
        assertNull(row.get("needs_budget"));
        assertNull(row.get("so_number"));
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

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            returnRequest(Long id, String remarks) {
        var builder = put("/api/proposals/{id}/return-to-ovcrige", id)
                .contentType(MediaType.APPLICATION_JSON);
        if (remarks != null) {
            builder.content("{\"remarks\":\"" + remarks + "\"}");
        }
        return builder;
    }

    private int historyCount(Long proposalId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM proposal_status_history WHERE proposal_id = ?",
                Integer.class, proposalId);
        return count == null ? 0 : count;
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

    /**
     * The Chancellor's Office returning a proposal is a real transition into the
     * existing OVCRIGE-owned state, recorded with its actor, remarks and time.
     */
    @Test
    void theChancellorCanReturnAProposalToOvcrigeAndTheTransitionIsPersisted() throws Exception {
        Proposal proposal = proposal("FOR_OC_APPROVAL");

        mockMvc.perform(returnRequest(proposal.getId(), "Please re-check the budget routing.")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposal.status").value("REC_APPROVED"))
                .andExpect(jsonPath("$.message").exists());

        proposalRepository.flush();
        assertEquals("REC_APPROVED", jdbcTemplate.queryForObject(
                "SELECT status FROM proposals WHERE id = ?", String.class, proposal.getId()));

        Map<String, Object> history = jdbcTemplate.queryForMap(
                "SELECT from_status, to_status, actor_role, actor_name, remarks, source, changed_at "
                        + "FROM proposal_status_history WHERE proposal_id = ?", proposal.getId());
        assertEquals("FOR_OC_APPROVAL", history.get("from_status"));
        assertEquals("REC_APPROVED", history.get("to_status"));
        assertEquals("OC", history.get("actor_role"), "the office that acted is recorded");
        assertEquals("Chancellor", history.get("actor_name"));
        assertEquals("Please re-check the budget routing.", history.get("remarks"));
        assertEquals("APPLICATION", history.get("source"));
        assertNotNull(history.get("changed_at"), "the transition is dated");
    }

    @Test
    void onlyTheChancellorCanReturnAProposalToOvcrige() throws Exception {
        Proposal proposal = proposal("FOR_OC_APPROVAL");

        mockMvc.perform(returnRequest(proposal.getId(), "not allowed")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken))
                .andExpect(status().isForbidden());

        proposalRepository.flush();
        assertEquals("FOR_OC_APPROVAL", jdbcTemplate.queryForObject(
                "SELECT status FROM proposals WHERE id = ?", String.class, proposal.getId()));
        assertEquals(0, historyCount(proposal.getId()), "a refused action records nothing");
    }

    @Test
    void aSecondReturnIsRefusedAndRecordsOnlyOneTransition() throws Exception {
        Proposal proposal = proposal("FOR_OC_APPROVAL");

        mockMvc.perform(returnRequest(proposal.getId(), null)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken))
                .andExpect(status().isOk());
        mockMvc.perform(returnRequest(proposal.getId(), null)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());

        proposalRepository.flush();
        assertEquals("REC_APPROVED", jdbcTemplate.queryForObject(
                "SELECT status FROM proposals WHERE id = ?", String.class, proposal.getId()));
        assertEquals(1, historyCount(proposal.getId()));
    }

    @Test
    void aProposalThatIsNotAwaitingTheChancellorCannotBeReturned() throws Exception {
        Proposal proposal = proposal("FOR_OVCAF_APPROVAL");

        mockMvc.perform(returnRequest(proposal.getId(), null)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());

        proposalRepository.flush();
        assertEquals("FOR_OVCAF_APPROVAL", jdbcTemplate.queryForObject(
                "SELECT status FROM proposals WHERE id = ?", String.class, proposal.getId()));
        assertEquals(0, historyCount(proposal.getId()));
    }

    /** An approval is audited too: the same transition the status shows is recorded. */
    @Test
    void anOcApprovalRecordsItsTransitionInTheHistoryTable() throws Exception {
        Proposal proposal = proposal("FOR_OC_APPROVAL");

        mockMvc.perform(approveRequest(proposal.getId(), null, "SO-2026-200", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken))
                .andExpect(status().isOk());

        proposalRepository.flush();
        Map<String, Object> history = jdbcTemplate.queryForMap(
                "SELECT from_status, to_status, actor_role, actor_name "
                        + "FROM proposal_status_history WHERE proposal_id = ?", proposal.getId());
        assertEquals("FOR_OC_APPROVAL", history.get("from_status"));
        assertEquals("FOR_OVCAF_APPROVAL", history.get("to_status"));
        assertEquals("OC", history.get("actor_role"));
        assertEquals("Chancellor", history.get("actor_name"));
        assertEquals(1, historyCount(proposal.getId()));
    }
}
