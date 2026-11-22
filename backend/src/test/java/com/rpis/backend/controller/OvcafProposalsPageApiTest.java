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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The OVCAF Proposals page: Date Received is the audited OC -> OVCAF event (falling
 * back only to the OC Special Order timestamp), and filtering, ordering and paging are
 * resolved on the server against the actual records.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OvcafProposalsPageApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProposalRepository proposalRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String ovcafToken;
    private String ocToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        User proponent = user("Page Proponent", "page.proponent@msunaawan.edu.ph", "PROPONENT");
        User ovcaf = user("Page OVCAF", "page.ovcaf@msunaawan.edu.ph", "OVCAF");
        User oc = user("Page Chancellor", "page.oc@msunaawan.edu.ph", "OC");
        ovcafToken = tokenService.generateToken(ovcaf.getId(), "OVCAF");
        ocToken = tokenService.generateToken(oc.getId(), "OC");
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
            String status, LocalDateTime soUploadedAt) {
        Proposal proposal = new Proposal();
        proposal.setDocumentId(documentId);
        proposal.setProjectTitle(title);
        proposal.setProjectLeader("Leader " + title);
        proposal.setCollege(college);
        proposal.setProgramTitle(program);
        proposal.setStatus(status);
        proposal.setSoNumber("SO-" + documentId);
        proposal.setSoUploadedAt(soUploadedAt);
        proposal.setProponent(userRepository.findAll().stream()
                .filter(u -> "PROPONENT".equals(u.getRole())).findFirst().orElseThrow());
        return proposalRepository.saveAndFlush(proposal);
    }

    private void receiptEvent(Proposal proposal, LocalDateTime changedAt) {
        jdbcTemplate.update(
                "INSERT INTO proposal_status_history "
                        + "(proposal_id, from_status, to_status, actor_role, actor_name, source, changed_at) "
                        + "VALUES (?, 'FOR_OC_APPROVAL', 'FOR_OVCAF_APPROVAL', 'OC', 'Chancellor', "
                        + "'APPLICATION', ?)",
                proposal.getId(), changedAt);
    }

    @Test
    void dateReceivedIsTheRealEventAndFallbackAndOrdersNewestFirst() throws Exception {
        Proposal oldest = proposal("PAGE-A", "Alpha", "College of Science", "DAGAT",
                "FOR_OVCAF_APPROVAL", LocalDateTime.of(2026, 1, 1, 8, 0));
        Proposal newest = proposal("PAGE-B", "Beta", "College of Engineering", "PUNLA",
                "FOR_OVCAF_APPROVAL", LocalDateTime.of(2026, 2, 1, 8, 0));
        Proposal fallback = proposal("PAGE-C", "Gamma", "College of Arts", "DAGAT",
                "FOR_OVCAF_APPROVAL", LocalDateTime.of(2026, 3, 1, 8, 0));
        receiptEvent(oldest, LocalDateTime.of(2026, 1, 10, 9, 0));
        receiptEvent(newest, LocalDateTime.of(2026, 3, 10, 9, 0));
        // No history row for `fallback`: the OC SO timestamp is the recorded receipt.

        mockMvc.perform(get("/api/ovcaf/proposals/page")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.items[0].documentId").value("PAGE-B"))
                .andExpect(jsonPath("$.items[0].receivedAt").value("2026-03-10T09:00:00"))
                .andExpect(jsonPath("$.items[1].documentId").value("PAGE-C"))
                .andExpect(jsonPath("$.items[1].receivedAt").value("2026-03-01T08:00:00"))
                .andExpect(jsonPath("$.items[2].documentId").value("PAGE-A"))
                .andExpect(jsonPath("$.items[2].receivedAt").value("2026-01-10T09:00:00"))
                .andExpect(jsonPath("$.filterOptions.colleges", hasItem("College of Science")))
                .andExpect(jsonPath("$.filterOptions.programs", hasItem("PUNLA")));
    }

    @Test
    void filtersAndSearchOperateOnTheServerRecords() throws Exception {
        proposal("PAGE-D", "Mangrove Study", "College of Science", "DAGAT",
                "FOR_OVCAF_APPROVAL", LocalDateTime.of(2026, 2, 1, 8, 0));
        proposal("PAGE-E", "Water Study", "College of Engineering", "PUNLA",
                "READY_FOR_RELEASE", LocalDateTime.of(2026, 2, 2, 8, 0));
        proposal("PAGE-F", "Coastal Study", "College of Science", "PUNLA",
                "APPROVED", LocalDateTime.of(2026, 2, 3, 8, 0));

        mockMvc.perform(get("/api/ovcaf/proposals/page")
                        .param("college", "College of Science")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[*].documentId", hasItem("PAGE-D")))
                .andExpect(jsonPath("$.items[*].documentId", hasItem("PAGE-F")))
                .andExpect(jsonPath("$.items[*].documentId", not(hasItem("PAGE-E"))));

        mockMvc.perform(get("/api/ovcaf/proposals/page")
                        .param("program", "PUNLA")
                        .param("status", "APPROVED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].documentId").value("PAGE-F"));

        mockMvc.perform(get("/api/ovcaf/proposals/page")
                        .param("search", "water")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].documentId").value("PAGE-E"));
    }

    @Test
    void paginationIsAppliedAfterFilteringAndSorting() throws Exception {
        for (int i = 1; i <= 3; i++) {
            proposal("PAGE-P" + i, "Project " + i, "College of Science", "DAGAT",
                    "FOR_OVCAF_APPROVAL", LocalDateTime.of(2026, 1, i, 8, 0));
        }

        mockMvc.perform(get("/api/ovcaf/proposals/page")
                        .param("size", "2")
                        .param("page", "2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ovcafToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void onlyOvcafCanReadTheList() throws Exception {
        mockMvc.perform(get("/api/ovcaf/proposals/page")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ocToken))
                .andExpect(status().isForbidden());
    }
}
