package com.rpis.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.PasswordService;
import com.rpis.backend.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * One account, several roles.
 *
 * <p>Pins the two rules the multi-role change exists for: the sign-in page may select any
 * role the account holds (and only those), and the role in a token is checked against the
 * account on every request - so revoking a role also revokes tokens that still name it.
 *
 * <p>Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MultiRoleAccountTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final String PASSWORD = "multi-role-pass-2026";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private PasswordService passwordService;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void requireTestDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertTrue(url.contains("rpis_test"),
                    "Multi-role tests must run against the isolated rpis_test database, "
                            + "but connected to: " + url);
        }
    }

    private User multiRoleAccount() {
        int n = SEQUENCE.incrementAndGet();
        User user = new User();
        user.setName("Mira Multi" + n);
        user.setEmail("mira.multi" + n + "@msunaawan.edu.ph");
        user.setPassword(passwordService.hash(PASSWORD));
        user.setRole(UserRoles.PROPONENT);
        user.assignRole(UserRoles.PROPONENT);
        user.assignRole(UserRoles.DEAN);
        user.setStatus("APPROVED");
        return userRepository.saveAndFlush(user);
    }

    private Map<String, Object> loginBody(User user, String email, String role) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", PASSWORD);
        body.put("role", role);
        return body;
    }

    @Test
    void theLoginPageCanSelectEitherRoleTheAccountHolds() throws Exception {
        User user = multiRoleAccount();

        // Typed with different capitalization and surrounding/invisible whitespace, exactly
        // like the Dean sign-in issue: the account is still found.
        String typed = "   " + user.getEmail().toUpperCase() + "  ​";

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                loginBody(user, typed, "DEAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(UserRoles.DEAN))
                .andExpect(jsonPath("$.roles", hasItems(UserRoles.PROPONENT, UserRoles.DEAN)))
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                loginBody(user, user.getEmail(), "PROPONENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(UserRoles.PROPONENT))
                .andExpect(jsonPath("$.token").isNotEmpty());

        // A role the account does not hold is refused as invalid credentials.
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                loginBody(user, user.getEmail(), UserRoles.RPS_ADMIN))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aRevokedRoleStopsAuthenticatingItsExistingToken() throws Exception {
        User user = multiRoleAccount();
        String deanToken = tokenService.generateToken(user.getId(), UserRoles.DEAN);
        String proponentToken = tokenService.generateToken(user.getId(), UserRoles.PROPONENT);

        mockMvc.perform(get("/api/endorsements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + deanToken))
                .andExpect(status().isOk());

        // Removing the assigned role must take effect on the next request, without waiting
        // for the token to expire.
        user.removeRole(UserRoles.DEAN);
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/endorsements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + deanToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/endorsements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken))
                .andExpect(status().isOk());
    }
}
