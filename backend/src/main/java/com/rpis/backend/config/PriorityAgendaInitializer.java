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
 * Prepares the Priority Agenda master data (section 05 of the Create Proposal form): the
 * constraints Hibernate cannot express, and the framework the form starts from.
 *
 * <h2>Schema changes are a migration, not a start-up step</h2>
 * This class used to run CREATE UNIQUE INDEX and ALTER TABLE ADD CONSTRAINT on every
 * application start, against whatever database RPIS_DB_URL named. The schema was therefore
 * a side effect of booting the application: every instance of a load-balanced deployment
 * re-applied the same DDL, and a schema change could reach a database without any operator
 * deciding that it should. All of those statements are already shipped as
 * backend/migration-add-priority-agendas.sql, which is where they belong.
 *
 * <p>Start-up now does one of two things:
 * <ul>
 *   <li><strong>Default</strong> (rpis.bootstrap.priority-agendas.enabled=false) - a
 *       read-only check of the constraints and the stored master data. Nothing is created,
 *       inserted, updated or dropped. A missing constraint is reported as a warning naming
 *       the migration file, because the application serves traffic without it.</li>
 *   <li><strong>Explicitly enabled</strong> - the previous behaviour, applied once, by an
 *       operator who asked for it (a new database, or a database built from schema.sql).
 *       Both steps stay idempotent, so an existing row is never overwritten or duplicated.</li>
 * </ul>
 * The step is gated by an explicit property rather than by a profile, so the same rule
 * holds for a developer, for a test run and for a deployment.
 */
@Slf4j
@Component
public class PriorityAgendaInitializer {

    /** The framework seeded on a fresh database: the one the form used to hard-code. */
    public static final String DEFAULT_FRAMEWORK_NAME = "MSUN RIIDE";
    public static final int DEFAULT_COVERAGE_START_YEAR = 2025;
    public static final int DEFAULT_COVERAGE_END_YEAR = 2028;

    /** The file that documents the DDL applied only when this step is explicitly enabled. */
    public static final String MIGRATION_FILE = "backend/migration-add-priority-agendas.sql";

    /**
     * The default agenda items, in display order. The keys are exactly the stable
     * identifiers the proponent form and the Proposal Announcement catalogue already
     * persist, so every historical proposal and every stored announcement keeps
     * resolving after the migration.
     */
    public static final List<Object[]> DEFAULT_AGENDAS = List.of(
            new Object[] { "dagat", "DAGAT Program", "Commodity",
                    "Development of Aquatic Governance and Advanced Technologies" },
            new Object[] { "punla", "PUNLA Program", "Priority Topic",
                    "Progressive Upliftment for Nutrition, Livelihood, and Agriculture" },
            new Object[] { "kalikasan", "KALIKASAN Program", "Sector",
                    "Knowledge and Actions for Lasting Impact: Keeping Atmospheres, Seas, and Nature Sustainable" },
            new Object[] { "negosyo", "NEGOSYO Program", "Sector",
                    "Nurturing Entrepreneurship and Growth through Outstanding Strategic Yielding Opportunities" },
            new Object[] { "tanglaw", "TANGLAW Program", "Sector",
                    "Transforming Academia through Nurturing Growth, Leadership, Advancement, and Wisdom" });

    private static final String SINGLE_ACTIVE_INDEX = "uk_priority_agenda_set_single_active";
    private static final String PERIOD_CONSTRAINT = "chk_priority_agenda_period";

    /** How this step is described in cluster-lock log lines. */
    private static final String LOCK_PURPOSE = "the priority agenda master data";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    /**
     * Whether this start-up may write the schema and seed the master data. Off unless
     * rpis.bootstrap.priority-agendas.enabled=true, so the default start-up is a read-only
     * verification and never a surprise migration.
     */
    private final boolean enabled;

    public PriorityAgendaInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource,
            @Value("${rpis.bootstrap.priority-agendas.enabled:false}") boolean enabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeMasterData() {
        if (!enabled) {
            verifyWithoutChangingAnything();
            return;
        }

        // One instance at a time. The constraint DDL below is genuinely idempotent, but the
        // seed is a check-then-insert: two instances that both see "the framework is absent"
        // both try to insert it and the second fails on the unique index, which aborts that
        // instance's start-up. This step also shares its row with SyntheticTestDataSeeder, so
        // the lock is shared with it rather than private to this class.
        try (PostgresAdvisoryLock lock = PostgresAdvisoryLock
                .tryAcquire(dataSource, PostgresAdvisoryLock.PURPOSE_REFERENCE_DATA, LOCK_PURPOSE)
                .orElse(null)) {
            if (lock == null) {
                log.info("Another instance is initialising the priority agenda master data; this "
                        + "instance will not. The constraints and the default framework end up "
                        + "identical either way.");
                return;
            }
            try {
                createConstraints();
                seedDefaultFramework();
            } catch (Exception e) {
                log.warn("Failed to initialize the priority agenda master data: {}", e.getMessage());
            }
        }
    }

