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

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The two calls the User Accounts "Actions" dialog makes for a pending registration:
 * Approve Account and Reject Account.
 *
 * <p>The dialog itself is only a front end over these existing endpoints, so the tests
 * assert the behaviour the UI depends on: the decision moves the account out of PENDING,
 * the stored status is the one the table then displays, and only an administrator may
 * decide (a Proponent token is refused). They run against the disposable test database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAccountApprovalApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private TokenService tokenService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String proponentToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        adminToken = tokenService.generateToken(
                user("Approval Admin", "RPS_ADMIN", "APPROVED").getId(), "RPS_ADMIN");
        proponentToken = tokenService.generateToken(
                user("Approval Proponent", "PROPONENT", "APPROVED").getId(), "PROPONENT");
    }

    private User user(String name, String role, String status) {
        User user = new User();
        user.setName(name);
        user.setEmail(name.toLowerCase().replace(' ', '-') + "-" + UUID.randomUUID()
                + "@msunaawan.edu.ph");
        user.setPassword("secret");
        user.setRole(role);
        user.setStatus(status);
        user.setDateRegistered(LocalDate.now());
        return userRepository.saveAndFlush(user);
    }

    @Test
    void approvingAPendingAccountMovesItOutOfPending() throws Exception {
        User pending = user("Pending Approve", "PROPONENT", "PENDING");

        mockMvc.perform(put("/api/users/{id}/approve", pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertEquals("APPROVED", userRepository.findById(pending.getId()).orElseThrow().getStatus(),
                "the table must read the approved state after the decision");
    }

    @Test
    void rejectingAPendingAccountMovesItOutOfPending() throws Exception {
        User pending = user("Pending Reject", "PROPONENT", "PENDING");

        mockMvc.perform(put("/api/users/{id}/reject", pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertEquals("REJECTED", userRepository.findById(pending.getId()).orElseThrow().getStatus(),
                "the table must read the rejected state after the decision");
    }

    @Test
    void aNonAdministratorCannotDecideAPendingAccount() throws Exception {
        User pending = user("Pending Guarded", "PROPONENT", "PENDING");

        mockMvc.perform(put("/api/users/{id}/approve", pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken))
                .andExpect(status().isForbidden());

        assertEquals("PENDING", userRepository.findById(pending.getId()).orElseThrow().getStatus(),
                "a refused decision leaves the account pending");
    }
}
