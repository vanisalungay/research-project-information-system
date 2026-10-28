package com.rpis.backend.service;

import com.rpis.backend.model.User;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.storage.UploadStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Electronic signature of a user account (Manage Profile -> Electronic Signature).
 *
 * <p>The signature is always addressed through the authenticated user's own
 * account id, which the controller resolves from the token - never from the
 * request - so one account can never read, replace or delete another account's
 * signature.
 *
 * <p>The image is validated by its actual content (not the submitted file name or
 * MIME type): only real PNG/JPEG raster images within the size and dimension
 * limits are accepted, which also rejects SVG, HTML, executables and any other
 * document. Stored files use a generated name inside uploads/signatures/ - the
 * existing upload convention - and the account row keeps only the original name
 * and the stored path.
 */
@Service
@RequiredArgsConstructor
public class UserSignatureService {

    /** Existing upload convention: signatures live beside the other uploads. */
    private static final String SIGNATURES_SUBDIRECTORY = "signatures";

    /** Maximum accepted signature image size (2 MB). */
    public static final long MAX_FILE_BYTES = 2L * 1024 * 1024;

    /** Maximum accepted width/height in pixels. */
    public static final int MAX_DIMENSION = 4000;

    /** Raster formats the browser can display as a signature. */
    private static final Set<String> ALLOWED_FORMATS = Set.of("png", "jpeg", "jpg");

    private static final String UNSUPPORTED_MESSAGE =
            "Only PNG and JPEG signature images are supported.";
    private static final String UNREADABLE_MESSAGE =
            "The selected file is not a readable image.";

    private final UserRepository userRepository;
    private final UploadStorage uploadStorage;

    /** The folder signatures live in, inside the configured storage root. */
    private static final String SIGNATURES_FOLDER = "signatures";

    /**
     * Stores {@code file} as the signature of {@code userId}, replacing any
     * previously saved signature. The previous file is removed only after the new
     * one has been stored and the account row updated.
     */
    @Transactional
    public User saveSignature(Long userId, MultipartFile file) {
        User user = getUser(userId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please draw or choose a signature image first.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("The signature image is larger than the 2 MB limit.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("The signature image could not be read.");
        }

        ImageInfo image = readImageInfo(bytes);
        String extension = "jpeg".equals(image.format()) ? "jpg" : image.format();
        // The storage layer generates the stored name; the extension is the only part of
        // the client name that is carried over.
        String stored = uploadStorage.store(bytes, SIGNATURES_SUBDIRECTORY, "signature." + extension);

        String previousPath = user.getSignatureFilePath();
        user.setSignatureFileName(originalName(file, extension));
        user.setSignatureFilePath(stored);
        User saved = userRepository.save(user);

        deleteStoredFile(previousPath);
        return saved;
    }

    /**
     * Reads the real image format and dimensions of the uploaded bytes. A file
     * that is not a PNG/JPEG raster image has no usable reader and is rejected, so
     * the stored type can never be spoofed through the file name or MIME type.
     */
    private ImageInfo readImageInfo(byte[] bytes) {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) {
                throw new IllegalArgumentException(UNREADABLE_MESSAGE);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException(UNSUPPORTED_MESSAGE);
            }

            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName() == null
                        ? ""
                        : reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!ALLOWED_FORMATS.contains(format)) {
                    throw new IllegalArgumentException(UNSUPPORTED_MESSAGE);
                }
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1) {
                    throw new IllegalArgumentException(UNREADABLE_MESSAGE);
                }
                if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw new IllegalArgumentException("The signature image is larger than "
                            + MAX_DIMENSION + " x " + MAX_DIMENSION + " pixels.");
                }
                return new ImageInfo(format, width, height);
            } catch (IOException e) {
                throw new IllegalArgumentException(UNREADABLE_MESSAGE);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(UNREADABLE_MESSAGE);
        }
    }

    /**
     * Original file name kept for display only. Any directory part is stripped and
     * the length is capped to the column size, so a crafted upload cannot store a
     * path-like value.
     */
    private String originalName(MultipartFile file, String extension) {
        String fallback = "signature." + extension;
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return fallback;
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        if (name.isEmpty()) {
            return fallback;
        }
        return name.length() > 255 ? name.substring(0, 255) : name;
    }

    /**
     * Returns the caller's saved signature, or {@code null} when none is stored (or the
     * stored file is no longer readable - the account then shows the empty state instead
     * of failing).
     */
    public SignatureFile loadSignature(Long userId) {
        User user = getUser(userId);
        String storedPath = user.getSignatureFilePath();
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }

        UploadStorage.StoredUpload stored = uploadStorage.open(storedPath).orElse(null);
        if (stored == null) {
            return null;
        }
        String fileName = user.getSignatureFileName();
        if (fileName == null || fileName.isBlank()) {
            fileName = stored.fileName();
        }
        return new SignatureFile(
                new InputStreamResource(stored.content()),
                fileName,
                contentTypeFor(stored.fileName()));
    }

    /** Removes the caller's signature reference and the stored file, if any. */
    @Transactional
    public void deleteSignature(Long userId) {
        User user = getUser(userId);
        String storedPath = user.getSignatureFilePath();
        user.setSignatureFileName(null);
        user.setSignatureFilePath(null);
        userRepository.save(user);
        deleteStoredFile(storedPath);
    }

    /**
     * Deletes a previously stored signature file. The storage layer only ever touches a
     * path inside its own root, so a corrupted column value cannot remove anything else,
     * and a leftover file never fails the request: the account no longer references it.
     */
    private void deleteStoredFile(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }
        uploadStorage.delete(storedPath);
    }

    /** Media type of a stored signature, derived from its own generated name. */
    private MediaType contentTypeFor(String storedName) {
        String name = storedName == null ? "" : storedName.toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg")
                ? MediaType.IMAGE_JPEG
                : MediaType.IMAGE_PNG;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    /** A stored signature image: bytes, display file name and media type. */
    public record SignatureFile(Resource resource, String fileName, MediaType contentType) {
    }

    /** Real format and pixel dimensions of an uploaded image. */
    private record ImageInfo(String format, int width, int height) {
    }
}
