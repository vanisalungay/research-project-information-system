package com.rpis.backend.controller;

import com.rpis.backend.model.CooperatingAgency;
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
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level test for {@code POST /api/proposals} with the Create Proposal form's
 * cooperating agency checklist: the JSON the form sends carries the ids of the
 * agencies selected from the shared directory, the selection is handed to the
 * directory service (never stored as free text) and the text column every existing
 * screen reads is derived from the resolved names.
 * <p>
 * Uses the real controller + service with mocked repositories, so no database and no
 * credentials are involved.
 */
class ProposalControllerCooperatingAgencyTest {

    private ProposalRepository proposalRepository;
    private CooperatingAgencyService cooperatingAgencyService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ProposalAnnouncementService announcementService = mock(ProposalAnnouncementService.class);
        cooperatingAgencyService = mock(CooperatingAgencyService.class);
        ProposalService service = new ProposalService(proposalRepository, userRepository,
                mock(NotificationService.class), announcementService,
                mock(ProposalVersionService.class), mock(ProposalSectionRemarkService.class), new CurrentUser(),
                cooperatingAgencyService, mock(PriorityAgendaService.class));
        ProposalController controller = new ProposalController(service,
                mock(ProposalReviewService.class), mock(ProposalVersionService.class),
                mock(ProposalSectionRemarkService.class), cooperatingAgencyService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        User proponent = new User();
        proponent.setId(1L);
        proponent.setName("Agency Proponent");
        proponent.setEmail("agency@msunaawan.edu.ph");
        proponent.setRole("PROPONENT");
        when(userRepository.findById(1L)).thenReturn(Optional.of(proponent));

        ProposalAnnouncement announcement = new ProposalAnnouncement();
        announcement.setId(9L);
        when(announcementService.getActiveAnnouncementOrNull()).thenReturn(announcement);

        when(proposalRepository.nextProposalCodeSequence()).thenReturn(1L);
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_PROPONENT"))));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private org.springframework.test.web.servlet.ResultActions submit(String payload) throws Exception {
        return mockMvc.perform(post("/api/proposals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void theSelectedAgencyIdsAreResolvedAndTheTextColumnIsDerived() throws Exception {
        CooperatingAgency denr = new CooperatingAgency(
                2L, "Department of Environment and Natural Resources (DENR)", null);
        CooperatingAgency dost = new CooperatingAgency(
                1L, "Department of Science and Technology (DOST)", null);
        when(cooperatingAgencyService.replaceSelection(any(Proposal.class), any()))
                .thenReturn(List.of(denr, dost));

        String payload = """
                {
                  "projectTitle": "Agency API test",
                  "duration": "12",
                  "status": "DRAFT",
                  "cooperatingAgencyIds": [2, 1]
                }
                """;

        submit(payload).andExpect(status().isCreated());

        ArgumentCaptor<Proposal> saved = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository, atLeastOnce()).save(saved.capture());
        Proposal proposal = saved.getValue();

        assertEquals(
                "Department of Environment and Natural Resources (DENR), "
                        + "Department of Science and Technology (DOST)",
                proposal.getCooperatingAgencies());

        // The ids are handed to the directory service, which is what decides which
        // agencies the proposal points at.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> ids = ArgumentCaptor.forClass(List.class);
        verify(cooperatingAgencyService).replaceSelection(any(Proposal.class), ids.capture());
        assertEquals(List.of(2L, 1L), ids.getValue());
    }

    @Test
    void aSubmissionWithoutAnyAgencyIsRejected() throws Exception {
        String payload = """
                {
                  "projectTitle": "Agency API test",
                  "duration": "12",
                  "status": "SUBMITTED",
                  "cooperatingAgencyIds": []
                }
                """;

        submit(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("At least one cooperating agency must be selected."));

        verify(cooperatingAgencyService, never()).replaceSelection(any(Proposal.class), any());
    }

    @Test
    void anUnknownAgencyIdIsRejectedWithAReadableError() throws Exception {
        when(cooperatingAgencyService.replaceSelection(any(Proposal.class), any()))
                .thenThrow(new IllegalArgumentException("Unknown cooperating agency: 999"));

        String payload = """
                {
                  "projectTitle": "Agency API test",
                  "duration": "12",
                  "status": "DRAFT",
                  "cooperatingAgencyIds": [999]
                }
                """;

        submit(payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown cooperating agency: 999"));
    }

    @Test
    void aClientThatSendsOnlyTheLegacyTextKeepsWorking() throws Exception {
        String payload = """
                {
                  "projectTitle": "Legacy agency text",
                  "duration": "12",
                  "status": "DRAFT",
                  "cooperatingAgencies": "DOST / DENR"
                }
                """;

        submit(payload).andExpect(status().isCreated());

        ArgumentCaptor<Proposal> saved = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository, atLeastOnce()).save(saved.capture());

        // The text is stored unchanged and the directory is not involved at all.
        assertEquals("DOST / DENR", saved.getValue().getCooperatingAgencies());
        verify(cooperatingAgencyService, never()).replaceSelection(any(Proposal.class), any());
    }
}
