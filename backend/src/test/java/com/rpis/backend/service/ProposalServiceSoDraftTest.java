package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.storage.UploadStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * The SO draft rules, at the service boundary. A draft records what OC has entered
 * so far, but it is not an OC approval: it must leave the status and
 * {@code soUploadedAt} untouched (OVCAF treats both as proof the approval
 * completed), never notify downstream roles, and never stage a file it cannot
 * reference or leave an orphaned upload behind.
 */
class ProposalServiceSoDraftTest {

    private static final Long PROPOSAL_ID = 42L;
    private static final byte[] PDF = "%PDF-1.4\n% test special order draft\n".getBytes();
    private static final String STORED_PATH = "special-orders/stored_special-order.pdf";

    private ProposalRepository proposalRepository;
    private UploadStorage uploadStorage;
    private NotificationService notificationService;
    private ProposalService service;
    private Proposal proposal;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        uploadStorage = mock(UploadStorage.class);
        notificationService = mock(NotificationService.class);
        service = new ProposalService(proposalRepository,
                mock(UserRepository.class),
                notificationService,
                mock(ProposalAnnouncementService.class),
                mock(ProposalVersionService.class),
                mock(ProposalSectionRemarkService.class),
                mock(CurrentUser.class),
                mock(CooperatingAgencyService.class),
                mock(PriorityAgendaService.class),
                uploadStorage);

        proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        proposal.setProjectTitle("Coastal Research");
        proposal.setStatus("FOR_OC_APPROVAL");
        when(proposalRepository.findByIdForUpdate(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(uploadStorage.store(any(byte[].class), anyString(), anyString())).thenReturn(STORED_PATH);
    }

    private MultipartFile pdf(String name) {
        return new MockMultipartFile("soFile", name, "application/pdf", PDF);
    }

    @Test
    void savingADraftRecordsTheEnteredValuesWithoutCompletingTheApproval() {
        Proposal saved = service.saveSpecialOrderDraft(
                PROPOSAL_ID, "  SO-2026-100  ", pdf("Draft SO.pdf"), true, "  Working notes.  ", false);

        assertEquals("FOR_OC_APPROVAL", saved.getStatus(), "a draft must not advance the proposal");
        assertNull(saved.getSoUploadedAt(), "a draft must not look like an issued Special Order");
        assertEquals("SO-2026-100", saved.getSoNumber(), "the SO number must be trimmed and kept");
        assertEquals(Boolean.TRUE, saved.getNeedsBudget());
        assertEquals("Draft SO.pdf", saved.getSoFileName());
        assertEquals(STORED_PATH, saved.getSoFilePath());
        assertEquals("Working notes.", saved.getRemarks());
        verify(proposalRepository, times(1)).save(proposal);
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }

    @Test
    void aNumberOnlyDraftIsSaved() {
        Proposal saved = service.saveSpecialOrderDraft(
                PROPOSAL_ID, "SO-2026-101", null, null, null, false);

        assertEquals("SO-2026-101", saved.getSoNumber());
        assertNull(saved.getSoFilePath(), "no document was supplied, so no file reference is invented");
        assertNull(saved.getSoUploadedAt());
        assertEquals("FOR_OC_APPROVAL", saved.getStatus());
        verify(proposalRepository, times(1)).save(proposal);
    }

    @Test
    void aDocumentOnlyDraftIsSaved() {
        Proposal saved = service.saveSpecialOrderDraft(
                PROPOSAL_ID, null, pdf("Scanned SO.pdf"), null, null, false);

        assertNull(saved.getSoNumber());
        assertEquals("Scanned SO.pdf", saved.getSoFileName());
        assertEquals(STORED_PATH, saved.getSoFilePath());
        assertNull(saved.getSoUploadedAt());
        assertEquals("FOR_OC_APPROVAL", saved.getStatus());
    }

    @Test
    void aDraftWithNeitherNumberNorDocumentIsRejected() {
        IllegalArgumentException numberless = assertThrows(IllegalArgumentException.class,
                () -> service.saveSpecialOrderDraft(PROPOSAL_ID, "   ", null, true, "Notes only", false));
        assertTrue(numberless.getMessage().contains("SO number or attach"),
                "the message must explain what a draft needs");

        IllegalArgumentException budgetOnly = assertThrows(IllegalArgumentException.class,
                () -> service.saveSpecialOrderDraft(PROPOSAL_ID, null, null, false, null, false));
        assertEquals(ProposalService.SPECIAL_ORDER_DRAFT_EMPTY_MESSAGE, budgetOnly.getMessage());

        assertNull(proposal.getNeedsBudget(), "budget-only input must not be written");
        assertNull(proposal.getRemarks(), "notes-only input must not be written");
        verify(uploadStorage, never()).store(any(byte[].class), anyString(), anyString());
        verify(proposalRepository, never()).save(any(Proposal.class));
        assertEquals("FOR_OC_APPROVAL", proposal.getStatus());
    }

    @Test
    void aProposalThatIsNotAwaitingTheChancellorCannotBeDrafted() {
        proposal.setStatus("FOR_OVCAF_APPROVAL");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.saveSpecialOrderDraft(PROPOSAL_ID, "SO-2026-102", pdf("so.pdf"), true, null, false));

        assertTrue(error.getMessage().contains("FOR_OC_APPROVAL"));
        verify(uploadStorage, never()).store(any(byte[].class), anyString(), anyString());
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void anOversizedDocumentIsRejectedBeforeAnythingIsWritten() {
        byte[] oversized = new byte[(int) service.getSpecialOrderMaxBytes() + 1];
        MultipartFile file = new MockMultipartFile("soFile", "so.pdf", "application/pdf", oversized);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveSpecialOrderDraft(PROPOSAL_ID, "SO-2026-103", file, true, null, false));

        assertTrue(error.getMessage().contains("MB"));
        verify(uploadStorage, never()).store(any(byte[].class), anyString(), anyString());
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void aFileThatIsNotReallyAPdfIsRejected() {
        MultipartFile disguised = new MockMultipartFile(
                "soFile", "so.pdf", "application/pdf", "not a pdf at all".getBytes());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveSpecialOrderDraft(PROPOSAL_ID, "SO-2026-104", disguised, true, null, false));

        assertTrue(error.getMessage().contains("format"));
        verify(uploadStorage, never()).store(any(byte[].class), anyString(), anyString());
    }

