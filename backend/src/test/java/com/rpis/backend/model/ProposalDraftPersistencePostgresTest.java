package com.rpis.backend.model;

import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.GadChecklistScoring;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL-backed round trip of the draft content the create-proposal workflow stores:
 * the GAD Checklist 2 JSON, the Certification &amp; Endorsement fields, the personnel rows
 * with their own "% Time Devoted" values, and the Logical Framework rows.
 *
 * <p>Runs against the isolated {@code rpis_test} database only.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProposalDraftPersistencePostgresTest {

    private static final List<String> GAD_ITEMS = List.of(
            "1.1", "1.2", "2.0", "3.1", "3.2", "4.0", "5.0",
            "6.1", "6.2", "6.3", "7.0", "8.0", "9.1", "9.2", "10.1", "10.2", "10.3");

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
        user.setName("Draft Persistence Proponent");
        user.setEmail("draft.persistence.proponent@msunaawan.edu.ph");
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

    private GadChecklistData completeChecklist() {
        GadChecklistData checklist = new GadChecklistData();
        Map<String, String> responses = new LinkedHashMap<>();
        Map<String, String> comments = new LinkedHashMap<>();
        for (int i = 0; i < GAD_ITEMS.size(); i++) {
            String item = GAD_ITEMS.get(i);
            responses.put(item, i % 2 == 0 ? "YES" : "PARTLY");
            comments.put(item, "Comment for " + item);
        }
        checklist.setResponses(responses);
        checklist.setComments(comments);
        GadChecklistScoring.recalculate(checklist);
        return checklist;
    }

    @Test
    void theGadChecklistSurvivesTheDatabaseRoundTrip() {
        Proposal proposal = newProposal("GAD checklist persistence");
        GadChecklistData checklist = completeChecklist();
        proposal.setGadChecklist(checklist);
        proposal.setGadScore(checklist.getTotalScore());
        Long id = proposalRepository.saveAndFlush(proposal).getId();

        Proposal reloaded = proposalRepository.findById(id).orElseThrow();

        assertNotNull(reloaded.getGadChecklist());
        assertEquals("YES", reloaded.getGadChecklist().getResponses().get("1.1"));
        assertEquals("PARTLY", reloaded.getGadChecklist().getResponses().get("1.2"));
        assertEquals("Comment for 1.1", reloaded.getGadChecklist().getComments().get("1.1"));
        assertEquals(checklist.getTotalScore(), reloaded.getGadChecklist().getTotalScore());
        assertEquals(Boolean.TRUE, reloaded.getGadChecklist().getCompleted());
        assertEquals(checklist.getInterpretation(), reloaded.getGadChecklist().getInterpretation());
        assertEquals(checklist.getTotalScore(), reloaded.getGadScore());
    }

    @Test
    void theChecklistIsStoredAsItsOwnJsonColumn() {
        Proposal proposal = newProposal("GAD checklist column");
        proposal.setGadChecklist(completeChecklist());
        Long id = proposalRepository.saveAndFlush(proposal).getId();

        String column = jdbcTemplate.queryForObject(
                "SELECT gad_checklist_json FROM proposals WHERE id = ?", String.class, id);

        assertTrue(column.contains("\"responses\""), column);
        assertTrue(column.contains("\"comments\""), column);
        assertTrue(column.contains("\"totalScore\""), column);
    }

    @Test
    void theCertificationFieldsSurviveTheDatabaseRoundTrip() {
        Proposal proposal = newProposal("Certification persistence");
        proposal.setSubmittedByName("Maria Santos");
        proposal.setSubmittedByDesignation("Project Leader");
        proposal.setSubmittedByDate("2026-11-10");
        proposal.setSubmittedByFileName("Maria signature.png");
        proposal.setSubmittedByFilePath("submitted-by-signatures/signature.png");
        proposal.setEndorsedByName("Dean Reyes");
        proposal.setEndorsedByDesignation("College Dean");
        proposal.setEndorsedByDate("2026-11-09");
        proposal.setEndorsementFileName("Dean Endorsement Letter.pdf");
        proposal.setEndorsementFilePath("endorsement-documents/letter.pdf");
        Long id = proposalRepository.saveAndFlush(proposal).getId();

        Proposal reloaded = proposalRepository.findById(id).orElseThrow();

        assertEquals("Maria Santos", reloaded.getSubmittedByName());
        assertEquals("Project Leader", reloaded.getSubmittedByDesignation());
        assertEquals("2026-11-10", reloaded.getSubmittedByDate());
        assertEquals("Maria signature.png", reloaded.getSubmittedByFileName());
        assertEquals("submitted-by-signatures/signature.png", reloaded.getSubmittedByFilePath());
        assertEquals("Dean Reyes", reloaded.getEndorsedByName());
        assertEquals("College Dean", reloaded.getEndorsedByDesignation());
        assertEquals("2026-11-09", reloaded.getEndorsedByDate());
        assertEquals("Dean Endorsement Letter.pdf", reloaded.getEndorsementFileName());
        assertEquals("endorsement-documents/letter.pdf", reloaded.getEndorsementFilePath());
    }

    @Test
    void everyPersonnelRowKeepsItsOwnTimeDevoted() {
        Proposal proposal = newProposal("Personnel persistence");
        proposal.setPersonnel(new ArrayList<>(List.of(
                new ProposalPersonnel(null, proposal, "Project Leader", "25", "Lead the project"),
                new ProposalPersonnel(null, proposal, "Encoder", "50", "Encode the data"))));
        Long id = proposalRepository.saveAndFlush(proposal).getId();

        Proposal reloaded = proposalRepository.findById(id).orElseThrow();

        assertEquals(2, reloaded.getPersonnel().size());
        assertEquals("Project Leader", reloaded.getPersonnel().get(0).getPosition());
        assertEquals("25", reloaded.getPersonnel().get(0).getTimeDevoted());
        assertEquals("Lead the project", reloaded.getPersonnel().get(0).getResponsibilities());
        assertEquals("Encoder", reloaded.getPersonnel().get(1).getPosition());
        assertEquals("50", reloaded.getPersonnel().get(1).getTimeDevoted());
        assertEquals("Encode the data", reloaded.getPersonnel().get(1).getResponsibilities());
    }

    @Test
    void theLogicalFrameworkRowsSurviveTheDatabaseRoundTrip() {
        Proposal proposal = newProposal("Logical framework persistence");
        proposal.setLogFrames(new ArrayList<>(List.of(
                new ProposalLogFrame(null, proposal, "Outcome A", "Output A"),
                new ProposalLogFrame(null, proposal, "Outcome B", "Output B"))));
        Long id = proposalRepository.saveAndFlush(proposal).getId();

        Proposal reloaded = proposalRepository.findById(id).orElseThrow();

        assertEquals(2, reloaded.getLogFrames().size());
        assertEquals("Outcome A", reloaded.getLogFrames().get(0).getOutcome());
        assertEquals("Output A", reloaded.getLogFrames().get(0).getOutput());
        assertEquals("Outcome B", reloaded.getLogFrames().get(1).getOutcome());
        assertEquals("Output B", reloaded.getLogFrames().get(1).getOutput());
    }

    @Test
    void aProposalThatPredatesTheChecklistKeepsItsLegacyScore() {
        Proposal proposal = newProposal("Legacy GAD score");
        proposal.setGadScore(new BigDecimal("15.00"));
        proposal.setGadFileName("legacy-gad.pdf");
        Long id = proposalRepository.saveAndFlush(proposal).getId();

        Proposal reloaded = proposalRepository.findById(id).orElseThrow();

        assertEquals(new BigDecimal("15.00"), reloaded.getGadScore());
        assertEquals("legacy-gad.pdf", reloaded.getGadFileName());
        assertNull(reloaded.getGadChecklist(), "no invented checklist for an older record");
    }
}
