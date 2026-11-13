package com.rpis.backend.storage;

import java.io.InputStream;
import java.util.Optional;

/**
 * Where uploaded documents live.
 *
 * <h2>Why this exists</h2>
 * Before this interface, eight classes each built their own path with
 * {@code Paths.get("uploads")}. That had three consequences:
 * <ul>
 *   <li>the root was <strong>relative to the process working directory</strong>, so the
 *       same code wrote to {@code ./uploads} when the application was started from the
 *       repository root and to {@code ./backend/uploads} when it was started from
 *       {@code backend/} - which is exactly how the repository ended up with two
 *       divergent upload directories;</li>
 *   <li>each class re-implemented its own path-traversal guard, so there were four
 *       copies to keep correct;</li>
 *   <li>a file written by one backend instance could not be read by another, because
 *       "the uploads directory" meant a different directory on every instance.</li>
 * </ul>
 *
 * <h2>Stored paths</h2>
 * A stored path is <strong>relative to the storage root</strong> and always uses
 * {@code /} as the separator (for example {@code technology-maps/abc.png}). Historic
 * rows in this database carry two shapes - some include a leading {@code uploads/} and
 * some do not - so implementations must accept both rather than require a data
 * migration.
 *
 * <h2>Pluggability</h2>
 * The interface deliberately traffics in bytes and streams rather than
 * {@link java.nio.file.Path}, so a backend that is not a filesystem (an S3/MinIO
 * bucket, for instance) can implement it without the callers changing. Only
 * {@link LocalUploadStorage} is implemented today; see {@code storage.type} in
 * {@code application.properties}.
 */
public interface UploadStorage {

    /**
     * Stores content under {@code subdirectory} and returns the path relative to the
     * storage root.
     *
     * <p>The caller supplies the bytes and the subdirectory; the generated file name is
     * the implementation's business. The original name is passed only so an
     * implementation can preserve the extension, and must never be used as the stored
     * name: it comes from the client.
     *
     * @param content      the bytes to store
     * @param subdirectory a single-level folder such as {@code technology-maps}
     * @param originalName the client-supplied name, used only for its extension
     * @return the stored path, relative to the root, with {@code /} separators
     */
    String store(byte[] content, String subdirectory, String originalName);

    /**
     * Opens a stored file for reading.
     *
     * <p>Returns {@link Optional#empty()} when the path is blank, points outside the
     * root (a traversal attempt or a corrupted column) or no longer exists. Callers
     * treat all three the same way: the document is simply not available.
     */
    Optional<StoredUpload> open(String storedPath);

    /** True when the stored file is present and readable. */
    boolean exists(String storedPath);

    /**
     * Removes a stored file.
     *
     * @return true when a file was removed
     */
    boolean delete(String storedPath);

    /**
     * A short description of the configured location, for logs and health output.
     *
     * <p>Implementations must not include credentials.
     */
    String describe();

    /**
     * A readable stored file.
     *
     * @param fileName the stored file name, for the download header
     * @param size     the byte length, so a response can set Content-Length without
     *                 buffering the whole document
     * @param content  the bytes; the caller closes the stream
     */
    record StoredUpload(String fileName, long size, InputStream content) {
    }
}
