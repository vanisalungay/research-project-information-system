package com.rpis.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rpis.backend.dto.OvcafValidationRequest;
import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.model.GadChecklistData;
import com.rpis.backend.model.Notification;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.NotificationRepository;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.TokenService;
import com.rpis.backend.util.NotificationTypes;
import com.rpis.backend.util.RpisTimeZone;
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
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The proposal chain through the real HTTP endpoints with one bearer token per role:
 * submission -> RPS endorsement -> REC office review -> REC approval -> OC final approval
 * with an SO -> OVCAF validation -> release. Every step asserts the recipient rows and
 * that a repeated idempotent transition does not create a duplicate.
 *
 * <p>The workflow is deliberately not one transaction: each request commits exactly as
 * it does in production, and the assertions read the committed notification rows.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationWorkflowAcceptanceApiTest {

    private static final byte[] PDF = "%PDF-1.4\n% e2e special order\n".getBytes(StandardCharsets.US_ASCII);
    private static final String[] GAD_ITEMS = {
            "1.1", "1.2", "2.0", "3.1", "3.2", "4.0", "5.0", "6.1", "6.2",
            "6.3", "7.0", "8.0", "9.1", "9.2", "10.1", "10.2", "10.3"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ProposalAnnouncementRepository announcementRepository;
    @Autowired private ProposalRepository proposalRepository;
    @Autowired private TokenService tokenService;

    private User proponent;
    private User rpsAdmin;
    private User rpsStaff;
    private User ovcrige;
    private User rec;
    private User oc;
    private User ovcaf;
    private String proponentToken;
    private String rpsAdminToken;
    private String rpsStaffToken;
    private String ovcrigeToken;
    private String recToken;
    private String ocToken;
    private String ovcafToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE notifications RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposal_reviews RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE ovcaf_validations RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proposal_announcements RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        proponent = user("E2E Proponent", "PROPONENT");
        rpsAdmin = user("E2E RPS Admin", "RPS_ADMIN");
        rpsStaff = user("E2E RPS Staff", "RPS_STAFF");
        ovcrige = user("E2E OVCRIGE", "OVCRIGE");
        rec = user("E2E REC", "REC");
        oc = user("E2E Chancellor", "OC");
        ovcaf = user("E2E OVCAF", "OVCAF");

        proponentToken = tokenService.generateToken(proponent.getId(), proponent.getRole());
        rpsAdminToken = tokenService.generateToken(rpsAdmin.getId(), rpsAdmin.getRole());
        rpsStaffToken = tokenService.generateToken(rpsStaff.getId(), rpsStaff.getRole());
        ovcrigeToken = tokenService.generateToken(ovcrige.getId(), ovcrige.getRole());
        recToken = tokenService.generateToken(rec.getId(), rec.getRole());
        ocToken = tokenService.generateToken(oc.getId(), oc.getRole());
        ovcafToken = tokenService.generateToken(ovcaf.getId(), ovcaf.getRole());

        ProposalAnnouncement call = new ProposalAnnouncement();
        call.setName("E2E Open Call");
        call.setStartDate(LocalDate.now().minusDays(1));
        call.setEndDate(LocalDate.now().plusDays(30));
        call.setStatus(ProposalAnnouncement.STATUS_ACTIVE);
        announcementRepository.saveAndFlush(call);
    }

    private User user(String name, String role) {
        User user = new User();
        user.setName(name);
        user.setEmail(name.toLowerCase().replace(' ', '-') + "-" + UUID.randomUUID()
                + "@example.edu.ph");
        user.setPassword("secret");
        user.setRole(role);
        user.setStatus("APPROVED");
        return userRepository.saveAndFlush(user);
    }

    private long count(User user, String type, String titleFragment) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .filter(notification -> type.equals(notification.getType()))
                .filter(notification -> notification.getTitle() != null
                        && notification.getTitle().contains(titleFragment))
                .count();
    }

    private ProposalRequest submissionRequest() {
        ProposalRequest request = new ProposalRequest();
        request.setProjectTitle("E2E Notification Proposal");
        request.setDuration("12");
        request.setStatus("SUBMITTED");
        GadChecklistData checklist = new GadChecklistData();
        Map<String, String> responses = new LinkedHashMap<>();
        for (String item : GAD_ITEMS) {
            responses.put(item, "YES");
        }
        checklist.setResponses(responses);
        request.setGadChecklist(checklist);
        return request;
    }

    private JsonNode submitProposal() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/proposals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(submissionRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** A REC review body. An approval must carry every criterion and an overall comment. */
    private String reviewPayload(String decision) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("decision", decision);
        if ("APPROVED".equals(decision)) {
            for (String field : new String[] {
                    "soundnessSectorNeedScore", "soundnessEffectiveSolutionScore",
                    "soundnessReasonableBudgetScore", "soundnessDoableWorkplanScore",
                    "suitabilityCostEffectiveScore", "suitabilityPartnersIdentifiedScore",
                    "suitabilityCommercializationScore", "suitabilityTimelyUtilizationScore",
                    "significanceEconomicImpactScore", "significanceSocialImpactScore",
                    "significanceEnvironmentalImpactScore", "significanceSustainabilityScore",
                    "competenceAlignmentScore", "competenceCollaborationScore",
                    "competenceDeliverablesUnderstandingScore"}) {
                payload.put(field, 4);
            }
            payload.put("overallComments", "The proposal meets the evaluation criteria.");
        }
        return objectMapper.writeValueAsString(payload);
    }

    private Proposal proposalAt(String status, String title) {
        Proposal proposal = new Proposal();
        proposal.setProjectTitle(title);
        proposal.setProponent(proponent);
        proposal.setStatus(status);
        return proposalRepository.saveAndFlush(proposal);
    }

    @Test
    void aRecApprovalCannotBypassTheScoresOrTheComment() throws Exception {
        Proposal proposal = proposalAt("UNDER_REVIEW", "Incomplete REC review");

        // No scores, no comment: the direct API is refused and nothing changes.
        mockMvc.perform(post("/api/proposals/{id}/reviews?reviewerId={reviewerId}",
                                proposal.getId(), rec.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + recToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isBadRequest());
        assertEquals("UNDER_REVIEW",
                proposalRepository.findById(proposal.getId()).orElseThrow().getStatus());

        // Every score, but a whitespace-only comment, is still not an approval.
        ObjectNode whitespace = (ObjectNode) objectMapper.readTree(reviewPayload("APPROVED"));
        whitespace.put("overallComments", "   ");
        mockMvc.perform(post("/api/proposals/{id}/reviews?reviewerId={reviewerId}",
                                proposal.getId(), rec.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + recToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(whitespace)))
                .andExpect(status().isBadRequest());

        // A score outside the 1-5 range is refused even with a comment.
        ObjectNode invalidScore = (ObjectNode) objectMapper.readTree(reviewPayload("APPROVED"));
        invalidScore.put("soundnessSectorNeedScore", 6);
        mockMvc.perform(post("/api/proposals/{id}/reviews?reviewerId={reviewerId}",
                                proposal.getId(), rec.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + recToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidScore)))
                .andExpect(status().isBadRequest());

        // A complete evaluation is accepted and moves the proposal on.
        mockMvc.perform(post("/api/proposals/{id}/reviews?reviewerId={reviewerId}",
                                proposal.getId(), rec.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + recToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewPayload("APPROVED")))
                .andExpect(status().isCreated());
        assertEquals("REC_APPROVED",
                proposalRepository.findById(proposal.getId()).orElseThrow().getStatus());
    }

    @Test
    void aRecRejectionGoesToOrpsAndARecReturnNotifiesOnlyRps() throws Exception {
        Proposal rejected = proposalAt("UNDER_REVIEW", "Will be rejected");
        mockMvc.perform(put("/api/proposals/{id}/reject", rejected.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + recToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remarks\":\"The methodology does not meet the review criteria.\"}"))
                .andExpect(status().isOk());

        // The REC rejection is a hand-off: ORPS is informed, the Proponent is not.
        Proposal handedOff = proposalRepository.findById(rejected.getId()).orElseThrow();
        assertEquals("RETURNED_TO_RPS", handedOff.getStatus());
        assertEquals("REJECTION", handedOff.getReturnKind());
        assertEquals("REC", handedOff.getReturnedByOffice());
        assertEquals("The methodology does not meet the review criteria.",
                handedOff.getReturnRemarks());
        assertEquals(1, count(rpsStaff, NotificationTypes.REJECTION, "Proposal Rejected by REC"));
        assertEquals(0, count(proponent, NotificationTypes.REJECTION, "Proposal Rejected"),
                "a reviewing office must not notify the Proponent directly");

        // Only after ORPS forwards the rejection is the Proponent told.
        mockMvc.perform(put("/api/proposals/{id}/finalize-rejection", rejected.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + rpsStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remarks\":\"Forwarded after ORPS review.\"}"))
                .andExpect(status().isOk());
        assertEquals(1, count(proponent, NotificationTypes.REJECTION, "Proposal Rejected"));

        Proposal returned = proposalAt("UNDER_REVIEW", "Will be returned");
        mockMvc.perform(put("/api/proposals/{id}/return-revision", returned.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + recToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remarks\":\"Please revise the methodology.\","
                                + "\"returnedByOffice\":\"REC\",\"returnedById\":" + rec.getId()
                                + ",\"returnedByName\":\"E2E REC\"}"))
                .andExpect(status().isOk());

        assertEquals(1, count(rpsStaff, NotificationTypes.REVISION, "Revision Request Received"));
        assertEquals(0, count(proponent, NotificationTypes.REVISION, "Proposal Returned for Revision"),
                "an office return goes to RPS first; the Proponent learns when RPS forwards it");
    }

    @Test
    void theProposalChainNotifiesEveryResponsibleOfficeExactlyOnce() throws Exception {
        JsonNode submitted = submitProposal();
        long proposalId = submitted.get("id").asLong();

        assertEquals(1, count(rpsAdmin, NotificationTypes.PROPOSAL_UPDATE, "New Proposal Submitted"));
        assertEquals(1, count(rpsStaff, NotificationTypes.PROPOSAL_UPDATE, "New Proposal Submitted"));
        assertEquals(1, count(proponent, NotificationTypes.PROPOSAL_SUBMISSION, "Proposal Submitted"));
        assertEquals(0, count(ovcrige, NotificationTypes.PROPOSAL_UPDATE, "New Proposal Submitted"));

        mockMvc.perform(put("/api/proposals/{id}/endorse", proposalId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + rpsStaffToken))
                .andExpect(status().isOk());
        assertEquals(1, count(ovcrige, NotificationTypes.PROPOSAL_ENDORSED, "Proposal Endorsed"));
        assertEquals(1, count(proponent, NotificationTypes.PROPOSAL_ENDORSED,
                "Proposal Endorsed by RPS"));

        // Repeating the same transition is a no-op, not a second notification.
        mockMvc.perform(put("/api/proposals/{id}/endorse", proposalId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + rpsStaffToken))
                .andExpect(status().isOk());
        assertEquals(1, count(ovcrige, NotificationTypes.PROPOSAL_ENDORSED, "Proposal Endorsed"));
        assertEquals(1, count(proponent, NotificationTypes.PROPOSAL_ENDORSED,
                "Proposal Endorsed by RPS"));

        mockMvc.perform(put("/api/proposals/{id}/forward-to-rec", proposalId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcrigeToken))
                .andExpect(status().isOk());
        assertEquals(1, count(rec, NotificationTypes.REVIEW_REQUEST, "REC Office Review Request"));
        assertEquals(1, count(proponent, NotificationTypes.REVIEW_REQUEST,
                "Proposal Forwarded to REC"));

        mockMvc.perform(post("/api/proposals/{id}/reviews?reviewerId={reviewerId}", proposalId,
                                rec.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + recToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewPayload("APPROVED")))
                .andExpect(status().isCreated());
        assertEquals(1, count(ovcrige, NotificationTypes.APPROVAL, "Proposal Approved by REC"));
        assertEquals(1, count(proponent, NotificationTypes.APPROVAL, "Proposal Approved by REC"));

        mockMvc.perform(put("/api/proposals/{id}/forward-to-oc", proposalId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcrigeToken))
                .andExpect(status().isOk());
        assertEquals(1, count(oc, NotificationTypes.APPROVAL, "Final Approval Required"));
        assertEquals(1, count(proponent, NotificationTypes.APPROVAL,
                "Proposal Forwarded to the Office of the Chancellor"));

        MockMultipartFile file = new MockMultipartFile(
                "soFile", "E2E-SO.pdf", "application/pdf", PDF);
        mockMvc.perform(multipart("/api/proposals/{id}/approve-with-so", proposalId)
                        .file(file)
                        .param("soNumber", "SO-E2E-001")
                        .param("needsBudget", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk());
        assertEquals(1, count(ovcaf, NotificationTypes.FUNDING, "Proposal for OVCAF Review"));
        assertEquals(1, count(proponent, NotificationTypes.APPROVAL,
                "Proposal Approved by the Chancellor"));

        OvcafValidationRequest validation = new OvcafValidationRequest();
        validation.setValidatedById(ovcaf.getId());
        validation.setDecision("APPROVED_ENDORSED");
        validation.setRemarks("Validated in the E2E workflow.");
        validation.setBudgetComplete(true);
        validation.setBudgetComputationCorrect(true);
        validation.setSupportingDocsComplete(true);
        validation.setExpensesReasonable(true);
        validation.setFundingSourceIdentified(true);
        validation.setComplianceVerified(true);
        mockMvc.perform(post("/api/ovcaf/proposals/{id}/validate", proposalId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(validation)))
                .andExpect(status().isCreated());
        assertEquals(1, count(proponent, NotificationTypes.APPROVAL, "Budget Approved"));
        assertEquals(1, count(rpsStaff, NotificationTypes.PROPOSAL_UPDATE, "Budget Approved"));
        assertEquals(1, count(oc, NotificationTypes.APPROVAL, "Budget Approved by OVCAF"),
                "OVCAF approving the budget must notify the Chancellor's Office exactly once");

        mockMvc.perform(put("/api/ovcaf/proposals/{id}/release?releasedById={releasedById}",
                                proposalId, ovcaf.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isOk());
        assertEquals(1, count(proponent, NotificationTypes.APPROVAL,
                "Proposal Approved for Implementation"));
        assertEquals(1, count(rpsStaff, NotificationTypes.PROPOSAL_UPDATE, "Implementation Monitoring"));

        // Read/unread state and the offset are part of the same persisted contract.
        Notification waiting = notificationRepository
                .findByUserOrderByCreatedAtDesc(rpsStaff).stream()
                .filter(notification -> "New Proposal Submitted".equals(notification.getTitle()))
                .findFirst()
                .orElseThrow();
        assertFalse(Boolean.TRUE.equals(waiting.getIsRead()));

        MvcResult list = mockMvc.perform(get("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + rpsStaffToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listed = objectMapper.readTree(list.getResponse().getContentAsString());
        JsonNode target = null;
        for (JsonNode item : listed) {
            if (item.get("id").asLong() == waiting.getId()) {
                target = item;
            }
        }
        assertNotNull(target);
        String expectedOffset = OffsetDateTime.now(RpisTimeZone.zone()).getOffset().getId();
        assertTrue(target.get("createdAt").asText().endsWith(expectedOffset)
                        || target.get("createdAt").asText().endsWith("Z"),
                "the API must append the configured offset");

        mockMvc.perform(put("/api/notifications/{id}/read", waiting.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + rpsStaffToken))
                .andExpect(status().isOk());
        assertTrue(notificationRepository.findById(waiting.getId()).orElseThrow().getIsRead(),
                "the read state must persist");
    }

    /**
     * Person names submitted through the API are stored in the standard
     * "Last Name, First Name, Middle Initial, Suffix" title case even when the client
     * sends capitals or lower case, so a request that bypasses the browser cannot save
     * an inconsistent name.
     */
    @Test
    void submittedPersonNamesAreStoredInTheStandardFormat() throws Exception {
        ProposalRequest request = submissionRequest();
        request.setProjectLeader("DELA CRUZ, JUAN, M., JR.");
        request.setSubmittedByName("juan dela cruz");
        request.setEndorsedByName("DELA CRUZ, MARIA, M.");

        MvcResult result = mockMvc.perform(post("/api/proposals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("Dela Cruz, Juan, M., Jr.", body.get("projectLeader").asText());
        assertEquals("Juan Dela Cruz", body.get("submittedByName").asText());
        assertEquals("Dela Cruz, Maria, M.", body.get("endorsedByName").asText());

        Proposal stored = proposalRepository.findById(body.get("id").asLong()).orElseThrow();
        assertEquals("Dela Cruz, Juan, M., Jr.", stored.getProjectLeader());
        assertEquals("Juan Dela Cruz", stored.getSubmittedByName());
        assertEquals("Dela Cruz, Maria, M.", stored.getEndorsedByName());
    }

    /**
     * Recording the reviewer and endorsing to OVCRIGE are RPS actions. Another office -
     * and the Proponent - must be refused, and the refusal must leave the proposal
     * untouched.
     */
    @Test
    void onlyRpsMayEndorseToOvcrige() throws Exception {
        Proposal proposal = proposalAt("FOR REVIEW", "RPS-only endorsement");

        mockMvc.perform(put("/api/proposals/{id}/endorse", proposal.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcrigeToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/proposals/{id}/endorse", proposal.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/proposals/{id}/reviewer-info", proposal.getId())
                        .param("reviewedBy", "E2E OVCRIGE")
                        .param("reviewedByPosition", "RPS_STAFF")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcrigeToken))
                .andExpect(status().isForbidden());

        assertEquals("FOR REVIEW",
                proposalRepository.findById(proposal.getId()).orElseThrow().getStatus());
        assertEquals(0, count(ovcrige, NotificationTypes.PROPOSAL_ENDORSED, "Proposal Endorsed"));
    }

    /**
     * Only the submitted state can be endorsed. A draft (or any later state) must be
     * refused rather than advanced, and no notification may be written.
     */
    @Test
    void aProposalThatIsNotAwaitingRpsReviewIsNotEndorsed() throws Exception {
        Proposal draft = proposalAt("DRAFT", "Not yet submitted");

        mockMvc.perform(put("/api/proposals/{id}/endorse", draft.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + rpsStaffToken))
                .andExpect(status().isBadRequest());

        assertEquals("DRAFT",
                proposalRepository.findById(draft.getId()).orElseThrow().getStatus());
        assertEquals(0, count(ovcrige, NotificationTypes.PROPOSAL_ENDORSED, "Proposal Endorsed"));
    }
}
