package com.rpis.backend.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The in-memory SSE registry: several tabs per account, cleanup when a stream dies, and
 * no failure leaking into another stream.
 */
class NotificationEmitterRegistryTest {

    private NotificationEmitterRegistry registry;

    @BeforeEach
    void setUp() {
        // heartbeat disabled in tests; the registry itself is exercised synchronously.
        registry = new NotificationEmitterRegistry(true, 60_000, 0);
    }

    @AfterEach
    void tearDown() {
        registry.closeAll();
    }

    private static final class CapturingEmitter extends SseEmitter {
        final List<Object> sent = new ArrayList<>();
        boolean fail;

        CapturingEmitter() {
            super(60_000L);
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (fail) {
                throw new IOException("client disconnected");
            }
            sent.add(builder);
        }
    }

    @Test
    void everyOpenTabOfTheAccountReceivesTheFrame() {
        CapturingEmitter first = new CapturingEmitter();
        CapturingEmitter second = new CapturingEmitter();
        registry.register(7L, first);
        registry.register(7L, second);

        registry.deliverToUser(7L, "notification", java.util.Map.of("id", 1L));

        // register() sends "connected" once, deliverToUser() adds one more frame.
        assertEquals(2, first.sent.size());
        assertEquals(2, second.sent.size());
        assertEquals(2, registry.connectionCount(7L));
    }

    @Test
    void aFailedStreamIsRemovedWithoutAffectingTheOtherTab() {
        CapturingEmitter broken = new CapturingEmitter();
        broken.fail = true;
        CapturingEmitter healthy = new CapturingEmitter();
        registry.register(7L, broken);
        registry.register(7L, healthy);

        registry.deliverToUser(7L, "notification", java.util.Map.of("id", 1L));

        assertEquals(1, registry.connectionCount(7L), "the disconnected stream must be dropped");
        assertEquals(2, healthy.sent.size(), "the healthy stream must still receive the frame");
    }

    @Test
    void anotherAccountsStreamIsNotTouched() {
        CapturingEmitter mine = new CapturingEmitter();
        CapturingEmitter other = new CapturingEmitter();
        registry.register(7L, mine);
        registry.register(8L, other);

        registry.deliverToUser(7L, "notification", java.util.Map.of("id", 1L));

        assertEquals(1, other.sent.size(), "only the 'connected' frame belongs to the other account");
    }

    @Test
    void closeAllDropsEveryStream() {
        registry.register(7L, new CapturingEmitter());
        registry.register(8L, new CapturingEmitter());

        registry.closeAll();

        assertEquals(0, registry.connectionCount(7L));
        assertEquals(0, registry.connectionCount(8L));
    }
}
