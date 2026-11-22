package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.util.NotificationEvent;
import com.rpis.backend.util.NotificationTypes;
import com.rpis.backend.util.PostgresAdvisoryLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Creates the opt-in revision-deadline reminders.
 *
 * <p>Only proposals with a real RPS-set revision deadline inside the configured window
 * are considered. Each reminder is idempotent: the dedupe key is the proposal and the
 * deadline date, and a PostgreSQL advisory lock serializes the sweep across instances.
 * A notification is produced only when the Proponent has explicitly selected
 * "Revision Deadline Reminders"; a missing preference is opt-out.
 */
@Slf4j
@Service
public class RevisionDeadlineReminderService {

    /** Statuses in which the Proponent can still submit a revision. */
    static final List<String> REVISION_STATUSES =
            List.of("RPS_RETURNED", "REC_REVISION", "REVISION");

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ProposalRepository proposalRepository;
    private final NotificationService notificationService;
    private final DataSource dataSource;
    private final long windowHours;

    public RevisionDeadlineReminderService(
            ProposalRepository proposalRepository,
            NotificationService notificationService,
            DataSource dataSource,
            @Value("${rpis.notifications.deadline-reminders.window-hours:72}") long windowHours) {
        this.proposalRepository = proposalRepository;
        this.notificationService = notificationService;
        this.dataSource = dataSource;
        this.windowHours = Math.max(1, windowHours);
    }

    /**
     * Runs one sweep. Returns the number of reminder rows actually written.
     * Exposed for tests; the scheduler calls it with the application clock.
     */
    public int remindDue(LocalDateTime now) {
        if (now == null) {
            return 0;
        }
        Optional<PostgresAdvisoryLock> lock;
        try {
            lock = PostgresAdvisoryLock.tryAcquire(dataSource,
                    PostgresAdvisoryLock.PURPOSE_REVISION_DEADLINE_REMINDERS,
                    "notification revision-deadline reminders");
        } catch (IllegalStateException lockError) {
            log.warn("Skipping revision-deadline reminders: {}", lockError.getMessage());
            return 0;
        }
        if (lock.isEmpty()) {
            log.debug("Another instance is running revision-deadline reminders; skipping.");
            return 0;
        }

        try (PostgresAdvisoryLock held = lock.get()) {
            return runSweep(now);
        } catch (RuntimeException sweepError) {
            log.warn("Revision-deadline reminder sweep failed: {}", sweepError.getMessage());
            return 0;
        }
    }

    /** The sweep without the advisory lock; package-private so tests can pin the clock. */
    int runSweep(LocalDateTime now) {
        LocalDateTime until = now.plusHours(windowHours);
        List<Proposal> due = proposalRepository.findRevisionDeadlinesDue(
                now, until, REVISION_STATUSES);
        int written = 0;

        for (Proposal proposal : due) {
            try {
                written += remindOne(proposal);
            } catch (RuntimeException error) {
                // One bad row must not stop the rest of the sweep.
                log.warn("Could not create the revision reminder for proposal {}: {}",
                        proposal.getId(), error.getMessage());
            }
        }
        if (written > 0) {
            log.info("Created {} revision-deadline reminder(s).", written);
        }
        return written;
    }

    private int remindOne(Proposal proposal) {
        User proponent = proposal.getProponent();
        LocalDateTime deadline = proposal.getRevisionDeadline();
        if (proponent == null || deadline == null) {
            return 0;
        }

        String dedupeKey = "revision-deadline:" + proposal.getId() + ":"
                + deadline.toLocalDate().format(DATE);
        String message = "The revision deadline for \"" + proposal.getProjectTitle()
                + "\" is " + deadline.toLocalDate().format(DATE)
                + ". Please submit your revision before the deadline.";

        return notificationService.notifyUsers(
                List.of(proponent), message, "Revision Deadline Reminder",
                NotificationTypes.REVISION_DEADLINE_REMINDER, proposal.getId(),
                NotificationEvent.REVISION_DEADLINE_REMINDER.withDedupeKey(dedupeKey),
                "PROPONENT").size();
    }
}
