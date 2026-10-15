package com.rpis.backend.controller;

import com.rpis.backend.dto.ProposalSectionRemarksResponse;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.service.NotificationService;
import com.rpis.backend.service.ProposalAnnouncementService;
import com.rpis.backend.service.ProposalReviewService;
import com.rpis.backend.service.ProposalSectionRemarkService;
import com.rpis.backend.service.ProposalService;
import com.rpis.backend.service.ProposalVersionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level wiring test for the two new endpoints:
 * {@code POST /api/proposals/{id}/duplicate} and
 * {@code GET /api/proposals/{id}/section-remarks}.
 * <p>
 * Exercises the real controller + service + {@link CurrentUser} (with a mocked
 * repository) to prove ownership is enforced from the authenticated principal, so a
 * Proponent cannot duplicate or read the remarks of a proposal they do not own.
 */
class ProposalControllerDuplicateTest {

    private ProposalRepository proposalRepository;
    private ProposalSectionRemarkService sectionRemarkService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        sectionRemarkService = mock(ProposalSectionRemarkService.class);
        ProposalService service = new ProposalService(proposalRepository, userRepository,
                mock(NotificationService.class), mock(ProposalAnnouncementService.class),
                mock(ProposalVersionService.class), sectionRemarkService, new CurrentUser());
        ProposalController controller = new ProposalController(service,
                mock(ProposalReviewService.class), mock(ProposalVersionService.class),
                sectionRemarkService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        when(proposalRepository.nextProposalCodeSequence()).thenReturn(7L);
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAll()).thenReturn(List.of());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Proposal proposal(Long id, Long ownerId, String status) {
        User owner = new User();
        owner.setId(ownerId);
        Proposal proposal = new Proposal();
        proposal.setId(id);
        proposal.setProponent(owner);
        proposal.setStatus(status);
        proposal.setProjectTitle("Completed Project");
        return proposal;
    }

    @Test
    void ownerCanDuplicateAnEligibleProposal() throws Exception {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal(1L, 7L, "COMPLETED")));

        mockMvc.perform(post("/api/proposals/1/duplicate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.proposalCode").value("007"))
                .andExpect(jsonPath("$.projectTitle").value("Completed Project"));
    }

    @Test
    void proponentCannotDuplicateAnotherUsersProposal() throws Exception {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal(1L, 8L, "COMPLETED")));

        mockMvc.perform(post("/api/proposals/1/duplicate"))
                .andExpect(status().isForbidden());

        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void ownerCanReadTheirOwnSectionRemarks() throws Exception {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal(1L, 7L, "RPS_RETURNED")));

        ProposalSectionRemarksResponse response = new ProposalSectionRemarksResponse();
        response.setProposalId(1L);
        response.setActiveRound(1);
        when(sectionRemarkService.getRemarksForProposal(1L)).thenReturn(response);

        mockMvc.perform(get("/api/proposals/1/section-remarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposalId").value(1))
                .andExpect(jsonPath("$.activeRound").value(1));
    }

    @Test
    void proponentCannotReadAnotherUsersSectionRemarks() throws Exception {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal(1L, 8L, "RPS_RETURNED")));

        mockMvc.perform(get("/api/proposals/1/section-remarks"))
                .andExpect(status().isForbidden());

        verify(sectionRemarkService, never()).getRemarksForProposal(any());
    }
}
