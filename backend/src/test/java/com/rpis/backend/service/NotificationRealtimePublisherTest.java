package com.rpis.backend.service;

import com.rpis.backend.config.NotificationEmitterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The SSE hint must follow the database transaction: nothing reaches a browser before the
 * commit, and a rollback never produces a notification.
 */
class NotificationRealtimePublisherTest {

    @Configuration
    @EnableTransactionManagement
    static class TransactionalListenerConfig {
    }

    private NotificationEmitterRegistry registry;
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        registry = mock(NotificationEmitterRegistry.class);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(NotificationEmitterRegistry.class, () -> registry);
        context.register(NotificationRealtimePublisher.class, TransactionalListenerConfig.class);
        context.refresh();
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
        TransactionSynchronizationManager.clearSynchronization();
        context.close();
    }

    private void complete(boolean committed) {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(
                    committed ? TransactionSynchronization.STATUS_COMMITTED
                            : TransactionSynchronization.STATUS_ROLLED_BACK);
        }
    }

    @Test
    void aCreatedNotificationIsSentOnlyAfterTheCommit() {
        org.junit.jupiter.api.Assertions.assertTrue(
                TransactionSynchronizationManager.isSynchronizationActive(),
                "the test must simulate an active transaction synchronization");
        context.publishEvent(new NotificationCreatedEvent(5L, 7L));

        verifyNoInteractions(registry);

        complete(true);

        verify(registry).sendToUser(7L, "notification", Map.of("id", 5L));
    }

    @Test
    void aRolledBackWorkflowSendsNothing() {
        context.publishEvent(new NotificationCreatedEvent(5L, 7L));

        complete(false);

        verify(registry, never()).sendToUser(7L, "notification", Map.of("id", 5L));
    }

    @Test
    void aReadStateChangeIsBroadcastToTheUsersSessions() {
        context.publishEvent(new NotificationChangedEvent(7L, "read-all"));

        complete(true);

        verify(registry).sendToUser(7L, "notifications-changed", Map.of("reason", "read-all"));
    }
}
