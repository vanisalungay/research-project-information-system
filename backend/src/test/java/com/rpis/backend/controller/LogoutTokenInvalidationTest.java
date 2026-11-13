package com.rpis.backend.controller;

import com.jayway.jsonpath.JsonPath;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A token must stop working when its account signs out.
 *
 * <p>These tests go through the real Spring Security filter chain and the real login
 * and logout endpoints: the token returned by sign-in is used on a protected route,
 * then sign-out is called with that same token, then the token is presented again.
 * Before server-side invalidation existed the third call still succeeded, because
 * sign-out was purely client-side.
 *
 * <p>Runs against the isolated {@code rpis_test} database only. Deliberately not
 * {@code @Transactional}: each request has to commit like a real one, and a shared
 * persistence context would flush the login response's hidden password field.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LogoutTokenInvalidationTest {

    private static final String EMAIL = "logout.proponent@msunaawan.edu.ph";
    private static final String PASSWORD = "Logout-Test-Password-1";
    private static final String ROLE = "PROPONENT";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** The same secret the application signs its tokens with, for the legacy-token case. */
    @Value("${rpis.security.token-secret}")
    private String tokenSecret;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        User user = new User();
        user.setName("Logout Proponent");
        user.setEmail(EMAIL);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(ROLE);
        user.setStatus("APPROVED");
        userRepository.saveAndFlush(user);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD
                                + "\",\"role\":\"" + ROLE + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private void signOut(String token) throws Exception {
        mockMvc.perform(post("/api/users/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    private void expectAuthenticated(String token) throws Exception {
        mockMvc.perform(get("/api/cooperating-agencies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void expectRejected(String token) throws Exception {
        mockMvc.perform(get("/api/cooperating-agencies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenCannotBeReusedAfterLogout() throws Exception {
        String token = login();
        expectAuthenticated(token);

        signOut(token);

        expectRejected(token);
    }

    @Test
    void signingInAgainIssuesATokenForTheNewGeneration() throws Exception {
        String beforeLogout = login();
        signOut(beforeLogout);

        String afterLogout = login();

        assertNotEquals(beforeLogout, afterLogout,
                "signing out must change the generation a new token belongs to");
        expectAuthenticated(afterLogout);
        // Issuing a new token does not revive the invalidated one.
        expectRejected(beforeLogout);
    }

    @Test
    void logoutRequiresAuthentication() throws Exception {
        int statusCode = mockMvc.perform(post("/api/users/logout"))
                .andReturn().getResponse().getStatus();
        // The filter chain answers 401 for an unauthenticated API call and 403 for a
        // route it can only deny by role; either is a refusal.
        assertTrue(statusCode == 401 || statusCode == 403,
                "signing out anonymously must be refused, but the status was " + statusCode);
    }

    @Test
    void logoutIsAllowedBeforeTheMandatoryPasswordChange() throws Exception {
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        user.setMustChangePassword(true);
        userRepository.saveAndFlush(user);

        String token = login();
        signOut(token);

        // The temporary-password guard must not keep a session that cannot be ended.
        expectRejected(token);
    }

    @Test
    void aTokenIssuedBeforeVersioningStillWorksUntilTheAccountSignsOut() throws Exception {
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        String legacyToken = legacyToken(user.getId(), ROLE);

        expectAuthenticated(legacyToken);

        signOut(legacyToken);

        expectRejected(legacyToken);
    }

    /**
     * Reproduces the token format used before the version field existed:
     * {@code base64(userId:role:issuedAtMillis:hmacSha256)}. Such a token belongs to
     * generation 0, so an upgrade does not sign anybody out - only a sign-out does.
     */
    private String legacyToken(Long userId, String role) throws Exception {
        long issuedAt = System.currentTimeMillis();
        String data = userId + ":" + role + ":" + issuedAt;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getEncoder()
                .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        String raw = data + ":" + signature;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
