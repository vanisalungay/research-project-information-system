package com.rpis.backend.controller;

import com.rpis.backend.dto.ProposalSectionRemarksResponse;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.service.CooperatingAgencyService;
import com.rpis.backend.service.NotificationService;
import com.rpis.backend.service.PriorityAgendaService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.rpis.backend.storage.LocalUploadStorage;

/**
 * HTTP-level wiring test for the two endpoints:
 * {@code POST /api/proposals/{id}/duplicate} ("Make a Copy" of a proposal, whatever
 * its status) and {@code GET /api/proposals/{id}/section-remarks}.
 * <p>
 * Exercises the real controller + service + {@link CurrentUser} (with a mocked
 * repository) to prove ownership is enforced from the authenticated principal, so a
 * Proponent cannot duplicate or read the remarks of a proposal they do not own.
 */
class ProposalControllerDuplicateTest {

    private ProposalRepository proposalRepository;
    private ProposalSectionRemarkService sectionRemarkService;
    private ProposalAnnouncementService announcementService;
    private MockMvc mockMvc;

    /** The Call for Proposal the proponent selects in the "Make a Copy" picker. */
    private static final Long CALL_ID = 5L;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        sectionRemarkService = mock(ProposalSectionRemarkService.class);
        announcementService = mock(ProposalAnnouncementService.class);
        ProposalService service = new ProposalService(proposalRepository, userRepository,
                mock(NotificationService.class), announcementService,
                mock(ProposalVersionService.class), sectionRemarkService, new CurrentUser(),
                mock(CooperatingAgencyService.class), mock(PriorityAgendaService.class), new LocalUploadStorage("uploads"));
        ProposalController controller = new ProposalController(service,
                mock(ProposalReviewService.class), mock(ProposalVersionService.class),
                sectionRemarkService, mock(CooperatingAgencyService.class), new LocalUploadStorage("uploads"));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        when(proposalRepository.nextProposalCodeSequence()).thenReturn(7L);
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAll()).thenReturn(List.of());
        // The call the proponent picked is open, so the copy may be created under it.
        when(announcementService.requireOpenAnnouncement(anyLong())).thenAnswer(inv -> {
            ProposalAnnouncement announcement = new ProposalAnnouncement();
            announcement.setId(inv.getArgument(0));
            return announcement;
        });
    }

    /** The JSON body of a "Make a Copy" request for the selected call. */
    private static String copyBody(Long announcementId) {
        return announcementId == null
                ? "{}"
                : "{\"proposalAnnouncementId\":" + announcementId + "}";
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
        proposal.setProjectTitle("Rejected Project");
        return proposal;
    }

    @Test
    void ownerCanCopyARejectedProposal() throws Exception {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal(1L, 7L, "REJECTED")));

        mockMvc.perform(post("/api/proposals/1/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(copyBody(CALL_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.proposalCode").value("007"))
                .andExpect(jsonPath("$.proposalAnnouncementId").value(CALL_ID))
                .andExpect(jsonPath("$.projectTitle").value("Rejected Project"));
    }

    @Test
    void aCopyWithoutASelectedCallIsRejected() throws Exception {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal(1L, 7L, "REJECTED")));

        mockMvc.perform(post("/api/proposals/1/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(copyBody(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Please select a call for proposal before making a copy."));

        // Nothing is created and no identifier is allocated.
        verify(proposalRepository, never()).save(any(Proposal.class));
        verify(proposalRepository, never()).nextProposalCodeSequence();
    }

    @Test
    void aCopyIsRejectedWhenTheSelectedCallIsClosed() throws Exception {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal(1L, 7L, "REJECTED")));
        when(announcementService.requireOpenAnnouncement(9L)).thenThrow(new IllegalStateException(
                "The selected Call for Proposal is no longer open for submissions. "
                        + "Please select an open call for proposal."));

        mockMvc.perform(post("/api/proposals/1/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(copyBody(9L)))
                .andExpect(status().isForbidden());

        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void ownerCanCopyAProposalInAnyStatus() throws Exception {
        for (String status : List.of("APPROVED", "SUBMITTED", "UNDER_REVIEW", "RPS_RETURNED",
                "IMPLEMENTATION", "COMPLETED", "DRAFT", "REJECTED")) {
            authenticate(7L, "PROPONENT");
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal(1L, 7L, status)));

            mockMvc.perform(post("/api/proposals/1/duplicate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(copyBody(CALL_ID)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.projectTitle").value("Rejected Project"));
        }
    }

    @Test
    void proponentCannotDuplicateAnotherUsersProposal() throws Exception {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal(1L, 8L, "REJECTED")));

        mockMvc.perform(post("/api/proposals/1/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(copyBody(CALL_ID)))
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
