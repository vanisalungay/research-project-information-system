package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.storage.LocalUploadStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Status validation on the direct status-update path. The generic service method must
 * accept every status the RPIS workflow actually writes (RPS, REC, OVCAF, OC and the
 * reporting workflow), keep the legacy SUBMITTED alias, and reject anything else before
 * the proposal row is changed.
 */
class ProposalStatusUpdateServiceTest {

    private static final Long PROPOSAL_ID = 5L;
    private static final Long ACTOR_ID = 2L;

    private ProposalRepository proposalRepository;
    private ProposalService service;
    private Proposal proposal;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        service = new ProposalService(proposalRepository, userRepository,
                mock(NotificationService.class), mock(ProposalAnnouncementService.class),
                mock(ProposalVersionService.class), mock(ProposalSectionRemarkService.class),
                new CurrentUser(), mock(CooperatingAgencyService.class),
                mock(PriorityAgendaService.class), new LocalUploadStorage("uploads"));

        proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        proposal.setProjectTitle("Status validation");
        proposal.setStatus("DRAFT");
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ACTOR_ID, null,
                        List.of(new SimpleGrantedAuthority("ROLE_RPS_STAFF"))));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aDraftStatusIsAccepted() {
        assertEquals("DRAFT", service.updateProposalStatus(PROPOSAL_ID, " draft ").getStatus());
    }

    @Test
    void theForReviewStatusIsAccepted() {
        assertEquals("FOR REVIEW",
                service.updateProposalStatus(PROPOSAL_ID, "FOR REVIEW").getStatus());
    }

    @Test
    void theLegacySubmittedAliasIsStoredAsForReview() {
        assertEquals("FOR REVIEW", service.updateProposalStatus(PROPOSAL_ID, "SUBMITTED").getStatus());
    }

    @Test
    void statusesWrittenByTheWorkflowAreAccepted() {
        List<String> workflowStatuses = List.of(
                "ENDORSED",              // RPS
                "UNDER_REVIEW",          // RPS forwarding to REC
                "REC_APPROVED",          // REC / RPS
                "OVC_APPROVED",          // OVCRIGE / OVC
                "FOR_OC_APPROVAL",       // OVC forwarding to OC
                "FOR_OVCAF_APPROVAL",    // OC with budget
                "READY_FOR_RELEASE",     // OVCAF
                "APPROVED",              // OC / OVCAF
                "COMPLETED");            // reporting workflow

        for (String status : workflowStatuses) {
            proposal.setStatus("DRAFT");
            assertEquals(status, service.updateProposalStatus(PROPOSAL_ID, status).getStatus());
        }
    }

    @Test
    void anUnknownStatusIsRejectedBeforeTheProposalIsChangedOrSaved() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.updateProposalStatus(PROPOSAL_ID, "NOT_A_PROPOSAL_STATUS"));

        assertTrue(error.getMessage().contains("NOT_A_PROPOSAL_STATUS"), error.getMessage());
        assertEquals("DRAFT", proposal.getStatus(), "the proposal is left untouched");
        verify(proposalRepository, never()).save(any(Proposal.class));
        verify(proposalRepository, never()).findById(PROPOSAL_ID);
    }

    @Test
    void aRevisionStatusStillCannotBeSetThroughTheGenericPath() {
        assertThrows(IllegalStateException.class,
                () -> service.updateProposalStatus(PROPOSAL_ID, "RPS_RETURNED"));
        assertEquals("DRAFT", proposal.getStatus());
        verify(proposalRepository, never()).save(any(Proposal.class));
    }
}
