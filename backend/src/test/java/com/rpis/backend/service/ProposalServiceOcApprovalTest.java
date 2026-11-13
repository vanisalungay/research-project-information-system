package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalStatusHistory;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.storage.UploadStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The Stage 1 rules of the OC -&gt; OVCAF workflow, at the service boundary:
 * validation happens before anything is written, the SO record and status change
 * happen together, and a failure leaves neither a changed proposal nor a stray
 * file behind.
 */
class ProposalServiceOcApprovalTest {

    private static final Long PROPOSAL_ID = 42L;
    private static final byte[] PDF = "%PDF-1.4\n% test special order\n".getBytes();

    private ProposalRepository proposalRepository;
    private UploadStorage uploadStorage;
    private CurrentUser currentUser;
    private UserRepository userRepository;
    private NotificationService notificationService;
    private ProposalService service;
    private Proposal proposal;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        uploadStorage = mock(UploadStorage.class);
        currentUser = mock(CurrentUser.class);
        userRepository = mock(UserRepository.class);
        notificationService = mock(NotificationService.class);
        service = new ProposalService(proposalRepository,
                userRepository,
                notificationService,
                mock(ProposalAnnouncementService.class),
                mock(ProposalVersionService.class),
                mock(ProposalSectionRemarkService.class),
                currentUser,
                mock(CooperatingAgencyService.class),
                mock(PriorityAgendaService.class),
                uploadStorage);

        proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        proposal.setProjectTitle("Coastal Research");
        proposal.setStatus("FOR_OC_APPROVAL");
        when(proposalRepository.findByIdForUpdate(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(uploadStorage.store(any(byte[].class), anyString(), anyString()))
                .thenReturn("special-orders/stored_special-order.pdf");
    }

    private MultipartFile pdf(String name) {
        return new MockMultipartFile("soFile", name, "application/pdf", PDF);
    }

    @Test
    void approvingWithBudgetRecordsTheSoAndForwardsToOvcaf() {
        Proposal saved = service.approveWithSpecialOrder(
                PROPOSAL_ID, "  SO-2026-042  ", pdf("SO No. 42.pdf"), true, "Approved.");

        assertEquals("FOR_OVCAF_APPROVAL", saved.getStatus());
        assertEquals("SO-2026-042", saved.getSoNumber());
        assertEquals(Boolean.TRUE, saved.getNeedsBudget());
        assertTrue(saved.getSoUploadedAt() != null, "the OC approval time is the approval record");
        assertEquals("SO No. 42.pdf", saved.getSoFileName());
        assertEquals("special-orders/stored_special-order.pdf", saved.getSoFilePath());
        assertEquals("Approved.", saved.getRemarks());
        verify(proposalRepository, times(1)).save(proposal);
    }

    @Test
    void approvingWithoutBudgetMarksTheProposalApprovedForImplementation() {
        Proposal saved = service.approveWithSpecialOrder(
                PROPOSAL_ID, "SO-2026-043", null, false, null);

        assertEquals("APPROVED", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getNeedsBudget());
        assertNull(saved.getSoFilePath(), "no document was supplied, so no file reference is invented");
        assertEquals("SO-2026-043", saved.getSoNumber());
    }

    @Test
    void aMissingSoNumberIsRejectedBeforeAnythingIsWritten() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.approveWithSpecialOrder(PROPOSAL_ID, "   ", pdf("so.pdf"), true, null));

