package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.rpis.backend.storage.LocalUploadStorage;

/**
 * Unit tests for the Line-Item Budget file upload of the Create Proposal form
 * (section 25).
 * <p>
 * They cover the rules the form relies on: only a real PDF or DOCX is stored (a
 * text file renamed to .pdf is rejected), the 10 MB limit is enforced again on the
 * server, the document is attached to the caller's own proposal only, the
 * proponent's own file name is kept for display while the stored file gets a
 * generated name, and replacing the file removes the previous one.
 * <p>
 * No database is required: the repositories are mocked and every file the service
 * writes is removed again after each test.
 */
class ProposalBudgetFileServiceTest {

    private static final Long PROPOSAL_ID = 42L;
    private static final String PDF_TYPE = "application/pdf";
    private static final String DOCX_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private ProposalRepository proposalRepository;
    private CurrentUser currentUser;
    private ProposalService service;
    private Proposal proposal;
    private final List<String> storedPaths = new ArrayList<>();

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        currentUser = mock(CurrentUser.class);
        service = new ProposalService(
                proposalRepository,
                mock(UserRepository.class),
                mock(NotificationService.class),
                mock(ProposalAnnouncementService.class),
                mock(ProposalVersionService.class),
                mock(ProposalSectionRemarkService.class),
                currentUser,
                mock(CooperatingAgencyService.class),
                mock(PriorityAgendaService.class), new LocalUploadStorage("uploads"));

        proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void removeStoredFiles() throws IOException {
        for (String stored : storedPaths) {
            Files.deleteIfExists(Paths.get(stored));
        }
        storedPaths.clear();
    }

    /** Tracks the file the service just wrote so the test can clean it up. */
    private void rememberStoredFile() {
        if (proposal.getBeneficiariesFilePath() != null) {
            storedPaths.add("uploads/" + proposal.getBeneficiariesFilePath());
        }
    }

    /** The file on disk of a stored path ("budget-files/x.pdf"). */
    private Path storedFile(String storedPath) {
        return Paths.get("uploads", storedPath);
    }

    /** A PDF: "%PDF-1.4" followed by filler bytes. */
    private byte[] pdfBytes(int totalLength) {
        byte[] bytes = new byte[totalLength];
        byte[] header = { 0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e, 0x34 };
        System.arraycopy(header, 0, bytes, 0, header.length);
        return bytes;
    }

    /** A DOCX: an OOXML package, i.e. a ZIP archive ("PK" + the local file header). */
    private byte[] docxBytes(int totalLength) {
        byte[] bytes = new byte[totalLength];
        byte[] header = { 0x50, 0x4b, 0x03, 0x04, 0x14, 0x00 };
        System.arraycopy(header, 0, bytes, 0, header.length);
        return bytes;
    }

    private MockMultipartFile upload(String fileName, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", fileName, contentType, bytes);
    }

    @Test
    void storesARealPdfOnTheProposal() throws IOException {
        MockMultipartFile file = upload("Line-Item Budget.pdf", PDF_TYPE, pdfBytes(2048));

        Proposal saved = service.attachBudgetFile(PROPOSAL_ID, file);
        rememberStoredFile();

        assertEquals("Line-Item Budget.pdf", saved.getBeneficiariesFileName());
        assertNotNull(saved.getBeneficiariesFilePath());
        assertTrue(saved.getBeneficiariesFilePath().startsWith("budget-files/"));
        assertTrue(saved.getBeneficiariesFilePath().endsWith("_line-item-budget.pdf"));
        // The generated name never reuses the client supplied name, so two uploads of
        // the same file cannot overwrite each other.
        assertFalse(saved.getBeneficiariesFilePath().contains("Line-Item Budget.pdf"));
        assertTrue(Files.exists(storedFile(saved.getBeneficiariesFilePath())));
        assertEquals(2048, Files.size(storedFile(saved.getBeneficiariesFilePath())));
    }

    @Test
    void storesARealDocxOnTheProposal() throws IOException {
        MockMultipartFile file = upload("Line-Item Budget.docx", DOCX_TYPE, docxBytes(4096));

        Proposal saved = service.attachBudgetFile(PROPOSAL_ID, file);
        rememberStoredFile();

        assertEquals("Line-Item Budget.docx", saved.getBeneficiariesFileName());
        assertTrue(saved.getBeneficiariesFilePath().endsWith("_line-item-budget.docx"));
        assertTrue(Files.exists(storedFile(saved.getBeneficiariesFilePath())));
    }

    @Test
    void acceptsADocxThatIsDeclaredAsAZipPackage() throws IOException {
        MockMultipartFile file = upload("budget.docx", "application/x-zip-compressed", docxBytes(512));

        Proposal saved = service.attachBudgetFile(PROPOSAL_ID, file);
        rememberStoredFile();

        assertTrue(saved.getBeneficiariesFilePath().endsWith("_line-item-budget.docx"));
    }

