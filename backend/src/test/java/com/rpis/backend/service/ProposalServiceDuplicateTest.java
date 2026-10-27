package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.ProposalBudget;
import com.rpis.backend.model.ProposalLimitation;
import com.rpis.backend.model.ProposalLogFrame;
import com.rpis.backend.model.ProposalOtherProject;
import com.rpis.backend.model.ProposalPersonnel;
import com.rpis.backend.model.ProposalPriorityAgenda;
import com.rpis.backend.model.ProposalSite;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the "Make a Copy" feature: a proposal can be reused as the starting
 * point for a brand-new draft that gets its own identifiers, copies only reusable
 * content, and never modifies or inherits the original proposal's workflow state.
 * The original's status never decides whether it can be copied.
 */
class ProposalServiceDuplicateTest {

    private ProposalRepository proposalRepository;
    private NotificationService notificationService;
    private ProposalVersionService versionService;
    private ProposalAnnouncementService announcementService;
    private ProposalService service;

    /** The Call for Proposal the copy is created under. */
    private static final Long CALL_ID = 5L;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        notificationService = mock(NotificationService.class);
        announcementService = mock(ProposalAnnouncementService.class);
        versionService = mock(ProposalVersionService.class);
        service = new ProposalService(proposalRepository, userRepository, notificationService,
                announcementService, versionService, mock(ProposalSectionRemarkService.class),
                new CurrentUser(), mock(CooperatingAgencyService.class));

