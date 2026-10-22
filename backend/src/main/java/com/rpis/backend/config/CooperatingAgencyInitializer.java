package com.rpis.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Prepares the shared cooperating agency directory (section 02 of the Create
 * Proposal form): it creates the case-insensitive unique index that makes duplicate
 * agencies impossible and seeds the default agencies.
 * <p>
 * Both steps are idempotent and run once after the application context is ready
 * (the tables already exist by then, exactly like
 * {@code ProposalCodeSequenceInitializer} relies on):
 * <ul>
 *   <li>the unique index on {@code LOWER(name)} is created with
 *       {@code IF NOT EXISTS}, so an agency can never be stored twice - not even
 *       when two proponents add the same name at the same moment or when two
 *       backend instances start together;</li>
 *   <li>the defaults are inserted with {@code ON CONFLICT DO NOTHING}, so a restart
 *       never creates duplicate seed records and never touches an existing row (an
 *       agency renamed or added by a user is preserved).</li>
 * </ul>
 */
@Slf4j
@Component
public class CooperatingAgencyInitializer {

    /**
     * The default cooperating agencies, ordered by their full name (the order the
     * checklist shows them in). Only the missing ones are inserted on startup.
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

    private static final String UNIQUE_INDEX = "uk_cooperating_agencies_name_lower";

    private final JdbcTemplate jdbcTemplate;

    /**
     * Written out explicitly instead of relying on {@code @RequiredArgsConstructor}
     * to stay consistent with the other startup initializer in this package.
     */
    public CooperatingAgencyInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDirectory() {
        try {
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS " + UNIQUE_INDEX
                    + " ON cooperating_agencies (LOWER(name))");

            for (String name : DEFAULT_AGENCIES) {
                jdbcTemplate.update(
                        "INSERT INTO cooperating_agencies (name) VALUES (?) ON CONFLICT DO NOTHING", name);
            }

            Integer total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM cooperating_agencies", Integer.class);
            log.info("cooperating_agencies directory is ready ({} agencies)", total);
        } catch (Exception e) {
            log.warn("Failed to initialize the cooperating agencies directory: {}", e.getMessage());
        }
    }
}
