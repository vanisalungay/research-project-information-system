package com.rpis.backend.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Filesystem implementation of {@link UploadStorage}.
 *
 * <h2>The root is absolute and configured</h2>
 * {@code rpis.storage.upload-dir} (environment variable {@code RPIS_UPLOAD_DIR}) is
 * resolved to an absolute path once, at start-up. The previous relative
 * {@code "uploads"} made the location depend on the process working directory, which is
 * how this repository came to contain two different upload directories and why two
 * instances could not see each other's files. The default keeps the old location
 * relative to the working directory, so an existing single-instance setup keeps working
 * unchanged.
 *
 * <h2>One traversal guard</h2>
 * {@link #resolve} is the only place a stored value is turned into a path, and the only
 * place that checks the result is still inside the root. A value that escapes - via
 * {@code ..}, an absolute path or a Windows drive letter - resolves to nothing, so every
 * caller fails closed without needing its own guard.
 *
 * <h2>Both historic path shapes</h2>
 * Rows written before this class existed may store either
 * {@code technology-maps/x.png} or {@code uploads/technology-maps/x.png}. Both are
 * accepted, so no database value has to be rewritten.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rpis.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalUploadStorage implements UploadStorage {

    /** Prefix some historic rows carry; stripped before resolving. */
    private static final String LEGACY_PREFIX = "uploads/";

    private final Path root;

    public LocalUploadStorage(@Value("${rpis.storage.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            // Not fatal: the first write reports the problem with a clearer message than a
            // start-up failure would, and read-only operations do not need the directory.
            log.warn("Could not create the upload directory {}: {}", root, e.getMessage());
        }
        log.info("Upload storage: {} (absolute: {})", uploadDir, root);
    }

    @Override
    public String store(byte[] content, String subdirectory, String originalName) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("There is nothing to store.");
        }
        String folder = normalizeSubdirectory(subdirectory);
        String extension = extensionOf(originalName);

        try {
            Path directory = resolveInsideRoot(folder);
            if (directory == null) {
                throw new IllegalStateException("The upload directory is not usable.");
            }
            Files.createDirectories(directory);

            // A generated name, never the client's: two proponents uploading
            // "budget.pdf" must not collide, and a crafted name must not be able to
            // address a path.
            String storedName = UUID.randomUUID() + "_" + safeBaseName(originalName) + extension;
            Path target = directory.resolve(storedName).normalize();
            Files.write(target, content);
            return relative(target);
        } catch (IOException e) {
            throw new IllegalStateException("The file could not be stored.", e);
        }
    }

    @Override
    public Optional<StoredUpload> open(String storedPath) {
        Path resolved = resolve(storedPath);
        if (resolved == null) {
            return Optional.empty();
        }
        try {
            if (!Files.isRegularFile(resolved) || !Files.isReadable(resolved)) {
                return Optional.empty();
            }
            Path fileName = resolved.getFileName();
            return Optional.of(new StoredUpload(
                    fileName != null ? fileName.toString() : "document",
                    Files.size(resolved),
                    Files.newInputStream(resolved)));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(String storedPath) {
        Path resolved = resolve(storedPath);
        return resolved != null && Files.isRegularFile(resolved) && Files.isReadable(resolved);
    }

    @Override
    public boolean delete(String storedPath) {
        Path resolved = resolve(storedPath);
        if (resolved == null) {
            return false;
        }
        try {
            return Files.deleteIfExists(resolved);
        } catch (IOException e) {
            // A leftover file must never fail the request that cleared the reference to
            // it; the caller has already removed the reference.
            log.warn("Could not delete stored file {}: {}", resolved, e.getMessage());
            return false;
        }
    }

    @Override
    public String describe() {
        return "local:" + root;
    }

    /** The configured root, for tests and diagnostics. */
    public Path root() {
        return root;
    }

    /**
     * Turns a stored value into an absolute path, or {@code null} when it is blank or
     * escapes the root. This is the only traversal guard in the application.
     */
    private Path resolve(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }
        String cleaned = storedPath.replace('\\', '/').trim();
        // A stored value that already names the root must not be prefixed a second time.
        if (cleaned.startsWith(LEGACY_PREFIX)) {
            cleaned = cleaned.substring(LEGACY_PREFIX.length());
        }
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        return resolveInsideRoot(cleaned);
    }

    private Path resolveInsideRoot(String relative) {
        if (relative == null || relative.isBlank()) {
            return null;
        }
        Path resolved = root.resolve(relative).normalize();
        // Fails closed for "..", an absolute path and a drive letter alike.
        return resolved.startsWith(root) ? resolved : null;
    }

    /** A single safe folder name; anything path-like is reduced to its last segment. */
    private String normalizeSubdirectory(String subdirectory) {
        if (subdirectory == null || subdirectory.isBlank()) {
            return "";
        }
        String cleaned = subdirectory.replace('\\', '/');
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        int lastSlash = cleaned.lastIndexOf('/');
        if (lastSlash >= 0) {
            cleaned = cleaned.substring(lastSlash + 1);
        }
        return cleaned.replace("..", "");
    }

    /** The lower-case extension of a client name, or an empty string. */
    private String extensionOf(String originalName) {
        String name = originalName == null ? "" : originalName.replace('\\', '/');
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        // Only a short alphanumeric extension is carried over.
        return extension.matches("[a-z0-9]{1,10}") ? "." + extension : "";
    }

    /** The client name reduced to a harmless file-name fragment, for readability. */
    private String safeBaseName(String originalName) {
        String name = originalName == null ? "" : originalName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        name = name.replaceAll("[^A-Za-z0-9._-]", "-");
        if (name.length() > 40) {
            name = name.substring(0, 40);
        }
        return name.isBlank() ? "file" : name;
    }

    /** The path of {@code target} relative to the root, with {@code /} separators. */
    private String relative(Path target) {
        return root.relativize(target).toString().replace('\\', '/');
    }
}
