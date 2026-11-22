package com.rpis.backend.controller;

import com.rpis.backend.model.Notification;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.NotificationService;
import com.rpis.backend.service.TokenService;
import com.rpis.backend.util.RpisTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end proof that the SSE endpoint really delivers a notification that was committed
 * by the application, over a real HTTP connection with the ordinary Bearer token.
 *
 * <p>The browser reads this stream with fetch (EventSource cannot send the Authorization
 * header), so the test uses the JDK HTTP client for the same request. The notification is
 * written inside a transaction; the after-commit listener is what must push it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificationStreamIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final List<Stream<String>> bodies = new ArrayList<>();

    @AfterEach
    void closeStreams() {
        bodies.forEach(Stream::close);
        bodies.clear();
    }

    private record OpenStream(Stream<String> body, BlockingQueue<String> lines) {
    }

    private User createUser() {
        User user = new User();
        user.setName("Stream test");
        user.setEmail("stream-" + UUID.randomUUID() + "@example.edu.ph");
        user.setPassword("{noop}unused");
        user.setRole("PROPONENT");
        user.setStatus("APPROVED");
        return userRepository.save(user);
    }

    private OpenStream openStream(String token, String name) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/notifications/stream"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<Stream<String>> response = HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .get(15, TimeUnit.SECONDS);

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("")
                        .startsWith("text/event-stream"),
                "the endpoint must answer as an SSE stream");

        BlockingQueue<String> lines = new LinkedBlockingQueue<>();
        Stream<String> body = response.body();
        bodies.add(body);
        Thread reader = new Thread(() -> body.forEach(lines::add), name);
        reader.setDaemon(true);
        reader.start();
        return new OpenStream(body, lines);
    }

    private String awaitEvent(BlockingQueue<String> lines, String eventName) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        String line;
        while (System.nanoTime() < deadline) {
            line = lines.poll(500, TimeUnit.MILLISECONDS);
            if (line != null && line.equals("event:" + eventName)) {
                return line;
            }
        }
        return null;
    }

    @Test
    void aCommittedNotificationArrivesOnTheOpenStream() throws Exception {
        User user = createUser();
        String token = tokenService.generateToken(user.getId(), user.getRole());
        OpenStream stream = openStream(token, "sse-test-reader");

        assertEquals("event:connected", awaitEvent(stream.lines(), "connected"),
                "the stream must announce the connection before any notification");

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                notificationService.createNotification(
                        user.getId(), "Live delivery", "Live delivery",
                        "PROPOSAL_UPDATE", null));

        assertEquals("event:notification", awaitEvent(stream.lines(), "notification"),
                "a committed notification must be pushed to the open stream");
    }

    @Test
    void twoTabsReceiveTheNotificationAndTheReadStateChange() throws Exception {
        User user = createUser();
        String token = tokenService.generateToken(user.getId(), user.getRole());
        OpenStream first = openStream(token, "sse-tab-one");
        OpenStream second = openStream(token, "sse-tab-two");

        assertEquals("event:connected", awaitEvent(first.lines(), "connected"));
        assertEquals("event:connected", awaitEvent(second.lines(), "connected"));

        Notification created = new TransactionTemplate(transactionManager).execute(status ->
                notificationService.createNotification(
                        user.getId(), "Two tabs", "Two tabs", "PROPOSAL_UPDATE", null));
        assertNotNull(created);
        assertEquals("event:notification", awaitEvent(first.lines(), "notification"),
                "the first tab must receive the new notification");
        assertEquals("event:notification", awaitEvent(second.lines(), "notification"),
                "the second tab must receive the same notification");

        HttpRequest read = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/notifications/"
                                + created.getId() + "/read"))
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(15))
                .PUT(BodyPublishers.noBody())
                .build();
        HttpResponse<String> readResponse = HttpClient.newHttpClient()
                .send(read, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, readResponse.statusCode());

        assertEquals("event:notifications-changed", awaitEvent(first.lines(), "notifications-changed"),
                "the first tab must learn that the item was read");
        assertEquals("event:notifications-changed", awaitEvent(second.lines(), "notifications-changed"),
                "the second tab must learn that the item was read");
    }

    @Test
    void aNotificationCreatedWhileOfflineIsRecoveredByTheList() throws Exception {
        User user = createUser();
        String token = tokenService.generateToken(user.getId(), user.getRole());

        // No stream is open while the event happens.
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                notificationService.createNotification(
                        user.getId(), "Missed while offline", "Missed while offline",
                        "PROPOSAL_UPDATE", null));

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/notifications"))
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Missed while offline"),
                "the persisted row must be available on the reconnect refresh");
    }

    @Test
    void theNotificationListReturnsAnOffsetTimestamp() throws Exception {
        User user = createUser();
        String token = tokenService.generateToken(user.getId(), user.getRole());

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                notificationService.createNotification(
                        user.getId(), "Timestamp check", "Timestamp check",
                        "PROPOSAL_UPDATE", null));

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/notifications"))
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String expectedOffset = OffsetDateTime.now(RpisTimeZone.zone()).getOffset().getId();
        assertTrue(response.body().contains(expectedOffset + "\""),
                "the API must carry the configured offset so the browser resolves the instant: "
                        + response.body());
    }
}
