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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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

/**
 * Unit tests for the Technology Map upload of the Create Proposal form (section 13).
 * <p>
 * They cover the rules the form relies on: only a real PNG/JPEG raster image is
 * stored (a text file renamed to .png is rejected), the 10 MB limit is enforced
 * again on the server, the image is attached to the caller's own proposal only,
 * the proponent's own file name is kept for display while the stored file gets a
 * generated name, and replacing an image removes the previous file.
 * <p>
 * No database is required: the repositories are mocked and every file the service
 * writes is removed again after each test.
 */
class ProposalTechnologyMapServiceTest {

    private static final Long PROPOSAL_ID = 42L;

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
                mock(CooperatingAgencyService.class));

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
        if (proposal.getRoadmapFilePath() != null) {
            storedPaths.add("uploads/" + proposal.getRoadmapFilePath());
        }
    }

    /** The file on disk of a stored path ("technology-maps/x.png"). */
    private Path storedFile(String storedPath) {
        return Paths.get("uploads", storedPath);
    }

    private byte[] imageBytes(String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, out), "no image writer for " + format);
        return out.toByteArray();
    }

    private MockMultipartFile upload(String fileName, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", fileName, contentType, bytes);
    }

    @Test
    void storesARealPngImageOnTheProposal() throws IOException {
        MockMultipartFile file = upload("technology map.png", "image/png", imageBytes("png", 400, 300));

        Proposal saved = service.attachTechnologyMap(PROPOSAL_ID, file);
        rememberStoredFile();

        assertEquals("technology map.png", saved.getRoadmapFileName());
        assertNotNull(saved.getRoadmapFilePath());
        assertTrue(saved.getRoadmapFilePath().startsWith("technology-maps/"));
        assertTrue(saved.getRoadmapFilePath().endsWith("_technology-map.png"));
        // The generated name never reuses the client supplied name, so two uploads of
        // the same file cannot overwrite each other.
        assertFalse(saved.getRoadmapFilePath().contains("technology map.png"));
        assertTrue(Files.exists(storedFile(saved.getRoadmapFilePath())));
    }

    @Test
    void storesARealJpegImageWithAJpgExtension() throws IOException {
        MockMultipartFile file = upload("map.jpeg", "image/jpeg", imageBytes("jpeg", 200, 200));

        Proposal saved = service.attachTechnologyMap(PROPOSAL_ID, file);
        rememberStoredFile();

        assertEquals("map.jpeg", saved.getRoadmapFileName());
        assertTrue(saved.getRoadmapFilePath().endsWith("_technology-map.jpg"));
    }

    @Test
    void rejectsAnImageLargerThanTenMegabytes() {
        byte[] oversized = new byte[(int) ProposalService.TECHNOLOGY_MAP_MAX_BYTES + 1];
        MockMultipartFile file = upload("big.png", "image/png", oversized);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.attachTechnologyMap(PROPOSAL_ID, file));

        assertEquals("File size exceeds the 10 MB limit. Please upload a smaller image.", error.getMessage());
        assertNull(proposal.getRoadmapFilePath());
        assertNull(proposal.getRoadmapFileName());
    }

    @Test
    void acceptsAnImageOfExactlyTenMegabytes() throws IOException {
        // 10 MB is the limit itself, not one byte below it: a real PNG padded to
        // exactly 10 * 1024 * 1024 bytes is still stored.
        byte[] png = imageBytes("png", 8, 8);
        byte[] bytes = new byte[(int) ProposalService.TECHNOLOGY_MAP_MAX_BYTES];
        System.arraycopy(png, 0, bytes, 0, png.length);
        MockMultipartFile file = upload("limit.png", "image/png", bytes);

        Proposal saved = service.attachTechnologyMap(PROPOSAL_ID, file);
        rememberStoredFile();

        assertEquals("limit.png", saved.getRoadmapFileName());
        assertEquals(ProposalService.TECHNOLOGY_MAP_MAX_BYTES,
                Files.size(storedFile(saved.getRoadmapFilePath())));
    }

    @Test
    void rejectsANonImageFileThatOnlyLooksLikeAPng() {
        byte[] text = "not an image at all".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = upload("fake.png", "image/png", text);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.attachTechnologyMap(PROPOSAL_ID, file));

        assertEquals("Invalid file format. Please upload a PNG or JPEG image.", error.getMessage());
        assertNull(proposal.getRoadmapFilePath());
    }

    @Test
    void rejectsAnImageFormatOtherThanPngOrJpeg() throws IOException {
        MockMultipartFile file = upload("map.png", "image/png", imageBytes("gif", 60, 60));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.attachTechnologyMap(PROPOSAL_ID, file));

        assertEquals("Invalid file format. Please upload a PNG or JPEG image.", error.getMessage());
        assertNull(proposal.getRoadmapFilePath());
    }

    @Test
    void rejectsAnEmptyUpload() {
        MockMultipartFile file = upload("empty.png", "image/png", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> service.attachTechnologyMap(PROPOSAL_ID, file));
    }

    @Test
    void replacingAnImageKeepsTheNewOneAndRemovesTheOldFile() throws IOException {
        service.attachTechnologyMap(PROPOSAL_ID, upload("first.png", "image/png", imageBytes("png", 30, 30)));
        Path first = storedFile(proposal.getRoadmapFilePath());

        service.attachTechnologyMap(PROPOSAL_ID, upload("second.jpg", "image/jpeg", imageBytes("jpeg", 30, 30)));
        rememberStoredFile();

        assertEquals("second.jpg", proposal.getRoadmapFileName());
        assertTrue(proposal.getRoadmapFilePath().endsWith("_technology-map.jpg"));
        assertFalse(Files.exists(first), "the replaced image should have been deleted");
        assertTrue(Files.exists(storedFile(proposal.getRoadmapFilePath())));
    }

    @Test
    void anImageCanOnlyBeAttachedToTheCallersOwnProposal() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "not your proposal"))
                .when(currentUser).requireAccessTo(any(Proposal.class));

        assertThrows(ResponseStatusException.class,
                () -> service.attachTechnologyMap(PROPOSAL_ID, upload("map.png", "image/png", new byte[] { 1 })));
    }

    @Test
    void aFileOfAnUnknownProposalIsReported() {
        when(proposalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.attachTechnologyMap(999L, upload("map.png", "image/png", new byte[] { 1 })));
    }
}
