package com.rpis.backend.util;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * A PostgreSQL advisory lock, so a start-up step that must happen once across a
 * cluster actually happens once.
 *
 * <h2>The problem this solves</h2>
 * Two start-up steps here are safe on a single instance but not on several at once:
 *
 * <ul>
 *   <li>{@code SyntheticTestDataSeeder} generates a whole data set. Two instances
 *       starting together against an empty database both see "no batch marker" and
 *       both generate, producing duplicate accounts and proposals and tripping the
 *       unique constraint on {@code users.email}.</li>
 *   <li>{@code PasswordMigrationService} re-hashes every plain-text password. Two
 *       instances each read the same rows, each compute their own BCrypt hash and
 *       race to write them. The result is not corrupt - both hashes verify - but the
 *       work is duplicated, the last writer wins, and the reported count is
 *       meaningless.</li>
 * </ul>
 *
 * <p>A JVM-level lock cannot fix either case, because the two instances are separate
 * processes. The lock has to live in the one thing they share: the database.
 * PostgreSQL advisory locks are that primitive.
 *
 * <h2>Why {@code pg_try_advisory_lock} and not {@code pg_advisory_lock}</h2>
 * The {@code try} form never blocks. A start-up step that waited would hang a rolling
 * deployment for as long as the other instance takes, turning a slow seed into a
 * failed deploy. Instead an instance that cannot get the lock records that fact and
 * moves on, because the other instance is already doing the work.
 *
 * <h2>Lock identity</h2>
 * The lock uses the two-integer form, {@code (classid, objid)}, so it is namespaced to
 * this application and cannot collide with an advisory lock taken by anything else on
 * the same database:
 *
 * <ul>
 *   <li>{@code classid} is always {@link #NAMESPACE}, the four ASCII bytes
 *       {@code "RPIS"} read as a 32-bit integer.</li>
 *   <li>{@code objid} identifies the step, from the {@code PURPOSE_*} constants.</li>
 * </ul>
 *
 * <h2>Release</h2>
 * A PostgreSQL advisory lock belongs to the <em>session</em>, not to a transaction, so
 * it must be released on the same connection that took it. This class holds that
 * connection for its lifetime and, on {@link #close()}, explicitly unlocks and then
 * has the pool destroy the physical connection. Destroying it matters: otherwise the
 * connection would go back to the pool with the lock still held, and a later borrower
 * would be holding a lock nobody remembers taking. The eviction makes the lock lifetime
 * exactly the lifetime of this object.
 *
 * <p>This is not a substitute for {@link DatabaseSafetyGuard}: the lock decides
 * <em>which instance</em> runs a step, and the guard decides <em>which database</em> it
 * may run against. Both are needed, and the guard is still asked first.
 */
@Slf4j
public final class PostgresAdvisoryLock implements AutoCloseable {

    /**
     * The {@code classid} half of every lock this application takes: the four ASCII
     * bytes {@code R}, {@code P}, {@code I}, {@code S} as a 32-bit integer. It keeps
     * RPIS locks in their own namespace so they cannot be confused with an advisory
     * lock taken by another application on the same database.
     */
    public static final int NAMESPACE = 0x52504953;

    /** {@code objid} for the bulk plain-text password migration. */
    public static final int PURPOSE_PASSWORD_MIGRATION = 1;

    /** {@code objid} for the synthetic load-test data generation. */
    public static final int PURPOSE_SYNTHETIC_SEED = 2;

    /**
     * {@code objid} for creating shared reference data - the default priority agenda
     * framework. Deliberately shared by every component that may create it, rather than
     * private to one of them: two components with private locks would still race each other
     * for the same row.
     */
    public static final int PURPOSE_REFERENCE_DATA = 3;

    private final DataSource dataSource;
    private final Connection connection;
    private final int purpose;
    private final String purposeName;
    private boolean held = true;

    private PostgresAdvisoryLock(DataSource dataSource, Connection connection, int purpose, String purposeName) {
        this.dataSource = dataSource;
        this.connection = connection;
        this.purpose = purpose;
        this.purposeName = purposeName;
    }

    /**
     * Tries to take the lock for one start-up step, without blocking.
     *
     * @param dataSource  the pool to borrow the lock-holding connection from
     * @param purpose     one of the {@code PURPOSE_*} constants
     * @param purposeName how the step is described in log lines
     * @return the held lock, or {@link Optional#empty()} when another instance already
     *         holds it, in which case the caller must not run the step
     * @throws IllegalStateException when the lock could not even be requested, for
     *                               example because the database is unreachable. The
     *                               caller should treat this the same as contention and
     *                               not run the step: running it without proof of
     *                               exclusivity is the thing being prevented.
     */
    public static Optional<PostgresAdvisoryLock> tryAcquire(
            DataSource dataSource, int purpose, String purposeName) {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT pg_try_advisory_lock(?, ?)")) {
                statement.setInt(1, NAMESPACE);
                statement.setInt(2, purpose);
                try (ResultSet resultSet = statement.executeQuery()) {
                    boolean acquired = resultSet.next() && resultSet.getBoolean(1);
                    if (!acquired) {
                        // Nothing was taken, so the connection can go straight back.
                        closeQuietly(connection);
                        return Optional.empty();
                    }
                }
            }
            log.info("Acquired the cluster lock for {} (namespace {}, purpose {}).",
                    purposeName, NAMESPACE, purpose);
            return Optional.of(new PostgresAdvisoryLock(dataSource, connection, purpose, purposeName));
        } catch (SQLException e) {
            closeQuietly(connection);
            throw new IllegalStateException(
                    "Could not request the cluster lock for " + purposeName + ": " + e.getMessage(), e);
        }
    }

    /** True while this object still owns the lock. */
    public boolean isHeld() {
        return held;
    }

    /**
     * Releases the lock and destroys the connection that held it. Idempotent.
     *
     * <p>The physical connection is evicted rather than returned to the pool, so a
     * failure to unlock cannot leave an orphaned advisory lock on a pooled session.
     */
    @Override
    public void close() {
        if (!held) {
            return;
        }
        held = false;
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT pg_advisory_unlock(?, ?)")) {
                statement.setInt(1, NAMESPACE);
                statement.setInt(2, purpose);
                try (ResultSet resultSet = statement.executeQuery()) {
                    boolean released = resultSet.next() && resultSet.getBoolean(1);
                    if (!released) {
                        log.warn("The cluster lock for {} was no longer held when it was released; "
                                + "the session will be discarded.", purposeName);
                    }
                }
            }
            log.info("Released the cluster lock for {}.", purposeName);
        } catch (SQLException e) {
            log.warn("Could not release the cluster lock for {} explicitly ({}). The connection is "
                    + "being discarded, which releases any lock still held on it.",
                    purposeName, e.getMessage());
        } finally {
            evictAndClose();
        }
    }

    /**
     * Destroys the underlying session. {@code evictConnection} is HikariCP's supported
     * way to remove a checked-out connection from the pool; the subsequent
     * {@code close()} then closes it for real instead of returning it to the pool.
     */
    private void evictAndClose() {
        try {
            if (dataSource instanceof HikariDataSource hikari) {
                hikari.evictConnection(connection);
            }
        } catch (RuntimeException e) {
            log.debug("Could not evict the lock connection from the pool: {}", e.getMessage());
        }
        closeQuietly(connection);
    }

    private static void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            log.debug("Could not close the lock connection: {}", e.getMessage());
        }
    }
}
