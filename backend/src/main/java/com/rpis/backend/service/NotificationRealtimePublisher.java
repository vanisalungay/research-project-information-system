package com.rpis.backend.service;

import com.rpis.backend.config.NotificationEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Bridges persisted notification events to the open SSE streams.
 *
 * <p>Both listeners run only after the surrounding transaction commits. A workflow that
 * fails and rolls back therefore never produces a browser notification, while a
 * notification saved without an active transaction (for example a direct service test)
 * is still delivered because {@code fallbackExecution} is enabled.
 */
@Service
@RequiredArgsConstructor
public class NotificationRealtimePublisher {

    private final NotificationEmitterRegistry emitterRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCreated(NotificationCreatedEvent event) {
        if (event == null || event.userId() == null || event.notificationId() == null) {
            return;
        }
        // The id is enough: the client refreshes the list so the computed fields
        // (formatted proposal id, announcement name/status) are exactly what a normal
        // GET returns. Sending a partial row would risk showing stale derived data.
        emitterRegistry.sendToUser(event.userId(), "notification",
                Map.of("id", event.notificationId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onChanged(NotificationChangedEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }
        emitterRegistry.sendToUser(event.userId(), "notifications-changed",
                Map.of("reason", event.reason() == null ? "read" : event.reason()));
    }
}
