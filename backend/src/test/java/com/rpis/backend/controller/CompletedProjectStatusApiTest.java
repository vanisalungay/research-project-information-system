package com.rpis.backend.controller;

import com.rpis.backend.model.Proposal;
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
import com.rpis.backend.storage.LocalUploadStorage;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The project completion status is stored on the proposal itself, so every role reads
 * the same COMPLETED value from the ordinary proposal endpoint. No role-specific
 * translation exists, and this guard proves it: the same completed proposal is read
 * back unchanged by a Proponent (its owner) and by each reviewing/administrative role.
 */
class CompletedProjectStatusApiTest {

    private static final Long PROPONENT_ID = 10L;
    private static final Long PROPOSAL_ID = 47L;

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
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Proposal completedProject() {
        Proposal proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        proposal.setStatus("COMPLETED");
        proposal.setSoNumber("SO-2026-001");
        proposal.setSoUploadedAt(java.time.LocalDateTime.of(2026, 7, 28, 23, 20));
        User owner = new User();
        owner.setId(PROPONENT_ID);
        proposal.setProponent(owner);
        return proposal;
    }

    @Test
    void everyRoleReadsTheSameCompletedStatus() throws Exception {
        when(proposalRepository.findById(PROPOSAL_ID))
                .thenReturn(Optional.of(completedProject()));

        // The Proponent owner and each role that may view university-wide proposals.
        for (String role : List.of("PROPONENT", "RPS_ADMIN", "RPS_STAFF", "DEAN",
                "REC", "OVCRIGE", "OC")) {
            authenticateAs(PROPONENT_ID, role);
            mockMvc.perform(get("/api/proposals/{id}", PROPOSAL_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }
    }

    @Test
    void aCompletedProjectIsNotReportedAsApprovedForImplementation() throws Exception {
        when(proposalRepository.findById(PROPOSAL_ID))
                .thenReturn(Optional.of(completedProject()));
        authenticateAs(PROPONENT_ID, "PROPONENT");

        mockMvc.perform(get("/api/proposals/{id}", PROPOSAL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.status").value(org.hamcrest.Matchers.not("APPROVED")));
    }
}
