package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.model.Proposal;
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
 * Section 24 (Personnel Requirement) - the "% Time Devoted" column.
 *
 * <p>The column holds a percentage: the form only accepts a number and shows the % symbol,
 * and the backend normalizes every value before it is written, so a legacy "50 %" becomes
 * "50" (the symbol can never be stored twice) and a value outside 0-100 is capped. A value
 * that is not a number at all is refused with the row number while the proposal is being
 * submitted, and kept as it is on a draft, so saving an older draft can never fail.</p>
 */
class ProposalPersonnelTimeDevotedTest {

    private static final Long PROPONENT_ID = 13L;

    private ProposalService service;

    @BeforeEach
    void setUp() {
        ProposalRepository proposalRepository = mock(ProposalRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ProposalAnnouncementService announcementService = mock(ProposalAnnouncementService.class);
        service = new ProposalService(proposalRepository, userRepository,
                mock(NotificationService.class), announcementService,
                mock(ProposalVersionService.class), mock(ProposalSectionRemarkService.class),
                new CurrentUser(), mock(CooperatingAgencyService.class), mock(PriorityAgendaService.class),
                new LocalUploadStorage("uploads"));

        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(proposalRepository.nextProposalCodeSequence()).thenReturn(40L);
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
        user.setName("Personnel Proponent");
        user.setRole("PROPONENT");
        return user;
    }

    private ProposalRequest.PersonnelDTO personnel(String position, String time, String responsibilities) {
        ProposalRequest.PersonnelDTO dto = new ProposalRequest.PersonnelDTO();
        dto.setPosition(position);
        dto.setTime(time);
        dto.setResponsibilities(responsibilities);
        return dto;
    }

    private ProposalRequest draftWithPersonnel(List<ProposalRequest.PersonnelDTO> personnel) {
        ProposalRequest request = new ProposalRequest();
        request.setProjectTitle("Personnel time devoted");
        request.setDuration("12");
        request.setStatus("DRAFT");
        request.setPersonnel(personnel);
        return request;
    }

    // ===================== the normalization itself =====================

    @Test
    void aPlainNumberIsStoredWithoutThePercentageSymbol() {
        assertEquals("25", ProposalService.normalizeTimeDevoted("25", 1, true));
        assertEquals("50", ProposalService.normalizeTimeDevoted("50", 1, true));
        assertEquals("100", ProposalService.normalizeTimeDevoted("100", 1, true));
        assertEquals("0", ProposalService.normalizeTimeDevoted("0", 1, true));
    }

    @Test
    void aLegacyValueWithTheSymbolOrSpacingBecomesANumber() {
        assertEquals("50", ProposalService.normalizeTimeDevoted("50%", 1, true));
        assertEquals("50", ProposalService.normalizeTimeDevoted("50 %", 1, true));
        assertEquals("75", ProposalService.normalizeTimeDevoted(" 75 ", 1, true));
        assertEquals("12.5", ProposalService.normalizeTimeDevoted("12.5", 1, true));
        assertEquals("12.5", ProposalService.normalizeTimeDevoted("12.50", 1, true),
                "trailing zeros are not stored");
        assertEquals("50", ProposalService.normalizeTimeDevoted("50.", 1, true),
                "a value that was still being typed is the number 50");
    }

    @Test
    void theSymbolIsNeverStoredTwice() {
        String once = ProposalService.normalizeTimeDevoted("50%", 1, true);
        String twice = ProposalService.normalizeTimeDevoted(once, 1, true);
        assertEquals("50", once);
        assertEquals(once, twice);
        assertTrue(!twice.contains("%"), "no symbol in the stored value");
    }

    @Test
    void aValueOutsideTheRangeIsCapped() {
        assertEquals("100", ProposalService.normalizeTimeDevoted("150", 1, true));
        assertEquals("0", ProposalService.normalizeTimeDevoted("-25", 1, true));
        assertEquals("0", ProposalService.normalizeTimeDevoted("-0.5", 1, true));
    }

    @Test
    void aBlankValueStaysBlank() {
        assertNull(ProposalService.normalizeTimeDevoted(null, 1, true));
        assertEquals("", ProposalService.normalizeTimeDevoted("", 1, true));
        assertEquals("   ", ProposalService.normalizeTimeDevoted("   ", 1, true));
    }

    @Test
    void aValueThatIsNotANumberIsRefusedOnSubmissionWithItsRowNumber() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ProposalService.normalizeTimeDevoted("fifty", 2, true));
        assertTrue(error.getMessage().contains("Personnel entry #2"), error.getMessage());
        assertTrue(error.getMessage().contains("% Time Devoted"), error.getMessage());
    }

    @Test
    void aValueThatIsNotANumberIsKeptOnADraftSoAnOldRecordStillSaves() {
        assertEquals("fifty", ProposalService.normalizeTimeDevoted("fifty", 2, false));
    }

    // ===================== the write path =====================

    @Test
    void theSavedPersonnelRowCarriesTheNormalizedNumber() {
        Proposal saved = service.saveProposal(
                draftWithPersonnel(List.of(personnel("Research Assistant", "50 %", "Data collection"))), null);

        assertEquals(1, saved.getPersonnel().size());
        assertEquals("50", saved.getPersonnel().get(0).getTimeDevoted());
        assertEquals("Research Assistant", saved.getPersonnel().get(0).getPosition());
        assertEquals("Data collection", saved.getPersonnel().get(0).getResponsibilities());
    }

    @Test
    void everyRowOfADraftKeepsItsOwnValue() {
        Proposal saved = service.saveProposal(draftWithPersonnel(List.of(
                personnel("Project Leader", "25", "Lead the project"),
                personnel("Encoder", "100%", "Encode the data"))), null);

        assertEquals("25", saved.getPersonnel().get(0).getTimeDevoted());
        assertEquals("100", saved.getPersonnel().get(1).getTimeDevoted());
    }

    @Test
    void aDecimalPercentageSurvivesTheSave() {
        Proposal saved = service.saveProposal(
                draftWithPersonnel(List.of(personnel("Statistician", "12.5", "Analysis"))), null);

        assertEquals("12.5", saved.getPersonnel().get(0).getTimeDevoted());
    }
}
