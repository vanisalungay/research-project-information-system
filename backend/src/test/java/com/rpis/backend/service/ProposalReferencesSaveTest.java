package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the persistence of section 23 (Literature Cited): the plain-text rendering of
 * the references and the structured entries of the APA reference builder must be stored
 * on the proposal, and a request that does not send them must never wipe what is already
 * there.
 */
class ProposalReferencesSaveTest {

    private static final Long PROPONENT_ID = 7L;
    private static final Long PROPOSAL_ID = 42L;

    private ProposalRepository proposalRepository;
    private UserRepository userRepository;
    private ProposalAnnouncementService announcementService;
    private ProposalService service;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        userRepository = mock(UserRepository.class);
        announcementService = mock(ProposalAnnouncementService.class);
        service = new ProposalService(proposalRepository, userRepository,
                mock(NotificationService.class), announcementService,
                mock(ProposalVersionService.class), mock(ProposalSectionRemarkService.class),
                new CurrentUser(), mock(CooperatingAgencyService.class), mock(PriorityAgendaService.class));

        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(proposalRepository.nextProposalCodeSequence()).thenReturn(22L);
        when(userRepository.findById(PROPONENT_ID)).thenReturn(Optional.of(proponent()));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                PROPONENT_ID, null, List.of(new SimpleGrantedAuthority("ROLE_PROPONENT"))));

        // A new proposal is only accepted while a call for proposal is active.
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
        user.setName("Literature Proponent");
        user.setRole("PROPONENT");
        return user;
    }

    private ProposalRequest request() {
        ProposalRequest request = new ProposalRequest();
        request.setProjectTitle("Literature cited");
        request.setDuration("12");
        request.setStatus("DRAFT");
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

    @Test
    void theReferencesTextAndTheStructuredEntriesAreStored() {
        ProposalRequest request = request();
        request.setReferencesText("Dela Cruz, J. P. (2020). Coral cover.\n\nSantos, M. L. (2018). Reef fish.");
        request.setReferencesJson(
                "[{\"type\":\"journal\",\"fields\":{\"title\":\"Coral cover\"},\"text\":\"Dela Cruz, J. P. (2020). Coral cover.\"}]");

        Proposal saved = service.saveProposal(request, null);

        assertEquals("Dela Cruz, J. P. (2020). Coral cover.\n\nSantos, M. L. (2018). Reef fish.",
                saved.getReferencesText());
        assertEquals("[{\"type\":\"journal\",\"fields\":{\"title\":\"Coral cover\"},"
                + "\"text\":\"Dela Cruz, J. P. (2020). Coral cover.\"}]", saved.getReferencesJson());
    }

    @Test
    void aRequestWithoutTheSectionLeavesTheStoredReferencesUntouched() {
        Proposal draft = existingDraft();
        draft.setReferencesText("Existing reference.");
        draft.setReferencesJson("[{\"type\":\"manual\",\"fields\":{\"text\":\"Existing reference.\"},"
                + "\"text\":\"Existing reference.\"}]");

        // A client that does not edit section 23 sends neither field (null).
        Proposal saved = service.saveProposal(request(), PROPOSAL_ID);

        assertEquals("Existing reference.", saved.getReferencesText());
        assertEquals(draft.getReferencesJson(), saved.getReferencesJson());
    }

    @Test
    void anEmptyStringClearsTheSectionWhenTheProponentRemovedEveryReference() {
        Proposal draft = existingDraft();
        draft.setReferencesText("Old reference.");

        ProposalRequest request = request();
        request.setReferencesText("");
        request.setReferencesJson("");

        Proposal saved = service.saveProposal(request, PROPOSAL_ID);

        assertEquals("", saved.getReferencesText());
        assertEquals("", saved.getReferencesJson());
    }

    @Test
    void theLegacyStructuredListIsStillFormattedAsText() {
        // A client that predates the builder sends only author/title/year.
        ProposalRequest request = request();
        ProposalRequest.ReferenceDTO reference = new ProposalRequest.ReferenceDTO();
        reference.setAuthor("Dela Cruz, J. P.");
        reference.setYear("2020");
        reference.setTitle("Coral cover");
        request.setReferences(List.of(reference));

        Proposal saved = service.saveProposal(request, null);

        // The legacy path is left exactly as it was (it keeps its own punctuation),
        // and it never writes the structured entries.
        assertEquals("Dela Cruz, J. P.. (2020). Coral cover.", saved.getReferencesText());
        assertNull(saved.getReferencesJson());
    }
}
