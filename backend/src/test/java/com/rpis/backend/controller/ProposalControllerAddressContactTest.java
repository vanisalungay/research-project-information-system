package com.rpis.backend.controller;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level test for {@code POST /api/proposals} with the Create Proposal
 * form's separated "Address *" and "Contact *" sections: the JSON the form sends
 * must bind to the request DTO, be persisted as distinct columns and still fill
 * the combined {@code address} text every existing screen reads.
 * <p>
 * Uses the real controller + service with mocked repositories, so no database
 * and no credentials are involved.
 */
class ProposalControllerAddressContactTest {

    private ProposalRepository proposalRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ProposalAnnouncementService announcementService = mock(ProposalAnnouncementService.class);
        ProposalService service = new ProposalService(proposalRepository, userRepository,
                mock(NotificationService.class), announcementService,
                mock(ProposalVersionService.class), mock(ProposalSectionRemarkService.class), new CurrentUser(),
                mock(CooperatingAgencyService.class), mock(PriorityAgendaService.class));
        ProposalController controller = new ProposalController(service,
                mock(ProposalReviewService.class), mock(ProposalVersionService.class),
                mock(ProposalSectionRemarkService.class), mock(CooperatingAgencyService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        User proponent = new User();
        proponent.setId(1L);
        proponent.setName("Address Proponent");
        proponent.setEmail("address.contact@msunaawan.edu.ph");
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

    @Test
    void theAddressAndContactSectionsOfTheRequestArePersisted() throws Exception {
        String payload = """
                {
                  "projectTitle": "Address / Contact API test",
                  "duration": "12",
                  "status": "DRAFT",
                  "addressCountry": "Philippines",
                  "addressRegion": "Region X \u2013 Northern Mindanao",
                  "addressProvince": "Misamis Oriental",
                  "addressDistrict": "",
                  "addressMunicipality": "Naawan",
                  "addressBarangay": "Balintad",
                  "contactType": "CONTACT_NUMBER",
                  "contactValue": "0917 123 4567"
                }
                """;

        mockMvc.perform(post("/api/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isCreated());

        ArgumentCaptor<Proposal> saved = ArgumentCaptor.forClass(Proposal.class);
        // The service persists the proposal more than once per request; the last
        // capture is the row that is returned to the caller.
        verify(proposalRepository, atLeastOnce()).save(saved.capture());
        Proposal proposal = saved.getValue();

        assertEquals("Philippines", proposal.getAddressCountry());
        assertEquals("Region X \u2013 Northern Mindanao", proposal.getAddressRegion());
        assertEquals("Misamis Oriental", proposal.getAddressProvince());
        assertEquals("", proposal.getAddressDistrict());
        assertEquals("Naawan", proposal.getAddressMunicipality());
        assertEquals("Balintad", proposal.getAddressBarangay());
        assertEquals("CONTACT_NUMBER", proposal.getContactType());
        assertEquals("0917 123 4567", proposal.getContactValue());
        assertEquals(
                "Balintad, Naawan, Misamis Oriental, Region X \u2013 Northern Mindanao, Philippines / 0917 123 4567",
                proposal.getAddress());
    }

    @Test
    void aSubmissionWithoutACountryIsStoredAsThePhilippines() throws Exception {
        // The Address section has no Country control any more: the address arrives as
        // its parts and the backend fills the country in.
        String payload = """
                {
                  "projectTitle": "No country API test",
                  "duration": "12",
                  "status": "DRAFT",
                  "addressRegion": "Region X \u2013 Northern Mindanao",
                  "addressProvince": "Misamis Oriental",
                  "addressDistrict": "",
                  "addressMunicipality": "Naawan",
                  "addressBarangay": "Balintad",
                  "contactType": "CONTACT_NUMBER",
                  "contactValue": "0917 123 4567"
                }
                """;

        mockMvc.perform(post("/api/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isCreated());

        ArgumentCaptor<Proposal> saved = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository, atLeastOnce()).save(saved.capture());
        Proposal proposal = saved.getValue();

        assertEquals("Philippines", proposal.getAddressCountry());
        assertEquals(
                "Balintad, Naawan, Misamis Oriental, Region X \u2013 Northern Mindanao, Philippines / 0917 123 4567",
                proposal.getAddress());
    }

    @Test
    void aRequestWithoutTheSplitFieldsKeepsTheLegacyAddress() throws Exception {
        String payload = """
                {
                  "projectTitle": "Legacy API test",
                  "duration": "12",
                  "status": "DRAFT",
                  "address": "San Miguel, Naawan / 0917 000 0000"
                }
                """;

        mockMvc.perform(post("/api/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isCreated());

        ArgumentCaptor<Proposal> saved = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository, atLeastOnce()).save(saved.capture());
        Proposal proposal = saved.getValue();

        assertEquals("San Miguel, Naawan / 0917 000 0000", proposal.getAddress());
        assertNull(proposal.getAddressCountry());
        assertNull(proposal.getContactType());
    }

    @Test
    void anInvalidContactIsRejectedOnSubmission() throws Exception {
        String payload = """
                {
                  "projectTitle": "Invalid contact",
                  "duration": "12",
                  "status": "SUBMITTED",
                  "addressCountry": "Philippines",
                  "addressRegion": "Northern Mindanao",
                  "addressProvince": "Misamis Oriental",
                  "addressDistrict": "",
                  "addressMunicipality": "Naawan",
                  "addressBarangay": "Balintad",
                  "contactType": "EMAIL_ADDRESS",
                  "contactValue": "not-an-email"
                }
                """;

        mockMvc.perform(post("/api/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isBadRequest());

        // Nothing may be persisted when the backend rejects the request.
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void aSubmissionWithAValidAddressAndContactIsAccepted() throws Exception {
        String payload = """
                {
                  "projectTitle": "Submitted address / contact",
                  "duration": "12",
                  "status": "SUBMITTED",
                  "addressCountry": "Philippines",
                  "addressRegion": "Northern Mindanao",
                  "addressProvince": "Misamis Oriental",
                  "addressDistrict": "",
                  "addressMunicipality": "Naawan",
                  "addressBarangay": "Balintad",
                  "contactType": "EMAIL_ADDRESS",
                  "contactValue": "juan.delaacruz@msunaawan.edu.ph"
                }
                """;

        mockMvc.perform(post("/api/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isCreated());

        ArgumentCaptor<Proposal> saved = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository, atLeastOnce()).save(saved.capture());
        assertEquals("EMAIL_ADDRESS", saved.getValue().getContactType());
        assertEquals("juan.delaacruz@msunaawan.edu.ph", saved.getValue().getContactValue());
    }
}
