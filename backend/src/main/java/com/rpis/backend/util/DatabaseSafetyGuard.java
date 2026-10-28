package com.rpis.backend.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Refuses to let a data-writing bootstrap step run against the wrong database.
 *
 * <p>The RPIS repository contains several start-up steps that create rows
 * (default accounts, version backfill) and one developer-facing step that creates
 * a whole synthetic data set. Those steps must never be able to write into the
 * live {@code rpis_db}. Each of them asks this guard first, so a mis-typed
 * configuration or a forgotten profile fails loudly instead of silently
 * polluting production data.
 *
 * <p>The guard reads the URL from the {@link DataSource} itself rather than from
 * configuration properties, so it cannot be fooled by a property that is
 * overridden later in the start-up sequence. This is the same technique already
 * used by {@code ProposalConcurrencyPostgresTest}, which refuses to run unless it
 * is connected to {@code rpis_test}.
 *
 * <p>Nothing here ever writes, truncates or drops anything: the guard is a pure
 * read of the connection metadata.
 */
public final class DatabaseSafetyGuard {

    private DatabaseSafetyGuard() {
        // Utility class.
    }

    /**
     * The database the automated tests and the synthetic data set are allowed to
     * write to. Anything else is treated as production.
     */
    public static final String TEST_DATABASE_MARKER = "rpis_test";

    /**
     * Returns the JDBC URL the given data source is actually connected to.
     *
     * @throws IllegalStateException when the connection metadata cannot be read
     */
    public static String currentUrl(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalStateException("No DataSource is available to verify the target database.");
        }
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getURL();
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Could not read the database URL to verify the target database: " + e.getMessage(), e);
        }
    }

    /**
     * True when the data source points at a database whose URL contains
     * {@code marker} (case-insensitive).
     */
    public static boolean targetsDatabase(DataSource dataSource, String marker) {
        if (marker == null || marker.isBlank()) {
            return false;
        }
        String url = currentUrl(dataSource);
        return url != null && url.toLowerCase().contains(marker.toLowerCase());
    }

    /**
     * Fails unless the data source points at the isolated test database.
     *
     * <p>Used by every synthetic-data entry point. The returned URL is handy for
     * logging, so callers can record which database they actually seeded.
     *
     * @throws ResponseStatusException with 403 when the target is not the test
     *                                 database, so an accidental HTTP trigger of a
     *                                 seeding endpoint cannot mutate production
     */
    public static String requireTestDatabase(DataSource dataSource, String action) {
        String url = currentUrl(dataSource);
        if (url == null || !url.toLowerCase().contains(TEST_DATABASE_MARKER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Refusing to " + action + ": this step may only run against the isolated '"
                            + TEST_DATABASE_MARKER + "' database, but the application is connected to '"
                            + url + "'. Nothing was written.");
        }
        return url;
    }

    /**
     * Describes the target database for an audit/log line. The URL is trimmed to
     * its database name so credentials embedded in a URL are never logged.
     */
    public static String describe(DataSource dataSource) {
        String url = currentUrl(dataSource);
        if (url == null) {
            return "<unknown>";
        }
        int slash = url.lastIndexOf('/');
        return slash >= 0 && slash < url.length() - 1 ? url.substring(slash + 1) : url;
    }
}
