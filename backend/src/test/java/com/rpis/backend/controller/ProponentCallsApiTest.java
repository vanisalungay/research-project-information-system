package com.rpis.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.PasswordService;
import com.rpis.backend.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The complete published Call for Proposals list behind the dashboard's View All
 * action. It must contain every open, scheduled and closed call - never only the five
 * the dashboard previews - while drafts and un-published records stay hidden.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProponentCallsApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProposalAnnouncementRepository announcementRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordService passwordService;
    @Autowired private TokenService tokenService;
    @Autowired private DataSource dataSource;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().contains("rpis_test"),
                    "this test must run against the isolated rpis_test database");
        }
        User proponent = new User();
        proponent.setName("Calls API Proponent");
        proponent.setEmail("calls.api." + UUID.randomUUID() + "@msunaawan.edu.ph");
        proponent.setPassword(passwordService.hash("CallsApiPass1!"));
        proponent.setRole(UserRoles.PROPONENT);
        proponent.setStatus("APPROVED");
        proponent = userRepository.saveAndFlush(proponent);
        token = tokenService.generateToken(proponent.getId(), UserRoles.PROPONENT);
    }

    private ProposalAnnouncement call(String name, LocalDate start, LocalDate end, String status) {
        ProposalAnnouncement announcement = new ProposalAnnouncement();
        announcement.setName(name);
        announcement.setStartDate(start);
        announcement.setEndDate(end);
        announcement.setStatus(status);
        return announcementRepository.saveAndFlush(announcement);
    }

    @Test
    void theFullListingReturnsEveryPublishedCallIncludingClosedOnes() throws Exception {
        LocalDate today = LocalDate.now();
        // Six open calls: more than the dashboard's five-call preview.
        for (int i = 1; i <= 6; i++) {
            call("Open call " + i, today.minusDays(i), today.plusDays(10), ProposalAnnouncement.STATUS_ACTIVE);
        }
        call("Scheduled call", today.plusDays(5), today.plusDays(20), ProposalAnnouncement.STATUS_ACTIVE);
        call("Closed call", today.minusDays(20), today.minusDays(10), ProposalAnnouncement.STATUS_ACTIVE);
        // Hidden: never published, and a stored CLOSED announcement.
        call("Draft call", today.minusDays(1), today.plusDays(5), ProposalAnnouncement.STATUS_DRAFT);
        call("Stored closed call", today.minusDays(1), today.plusDays(5),
                ProposalAnnouncement.STATUS_CLOSED);

        MvcResult result = mockMvc.perform(get("/api/proposal-announcements/proponent-calls")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listing = objectMapper.readTree(result.getResponse().getContentAsString());
        java.util.List<String> names = new java.util.ArrayList<>();
        listing.forEach((node) -> names.add(node.get("name").asText()));

        // All six open calls are present: the full listing is not the dashboard's five.
        for (int i = 1; i <= 6; i++) {
            assertTrue(names.contains("Open call " + i), "the listing must include open call " + i);
        }
        assertTrue(names.contains("Scheduled call"));
        assertTrue(names.contains("Closed call"));
        assertFalse(names.contains("Draft call"), "a draft call is never exposed");
        assertFalse(names.contains("Stored closed call"), "a stored CLOSED call stays hidden");
        assertTrue(names.indexOf("Scheduled call") > names.indexOf("Open call 1"),
                "open calls come before scheduled calls");
        assertTrue(names.indexOf("Closed call") > names.indexOf("Scheduled call"),
                "closed calls come last");
    }

    @Test
    void anUnauthenticatedCallerCannotReadTheListing() throws Exception {
        mockMvc.perform(get("/api/proposal-announcements/proponent-calls"))
                .andExpect(status().isForbidden());
    }
}
