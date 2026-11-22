package com.rpis.backend.controller;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ORPS Rejected Proposals queue. The Date Processed values come from the audited
 * transitions: the hand-off into RETURNED_TO_RPS for a pending rejection, and the
 * finalization into REJECTED for a processed one. Ordinary revision returns never
 * appear in the rejection queue.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RejectionQueueApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProposalRepository proposalRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String rpsToken;
    private String ovcafToken;
    private User proponent;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        proponent = user("Rejection Proponent", "rejection.proponent@msunaawan.edu.ph", "PROPONENT");
        User rps = user("Rejection ORPS", "rejection.rps@msunaawan.edu.ph", "RPS_STAFF");
        User ovcaf = user("Rejection OVCAF", "rejection.ovcaf@msunaawan.edu.ph", "OVCAF");
        rpsToken = tokenService.generateToken(rps.getId(), "RPS_STAFF");
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

    private Proposal proposal(String documentId, String title, String status, String returnKind,
            String office, LocalDateTime returnedAt) {
        Proposal proposal = new Proposal();
        proposal.setDocumentId(documentId);
        proposal.setProjectTitle(title);
        proposal.setProjectLeader("Leader " + title);
        proposal.setCollege("College of Science");
        proposal.setStatus(status);
        proposal.setReturnKind(returnKind);
        proposal.setReturnedByOffice(office);
        proposal.setReturnedAt(returnedAt);
        proposal.setReturnRemarks("Rejection reason for " + title);
        proposal.setProponent(proponent);
        return proposalRepository.saveAndFlush(proposal);
    }

    private void transition(Proposal proposal, String from, String to, LocalDateTime at) {
        jdbcTemplate.update(
                "INSERT INTO proposal_status_history "
                        + "(proposal_id, from_status, to_status, actor_role, actor_name, source, changed_at) "
                        + "VALUES (?, ?, ?, 'OVCAF', 'Reviewer', 'APPLICATION', ?)",
                proposal.getId(), from, to, at);
    }

    @Test
    void pendingAndFinalizedRejectionsCarryTheirWorkflowDates() throws Exception {
        Proposal pending = proposal("REJ-PENDING", "Pending Rejection", "RETURNED_TO_RPS",
                "REJECTION", "OVCAF", LocalDateTime.of(2026, 10, 1, 9, 0));
        transition(pending, "FOR_OVCAF_APPROVAL", "RETURNED_TO_RPS",
                LocalDateTime.of(2026, 10, 1, 9, 0));

        Proposal finalized = proposal("REJ-DONE", "Finalized Rejection", "REJECTED",
                "REJECTION", "OVCRIGE", LocalDateTime.of(2026, 9, 1, 9, 0));
        transition(finalized, "FOR_OVCAF_APPROVAL", "RETURNED_TO_RPS",
                LocalDateTime.of(2026, 9, 1, 9, 0));
        transition(finalized, "RETURNED_TO_RPS", "REJECTED",
                LocalDateTime.of(2026, 10, 5, 14, 30));

        // A revision return shares RETURNED_TO_RPS but is not a rejection.
        proposal("REJ-REV", "Revision Return", "RETURNED_TO_RPS", "REVISION", "OVCRIGE",
                LocalDateTime.of(2026, 10, 2, 9, 0));

        mockMvc.perform(get("/api/proposals/rejections")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + rpsToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].documentId", hasItem("REJ-PENDING")))
                .andExpect(jsonPath("$[*].documentId", hasItem("REJ-DONE")))
                .andExpect(jsonPath("$[*].documentId", not(hasItem("REJ-REV"))))
                // Newest processed/routed first: the finalized rejection was forwarded last.
                .andExpect(jsonPath("$[0].documentId").value("REJ-DONE"))
                .andExpect(jsonPath("$[0].processedAt").value("2026-10-05T14:30:00"))
                .andExpect(jsonPath("$[0].returnedByOffice").value("OVCRIGE"))
                .andExpect(jsonPath("$[1].documentId").value("REJ-PENDING"))
                .andExpect(jsonPath("$[1].routedAt").value("2026-10-01T09:00:00"))
                .andExpect(jsonPath("$[1].processedAt").doesNotExist())
                .andExpect(jsonPath("$[1].returnedByOffice").value("OVCAF"));
    }

    @Test
    void onlyOrpsCanReadTheRejectionQueue() throws Exception {
        mockMvc.perform(get("/api/proposals/rejections")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isForbidden());
    }
}
