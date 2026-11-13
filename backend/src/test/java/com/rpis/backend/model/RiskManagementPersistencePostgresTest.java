package com.rpis.backend.model;

import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL-backed round trip of the structured Risk Management Plan (section 21): the
 * entries a proponent enters as a table are written to the proposals table and read back
 * with the four fields of every entry intact, in the order they were entered.
 *
 * <p>Also pins the backward-compatibility rule: a proposal that predates the table has an
 * empty structured plan and keeps its single-text {@code risks} column exactly as it was.
 * </p>
 *
 * <p>Runs against the isolated {@code rpis_test} database only.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RiskManagementPersistencePostgresTest {

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
        user.setName("Risk Plan Proponent");
        user.setEmail("risk.plan.proponent@msunaawan.edu.ph");
        user.setPassword("secret");
        user.setRole("PROPONENT");
        user.setStatus("APPROVED");
        proponent = userRepository.saveAndFlush(user);
    }

    private Proposal newProposal(String title) {
        Proposal proposal = new Proposal();
        proposal.setProponent(proponent);
        proposal.setProjectTitle(title);
        proposal.setStatus("DRAFT");
        return proposal;
    }

    @Test
    void severalEntriesSurviveTheDatabaseRoundTrip() {
        Proposal proposal = newProposal("Structured risk plan");
        proposal.setRiskManagement(new ArrayList<>(List.of(
                new RiskManagementEntry("Delayed delivery of research equipment", "High", "Medium",
                        "Identify alternative suppliers."),
                new RiskManagementEntry("Low participation of fisherfolk", "Low", "High",
                        "Hold community assemblies."))));
        Long id = proposalRepository.saveAndFlush(proposal).getId();

        // Read it back from a fresh query, so the values really come from the database.
        Proposal reloaded = proposalRepository.findById(id).orElseThrow();

        assertEquals(2, reloaded.getRiskManagement().size());
        assertEquals("Delayed delivery of research equipment",
                reloaded.getRiskManagement().get(0).getIdentifiedRisks());
        assertEquals("High", reloaded.getRiskManagement().get(0).getLikelihood());
        assertEquals("Medium", reloaded.getRiskManagement().get(0).getImpact());
        assertEquals("Identify alternative suppliers.",
                reloaded.getRiskManagement().get(0).getMitigationMeasure());
        assertEquals("Low participation of fisherfolk",
                reloaded.getRiskManagement().get(1).getIdentifiedRisks());
        assertEquals("Low", reloaded.getRiskManagement().get(1).getLikelihood());
        assertEquals("High", reloaded.getRiskManagement().get(1).getImpact());
        assertEquals("Hold community assemblies.", reloaded.getRiskManagement().get(1).getMitigationMeasure());
    }

    @Test
    void theEntriesAreStoredAsTheirOwnColumnAndNotAsConcatenatedText() {
        Proposal proposal = newProposal("Structured risk plan column");
        proposal.setRiskManagement(new ArrayList<>(List.of(
                new RiskManagementEntry("Delayed delivery", "High", "Medium", "Order early."))));
        Long id = proposalRepository.saveAndFlush(proposal).getId();

        String column = jdbcTemplate.queryForObject(
                "SELECT risk_management_json FROM proposals WHERE id = ?", String.class, id);

        assertTrue(column.contains("\"identifiedRisks\":\"Delayed delivery\""),
                "the identified risk is stored on its own field: " + column);
        assertTrue(column.contains("\"likelihood\":\"High\""), "so is the likelihood: " + column);
        assertTrue(column.contains("\"impact\":\"Medium\""), "so is the impact: " + column);
        assertTrue(column.contains("\"mitigationMeasure\":\"Order early.\""),
                "so is the mitigation measure: " + column);
    }

    @Test
    void aProposalThatPredatesTheTableKeepsItsSingleTextAndHasNoEntries() {
        Proposal proposal = newProposal("Legacy risk plan");
        proposal.setRisks("Risks, assumptions and mitigation strategies in prose.");
        Long id = proposalRepository.saveAndFlush(proposal).getId();

        Proposal reloaded = proposalRepository.findById(id).orElseThrow();

        assertTrue(reloaded.getRiskManagement().isEmpty(), "no structured entries for an older record");
        assertEquals("Risks, assumptions and mitigation strategies in prose.", reloaded.getRisks(),
                "and its text is untouched");
    }
}
