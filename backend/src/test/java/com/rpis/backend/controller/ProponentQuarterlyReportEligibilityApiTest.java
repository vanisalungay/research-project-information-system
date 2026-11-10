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
import com.rpis.backend.storage.LocalUploadStorage;

/**
 * HTTP-level guard for the data the proponent interface uses to decide whether the
 * Create Quarterly Report action may be offered.
 * <p>
 * The UI reads {@code status}, {@code soNumber} and {@code soUploadedAt} from
 * {@code GET /api/proposals/{id}} (and from the list route). If a response stops
 * carrying any of them the action would silently disappear for every project, so
 * all three are asserted here together with the ownership rule that keeps another
 * proponent's project unreachable.
 */
class ProponentQuarterlyReportEligibilityApiTest {

    private static final Long PROPONENT_ID = 10L;
    private static final Long PROPOSAL_ID = 47L;

    private ProposalRepository proposalRepository;
    private UserRepository userRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        userRepository = mock(UserRepository.class);
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

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                PROPONENT_ID, null, List.of(new SimpleGrantedAuthority("ROLE_PROPONENT"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Proposal proposalOwnedBy(Long ownerId, String status, String soNumber) {
        Proposal proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        proposal.setStatus(status);
        proposal.setSoNumber(soNumber);
        proposal.setSoUploadedAt(java.time.LocalDateTime.of(2026, 7, 28, 23, 20));
        User owner = new User();
        owner.setId(ownerId);
        proposal.setProponent(owner);
        return proposal;
    }

    @Test
    void theProponentResponseCarriesTheStatusAndIssuedSo() throws Exception {
        when(proposalRepository.findById(PROPOSAL_ID))
                .thenReturn(Optional.of(proposalOwnedBy(PROPONENT_ID, "APPROVED", "SO-2026-001")));

        mockMvc.perform(get("/api/proposals/{id}", PROPOSAL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.soNumber").value("SO-2026-001"))
                .andExpect(jsonPath("$.soUploadedAt").exists());
    }

    @Test
    void anApprovedProjectWithoutAnSoStillReportsItsApprovedStatus() throws Exception {
        when(proposalRepository.findById(PROPOSAL_ID))
                .thenReturn(Optional.of(proposalOwnedBy(PROPONENT_ID, "APPROVED", "")));

        mockMvc.perform(get("/api/proposals/{id}", PROPOSAL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.soNumber").value(""));
    }

    @Test
    void theProjectsListCarriesTheStatusAndIssuedSo() throws Exception {
        User owner = new User();
        owner.setId(PROPONENT_ID);
        owner.setRole("PROPONENT");
        when(userRepository.findById(PROPONENT_ID)).thenReturn(Optional.of(owner));
        when(proposalRepository.findByProponent(owner))
                .thenReturn(List.of(proposalOwnedBy(PROPONENT_ID, "APPROVED", "SO-2026-001")));

        mockMvc.perform(get("/api/proposals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[0].soNumber").value("SO-2026-001"))
                .andExpect(jsonPath("$[0].soUploadedAt").exists());
    }

    @Test
    void anotherProponentsProjectCannotBeRead() throws Exception {
        when(proposalRepository.findById(PROPOSAL_ID))
                .thenReturn(Optional.of(proposalOwnedBy(99L, "APPROVED", "SO-2026-001")));

        mockMvc.perform(get("/api/proposals/{id}", PROPOSAL_ID))
                .andExpect(status().isForbidden());
    }
}
