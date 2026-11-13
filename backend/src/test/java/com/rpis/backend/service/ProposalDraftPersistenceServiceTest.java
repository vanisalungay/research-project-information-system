package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.model.GadChecklistData;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalLogFrame;
import com.rpis.backend.model.ProposalPersonnel;
import com.rpis.backend.model.ProposalSite;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.storage.LocalUploadStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Draft-save persistence: everything the Proponent enters must survive a save and be
 * returned unchanged on reopen, and a save that does not mention a section must never
 * delete what is already stored.
 *
 * <p>The rules covered here are the ones the create-proposal workflow depends on:</p>
 * <ul>
 *   <li>the interactive GAD Checklist 2 is stored with its responses and comments, its
 *       total/interpretation/completion are recalculated by the server, and the total is
 *       mirrored onto the legacy {@code gadScore};</li>
 *   <li>an incomplete checklist is fine for a draft but blocks a final submission;</li>
 *   <li>an omitted document name preserves the stored reference, an explicit {@code ""}
 *       clears it (the revision form's Remove action);</li>
 *   <li>an empty checklist does not zero a legacy GAD score, while clearing a previously
 *       stored checklist does;</li>
 *   <li>omitted child lists (sites, log frames, personnel, budget, other projects) are
 *       preserved, and sent lists replace them;</li>
 *   <li>the "Submitted By" / "Endorsed By" certification fields persist and are preserved
 *       by a later partial save.</li>
 * </ul>
 */
class ProposalDraftPersistenceServiceTest {

    private static final Long PROPONENT_ID = 11L;
    private static final Long PROPOSAL_ID = 77L;

    private static final String GAD_INCOMPLETE_MESSAGE =
            "Please select No, Partly or Yes for every GAD assessment item before submitting the proposal.";

    /** The 17 assessment items of GadChecklistScoring, in document order. */
    private static final List<String> GAD_ITEMS = List.of(
            "1.1", "1.2", "2.0", "3.1", "3.2", "4.0", "5.0",
            "6.1", "6.2", "6.3", "7.0", "8.0", "9.1", "9.2", "10.1", "10.2", "10.3");

    private ProposalRepository proposalRepository;
    private UserRepository userRepository;
    private ProposalVersionService versionService;
    private ProposalService service;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        userRepository = mock(UserRepository.class);
        ProposalAnnouncementService announcementService = mock(ProposalAnnouncementService.class);
        versionService = mock(ProposalVersionService.class);
        service = new ProposalService(proposalRepository, userRepository,
                mock(NotificationService.class), announcementService, versionService,
                mock(ProposalSectionRemarkService.class), new CurrentUser(),
                mock(CooperatingAgencyService.class), mock(PriorityAgendaService.class),
                new LocalUploadStorage("uploads"));

        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(proposalRepository.nextProposalCodeSequence()).thenReturn(31L);
        when(userRepository.findById(PROPONENT_ID)).thenReturn(Optional.of(proponent()));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                PROPONENT_ID, null, List.of(new SimpleGrantedAuthority("ROLE_PROPONENT"))));

        com.rpis.backend.model.ProposalAnnouncement announcement =
                new com.rpis.backend.model.ProposalAnnouncement();
        announcement.setId(1L);
        announcement.setStatus(com.rpis.backend.model.ProposalAnnouncement.STATUS_ACTIVE);
        announcement.setStartDate(java.time.LocalDate.now().minusDays(1));
        announcement.setEndDate(java.time.LocalDate.now().plusDays(5));
        when(announcementService.getActiveAnnouncementOrNull()).thenReturn(announcement);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User proponent() {
        User user = new User();
        user.setId(PROPONENT_ID);
        user.setName("Draft Proponent");
        user.setRole("PROPONENT");
        return user;
    }

    private ProposalRequest request(String status) {
        ProposalRequest request = new ProposalRequest();
        request.setProjectTitle("Draft persistence");
        request.setDuration("12");
        request.setStatus(status);
        return request;
    }

    private Proposal existingDraft() {
        Proposal proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        proposal.setProponent(proponent());
        proposal.setStatus("DRAFT");
        when(proposalRepository.findByIdForUpdate(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        return proposal;
    }

    private GadChecklistData checklist(Map<String, String> responses, Map<String, String> comments) {
        GadChecklistData checklist = new GadChecklistData();
        checklist.setResponses(new LinkedHashMap<>(responses));
        checklist.setComments(new LinkedHashMap<>(comments));
        return checklist;
    }

    private Map<String, String> allAnswered(String response) {
        Map<String, String> responses = new LinkedHashMap<>();
        for (String item : GAD_ITEMS) {
            responses.put(item, response);
        }
        return responses;
    }

    // ===================== GAD checklist =====================

    @Test
    void aDraftStoresTheChecklistResponsesCommentsAndRecalculatedTotal() {
        ProposalRequest request = request("DRAFT");
        request.setGadChecklist(checklist(
                Map.of("1.1", "YES", "1.2", "NO"),
                Map.of("1.1", "Women are consulted.")));

        Proposal saved = service.saveProposal(request, null);

        assertNotNull(saved.getGadChecklist());
        assertEquals("YES", saved.getGadChecklist().getResponses().get("1.1"));
        assertEquals("NO", saved.getGadChecklist().getResponses().get("1.2"));
        assertEquals("Women are consulted.", saved.getGadChecklist().getComments().get("1.1"));
        // Criterion 1: 1.1 YES (1.00) + 1.2 NO (0) = 1.00; every other item unanswered.
        assertEquals(new BigDecimal("1.00"), saved.getGadChecklist().getTotalScore());
        assertEquals(Boolean.FALSE, saved.getGadChecklist().getCompleted());
        assertEquals("", saved.getGadChecklist().getInterpretation());
        // The legacy column follows the checklist so older screens keep working.
        assertEquals(new BigDecimal("1.00"), saved.getGadScore());
    }

    @Test
    void aCompleteChecklistInterpretsTheScore() {
        ProposalRequest request = request("DRAFT");
        request.setGadChecklist(checklist(allAnswered("YES"), Map.of()));

        Proposal saved = service.saveProposal(request, null);

        assertEquals(new BigDecimal("20.00"), saved.getGadChecklist().getTotalScore());
        assertEquals(Boolean.TRUE, saved.getGadChecklist().getCompleted());
        assertEquals("Proposed project is gender-responsive",
                saved.getGadChecklist().getInterpretation());
        assertEquals(new BigDecimal("20.00"), saved.getGadScore());
    }

    @Test
    void anIncompleteChecklistSavesAsADraft() {
        ProposalRequest request = request("DRAFT");
        request.setGadChecklist(checklist(Map.of("1.1", "PARTLY"), Map.of()));

        assertNotNull(service.saveProposal(request, null).getGadChecklist());
    }

    @Test
    void anIncompleteChecklistBlocksAFinalSubmission() {
        ProposalRequest request = request("SUBMITTED");
        request.setGadChecklist(checklist(Map.of("1.1", "PARTLY"), Map.of()));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveProposal(request, null));
        assertEquals(GAD_INCOMPLETE_MESSAGE, error.getMessage());
    }

    @Test
    void aCompleteChecklistIsAcceptedForAFinalSubmission() {
        ProposalRequest request = request("SUBMITTED");
        request.setGadChecklist(checklist(allAnswered("YES"), Map.of()));

        Proposal saved = service.saveProposal(request, null);

        assertEquals("FOR REVIEW", saved.getStatus());
        assertEquals(Boolean.TRUE, saved.getGadChecklist().getCompleted());
    }

    @Test
    void aForReviewRequestIsStoredAsForReviewAndRecordedInHistory() {
        when(versionService.isSubmissionStatus("FOR REVIEW")).thenReturn(true);
        ProposalRequest request = request("FOR REVIEW");
        request.setGadChecklist(checklist(allAnswered("YES"), Map.of()));

        Proposal saved = service.saveProposal(request, null);

        assertEquals("FOR REVIEW", saved.getStatus());
        verify(versionService).recordSubmission(eq(saved), eq(false), any());
    }

    @Test
    void aDraftRequestStaysADraft() {
        assertEquals("DRAFT", service.saveProposal(request("DRAFT"), null).getStatus());
    }

    @Test
    void anUnknownStatusIsRejectedBeforeAnythingIsSaved() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveProposal(request("NOT_A_PROPOSAL_STATUS"), null));

        assertTrue(error.getMessage().contains("NOT_A_PROPOSAL_STATUS"), error.getMessage());
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void aSubmissionWithoutChecklistOrLegacyDocumentIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveProposal(request("SUBMITTED"), null));
        assertEquals(GAD_INCOMPLETE_MESSAGE, error.getMessage());
    }

    @Test
    void aStoredChecklistLetsALaterSubmissionPassWithoutResendingIt() {
        Proposal draft = existingDraft();
        GadChecklistData stored = checklist(allAnswered("YES"), Map.of());
        GadChecklistScoring.recalculate(stored);
        draft.setGadChecklist(stored);

        Proposal saved = service.saveProposal(request("SUBMITTED"), PROPOSAL_ID);

        assertEquals("FOR REVIEW", saved.getStatus());
        assertSame(stored, saved.getGadChecklist());
    }

    @Test
    void aLegacyGadDocumentStillSatisfiesTheSubmissionRequirement() {
        Proposal draft = existingDraft();
        draft.setGadFileName("gad-score.pdf");

        Proposal saved = service.saveProposal(request("SUBMITTED"), PROPOSAL_ID);

        assertEquals("FOR REVIEW", saved.getStatus());
        assertEquals("gad-score.pdf", saved.getGadFileName());
    }

    @Test
    void aRevisionResubmissionIsExemptFromTheChecklistRequirement() {
        Proposal draft = existingDraft();
        draft.setStatus("RPS_RETURNED");
        when(versionService.isRevisionStatus(anyString())).thenReturn(true);

        Proposal saved = service.saveProposal(request("SUBMITTED"), PROPOSAL_ID);

        assertEquals("FOR REVIEW", saved.getStatus());
    }

    @Test
    void anEmptyChecklistDoesNotZeroALegacyGadScore() {
        Proposal draft = existingDraft();
        draft.setGadScore(new BigDecimal("15.00"));

        ProposalRequest request = request("DRAFT");
        request.setGadChecklist(checklist(Map.of(), Map.of()));

        Proposal saved = service.saveProposal(request, PROPOSAL_ID);

        assertNotNull(saved.getGadChecklist(), "the draft checklist itself is still stored");
        assertEquals(new BigDecimal("15.00"), saved.getGadScore(),
                "a legacy score survives while no answer was ever entered");
    }

    @Test
    void clearingAStoredChecklistZeroesTheScore() {
        Proposal draft = existingDraft();
        GadChecklistData stored = checklist(allAnswered("YES"), Map.of());
        GadChecklistScoring.recalculate(stored);
        draft.setGadChecklist(stored);
        draft.setGadScore(new BigDecimal("20.00"));

        ProposalRequest request = request("DRAFT");
        request.setGadChecklist(checklist(Map.of(), Map.of()));

        Proposal saved = service.saveProposal(request, PROPOSAL_ID);

        assertEquals(new BigDecimal("0.00"), saved.getGadScore(),
                "the proponent explicitly cleared every answer");
    }

    // ===================== document names =====================

    @Test
    void omittedDocumentNamesPreserveTheStoredReferences() {
        Proposal draft = existingDraft();
        draft.setReviewFileName("review.pdf");
        draft.setRoadmapFileName("roadmap.png");
        draft.setBeneficiariesFileName("budget.xlsx");
        draft.setGadFileName("gad.pdf");

        Proposal saved = service.saveProposal(request("DRAFT"), PROPOSAL_ID);

        assertEquals("review.pdf", saved.getReviewFileName());
        assertEquals("roadmap.png", saved.getRoadmapFileName());
        assertEquals("budget.xlsx", saved.getBeneficiariesFileName());
        assertEquals("gad.pdf", saved.getGadFileName());
    }

    @Test
    void anExplicitEmptyStringClearsADocumentName() {
        Proposal draft = existingDraft();
        draft.setReviewFileName("review.pdf");
        draft.setRoadmapFileName("roadmap.png");
        draft.setBeneficiariesFileName("budget.xlsx");
        draft.setGadFileName("gad.pdf");

        ProposalRequest request = request("DRAFT");
        request.setReviewFileName("");
        request.setRoadmapFileName("");
        request.setBeneficiariesFileName("");
        request.setGadFileName("");

        Proposal saved = service.saveProposal(request, PROPOSAL_ID);

        assertEquals("", saved.getReviewFileName());
        assertEquals("", saved.getRoadmapFileName());
        assertEquals("", saved.getBeneficiariesFileName());
        assertEquals("", saved.getGadFileName());
    }

    // ===================== repeated sections =====================

    @Test
    void omittedChildListsArePreserved() {
        Proposal draft = existingDraft();
        draft.getSites().add(new ProposalSite(null, draft, "Philippines",
                "Caraga", "Agusan del Norte", null, "Butuan City", "Barangay 1"));
        draft.getLogFrames().add(new ProposalLogFrame(null, draft, "Outcome A", "Output A"));
        draft.getPersonnel().add(new ProposalPersonnel(null, draft, "Project Leader", "25", "Lead"));
        draft.getOtherProjects().add(new com.rpis.backend.model.ProposalOtherProject(
                null, draft, "Other Project", "DA", "Lead"));

        Proposal saved = service.saveProposal(request("DRAFT"), PROPOSAL_ID);

        assertEquals(1, saved.getSites().size(), "sites must not be deleted");
        assertEquals(1, saved.getLogFrames().size(), "log frames must not be deleted");
        assertEquals(1, saved.getPersonnel().size(), "personnel must not be deleted");
        assertEquals(1, saved.getOtherProjects().size(), "other projects must not be deleted");
    }

    @Test
    void aSentPersonnelListReplacesTheStoredRowsAndKeepsEveryValue() {
        Proposal draft = existingDraft();
        draft.getPersonnel().add(new ProposalPersonnel(null, draft, "Old role", "10", "Old duties"));

        ProposalRequest.PersonnelDTO first = new ProposalRequest.PersonnelDTO();
        first.setPosition("Project Leader");
        first.setTime("25");
        first.setResponsibilities("Lead the project");
        ProposalRequest.PersonnelDTO second = new ProposalRequest.PersonnelDTO();
        second.setPosition("Encoder");
        second.setTime("50");
        second.setResponsibilities("Encode the data");

        ProposalRequest request = request("DRAFT");
        request.setPersonnel(List.of(first, second));

        Proposal saved = service.saveProposal(request, PROPOSAL_ID);

        assertEquals(2, saved.getPersonnel().size());
        assertEquals("Project Leader", saved.getPersonnel().get(0).getPosition());
        assertEquals("25", saved.getPersonnel().get(0).getTimeDevoted());
        assertEquals("Lead the project", saved.getPersonnel().get(0).getResponsibilities());
        assertEquals("Encoder", saved.getPersonnel().get(1).getPosition());
        assertEquals("50", saved.getPersonnel().get(1).getTimeDevoted());
        assertEquals("Encode the data", saved.getPersonnel().get(1).getResponsibilities());
    }

    @Test
    void aSentLogicalFrameworkReplacesTheStoredRows() {
        Proposal draft = existingDraft();
        draft.getLogFrames().add(new ProposalLogFrame(null, draft, "Old outcome", "Old output"));

        ProposalRequest.LogFrameDTO row = new ProposalRequest.LogFrameDTO();
        row.setOutcome("New outcome");
        row.setOutput("New output");

        ProposalRequest request = request("DRAFT");
        request.setLogFrame(List.of(row));

        Proposal saved = service.saveProposal(request, PROPOSAL_ID);

        assertEquals(1, saved.getLogFrames().size());
        assertEquals("New outcome", saved.getLogFrames().get(0).getOutcome());
        assertEquals("New output", saved.getLogFrames().get(0).getOutput());
    }

    // ===================== certification =====================

    @Test
    void certificationFieldsAreStored() {
        ProposalRequest request = request("DRAFT");
        request.setSubmittedByName("Maria Santos");
        request.setSubmittedByDesignation("Project Leader");
        request.setSubmittedByDate("2026-11-10");
        request.setEndorsedByName("Dean Reyes");
        request.setEndorsedByDesignation("College Dean");
        request.setEndorsedByDate("2026-11-09");

        Proposal saved = service.saveProposal(request, null);

        assertEquals("Maria Santos", saved.getSubmittedByName());
        assertEquals("Project Leader", saved.getSubmittedByDesignation());
        assertEquals("2026-11-10", saved.getSubmittedByDate());
        assertEquals("Dean Reyes", saved.getEndorsedByName());
        assertEquals("College Dean", saved.getEndorsedByDesignation());
        assertEquals("2026-11-09", saved.getEndorsedByDate());
    }

    @Test
    void aPartialSaveDoesNotClearTheCertificationFields() {
        Proposal draft = existingDraft();
        draft.setSubmittedByName("Maria Santos");
        draft.setSubmittedByDesignation("Project Leader");
        draft.setEndorsedByName("Dean Reyes");

        Proposal saved = service.saveProposal(request("DRAFT"), PROPOSAL_ID);

        assertEquals("Maria Santos", saved.getSubmittedByName());
        assertEquals("Project Leader", saved.getSubmittedByDesignation());
        assertEquals("Dean Reyes", saved.getEndorsedByName());
        assertNull(saved.getEndorsedByDesignation());
    }

    @Test
    void anExplicitEmptyStringClearsACertificationField() {
        Proposal draft = existingDraft();
        draft.setSubmittedByName("Maria Santos");

        ProposalRequest request = request("DRAFT");
        request.setSubmittedByName("");

        assertEquals("", service.saveProposal(request, PROPOSAL_ID).getSubmittedByName());
    }

    @Test
    void aSubmissionKeepsTheChecklistAndCertificationTogether() {
        ProposalRequest request = request("SUBMITTED");
        request.setGadChecklist(checklist(allAnswered("YES"), Map.of()));
        request.setSubmittedByName("Maria Santos");
        request.setSubmittedByDesignation("Project Leader");
        request.setSubmittedByDate("2026-11-10");
        request.setEndorsedByName("Dean Reyes");
        request.setEndorsedByDesignation("College Dean");
        request.setEndorsedByDate("2026-11-09");

        Proposal saved = service.saveProposal(request, null);

        assertEquals("FOR REVIEW", saved.getStatus());
        assertTrue(Boolean.TRUE.equals(saved.getGadChecklist().getCompleted()));
        assertFalse(saved.getSubmittedByName().isBlank());
        assertEquals("Dean Reyes", saved.getEndorsedByName());
    }
}
