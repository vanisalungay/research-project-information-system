package com.rpis.backend.config;

import com.rpis.backend.util.PostgresAdvisoryLock;
import com.rpis.backend.util.SchemaVerification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

/**
 * Prepares the shared cooperating agency directory (section 02 of the Create Proposal
 * form): the case-insensitive unique index that makes duplicate agencies impossible, and
 * the default agency rows.
 *
 * <h2>Schema changes are a migration, not a start-up step</h2>
 * This class used to create the unique index and insert the defaults on every application
 * start, against whatever database RPIS_DB_URL named. Both are already shipped as
 * backend/migration-add-cooperating-agencies.sql (which contains the same index, the same
 * 21 agency rows and the same ON CONFLICT DO NOTHING). Start-up therefore now does one of
 * two things:
 *
 * <ul>
 *   <li><strong>Default</strong> ({@code rpis.bootstrap.cooperating-agencies.enabled=false},
 *       environment variable {@code RPIS_BOOTSTRAP_COOPERATING_AGENCIES_ENABLED}) - a
 *       read-only check that issues SELECT statements only. No DDL, no INSERT, no UPDATE,
 *       no DELETE, no sequence use. A missing index is reported as a warning naming the
 *       migration file, because the application still serves traffic without it.</li>
 *   <li><strong>Explicitly enabled</strong> - the previous behaviour, applied once, by an
 *       operator who asked for it (a new database, or a database built from schema.sql),
 *       under the cluster lock below.</li>
 * </ul>
 *
 * <p>Nothing here ever updates, renames, deletes or deactivates an existing row: the seed
 * is an INSERT that is skipped by ON CONFLICT DO NOTHING, so an agency an administrator has
 * renamed is preserved exactly as it is.
 */
@Slf4j
@Component
public class CooperatingAgencyInitializer {

    /**
     * The default cooperating agencies, ordered by their full name (the order the
     * checklist shows them in). Only the missing ones are inserted when bootstrap is on.
     */
    public static final List<String> DEFAULT_AGENCIES = List.of(
            "Commission on Higher Education (CHED)",
            "Department of Agriculture (DA)",
            "Department of Education (DepEd)",
            "Department of Energy (DOE)",
            "Department of Environment and Natural Resources (DENR)",
            "Department of Health (DOH)",
            "Department of Information and Communications Technology (DICT)",
            "Department of Labor and Employment (DOLE)",
            "Department of Public Works and Highways (DPWH)",
            "Department of Science and Technology (DOST)",
            "Department of Social Welfare and Development (DSWD)",
            "Department of the Interior and Local Government (DILG)",
            "Department of Tourism (DOT)",
            "Department of Trade and Industry (DTI)",
            "Intellectual Property Office of the Philippines (IPOPHL)",
            "Local Government Unit (LGU)",
            "National Commission on Indigenous Peoples (NCIP)",
            "National Disaster Risk Reduction and Management Council (NDRRMC)",
            "National Economic and Development Authority (NEDA)",
            "Philippine Space Agency (PhilSA)",
            "Philippine Statistics Authority (PSA)");

    /** The index the enabled path creates; the migration documents the same statement. */
    static final String UNIQUE_INDEX = "uk_cooperating_agencies_name_lower";

    /** The file that documents the DDL applied only when this step is explicitly enabled. */
    public static final String MIGRATION_FILE = "backend/migration-add-cooperating-agencies.sql";

    /** How this step is described in cluster-lock log lines. */
    private static final String LOCK_PURPOSE = "the cooperating agency directory";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    /**
     * Whether this start-up may create the index and insert the defaults. False unless
     * rpis.bootstrap.cooperating-agencies.enabled=true, so the default start-up is a
     * read-only verification and never a surprise migration.
     */
    private final boolean enabled;

    public CooperatingAgencyInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource,
            @Value("${rpis.bootstrap.cooperating-agencies.enabled:false}") boolean enabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDirectory() {
        if (!enabled) {
            verifyWithoutChangingAnything();
            return;
        }
        applyDirectory();
    }

    /**
     * The idempotent bootstrap: the case-insensitive unique index and the missing default
     * agencies. Private on purpose - the only caller is the gate above, so no other code
     * path can run DDL/DML by forgetting to check the property.
     *
     * <p>Wrapped in the cluster lock so two instances starting together do not both run the
     * seed. The lock is a courtesy rather than a correctness requirement (the statements are
     * idempotent), so an instance that cannot take it logs and skips instead of failing to
     * start or running the seed anyway.
     */
    private void applyDirectory() {
        try (PostgresAdvisoryLock lock = PostgresAdvisoryLock
                .tryAcquire(dataSource, PostgresAdvisoryLock.PURPOSE_COOPERATING_AGENCIES, LOCK_PURPOSE)
                .orElse(null)) {
            if (lock == null) {
                log.info("Another instance is seeding the cooperating agency directory; this instance "
                        + "will not. The directory ends up identical either way.");
                return;
            }
            seedDirectory();
        } catch (Exception e) {
            log.warn("Failed to initialize the cooperating agencies directory ({}). Nothing further "
                    + "was changed by this instance.", e.getMessage());
        }
    }

    /**
     * Creates the index when missing and inserts only the agencies that are not stored yet.
     * Every statement is idempotent; none of them updates or removes a row.
     */
    private void seedDirectory() {
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS " + UNIQUE_INDEX
                + " ON cooperating_agencies (LOWER(name))");

        for (String name : DEFAULT_AGENCIES) {
            jdbcTemplate.update(
                    "INSERT INTO cooperating_agencies (name) VALUES (?) ON CONFLICT DO NOTHING", name);
        }

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cooperating_agencies", Integer.class);
        log.info("cooperating_agencies directory is ready ({} agencies)", total);
    }

    /**
     * Reports the state of the directory without touching it. Two SELECT statements, one of
     * them a catalog lookup; no DDL and no DML.
     */
    private void verifyWithoutChangingAnything() {
        try {
            boolean indexPresent = SchemaVerification.relationExists(jdbcTemplate, UNIQUE_INDEX);
            Integer total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM cooperating_agencies", Integer.class);
            if (indexPresent) {
                log.info("cooperating_agencies verified read-only: {} row(s), case-insensitive unique index "
                        + "present. Schema bootstrap is disabled, so nothing was changed.", total);
            } else {
                log.warn("The cooperating agencies unique index is MISSING and schema bootstrap is disabled "
                        + "(rpis.bootstrap.cooperating-agencies.enabled=false, environment variable "
                        + "RPIS_BOOTSTRAP_COOPERATING_AGENCIES_ENABLED). Apply {} to add it, or start once "
                        + "with that variable set to true. Nothing was changed by this instance.", MIGRATION_FILE);
            }
        } catch (Exception e) {
            log.warn("Could not verify the cooperating agencies directory ({}). Nothing was changed.",
                    e.getMessage());
        }
    }
}