package com.rpis.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.storage.LocalUploadStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The endorsement document upload of the Certification &amp; Endorsement step ("Endorsed
 * By").
 *
 * <p>The rules the form relies on are enforced again here, on the real bytes: only a PDF
 * is stored - a PNG or a DOCX, and an image renamed to {@code .pdf}, are refused because
 * the content is not a PDF - the 10 MB limit is applied, a document that merely starts
 * with the PDF header but cannot be opened (a corrupted file) is refused too, the document
 * is attached to the caller's own proposal only, the proponent's own file name is kept for
 * display while the stored file gets a generated name, and replacing the document removes
 * the previous file.</p>
 *
 * <p>No database is required: the repositories are mocked and every file the service
 * writes is removed again after each test.</p>
 */
class ProposalEndorsementDocumentServiceTest {

    private static final Long PROPOSAL_ID = 55L;
    private static final String PDF_TYPE = "application/pdf";

    private ProposalRepository proposalRepository;
    private ProposalService service;
    private Proposal proposal;
    private final List<String> storedPaths = new ArrayList<>();

    @BeforeEach
    void setUp() {
        proposalRepository = mock(ProposalRepository.class);
        service = new ProposalService(
                proposalRepository,
                mock(UserRepository.class),
                mock(NotificationService.class),
                mock(ProposalAnnouncementService.class),
                mock(ProposalVersionService.class),
                mock(ProposalSectionRemarkService.class),
                mock(CurrentUser.class),
                mock(CooperatingAgencyService.class),
                mock(PriorityAgendaService.class), new LocalUploadStorage("uploads"));

        proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        when(proposalRepository.findById(PROPOSAL_ID)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void removeStoredFiles() throws Exception {
        for (String stored : storedPaths) {
            Files.deleteIfExists(Paths.get(stored));
        }
        storedPaths.clear();
    }

    /** Tracks the file the service just wrote so the test can clean it up. */
    private void rememberStoredFile() {
        if (proposal.getEndorsementFilePath() != null) {
            storedPaths.add("uploads/" + proposal.getEndorsementFilePath());
        }
    }

    /** A PDF the service can really open, generated with the same library it uses. */
    private byte[] validPdfBytes() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph("Endorsement of the project"));
        document.close();
        return out.toByteArray();
    }

    /** The "%PDF-" header followed by garbage: a file the reader cannot open. */
    private byte[] corruptedPdfBytes() {
        byte[] bytes = new byte[64];
        byte[] header = { 0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e, 0x34 };
        System.arraycopy(header, 0, bytes, 0, header.length);
        return bytes;
    }

    private byte[] pngBytes() {
        return new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3 };
    }

    private MockMultipartFile upload(String fileName, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", fileName, contentType, bytes);
    }

    // ===================== a valid PDF is stored =====================

    @Test
    void aValidPdfIsStoredAndRecordedOnTheProposal() throws Exception {
        Proposal saved = service.attachEndorsementDocument(PROPOSAL_ID,
                upload("Dean Endorsement Letter.pdf", PDF_TYPE, validPdfBytes()));

        assertEquals("Dean Endorsement Letter.pdf", saved.getEndorsementFileName(),
                "the proponent's own file name is kept for display");
        assertNotNull(saved.getEndorsementFilePath());
        assertTrue(saved.getEndorsementFilePath().startsWith("endorsement-documents/"),
                "the document is stored in its own folder: " + saved.getEndorsementFilePath());
        assertEquals(saved.getEndorsementFilePath(), proposal.getEndorsementFilePath());

        rememberStoredFile();
        assertTrue(Files.exists(Paths.get("uploads", saved.getEndorsementFilePath())),
                "the bytes are on disk, so the authorized download route can serve them");
    }

    @Test
    void theStoredFileIsAPdfAndNotTheProponentFileName() throws Exception {
        Proposal saved = service.attachEndorsementDocument(PROPOSAL_ID,
                upload("my letter.pdf", PDF_TYPE, validPdfBytes()));

        String stored = Paths.get("uploads", saved.getEndorsementFilePath()).getFileName().toString();
        assertTrue(stored.endsWith(".pdf"), stored);
        assertFalse(stored.contains("my letter"), "the stored name is generated: " + stored);
        rememberStoredFile();
    }

    @Test
    void aGenericOrMissingDeclaredTypeIsAcceptedWhenTheBytesAreAPdf() throws Exception {
        Proposal saved = service.attachEndorsementDocument(PROPOSAL_ID,
                upload("letter.pdf", null, validPdfBytes()));
        assertNotNull(saved.getEndorsementFilePath());
        rememberStoredFile();

        Proposal again = service.attachEndorsementDocument(PROPOSAL_ID,
                upload("letter.pdf", "application/octet-stream", validPdfBytes()));
        assertNotNull(again.getEndorsementFilePath());
        rememberStoredFile();
    }

    // ===================== anything that is not a PDF is refused =====================

    @Test
    void aPngOrJpgIsRefused() {
        IllegalArgumentException png = assertThrows(IllegalArgumentException.class,
                () -> service.attachEndorsementDocument(PROPOSAL_ID,
                        upload("endorsement.png", "image/png", pngBytes())));
        assertEquals(ProposalService.ENDORSEMENT_DOCUMENT_FORMAT_MESSAGE, png.getMessage());

        IllegalArgumentException jpg = assertThrows(IllegalArgumentException.class,
                () -> service.attachEndorsementDocument(PROPOSAL_ID,
                        upload("endorsement.jpg", "image/jpeg", pngBytes())));
        assertEquals(ProposalService.ENDORSEMENT_DOCUMENT_FORMAT_MESSAGE, jpg.getMessage());
        assertNull(proposal.getEndorsementFilePath(), "nothing was stored for a refused upload");
    }

    @Test
    void anImageRenamedToPdfIsRefusedOnItsContent() {
        // The name and the declared type both claim PDF; only the bytes can catch it.
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.attachEndorsementDocument(PROPOSAL_ID,
                        upload("endorsement.pdf", PDF_TYPE, pngBytes())));
        assertEquals(ProposalService.ENDORSEMENT_DOCUMENT_FORMAT_MESSAGE, error.getMessage());
        assertNull(proposal.getEndorsementFilePath());
    }

    @Test
    void aWordDocumentOrAnyOtherExtensionIsRefused() throws Exception {
        for (String name : new String[] { "endorsement.docx", "endorsement.doc", "endorsement", "endorsement.txt" }) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> service.attachEndorsementDocument(PROPOSAL_ID,
                            upload(name, "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    validPdfBytes())));
            assertEquals(ProposalService.ENDORSEMENT_DOCUMENT_FORMAT_MESSAGE, error.getMessage(), name);
        }
    }

    @Test
    void aDeclaredImageTypeIsRefusedEvenWhenTheBytesAreAPdf() throws Exception {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.attachEndorsementDocument(PROPOSAL_ID,
                        upload("endorsement.pdf", "image/png", validPdfBytes())));
        assertEquals(ProposalService.ENDORSEMENT_DOCUMENT_FORMAT_MESSAGE, error.getMessage());
    }

    @Test
    void aCorruptedPdfIsRefusedBecauseItCannotBeOpened() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.attachEndorsementDocument(PROPOSAL_ID,
                        upload("endorsement.pdf", PDF_TYPE, corruptedPdfBytes())));
        assertEquals(ProposalService.ENDORSEMENT_DOCUMENT_FORMAT_MESSAGE, error.getMessage());
        assertNull(proposal.getEndorsementFilePath());
    }

    @Test
    void aFileThatIsNotAPdfAtAllIsRefusedBeforeItIsRead() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.attachEndorsementDocument(PROPOSAL_ID,
                        upload("endorsement.pdf", PDF_TYPE, "not a pdf".getBytes(StandardCharsets.UTF_8))));
        assertEquals(ProposalService.ENDORSEMENT_DOCUMENT_FORMAT_MESSAGE, error.getMessage());
    }

    @Test
    void theSizeLimitAndTheEmptyUploadAreReportedWithTheirOwnMessage() {
        byte[] tooBig = new byte[(int) ProposalService.ENDORSEMENT_DOCUMENT_MAX_BYTES + 1];
        IllegalArgumentException size = assertThrows(IllegalArgumentException.class,
                () -> service.attachEndorsementDocument(PROPOSAL_ID, upload("big.pdf", PDF_TYPE, tooBig)));
        assertEquals(ProposalService.ENDORSEMENT_DOCUMENT_SIZE_MESSAGE, size.getMessage());

        IllegalArgumentException empty = assertThrows(IllegalArgumentException.class,
                () -> service.attachEndorsementDocument(PROPOSAL_ID, upload("empty.pdf", PDF_TYPE, new byte[0])));
        assertEquals(ProposalService.ENDORSEMENT_DOCUMENT_EMPTY_MESSAGE, empty.getMessage());
    }

    // ===================== replacing and keeping =====================

    @Test
    void replacingTheDocumentRemovesTheFileTheProposalStoredBefore() throws Exception {
        Proposal first = service.attachEndorsementDocument(PROPOSAL_ID,
                upload("first.pdf", PDF_TYPE, validPdfBytes()));
        Path firstFile = Paths.get("uploads", first.getEndorsementFilePath());
        assertTrue(Files.exists(firstFile));

        Proposal second = service.attachEndorsementDocument(PROPOSAL_ID,
                upload("second.pdf", PDF_TYPE, validPdfBytes()));
        rememberStoredFile();

        assertFalse(Files.exists(firstFile), "the previous upload is not left behind");
        assertTrue(Files.exists(Paths.get("uploads", second.getEndorsementFilePath())));
        assertEquals("second.pdf", second.getEndorsementFileName());
    }

    @Test
    void aProposalThatNeverHadADocumentKeepsBothFieldsEmpty() {
        assertNull(proposal.getEndorsementFileName());
        assertNull(proposal.getEndorsementFilePath());
    }
}
