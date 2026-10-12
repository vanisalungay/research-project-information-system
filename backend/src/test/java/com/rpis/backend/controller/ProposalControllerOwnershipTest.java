package com.rpis.backend.controller;

import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.service.NotificationService;
import com.rpis.backend.service.ProposalAnnouncementService;
import com.rpis.backend.service.ProposalReviewService;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end wiring test for the leaking endpoint {@code GET /api/proposals}.
 * Exercises the real controller + service + {@link CurrentUser} (with a mocked
 * repository) to prove the HTTP response is scoped by the authenticated principal.
 */
class ProposalControllerOwnershipTest {

    private ProposalRepository proposalRepository;
    private UserRepository userRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        userRepository = mock(UserRepository.class);
        ProposalService service = new ProposalService(proposalRepository, userRepository,
                mock(NotificationService.class), mock(ProposalAnnouncementService.class),
                mock(ProposalVersionService.class), new CurrentUser());
        ProposalController controller = new ProposalController(service,
                mock(ProposalReviewService.class), mock(ProposalVersionService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    @Test
    void newProponentWithNoProposalsReceivesAnEmptyArray() throws Exception {
        User me = user(7L);
        authenticate(7L, "PROPONENT");
        when(userRepository.findById(7L)).thenReturn(Optional.of(me));
        when(proposalRepository.findByProponent(me)).thenReturn(List.of());

        mockMvc.perform(get("/api/proposals"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(proposalRepository).findByProponent(me);
        verify(proposalRepository, never()).findAll();
    }

    @Test
    void authorizedStaffStillReceiveTheUniversityWideList() throws Exception {
        authenticate(2L, "RPS_STAFF");
        when(proposalRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/proposals"))
                .andExpect(status().isOk());

        verify(proposalRepository).findAll();
    }
}
