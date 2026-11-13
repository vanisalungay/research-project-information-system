package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.RiskManagementEntry;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers section 21 (Risk Management Plan) now that the proponent fills it in as a
 * structured table instead of one big text field.
 *
 * <p>What is asserted here is the persistence contract the form and every review page
 * rely on: several entries are stored per proposal with their four fields kept separate,
 * the entries survive a draft save, a request that does not edit the section can never
 * wipe what is already stored, and a proposal whose plan is only the legacy hand-written
 * text keeps that text.</p>
 */
class RiskManagementPlanSaveTest {

    private static final Long PROPONENT_ID = 11L;
    private static final Long PROPOSAL_ID = 77L;

    private ProposalRepository proposalRepository;
    private ProposalService service;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ProposalAnnouncementService announcementService = mock(ProposalAnnouncementService.class);
        service = new ProposalService(proposalRepository, userRepository,
                mock(NotificationService.class), announcementService,
                mock(ProposalVersionService.class), mock(ProposalSectionRemarkService.class),
                new CurrentUser(), mock(CooperatingAgencyService.class), mock(PriorityAgendaService.class),
                new LocalUploadStorage("uploads"));

        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(proposalRepository.nextProposalCodeSequence()).thenReturn(30L);
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
        user.setName("Risk Proponent");
        user.setRole("PROPONENT");
        return user;
    }

    private ProposalRequest request() {
        ProposalRequest request = new ProposalRequest();
        request.setProjectTitle("Structured risk plan");
        request.setDuration("12");
        request.setStatus("DRAFT");
        return request;
    }

    private static ProposalRequest.RiskManagementDTO row(String identifiedRisks, String likelihood,
            String impact, String mitigation) {
        ProposalRequest.RiskManagementDTO dto = new ProposalRequest.RiskManagementDTO();
        dto.setIdentifiedRisks(identifiedRisks);
        dto.setLikelihood(likelihood);
        dto.setImpact(impact);
        dto.setMitigationMeasure(mitigation);
        return dto;
    }

    private Proposal existingDraft() {
        Proposal proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        proposal.setProponent(proponent());
        proposal.setStatus("DRAFT");
        when(proposalRepository.findByIdForUpdate(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        return proposal;
    }

    @Test
    void everyEntryIsStoredWithItsFourFieldsSeparate() {
        ProposalRequest request = request();
        request.setRiskManagement(List.of(
                row("Delayed delivery of research equipment", "High", "Medium",
                        "Identify alternative suppliers and coordinate procurement in advance."),
                row("Low participation of fisherfolk", "Medium", "High",
                        "Hold community assemblies with local government support.")));

        Proposal saved = service.saveProposal(request, null);

        List<RiskManagementEntry> entries = saved.getRiskManagement();
        assertEquals(2, entries.size(), "both entries are kept");
        assertEquals("Delayed delivery of research equipment", entries.get(0).getIdentifiedRisks());
        assertEquals("High", entries.get(0).getLikelihood());
        assertEquals("Medium", entries.get(0).getImpact());
        assertEquals("Identify alternative suppliers and coordinate procurement in advance.",
                entries.get(0).getMitigationMeasure());
        assertEquals("Low participation of fisherfolk", entries.get(1).getIdentifiedRisks());
        assertEquals("Medium", entries.get(1).getLikelihood());
        assertEquals("High", entries.get(1).getImpact());
    }

    @Test
    void theLegacyTextColumnCarriesTheReadableRenderingOfTheEntries() {
        ProposalRequest request = request();
        request.setRiskManagement(List.of(
                row("Delayed delivery of research equipment", "High", "Medium", "Identify alternative suppliers."),
                row("Low participation", "Low", "High", "Hold community assemblies.")));

        String text = service.saveProposal(request, null).getRisks();

        assertTrue(text.startsWith("1. Delayed delivery of research equipment (Likelihood: High, Impact: Medium)"),
                "the first entry is rendered with its levels: " + text);
        assertTrue(text.contains("Mitigation: Identify alternative suppliers."), "and its mitigation measure");
        assertTrue(text.contains("2. Low participation (Likelihood: Low, Impact: High)"),
                "the second entry follows on its own line");
    }

    @Test
    void aRequestWithoutTheSectionLeavesTheStoredEntriesUntouched() {
        Proposal draft = existingDraft();
        draft.setRiskManagement(new ArrayList<>(List.of(
                new RiskManagementEntry("Existing risk", "High", "Low", "Existing mitigation"))));
        draft.setRisks("1. Existing risk (Likelihood: High, Impact: Low)\n   Mitigation: Existing mitigation");

        // A client that does not edit section 21 sends neither the table nor a new text.
        Proposal saved = service.saveProposal(request(), PROPOSAL_ID);

        assertEquals(1, saved.getRiskManagement().size(), "the stored entry survives");
        assertEquals("Existing risk", saved.getRiskManagement().get(0).getIdentifiedRisks());
        assertEquals("1. Existing risk (Likelihood: High, Impact: Low)\n   Mitigation: Existing mitigation",
                saved.getRisks());
    }

    @Test
    void editingReplacesTheEntriesWithoutLosingTheOtherRows() {
        Proposal draft = existingDraft();
        draft.setRiskManagement(new ArrayList<>(List.of(
                new RiskManagementEntry("First risk", "High", "High", "First mitigation"))));

        ProposalRequest request = request();
        request.setRiskManagement(List.of(
                row("First risk", "High", "High", "First mitigation"),
                row("Second risk", "Low", "Medium", "Second mitigation")));

        Proposal saved = service.saveProposal(request, PROPOSAL_ID);

        assertEquals(2, saved.getRiskManagement().size(), "the new set of rows replaces the old one");
        assertEquals("Second risk", saved.getRiskManagement().get(1).getIdentifiedRisks());
    }


    @Test
    void aLegacyOnlyRequestKeepsItsTextAndWritesNoEntries() {
        // A client that predates the table (or an old proposal being resubmitted) sends
        // only the single text field.
        ProposalRequest request = request();
        request.setRisks("Risks, assumptions and mitigation strategies in prose.");

        Proposal saved = service.saveProposal(request, null);

        assertEquals("Risks, assumptions and mitigation strategies in prose.", saved.getRisks());
        assertTrue(saved.getRiskManagement().isEmpty(), "no row is invented from hand-written text");
    }

    @Test
    void anEmptyRowIsNotStoredAsAnEntry() {
        ProposalRequest request = request();
        request.setRiskManagement(List.of(
                row("", "", "", ""),
                row("   ", "   ", "   ", "   "),
                row("Delayed delivery", "High", "Medium", "Identify alternative suppliers.")));

        Proposal saved = service.saveProposal(request, null);

        assertEquals(1, saved.getRiskManagement().size(), "the seeded empty row carries no information");
        assertEquals("Delayed delivery", saved.getRiskManagement().get(0).getIdentifiedRisks());
    }

    @Test
    void aPartialDraftRowIsKeptSoNothingTypedInIsLost() {
        ProposalRequest request = request();
        request.setRiskManagement(List.of(row("Delayed delivery", "High", null, "")));

        Proposal saved = service.saveProposal(request, null);

        assertEquals(1, saved.getRiskManagement().size());
        assertEquals("High", saved.getRiskManagement().get(0).getLikelihood());
        assertNull(saved.getRiskManagement().get(0).getImpact());
    }

    @Test
    void anUnsupportedLevelIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateRiskManagement(List.of(
                        row("Delayed delivery", "Very high", "Medium", "Mitigate")), true));
        assertTrue(error.getMessage().contains("Risk entry #1"), error.getMessage());
        assertTrue(error.getMessage().contains("must be High, Medium or Low"), error.getMessage());
    }

    @Test
    void theLevelsAreMatchedCaseInsensitivelyAndStoredCanonically() {
        assertEquals("High", ProposalService.normalizeRiskLevel("high", 0, "likelihood"));
        assertEquals("Medium", ProposalService.normalizeRiskLevel("MEDIUM", 0, "likelihood"));
        assertEquals("Low", ProposalService.normalizeRiskLevel("  Low  ", 0, "impact"));
        assertNull(ProposalService.normalizeRiskLevel(null, 0, "impact"));
    }

    @Test
    void aSubmittedRowMustBeComplete() {
        // Whitespace-only text is empty, so this row is missing its identified risk.
        IllegalArgumentException identified = assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateRiskManagement(List.of(
                        row("   ", "High", "Medium", "Mitigate")), true));
        assertTrue(identified.getMessage().contains("the identified risk is required"), identified.getMessage());

        IllegalArgumentException likelihood = assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateRiskManagement(List.of(
                        row("Delayed delivery", null, "Medium", "Mitigate")), true));
        assertTrue(likelihood.getMessage().contains("the likelihood is required"), likelihood.getMessage());

        IllegalArgumentException impact = assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateRiskManagement(List.of(
                        row("Delayed delivery", "High", "", "Mitigate")), true));
        assertTrue(impact.getMessage().contains("the impact is required"), impact.getMessage());

        IllegalArgumentException mitigation = assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateRiskManagement(List.of(
                        row("Delayed delivery", "High", "Medium", "  ")), true));
        assertTrue(mitigation.getMessage().contains("the mitigation measure is required"),
                mitigation.getMessage());
    }

    @Test
    void theSecondRowIsIdentifiedInTheMessageWhenOnlyItIsIncomplete() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ProposalService.validateRiskManagement(List.of(
                        row("Delayed delivery", "High", "Medium", "Mitigate"),
                        row("Low participation", "Low", "High", "")), true));
        assertTrue(error.getMessage().startsWith("Risk entry #2"), error.getMessage());
    }
}

