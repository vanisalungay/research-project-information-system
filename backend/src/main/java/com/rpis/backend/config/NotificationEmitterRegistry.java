package com.rpis.backend.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keeps the open Server-Sent Events streams of every signed-in account.
 *
 * <p>One account may have several open tabs, so each user maps to a set of emitters.
 * A notification row is always persisted first; this registry only carries the hint that
 * something changed, so a dropped stream can never lose a notification.
 *
 * <p><strong>Single-instance limitation:</strong> the registry is in memory. If the
 * application is ever run behind more than one instance without sticky routing, an event
 * committed on one instance is not seen by a stream connected to another. The frontend's
 * periodic reconciliation covers that case; a shared broker (for example Redis pub/sub)
 * would be required for true cross-instance push.
 */
@Slf4j
@Component
public class NotificationEmitterRegistry {

    private final Map<Long, Set<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final long timeoutMs;
    private final ScheduledExecutorService heartbeatExecutor;
    private final ExecutorService sendExecutor;

    public NotificationEmitterRegistry(
            @Value("${rpis.notifications.sse.enabled:true}") boolean enabled,
            @Value("${rpis.notifications.sse.timeout-ms:600000}") long timeoutMs,
            @Value("${rpis.notifications.sse.heartbeat-ms:20000}") long heartbeatMs) {
        this.enabled = enabled;
        this.timeoutMs = timeoutMs;

        this.heartbeatExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
                daemonFactory("notification-sse-heartbeat"));
        if (enabled && heartbeatMs > 0) {
            heartbeatExecutor.scheduleAtFixedRate(
                    this::heartbeat, heartbeatMs, heartbeatMs, TimeUnit.MILLISECONDS);
        }

        // Bounded so a slow client cannot grow an unbounded queue. A dropped event is
        // harmless: the row is persisted and the client reconciles on its next refresh.
        this.sendExecutor = new ThreadPoolExecutor(
                2, 2, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(500),
                daemonFactory("notification-sse-send"),
                new ThreadPoolExecutor.DiscardPolicy());
    }

    private static java.util.concurrent.ThreadFactory daemonFactory(String name) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, name + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int connectionCount(Long userId) {
        Set<SseEmitter> emitters = emittersByUser.get(userId);
        return emitters == null ? 0 : emitters.size();
    }

    /**
     * Creates a stream for the account and registers it for cleanup. The caller (the
     * controller) returns the emitter from the request.
     */
    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        register(userId, emitter);
        return emitter;
    }

    /** Registers an already-created emitter (also used by tests). */
    public void register(Long userId, SseEmitter emitter) {
        if (!enabled) {
            throw new IllegalStateException("Real-time notifications are disabled.");
        }
        Set<SseEmitter> emitters = emittersByUser.computeIfAbsent(
                userId, key -> ConcurrentHashMap.newKeySet());
        emitters.add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            remove(userId, emitter);
            emitter.complete();
        });
        emitter.onError(error -> remove(userId, emitter));

        sendNow(emitter, "connected", Map.of("userId", userId));
    }

    /**
     * Queues a frame for every open stream of the account. The send happens on a small
     * worker pool so the committing workflow thread never waits on a slow client.
     */
    public void sendToUser(Long userId, String eventName, Object payload) {
        if (!enabled || userId == null) {
            return;
        }
        Set<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        try {
            sendExecutor.execute(() -> deliverToUser(userId, eventName, payload));
        } catch (RejectedExecutionException e) {
            log.debug("Notification SSE queue is full; the client will reconcile on refresh.");
        }
    }

    /** Synchronous delivery; package-private so tests can assert without the pool. */
    void deliverToUser(Long userId, String eventName, Object payload) {
        Set<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : Set.copyOf(emitters)) {
            sendNow(emitter, eventName, payload);
        }
    }

    private void sendNow(SseEmitter emitter, String eventName, Object payload) {
        try {
            synchronized (emitter) {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(payload, MediaType.APPLICATION_JSON));
            }
        } catch (IOException | IllegalStateException e) {
            removeEmitter(emitter);
        }
    }

    private void heartbeat() {
        for (Map.Entry<Long, Set<SseEmitter>> entry : emittersByUser.entrySet()) {
            for (SseEmitter emitter : Set.copyOf(entry.getValue())) {
                try {
                    synchronized (emitter) {
                        emitter.send(SseEmitter.event().name("ping")
                                .data(Map.of("ping", true), MediaType.APPLICATION_JSON));
                    }
                } catch (IOException | IllegalStateException e) {
                    remove(entry.getKey(), emitter);
                }
            }
        }
    }

    private void removeEmitter(SseEmitter emitter) {
        for (Map.Entry<Long, Set<SseEmitter>> entry : emittersByUser.entrySet()) {
            if (entry.getValue().remove(emitter)) {
                removeIfEmpty(entry.getKey(), entry.getValue());
                return;
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        removeIfEmpty(userId, emitters);
    }

    private void removeIfEmpty(Long userId, Set<SseEmitter> emitters) {
        if (emitters.isEmpty()) {
            emittersByUser.remove(userId, emitters);
        }
    }

    @PreDestroy
    public void closeAll() {
        heartbeatExecutor.shutdownNow();
        sendExecutor.shutdownNow();
        for (Map.Entry<Long, Set<SseEmitter>> entry : emittersByUser.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.complete();
                } catch (RuntimeException ignored) {
                    // The connection is already gone.
                }
            }
        }
        emittersByUser.clear();
    }
}
