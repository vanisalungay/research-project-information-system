package com.rpis.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One preserved version of an issued endorsement letter.
 *
 * <p>A reissue never overwrites the official record: the new PDF is written to a new file,
 * the request's {@code letter_version} is incremented, and the superseded file is left in
 * place. This row is what makes that file reachable again - the request itself only points
 * at the current version. Rows are append-only: no service updates or deletes a version,
 * and the stored file is never removed.
 *
 * <p>{@link #filePath} is relative to the configured upload directory, exactly like
 * {@code endorsement_requests.letter_file_path}.
 */
@Entity
@Table(
        name = "endorsement_letter_versions",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_endorsement_letter_versions_request_version",
                columnNames = { "endorsement_request_id", "version_number" }),
        indexes = @Index(
                name = "idx_endorsement_letter_versions_request",
                columnList = "endorsement_request_id, version_number"))
@Data
@NoArgsConstructor
public class EndorsementLetterVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The endorsement request this version belongs to. */
    @Column(name = "endorsement_request_id", nullable = false)
    private Long endorsementRequestId;

    /** 1 for the originally issued letter, incremented by each reissue. */
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    /** The document code printed on this version (stable across reissues). */
    @Column(name = "document_code")
    private String documentCode;

    /** The name offered on download, e.g. {@code endorsement-letter.pdf}. */
    @Column(name = "file_name")
    private String fileName;

    /** Path of the stored PDF, relative to the upload directory. Never deleted. */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /** When this version was generated. */
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    /** The Dean / administrator who generated this version. */
    @Column(name = "generated_by_user_id")
    private Long generatedByUserId;

    @Column(name = "generated_by_name")
    private String generatedByName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
