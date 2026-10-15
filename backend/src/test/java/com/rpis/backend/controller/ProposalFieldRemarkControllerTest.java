package com.rpis.backend.controller;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalSectionRemark;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.ProposalSectionRemarkRepository;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level wiring test for the field-level remark endpoints:
 * {@code PUT /api/proposals/{id}/field-remarks},
 * {@code DELETE /api/proposals/{id}/field-remarks/{remarkId}} and
 * {@code GET /api/proposals/{id}/field-remarks}.
 * <p>
 * Exercises the real controller + service + {@link CurrentUser} (mocked repository) so
 * the reviewer-only write guard and the per-proposal ownership check are proven from
 * the authenticated principal.
 */
class ProposalFieldRemarkControllerTest {

    private ProposalRepository proposalRepository;
    private ProposalSectionRemarkRepository remarkRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        remarkRepository = mock(ProposalSectionRemarkRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUser currentUser = new CurrentUser();
        ProposalSectionRemarkService remarkService =
                new ProposalSectionRemarkService(remarkRepository, userRepository, currentUser);
        ProposalService proposalService = new ProposalService(proposalRepository, userRepository,
                mock(NotificationService.class), mock(ProposalAnnouncementService.class),
                mock(ProposalVersionService.class), remarkService, currentUser);
        ProposalController controller = new ProposalController(proposalService,
                mock(ProposalReviewService.class), mock(ProposalVersionService.class), remarkService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(remarkRepository.save(any(ProposalSectionRemark.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(remarkRepository.findByProposalIdAndStatus(any(), any())).thenReturn(List.of());
        when(remarkRepository.findByProposalIdOrderByRoundDescIdAsc(any())).thenReturn(List.of());
        when(userRepository.findById(9L)).thenReturn(Optional.of(reviewer()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private User reviewer() {
        User user = new User();
        user.setId(9L);
        user.setName("RPS Reviewer");
        user.setRole("RPS_STAFF");
        return user;
    }

    private Proposal proposal(Long id, Long ownerId) {
        User owner = new User();
        owner.setId(ownerId);
        Proposal proposal = new Proposal();
        proposal.setId(id);
        proposal.setProponent(owner);
        proposal.setStatus("SUBMITTED");
        proposal.setProjectTitle("Water Filtration");
        return proposal;
    }

    private String body(String sectionKey, String fieldKey, String remark) {
        return "{\"sectionKey\":\"" + sectionKey + "\",\"fieldKey\":\"" + fieldKey
                + "\",\"fieldLabel\":\"Project Title\",\"remark\":\"" + remark + "\"}";
    }

    @Test
    void reviewerCanSaveARemarkBesideOneField() throws Exception {
        authenticate(9L, "RPS_STAFF");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal(1L, 7L)));

        mockMvc.perform(put("/api/proposals/1/field-remarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("01", "projectTitle", "Shorten the title.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionKey").value("01"))
                .andExpect(jsonPath("$.fieldKey").value("projectTitle"))
                .andExpect(jsonPath("$.remark").value("Shorten the title."))
                .andExpect(jsonPath("$.reviewerRole").value("RPS_STAFF"));
    }

    @Test
    void aProponentCannotSaveAFieldRemark() throws Exception {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal(1L, 7L)));

        mockMvc.perform(put("/api/proposals/1/field-remarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("01", "projectTitle", "Sneaky")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anInvalidFieldIdentifierIsRejectedWith400() throws Exception {
        authenticate(9L, "RPS_STAFF");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal(1L, 7L)));

        mockMvc.perform(put("/api/proposals/1/field-remarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("01", "Project Title!", "Text")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reviewerCanReadTheFieldRemarksOfAProposal() throws Exception {
        authenticate(9L, "RPS_STAFF");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal(1L, 7L)));

        mockMvc.perform(get("/api/proposals/1/field-remarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposalId").value(1));
    }

    @Test
    void aProponentCannotReadAnotherUsersFieldRemarks() throws Exception {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal(1L, 8L)));

        mockMvc.perform(get("/api/proposals/1/field-remarks"))
                .andExpect(status().isForbidden());
    }

    @Test
    void reviewerCanDeleteARemark() throws Exception {
        authenticate(9L, "RPS_STAFF");
        Proposal source = proposal(1L, 7L);
        ProposalSectionRemark existing = new ProposalSectionRemark();
        existing.setId(55L);
        existing.setProposal(source);
        existing.setSectionKey("01");
        existing.setRemark("Old remark");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));
        when(remarkRepository.findById(55L)).thenReturn(Optional.of(existing));

        mockMvc.perform(delete("/api/proposals/1/field-remarks/55"))
                .andExpect(status().isNoContent());
    }

    @Test
    void aProponentCannotDeleteARemark() throws Exception {
        authenticate(7L, "PROPONENT");
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal(1L, 7L)));

        mockMvc.perform(delete("/api/proposals/1/field-remarks/55"))
                .andExpect(status().isForbidden());
    }
}
