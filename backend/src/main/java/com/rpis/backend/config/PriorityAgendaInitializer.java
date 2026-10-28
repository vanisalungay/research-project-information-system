package com.rpis.backend.config;

import com.rpis.backend.util.PostgresAdvisoryLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

/**
 * Prepares the Priority Agenda master data (section 05 of the Create Proposal form):
 * it creates the constraints Hibernate cannot express and seeds the current
 * framework so the feature works out of the box.
 * <p>
 * {@code spring.jpa.hibernate.ddl-auto=update} creates the two tables from the
 * entities, but it does not create the coverage-period check, the case-insensitive
 * unique indexes or the partial unique index that guarantees at most one active set.
 * The {@code migration-add-priority-agendas.sql} script documents the same DDL; this
 * initializer is what actually applies it in the running environment, because the
 * SQL files of this project are not executed automatically.
 * <p>
 * Every step is idempotent and runs once after the application context is ready:
 * <ul>
 *   <li>the indexes are created with {@code IF NOT EXISTS}, so a restart never fails
 *       and a concurrent start of two instances is harmless;</li>
 *   <li>the default set and its five agendas are inserted only when they are absent,
 *       so a restart never duplicates them and never overwrites a row an
 *       administrator has since edited or deactivated - in particular an
 *       administrator's newly activated framework is never replaced by the seed.</li>
 * </ul>
 */
@Slf4j
@Component
public class PriorityAgendaInitializer {

    /** The framework seeded on a fresh database: the one the form used to hard-code. */
    public static final String DEFAULT_FRAMEWORK_NAME = "MSUN RIIDE";
    public static final int DEFAULT_COVERAGE_START_YEAR = 2025;
    public static final int DEFAULT_COVERAGE_END_YEAR = 2028;

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

    /** How this step is described in cluster-lock log lines. */
    private static final String LOCK_PURPOSE = "the priority agenda master data";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public PriorityAgendaInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeMasterData() {
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
     * Creates the constraints the entity mapping cannot declare. All are idempotent;
     * the partial unique index is the database-level guarantee that a second active
     * framework can never be stored, even if two administrators activate a set at the
     * same moment.
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
        jdbcTemplate.execute("ALTER TABLE priority_agenda_sets DROP CONSTRAINT IF EXISTS chk_priority_agenda_period");
        jdbcTemplate.execute("ALTER TABLE priority_agenda_sets ADD CONSTRAINT chk_priority_agenda_period "
                + "CHECK (coverage_end_year >= coverage_start_year)");
    }

    /**
     * Seeds the MSUN RIIDE framework only when it is missing, so the form shows the
     * agenda list it always showed until an administrator configures a new one.
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

