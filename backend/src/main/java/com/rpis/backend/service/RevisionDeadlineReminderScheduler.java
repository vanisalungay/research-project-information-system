package com.rpis.backend.service;

import com.rpis.backend.util.RpisTimeZone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Runs the revision-deadline reminder sweep in the background.
 *
 * <p>Disabled through {@code rpis.notifications.deadline-reminders.enabled=false}
 * (the test profile does this). The service itself remains available so tests can call
 * {@link RevisionDeadlineReminderService#remindDue(LocalDateTime)} with a fixed clock.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "rpis.notifications.deadline-reminders.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class RevisionDeadlineReminderScheduler {

    private final RevisionDeadlineReminderService reminderService;

    @Scheduled(
            initialDelayString = "${rpis.notifications.deadline-reminders.initial-delay-ms:60000}",
            fixedDelayString = "${rpis.notifications.deadline-reminders.interval-ms:1800000}")
    public void run() {
        try {
            reminderService.remindDue(LocalDateTime.now(RpisTimeZone.zone()));
        } catch (RuntimeException error) {
            // The next scheduled run retries; a failed sweep must never kill the scheduler.
            log.warn("Revision-deadline reminder run failed: {}", error.getMessage());
        }
    }
}
