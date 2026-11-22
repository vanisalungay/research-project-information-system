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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The password policy as an API contract.
 *
 * <p>The browser shows the same rules, but a caller who skips the browser - a direct
 * change-password call or a direct registration POST - must hit the same wall. These tests
 * exercise the real controllers and the isolated {@code rpis_test} database; every test is
 * transactional, so the rows it creates are rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordChangeApiTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final String OLD_PASSWORD = "OldPassword1!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordService passwordService;
    @Autowired
    private DataSource dataSource;

    private User account;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertTrue(url.contains("rpis_test"),
                    "Password API tests must run against the isolated rpis_test database, "
                            + "but connected to: " + url);
        }

        account = new User();
        account.setName("Password Api Test");
        account.setEmail("password.api." + SEQUENCE.incrementAndGet() + "@msunaawan.edu.ph");
        account.setPassword(passwordService.hash(OLD_PASSWORD));
        account.setRole(UserRoles.RPS_STAFF);
        account.setStatus("APPROVED");
        account.setMustChangePassword(true);
        account = userRepository.saveAndFlush(account);
        token = tokenService.generateToken(account.getId(), UserRoles.RPS_STAFF);
    }

    private Map<String, Object> changeBody(String current, String next, String confirm) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("currentPassword", current);
        body.put("newPassword", next);
        body.put("confirmPassword", confirm);
        return body;
    }

    private void expectChangeRejected(String next, String expectedFragment) throws Exception {
        mockMvc.perform(put("/api/users/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                changeBody(OLD_PASSWORD, next, next))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(expectedFragment)))
                .andExpect(content().string(not(containsString(next))));
    }

    @Test
    void fewerThanEightCharactersIsRejected() throws Exception {
        expectChangeRejected("Ab1!xyz", "8 characters");
    }

    @Test
    void aNewPasswordWithoutAnUppercaseLetterIsRejected() throws Exception {
        expectChangeRejected("rpis@2026", "uppercase");
    }

    @Test
    void aNewPasswordWithoutADigitIsRejected() throws Exception {
        expectChangeRejected("RpisSystem@", "number");
    }

    @Test
    void aNewPasswordWithoutASymbolIsRejected() throws Exception {
        expectChangeRejected("RpisSystem2026", "special character");
    }

    @Test
    void aConfirmationThatDoesNotMatchIsRejected() throws Exception {
        mockMvc.perform(put("/api/users/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                changeBody(OLD_PASSWORD, "NewPassword1!", "NewPassword2!"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("do not match")));
    }

    @Test
    void anIncorrectCurrentPasswordIsRejected() throws Exception {
        mockMvc.perform(put("/api/users/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                changeBody("WrongPassword1!", "NewPassword1!", "NewPassword1!"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("current password is incorrect")));
    }

    @Test
    void aValidChangeUpdatesTheStoredHashAndRetiresTheOldPassword() throws Exception {
        String next = "NewPassword1!";
        mockMvc.perform(put("/api/users/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                changeBody(OLD_PASSWORD, next, next))))
                .andExpect(status().isOk());

        userRepository.flush();
        User reloaded = userRepository.findById(account.getId()).orElseThrow();
        assertTrue(passwordService.isHashed(reloaded.getPassword()),
                "the new value must be stored as a BCrypt hash");
        assertTrue(passwordService.verify(reloaded.getPassword(), next).matches(),
                "the new password must work");
        assertFalse(passwordService.verify(reloaded.getPassword(), OLD_PASSWORD).matches(),
                "the old password must stop working");
        assertFalse(Boolean.TRUE.equals(reloaded.getMustChangePassword()),
                "a successful change clears the first-login flag");
    }

    @Test
    void anAdministratorResetCannotStoreAWeakPassword() throws Exception {
        User admin = new User();
        admin.setName("Password Reset Admin");
        admin.setEmail("password.admin." + SEQUENCE.incrementAndGet() + "@msunaawan.edu.ph");
        admin.setPassword(passwordService.hash(OLD_PASSWORD));
        admin.setRole(UserRoles.RPS_ADMIN);
        admin.setStatus("APPROVED");
        admin = userRepository.saveAndFlush(admin);
        String adminToken = tokenService.generateToken(admin.getId(), UserRoles.RPS_ADMIN);

        mockMvc.perform(put("/api/users/" + account.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "weak"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("8 characters")));

        // A valid reset is exercised end-to-end by the live UI check, where the
        // request commits in its own transaction. This test covers the rejection
        // contract: the API cannot be used to store a password below the policy.
    }

    @Test
    void registrationCannotBypassThePolicy() throws Exception {
        Map<String, Object> weak = new LinkedHashMap<>();
        weak.put("name", "Weak Password Applicant");
        weak.put("email", "weak.password." + SEQUENCE.incrementAndGet() + "@msunaawan.edu.ph");
        weak.put("password", "weak");
        weak.put("role", UserRoles.PROPONENT);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weak)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("8 characters")));

        Map<String, Object> strong = new LinkedHashMap<>(weak);
        strong.put("email", "strong.password." + SEQUENCE.incrementAndGet() + "@msunaawan.edu.ph");
        strong.put("password", "StrongPass1!");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(strong)))
                .andExpect(status().isCreated());
    }
}
