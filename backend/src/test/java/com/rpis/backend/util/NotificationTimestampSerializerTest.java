package com.rpis.backend.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpis.backend.model.Notification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationTimestampSerializerTest {

    private final ZoneId original = RpisTimeZone.zone();

    @AfterEach
    void restoreZone() {
        RpisTimeZone.use(original.getId());
    }

    @Test
    void theTimestampCarriesAnExplicitOffset() throws Exception {
        RpisTimeZone.use("Asia/Manila");
        Notification notification = new Notification();
        notification.setCreatedAt(LocalDateTime.of(2026, 11, 18, 14, 7, 12, 345_000_000));

        String json = new ObjectMapper().writeValueAsString(notification);

        assertTrue(json.contains("\"createdAt\":\"2026-11-18T14:07:12.345+08:00\""),
                "a browser must be able to resolve the instant without guessing the server zone: " + json);
    }

    @Test
    void aMissingTimestampStaysNull() throws Exception {
        Notification notification = new Notification();
        String json = new ObjectMapper().writeValueAsString(notification);
        assertTrue(json.contains("\"createdAt\":null"), json);
    }

    @Test
    void theOffsetFollowsTheConfiguredZone() throws Exception {
        RpisTimeZone.use("UTC");
        Notification notification = new Notification();
        notification.setCreatedAt(LocalDateTime.of(2026, 11, 18, 14, 7, 12));

        String json = new ObjectMapper().writeValueAsString(notification);

        assertTrue(json.contains("2026-11-18T14:07:12Z") || json.contains("2026-11-18T14:07:12+00:00"),
                json);
        assertNull(notification.getProposalDocumentId());
    }
}
