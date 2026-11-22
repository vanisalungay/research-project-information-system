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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The paginated proposal list used by the RPS Submitted table. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProposalPageApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProposalRepository proposalRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String rpsToken;
    private User proponent;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
        proponent = user("Page Proponent", "page.proponent@msunaawan.edu.ph", "PROPONENT");
        User rps = user("Page RPS", "page.rps@msunaawan.edu.ph", "RPS_STAFF");
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

    private Proposal proposal(String documentId, String title, String status) {
        Proposal proposal = new Proposal();
        proposal.setDocumentId(documentId);
        proposal.setProjectTitle(title);
        proposal.setProjectLeader("Leader " + title);
        proposal.setStatus(status);
        proposal.setProponent(proponent);
        return proposalRepository.saveAndFlush(proposal);
    }

    @Test
    void theEndpointPagesFiltersAndSearchesServerSide() throws Exception {
        proposal("PG-1", "Mangrove Study", "FOR REVIEW");
        proposal("PG-2", "Water Study", "FOR REVIEW");
        proposal("PG-3", "Coastal Study", "FOR REVIEW");
        proposal("PG-4", "Endorsed Study", "ENDORSED");

        mockMvc.perform(get("/api/proposals/page")
                        .param("statusIn", "FOR REVIEW")
                        .param("sort", "submittedAt")
                        .param("page", "2")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + rpsToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[*].documentId", not(hasItem("PG-4"))));

        mockMvc.perform(get("/api/proposals/page")
                        .param("statusIn", "FOR REVIEW")
                        .param("search", "water")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + rpsToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].documentId").value("PG-2"));

        mockMvc.perform(get("/api/proposals/page")
                        .param("statusIn", "FOR REVIEW")
                        .param("status", "REVISION")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + rpsToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }
}