        assertTrue(error.getMessage().contains("SO"));
        assertEquals("FOR_OC_APPROVAL", proposal.getStatus());
        assertNull(proposal.getSoNumber());
        verify(uploadStorage, never()).store(any(byte[].class), anyString(), anyString());
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void aMissingBudgetDecisionIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.approveWithSpecialOrder(PROPOSAL_ID, "SO-2026-044", null, null, null));
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void anOversizedSoDocumentIsRejectedWithTheConfiguredLimit() {
        byte[] oversized = new byte[(int) service.getSpecialOrderMaxBytes() + 1];
        MultipartFile file = new MockMultipartFile("soFile", "so.pdf", "application/pdf", oversized);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.approveWithSpecialOrder(PROPOSAL_ID, "SO-2026-045", file, true, null));

        assertTrue(error.getMessage().contains("MB"));
        verify(uploadStorage, never()).store(any(byte[].class), anyString(), anyString());
        assertEquals("FOR_OC_APPROVAL", proposal.getStatus());
    }

    @Test
    void aFileThatIsNotReallyAPdfIsRejected() {
        MultipartFile disguised = new MockMultipartFile(
                "soFile", "so.pdf", "application/pdf", "not a pdf at all".getBytes());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.approveWithSpecialOrder(PROPOSAL_ID, "SO-2026-046", disguised, true, null));

        assertTrue(error.getMessage().contains("format"));
        verify(uploadStorage, never()).store(any(byte[].class), anyString(), anyString());
    }

    @Test
    void anUnsupportedExtensionIsRejected() {
        MultipartFile executable = new MockMultipartFile(
                "soFile", "so.exe", "application/octet-stream", PDF);

        assertThrows(IllegalArgumentException.class,
                () -> service.approveWithSpecialOrder(PROPOSAL_ID, "SO-2026-047", executable, true, null));
        verify(uploadStorage, never()).store(any(byte[].class), anyString(), anyString());
    }

    @Test
    void aProposalThatIsNotAwaitingTheChancellorCannotBeApproved() {
        proposal.setStatus("FOR_OVCAF_APPROVAL");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.approveWithSpecialOrder(PROPOSAL_ID, "SO-2026-048", pdf("so.pdf"), true, null));

        assertTrue(error.getMessage().contains("FOR_OC_APPROVAL"));
        verify(uploadStorage, never()).store(any(byte[].class), anyString(), anyString());
        verify(proposalRepository, never()).save(any(Proposal.class));
        assertEquals("FOR_OVCAF_APPROVAL", proposal.getStatus());
    }

    /**
     * The entity is mutated before the write, exactly as inside the transaction; the
     * database change is undone by the rollback (verified at HTTP level in
     * OcOvcafWorkflowApiTest). What this test can and must prove is the filesystem
     * half: the stored document is deleted again, so the failed write leaves no
     * orphaned upload and no proposal can reference it.
     */
    @Test
    void aDatabaseFailureAfterStoringTheFileLeavesNoOrphanedUpload() {
        when(proposalRepository.save(any(Proposal.class)))
                .thenThrow(new RuntimeException("database is down"));

        assertThrows(RuntimeException.class,
                () -> service.approveWithSpecialOrder(PROPOSAL_ID, "SO-2026-049", pdf("so.pdf"), true, null));

        verify(uploadStorage, times(1)).delete("special-orders/stored_special-order.pdf");
        verify(proposalRepository, times(1)).save(proposal);
    }

    @Test
    void aStorageFailureIsReportedAndChangesNothing() {
        when(uploadStorage.store(any(byte[].class), anyString(), anyString()))
                .thenThrow(new IllegalStateException("disk full"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.approveWithSpecialOrder(PROPOSAL_ID, "SO-2026-050", pdf("so.pdf"), true, null));

        assertEquals(500, error.getStatusCode().value());
        assertTrue(error.getReason().contains("could not be stored"));
        verify(proposalRepository, never()).save(any(Proposal.class));
        assertEquals("FOR_OC_APPROVAL", proposal.getStatus());
        assertNull(proposal.getSoNumber());
        assertTrue(proposal.getSoUploadedAt() == null);
    }

    /**
     * A retry after a failed attempt is a fresh operation: the first attempt
     * compensated its stored file and recorded nothing, and the retry records the
     * SO exactly once.
     */
    @Test
    void aRetryAfterAFailureRecordsTheSoOnce() {
        when(proposalRepository.save(any(Proposal.class)))
                .thenThrow(new RuntimeException("transient database error"))
                .thenAnswer(inv -> inv.getArgument(0));

        assertThrows(RuntimeException.class, () -> service.approveWithSpecialOrder(
                PROPOSAL_ID, "SO-2026-051", pdf("so.pdf"), true, null));
        verify(uploadStorage, times(1)).delete("special-orders/stored_special-order.pdf");

        // In Spring the failed transaction rolls the entity back; simulate that here.
        proposal.setStatus("FOR_OC_APPROVAL");
        proposal.setSoNumber(null);
        proposal.setSoUploadedAt(null);
        proposal.setSoFilePath(null);
        proposal.setSoFileName(null);

        Proposal saved = service.approveWithSpecialOrder(
                PROPOSAL_ID, "SO-2026-051", pdf("so.pdf"), true, null);

        assertEquals("FOR_OVCAF_APPROVAL", saved.getStatus());
        assertEquals("SO-2026-051", saved.getSoNumber());
        verify(proposalRepository, times(2)).save(proposal);
    }

    /**
     * Once the first approval committed, the proposal is no longer at
     * FOR_OC_APPROVAL. A second (double-clicked or retried) request is refused and
     * cannot overwrite the recorded SO or store a second file.
     */
    @Test
    void aDuplicateApprovalIsRefused() {
        service.approveWithSpecialOrder(PROPOSAL_ID, "SO-2026-052", pdf("so.pdf"), true, null);
        when(uploadStorage.store(any(byte[].class), anyString(), anyString()))
                .thenReturn("special-orders/second_special-order.pdf");

        assertThrows(IllegalStateException.class,
                () -> service.approveWithSpecialOrder(PROPOSAL_ID, "SO-DIFFERENT", pdf("other.pdf"), true, null));

        assertEquals("SO-2026-052", proposal.getSoNumber());
        assertEquals("special-orders/stored_special-order.pdf", proposal.getSoFilePath());
        verify(proposalRepository, times(1)).save(proposal);
        verify(uploadStorage, times(1)).store(any(byte[].class), anyString(), anyString());
    }

    @Test
    void aMissingProposalIsReportedAsNotFound() {
        when(proposalRepository.findByIdForUpdate(eq(404L))).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.approveWithSpecialOrder(404L, "SO-2026-053", null, true, null));

        assertEquals(404, error.getStatusCode().value());
    }

    /**
     * Every approval writes exactly one audited transition, with the office that made
     * it and its notes - this is what the proposal history page reads.
     */
    @Test
    void anApprovalRecordsOneAuditedStatusTransitionWithItsActor() {
        when(currentUser.getUserId()).thenReturn(5L);
        when(currentUser.getRole()).thenReturn("OC");
        User actor = new User();
        actor.setName("The Chancellor");
        when(userRepository.findById(5L)).thenReturn(Optional.of(actor));

        Proposal saved = service.approveWithSpecialOrder(
                PROPOSAL_ID, "SO-2026-060", null, false, "  Approved.  ");

        assertEquals(1, saved.getStatusHistory().size(), "one transition per approval");
        ProposalStatusHistory entry = saved.getStatusHistory().get(0);
        assertEquals("FOR_OC_APPROVAL", entry.getFromStatus());
        assertEquals("APPROVED", entry.getToStatus());
        assertEquals(5L, entry.getActorUserId());
        assertEquals("OC", entry.getActorRole());
        assertEquals("The Chancellor", entry.getActorName());
        assertEquals("Approved.", entry.getRemarks());
        assertEquals(ProposalStatusHistory.SOURCE_APPLICATION, entry.getSource());
        assertSame(saved, entry.getProposal());
        assertNotNull(entry.getChangedAt());
    }

    /**
     * The Chancellor's WITHOUT BUDGET classification sends the proposal straight to
     * implementation. Finance must not receive the "ready for budget endorsement"
     * notification, because no budget was recorded for it.
     */
    @Test
    void aWithoutBudgetApprovalDoesNotAskFinanceToEndorseABudget() {
        User finance = new User();
        finance.setId(9L);
        finance.setRole("OVCAF");
        when(userRepository.findByRoleIn(List.of("OVCAF"))).thenReturn(List.of(finance));

        service.approveWithSpecialOrder(PROPOSAL_ID, "SO-2026-062", null, false, null);

        verify(notificationService, never()).createNotification(
                eq(9L), anyString(), anyString(), anyString(), any());
    }

    /**
     * The WITH BUDGET route still puts the proposal in front of Finance, so the
     * without-budget guard must not silence the OVCAF review notification.
     */
    @Test
    void aWithBudgetApprovalStillNotifiesFinanceToReviewTheProposal() {
        User finance = new User();
        finance.setId(9L);
        finance.setRole("OVCAF");
        when(userRepository.findByRoleIn(List.of("OVCAF"))).thenReturn(List.of(finance));

        service.approveWithSpecialOrder(PROPOSAL_ID, "SO-2026-063", null, true, null);

        verify(notificationService).createNotification(
                eq(9L), anyString(), eq("Proposal for OVCAF Review"), eq("ENDORSEMENT"), eq(PROPOSAL_ID));
    }
}