    @Test
    void anSoNumberLongerThanTheColumnIsRejected() {
        String tooLong = "S".repeat(101);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveSpecialOrderDraft(PROPOSAL_ID, tooLong, null, null, null, false));

        assertEquals(ProposalService.SPECIAL_ORDER_NUMBER_TOO_LONG_MESSAGE, error.getMessage());
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void savingAgainUpdatesTheDraftAndStillDoesNotFinalizeIt() {
        service.saveSpecialOrderDraft(PROPOSAL_ID, "SO-2026-105", null, true, "First", false);

        Proposal saved = service.saveSpecialOrderDraft(
                PROPOSAL_ID, "SO-2026-106", null, false, "Second", false);

        assertEquals("SO-2026-106", saved.getSoNumber());
        assertEquals(Boolean.FALSE, saved.getNeedsBudget());
        assertEquals("Second", saved.getRemarks());
        assertNull(saved.getSoUploadedAt());
        assertEquals("FOR_OC_APPROVAL", saved.getStatus());
        verify(proposalRepository, times(2)).save(proposal);
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }

    @Test
    void aFirstSaveWithNoNotesKeepsEarlierNonDraftRemarks() {
        // A return-for-revision comment may already sit in `remarks`; the first
        // draft save must not erase text it does not own.
        proposal.setRemarks("Please revise the budget.");

        Proposal saved = service.saveSpecialOrderDraft(
                PROPOSAL_ID, "SO-2026-107", null, null, "   ", false);

        assertEquals("Please revise the budget.", saved.getRemarks());
    }

