package com.rpis.backend.controller;

import com.rpis.backend.model.User;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization of the Dean endorsement workflow, exercised through the real Spring
 * Security filter chain.
 *
 * <p>These are the rules the feature rests on: only a Faculty Proponent may raise a
 * request, only a Dean may approve or reject one, only an RPS administrator may grant
 * the Dean role or change the endorsement letter settings, and no route is reachable
 * anonymously. The service layer re-checks ownership and scope on top of this.
 *
 * <p>Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EndorsementSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String proponentToken;
    private String deanToken;
    private String adminToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE endorsement_request_events RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE certifications RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE endorsement_requests RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        proponentToken = tokenFor("PROPONENT", "security.proponent@msunaawan.edu.ph");
        deanToken = tokenFor("RPS_DEAN", "security.dean@msunaawan.edu.ph");
        adminToken = tokenFor("RPS_ADMIN", "security.admin@msunaawan.edu.ph");
        staffToken = tokenFor("RPS_STAFF", "security.staff@msunaawan.edu.ph");
    }

    private String tokenFor(String role, String email) {
        User user = new User();
        user.setName(role + " Account");
        user.setEmail(email);
        user.setPassword("secret");
        user.setRole(role);
        user.setStatus("APPROVED");
        user = userRepository.saveAndFlush(user);
        return tokenService.generateToken(user.getId(), role);
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder builder,
            String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    /**
     * An anonymous caller is denied. The filter chain answers 401 for a route that
     * requires authentication and 403 for one that additionally requires a role, so
     * both are accepted - what matters is that the call never reaches the endpoint.
     */
    private void expectDenied(MockHttpServletRequestBuilder builder) throws Exception {
        int status = mockMvc.perform(builder).andReturn().getResponse().getStatus();
        assertTrue(status == 401 || status == 403,
                "the anonymous call must be denied, but the status was " + status);
    }

    // ==================== ANONYMOUS ====================

    @Test
    void anonymousCallersCannotReachAnyEndorsementRoute() throws Exception {
        expectDenied(get("/api/endorsements"));
        expectDenied(post("/api/endorsements"));
        expectDenied(get("/api/endorsements/1"));
        expectDenied(get("/api/endorsements/1/document"));
        expectDenied(get("/api/certifications"));
        expectDenied(get("/api/users/deans"));
    }

    // ==================== RAISING A REQUEST ====================

    @Test
    void onlyAProponentMayRaiseAnEndorsementRequest() throws Exception {
        mockMvc.perform(withToken(post("/api/endorsements"), deanToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(post("/api/endorsements"), adminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(post("/api/endorsements"), staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void aProponentCanReachTheirOwnEndorsementListAndCertifications() throws Exception {
        mockMvc.perform(withToken(get("/api/endorsements"), proponentToken))
                .andExpect(status().isOk());
        mockMvc.perform(withToken(get("/api/certifications"), proponentToken))
                .andExpect(status().isOk());
    }

    // ==================== DECIDING A REQUEST ====================

    @Test
    void onlyADeanMayApproveOrReject() throws Exception {
        for (String token : new String[] {proponentToken, adminToken, staffToken}) {
            mockMvc.perform(withToken(put("/api/endorsements/1/approve"), token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(withToken(put("/api/endorsements/1/reject"), token))
                    .andExpect(status().isForbidden());
        }
        // A Proponent and ORPS staff can never reissue an issued letter.
        mockMvc.perform(withToken(put("/api/endorsements/1/reissue"), proponentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(put("/api/endorsements/1/reissue"), staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void aDeanCanReachTheReviewList() throws Exception {
        mockMvc.perform(withToken(get("/api/endorsements"), deanToken)).andExpect(status().isOk());
    }

    // ==================== LETTER SETTINGS ====================

    @Test
    void onlyAnAdministratorMayChangeTheLetterSettings() throws Exception {
        mockMvc.perform(withToken(put("/api/endorsements/letter-settings"), proponentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(put("/api/endorsements/letter-settings"), deanToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(put("/api/endorsements/letter-settings"), staffToken))
                .andExpect(status().isForbidden());

        // Reading them is allowed for every authenticated user.
        mockMvc.perform(withToken(get("/api/endorsements/letter-settings"), deanToken))
                .andExpect(status().isOk());
    }

    // ==================== DEAN ACCOUNTS ====================

    @Test
    void onlyAnAdministratorMayManageDeanAccounts() throws Exception {
        mockMvc.perform(withToken(get("/api/users/deans"), proponentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(get("/api/users/deans"), deanToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(get("/api/users/deans"), staffToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(post("/api/users/deans"), staffToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(delete("/api/users/deans/1"), staffToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(withToken(get("/api/users/deans"), adminToken)).andExpect(status().isOk());
    }
}
