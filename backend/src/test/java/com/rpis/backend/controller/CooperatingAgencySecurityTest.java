package com.rpis.backend.controller;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.CooperatingAgencyRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization of the cooperating agency directory, exercised through the real
 * Spring Security filter chain: an anonymous caller can neither read nor change the
 * directory, while any authenticated user (here a Faculty Proponent, the role the
 * Create Proposal form is used with) can read it and add a missing agency. A
 * proponent can only read the saved selection of their own proposal.
 * <p>
 * Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CooperatingAgencySecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private CooperatingAgencyRepository cooperatingAgencyRepository;
    @Autowired
    private ProposalRepository proposalRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User proponent;
    private User otherProponent;
    private String proponentToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        proponent = proponent("Security Proponent", "security@msunaawan.edu.ph");
        otherProponent = proponent("Other Proponent", "other.security@msunaawan.edu.ph");
        proponentToken = tokenService.generateToken(proponent.getId(), "PROPONENT");
    }

    private User proponent(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("secret");
        user.setRole("PROPONENT");
        user.setStatus("APPROVED");
        return userRepository.saveAndFlush(user);
    }

    private Proposal proposal(String title, User owner) {
        Proposal proposal = new Proposal();
        proposal.setProponent(owner);
        proposal.setProjectTitle(title);
        proposal.setStatus("DRAFT");
        return proposalRepository.saveAndFlush(proposal);
    }

    private static byte[] body(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void anAnonymousCallerCannotChangeTheDirectory() throws Exception {
        int before = cooperatingAgencyRepository.findAll().size();

        mockMvc.perform(post("/api/cooperating-agencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"name\":\"Anonymous Agency\"}")))
                .andExpect(status().isForbidden());

        assertEquals(before, cooperatingAgencyRepository.findAll().size());
    }

    @Test
    void anAnonymousCallerCannotReadTheDirectory() throws Exception {
        mockMvc.perform(get("/api/cooperating-agencies")).andExpect(status().isForbidden());
    }

    @Test
    void anAuthenticatedProponentCanReadTheDirectoryAndAddAnAgency() throws Exception {
        mockMvc.perform(get("/api/cooperating-agencies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Commission on Higher Education (CHED)"));

        mockMvc.perform(post("/api/cooperating-agencies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"name\":\"Bureau of Fisheries and Aquatic Resources (BFAR)\"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Bureau of Fisheries and Aquatic Resources (BFAR)"));

        // Adding the same agency again in another case is a conflict, not a new row.
        mockMvc.perform(post("/api/cooperating-agencies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"name\":\"bureau of fisheries and aquatic resources (bfar)\"}")))
                .andExpect(status().isConflict());
    }

    @Test
    void aProponentCannotReadTheSelectionOfAnotherProponentsProposal() throws Exception {
        Proposal mine = proposal("Mine", proponent);
        Proposal theirs = proposal("Theirs", otherProponent);

        mockMvc.perform(get("/api/proposals/" + mine.getId() + "/cooperating-agencies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/proposals/" + theirs.getId() + "/cooperating-agencies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken))
                .andExpect(status().isForbidden());
    }
}