    @Test
    void aLaterSaveWithNoNotesClearsTheDraftNotes() {
        service.saveSpecialOrderDraft(PROPOSAL_ID, "SO-2026-108", null, null, "Draft notes", false);

        Proposal saved = service.saveSpecialOrderDraft(
                PROPOSAL_ID, "SO-2026-108", null, null, "   ", false);

        assertNull(saved.getRemarks(), "clearing the notes box must clear the saved draft notes");
    }

    @Test
    void replacingTheStagedDocumentDeletesTheOldFile() {
        proposal.setSoNumber("SO-2026-109");
        proposal.setSoFileName("old.pdf");
        proposal.setSoFilePath("special-orders/old_special-order.pdf");

        Proposal saved = service.saveSpecialOrderDraft(
                PROPOSAL_ID, "SO-2026-109", pdf("new.pdf"), null, null, false);

        assertEquals("new.pdf", saved.getSoFileName());
        assertEquals(STORED_PATH, saved.getSoFilePath());
        verify(uploadStorage, times(1)).delete("special-orders/old_special-order.pdf");
        assertNull(saved.getSoUploadedAt());
    }

    @Test
    void removingTheStagedDocumentClearsIt() {
        proposal.setSoNumber("SO-2026-110");
        proposal.setSoFileName("old.pdf");
        proposal.setSoFilePath("special-orders/old_special-order.pdf");

        Proposal saved = service.saveSpecialOrderDraft(
                PROPOSAL_ID, "SO-2026-110", null, null, null, true);

        assertNull(saved.getSoFileName());
        assertNull(saved.getSoFilePath());
        verify(uploadStorage, times(1)).delete("special-orders/old_special-order.pdf");
        assertNull(saved.getSoUploadedAt());
    }

    @Test
    void removingTheOnlyDocumentWithoutANumberIsRejected() {
        proposal.setSoFilePath("special-orders/old_special-order.pdf");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveSpecialOrderDraft(PROPOSAL_ID, "   ", null, null, null, true));

        assertEquals(ProposalService.SPECIAL_ORDER_DRAFT_EMPTY_MESSAGE, error.getMessage());
        assertEquals("special-orders/old_special-order.pdf", proposal.getSoFilePath(),
                "a refused draft must not remove the stored document");
        verify(uploadStorage, never()).delete(anyString());
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void aDatabaseFailureAfterStoringTheFileLeavesNoOrphanedUpload() {
        when(proposalRepository.save(any(Proposal.class)))
                .thenThrow(new RuntimeException("database is down"));

        assertThrows(RuntimeException.class,
                () -> service.saveSpecialOrderDraft(PROPOSAL_ID, "SO-2026-111", pdf("so.pdf"), true, null, false));

        verify(uploadStorage, times(1)).delete(STORED_PATH);
        verify(proposalRepository, times(1)).save(proposal);
    }

    @Test
    void aStorageFailureIsReportedAndChangesNothing() {
        when(uploadStorage.store(any(byte[].class), anyString(), anyString()))
                .thenThrow(new IllegalStateException("disk full"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.saveSpecialOrderDraft(PROPOSAL_ID, "SO-2026-112", pdf("so.pdf"), true, null, false));

        assertEquals(500, error.getStatusCode().value());
        assertTrue(error.getReason().contains("could not be stored"));
        verify(proposalRepository, never()).save(any(Proposal.class));
        assertNull(proposal.getSoNumber());
        assertNull(proposal.getSoUploadedAt());
        assertEquals("FOR_OC_APPROVAL", proposal.getStatus());
    }

    @Test
    void aMissingProposalIsReportedAsNotFound() {
        when(proposalRepository.findByIdForUpdate(eq(404L))).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.saveSpecialOrderDraft(404L, "SO-2026-113", null, null, null, false));

        assertEquals(404, error.getStatusCode().value());
    }
}
