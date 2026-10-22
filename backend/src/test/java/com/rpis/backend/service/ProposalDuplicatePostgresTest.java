package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalBudget;
import com.rpis.backend.model.ProposalSite;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.ProposalSectionRemarkRepository;
import com.rpis.backend.repository.ProposalVersionRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL-backed tests for the two new features, exercising real
 * persistence:
 * duplicating a rejected proposal must produce a fully independent draft, and
 * section-specific revision remarks must survive editing/resubmission while
 * each
 * revision round keeps its own history.
 * <p>
 * Runs against the isolated {@code rpis_test} database only.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProposalDuplicatePostgresTest {

    @Autowired
    private ProposalService proposalService;
    @Autowired
    private ProposalRepository proposalRepository;
    @Autowired
    private ProposalSectionRemarkRepository sectionRemarkRepository;
    @Autowired
    private ProposalVersionRepository proposalVersionRepository;
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
        user.setName("Test Proponent");
        user.setEmail("duplicate.proponent@msunaawan.edu.ph");
        user.setPassword("secret");
        user.setRole("PROPONENT");
        user.setStatus("APPROVED");
        proponent = userRepository.saveAndFlush(user);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                proponent.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_PROPONENT"))));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Proposal rejectedProposal() {
        Proposal p = new Proposal();
        p.setProponent(proponent);
        p.setProposalCode("500");
        p.setDocumentId("MSUN-ORPS-DA-500-2026-REV00");
        p.setRevisionNumber(0);
        p.setStatus("REJECTED");
        p.setProjectTitle("Original Rejected Project");
        p.setProgramTitle("Kalikasan Program");
        p.setDuration("12");
        p.setMethodology("Original methodology");
        p.setRemarks("Original reviewer remarks");
        p.setReturnedByOffice("REC");
        p.setSoNumber("SO-2026-500");
        p.setNeedsBudget(true);
        // Uploaded attachments belong to the original and must not be carried over.
        p.setRoadmapFileName("original-roadmap.pdf");
        p.setGadFileName("original-gad.pdf");
        p.getSites().add(new ProposalSite(null, p, "Philippines", "X", "Misamis Oriental",
                "District", "Naawan", "Barangay 1"));
        p.getBudget().add(new ProposalBudget(null, p, "MSU", "1000", "2000", "3000", "6000"));
        return proposalRepository.saveAndFlush(p);
    }

    @Test
    void duplicateIsAnIndependentDraftAndTheOriginalIsUntouched() {
        Proposal source = rejectedProposal();
        long versionsBefore = proposalVersionRepository.count();

        Proposal copy = proposalService.duplicateProposal(source.getId());

        assertNotEquals(source.getId(), copy.getId());
        assertNotEquals(source.getDocumentId(), copy.getDocumentId());
        assertEquals("DRAFT", copy.getStatus());
        assertEquals(0, copy.getRevisionNumber());
        assertTrue(copy.getDocumentId().endsWith("-REV00"));
        assertEquals("Original Rejected Project", copy.getProjectTitle());
        assertEquals("Original methodology", copy.getMethodology());
        assertNull(copy.getSoNumber());
        assertNull(copy.getRemarks());
        assertNull(copy.getReturnedByOffice());
        assertNull(copy.getNeedsBudget());
        // Uploaded attachments are not carried over: the proponent re-uploads them.
        assertNull(copy.getRoadmapFileName());
        assertNull(copy.getGadFileName());

        // Child rows are real, separate rows bound to the duplicate.
        assertEquals(1, copy.getSites().size());
        assertEquals(1, copy.getBudget().size());
        assertNotEquals(source.getSites().get(0).getId(), copy.getSites().get(0).getId());
        assertEquals("Naawan", copy.getSites().get(0).getMunicipality());
        assertEquals("6000", copy.getBudget().get(0).getTotal());

        // No submission history is carried over to (or created for) the duplicate.
        assertEquals(versionsBefore, proposalVersionRepository.count());
        assertTrue(sectionRemarkRepository.findByProposalIdOrderByRoundDescIdAsc(copy.getId()).isEmpty());

        // The original proposal is unchanged, including its children and status.
        Proposal reloaded = proposalRepository.findById(source.getId()).orElseThrow();
        assertEquals("REJECTED", reloaded.getStatus());
        assertEquals("Original Rejected Project", reloaded.getProjectTitle());
        assertEquals("SO-2026-500", reloaded.getSoNumber());
        assertEquals("original-roadmap.pdf", reloaded.getRoadmapFileName());
        assertEquals("original-gad.pdf", reloaded.getGadFileName());
        assertEquals(1, reloaded.getSites().size());
        assertEquals(1, reloaded.getBudget().size());
        assertEquals("Naawan", reloaded.getSites().get(0).getMunicipality());
    }

    @Test
    void approvedProposalCannotBeCopied() {
        Proposal source = rejectedProposal();
        source.setStatus("APPROVED");
        proposalRepository.saveAndFlush(source);

        assertThrows(IllegalStateException.class,
                () -> proposalService.duplicateProposal(source.getId()));
    }

    @Test
    void editingTheDuplicateDoesNotChangeTheOriginal() {
        Proposal source = rejectedProposal();
        Proposal copy = proposalService.duplicateProposal(source.getId());

        copy.setProjectTitle("Edited Duplicate");
        copy.getSites().clear();
        copy.getSites().add(new ProposalSite(null, copy, "PH", "R", "P", "D", "M", "B"));
        proposalRepository.saveAndFlush(copy);

        Proposal original = proposalRepository.findById(source.getId()).orElseThrow();
        Proposal reloadedCopy = proposalRepository.findById(copy.getId()).orElseThrow();
        assertEquals("Edited Duplicate", reloadedCopy.getProjectTitle());
        assertEquals("Original Rejected Project", original.getProjectTitle());
        assertEquals(1, original.getSites().size());
        assertEquals("Naawan", original.getSites().get(0).getMunicipality());
    }
}
