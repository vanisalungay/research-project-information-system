package com.rpis.backend.service;

import com.rpis.backend.model.User;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.rpis.backend.storage.LocalUploadStorage;

/**
 * Unit tests for the electronic signature of a user account. They cover the rules
 * that protect the feature: only real PNG/JPEG raster images inside the size and
 * dimension limits are stored, a stored signature is addressed through the
 * account, replacing it removes the previous file, and a missing or removed file
 * is reported as "no signature" instead of failing.
 *
 * <p>No database is required: the repository is mocked and the files the service
 * writes are removed again after each test.
 */
class UserSignatureServiceTest {

    private static final Long USER_ID = 11L;

    private UserRepository userRepository;
    private UserSignatureService service;
    private User user;
    private final List<String> storedPaths = new ArrayList<>();

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new UserSignatureService(userRepository, new LocalUploadStorage("uploads"));
        user = new User();
        user.setId(USER_ID);
        user.setName("Proponent");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void removeStoredFiles() throws IOException {
        for (String stored : storedPaths) {
            Files.deleteIfExists(storedFile(stored));
        }
        storedPaths.clear();
    }

    /** Tracks the file the service just wrote so the test can clean it up. */
    /**
     * The stored signature file, resolved against the storage root this test uses.
     * A stored value is relative to that root ("signatures/x.png"), so it cannot be
     * turned into a path on its own.
     */
    private Path storedFile(String storedPath) {
        return Paths.get("uploads").resolve(storedPath);
    }
    private void rememberStoredFile() {
        if (user.getSignatureFilePath() != null) {
            storedPaths.add(user.getSignatureFilePath());
        }
    }