    /**
     * Reports the state of the schema objects and the stored master data without touching
     * either. This is what a normal start-up runs, so a single log line shows whether a
     * pending migration is actually outstanding. No DDL and no DML is issued.
     */
    private void verifyWithoutChangingAnything() {
        try {
            boolean constraintsPresent = SchemaVerification.relationExists(jdbcTemplate, SINGLE_ACTIVE_INDEX)
                    && SchemaVerification.constraintExists(jdbcTemplate, PERIOD_CONSTRAINT);
            Integer sets = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM priority_agenda_sets", Integer.class);
            Integer agendas = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM priority_agendas", Integer.class);

            if (constraintsPresent) {
                log.info("Priority agenda master data verified read-only: constraints present, {} framework(s) "
                        + "and {} agenda item(s) stored. Schema bootstrap is disabled, so nothing was changed.",
                        sets, agendas);
            } else {
                log.warn("Priority agenda schema constraints are MISSING and schema bootstrap is disabled "
                        + "(rpis.bootstrap.priority-agendas.enabled=false, environment variable \""
                        + "RPIS_BOOTSTRAP_PRIORITY_AGENDAS_ENABLED). Apply {} to add them. "
                        + "Nothing was changed by this instance.", MIGRATION_FILE);
            }
        } catch (Exception e) {
            log.warn("Could not verify the priority agenda master data ({}). Nothing was changed.",
                    e.getMessage());
        }
    }

    /**
     * Creates the constraints the entity mapping cannot declare. Only reached on an
     * explicitly enabled bootstrap run. All are idempotent; the partial unique index is the
     * database-level guarantee that a second active framework can never be stored, even if
     * two administrators activate a set at the same moment.
     */
    private void createConstraints() {
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_priority_agenda_set_framework_period "
                + "ON priority_agenda_sets (LOWER(framework_name), coverage_start_year, coverage_end_year)");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS " + SINGLE_ACTIVE_INDEX
                + " ON priority_agenda_sets (active) WHERE active = TRUE");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_priority_agendas_set_key "
                + "ON priority_agendas (agenda_set_id, LOWER(agenda_key))");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_priority_agendas_set_name_lower "
                + "ON priority_agendas (agenda_set_id, LOWER(name))");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_priority_agendas_set_active "
                + "ON priority_agendas (agenda_set_id, active, display_order)");
        jdbcTemplate.execute("ALTER TABLE priority_agenda_sets DROP CONSTRAINT IF EXISTS " + PERIOD_CONSTRAINT);
        jdbcTemplate.execute("ALTER TABLE priority_agenda_sets ADD CONSTRAINT " + PERIOD_CONSTRAINT
                + " CHECK (coverage_end_year >= coverage_start_year)");
    }

    /**
     * Seeds the MSUN RIIDE framework only when it is missing, so the form shows the agenda
     * list it always showed until an administrator configures a new one. Only reached on an
     * explicitly enabled bootstrap run.
     */
    private void seedDefaultFramework() {
        int inserted = jdbcTemplate.update(
                "INSERT INTO priority_agenda_sets "
                        + "(framework_name, coverage_start_year, coverage_end_year, active, created_at, updated_at) "
                        + "SELECT ?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP "
                        + "WHERE NOT EXISTS (SELECT 1 FROM priority_agenda_sets "
                        + "WHERE LOWER(framework_name) = LOWER(?) AND coverage_start_year = ? "
                        + "AND coverage_end_year = ?) "
                        + "ON CONFLICT DO NOTHING",
                DEFAULT_FRAMEWORK_NAME, DEFAULT_COVERAGE_START_YEAR, DEFAULT_COVERAGE_END_YEAR,
                DEFAULT_FRAMEWORK_NAME, DEFAULT_COVERAGE_START_YEAR, DEFAULT_COVERAGE_END_YEAR);
        if (inserted == 0) {
            log.debug("Default priority agenda framework already present; nothing seeded.");
            return;
        }

        Long setId = jdbcTemplate.queryForObject(
                "SELECT id FROM priority_agenda_sets WHERE LOWER(framework_name) = LOWER(?) "
                        + "AND coverage_start_year = ? AND coverage_end_year = ?",
                Long.class, DEFAULT_FRAMEWORK_NAME, DEFAULT_COVERAGE_START_YEAR, DEFAULT_COVERAGE_END_YEAR);

        int order = 1;
        for (Object[] agenda : DEFAULT_AGENDAS) {
            jdbcTemplate.update(
                    "INSERT INTO priority_agendas "
                            + "(agenda_set_id, agenda_key, name, sub_label, description, display_order, active, "
                            + "created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                            + "ON CONFLICT DO NOTHING",
                    setId, agenda[0], agenda[1], agenda[2], agenda[3], order++);
        }
        log.info("Seeded the default priority agenda framework {} {}-{} with {} agendas",
                DEFAULT_FRAMEWORK_NAME, DEFAULT_COVERAGE_START_YEAR, DEFAULT_COVERAGE_END_YEAR,
                DEFAULT_AGENDAS.size());
    }
}
