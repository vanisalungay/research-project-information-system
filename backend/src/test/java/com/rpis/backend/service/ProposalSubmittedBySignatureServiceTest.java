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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The "Submitted By" signature upload of the Certification &amp; Endorsement step.
 *
 * <p>The form offers an account signature or an upload for this proposal only, and accepts
 * images and PDFs. The service decides the format from the real bytes (a mislabelled file is
 * refused), keeps the proponent's own file name for display, stores the bytes under a
 * generated name in the proposal's own folder, and on replacement deletes only the previous
 * submitted-signature file - the endorsement PDF and every other attachment are untouched.</p>
 */
class ProposalSubmittedBySignatureServiceTest {

    private static final Long PROPOSAL_ID = 66L;

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

    private void rememberStoredFile() {
        if (proposal.getSubmittedByFilePath() != null) {
            storedPaths.add("uploads/" + proposal.getSubmittedByFilePath());
        }
    }

    private byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private byte[] jpegBytes() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", out);
        return out.toByteArray();
    }

    private byte[] pdfBytes() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph("Signature"));
        document.close();
        return out.toByteArray();
    }

    private MockMultipartFile upload(String fileName, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", fileName, contentType, bytes);
    }

    // ===================== accepted formats =====================

    @Test
    void aPngSignatureIsStoredWithItsDisplayName() throws Exception {
        Proposal saved = service.attachSubmittedBySignature(PROPOSAL_ID,
                upload("Maria signature.png", "image/png", pngBytes()));

        assertEquals("Maria signature.png", saved.getSubmittedByFileName());
        assertNotNull(saved.getSubmittedByFilePath());
        assertTrue(saved.getSubmittedByFilePath().startsWith("submitted-by-signatures/"),
                saved.getSubmittedByFilePath());
        assertEquals(saved.getSubmittedByFilePath(), proposal.getSubmittedByFilePath());

        rememberStoredFile();
        assertTrue(Files.exists(Paths.get("uploads", saved.getSubmittedByFilePath())),
                "the bytes are on disk, so the download route can serve them");
    }

    @Test
    void aJpegSignatureIsStoredAsAJpg() throws Exception {
        Proposal saved = service.attachSubmittedBySignature(PROPOSAL_ID,
                upload("signature.jpeg", "image/jpeg", jpegBytes()));

        String stored = Paths.get("uploads", saved.getSubmittedByFilePath())
                .getFileName().toString();
        assertTrue(stored.endsWith(".jpg"), stored);
        rememberStoredFile();
    }

    @Test
    void aPdfSignatureIsStored() throws Exception {
        Proposal saved = service.attachSubmittedBySignature(PROPOSAL_ID,
                upload("signature.pdf", "application/pdf", pdfBytes()));

        assertTrue(saved.getSubmittedByFilePath().endsWith(".pdf"), saved.getSubmittedByFilePath());
        rememberStoredFile();
    }

    // ===================== refused uploads =====================

    @Test
    void aFileThatIsNotAnImageOrPdfIsRefused() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.attachSubmittedBySignature(PROPOSAL_ID,
                        upload("signature.png", "image/png",
                                "not an image".getBytes(StandardCharsets.UTF_8))));
        assertEquals(ProposalService.SUBMITTED_BY_SIGNATURE_FORMAT_MESSAGE, error.getMessage());
        assertNull(proposal.getSubmittedByFilePath(), "nothing was stored for a refused upload");
    }

    @Test
    void anImageRenamedToPdfIsRefusedOnItsContent() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.attachSubmittedBySignature(PROPOSAL_ID,
                        upload("signature.pdf", "application/pdf", new byte[] {
                                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3 })));
        assertEquals(ProposalService.ENDORSEMENT_DOCUMENT_FORMAT_MESSAGE, error.getMessage());
    }

    @Test
    void anEmptyOrOversizedUploadIsReportedWithItsOwnMessage() {
        IllegalArgumentException empty = assertThrows(IllegalArgumentException.class,
                () -> service.attachSubmittedBySignature(PROPOSAL_ID,
                        upload("signature.png", "image/png", new byte[0])));
        assertEquals(ProposalService.SUBMITTED_BY_SIGNATURE_EMPTY_MESSAGE, empty.getMessage());

        byte[] tooBig = new byte[(int) ProposalService.SUBMITTED_BY_SIGNATURE_MAX_BYTES + 1];
        IllegalArgumentException size = assertThrows(IllegalArgumentException.class,
                () -> service.attachSubmittedBySignature(PROPOSAL_ID,
                        upload("signature.png", "image/png", tooBig)));
        assertEquals(ProposalService.SUBMITTED_BY_SIGNATURE_SIZE_MESSAGE, size.getMessage());
    }

    // ===================== replacement =====================

    @Test
    void replacingTheSignatureDeletesOnlyThePreviousSignatureFile() throws Exception {
        proposal.setEndorsementFilePath("endorsement-documents/existing-endorsement.pdf");

        Proposal first = service.attachSubmittedBySignature(PROPOSAL_ID,
                upload("first.png", "image/png", pngBytes()));
        Path firstFile = Paths.get("uploads", first.getSubmittedByFilePath());
        assertTrue(Files.exists(firstFile));

        Proposal second = service.attachSubmittedBySignature(PROPOSAL_ID,
                upload("second.png", "image/png", pngBytes()));
        rememberStoredFile();

        assertFalse(Files.exists(firstFile), "the previous signature is not left behind");
        assertTrue(Files.exists(Paths.get("uploads", second.getSubmittedByFilePath())));
        assertEquals("second.png", second.getSubmittedByFileName());
        assertEquals("endorsement-documents/existing-endorsement.pdf",
                second.getEndorsementFilePath(),
                "the endorsement PDF is untouched by a signature replacement");
    }

    @Test
    void aProposalThatNeverHadASignatureKeepsBothFieldsEmpty() {
        assertNull(proposal.getSubmittedByFileName());
        assertNull(proposal.getSubmittedByFilePath());
    }
}
