package com.rpis.backend.controller;

import com.rpis.backend.model.Notification;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.NotificationRepository;
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
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role-specific notification experience for one multi-role account.
 *
 * <p>The same account holds PROPONENT and DEAN. Each role's token sees only the
 * notifications raised for that role; a read action aimed at the other role is refused,
 * and "mark all read" only touches the current role's rows. Switching roles therefore
 * never marks or reveals the other role's notifications.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationRoleSeparationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private PasswordService passwordService;
    @Autowired
    private DataSource dataSource;

    private User account;
    private Notification proponentNotification;
    private Notification deanNotification;
    private String proponentToken;
    private String deanToken;

    @BeforeEach
    void setUp() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertTrue(url.contains("rpis_test"),
                    "Notification role tests must run against the isolated rpis_test database");
        }

        int n = SEQUENCE.incrementAndGet();
        account = new User();
        account.setName("Mira Multi" + n);
        account.setEmail("mira.multirole" + n + "@msunaawan.edu.ph");
        account.setPassword(passwordService.hash("multi-role-pass-2026"));
        account.setRole(UserRoles.PROPONENT);
        account.assignRole(UserRoles.DEAN);
        account.setStatus("APPROVED");
        account = userRepository.saveAndFlush(account);

        proponentNotification = notification("Proponent approval",
                "APPROVAL", UserRoles.PROPONENT);
        deanNotification = notification("New endorsement request",
                "ENDORSEMENT", UserRoles.DEAN);

        proponentToken = tokenService.generateToken(account.getId(), UserRoles.PROPONENT);
        deanToken = tokenService.generateToken(account.getId(), UserRoles.DEAN);
    }

    private Notification notification(String title, String type, String role) {
        Notification notification = new Notification();
        notification.setUser(account);
        notification.setMessage(title + " for " + role);
        notification.setTitle(title);
        notification.setType(type);
        notification.setRecipientRole(role);
        notification.setIsRead(false);
        return notificationRepository.saveAndFlush(notification);
    }

    @Test
    void eachRoleSeesOnlyItsOwnNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(proponentNotification.getId()))
                .andExpect(jsonPath("$[0].recipientRole").value(UserRoles.PROPONENT));

        mockMvc.perform(get("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + deanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(deanNotification.getId()))
                .andExpect(jsonPath("$[0].recipientRole").value(UserRoles.DEAN));
    }

    @Test
    void aRoleCannotMarkAnotherRolesNotificationRead() throws Exception {
        mockMvc.perform(put("/api/notifications/" + deanNotification.getId() + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + proponentToken))
                .andExpect(status().isForbidden());
        assertFalse(notificationRepository.findById(deanNotification.getId())
                .orElseThrow().getIsRead());

        mockMvc.perform(put("/api/notifications/" + deanNotification.getId() + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + deanToken))
                .andExpect(status().isOk());
        assertTrue(notificationRepository.findById(deanNotification.getId())
                .orElseThrow().getIsRead());
    }

    @Test
    void markAllReadOnlyTouchesTheCurrentRolesRows() throws Exception {
        mockMvc.perform(put("/api/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + deanToken))
                .andExpect(status().isOk());

        assertTrue(notificationRepository.findById(deanNotification.getId())
                .orElseThrow().getIsRead(), "the Dean notification is read");
        assertFalse(notificationRepository.findById(proponentNotification.getId())
                .orElseThrow().getIsRead(),
                "the Proponent notification is untouched while signed in as Dean");
        assertEquals(1L, notificationRepository.findAll().stream()
                .filter(row -> Boolean.TRUE.equals(row.getIsRead())).count());
    }
}
