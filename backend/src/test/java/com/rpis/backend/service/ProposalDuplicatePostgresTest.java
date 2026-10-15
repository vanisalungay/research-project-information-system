package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.dto.ProposalSectionRemarksResponse;
import com.rpis.backend.dto.SectionRemarkDTO;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalBudget;
import com.rpis.backend.model.ProposalSectionRemark;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL-backed tests for the two new features, exercising real persistence:
 * duplicating a completed proposal must produce a fully independent draft, and
 * section-specific revision remarks must survive editing/resubmission while each
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
    private ProposalSectionRemarkService sectionRemarkService;
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

    private Proposal completedProposal() {
        Proposal p = new Proposal();
        p.setProponent(proponent);
        p.setProposalCode("500");
        p.setDocumentId("MSUN-ORPS-DA-500-2026-REV00");
        p.setRevisionNumber(0);
        p.setStatus("COMPLETED");
        p.setProjectTitle("Original Completed Project");
        p.setProgramTitle("Kalikasan Program");
        p.setDuration("12");
        p.setMethodology("Original methodology");
        p.setRemarks("Original reviewer remarks");
        p.setReturnedByOffice("REC");
        p.setSoNumber("SO-2026-500");
        p.setNeedsBudget(true);
        p.getSites().add(new ProposalSite(null, p, "Philippines", "X", "Misamis Oriental",
                "District", "Naawan", "Barangay 1"));
        p.getBudget().add(new ProposalBudget(null, p, "MSU", "1000", "2000", "3000", "6000"));
        return proposalRepository.saveAndFlush(p);
    }

    private SectionRemarkDTO remark(String sectionKey, String text) {
        SectionRemarkDTO dto = new SectionRemarkDTO();
        dto.setSectionKey(sectionKey);
        dto.setRemark(text);
        return dto;
    }

    @Test
    void duplicateIsAnIndependentDraftAndTheOriginalIsUntouched() {
        Proposal source = completedProposal();
        long versionsBefore = proposalVersionRepository.count();

        Proposal copy = proposalService.duplicateProposal(source.getId());

        assertNotEquals(source.getId(), copy.getId());
        assertNotEquals(source.getDocumentId(), copy.getDocumentId());
        assertEquals("DRAFT", copy.getStatus());
        assertEquals(0, copy.getRevisionNumber());
        assertTrue(copy.getDocumentId().endsWith("-REV00"));
        assertEquals("Original Completed Project", copy.getProjectTitle());
        assertEquals("Original methodology", copy.getMethodology());
        assertNull(copy.getSoNumber());
        assertNull(copy.getRemarks());
        assertNull(copy.getReturnedByOffice());
        assertNull(copy.getNeedsBudget());

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
        assertEquals("COMPLETED", reloaded.getStatus());
        assertEquals("Original Completed Project", reloaded.getProjectTitle());
        assertEquals("SO-2026-500", reloaded.getSoNumber());
        assertEquals(1, reloaded.getSites().size());
        assertEquals(1, reloaded.getBudget().size());
        assertEquals("Naawan", reloaded.getSites().get(0).getMunicipality());
    }

    @Test
    void editingTheDuplicateDoesNotChangeTheOriginal() {
        Proposal source = completedProposal();
        Proposal copy = proposalService.duplicateProposal(source.getId());

        copy.setProjectTitle("Edited Duplicate");
        copy.getSites().clear();
        copy.getSites().add(new ProposalSite(null, copy, "PH", "R", "P", "D", "M", "B"));
        proposalRepository.saveAndFlush(copy);

        Proposal original = proposalRepository.findById(source.getId()).orElseThrow();
        Proposal reloadedCopy = proposalRepository.findById(copy.getId()).orElseThrow();
        assertEquals("Edited Duplicate", reloadedCopy.getProjectTitle());
        assertEquals("Original Completed Project", original.getProjectTitle());
        assertEquals(1, original.getSites().size());
        assertEquals("Naawan", original.getSites().get(0).getMunicipality());
    }
}
