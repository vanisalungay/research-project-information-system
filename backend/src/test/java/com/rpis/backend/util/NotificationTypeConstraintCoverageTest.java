package com.rpis.backend.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the database allow-list of {@code notifications.type} against drift.
 *
 * <p>{@code migration-01-db-audit-integrity.sql} created {@code chk_notifications_type}
 * with the values that existed then. A {@code NOT VALID} CHECK is still enforced for
 * every new row, so a type added to {@link NotificationTypes} but not to the CHECK
 * makes its workflow fail at insert time and roll back - which is exactly how the RPS
 * endorsement to OVCRIGE ({@code PROPOSAL_ENDORSED}) answered 409 Conflict.</p>
 *
 * <p>{@code migration-widen-notification-type-check.sql} carries the full list.
 * Asserting the two agree means the next type added to the Java vocabulary cannot
 * silently miss the database.</p>
 */
class NotificationTypeConstraintCoverageTest {

    /** Surefire runs with the backend module as the working directory. */
    private static final Path MIGRATION = Path.of("migration-widen-notification-type-check.sql");

    @Test
    void everyNotificationTypeIsAllowedByTheMigration() throws IOException {
        List<String> types = notificationTypes();
        assertFalse(types.isEmpty(), "the notification vocabulary must not be empty");

        String sql = Files.readString(MIGRATION);
        List<String> missing = new ArrayList<>();
        for (String type : types) {
            if (!sql.contains("'" + type + "'")) {
                missing.add(type);
            }
        }

        assertTrue(missing.isEmpty(),
                "migration-widen-notification-type-check.sql must allow every NotificationTypes value,"
                        + " but is missing " + missing);
    }

    @Test
    void theMigrationReplacesTheConstraintWithoutRewritingExistingRows() throws IOException {
        String sql = Files.readString(MIGRATION);

        assertTrue(sql.contains("chk_notifications_type"),
                "the existing constraint must be replaced, not duplicated under another name");
        assertTrue(sql.contains("DROP CONSTRAINT chk_notifications_type"),
                "the old allow-list must be dropped before the widened one is added");
        assertTrue(sql.contains("NOT VALID"),
                "the replacement stays NOT VALID so unchanged historical rows are not re-validated");
        assertFalse(sql.toUpperCase().contains("UPDATE PUBLIC.NOTIFICATIONS"),
                "the migration must not rewrite existing notification rows");
    }

    /**
     * Every {@code public static final String} of {@link NotificationTypes}: the stored
     * type values. Other members (such as {@code PROPOSAL_WORKFLOW}) are not values.
     */
    private static List<String> notificationTypes() {
        List<String> values = new ArrayList<>();
        for (Field field : NotificationTypes.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)
                    && field.getType() == String.class) {
                try {
                    values.add((String) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Cannot read the notification vocabulary", e);
                }
            }
        }
        return values;
    }
}