    @Test
    void acceptsAFileOfExactlyTenMegabytes() throws IOException {
        // 10 MB is the limit itself, not one byte below it.
        MockMultipartFile file = upload("limit.pdf", PDF_TYPE,
                pdfBytes((int) ProposalService.BUDGET_FILE_MAX_BYTES));

        Proposal saved = service.attachBudgetFile(PROPOSAL_ID, file);
        rememberStoredFile();

        assertEquals("limit.pdf", saved.getBeneficiariesFileName());
        assertEquals(ProposalService.BUDGET_FILE_MAX_BYTES,
                Files.size(storedFile(saved.getBeneficiariesFilePath())));
    }

    @Test
    void rejectsAFileLargerThanTenMegabytes() {
        MockMultipartFile file = upload("big.pdf", PDF_TYPE,
                pdfBytes((int) ProposalService.BUDGET_FILE_MAX_BYTES + 1));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.attachBudgetFile(PROPOSAL_ID, file));

        assertEquals("File size exceeds the 10 MB limit. Please upload a smaller file.",
                error.getMessage());
        assertNull(proposal.getBeneficiariesFilePath());
        assertNull(proposal.getBeneficiariesFileName());
    }

    @Test
    void rejectsAnUnsupportedExtension() {
        for (String name : new String[] { "scan.png", "sheet.xlsx", "report.doc", "notes.txt" }) {
            MockMultipartFile file = upload(name, "application/octet-stream", pdfBytes(64));

            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> service.attachBudgetFile(PROPOSAL_ID, file),
                    name + " should be rejected");

            assertEquals("Invalid file format. Please upload a PDF or DOCX file.", error.getMessage());
        }
        assertNull(proposal.getBeneficiariesFilePath());
    }

    @Test
    void rejectsADeclaredMediaTypeOfAnotherFormat() {
        MockMultipartFile file = upload("budget.docx", "image/png", docxBytes(64));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.attachBudgetFile(PROPOSAL_ID, file));

        assertEquals("Invalid file format. Please upload a PDF or DOCX file.", error.getMessage());
    }

    @Test
    void rejectsAFileWhoseBytesAreNotTheFormatItsNameClaims() {
        // An image renamed to ".pdf" and a PDF renamed to ".docx" pass the extension
        // and media type checks, so only the content check can catch them.
        byte[] png = { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
        MockMultipartFile fakePdf = upload("budget.pdf", PDF_TYPE, png);
        MockMultipartFile fakeDocx = upload("budget.docx", DOCX_TYPE, pdfBytes(64));
        MockMultipartFile text = upload("budget.pdf", PDF_TYPE,
                "not a budget document".getBytes(StandardCharsets.UTF_8));

        for (MockMultipartFile file : new MockMultipartFile[] { fakePdf, fakeDocx, text }) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> service.attachBudgetFile(PROPOSAL_ID, file));

            assertEquals("Invalid file format. Please upload a PDF or DOCX file.", error.getMessage());
        }
        assertNull(proposal.getBeneficiariesFilePath());
    }

    @Test
    void rejectsAnEmptyUpload() {
        MockMultipartFile file = upload("empty.pdf", PDF_TYPE, new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> service.attachBudgetFile(PROPOSAL_ID, file));
    }

    @Test
    void replacingTheFileKeepsTheNewOneAndRemovesTheOldFile() throws IOException {
        service.attachBudgetFile(PROPOSAL_ID, upload("first.pdf", PDF_TYPE, pdfBytes(128)));
        Path first = storedFile(proposal.getBeneficiariesFilePath());

        service.attachBudgetFile(PROPOSAL_ID, upload("second.docx", DOCX_TYPE, docxBytes(128)));
        rememberStoredFile();

        assertEquals("second.docx", proposal.getBeneficiariesFileName());
        assertTrue(proposal.getBeneficiariesFilePath().endsWith("_line-item-budget.docx"));
        assertFalse(Files.exists(first), "the replaced file should have been deleted");
        assertTrue(Files.exists(storedFile(proposal.getBeneficiariesFilePath())));
    }

    @Test
    void theFileCanOnlyBeAttachedToTheCallersOwnProposal() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "not your proposal"))
                .when(currentUser).requireAccessTo(any(Proposal.class));

        assertThrows(ResponseStatusException.class, () -> service.attachBudgetFile(PROPOSAL_ID,
                upload("budget.pdf", PDF_TYPE, pdfBytes(64))));
    }

    @Test
    void aFileOfAnUnknownProposalIsReported() {
        when(proposalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.attachBudgetFile(999L,
                upload("budget.pdf", PDF_TYPE, pdfBytes(64))));
    }
}
