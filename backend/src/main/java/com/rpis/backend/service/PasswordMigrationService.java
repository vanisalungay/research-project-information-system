package com.rpis.backend.service;

import com.rpis.backend.model.User;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.util.PostgresAdvisoryLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

/**
 * Converts accounts whose password is still stored as plain text into BCrypt hashes.
 *
 * <h2>Why a bulk pass as well as the lazy one</h2>
 * {@link UserService#login} already upgrades a legacy row the moment its owner signs
 * in, which is enough to keep everybody working. But an account that has not signed in
 * since the change would still sit in the database as readable text, and reading the
 * password column is exactly the exposure this work removes. This pass closes that
 * window in one go, without needing anybody to log in first.
 *
 * <h2>Safety properties</h2>
 * <ul>
 *   <li><strong>Off by default.</strong> Runs only when
 *       {@code rpis.security.password-migration.enabled=true}, so a deployment decides
 *       when it happens rather than discovering it.</li>
 *   <li><strong>Only one instance runs it.</strong> The pass is wrapped in a PostgreSQL
 *       advisory lock, so two instances starting at the same time cannot both convert
 *       the table. See {@link PostgresAdvisoryLock}.</li>
 *   <li><strong>Idempotent.</strong> A row is selected only when its value is not
 *       already a BCrypt hash, so re-running converts nothing and never double-hashes.
 *       It is safe to run on every start-up.</li>
 *   <li><strong>Cannot lock anyone out.</strong> Only the encoding changes; the
 *       password itself is preserved, so the same credentials keep working. If the
 *       process dies half way through, the rows it already converted are hashes and the
 *       rest are still plain text - and both shapes verify, so the database is never in
 *       a state where nobody can sign in.</li>
 *   <li><strong>Writes nothing to the log but a count.</strong> No password, no hash and
 *       no e-mail address is logged, because a migration log is exactly the kind of
 *       artefact that ends up in a ticket or a chat message.</li>
 * </ul>
 *
 * <p>The conversion goes through the entity and its repository rather than raw SQL, so
 * the {@code users} table is touched only through the mappings the rest of the
 * application uses. The scan is paged, so a large table is never loaded in one go.
 */
@Slf4j
@Component
public class PasswordMigrationService {

    /** How many accounts are loaded and converted per batch. */
    private static final int BATCH_SIZE = 200;

    /** How the pass is described in lock log lines. */
    private static final String LOCK_PURPOSE = "the plain-text password migration";

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final DataSource dataSource;

    @Value("${rpis.security.password-migration.enabled:false}")
    private boolean enabled;

    public PasswordMigrationService(UserRepository userRepository,
                                    PasswordService passwordService,
                                    DataSource dataSource) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.dataSource = dataSource;
    }

    /**
     * Runs the conversion once the application is ready, on at most one instance.
     *
     * <p>Deliberately not a hard failure: if it cannot complete, every affected account
     * is still upgraded on its next successful sign-in by {@link UserService#login}, so
     * a failed migration degrades rather than blocks.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void migrateOnStartup() {
        if (!enabled) {
            log.info("Password migration is disabled (rpis.security.password-migration.enabled=false). "
                    + "Legacy plain-text passwords are still upgraded on the owner's next sign-in.");
            return;
        }

        Optional<PostgresAdvisoryLock> acquired;
        try {
            acquired = PostgresAdvisoryLock.tryAcquire(
                    dataSource, PostgresAdvisoryLock.PURPOSE_PASSWORD_MIGRATION, LOCK_PURPOSE);
        } catch (IllegalStateException e) {
            // The lock could not even be requested. Running the pass without proof of
            // exclusivity is the thing the lock exists to prevent, so this instance
            // does not run it at all.
            log.warn("Could not request the cluster lock for the password migration ({}). Nothing was "
                    + "converted by this instance; affected accounts are still upgraded on their "
                    + "next successful sign-in.", e.getMessage());
            return;
        }

        if (acquired.isEmpty()) {
            log.info("Another instance is already running the password migration, so this instance "
                    + "will not run it. Affected accounts are still upgraded on their next "
                    + "successful sign-in.");
            return;
        }

        try (PostgresAdvisoryLock held = acquired.get()) {
            int converted = migratePlainTextPasswords();
            if (converted == 0) {
                log.info("Password migration: no plain-text passwords found.");
            } else {
                log.info("Password migration: converted {} account(s) to BCrypt.", converted);
            }
        } catch (Exception e) {
            log.warn("Password migration did not complete ({}). Affected accounts are still upgraded "
                    + "on their next successful sign-in.", e.getMessage());
        }
    }

    /**
     * Converts every account whose stored password is not yet a hash.
     *
     * <p>Callers are expected to hold the cluster lock; this method does not take one
     * itself so that the whole pass is covered by a single acquisition.
     *
     * @return how many accounts were converted
     */
    @Transactional
    public int migratePlainTextPasswords() {
        int converted = 0;
        int page = 0;

        while (true) {
            List<User> batch = userRepository.findAll(PageRequest.of(page, BATCH_SIZE)).getContent();
            if (batch.isEmpty()) {
                break;
            }

            boolean touched = false;
            for (User user : batch) {
                String stored = user.getPassword();
                if (stored == null || stored.isBlank() || passwordService.isHashed(stored)) {
                    // Already hashed, or an account with no password at all: nothing to do.
                    continue;
                }
                // The value read from the column IS the password - that is the problem
                // being fixed - so it can be hashed directly without anyone knowing it.
                user.setPassword(passwordService.hash(stored));
                converted++;
                touched = true;
            }

            if (touched) {
                userRepository.saveAll(batch);
            }
            page++;
        }

        return converted;
    }

    /**
     * Counts the accounts whose password is still plain text.
     *
     * <p>Intended for an operator to run before and after a deployment: it returns a
     * number and nothing else, so it is safe to print in a deployment log.
     *
     * <p>The count is done by the database rather than in memory. The previous
     * implementation loaded every account with {@code findAll()} and filtered the list,
     * which meant an operator asking "how many are left?" pulled the entire user table
     * into the heap to produce a single integer.
     */
    public long countPlainTextPasswords() {
        return userRepository.countPlainTextPasswords();
    }
}