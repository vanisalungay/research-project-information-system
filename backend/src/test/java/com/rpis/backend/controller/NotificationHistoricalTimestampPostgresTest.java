package com.rpis.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.TokenService;
import com.rpis.backend.util.RpisTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Historical notification rows are zone-less wall-clock values. The API must keep that
 * wall-clock part intact and append the configured offset; converting the stored value
 * would silently move every old notification by hours.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationHistoricalTimestampPostgresTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private TokenService tokenService;

    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName("Historical Timestamp");
        user.setEmail("historical-" + UUID.randomUUID() + "@example.edu.ph");
        user.setPassword("secret");
        user.setRole("PROPONENT");
        user.setStatus("APPROVED");
        user = userRepository.saveAndFlush(user);
        token = tokenService.generateToken(user.getId(), user.getRole());

        jdbcTemplate.update(
                "INSERT INTO notifications (user_id, message, title, type, is_read, created_at) "
                        + "VALUES (?, ?, ?, ?, FALSE, TIMESTAMP '2026-07-05 08:15:00')",
                user.getId(), "Historical reminder", "Historical reminder", "PROPOSAL_UPDATE");
    }

    @Test
    void theStoredWallClockIsUnchangedAndTheOffsetIsAppended() throws Exception {
        String body = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        String createdAt = json.get(0).get("createdAt").asText();
        String expectedOffset = OffsetDateTime.now(RpisTimeZone.zone()).getOffset().getId();

        assertTrue(createdAt.startsWith("2026-07-05T08:15"),
                "the stored wall-clock must not be shifted: " + createdAt);
        assertTrue(createdAt.endsWith(expectedOffset) || createdAt.endsWith("Z"),
                "the API must append the configured offset: " + createdAt);
        assertEquals("Historical reminder", json.get(0).get("title").asText());
    }
}
