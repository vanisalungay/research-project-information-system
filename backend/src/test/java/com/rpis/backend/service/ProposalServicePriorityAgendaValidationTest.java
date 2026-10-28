package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalPriorityAgenda;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.rpis.backend.storage.LocalUploadStorage;

/**
 * Section 05 submission rules: a priority-agenda key a client sends is accepted only
 * when it is one of the active framework's active agendas or an agenda the proposal
 * already stores (so a revision of a proposal submitted under an older framework keeps
 * working). Anything else is rejected with a readable error instead of being persisted.
 */
class ProposalServicePriorityAgendaValidationTest {

    private static final Long PROPONENT_ID = 11L;
    private static final Long PROPOSAL_ID = 77L;

    private ProposalRepository proposalRepository;
    private ProposalAnnouncementService announcementService;
    private PriorityAgendaService priorityAgendaService;
    private ProposalService service;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        announcementService = mock(ProposalAnnouncementService.class);
        priorityAgendaService = mock(PriorityAgendaService.class);
        service = new ProposalService(proposalRepository, userRepository,
                mock(NotificationService.class), announcementService,
                mock(ProposalVersionService.class), mock(ProposalSectionRemarkService.class),
                new CurrentUser(), mock(CooperatingAgencyService.class), priorityAgendaService, new LocalUploadStorage("uploads"));

        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(proposalRepository.nextProposalCodeSequence()).thenReturn(1L);
        when(userRepository.findById(PROPONENT_ID)).thenReturn(Optional.of(proponent()));
        // Mirrors the real service: the active keys PLUS the keys the proposal already
        // stores. A fixed set would not exercise the historical-selection rule.
        when(priorityAgendaService.allowedProposalKeys(any())).thenAnswer(invocation -> {
            Collection<String> stored = invocation.getArgument(0);
            Set<String> allowed = new java.util.LinkedHashSet<>(Set.of("dagat", "punla"));
            if (stored != null) {
                allowed.addAll(stored);
            }
            return allowed;
        });

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
        user.setName("Agenda Proponent");
        user.setRole("PROPONENT");
        return user;
    }

    private ProposalRequest request() {
        ProposalRequest request = new ProposalRequest();
        request.setProjectTitle("Priority agenda validation");
        request.setDuration("12");
        request.setStatus("DRAFT");
        return request;
    }

    private ProposalRequest.PriorityAgendaDTO selected(String value) {
        ProposalRequest.PriorityAgendaDTO dto = new ProposalRequest.PriorityAgendaDTO();
        dto.setSelected(true);
        dto.setValue(value);
        return dto;
    }

    private Proposal existingDraft(String... storedKeys) {
        Proposal proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        proposal.setProponent(proponent());
        proposal.setStatus("DRAFT");
        for (String key : storedKeys) {
            proposal.getPriorityAgendas().add(new ProposalPriorityAgenda(null, proposal, key, true, "stored"));
        }
        when(proposalRepository.findByIdForUpdate(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        return proposal;
    }

    @Test
    void anActiveAgendaOfTheConfiguredFrameworkIsAccepted() {
        ProposalRequest request = request();
        request.setPriorityAgenda(Map.of("dagat", selected("Coastal monitoring")));

        Proposal saved = service.saveProposal(request, null);

        assertEquals(1, saved.getPriorityAgendas().size());
        assertEquals("dagat", saved.getPriorityAgendas().get(0).getAgendaKey());
        assertEquals("Coastal monitoring", saved.getPriorityAgendas().get(0).getValue());
    }

    @Test
    void anUnknownOrInactiveAgendaIsRejected() {
        ProposalRequest request = request();
        request.setPriorityAgenda(Map.of("not-a-real-agenda", selected("Sneaky")));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveProposal(request, null));

        assertTrue(error.getMessage().contains("not-a-real-agenda"));
    }

    @Test
    void anAgendaTheProposalAlreadyStoresIsStillAccepted() {
        // The active framework no longer offers "legacy-agenda", but the proposal stores
        // it: a revision must be able to resubmit it unchanged.
        existingDraft("legacy-agenda");
        ProposalRequest request = request();
        request.setPriorityAgenda(Map.of("legacy-agenda", selected("Kept")));

        Proposal saved = service.saveProposal(request, PROPOSAL_ID);

        assertEquals("legacy-agenda", saved.getPriorityAgendas().get(0).getAgendaKey());
        assertEquals("Kept", saved.getPriorityAgendas().get(0).getValue());
    }

    @Test
    void anAgendaOutsideBothTheActiveAndTheStoredKeysIsRejected() {
        existingDraft("legacy-agenda");
        ProposalRequest request = request();
        request.setPriorityAgenda(Map.of("brand-new-agenda", selected("Sneaky")));

        assertThrows(IllegalArgumentException.class, () -> service.saveProposal(request, PROPOSAL_ID));
    }

    @Test
    void theStoredKeysOfTheProposalAreOfferedToTheValidator() {
        existingDraft("legacy-agenda");
        ProposalRequest request = request();
        request.setPriorityAgenda(Map.of("legacy-agenda", selected("Kept")));

        service.saveProposal(request, PROPOSAL_ID);

        // The service asks for "active keys plus what this proposal already stores", which
        // is what keeps a historical selection resubmittable.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(priorityAgendaService).allowedProposalKeys(captor.capture());
        assertTrue(captor.getValue().contains("legacy-agenda"));
    }

    @Test
    void aRequestWithoutAgendasDoesNotPersistAnyAgenda() {
        assertTrue(service.saveProposal(request(), null).getPriorityAgendas().isEmpty());
    }
}