    private byte[] imageBytes(String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, out), "no image writer for " + format);
        return out.toByteArray();
    }

    private MockMultipartFile upload(String fileName, String contentType, byte[] bytes) {
        return new MockMultipartFile("signature", fileName, contentType, bytes);
    }

    @Test
    void savesAPngSignatureOnTheAccount() throws IOException {
        byte[] png = imageBytes("png", 300, 120);

        User saved = service.saveSignature(USER_ID, upload("my-signature.png", "image/png", png));

        assertEquals("my-signature.png", saved.getSignatureFileName());
        rememberStoredFile();

        Path stored = storedFile(saved.getSignatureFilePath());
        assertTrue(Files.exists(stored), "the signature file should be stored");
        assertArrayEquals(png, Files.readAllBytes(stored));
        assertTrue(stored.getFileName().toString().endsWith("_signature.png"),
                "stored files use a generated name: " + stored.getFileName());
        verify(userRepository).save(user);
    }

    @Test
    void acceptsJpegAndStoresItWithAJpgExtension() throws IOException {
        service.saveSignature(USER_ID, upload("scan.jpeg", "image/jpeg", imageBytes("jpeg", 200, 80)));
        rememberStoredFile();

        assertTrue(user.getSignatureFilePath().endsWith("_signature.jpg"));
        assertEquals("scan.jpeg", user.getSignatureFileName());
    }

    @Test
    void replacingASignatureRemovesThePreviousFile() throws IOException {
        service.saveSignature(USER_ID, upload("first.png", "image/png", imageBytes("png", 120, 60)));
        String first = user.getSignatureFilePath();
        rememberStoredFile();

        service.saveSignature(USER_ID, upload("second.png", "image/png", imageBytes("png", 140, 70)));
        rememberStoredFile();

        assertNotEquals(first, user.getSignatureFilePath());
        assertFalse(Files.exists(storedFile(first)), "the replaced file should be deleted");
        assertTrue(Files.exists(storedFile(user.getSignatureFilePath())));
        assertEquals("second.png", user.getSignatureFileName());
    }

    @Test
    void rejectsAMissingOrEmptyUpload() {
        assertThrows(IllegalArgumentException.class, () -> service.saveSignature(USER_ID, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.saveSignature(USER_ID, upload("empty.png", "image/png", new byte[0])));
        assertNull(user.getSignatureFilePath());
    }

    @Test
    void rejectsAFileLargerThanTwoMegabytes() {
        byte[] tooLarge = new byte[(int) UserSignatureService.MAX_FILE_BYTES + 1];

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveSignature(USER_ID, upload("big.png", "image/png", tooLarge)));

        assertTrue(error.getMessage().contains("2 MB"), error.getMessage());
        assertNull(user.getSignatureFilePath());
    }

    @Test
    void rejectsContentThatIsNotAnImageEvenWhenNamedPng() {
        byte[] html = "<html><body>not a signature</body></html>".getBytes(StandardCharsets.UTF_8);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveSignature(USER_ID, upload("signature.png", "image/png", html)));

        assertTrue(error.getMessage().contains("PNG and JPEG"), error.getMessage());
        assertNull(user.getSignatureFilePath());
    }

    @Test
    void rejectsSvgContent() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".getBytes(StandardCharsets.UTF_8);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveSignature(USER_ID, upload("signature.svg", "image/svg+xml", svg)));

        assertTrue(error.getMessage().contains("PNG and JPEG"), error.getMessage());
        assertNull(user.getSignatureFilePath());
    }

    @Test
    void rejectsATruncatedImage() throws IOException {
        byte[] truncated = Arrays.copyOf(imageBytes("png", 120, 60), 24);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveSignature(USER_ID, upload("broken.png", "image/png", truncated)));

        assertTrue(error.getMessage().contains("readable image"), error.getMessage());
        assertNull(user.getSignatureFilePath());
    }

    @Test
    void rejectsAnImageFormatThatIsNotPngOrJpeg() throws IOException {
        byte[] gif = imageBytes("gif", 120, 60);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveSignature(USER_ID, upload("signature.gif", "image/gif", gif)));

        assertTrue(error.getMessage().contains("PNG and JPEG"), error.getMessage());
        assertNull(user.getSignatureFilePath());
    }

    @Test
    void rejectsAnImageBeyondTheDimensionLimit() throws IOException {
        byte[] tooWide = imageBytes("png", UserSignatureService.MAX_DIMENSION + 1, 10);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveSignature(USER_ID, upload("wide.png", "image/png", tooWide)));

        assertTrue(error.getMessage().contains("4000 x 4000"), error.getMessage());
        assertNull(user.getSignatureFilePath());
    }

    @Test
    void reportsNoSignatureWhenNoneIsStored() {
        assertNull(service.loadSignature(USER_ID));
    }

    @Test
    void loadsTheStoredSignatureWithItsMediaType() throws IOException {
        service.saveSignature(USER_ID, upload("sig.jpg", "image/jpeg", imageBytes("jpeg", 100, 50)));
        rememberStoredFile();

        UserSignatureService.SignatureFile loaded = service.loadSignature(USER_ID);

        assertNotNull(loaded);
        assertEquals("sig.jpg", loaded.fileName());
        assertEquals(MediaType.IMAGE_JPEG, loaded.contentType());
        assertTrue(loaded.resource().exists());
    }

    @Test
    void reportsNoSignatureWhenTheStoredFileIsGone() throws IOException {
        service.saveSignature(USER_ID, upload("sig.png", "image/png", imageBytes("png", 60, 30)));
        rememberStoredFile();
        Files.deleteIfExists(storedFile(user.getSignatureFilePath()));

        assertNull(service.loadSignature(USER_ID));
    }

    @Test
    void deletingTheSignatureClearsTheAccountAndRemovesTheFile() throws IOException {
        service.saveSignature(USER_ID, upload("sig.png", "image/png", imageBytes("png", 60, 30)));
        String stored = user.getSignatureFilePath();

        service.deleteSignature(USER_ID);

        assertNull(user.getSignatureFileName());
        assertNull(user.getSignatureFilePath());
        assertFalse(Files.exists(storedFile(stored)));
        verify(userRepository, org.mockito.Mockito.times(2)).save(user);
    }

    @Test
    void anUnknownAccountIsRejected() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.loadSignature(99L));
        assertThrows(IllegalArgumentException.class,
                () -> service.saveSignature(99L, upload("sig.png", "image/png", new byte[] { 1 })));
    }
}
