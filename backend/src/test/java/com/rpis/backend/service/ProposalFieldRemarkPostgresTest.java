package com.rpis.backend.service;

import com.rpis.backend.dto.FieldRemarkRequest;
import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.dto.ProposalSectionRemarksResponse;
import com.rpis.backend.dto.SectionRemarkDTO;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalLimitation;
import com.rpis.backend.model.ProposalPersonnel;
import com.rpis.backend.model.ProposalSectionRemark;
import com.rpis.backend.model.ProposalSite;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.ProposalSectionRemarkRepository;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL-backed tests for field-level revision remarks, exercising real
 * persistence: remarks saved beside individual fields (including repeated entries)
 * must be readable after a reload, must stay visible to the Proponent when the
 * proposal is returned, and must be retained as history when the Proponent
 * resubmits and a new review cycle begins.
 * <p>
 * Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProposalFieldRemarkPostgresTest {

    @Autowired
    private ProposalService proposalService;
    @Autowired
    private ProposalSectionRemarkService sectionRemarkService;
    @Autowired
    private ProposalRepository proposalRepository;
    @Autowired
    private ProposalSectionRemarkRepository sectionRemarkRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User reviewer;
    private User proponent;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE proposals RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");

        reviewer = new User();
        reviewer.setName("RPS Reviewer");
        reviewer.setEmail("field.remark.reviewer@msunaawan.edu.ph");
        reviewer.setPassword("secret");
        reviewer.setRole("RPS_STAFF");
        reviewer.setStatus("APPROVED");
        reviewer = userRepository.saveAndFlush(reviewer);

        proponent = new User();
        proponent.setName("Test Proponent");
        proponent.setEmail("field.remark.proponent@msunaawan.edu.ph");
        proponent.setPassword("secret");
        proponent.setRole("PROPONENT");
        proponent.setStatus("APPROVED");
        proponent = userRepository.saveAndFlush(proponent);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))));
    }

    private Proposal submittedProposal() {
        Proposal p = new Proposal();
        p.setProponent(proponent);
        p.setProposalCode("700");
        p.setDocumentId("MSUN-ORPS-DA-700-2026-REV00");
        p.setRevisionNumber(0);
        p.setStatus("SUBMITTED");
        p.setProjectTitle("Water Filtration");
        p.setDuration("12");
        p.setMethodology("Original methodology");
        p.setLimitationsText("Original limitations text");
        p.setProjectLeaderSex("F");
        p.getSites().add(new ProposalSite(null, p, "Philippines", "X", "Misamis Oriental",
                "District", "Naawan", "Barangay 1"));
        p.getPersonnel().add(new ProposalPersonnel(null, p, "Project Leader", "12", "Lead"));
        p.getPersonnel().add(new ProposalPersonnel(null, p, "Assistant", "50", "Assist"));
        p.getLimitations().add(new ProposalLimitation(null, p, "Limited data", "Use secondary data"));
        return proposalRepository.saveAndFlush(p);
    }

    private FieldRemarkRequest field(String sectionKey, String fieldKey, String remark) {
        FieldRemarkRequest request = new FieldRemarkRequest();
        request.setSectionKey(sectionKey);
        request.setFieldKey(fieldKey);
        request.setFieldLabel(fieldKey);
        request.setRemark(remark);
        return request;
    }

    private FieldRemarkRequest entry(String sectionKey, String fieldKey, int index, Long entryId,
                                     String snapshot, String remark) {
        FieldRemarkRequest request = field(sectionKey, fieldKey, remark);
        request.setEntryIndex(index);
        request.setEntryId(entryId);
        request.setEntrySnapshot(snapshot);
        return request;
    }

    @Test
    void remarksOnSeveralFieldsIncludingRepeatedEntriesArePersisted() {
        authenticateAs(reviewer);
        Proposal proposal = submittedProposal();
        Long personnelId = proposal.getPersonnel().get(0).getId();
        assertNotNull(personnelId, "child rows must have ids once persisted");

        sectionRemarkService.upsertFieldRemark(proposal, field("01", "projectTitle", "Shorten the title."));
        sectionRemarkService.upsertFieldRemark(proposal, field("12", "methodology", "Clarify sampling."));
        sectionRemarkService.upsertFieldRemark(proposal,
                entry("24", "personnel[0].time", 0, personnelId,
                        "{\"position\":\"Project Leader\",\"time\":\"12\"}", "Increase to 25%."));

        // Read back in a fresh transaction so persistence (not the identity map) is proven.
        ProposalSectionRemarksResponse response = sectionRemarkService.getRemarksForProposal(proposal.getId());
        assertEquals(1, response.getActiveRound());
        assertEquals(1, response.getRounds().size());
        assertEquals(3, response.getRounds().get(0).getRemarks().size());

        ProposalSectionRemarksResponse.Item title = response.getRounds().get(0).getRemarks().stream()
                .filter(item -> "projectTitle".equals(item.getFieldKey())).findFirst().orElseThrow();
        assertEquals("01", title.getSectionKey());
        assertEquals("Project Profile", title.getSectionLabel());
        assertEquals("Shorten the title.", title.getRemark());
        assertEquals("RPS_STAFF", title.getReviewerRole());

        ProposalSectionRemarksResponse.Item personnel = response.getRounds().get(0).getRemarks().stream()
                .filter(item -> "personnel[0].time".equals(item.getFieldKey())).findFirst().orElseThrow();
        assertEquals(0, personnel.getEntryIndex());
        assertEquals(personnelId, personnel.getEntryId());
        assertEquals("{\"position\":\"Project Leader\",\"time\":\"12\"}", personnel.getEntrySnapshot());
    }

    @Test
    void remarksSavedBeforeTheReturnStayVisibleToTheProponent() {
        authenticateAs(reviewer);
        Proposal proposal = submittedProposal();
        sectionRemarkService.upsertFieldRemark(proposal, field("01", "projectTitle", "Shorten the title."));

        // RPS returns the proposal for revision, adding a general note.
        SectionRemarkDTO note = new SectionRemarkDTO();
        note.setSectionKey("12");
        note.setRemark("General: revise the method.");
        sectionRemarkService.recordForwardRemarks(proposal, List.of(note), "RPS",
                reviewer.getId(), reviewer.getName());

        ProposalSectionRemarksResponse response = sectionRemarkService.getRemarksForProposal(proposal.getId());
        assertEquals(1, response.getActiveRound(), "the field remark must still be the active feedback");
        assertEquals(1, response.getRounds().size(), "both remarks belong to the same review cycle");
        assertEquals(2, response.getRounds().get(0).getRemarks().size());
        assertTrue(response.getRounds().get(0).isActive());
    }

    @Test
    void entryMetadataIsStoredSoRemarksCanBeRealignedAfterEdits() {
        authenticateAs(reviewer);
        Proposal proposal = submittedProposal();
        Long firstId = proposal.getPersonnel().get(0).getId();

        sectionRemarkService.upsertFieldRemark(proposal,
                entry("24", "personnel[0].position", 0, firstId, "{\"position\":\"Project Leader\"}",
                        "Rename this role."));

        ProposalSectionRemark stored = sectionRemarkRepository
                .findByProposalIdOrderByRoundDescIdAsc(proposal.getId()).get(0);
        assertEquals(0, stored.getEntryIndex());
        assertEquals(firstId, stored.getEntryId());
        assertEquals("{\"position\":\"Project Leader\"}", stored.getEntrySnapshot());
        assertEquals("personnel[0].position", stored.getFieldKey());
        assertEquals("24", stored.getSectionKey());
    }

    @Test
    void resubmissionClosesTheRoundAndRetainsItAsHistory() {
        authenticateAs(reviewer);
        Proposal proposal = submittedProposal();
        sectionRemarkService.upsertFieldRemark(proposal, field("01", "projectTitle", "Round one remark."));
        proposal.setStatus("RPS_RETURNED");
        proposalRepository.saveAndFlush(proposal);

        // The proponent edits the flagged field and resubmits.
        authenticateAs(proponent);
        ProposalRequest request = new ProposalRequest();
        request.setProjectTitle("Shortened title");
        request.setDuration("12");
        request.setStatus("SUBMITTED");
        proposalService.saveProposal(request, proposal.getId());

        Proposal resubmitted = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertEquals("SUBMITTED", resubmitted.getStatus());
        assertEquals(1, resubmitted.getRevisionNumber());

        // The remark is retained (never deleted) but the round is closed.
        ProposalSectionRemarksResponse afterSubmit =
                sectionRemarkService.getRemarksForProposal(proposal.getId());
        assertNull(afterSubmit.getActiveRound());
        assertEquals(1, afterSubmit.getRounds().size());
        assertEquals("Round one remark.", afterSubmit.getRounds().get(0).getRemarks().get(0).getRemark());
        assertEquals(ProposalSectionRemark.STATUS_SUPERSEDED,
                sectionRemarkRepository.findByProposalIdOrderByRoundDescIdAsc(proposal.getId())
                        .get(0).getStatus());

        // A new review cycle starts round 2 and keeps round 1 as history.
        authenticateAs(reviewer);
        sectionRemarkService.upsertFieldRemark(proposal, field("01", "projectTitle", "Round two remark."));
        ProposalSectionRemarksResponse secondRound =
                sectionRemarkService.getRemarksForProposal(proposal.getId());
        assertEquals(2, secondRound.getActiveRound());
        assertEquals(2, secondRound.getRounds().size());
        assertEquals("Round two remark.", secondRound.getRounds().get(0).getRemarks().get(0).getRemark());
        assertEquals("Round one remark.", secondRound.getRounds().get(1).getRemarks().get(0).getRemark());
    }

    @Test
    void projectLeaderSexAndLimitationsTextArePersistedAndLimitationsTableIsNotWiped() {
        Proposal proposal = submittedProposal();

        authenticateAs(proponent);
        ProposalRequest request = new ProposalRequest();
        request.setProjectTitle("Water Filtration");
        request.setDuration("12");
        request.setProjectLeaderSex("M");
        request.setLimitationsText("Updated limitations text");
        request.setStatus("DRAFT");
        proposalService.saveProposal(request, proposal.getId());

        Proposal saved = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertEquals("M", saved.getProjectLeaderSex());
        assertEquals("Updated limitations text", saved.getLimitationsText());
        // A request that omits the limitations table must not delete existing rows.
        assertEquals(1, saved.getLimitations().size());
        assertEquals("Limited data", saved.getLimitations().get(0).getLimitation());
    }
}
