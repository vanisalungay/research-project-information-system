package com.rpis.backend.service;

import com.rpis.backend.dto.OvcafProposalResponse;
import com.rpis.backend.dto.OvcafValidationRequest;
import com.rpis.backend.model.OvcafValidation;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.OvcafValidationRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for what OVCAF is shown on a proposal card and details page.
 * <p>
 * Two rules are covered here:
 * <ul>
 *   <li>the "Proponent" shown is the name typed into the proposal form
 *       ({@code projectLeader}), not the generic login account that owns the record;</li>
 *   <li>the attached Line-Item Budget document (name + stored path) is carried by the
 *       response, and a draft is never reachable through the OVCAF detail route.</li>
 * </ul>
 */
class OvcafServiceTest {

    private static final Long PROPOSAL_ID = 117L;

    private ProposalRepository proposalRepository;
    private OvcafValidationRepository validationRepository;
    private OvcafService service;
    private Proposal proposal;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        validationRepository = mock(OvcafValidationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        service = new OvcafService(proposalRepository, validationRepository, userRepository, notificationService);

        proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        proposal.setDocumentId("MSUN-ORPS-DA-117-2026-REV00");
        proposal.setProjectTitle("Coastal Research");
        proposal.setCollege("College of Engineering");
        proposal.setStatus("FOR_OVCAF_APPROVAL");
        // The OC final-approval record every OVCAF-visible proposal must carry.
        proposal.setSoNumber("SO-2026-117");
        proposal.setSoUploadedAt(java.time.LocalDateTime.of(2026, 8, 25, 15, 10));
    }

    private OvcafProposalResponse details() {
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        when(validationRepository.findTopByProposalIdOrderByCreatedAtDesc(PROPOSAL_ID))
                .thenReturn(Optional.empty());
        return service.getProposalDetails(PROPOSAL_ID);
    }

    @Test
    void theProponentIsTheNameOnTheProposalFormNotTheLoginAccount() {
        proposal.setProjectLeader("Andrea Mae Villanueva");
        User account = new User();
        account.setId(9L);
        account.setName("Proponent User");
        proposal.setProponent(account);

        OvcafProposalResponse response = details();

        assertEquals("Andrea Mae Villanueva", response.getProponentName(),
                "the form's Proponent Name must win over the generic account name");
        assertEquals(9L, response.getProponentId(),
                "the account relationship is still reported for authorization/notification");
    }

    @Test
    void theLoginAccountIsOnlyAFallbackWhenTheFormNameIsBlank() {
        proposal.setProjectLeader("   ");
        User account = new User();
        account.setId(9L);
        account.setName("Proponent User");
        proposal.setProponent(account);

        assertEquals("Proponent User", details().getProponentName());
    }

    @Test
    void theAttachedLineItemBudgetDocumentIsCarriedByTheResponse() {
        proposal.setProjectLeader("Andrea Mae Villanueva");
        proposal.setBeneficiariesFileName("Line-Item Budget.pdf");
        proposal.setBeneficiariesFilePath("budget-files/9f8e7d6c_line-item-budget.pdf");

        OvcafProposalResponse response = details();

        assertEquals("Line-Item Budget.pdf", response.getBeneficiariesFileName());
        assertEquals("budget-files/9f8e7d6c_line-item-budget.pdf", response.getBeneficiariesFilePath());
    }

    @Test
    void aProposalWithoutABudgetDocumentReportsNoDocument() {
        OvcafProposalResponse response = details();

        assertNull(response.getBeneficiariesFileName());
        assertNull(response.getBeneficiariesFilePath());
    }

