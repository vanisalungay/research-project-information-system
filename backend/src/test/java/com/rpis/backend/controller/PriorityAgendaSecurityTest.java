package com.rpis.backend.controller;

import com.rpis.backend.model.User;
import com.rpis.backend.repository.PriorityAgendaSetRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization of the Priority Agenda master data, exercised through the real Spring
 * Security filter chain: an anonymous caller cannot read the configuration, an
 * authenticated proponent can read the active framework and the label lookup but can
 * never create a framework or edit an agenda through a direct API request, and an
 * administrator (RPS_ADMIN / RPS_STAFF) still can.
 * <p>
 * Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PriorityAgendaSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private PriorityAgendaSetRepository agendaSetRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String proponentToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
        proponentToken = tokenService.generateToken(
                user("Priority Proponent", "priority@msunaawan.edu.ph", "PROPONENT").getId(), "PROPONENT");
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

    private String body(String json) {
        return new String(json.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    private String setPayload(String frameworkName, int start, int end) {
        return body("{\"frameworkName\":\"" + frameworkName + "\",\"coverageStartYear\":" + start
                + ",\"coverageEndYear\":" + end + "}");
    }

    // ===== READ =====

    @Test
    void anAnonymousCallerCannotReadTheConfiguration() throws Exception {
        mockMvc.perform(get("/api/priority-agendas/active")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/priority-agendas/lookup").param("keys", "dagat"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAuthenticatedProponentCanReadTheActiveConfiguration() throws Exception {
        mockMvc.perform(get("/api/priority-agendas/active")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frameworkName").value("MSUN RIIDE"))
                .andExpect(jsonPath("$.coverageStartYear").value(2025))
                .andExpect(jsonPath("$.coverageEndYear").value(2028))
                .andExpect(jsonPath("$.label").value("(based on MSUN RIIDE 2025\u20132028)"))
                .andExpect(jsonPath("$.agendas.length()").value(5));
    }

    @Test
    void anAuthenticatedProponentCanResolveHistoricalAgendaLabels() throws Exception {
        mockMvc.perform(get("/api/priority-agendas/lookup")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken)
                        .param("keys", "dagat,punla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("DAGAT Program"))
                .andExpect(jsonPath("$[1].name").value("PUNLA Program"));
    }

    // ===== WRITE =====

    @Test
    void anAnonymousCallerCannotManageAgendas() throws Exception {
        mockMvc.perform(post("/api/priority-agendas/sets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setPayload("Anonymous Framework", 2030, 2033)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aProponentCannotCreateAFrameworkThroughADirectRequest() throws Exception {
        long before = agendaSetRepository.count();

        mockMvc.perform(post("/api/priority-agendas/sets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setPayload("Proponent Framework", 2030, 2033)))
                .andExpect(status().isForbidden());

        assertEquals(before, agendaSetRepository.count(), "nothing may be stored for a proponent");
    }

    @Test
    void aProponentCannotAddOrEditAnAgenda() throws Exception {
        Long setId = agendaSetRepository.findFirstByActiveTrue().orElseThrow().getId();

        mockMvc.perform(post("/api/priority-agendas/sets/" + setId + "/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"name\":\"Sneaky Agenda\"}")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/priority-agendas/items/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"name\":\"Renamed\",\"active\":false}")))
                .andExpect(status().isForbidden());

        // The seeded agenda is untouched.
        assertEquals("DAGAT Program",
                jdbcTemplate.queryForObject("SELECT name FROM priority_agendas WHERE agenda_key = 'dagat'",
                        String.class));
    }

    @Test
    void anAdministratorCanManageTheFrameworksAndAgendas() throws Exception {
        for (String role : List.of("RPS_ADMIN", "RPS_STAFF")) {
            String token = tokenService.generateToken(
                    user(role + " Manager", role.toLowerCase() + "@msunaawan.edu.ph", role).getId(), role);
            String framework = "MSUN RIDE " + role;

            mockMvc.perform(post("/api/priority-agendas/sets")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(setPayload(framework, 2029, 2032)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.frameworkName").value(framework))
                    .andExpect(jsonPath("$.active").value(false));

            Long setId = agendaSetRepository.findAll().stream()
                    .filter(set -> framework.equals(set.getFrameworkName()))
                    .findFirst()
                    .orElseThrow()
                    .getId();

            // Add an agenda, then activate the framework for new submissions.
            mockMvc.perform(post("/api/priority-agendas/sets/" + setId + "/items")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("{\"name\":\"BLUE ECONOMY Program\",\"subLabel\":\"Sector\"}")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.agendaKey").value("blue-economy-program"));

            mockMvc.perform(put("/api/priority-agendas/sets/" + setId + "/activate")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true));

            // The Proponent form now reads the administrator's configuration.
            mockMvc.perform(get("/api/priority-agendas/active")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.frameworkName").value(framework))
                    .andExpect(jsonPath("$.label").value("(based on " + framework + " 2029\u20132032)"))
                    .andExpect(jsonPath("$.agendas[0].agendaKey").value("blue-economy-program"));

            assertEquals(1, agendaSetRepository.findAll().stream()
                            .filter(set -> Boolean.TRUE.equals(set.getActive())).count(),
                    "only one framework may be active");
        }
    }

    @Test
    void anInvalidPayloadIsAnsweredWithAReadableBadRequest() throws Exception {
        String token = tokenService.generateToken(
                user("RPS Admin", "rps.admin@msunaawan.edu.ph", "RPS_ADMIN").getId(), "RPS_ADMIN");

        // End year before the start year.
        mockMvc.perform(post("/api/priority-agendas/sets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setPayload("Broken Framework", 2032, 2029)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        // Duplicate of the seeded framework + coverage period.
        mockMvc.perform(post("/api/priority-agendas/sets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setPayload("MSUN RIIDE", 2025, 2028)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());

        // A missing framework is reported as a bad request, not as a server error.
        mockMvc.perform(post("/api/priority-agendas/sets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{}")))
                .andExpect(status().isBadRequest());
    }
}
