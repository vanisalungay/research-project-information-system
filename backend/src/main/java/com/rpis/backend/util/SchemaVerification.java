package com.rpis.backend.util;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Read-only checks for the database objects a start-up step would otherwise create.
 *
 * <h2>Why this exists</h2>
 * Several start-up initializers used to create their own schema objects on every boot
 * (unique indexes, a CHECK constraint, two sequences). That made the schema a side effect
 * of starting the application: a load-balanced deployment re-ran the same DDL on every
 * instance start, and a schema change could reach a database without any operator
 * deciding that it should. The project already ships every one of those statements as a
 * versioned file under {@code backend/migration-*.sql}, so the DDL belongs there.
 *
 * <p>What remains useful at start-up is <em>knowing</em> whether the objects are there.
 * These helpers answer exactly that, and nothing else: every method issues a
 * {@code SELECT} against the PostgreSQL catalog. None of them issues DDL or DML, so a
 * caller can verify a database it must not modify.
 *
 * <p>Each method fails closed to {@code false}: a catalog that cannot be read makes the
 * object look absent, which turns into a warning rather than a silent claim that
 * everything is fine.
 */
public final class SchemaVerification {

    /** Every object the application owns lives in the default schema. */
    public static final String SCHEMA = "public";

    private SchemaVerification() {
        // Utility class.
    }

    /**
     * True when a relation - table, view, index or sequence - exists.
     *
     * <p>{@code to_regclass} is PostgreSQL's own name resolver: it returns {@code NULL}
     * for a name that matches nothing instead of raising an error, which is what makes
     * this usable as a check. The name is passed as a bind parameter, so the value can
     * never be read as SQL.
     */
    public static boolean relationExists(JdbcTemplate jdbcTemplate, String relationName) {
        try {
            String resolved = jdbcTemplate.queryForObject(
                    "SELECT to_regclass(?)::text", String.class, SCHEMA + "." + relationName);
            return resolved != null && !resolved.isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    /** True when a named constraint exists in the default schema. */
    public static boolean constraintExists(JdbcTemplate jdbcTemplate, String constraintName) {
        try {
            Integer found = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_constraint WHERE conname = ? AND connamespace = ?::regnamespace",
                    Integer.class, constraintName, SCHEMA);
            return found != null && found > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