        when(proposalRepository.nextProposalCodeSequence()).thenReturn(100L);
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAll()).thenReturn(List.of());
        // The picker's call: open for submissions, so the copy may be created under it.
        when(announcementService.requireOpenAnnouncement(CALL_ID)).thenReturn(openCall(CALL_ID));
    }

    /** The Call for Proposal the proponent selected in the "Make a Copy" picker. */
    private ProposalAnnouncement openCall(Long id) {
        ProposalAnnouncement announcement = new ProposalAnnouncement();
        announcement.setId(id);
        announcement.setName("Call " + id);
        return announcement;
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private User proponent(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Proponent " + id);
        user.setRole("PROPONENT");
        return user;
    }

    /** A rejected proposal carrying reusable content plus workflow state. */
    private Proposal rejectedProposal(Long id, Long ownerId) {
        Proposal p = new Proposal();
        p.setId(id);
        p.setProponent(proponent(ownerId));
        p.setProposalCode("041");
        p.setDocumentId("MSUN-ORPS-DA-041-2026-REV00");
        p.setRevisionNumber(0);
        p.setStatus("REJECTED");
        p.setProposalAnnouncementId(3L);

        p.setProgramTitle("Kalikasan Program");
        p.setProjectTitle("Community-Based Water Filtration System");
        p.setProjectLeader("Juan Dela Cruz");
        p.setDuration("24");
        p.setCollege("College of Engineering");
        p.setAddress("Naawan, Misamis Oriental");
        p.setCooperatingAgencies("DOST");
        p.setResearchType("Applied");
        p.setInnovationGoals("Clean water");
        p.setSectorRelevance("Environment");
        p.setSdg(new ArrayList<>(List.of("Clean Water and Sanitation")));
        p.setExecutiveSummary("Summary");
        p.setRationale("Rationale");
        p.setFramework("Framework");
        p.setObjectivesGeneral("General objective");
        p.setObjectivesSpecific("Specific objective");
        p.setReview("Review");
        p.setReviewFileName("literature.pdf");
        p.setMethodology("Methodology");
        p.setRoadmapFileName("roadmap.pdf");
        p.setTechnologyTrl("5");
        p.setOutputs("Outputs");
        p.setOutcomes("Outcomes");
        p.setImpactEconomic("Economic");
        p.setImpactSocial("Social");
        p.setBeneficiaries("Beneficiaries");
        p.setBeneficiariesFileName("budget.pdf");
        p.setSustainability("Sustainability");
        p.setGadScore(new BigDecimal("8.50"));
        p.setGadFileName("gad.pdf");
        p.setRisks("Risks");
        p.setReferencesText("References");
        p.setOtherProjectsNumber("2");

        // Workflow state that must never be duplicated.
        p.setRemarks("General reviewer remarks");
        p.setReturnRemarks("Return remarks");
        p.setReturnedByOffice("REC");
        p.setReturnedByName("Reviewer");
        p.setReturnedByUserId(9L);
        p.setRevisionNotes("RPS note");
        p.setSoNumber("SO-2026-001");
        p.setSoFileName("so.pdf");
        p.setNeedsBudget(true);
        p.setReviewedBy("Reviewer");
        p.setReviewedByPosition("REC");

        p.getSites().add(new ProposalSite(null, p, "Philippines", "X", "Misamis Oriental",
                "District", "Naawan", "Barangay 1"));
        p.getLogFrames().add(new ProposalLogFrame(null, p, "Outcome 1", "Output 1"));
        p.getPersonnel().add(new ProposalPersonnel(null, p, "Project Leader", "12", "Lead"));
        p.getBudget().add(new ProposalBudget(null, p, "MSU", "1000", "2000", "3000", "6000"));
        p.getOtherProjects().add(new ProposalOtherProject(null, p, "Other project", "DOST", "Leader"));
        p.getPriorityAgendas().add(new ProposalPriorityAgenda(null, p, "kalikasan", true, "Value"));
        p.getLimitations().add(new ProposalLimitation(null, p, "Limitation", "Remark"));
        return p;
    }

    @Test
    void duplicateReceivesNewIdentifiersAndDraftWorkflowDefaults() {
        authenticate(7L, "PROPONENT");
        Proposal source = rejectedProposal(1L, 7L);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));

        Proposal copy = service.duplicateProposal(1L, CALL_ID);

        assertEquals("100", copy.getProposalCode());
        assertEquals("MSUN-ORPS-DA-100-" + Year.now().getValue() + "-REV00", copy.getDocumentId());
        assertEquals("DRAFT", copy.getStatus());
        assertEquals(0, copy.getRevisionNumber());
        assertSame(source.getProponent(), copy.getProponent());
        assertNotSame(source, copy);
    }

    @Test
    void duplicateCopiesReusableContentAndChildCollectionsAsIndependentRows() {
        authenticate(7L, "PROPONENT");
        Proposal source = rejectedProposal(1L, 7L);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));

        Proposal copy = service.duplicateProposal(1L, CALL_ID);

        assertEquals(source.getProjectTitle(), copy.getProjectTitle());
        assertEquals(source.getProgramTitle(), copy.getProgramTitle());
        assertEquals(source.getDuration(), copy.getDuration());
        assertEquals(source.getMethodology(), copy.getMethodology());
        assertEquals(source.getReferencesText(), copy.getReferencesText());
        assertEquals(source.getSdg(), copy.getSdg());
        assertNotSame(source.getSdg(), copy.getSdg());

        assertEquals(1, copy.getSites().size());
        assertEquals(1, copy.getLogFrames().size());
        assertEquals(1, copy.getPersonnel().size());
        assertEquals(1, copy.getBudget().size());
        assertEquals(1, copy.getOtherProjects().size());
        assertEquals(1, copy.getPriorityAgendas().size());
        assertEquals(1, copy.getLimitations().size());

        // Every child row is a brand-new instance bound to the duplicate.
        assertNotSame(source.getSites().get(0), copy.getSites().get(0));
        assertSame(copy, copy.getSites().get(0).getProposal());
        assertNotSame(source.getBudget().get(0), copy.getBudget().get(0));
        assertSame(copy, copy.getBudget().get(0).getProposal());
        assertEquals("6000", copy.getBudget().get(0).getTotal());
        assertEquals("kalikasan", copy.getPriorityAgendas().get(0).getAgendaKey());
    }

    @Test
    void editingTheDuplicateNeverModifiesTheOriginalProposal() {
        authenticate(7L, "PROPONENT");
        Proposal source = rejectedProposal(1L, 7L);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));

        Proposal copy = service.duplicateProposal(1L, CALL_ID);

        // Edit the duplicate the way the proposal form would.
        copy.setProjectTitle("A Completely Different Project");
        copy.getSites().clear();
        copy.getSites().add(new ProposalSite(null, copy, "New", "New", "New", "New", "New", "New"));

        assertEquals("Community-Based Water Filtration System", source.getProjectTitle());
        assertEquals(1, source.getSites().size());
        assertEquals("Naawan", source.getSites().get(0).getMunicipality());
    }

    @Test
    void workflowStateOfTheOriginalIsNotCopied() {
        authenticate(7L, "PROPONENT");
        Proposal source = rejectedProposal(1L, 7L);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));

        Proposal copy = service.duplicateProposal(1L, CALL_ID);

        assertNull(copy.getRemarks());
        assertNull(copy.getReturnRemarks());
        assertNull(copy.getReturnedByOffice());
        assertNull(copy.getReturnedByName());
        assertNull(copy.getReturnedByUserId());
        assertNull(copy.getReturnedAt());
        assertNull(copy.getRevisionDeadline());
        assertNull(copy.getRevisionForwardedAt());
        assertNull(copy.getRevisionNotes());
        assertNull(copy.getSoNumber());
        assertNull(copy.getSoFileName());
        assertNull(copy.getNeedsBudget());
        assertNull(copy.getReviewedBy());
        assertNull(copy.getReviewedByPosition());
        // A duplicate is a draft that belongs to the call the proponent selected in the
        // picker - never to the original's own call (3L in this fixture).
        assertEquals(CALL_ID, copy.getProposalAnnouncementId());
    }

    @Test
    void aCopyWithoutASelectedCallIsRejectedAndCreatesNothing() {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(rejectedProposal(1L, 7L)));

        assertThrows(IllegalArgumentException.class, () -> service.duplicateProposal(1L, null));

        // No identifier is allocated and nothing is stored before a call is selected.
        verify(proposalRepository, never()).save(any(Proposal.class));
        verify(proposalRepository, never()).nextProposalCodeSequence();
    }

    @Test
    void aCopyIsRejectedWhenTheSelectedCallIsNoLongerOpen() {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(rejectedProposal(1L, 7L)));
        when(announcementService.requireOpenAnnouncement(9L)).thenThrow(new IllegalStateException(
                "The selected Call for Proposal is no longer open for submissions. "
                        + "Please select an open call for proposal."));

        assertThrows(IllegalStateException.class, () -> service.duplicateProposal(1L, 9L));

        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void theCopyKeepsTheSelectedCallAndTheOriginalKeepsItsOwn() {
        authenticate(7L, "PROPONENT");
        Proposal source = rejectedProposal(1L, 7L);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));

        Proposal copy = service.duplicateProposal(1L, CALL_ID);

        assertEquals(CALL_ID, copy.getProposalAnnouncementId());
        assertEquals(3L, source.getProposalAnnouncementId(),
                "the original proposal keeps its own call");
    }

    @Test
    void duplicateDoesNotRecordHistoryOrNotifyAnyone() {
        authenticate(7L, "PROPONENT");
        Proposal source = rejectedProposal(1L, 7L);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));

        service.duplicateProposal(1L, CALL_ID);

        verify(versionService, never()).recordSubmission(any(), any(Boolean.class), any());
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
        verify(notificationService, never()).createNotification(any(), any());
    }

    @Test
    void proponentCannotDuplicateAnotherUsersProposal() {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(rejectedProposal(1L, 8L)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.duplicateProposal(1L, CALL_ID));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void rejectedProposalsCanBeCopied() {
        authenticate(7L, "PROPONENT");
        Proposal source = rejectedProposal(1L, 7L);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));

        Proposal copy = service.duplicateProposal(1L, CALL_ID);

        assertEquals("DRAFT", copy.getStatus());
        assertSame(source.getProponent(), copy.getProponent());
    }

    @Test
    void approvedProposalsCanBeCopied() {
        authenticate(7L, "PROPONENT");
        Proposal source = rejectedProposal(1L, 7L);
        source.setStatus("APPROVED");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));

        Proposal copy = service.duplicateProposal(1L, CALL_ID);

        // The copy is a draft: the approval itself is not carried over.
        assertEquals("DRAFT", copy.getStatus());
        assertNull(copy.getSoNumber());
    }

    @Test
    void returnedForRevisionProposalsCanBeCopied() {
        authenticate(7L, "PROPONENT");
        Proposal source = rejectedProposal(1L, 7L);
        source.setStatus("RPS_RETURNED");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));

        Proposal copy = service.duplicateProposal(1L, CALL_ID);

        assertEquals("DRAFT", copy.getStatus());
        assertNull(copy.getReturnRemarks());
    }

    @Test
    void everyStatusCanBeCopied() {
        authenticate(7L, "PROPONENT");
        for (String status : List.of("DRAFT", "SUBMITTED", "UNDER_REVIEW", "RPS_REVIEW",
                "RPS_RETURNED", "OVCRIGE_COORDINATION", "REC_EVALUATION", "CHANCELLOR_REVIEW",
                "FINANCE_AUTHORIZATION", "APPROVED", "REJECTED", "REVISION", "RETURNED_TO_RPS",
                "RELEASED", "IMPLEMENTATION", "COMPLETED")) {
            Proposal source = rejectedProposal(1L, 7L);
            source.setStatus(status);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));

            Proposal copy = service.duplicateProposal(1L, CALL_ID);

            assertEquals("DRAFT", copy.getStatus(), status + " must be copyable");
            assertSame(source.getProponent(), copy.getProponent());
        }
    }

    @Test
    void uploadedAttachmentsAreNotCarriedOver() {
        authenticate(7L, "PROPONENT");
        Proposal source = rejectedProposal(1L, 7L);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));

        Proposal copy = service.duplicateProposal(1L, CALL_ID);

        // The uploaded files belong to the original proposal, so the copy must not
        // point at them; the proponent re-uploads them before submitting.
        assertNull(copy.getReviewFileName());
        assertNull(copy.getRoadmapFileName());
        assertNull(copy.getBeneficiariesFileName());
        assertNull(copy.getGadFileName());
    }

    @Test
    void proposalWithoutAProponentCannotBeDuplicated() {
        authenticate(2L, "RPS_STAFF");
        Proposal source = rejectedProposal(1L, 7L);
        source.setProponent(null);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));

        assertThrows(IllegalStateException.class, () -> service.duplicateProposal(1L, CALL_ID));
    }

    @Test
    void aFailedSavePropagatesSoNoPartialDuplicateIsLeftBehind() {
        authenticate(7L, "PROPONENT");
        Proposal source = rejectedProposal(1L, 7L);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));
        when(proposalRepository.save(any(Proposal.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(DataIntegrityViolationException.class, () -> service.duplicateProposal(1L, CALL_ID));
    }
}
