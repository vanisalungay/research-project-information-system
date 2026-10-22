package com.rpis.backend.service;

import com.rpis.backend.config.CooperatingAgencyInitializer;
import com.rpis.backend.model.CooperatingAgency;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.CooperatingAgencyRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL-backed test of the cooperating agency directory and of the selection a
 * proposal stores: the 21 defaults are seeded in the documented order and never
 * seeded twice, a custom agency is stored exactly once even when the same name is
 * added again in a different case, and a proposal's selection is stored relationally
 * (unchecking an agency removes only the link, never the agency itself).
 * <p>
 * Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CooperatingAgencyDirectoryPostgresTest {

    @Autowired
    private CooperatingAgencyService cooperatingAgencyService;
    @Autowired
    private CooperatingAgencyRepository cooperatingAgencyRepository;
    @Autowired
    private ProposalRepository proposalRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User proponent;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        User user = new User();
        user.setName("Agency Proponent");
        user.setEmail("agency@msunaawan.edu.ph");
        user.setPassword("secret");
        user.setRole("PROPONENT");
        user.setStatus("APPROVED");
        proponent = userRepository.saveAndFlush(user);
    }

    private CooperatingAgency agencyNamed(String name) {
        return cooperatingAgencyRepository.findAllOrdered().stream()
                .filter(agency -> name.equals(agency.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("seeded agency not found: " + name));
    }

    private Proposal draft(String title) {
        Proposal proposal = new Proposal();
        proposal.setProponent(proponent);
        proposal.setProjectTitle(title);
        proposal.setStatus("DRAFT");
        return proposalRepository.saveAndFlush(proposal);
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value != null ? value : 0;
    }

    @Test
    void everyDefaultAgencyIsSeededInTheDocumentedOrder() {
        List<String> names = cooperatingAgencyService.getAllAgencies().stream()
                .map(CooperatingAgency::getName)
                .toList();

        assertTrue(names.containsAll(CooperatingAgencyInitializer.DEFAULT_AGENCIES),
                "the seeded directory is missing default agencies: " + names);
        assertEquals(CooperatingAgencyInitializer.DEFAULT_AGENCIES,
                names.stream().filter(CooperatingAgencyInitializer.DEFAULT_AGENCIES::contains).toList(),
                "the checklist order must be the documented one");
    }

    @Test
    void theSeedIsIdempotentSoARestartNeverDuplicatesIt() {
        int before = cooperatingAgencyService.getAllAgencies().size();

        new CooperatingAgencyInitializer(jdbcTemplate).initializeDirectory();
        new CooperatingAgencyInitializer(jdbcTemplate).initializeDirectory();

        assertEquals(before, cooperatingAgencyService.getAllAgencies().size());
    }

    @Test
    void aCustomAgencyIsStoredOnceAndIsAvailableToEveryProponent() {
        CooperatingAgency created =
                cooperatingAgencyService.addAgency("  Bureau of Fisheries and Aquatic Resources (BFAR) ");
        assertNotNull(created.getId());
        assertEquals("Bureau of Fisheries and Aquatic Resources (BFAR)", created.getName());

        // Stored as exactly one row, with the name trimmed.
        assertEquals(1, count("SELECT COUNT(*) FROM cooperating_agencies WHERE LOWER(name) = LOWER(?)",
                created.getName()));

        // Another proponent adding the same agency in a different case is rejected and
        // no second row appears.
        IllegalStateException duplicate = assertThrows(IllegalStateException.class,
                () -> cooperatingAgencyService.addAgency("bureau of fisheries and aquatic resources (bfar)"));
        assertTrue(duplicate.getMessage().contains("already in the cooperating agencies list"));
        assertEquals(1, count("SELECT COUNT(*) FROM cooperating_agencies WHERE LOWER(name) = LOWER(?)",
                created.getName()));

        // The new agency is offered by the directory the checklist loads, in its place.
        assertEquals(created.getName(), cooperatingAgencyService.getAllAgencies().get(0).getName());
    }

    @Test
    void theSelectionIsStoredRelationallyAndCanBeRestored() {
        CooperatingAgency dost = agencyNamed("Department of Science and Technology (DOST)");
        CooperatingAgency denr = agencyNamed("Department of Environment and Natural Resources (DENR)");
        Proposal proposal = draft("Agency selection persistence");

        List<CooperatingAgency> selected =
                cooperatingAgencyService.replaceSelection(proposal, List.of(denr.getId(), dost.getId()));

        // Directory order, and the legacy text column is derived from it.
        assertEquals(List.of(denr.getName(), dost.getName()),
                selected.stream().map(CooperatingAgency::getName).toList());
        assertEquals(2, count("SELECT COUNT(*) FROM proposal_cooperating_agencies WHERE proposal_id = ?",
                proposal.getId()));

        // It reads back through the service, which is what the edit form restores from.
        assertEquals(List.of(denr.getName(), dost.getName()),
                cooperatingAgencyService.getSelection(proposal.getId()).stream()
                        .map(CooperatingAgency::getName)
                        .toList());
    }

    @Test
    void uncheckingAnAgencyKeepsItInTheDirectoryAndInOtherProposals() {
        CooperatingAgency dost = agencyNamed("Department of Science and Technology (DOST)");
        Proposal first = draft("First proposal");
        Proposal second = draft("Second proposal");
        cooperatingAgencyService.replaceSelection(first, List.of(dost.getId()));
        cooperatingAgencyService.replaceSelection(second, List.of(dost.getId()));

        // The first proponent unchecks the agency.
        cooperatingAgencyService.replaceSelection(first, List.of());

        assertEquals(0, count("SELECT COUNT(*) FROM proposal_cooperating_agencies WHERE proposal_id = ?",
                first.getId()));
        assertEquals(1, count("SELECT COUNT(*) FROM proposal_cooperating_agencies WHERE proposal_id = ?",
                second.getId()));
        // The agency itself is untouched: it is still in the shared directory.
        assertTrue(cooperatingAgencyRepository.findById(dost.getId()).isPresent());
    }

    @Test
    void deletingAProposalRemovesItsLinksAndKeepsTheAgencies() {
        CooperatingAgency dost = agencyNamed("Department of Science and Technology (DOST)");
        Proposal proposal = draft("Proposal to delete");
        cooperatingAgencyService.replaceSelection(proposal, List.of(dost.getId()));

        // Mirrors ProposalService.deleteProposal(): the links go first, then the row.
        cooperatingAgencyService.deleteSelection(proposal.getId());
        proposalRepository.delete(proposal);
        proposalRepository.flush();

        assertEquals(0, count("SELECT COUNT(*) FROM proposal_cooperating_agencies WHERE proposal_id = ?",
                proposal.getId()));
        assertTrue(cooperatingAgencyRepository.findById(dost.getId()).isPresent());
    }

    @Test
    void anUnknownAgencyIdIsRejectedBeforeAnythingIsStored() {
        Proposal proposal = draft("Unknown agency");
        cooperatingAgencyService.replaceSelection(proposal, List.of(agencyNamed(
                "Department of Science and Technology (DOST)").getId()));

        assertThrows(IllegalArgumentException.class,
                () -> cooperatingAgencyService.replaceSelection(proposal, List.of(999_999L)));

        // The failed save left the previous selection untouched (same transaction).
        assertEquals(1, count("SELECT COUNT(*) FROM proposal_cooperating_agencies WHERE proposal_id = ?",
                proposal.getId()));
    }
}
