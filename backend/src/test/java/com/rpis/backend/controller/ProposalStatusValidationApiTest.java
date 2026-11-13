package com.rpis.backend.controller;

import com.rpis.backend.model.Proposal;
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
import com.rpis.backend.storage.LocalUploadStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The HTTP contract of proposal-status validation: an unknown status is a client error
 * (400) with a message naming the value, not a database constraint failure surfacing as
 * a 500.
 */
class ProposalStatusValidationApiTest {

    private ProposalRepository proposalRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ProposalService service = new ProposalService(proposalRepository, userRepository,
                mock(NotificationService.class), mock(ProposalAnnouncementService.class),
                mock(ProposalVersionService.class), mock(ProposalSectionRemarkService.class),
                new CurrentUser(), mock(CooperatingAgencyService.class),
                mock(PriorityAgendaService.class), new LocalUploadStorage("uploads"));
        ProposalController controller = new ProposalController(service,
                mock(ProposalReviewService.class), mock(ProposalVersionService.class),
                mock(ProposalSectionRemarkService.class), mock(CooperatingAgencyService.class),
                new LocalUploadStorage("uploads"));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(2L, null,
                        List.of(new SimpleGrantedAuthority("ROLE_RPS_STAFF"))));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creatingAProposalWithAnUnknownStatusReturns400() throws Exception {
        mockMvc.perform(post("/api/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectTitle\":\"Unknown status\",\"status\":\"NOT_A_PROPOSAL_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("NOT_A_PROPOSAL_STATUS")));

        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void updatingAProposalWithAnUnknownStatusReturns400() throws Exception {
        Proposal existing = new Proposal();
        existing.setId(7L);
        existing.setProjectTitle("Unknown status");
        existing.setStatus("DRAFT");
        when(proposalRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(existing));

        mockMvc.perform(put("/api/proposals/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectTitle\":\"Unknown status\",\"status\":\"NOT_A_PROPOSAL_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("NOT_A_PROPOSAL_STATUS")));

        verify(proposalRepository, never()).save(any(Proposal.class));
    }
}