    @Test
    void aDraftIsNotReachableThroughTheOvcafDetailRoute() {
        proposal.setStatus("DRAFT");
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));

        assertThrows(IllegalArgumentException.class, () -> service.getProposalDetails(PROPOSAL_ID));
    }

    @Test
    void aProposalStillInAnEarlierReviewStageIsNotReachable() {
        proposal.setStatus("RPS_REVIEW");
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));

        assertThrows(IllegalArgumentException.class, () -> service.getProposalDetails(PROPOSAL_ID));
    }

    @Test
    void everyStatusInTheOvcafWorkflowCanBeOpened() {
        for (String status : new String[] {
                "FOR_OVCAF_APPROVAL", "APPROVED", "FOR_OC_APPROVAL",
                "READY_FOR_RELEASE", "RELEASED", "RETURNED", "REJECTED" }) {
            proposal.setStatus(status);
            assertEquals(status, details().getStatus(), status + " must remain reviewable");
        }
    }

    private OvcafValidationRequest decision(String value) {
        OvcafValidationRequest request = new OvcafValidationRequest();
        request.setDecision(value);
        return request;
    }

    @Test
    void approvingForImplementationWithoutAnIssuedSoIsRefused() {
        proposal.setSoNumber(null);
        proposal.setSoUploadedAt(null);
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        when(validationRepository.save(any(OvcafValidation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.submitValidation(PROPOSAL_ID, decision("APPROVED_ENDORSED")));
        assertTrue(error.getMessage().contains("Special Order"),
                "the reviewer must be told which requirement is missing");
        assertEquals("FOR_OVCAF_APPROVAL", proposal.getStatus(),
                "a refused approval must not change the workflow status");
        assertNull(proposal.getSoNumber(), "a refused approval must not invent an SO");
    }

    @Test
    void approvingForImplementationWithAnIssuedSoSucceeds() {
        proposal.setSoNumber("SO-2026-117");
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        when(validationRepository.save(any(OvcafValidation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(proposalRepository.save(any(Proposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OvcafValidation validation = service.submitValidation(PROPOSAL_ID, decision("APPROVED_ENDORSED"));

        assertEquals("APPROVED_ENDORSED", validation.getDecision());
        assertEquals("APPROVED", proposal.getStatus());
    }

    @Test
    void returningAProposalMovesItToReturned() {
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        when(validationRepository.save(any(OvcafValidation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(proposalRepository.save(any(Proposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.submitValidation(PROPOSAL_ID, decision("RETURNED_FOR_REVISION"));
        assertEquals("RETURNED", proposal.getStatus());
    }

    @Test
    void rejectingAProposalMovesItToRejected() {
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        when(validationRepository.save(any(OvcafValidation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(proposalRepository.save(any(Proposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.submitValidation(PROPOSAL_ID, decision("REJECTED"));
        assertEquals("REJECTED", proposal.getStatus());
    }

    @Test
    void aReturnWithoutTheOcApprovalRecordIsRefused() {
        proposal.setSoUploadedAt(null);
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.submitValidation(PROPOSAL_ID, decision("RETURNED_FOR_REVISION")));
        assertTrue(error.getMessage().contains("Office of the Chancellor"));
        assertEquals("FOR_OVCAF_APPROVAL", proposal.getStatus());
    }

    @Test
    void anAlreadyDecidedProposalCannotBeValidatedAgain() {
        proposal.setStatus("APPROVED");
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));

        assertThrows(IllegalStateException.class,
                () -> service.submitValidation(PROPOSAL_ID, decision("APPROVED_ENDORSED")));
        assertEquals("APPROVED", proposal.getStatus());
    }

    @Test
    void anUnknownDecisionIsRejected() {
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));

        assertThrows(IllegalArgumentException.class,
                () -> service.submitValidation(PROPOSAL_ID, decision("MAYBE")));
        assertEquals("FOR_OVCAF_APPROVAL", proposal.getStatus());
    }

    @Test
    void thePendingListExcludesAProposalWithoutTheOcApprovalRecord() {
        proposal.setSoNumber(null);
        proposal.setSoUploadedAt(null);
        when(proposalRepository.findByStatus("FOR_OVCAF_APPROVAL")).thenReturn(java.util.List.of(proposal));

        assertTrue(service.getPendingValidationProposals().isEmpty());

        proposal.setSoNumber("SO-2026-117");
        proposal.setSoUploadedAt(java.time.LocalDateTime.of(2026, 8, 25, 15, 10));
        when(validationRepository.findTopByProposalIdOrderByCreatedAtDesc(PROPOSAL_ID))
                .thenReturn(Optional.empty());
        assertEquals(1, service.getPendingValidationProposals().size());
    }
}
