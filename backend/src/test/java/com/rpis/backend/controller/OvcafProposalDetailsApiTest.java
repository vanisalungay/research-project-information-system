package com.rpis.backend.controller;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.OvcafValidationRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.service.NotificationService;
import com.rpis.backend.service.OvcafService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level wiring test for the OVCAF proposal detail response.
 * <p>
 * The evaluation card and the details page read this response (and the saved proposal
 * record) to show the Project Leader and the attached Line-Item Budget. This test proves
 * the JSON actually exposes both fields for a proposal that reached OVCAF: the form's
 * Project Leader is set, the account name differs, the document display name is absent,
 * and the stored path is present. The fixture also carries the OC approval record
 * (SO number + recorded time) that OVCAF now requires, and a separate test proves a
 * proposal without it is not reachable (the shape of records #78 / #80).
 */
class OvcafProposalDetailsApiTest {

    private static final Long PROPOSAL_ID = 78L;
    private static final String STORED_PATH =
            "budget-files/e2e28185-53ba-4b72-b745-231e9d8a0b9a_line-item-budget.docx";

    private ProposalRepository proposalRepository;
    private OvcafValidationRepository validationRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        validationRepository = mock(OvcafValidationRepository.class);
        OvcafService service = new OvcafService(proposalRepository, validationRepository,
                mock(UserRepository.class), mock(NotificationService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(new OvcafController(service)).build();
    }

    private Proposal proposal(String status) {
        Proposal proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        proposal.setDocumentId("MSUN-ORPS-DA-117-2026-REV00");
        proposal.setProjectTitle("Mangrove Monitoring");
        proposal.setCollege("College of Science and Mathematics");
        proposal.setStatus(status);
        // The OC final-approval record every OVCAF-visible proposal must carry.
        proposal.setSoNumber("SO-2026-117");
        proposal.setSoUploadedAt(java.time.LocalDateTime.of(2026, 8, 25, 15, 10));

        User account = new User();
        account.setId(9L);
        account.setName("Proponent User");
        proposal.setProponent(account);
        return proposal;
    }

    private void returned(Proposal proposal) {
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        when(validationRepository.findTopByProposalIdOrderByCreatedAtDesc(PROPOSAL_ID))
                .thenReturn(Optional.empty());
    }

    @Test
    void theResponseCarriesTheFormProjectLeaderAndTheAttachedDocument() throws Exception {
        Proposal proposal = proposal("FOR_OC_APPROVAL");
        proposal.setProjectLeader("Andrea Mae Villanueva");
        // The existing record stores the attachment path but no display name; the client
        // derives a readable name from the path instead of reporting "not uploaded".
        proposal.setBeneficiariesFilePath(STORED_PATH);
        returned(proposal);

        mockMvc.perform(get("/api/ovcaf/proposals/{id}", PROPOSAL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectLeader").value("Andrea Mae Villanueva"))
                .andExpect(jsonPath("$.proponentName").value("Andrea Mae Villanueva"))
                .andExpect(jsonPath("$.beneficiariesFilePath").value(STORED_PATH))
                .andExpect(jsonPath("$.beneficiariesFileName").doesNotExist());
    }

    @Test
    void aProposalWithoutADocumentReportsNoReference() throws Exception {
        Proposal proposal = proposal("FOR_OVCAF_APPROVAL");
        proposal.setProjectLeader("Andrea Mae Villanueva");
        returned(proposal);

        mockMvc.perform(get("/api/ovcaf/proposals/{id}", PROPOSAL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beneficiariesFileName").doesNotExist())
                .andExpect(jsonPath("$.beneficiariesFilePath").doesNotExist());
    }

    @Test
    void aDraftIsNotReachableThroughTheOvcafDetailRoute() throws Exception {
        returned(proposal("DRAFT"));

        mockMvc.perform(get("/api/ovcaf/proposals/{id}", PROPOSAL_ID))
                .andExpect(status().isNotFound());
    }

    /**
     * The exact shape of the historic inconsistent records (#78 / #80): an
     * OVCAF-visible status with no OC approval and no SO. OVCAF must not be able to
     * open it, even by knowing its id.
     */
    @Test
    void aProposalWithoutTheOcApprovalAndSoIsNotReachable() throws Exception {
        Proposal inconsistent = proposal("FOR_OVCAF_APPROVAL");
        inconsistent.setSoNumber(null);
        inconsistent.setSoUploadedAt(null);
        returned(inconsistent);

        mockMvc.perform(get("/api/ovcaf/proposals/{id}", PROPOSAL_ID))
                .andExpect(status().isNotFound());
    }
}
