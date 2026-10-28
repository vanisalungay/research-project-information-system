package com.rpis.backend.dev;

import com.rpis.backend.model.Certification;
import com.rpis.backend.model.CooperatingAgency;
import com.rpis.backend.model.EndorsementRequest;
import com.rpis.backend.model.EndorsementRequestEvent;
import com.rpis.backend.model.Notification;
import com.rpis.backend.model.PriorityAgenda;
import com.rpis.backend.model.PriorityAgendaSet;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.ProposalBudget;
import com.rpis.backend.model.ProposalLimitation;
import com.rpis.backend.model.ProposalLogFrame;
import com.rpis.backend.model.ProposalOtherProject;
import com.rpis.backend.model.ProposalPersonnel;
import com.rpis.backend.model.ProposalPriorityAgenda;
import com.rpis.backend.model.ProposalReview;
import com.rpis.backend.model.ProposalSite;
import com.rpis.backend.model.ProposalVersion;
import com.rpis.backend.model.QuarterlyProgressReport;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.CertificationRepository;
import com.rpis.backend.repository.CooperatingAgencyRepository;
import com.rpis.backend.repository.EndorsementRequestEventRepository;
import com.rpis.backend.repository.EndorsementRequestRepository;
import com.rpis.backend.repository.NotificationRepository;
import com.rpis.backend.repository.PriorityAgendaRepository;
import com.rpis.backend.repository.PriorityAgendaSetRepository;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.ProposalReviewRepository;
import com.rpis.backend.repository.ProposalVersionRepository;
import com.rpis.backend.repository.QuarterlyProgressReportRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.PasswordService;
import com.rpis.backend.util.DatabaseSafetyGuard;
import com.rpis.backend.util.PostgresAdvisoryLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Generates a realistic synthetic data set for load and stress testing.
 *
 * <h2>Safety</h2>
 * This class can only ever write to the isolated test database:
 * <ul>
 *   <li>it is bound to the {@code loadtest} profile, which is the only profile whose
 *       datasource points at {@code rpis_test};</li>
 *   <li>it additionally calls {@link DatabaseSafetyGuard#requireTestDatabase} as its
 *       very first statement, so even a mis-set profile aborts before writing
 *       anything. A production database can therefore never be seeded by accident.</li>
 * </ul>
 *
 * <h2>Marking</h2>
 * Every synthetic row is recognisable:
 * <ul>
 *   <li>accounts carry the {@value #EMAIL_LOCAL_PREFIX} local-part prefix in their
 *       e-mail address (for example {@code loadtest.proponent.0001@msunaawan.edu.ph})
 *       and the {@value #MARKER} prefix in their display name;</li>
 *   <li>proposals, notifications and endorsements carry the {@value #MARKER} prefix in
 *       their title;</li>
 *   <li>the ids written are recorded in the {@value #REGISTRY_TABLE} table.</li>
 * </ul>
 *
 * <h2>Repeatability</h2>
 * The generator is deterministic ({@code new Random(20260101L)}), so two runs
 * produce the same data, and it is idempotent: a batch marker row in
 * {@value #REGISTRY_TABLE} makes a second run skip the whole generation instead of
 * duplicating rows. Delete the marker row (or the whole table) to force a rebuild.
 *
 * <h2>Realism</h2>
 * Timestamps are spread over several years and months rather than being stamped
 * with "now", statuses cover the whole workflow, and every relationship is a real
 * foreign key (proposal -> proponent, version -> proposal, notification -> user,
 * endorsement -> proponent/dean/announcement, report -> proposal).
 */
@Slf4j
@Component
@Profile("loadtest")
public class SyntheticTestDataSeeder implements ApplicationRunner {

    /** Prefix that marks every synthetic title. */
    static final String MARKER = "[SYNTHETIC]";
    /**
     * Local-part prefix that marks every synthetic account.
     *
     * <p>The domain has to stay {@code @msunaawan.edu.ph} because the login endpoint
     * only accepts that domain - changing it for test convenience would weaken a real
     * rule. The {@code loadtest.} local part is therefore the marker, and it cannot
     * collide with a real address because no real account is named that way.
     */
    static final String EMAIL_LOCAL_PREFIX = "loadtest.";
    /** The only e-mail domain RPIS accepts for an account. */
    static final String EMAIL_DOMAIN = "@msunaawan.edu.ph";
    /** Shared password of every synthetic account (test data only). */
    static final String PASSWORD = "LoadTest#2026";
    /** The registry table this generator owns. It is never created in production. */
    static final String REGISTRY_TABLE = "synthetic_seed_registry";
    /** Marker key for this generator version. */
    static final String BATCH_KEY = "rpis-loadtest-v1";

    /** How the generation pass is described in cluster-lock log lines. */
    private static final String LOCK_PURPOSE = "the synthetic load-test data generation";

    /** How the shared reference-data step is described in cluster-lock log lines. */
    private static final String LOCK_PURPOSE_REFERENCE = "the default priority agenda framework";

    private static final long RANDOM_SEED = 20260101L;

    private static final int PROPONENT_COUNT = 500;
    private static final int DEAN_COUNT = 12;
    private static final int PROPOSAL_COUNT = 1200;
    private static final int ANNOUNCEMENT_COUNT = 12;
    private static final int NOTIFICATIONS_PER_PROPONENT = 6;
    private static final int ENDORSEMENT_COUNT = 220;
    private static final int QUARTERLY_REPORT_COUNT = 140;

    /**
     * The proposal workflow statuses with a relative weight, so the set looks like a
     * live system (mostly in-flight work, with a long tail of finished and rejected
     * records) instead of one status repeated 1200 times.
     */
    private static final Object[][] STATUS_WEIGHTS = {
            { "DRAFT", 6 },
            { "SUBMITTED", 8 },
            { "RPS_REVIEW", 7 },
            { "RPS_RETURNED", 5 },
            { "OVCRIGE_COORDINATION", 6 },
            { "REC_EVALUATION", 6 },
            { "CHANCELLOR_REVIEW", 5 },
            { "FINANCE_AUTHORIZATION", 4 },
            { "IMPLEMENTATION", 9 },
            { "APPROVED", 6 },
            { "RELEASED", 4 },
            { "COMPLETED", 7 },
            { "REJECTED", 4 },
            { "REVISION", 5 },
            { "RETURNED_TO_RPS", 3 },
            { "UNDER_REVIEW", 4 },
    };

    /** Statuses whose proposals have been through review, so they carry a history. */
    private static final List<String> REVIEWED_STATUSES = List.of(
            "RPS_RETURNED", "OVCRIGE_COORDINATION", "REC_EVALUATION", "CHANCELLOR_REVIEW",
            "FINANCE_AUTHORIZATION", "IMPLEMENTATION", "APPROVED", "RELEASED", "COMPLETED",
            "REJECTED", "REVISION", "RETURNED_TO_RPS", "UNDER_REVIEW");

    /** Statuses that have an issued Special Order, so they can carry reports. */
    private static final List<String> REPORTABLE_STATUSES = List.of(
            "IMPLEMENTATION", "APPROVED", "RELEASED", "COMPLETED");

    private static final String[] COLLEGES = {
            "College of Science and Technology",
            "College of Education",
            "College of Business and Management",
            "College of Agriculture and Forestry",
            "College of Fisheries and Marine Sciences",
    };

    private static final String[] DEPARTMENTS = {
            "Department of Information Technology",
            "Department of Biological Sciences",
            "Department of Mathematics",
            "Department of Marine Biology",
            "Department of Agricultural Sciences",
            "Department of Education",
            "Department of Business Administration",
            "Department of Environmental Science",
    };

    private static final String[] PROGRAM_TITLES = {
            "MSUN RIIDE - Food Security and Nutrition",
            "MSUN RIIDE - Blue Economy and Marine Resources",
            "MSUN RIIDE - Climate Resilience and Disaster Risk Reduction",
            "MSUN RIIDE - Health and Wellness",
            "MSUN RIIDE - Sustainable Energy and Environment",
            "MSUN RIIDE - Digital Transformation and Smart Communities",
    };

    private static final String[] RESEARCH_TYPES = {
            "Basic Research", "Applied Research", "Developmental Research", "Action Research",
    };

    private static final String[] DURATIONS = { "12", "24", "36" };

    private static final String[] SDG_KEYS = {
            "SDG 2 - Zero Hunger",
            "SDG 3 - Good Health and Well-being",
            "SDG 4 - Quality Education",
            "SDG 6 - Clean Water and Sanitation",
            "SDG 8 - Decent Work and Economic Growth",
            "SDG 13 - Climate Action",
            "SDG 14 - Life Below Water",
            "SDG 15 - Life on Land",
    };

    private static final String[] FIRST_NAMES = {
            "Althea", "Bienvenido", "Carmela", "Danilo", "Elmer", "Fatima", "Gregorio", "Hazel",
            "Ignacio", "Jasmine", "Kristoffer", "Lorena", "Marlon", "Nerissa", "Oliver", "Perlita",
            "Quintin", "Rosario", "Salvador", "Teresita", "Ulysses", "Veronica", "Wilfredo", "Yolanda",
            "Zenaida", "Andres", "Bernadette", "Cesar", "Divina", "Emilio",
    };

    private static final String[] LAST_NAMES = {
            "Abellanosa", "Bacalso", "Cabrera", "Dagondon", "Eguia", "Fabian", "Galan", "Hinlo",
            "Ibanez", "Jamisola", "Kalimbo", "Lumactod", "Magallanes", "Nacua", "Omandam",
            "Padilla", "Quinones", "Rabago", "Salcedo", "Tabamo", "Ubalde", "Valdez", "Wenceslao",
            "Ybanez", "Zamora",
    };

    private static final String[] PROJECT_TOPICS = {
            "Water Quality Assessment of Naawan Coastal Waters",
            "Seaweed Farming Productivity in Misamis Oriental",
            "Mangrove Rehabilitation and Carbon Sequestration",
            "Digital Literacy Program for Coastal Communities",
            "Indigenous Crop Varieties Conservation",
            "Low-Cost Water Filtration for Rural Households",
            "Fisheries Catch Monitoring and Data Analytics",
            "Climate-Resilient Rice Farming Practices",
            "Community-Based Solid Waste Management",
            "Nutritional Profiling of School Children",
            "Renewable Energy Potential Assessment",
            "Marine Biodiversity Baseline Survey",
            "Organic Fertilizer from Agricultural Waste",
            "Telehealth Access for Remote Barangays",
            "Coastal Erosion Monitoring with Drones",
            "Cacao Value Chain Improvement",
            "Antimicrobial Properties of Local Seaweeds",
            "Learning Recovery Program for Coastal Schools",
            "Microplastics in Municipal Water Sources",
            "Coral Reef Restoration Techniques",
            "Bamboo-Based Sustainable Construction Materials",
            "Disaster Preparedness Information System",
            "Post-Harvest Loss Reduction for Smallholders",
            "Traditional Medicine Documentation and Validation",
    };

    private static final String[] BARANGAYS = {
            "Poblacion", "San Miguel", "Linangkayan", "Mapulog", "Mat-I", "Patag", "Tuburan",
    };

    private static final String[] MUNICIPALITIES = { "Naawan", "Initao", "Manticao", "Libertad" };

    private static final String[] AGENCY_NAMES = {
            "Department of Science and Technology - Region X",
            "Department of Agriculture - Region X",
            "Bureau of Fisheries and Aquatic Resources",
            "Department of Environment and Natural Resources",
            "Local Government Unit of Naawan",
            "MSU Naawan Research and Publication Services",
            "Department of Health - Region X",
            "Department of Education - Division of Misamis Oriental",
            "National Economic and Development Authority - Region X",
            "Philippine Council for Agriculture, Aquatic and Natural Resources Research",
    };

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;
    private final ProposalRepository proposalRepository;
    private final ProposalAnnouncementRepository announcementRepository;
    private final ProposalVersionRepository versionRepository;
    private final ProposalReviewRepository reviewRepository;
    private final EndorsementRequestRepository endorsementRepository;
    private final EndorsementRequestEventRepository endorsementEventRepository;
    private final NotificationRepository notificationRepository;
    private final QuarterlyProgressReportRepository quarterlyReportRepository;
    private final PriorityAgendaSetRepository agendaSetRepository;
    private final PriorityAgendaRepository agendaRepository;
    private final CooperatingAgencyRepository agencyRepository;
    private final CertificationRepository certificationRepository;
    private final PasswordService passwordService;

    /**
     * The BCrypt hash of {@link #PASSWORD}, computed once and shared by every synthetic
     * account.
     *
     * <p>Every synthetic account has the same password, so hashing it once keeps the
     * seed at a few seconds instead of the ~40 s that 518 separate BCrypt operations
     * would cost. It does mean the 518 rows carry the same hash value, which is
     * deliberate and limited to this test-only data set: the point of the load test is
     * to exercise the login path, not to give each fake account a distinct salt.
     * Real accounts always go through {@code PasswordService.hash} individually.
     */
    private volatile String sharedPasswordHash;

    @Value("${rpis.seed.synthetic.enabled:false}")
    private boolean enabled;

    private Random random;

    public SyntheticTestDataSeeder(
            DataSource dataSource,
            JdbcTemplate jdbc,
            UserRepository userRepository,
            ProposalRepository proposalRepository,
            ProposalAnnouncementRepository announcementRepository,
            ProposalVersionRepository versionRepository,
            ProposalReviewRepository reviewRepository,
            EndorsementRequestRepository endorsementRepository,
            EndorsementRequestEventRepository endorsementEventRepository,
            NotificationRepository notificationRepository,
            QuarterlyProgressReportRepository quarterlyReportRepository,
            PriorityAgendaSetRepository agendaSetRepository,
            PriorityAgendaRepository agendaRepository,
            CooperatingAgencyRepository agencyRepository,
            CertificationRepository certificationRepository,
            PasswordService passwordService) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
        this.userRepository = userRepository;
        this.proposalRepository = proposalRepository;
        this.announcementRepository = announcementRepository;
        this.versionRepository = versionRepository;
        this.reviewRepository = reviewRepository;
        this.endorsementRepository = endorsementRepository;
        this.endorsementEventRepository = endorsementEventRepository;
        this.notificationRepository = notificationRepository;
        this.quarterlyReportRepository = quarterlyReportRepository;
        this.agendaSetRepository = agendaSetRepository;
        this.agendaRepository = agendaRepository;
        this.agencyRepository = agencyRepository;
        this.certificationRepository = certificationRepository;
        this.passwordService = passwordService;
    }

    // ========================
    // ORCHESTRATION
    // ========================

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Synthetic data generation is disabled (rpis.seed.synthetic.enabled=false).");
            return;
        }

        // FIRST statement, before anything is read or written. If this passes, the
        // connection is known to point at the isolated test database.
        String url = DatabaseSafetyGuard.requireTestDatabase(dataSource,
                "generate synthetic load-test data");
        log.info("Synthetic data generation started against {} (profile: loadtest).", url);

        // The guard above decides WHICH DATABASE this may write to; the lock below decides
        // WHICH INSTANCE does it. Without it, two instances starting together against an
        // empty database both see "no batch marker" and both generate.
        Optional<PostgresAdvisoryLock> acquired;
        try {
            acquired = PostgresAdvisoryLock.tryAcquire(dataSource,
                    PostgresAdvisoryLock.PURPOSE_SYNTHETIC_SEED, LOCK_PURPOSE);
        } catch (IllegalStateException e) {
            // The lock could not even be requested. Generating without proof of exclusivity
            // is exactly what the lock exists to prevent, so this instance writes nothing.
            log.warn("Could not request the cluster lock for synthetic data generation ({}). "
                    + "Nothing was written by this instance.", e.getMessage());
            return;
        }

        if (acquired.isEmpty()) {
            // Deliberately does NOT conclude that the data set is complete. Another instance
            // holds the lock, which means it may still be generating, or it may have failed
            // part way through. Report what is actually in the table and leave the decision
            // to the operator rather than assuming.
            log.info("Another instance holds the cluster lock for synthetic data generation, so "
                    + "this instance will not generate. This does NOT mean the data set is "
                    + "complete: the other instance may still be working, or may have failed. "
                    + "Synthetic proposals visible now: {}. Verify the data set before running a "
                    + "load test.", countSyntheticProposals());
            return;
        }

        try (PostgresAdvisoryLock held = acquired.get()) {
            this.random = new Random(RANDOM_SEED);

            ensureRegistryTable();
            if (alreadySeeded()) {
                log.info("Synthetic batch '{}' is already present; nothing was written. "
                        + "Delete the '{}' row to rebuild the data set.", BATCH_KEY, REGISTRY_TABLE);
                printCounts();
                return;
            }

            seedPriorityAgendaSet();
            seedCooperatingAgencies();
            seedAnnouncements();

            List<User> proponents = seedUsers();
            List<User> deans = userRepository.findAll().stream()
                    .filter(u -> UserRoles.DEAN.equalsIgnoreCase(u.getRole()))
                    .toList();
            List<ProposalAnnouncement> announcements = announcementRepository.findAll();
            List<PriorityAgenda> agendas = agendaRepository.findAll();

            List<Proposal> proposals = seedProposals(proponents, announcements, agendas);
            seedVersionsAndReviews(proposals, deans);
            seedEndorsements(proponents, deans, announcements);
            seedNotifications(proponents, proposals, announcements);
            seedQuarterlyReports(proposals);

            spreadTimestamps();
            advanceSequences();
            recordBatch();
            printCounts();

            log.info("Synthetic data generation finished.");
        }
    }

    /**
     * How many synthetic proposals are in the table right now.
     *
     * <p>Used only to report state when another instance holds the generation lock. The
     * table may not exist yet, so a failure to read it reports {@code unknown} rather
     * than propagating: this is diagnostics, not a control path.
     */
    private String countSyntheticProposals() {
        try {
            Integer rows = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM proposals WHERE project_title LIKE ?",
                    Integer.class, MARKER + "%");
            return rows == null ? "unknown" : String.valueOf(rows);
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

    // ========================
    // REGISTRY (idempotency)
    // ========================

    /**
     * Creates the registry table if it is missing. The table is owned by this
     * generator: Hibernate never creates it, so it does not exist in production.
     */
    private void ensureRegistryTable() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + REGISTRY_TABLE + " ("
                + "batch_key VARCHAR(120) PRIMARY KEY, "
                + "generated_at TIMESTAMP NOT NULL, "
                + "summary TEXT)");
    }

    /**
     * True only when the batch marker exists <em>and</em> the rows it stands for are
     * still there.
     *
     * <p>The marker alone is not enough. The registry table is owned by this generator
     * rather than by Hibernate, so it survives a schema rebuild: the {@code test}
     * profile uses {@code ddl-auto=create-drop}, which drops every JPA table when a
     * test run ends but leaves this one. Checking only the marker would then make the
     * generator skip on a database with no data in it at all, leaving the environment
     * silently empty. When the rows are gone the marker is cleared and the data set is
     * rebuilt.
     */
    private boolean alreadySeeded() {
        Integer marker = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + REGISTRY_TABLE + " WHERE batch_key = ?",
                Integer.class, BATCH_KEY);
        if (marker == null || marker == 0) {
            return false;
        }

        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM proposals WHERE project_title LIKE ?",
                Integer.class, MARKER + "%");
        if (rows == null || rows == 0) {
            log.warn("Batch marker '{}' is present but no synthetic rows are (the schema "
                    + "was recreated). Clearing the marker and regenerating the data set.",
                    BATCH_KEY);
            jdbc.update("DELETE FROM " + REGISTRY_TABLE + " WHERE batch_key = ?", BATCH_KEY);
            return false;
        }

        return true;
    }

    private void recordBatch() {
        jdbc.update("INSERT INTO " + REGISTRY_TABLE + " (batch_key, generated_at, summary) VALUES (?, ?, ?)",
                BATCH_KEY, java.sql.Timestamp.valueOf(LocalDateTime.now()),
                "Synthetic load-test data set generated by SyntheticTestDataSeeder. "
                        + "Accounts use the " + EMAIL_DOMAIN + " domain; titles are prefixed with "
                        + MARKER + ".");
    }


    // ========================
    // REFERENCE DATA
    // ========================

    /**
     * Takes the shared reference-data lock, waiting briefly if another component or instance is
     * part way through creating the same framework.
     *
     * <p>The wait is deliberate rather than a plain skip. The seeder genuinely needs this row: if
     * it gave up it would go on to create 1200 proposals with no priority agenda attached, which
     * is a quietly incomplete data set. The wait is bounded so that a holder which is genuinely
     * stuck cannot hang the start-up.
     *
     * @return the held lock, or {@code null} if it could not be taken in time
     */
    private PostgresAdvisoryLock awaitReferenceLock(int timeoutSeconds) {
        long deadline = System.nanoTime() + (timeoutSeconds * 1_000_000_000L);
        while (System.nanoTime() < deadline) {
            try {
                Optional<PostgresAdvisoryLock> lock = PostgresAdvisoryLock.tryAcquire(dataSource,
                        PostgresAdvisoryLock.PURPOSE_REFERENCE_DATA, LOCK_PURPOSE_REFERENCE);
                if (lock.isPresent()) {
                    return lock.get();
                }
            } catch (IllegalStateException e) {
                log.warn("Could not request the reference-data cluster lock ({}).", e.getMessage());
                return null;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        log.warn("Timed out waiting for the reference-data cluster lock; the synthetic data set "
                + "will use whatever priority agenda framework is already present.");
        return null;
    }

    private void seedPriorityAgendaSet() {
        // PriorityAgendaInitializer creates this same framework on every instance, on the same
        // start-up, so this method is not the only writer of that row. The two share the
        // reference-data cluster lock: without it both can decide the row is absent and both
        // insert it, and the second one aborts its instance's start-up on the unique index -
        // which is exactly what happened the first time two instances were started together.
        try (PostgresAdvisoryLock referenceLock = awaitReferenceLock(15)) {
            if (agendaSetRepository.count() > 0) {
                // Already created - by a previous run, by the initializer, or by the other
                // instance. Reuse it rather than assuming this method is the only writer.
                return;
            }
            seedPriorityAgendaSetWithinLock();
        }
    }

    /** Creates the framework and its agendas. Only ever called while holding the lock. */
    private void seedPriorityAgendaSetWithinLock() {
        PriorityAgendaSet set = new PriorityAgendaSet();
        set.setFrameworkName("MSUN RIIDE");
        set.setCoverageStartYear(2025);
        set.setCoverageEndYear(2028);
        set.setActive(true);
        set = agendaSetRepository.save(set);

        String[][] entries = {
                { "food-security", "Food Security and Nutrition", "Availability, access and utilization" },
                { "blue-economy", "Blue Economy and Marine Resources", "Sustainable use of aquatic resources" },
                { "climate-resilience", "Climate Resilience and Disaster Risk Reduction",
                        "Adaptation and mitigation" },
                { "health-wellness", "Health and Wellness", "Public health and wellbeing" },
                { "energy-environment", "Sustainable Energy and Environment", "Clean energy and conservation" },
                { "digital-transformation", "Digital Transformation and Smart Communities",
                        "ICT and innovation" },
        };
        int order = 0;
        List<PriorityAgenda> agendas = new ArrayList<>();
        for (String[] entry : entries) {
            PriorityAgenda agenda = new PriorityAgenda();
            agenda.setAgendaSet(set);
            agenda.setAgendaKey(entry[0]);
            agenda.setName(entry[1]);
            agenda.setSubLabel(entry[2]);
            agenda.setDescription(entry[1] + " under the MSUN RIIDE framework.");
            agenda.setDisplayOrder(order++);
            agenda.setActive(true);
            agendas.add(agenda);
        }
        agendaRepository.saveAll(agendas);
    }

    private void seedCooperatingAgencies() {
        if (agencyRepository.count() > 0) {
            return;
        }
        List<CooperatingAgency> agencies = new ArrayList<>();
        for (String name : AGENCY_NAMES) {
            CooperatingAgency agency = new CooperatingAgency();
            agency.setName(name);
            agencies.add(agency);
        }
        agencyRepository.saveAll(agencies);
    }

    /**
     * Calls for Proposals spanning several years, so the "Call for Proposals" and
     * announcement-history screens have data with genuinely different dates. Exactly
     * one call is open right now, which is what the submission gatekeeper requires.
     */
    private void seedAnnouncements() {
        if (announcementRepository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        List<ProposalAnnouncement> announcements = new ArrayList<>();

        // One open call: the current submission window.
        announcements.add(announcement(today.minusDays(30), today.plusDays(60), "ACTIVE",
                "CY " + today.getYear() + " First Semester Research Call"));

        // One scheduled call: published, opens later.
        announcements.add(announcement(today.plusDays(75), today.plusDays(165), "ACTIVE",
                "CY " + today.getYear() + " Second Semester Research Call"));

        // Closed calls from previous cycles, spread over several years.
        for (int i = 1; i <= ANNOUNCEMENT_COUNT - 3; i++) {
            LocalDate start = today.minusDays(120L * i);
            LocalDate end = start.plusDays(90);
            String status = end.isBefore(today) ? "CLOSED" : "ACTIVE";
            announcements.add(announcement(start, end, status,
                    "CY " + start.getYear() + " Research Call (cycle " + i + ")"));
        }

        // One draft, so the DRAFT state is exercised as well.
        announcements.add(announcement(today.plusDays(10), today.plusDays(120), "DRAFT",
                "Draft Call - pending publication"));

        announcementRepository.saveAll(announcements);
    }

    private ProposalAnnouncement announcement(LocalDate start, LocalDate end, String status, String name) {
        ProposalAnnouncement announcement = new ProposalAnnouncement();
        announcement.setName(name);
        announcement.setStartDate(start);
        announcement.setEndDate(end);
        announcement.setStatus(status);
        announcement.setAdditionalAnnouncement(
                "Synthetic announcement generated for load testing. Submission window from "
                        + start + " to " + end + ".");
        return announcement;
    }


    // ========================
    // USERS
    // ========================

    /**
     * Creates the synthetic accounts: {@value #PROPONENT_COUNT} proponents (the 500
     * concurrent users), {@value #DEAN_COUNT} Deans and one account for each of the
     * reviewing offices, so every role's screens can be loaded.
     *
     * @return the proponents, in creation order
     */
    private List<User> seedUsers() {
        List<User> proponents = new ArrayList<>(PROPONENT_COUNT);
        List<User> others = new ArrayList<>();

        for (int i = 1; i <= PROPONENT_COUNT; i++) {
            User user = new User();
            user.setName(MARKER + " " + personName(i));
            user.setEmail(EMAIL_LOCAL_PREFIX + "proponent." + pad(i, 4) + EMAIL_DOMAIN);
            user.setPassword(passwordHash());
            user.setRole("PROPONENT");
            user.setStatus("APPROVED");
            user.setEmailVerified(true);
            user.setDepartmentOffice(DEPARTMENTS[i % DEPARTMENTS.length]);
            user.setContactNumber("09" + (170000000L + i * 37L));
            user.setSpecialization("Applied Research, Environmental Science");
            user.setDateRegistered(LocalDate.now().minusDays(60 + (i % 900)));
            proponents.add(user);
        }

        for (int i = 1; i <= DEAN_COUNT; i++) {
            String college = COLLEGES[(i - 1) % COLLEGES.length];
            User dean = new User();
            dean.setName(MARKER + " Dean " + personName(900 + i));
            dean.setEmail(EMAIL_LOCAL_PREFIX + "dean." + pad(i, 2) + EMAIL_DOMAIN);
            dean.setPassword(passwordHash());
            dean.setRole(UserRoles.DEAN);
            dean.setStatus("APPROVED");
            dean.setEmailVerified(true);
            dean.setDesignation("Dean");
            dean.setAssignedCollege(college);
            dean.setDepartmentOffice(college);
            dean.setDateRegistered(LocalDate.now().minusDays(500 + i));
            others.add(dean);
        }

        others.add(officeAccount("RPS Administrator", "rps.admin", "RPS_ADMIN", 1));
        others.add(officeAccount("RPS Staff", "rps.staff", "RPS_STAFF", 1));
        others.add(officeAccount("OVCRIGE Officer", "ovcrige.officer", "OVCRIGE", 1));
        others.add(officeAccount("OVCAF Officer", "ovcaf.officer", "OVCAF", 1));
        others.add(officeAccount("REC Evaluator", "rec.evaluator", "REC", 1));
        others.add(officeAccount("Chancellor Office", "oc.chancellor", "OC", 1));

        userRepository.saveAll(proponents);
        userRepository.saveAll(others);
        return proponents;
    }

    /** The BCrypt hash every synthetic account stores, computed on first use. */
    private String passwordHash() {
        String cached = sharedPasswordHash;
        if (cached == null) {
            synchronized (this) {
                if (sharedPasswordHash == null) {
                    sharedPasswordHash = passwordService.hash(PASSWORD);
                }
                cached = sharedPasswordHash;
            }
        }
        return cached;
    }

    private User officeAccount(String displayName, String localPart, String role, int index) {
        User user = new User();
        user.setName(MARKER + " " + displayName);
        user.setEmail(EMAIL_LOCAL_PREFIX + localPart + EMAIL_DOMAIN);
        user.setPassword(passwordHash());
        user.setRole(role);
        user.setStatus("APPROVED");
        user.setEmailVerified(true);
        user.setDepartmentOffice("Research and Publication Services");
        user.setDateRegistered(LocalDate.now().minusDays(700 + index));
        return user;
    }

    /** A stable, realistic Filipino name for the given index. */
    private String personName(int index) {
        String first = FIRST_NAMES[Math.floorMod(index * 7, FIRST_NAMES.length)];
        String last = LAST_NAMES[Math.floorMod(index * 11, LAST_NAMES.length)];
        return first + " " + last;
    }

    private static String pad(int value, int width) {
        return String.format("%0" + width + "d", value);
    }


    // ========================
    // PROPOSALS
    // ========================

    /**
     * Creates {@value #PROPOSAL_COUNT} proposals with their repeated child sections.
     *
     * <p>Proponents are assigned round-robin, so every one of the 500 synthetic
     * accounts owns two or three proposals - which is what makes the per-user views
     * (dashboard counters, "My Projects", the monitoring timeline) return realistic
     * result sets instead of one row or none.
     */
    private List<Proposal> seedProposals(List<User> proponents,
            List<ProposalAnnouncement> announcements,
            List<PriorityAgenda> agendas) {
        List<String> statusPool = buildStatusPool();
        List<Proposal> proposals = new ArrayList<>(PROPOSAL_COUNT);
        int year = LocalDate.now().getYear();

        for (int i = 1; i <= PROPOSAL_COUNT; i++) {
            String status = statusPool.get((i - 1) % statusPool.size());
            User proponent = proponents.get((i - 1) % proponents.size());
            ProposalAnnouncement announcement = announcements.get((i - 1) % announcements.size());

            // Five-digit codes, so a synthetic code can never collide with the
            // three-digit codes the application allocates from proposal_code_seq.
            String code = String.format("%05d", 5000 + i);
            String topic = PROJECT_TOPICS[(i - 1) % PROJECT_TOPICS.length];
            int durationMonths = Integer.parseInt(DURATIONS[(i - 1) % DURATIONS.length]);
            int collegeIndex = (i - 1) % COLLEGES.length;
            LocalDate start = LocalDate.of(year - ((i % 3) + 1), ((i % 12) + 1), 1 + (i % 20));

            Proposal p = new Proposal();
            p.setProposalCode(code);
            p.setRevisionNumber(revisionNumberFor(status));
            p.setDocumentId(String.format("MSUN-ORPS-DA-%s-%d-REV%02d", code, start.getYear(),
                    p.getRevisionNumber()));
            p.setProponent(proponent);
            p.setProposalAnnouncementId(announcement.getId());
            p.setProgramTitle(PROGRAM_TITLES[collegeIndex]);
            p.setProjectTitle(MARKER + " " + topic + " (batch " + (i % 40) + ")");
            p.setProjectLeader(proponent.getName());
            p.setProjectLeaderSex((i % 2 == 0) ? "Female" : "Male");
            p.setDuration(String.valueOf(durationMonths));
            p.setStartDate(start);
            p.setEndDate(start.plusMonths(durationMonths));
            p.setCollege(COLLEGES[collegeIndex]);
            p.setAddress(BARANGAYS[i % BARANGAYS.length] + ", "
                    + MUNICIPALITIES[i % MUNICIPALITIES.length]
                    + ", Misamis Oriental, Philippines");
            p.setAddressCountry("Philippines");
            p.setAddressRegion("Region X - Northern Mindanao");
            p.setAddressProvince("Misamis Oriental");
            p.setAddressDistrict("District 2");
            p.setAddressMunicipality(MUNICIPALITIES[i % MUNICIPALITIES.length]);
            p.setAddressBarangay(BARANGAYS[i % BARANGAYS.length]);
            p.setContactType("EMAIL_ADDRESS");
            p.setContactValue(proponent.getEmail());
            p.setCooperatingAgencies(AGENCY_NAMES[i % AGENCY_NAMES.length]);
            p.setResearchType(RESEARCH_TYPES[i % RESEARCH_TYPES.length]);
            p.setInnovationGoals("Improve local practice in " + topic.toLowerCase() + ".");
            p.setSectorRelevance("Environment, Agriculture, Public Health");
            p.setSdg(List.of(SDG_KEYS[i % SDG_KEYS.length], SDG_KEYS[(i + 3) % SDG_KEYS.length]));
            p.setExecutiveSummary("Synthetic executive summary for " + topic
                    + ". Generated for load testing, not a real proposal.");
            p.setRationale("Synthetic rationale describing why " + topic.toLowerCase()
                    + " matters locally.");
            p.setFramework("Community-based participatory research framework.");
            p.setObjectivesGeneral("Assess and improve " + topic.toLowerCase() + ".");
            p.setObjectivesSpecific("1. Establish a baseline; 2. Pilot an intervention; 3. Publish.");
            p.setReview("Synthetic review of related literature.");
            p.setMethodology(
                    "Mixed-methods design: household survey, water/soil sampling, key informant interviews.");
            p.setTechnologyTrl("TRL " + (3 + (i % 4)));
            p.setOutputs("Baseline dataset, prototype, training modules, refereed publication.");
            p.setOutcomes("Adoption of improved practice by at least 60% of participating households.");
            p.setImpactEconomic("Higher household income from improved yield.");
            p.setImpactSocial("Stronger community participation in resource management.");
            p.setBeneficiaries("Coastal households, fisherfolk associations and local government units.");
            p.setSustainability("Turned over to the LGU with a trained local counterpart team.");
            p.setGadScore(BigDecimal.valueOf(2 + (i % 4)).divide(BigDecimal.valueOf(2)));
            p.setRisks("Weather disruption, delayed permits, limited laboratory access.");
            p.setLimitationsText("Limited to four barangays and a 24-month observation window.");
            p.setReferencesText("Synthetic reference list for load testing.");
            p.setOtherProjectsNumber(String.valueOf(i % 5));
            p.setStatus(status);


            applyWorkflowState(p, status, i, start);

            p.setSites(List.of(site(p, i)));
            p.setPersonnel(List.of(personnel(p, i), personnel(p, i + 1)));
            p.setBudget(List.of(budget(p, i)));
            p.setLogFrames(List.of(logFrame(p, i), logFrame(p, i + 5)));
            p.setOtherProjects(List.of(otherProject(p, i)));
            p.setLimitations(List.of(limitation(p, i)));
            p.setPriorityAgendas(List.of(priorityAgenda(p, agendas, i),
                    priorityAgenda(p, agendas, i + 1)));

            proposals.add(p);
        }

        proposalRepository.saveAll(proposals);
        return proposals;
    }

    /** Expands the weighted status table into a pool that is indexed by proposal number. */
    private List<String> buildStatusPool() {
        List<String> pool = new ArrayList<>();
        for (Object[] entry : STATUS_WEIGHTS) {
            for (int i = 0; i < (Integer) entry[1]; i++) {
                pool.add((String) entry[0]);
            }
        }
        return pool;
    }

    private int revisionNumberFor(String status) {
        return switch (status) {
            case "RPS_RETURNED", "REVISION", "RETURNED_TO_RPS" -> 1 + random.nextInt(2);
            case "IMPLEMENTATION", "APPROVED", "RELEASED", "COMPLETED" -> random.nextInt(3);
            default -> 0;
        };
    }

    /**
     * Fills in the workflow fields a status implies, so each record is internally
     * consistent: an approved project has a Special Order, a returned proposal has a
     * deadline and a returning office, a draft has neither.
     */
    private void applyWorkflowState(Proposal p, String status, int index, LocalDate start) {
        switch (status) {
            case "DRAFT" -> {
                // Drafts carry no review state at all.
            }
            case "RPS_REVIEW", "OVCRIGE_COORDINATION", "REC_EVALUATION", "UNDER_REVIEW" -> {
                p.setReviewedBy(MARKER + " RPS Reviewer");
                p.setReviewedByPosition(index % 2 == 0 ? "RPS Director" : "RPS Staff");
                p.setRemarks("Synthetic compliance review in progress.");
            }
            case "RPS_RETURNED", "REVISION", "RETURNED_TO_RPS" -> {
                p.setReviewedBy(MARKER + " RPS Reviewer");
                p.setReviewedByPosition(index % 2 == 0 ? "RPS Director" : "RPS Staff");
                p.setRemarks("Synthetic return-for-revision remarks.");
                p.setReturnedByOffice(index % 2 == 0 ? "RPS" : "REC");
                p.setReturnedByName(MARKER + " Reviewing Officer");
                p.setReturnRemarks("Please revise the workplan and the line-item budget.");
                p.setRevisionDeadline(LocalDateTime.now().plusDays(7 + (index % 21)));
                p.setRevisionForwardedByName(MARKER + " RPS Director");
                p.setRevisionNotes("Forwarded to the proponent with the consolidated comments.");
            }
            case "CHANCELLOR_REVIEW" -> {
                p.setReviewedBy(MARKER + " REC Chair");
                p.setReviewedByPosition("REC");
                p.setRemarks("Synthetic evaluation passed; awaiting final approval.");
                p.setNeedsBudget(index % 3 != 0);
            }
            case "FINANCE_AUTHORIZATION" -> {
                p.setReviewedBy(MARKER + " Chancellor");
                p.setReviewedByPosition("Office of the Chancellor");
                p.setRemarks("Synthetic final approval granted.");
                p.setNeedsBudget(true);
            }
            case "REJECTED" -> {
                p.setReviewedBy(MARKER + " REC Chair");
                p.setReviewedByPosition("REC");
                p.setRemarks("Synthetic rejection: the objectives were not measurable.");
            }
            default -> {
                // APPROVED / RELEASED / IMPLEMENTATION / COMPLETED: an issued SO.
                p.setReviewedBy(MARKER + " Chancellor");
                p.setReviewedByPosition("Office of the Chancellor");
                p.setRemarks("Synthetic final approval granted.");
                p.setNeedsBudget(index % 4 != 0);
                p.setSoNumber("SO-" + start.getYear() + "-" + pad(100 + index, 4));
                p.setSoUploadedAt(LocalDateTime.now().minusDays(30L + index));
            }
        }
    }

    // ========================
    // PROPOSAL CHILD SECTIONS
    // ========================

    private ProposalSite site(Proposal p, int i) {
        ProposalSite site = new ProposalSite();
        site.setProposal(p);
        site.setCountry("Philippines");
        site.setRegion("Region X - Northern Mindanao");
        site.setProvince("Misamis Oriental");
        site.setDistrict("District 2");
        site.setMunicipality(MUNICIPALITIES[i % MUNICIPALITIES.length]);
        site.setBarangay(BARANGAYS[i % BARANGAYS.length]);
        return site;
    }

    private ProposalPersonnel personnel(Proposal p, int i) {
        ProposalPersonnel member = new ProposalPersonnel();
        member.setProposal(p);
        member.setPosition(i % 2 == 0 ? "Project Leader" : "Research Assistant");
        member.setTimeDevoted(i % 2 == 0 ? "100%" : "50%");
        member.setResponsibilities(i % 2 == 0
                ? "Overall coordination, reporting and publication."
                : "Data collection, encoding and laboratory analysis.");
        return member;
    }

    private ProposalBudget budget(Proposal p, int i) {
        BigDecimal ps = BigDecimal.valueOf(120000 + (i % 10) * 15000L);
        BigDecimal mooe = BigDecimal.valueOf(90000 + (i % 7) * 12000L);
        BigDecimal eo = BigDecimal.valueOf(30000 + (i % 5) * 8000L);
        ProposalBudget line = new ProposalBudget();
        line.setProposal(p);
        line.setAgency("MSU Naawan - Internal Fund");
        line.setPs(ps.toPlainString());
        line.setMooe(mooe.toPlainString());
        line.setEo(eo.toPlainString());
        line.setTotal(ps.add(mooe).add(eo).toPlainString());
        return line;
    }

    private ProposalLogFrame logFrame(Proposal p, int i) {
        ProposalLogFrame frame = new ProposalLogFrame();
        frame.setProposal(p);
        frame.setOutcome("Improved local capacity in " + PROJECT_TOPICS[i % PROJECT_TOPICS.length]
                .toLowerCase() + ".");
        frame.setOutput("At least " + (3 + (i % 5)) + " deliverables produced and disseminated.");
        return frame;
    }

    private ProposalOtherProject otherProject(Proposal p, int i) {
        ProposalOtherProject other = new ProposalOtherProject();
        other.setProposal(p);
        other.setTitle("Synthetic prior project " + i + " on community resource assessment.");
        other.setAgency("Department of Science and Technology - Region X");
        other.setInvolvement(i % 2 == 0 ? "Project Leader" : "Co-Investigator");
        return other;
    }

    private ProposalLimitation limitation(Proposal p, int i) {
        ProposalLimitation limitation = new ProposalLimitation();
        limitation.setProposal(p);
        limitation.setLimitation("Coverage is limited to " + (3 + (i % 3)) + " barangays.");
        limitation.setRemarks("Extend coverage in a follow-up study.");
        return limitation;
    }

    private ProposalPriorityAgenda priorityAgenda(Proposal p, List<PriorityAgenda> agendas, int i) {
        PriorityAgenda agenda = agendas.get(i % agendas.size());
        ProposalPriorityAgenda selection = new ProposalPriorityAgenda();
        selection.setProposal(p);
        selection.setAgendaKey(agenda.getAgendaKey());
        selection.setSelected(true);
        selection.setValue("Addresses " + agenda.getName() + " through field-based assessment.");
        return selection;
    }


    // ========================
    // VERSION HISTORY & REVIEWS
    // ========================

    /**
     * Gives every reviewed proposal a submission/revision history and, where the
     * workflow has reached an evaluation body, an evaluation record. This is what the
     * "Submission History" and review panels read.
     */
    private void seedVersionsAndReviews(List<Proposal> proposals, List<User> deans) {
        List<ProposalVersion> versions = new ArrayList<>();
        List<ProposalReview> reviews = new ArrayList<>();
        List<User> reviewers = userRepository.findAll().stream()
                .filter(u -> List.of("RPS_ADMIN", "RPS_STAFF", "OVCRIGE", "REC", "OC", "OVCAF")
                        .contains(u.getRole()))
                .toList();

        for (Proposal p : proposals) {
            if ("DRAFT".equals(p.getStatus())) {
                continue; // A draft has never been submitted.
            }

            int latest = Math.max(1, p.getRevisionNumber() + 1);
            for (int v = 1; v <= latest; v++) {
                ProposalVersion version = new ProposalVersion();
                version.setProposal(p);
                version.setVersionNumber(v);
                version.setDocumentId(p.getDocumentId());
                version.setStatus(v == latest ? p.getStatus() : "SUBMITTED");
                version.setSubmittedBy(p.getProponent());
                version.setSubmittedByName(p.getProponent().getName());
                version.setIsCurrent(v == latest);
                versions.add(version);
            }

            if (!REVIEWED_STATUSES.contains(p.getStatus())) {
                continue;
            }

            // One or two evaluation records per reviewed proposal.
            int reviewCount = 1 + (p.getId() % 2 == 0 ? 1 : 0);
            for (int r = 0; r < reviewCount && !reviewers.isEmpty(); r++) {
                User reviewer = reviewers.get((int) ((p.getId() + r) % reviewers.size()));
                ProposalReview review = new ProposalReview();
                review.setProposal(p);
                review.setReviewer(reviewer);
                review.setReviewerRole(reviewer.getRole());
                review.setSoundnessSectorNeedScore(score(1));
                review.setSoundnessEffectiveSolutionScore(score(2));
                review.setSoundnessReasonableBudgetScore(score(3));
                review.setSoundnessDoableWorkplanScore(score(4));
                review.setSuitabilityCostEffectiveScore(score(5));
                review.setSuitabilityPartnersIdentifiedScore(score(6));
                review.setSuitabilityCommercializationScore(score(7));
                review.setSuitabilityTimelyUtilizationScore(score(8));
                review.setSignificanceEconomicImpactScore(score(9));
                review.setSignificanceSocialImpactScore(score(10));
                review.setSignificanceEnvironmentalImpactScore(score(11));
                review.setSignificanceSustainabilityScore(score(12));
                review.setCompetenceAlignmentScore(score(13));
                review.setCompetenceCollaborationScore(score(14));
                review.setCompetenceDeliverablesUnderstandingScore(score(15));
                review.setOverallComments("Synthetic evaluation comment for load testing.");
                review.setDecision(decisionFor(p.getStatus()));
                reviews.add(review);
            }
        }

        versionRepository.saveAll(versions);
        reviewRepository.saveAll(reviews);
    }

    /** A score between 3 and 5, the range the evaluation form uses. */
    private int score(int offset) {
        return 3 + random.nextInt(3);
    }

    private String decisionFor(String status) {
        return switch (status) {
            case "REJECTED" -> "REJECTED";
            case "RPS_RETURNED", "REVISION", "RETURNED_TO_RPS" -> "FOR_REVISION";
            case "UNDER_REVIEW", "REC_EVALUATION", "OVCRIGE_COORDINATION" -> "PENDING";
            default -> "APPROVED";
        };
    }




    // ========================
    // DEAN ENDORSEMENTS
    // ========================

    /**
     * Creates {@value #ENDORSEMENT_COUNT} endorsement requests with their audit
     * events and, for the approved ones, the issued certification. Every relationship
     * is a real foreign key: the request points at a Proponent, an assigned Dean, the
     * call it answers and (when it has been used) the proposal it backs.
     */
    private void seedEndorsements(List<User> proponents, List<User> deans,
            List<ProposalAnnouncement> announcements) {
        if (deans.isEmpty()) {
            log.warn("No Dean account exists; skipping the endorsement records.");
            return;
        }

        List<EndorsementRequest> requests = new ArrayList<>();
        List<EndorsementRequestEvent> events = new ArrayList<>();
        List<Certification> certifications = new ArrayList<>();
        int year = LocalDate.now().getYear();

        for (int i = 1; i <= ENDORSEMENT_COUNT; i++) {
            User proponent = proponents.get((i * 3) % proponents.size());
            User dean = deans.get(i % deans.size());
            ProposalAnnouncement announcement = announcements.get(i % announcements.size());

            // A third of the requests are still awaiting the Dean, a sixth were
            // rejected, the rest were approved (and are therefore usable).
            String status = (i % 3 == 0) ? EndorsementRequest.STATUS_PENDING
                    : (i % 6 == 0) ? EndorsementRequest.STATUS_REJECTED
                            : EndorsementRequest.STATUS_APPROVED;

            EndorsementRequest request = new EndorsementRequest();
            request.setReferenceNumber(String.format("MSUN-ORPS-END-%d-%04d", 9000 + i, year));
            request.setProponent(proponent);
            request.setAssignedDean(dean);
            request.setCollege(dean.getAssignedCollege() != null ? dean.getAssignedCollege()
                    : COLLEGES[i % COLLEGES.length]);
            request.setDepartment(DEPARTMENTS[i % DEPARTMENTS.length]);
            // Two independent titles, as on the Create Proposal form: the program title is
            // optional and the project title is required. The program title is left empty
            // for every third request so both shapes of record exist in the test data.
            request.setProgramTitle(i % 3 == 0 ? null : PROGRAM_TITLES[i % PROGRAM_TITLES.length]);
            request.setProjectTitle(MARKER + " " + PROJECT_TOPICS[i % PROJECT_TOPICS.length]
                    + " (endorsement " + i + ")");
            request.setProposalAnnouncementId(announcement.getId());
            request.setResearchSummary("Synthetic research summary submitted with the endorsement request.");
            request.setNotes("Synthetic note for load testing.");
            request.setStatus(status);

            if (!EndorsementRequest.STATUS_PENDING.equals(status)) {
                request.setDecidedByUserId(dean.getId());
                request.setDecidedByName(dean.getName());
                request.setDecidedByDesignation("Dean");
                request.setEndorsementDate(LocalDate.now().minusDays(20 + (i % 400)));
            }
            if (EndorsementRequest.STATUS_REJECTED.equals(status)) {
                request.setRejectionReason("Synthetic rejection: the scope was too broad.");
            }
            if (EndorsementRequest.STATUS_APPROVED.equals(status)) {
                request.setLetterDocumentCode(String.format("MSUN-ORPS-END-LTR-%04d", i));
                request.setLetterFileName("endorsement-" + pad(i, 4) + ".pdf");
                request.setLetterVersion(1);
            }
            requests.add(request);
        }

        List<EndorsementRequest> saved = endorsementRepository.saveAll(requests);
        buildEndorsementHistory(saved, events, certifications);

        endorsementEventRepository.saveAll(events);
        List<Certification> savedCertifications = certificationRepository.saveAll(certifications);

        // Back-reference each certification on its request, the way the workflow does.
        for (Certification certification : savedCertifications) {
            saved.stream()
                    .filter(r -> r.getId().equals(certification.getEndorsementRequestId()))
                    .findFirst()
                    .ifPresent(r -> r.setCertificationId(certification.getId()));
        }
        endorsementRepository.saveAll(saved);
    }

    private void buildEndorsementHistory(List<EndorsementRequest> saved,
            List<EndorsementRequestEvent> events, List<Certification> certifications) {
        for (EndorsementRequest request : saved) {
            events.add(endorsementEvent(request, EndorsementRequestEvent.TYPE_CREATED,
                    request.getProponent(), "Endorsement request submitted."));
            events.add(endorsementEvent(request, EndorsementRequestEvent.TYPE_ASSIGNED,
                    request.getAssignedDean(), "Routed to the Dean of the proponent's college."));

            if (EndorsementRequest.STATUS_APPROVED.equals(request.getStatus())) {
                events.add(endorsementEvent(request, EndorsementRequestEvent.TYPE_APPROVED,
                        request.getAssignedDean(), "Endorsement approved."));
                events.add(endorsementEvent(request, EndorsementRequestEvent.TYPE_LETTER_GENERATED,
                        request.getAssignedDean(), "Official endorsement letter generated."));
                events.add(endorsementEvent(request, EndorsementRequestEvent.TYPE_CERTIFICATION_CREATED,
                        request.getAssignedDean(), "Certification recorded for the proponent."));

                Certification certification = new Certification();
                certification.setProponent(request.getProponent());
                certification.setEndorsementRequestId(request.getId());
                certification.setTitle("Endorsement Letter - " + request.getReferenceNumber());
                certification.setFileName(request.getLetterFileName());
                certification.setFilePath("endorsements/" + request.getLetterFileName());
                certification.setEndorsementDate(request.getEndorsementDate());
                certification.setIssuingDeanName(request.getDecidedByName());
                certifications.add(certification);
            } else if (EndorsementRequest.STATUS_REJECTED.equals(request.getStatus())) {
                events.add(endorsementEvent(request, EndorsementRequestEvent.TYPE_REJECTED,
                        request.getAssignedDean(), "Endorsement rejected."));
            }
        }
    }

    private EndorsementRequestEvent endorsementEvent(EndorsementRequest request, String type,
            User actor, String detail) {
        EndorsementRequestEvent event = new EndorsementRequestEvent();
        event.setEndorsementRequestId(request.getId());
        event.setEventType(type);
        event.setActorId(actor != null ? actor.getId() : null);
        event.setActorName(actor != null ? actor.getName() : MARKER + " System");
        event.setActorRole(actor != null ? actor.getRole() : "SYSTEM");
        event.setDetail(detail);
        return event;
    }

    // ========================
    // NOTIFICATIONS
    // ========================

    /**
     * Creates {@value #NOTIFICATIONS_PER_PROPONENT} notifications per Proponent, each
     * pointing at a real proposal (or a real Call for Proposals) that the account is
     * actually related to, with a mix of read and unread so the unread badge has a
     * meaningful value.
     */
    private void seedNotifications(List<User> proponents, List<Proposal> proposals,
            List<ProposalAnnouncement> announcements) {
        List<Notification> notifications = new ArrayList<>();

        for (int u = 0; u < proponents.size(); u++) {
            User proponent = proponents.get(u);
            for (int n = 0; n < NOTIFICATIONS_PER_PROPONENT; n++) {
                Notification notification = new Notification();
                notification.setUser(proponent);
                notification.setTitle(MARKER + " " + notificationTitle(n));
                notification.setMessage("Synthetic notification generated for load testing.");
                notification.setType(notificationType(n));
                notification.setIsRead(n % 3 == 0);

                // Link to one of this proponent's own proposals where the notification
                // is about a proposal, and to a real call otherwise.
                if (n % 2 == 0) {
                    Proposal own = proposals.get((u * 2 + n) % proposals.size());
                    notification.setProposalId(own.getId());
                } else {
                    ProposalAnnouncement announcement = announcements.get((u + n) % announcements.size());
                    notification.setProposalAnnouncementId(announcement.getId());
                }
                notifications.add(notification);
            }
        }

        notificationRepository.saveAll(notifications);
    }

    private String notificationTitle(int n) {
        return switch (n % NOTIFICATIONS_PER_PROPONENT) {
            case 0 -> "Proposal submitted";
            case 1 -> "New Call for Proposals published";
            case 2 -> "Revision requested";
            case 3 -> "Evaluation completed";
            case 4 -> "Special Order issued";
            default -> "Quarterly report due";
        };
    }

    private String notificationType(int n) {
        return switch (n % NOTIFICATIONS_PER_PROPONENT) {
            case 0 -> "PROPOSAL";
            case 1 -> "ANNOUNCEMENT";
            case 2 -> "REVISION";
            case 3 -> "REVIEW";
            case 4 -> "APPROVAL";
            default -> "REPORT";
        };
    }


    // ========================
    // QUARTERLY PROGRESS REPORTS
    // ========================

    /**
     * Creates implementation-phase progress reports for a sample of the reportable
     * proposals. Only proposals that already have an issued Special Order are used,
     * because those are the only ones the application considers eligible.
     */
    private void seedQuarterlyReports(List<Proposal> proposals) {
        List<Proposal> eligible = proposals.stream()
                .filter(p -> p.getSoNumber() != null && !p.getSoNumber().isBlank())
                .filter(p -> REPORTABLE_STATUSES.contains(p.getStatus()))
                .toList();
        if (eligible.isEmpty()) {
            return;
        }

        String[] periods = { "Q1", "Q2", "Q3", "Q4" };
        List<QuarterlyProgressReport> reports = new ArrayList<>();
        int created = 0;

        for (int i = 0; created < QUARTERLY_REPORT_COUNT && i < eligible.size(); i++) {
            Proposal proposal = eligible.get(i);
            int year = LocalDate.now().getYear() - (i % 2);
            String period = periods[i % periods.length];

            QuarterlyProgressReport report = new QuarterlyProgressReport();
            report.setProposalId(proposal.getId());
            report.setPeriod(period);
            report.setYear(year);
            report.setStatus(i % 4 == 0 ? "DRAFT" : "SUBMITTED");
            report.setProjectTitle(proposal.getProjectTitle());
            report.setProjectLeader(proposal.getProjectLeader());
            report.setLeaderGender(proposal.getProjectLeaderSex());
            report.setBaseStation("MSU Naawan, Naawan, Misamis Oriental");
            report.setSitesOfImplementation(proposal.getAddressBarangay() + ", "
                    + proposal.getAddressMunicipality());
            report.setProjectDuration(proposal.getDuration());
            report.setProjectStartDate(proposal.getStartDate());
            report.setProjectEndDate(proposal.getEndDate());
            report.setCatchUpPlan("Synthetic catch-up plan for the reporting quarter.");
            report.setProblemsConcerns("Delayed procurement and intermittent field access.");
            report.setSuggestedSolutions("Advance procurement and reschedule field work.");
            report.setCreatedBy(proposal.getProponent() != null ? proposal.getProponent().getId() : null);
            if (!"DRAFT".equals(report.getStatus())) {
                report.setSubmittedBy(proposal.getProponent() != null
                        ? proposal.getProponent().getId()
                        : null);
            }
            reports.add(report);
            created++;
        }

        quarterlyReportRepository.saveAll(reports);
    }

    // ========================
    // DATE SPREADING
    // ========================

    /**
     * Spreads the timestamps of the synthetic rows over several years and months.
     *
     * <p>{@code @CreationTimestamp} / {@code @UpdateTimestamp} overwrite any value set
     * before the insert, so every row would otherwise carry the same "now" timestamp
     * and the date filters, the monitoring timeline and the deadline logic would all
     * see one single day of history. Each statement below is one pass over a table and
     * is restricted to synthetic rows only (by the marker or the e-mail prefix), so no
     * genuine record is ever touched.
     */
    private void spreadTimestamps() {
        // Roughly 3.5 years of history, plus a few minutes so no two rows share a
        // timestamp.
        jdbc.update("UPDATE proposals SET "
                + "  created_at = TIMESTAMP '2023-01-05 08:00:00' "
                + "             + ((id % 1270) * INTERVAL '1 day') "
                + "             + ((id % 53) * INTERVAL '41 minutes'), "
                + "  updated_at = TIMESTAMP '2023-01-05 08:00:00' "
                + "             + ((id % 1270) * INTERVAL '1 day') "
                + "             + ((id % 53) * INTERVAL '41 minutes') "
                + "             + INTERVAL '6 hours' "
                + "WHERE project_title LIKE ?", MARKER + "%");

        // Revisions and deadlines land after the proposal's own creation.
        jdbc.update("UPDATE proposals SET "
                + "  returned_at = created_at + INTERVAL '45 days', "
                + "  revision_deadline = created_at + INTERVAL '75 days', "
                + "  revision_forwarded_at = created_at + INTERVAL '50 days', "
                + "  so_uploaded_at = created_at + INTERVAL '120 days' "
                + "WHERE project_title LIKE ?", MARKER + "%");

        jdbc.update("UPDATE proposal_versions SET "
                + "  created_at = TIMESTAMP '2023-01-05 08:00:00' "
                + "             + ((id % 1270) * INTERVAL '1 day') "
                + "             + ((id % 47) * INTERVAL '23 minutes'), "
                + "  submission_date = TIMESTAMP '2023-01-05 08:00:00' "
                + "             + ((id % 1270) * INTERVAL '1 day') "
                + "             + ((id % 47) * INTERVAL '23 minutes'), "
                + "  revision_date = CASE WHEN version_number > 1 THEN "
                + "                    TIMESTAMP '2023-01-05 08:00:00' "
                + "                    + ((id % 1270) * INTERVAL '1 day') "
                + "                    + INTERVAL '40 days' ELSE NULL END "
                + "WHERE proposal_id IN (SELECT id FROM proposals WHERE project_title LIKE ?)",
                MARKER + "%");

        jdbc.update("UPDATE proposal_reviews SET created_at = "
                + "  TIMESTAMP '2023-02-01 09:00:00' + ((id % 1270) * INTERVAL '1 day') "
                + "  + ((id % 31) * INTERVAL '17 minutes') "
                + "WHERE proposal_id IN (SELECT id FROM proposals WHERE project_title LIKE ?)",
                MARKER + "%");

        jdbc.update("UPDATE notifications SET "
                + "  created_at = TIMESTAMP '2024-01-01 07:00:00' "
                + "             + ((id % 1000) * INTERVAL '1 day') "
                + "             + ((id % 37) * INTERVAL '29 minutes') "
                + "WHERE title LIKE ?", MARKER + "%");

        jdbc.update("UPDATE endorsement_requests SET "
                + "  created_at = TIMESTAMP '2023-03-01 08:30:00' "
                + "             + ((id % 1270) * INTERVAL '1 day') "
                + "             + ((id % 29) * INTERVAL '31 minutes'), "
                + "  updated_at = TIMESTAMP '2023-03-01 08:30:00' "
                + "             + ((id % 1270) * INTERVAL '1 day') "
                + "             + INTERVAL '3 days', "
                + "  decided_at = CASE WHEN status <> 'PENDING' THEN "
                + "                 TIMESTAMP '2023-03-01 08:30:00' "
                + "                 + ((id % 1270) * INTERVAL '1 day') + INTERVAL '5 days' "
                + "               ELSE NULL END "
                + "WHERE proposal_title LIKE ?", MARKER + "%");

        jdbc.update("UPDATE endorsement_request_events SET created_at = "
                + "  TIMESTAMP '2023-03-01 08:30:00' + ((id % 1270) * INTERVAL '1 day') "
                + "  + ((id % 23) * INTERVAL '13 minutes') "
                + "WHERE endorsement_request_id IN "
                + "  (SELECT id FROM endorsement_requests WHERE proposal_title LIKE ?)",
                MARKER + "%");

        jdbc.update("UPDATE users SET date_registered = "
                + "  (DATE '2023-01-01' + ((id % 1200) * INTERVAL '1 day')) "
                + "WHERE email LIKE ?", EMAIL_LOCAL_PREFIX + "%");

        jdbc.update("UPDATE quarterly_progress_reports SET "
                + "  created_at = TIMESTAMP '2024-01-15 09:00:00' "
                + "             + ((id % 900) * INTERVAL '1 day') "
                + "             + ((id % 41) * INTERVAL '19 minutes'), "
                + "  updated_at = TIMESTAMP '2024-01-15 09:00:00' "
                + "             + ((id % 900) * INTERVAL '1 day') + INTERVAL '2 days', "
                + "  submitted_at = CASE WHEN status = 'SUBMITTED' THEN "
                + "                   TIMESTAMP '2024-01-15 09:00:00' "
                + "                   + ((id % 900) * INTERVAL '1 day') + INTERVAL '1 day' "
                + "                 ELSE NULL END "
                + "WHERE proposal_id IN (SELECT id FROM proposals WHERE project_title LIKE ?)",
                MARKER + "%");
    }

    // ========================
    // SEQUENCES & SUMMARY
    // ========================

    /**
     * Moves the identifier sequences past the synthetic codes.
     *
     * <p>Synthetic proposals use five-digit codes and synthetic endorsements use the
     * {@code 9000+} block, so the application's own three-digit allocation could never
     * collide with them anyway. This makes the invariant explicit and keeps a future
     * change to either format from silently reintroducing a collision.
     */
    private void advanceSequences() {
        // The sequences exist in a real database; create them here so a freshly
        // created test database has the same starting point as production.
        jdbc.execute("CREATE SEQUENCE IF NOT EXISTS proposal_code_seq");
        jdbc.execute("CREATE SEQUENCE IF NOT EXISTS endorsement_code_seq");

        Long maxProposalCode = jdbc.queryForObject(
                "SELECT MAX(CAST(proposal_code AS INTEGER)) FROM proposals "
                        + "WHERE proposal_code ~ '^[0-9]+$'",
                Long.class);
        if (maxProposalCode != null) {
            jdbc.queryForObject("SELECT setval('proposal_code_seq', ?, true)", Long.class, maxProposalCode);
        }
    }

    /** Prints the row counts, so a run can be verified at a glance. */
    private void printCounts() {
        log.info("Synthetic data set - row counts (marked rows only where applicable):");
        count("users (synthetic)", "SELECT COUNT(*) FROM users WHERE email LIKE '"
                + EMAIL_LOCAL_PREFIX + "%'");
        count("proposals (synthetic)", "SELECT COUNT(*) FROM proposals WHERE project_title LIKE '"
                + MARKER + "%'");
        count("  distinct statuses", "SELECT COUNT(DISTINCT status) FROM proposals "
                + "WHERE project_title LIKE '" + MARKER + "%'");
        count("  distinct creation months", "SELECT COUNT(DISTINCT date_trunc('month', created_at)) "
                + "FROM proposals WHERE project_title LIKE '" + MARKER + "%'");
        count("  distinct creation years", "SELECT COUNT(DISTINCT date_part('year', created_at)) "
                + "FROM proposals WHERE project_title LIKE '" + MARKER + "%'");
        count("proposal_versions (synthetic)", "SELECT COUNT(*) FROM proposal_versions WHERE proposal_id IN "
                + "(SELECT id FROM proposals WHERE project_title LIKE '" + MARKER + "%')");
        count("proposal_reviews (synthetic)", "SELECT COUNT(*) FROM proposal_reviews WHERE proposal_id IN "
                + "(SELECT id FROM proposals WHERE project_title LIKE '" + MARKER + "%')");
        count("endorsement_requests (synthetic)", "SELECT COUNT(*) FROM endorsement_requests "
                + "WHERE proposal_title LIKE '" + MARKER + "%'");
        count("endorsement_request_events (synthetic)", "SELECT COUNT(*) FROM endorsement_request_events "
                + "WHERE endorsement_request_id IN (SELECT id FROM endorsement_requests "
                + "WHERE proposal_title LIKE '" + MARKER + "%')");
        count("certifications (synthetic)", "SELECT COUNT(*) FROM certifications WHERE endorsement_request_id IN "
                + "(SELECT id FROM endorsement_requests WHERE proposal_title LIKE '" + MARKER + "%')");
        count("notifications (synthetic)", "SELECT COUNT(*) FROM notifications WHERE title LIKE '"
                + MARKER + "%'");
        count("quarterly_progress_reports (synthetic)", "SELECT COUNT(*) FROM quarterly_progress_reports "
                + "WHERE proposal_id IN (SELECT id FROM proposals WHERE project_title LIKE '" + MARKER + "%')");
        count("proposal_announcements (all)", "SELECT COUNT(*) FROM proposal_announcements");
    }

    private void count(String label, String sql) {
        try {
            Integer value = jdbc.queryForObject(sql, Integer.class);
            log.info("  {}{}", String.format("%-42s", label), value);
        } catch (Exception e) {
            log.warn("  {}{}", String.format("%-42s", label), "n/a (" + e.getMessage() + ")");
        }
    }
}
