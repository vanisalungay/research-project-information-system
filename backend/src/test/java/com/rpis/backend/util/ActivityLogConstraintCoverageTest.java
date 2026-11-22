package com.rpis.backend.util;

import com.rpis.backend.model.ActivityLog;
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
 * Guards the database allow-lists of {@code activity_logs.action} and
 * {@code activity_logs.module} against drift.
 *
 * <p>{@code migration-01-db-audit-integrity.sql} created both CHECK constraints with
 * the values that existed then. A {@code NOT VALID} CHECK is still enforced for every
 * new row, so an action or module added to {@link ActivityLog} but not to the CHECK
 * makes the activity insert fail - and, inside the same transaction, the administrative
 * action it describes rolled back with it. That is exactly how the first Priority
 * Agenda action failed after logging was added for every RPS Administration module.</p>
 *
 * <p>{@code migration-widen-activity-log-checks.sql} carries the full lists. Asserting
 * the two agree means the next action or module added to the Java vocabulary cannot
 * silently miss the database.</p>
 */
class ActivityLogConstraintCoverageTest {

    /** Surefire runs with the backend module as the working directory. */
    private static final Path MIGRATION = Path.of("migration-widen-activity-log-checks.sql");

    @Test
    void everyActivityActionAndModuleIsAllowedByTheMigration() throws IOException {
        String sql = Files.readString(MIGRATION);

        List<String> actions = values("ACTION_");
        List<String> modules = values("MODULE_");
        assertFalse(actions.isEmpty(), "the activity action vocabulary must not be empty");
        assertFalse(modules.isEmpty(), "the activity module vocabulary must not be empty");

        List<String> missing = new ArrayList<>();
        for (String action : actions) {
            if (!sql.contains("'" + action + "'")) {
                missing.add("action " + action);
            }
        }
        for (String module : modules) {
            if (!sql.contains("'" + module + "'")) {
                missing.add("module " + module);
            }
        }

        assertTrue(missing.isEmpty(),
                "migration-widen-activity-log-checks.sql must allow every ActivityLog value,"
                        + " but is missing " + missing);
    }

    @Test
    void theMigrationReplacesBothConstraintsWithoutRewritingExistingRows() throws IOException {
        String sql = Files.readString(MIGRATION);

        assertTrue(sql.contains("DROP CONSTRAINT chk_activity_logs_action"),
                "the old action allow-list must be dropped before the widened one is added");
        assertTrue(sql.contains("DROP CONSTRAINT chk_activity_logs_module"),
                "the old module allow-list must be dropped before the widened one is added");
        assertTrue(sql.contains("chk_activity_logs_action") && sql.contains("chk_activity_logs_module"),
                "both constraints must be recreated under their existing names");
        assertTrue(sql.contains("NOT VALID"),
                "the replacements stay NOT VALID so unchanged historical rows are not re-validated");
        String upper = sql.toUpperCase();
        assertFalse(upper.contains("UPDATE PUBLIC.ACTIVITY_LOGS"),
                "the migration must not rewrite existing activity rows");
        assertFalse(upper.contains("DELETE FROM PUBLIC.ACTIVITY_LOGS"),
                "the migration must not delete existing activity rows");
    }

    /** Every {@code public static final String} starting with the given prefix. */
    private static List<String> values(String prefix) {
        List<String> values = new ArrayList<>();
        for (Field field : ActivityLog.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (field.getName().startsWith(prefix)
                    && Modifier.isStatic(modifiers)
                    && Modifier.isFinal(modifiers)
                    && field.getType() == String.class) {
                try {
                    values.add((String) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Cannot read the activity vocabulary", e);
                }
            }
        }
        return values;
    }
}
