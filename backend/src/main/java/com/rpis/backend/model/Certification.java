package com.rpis.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One document held in a Proponent's Certifications section.
 *
 * <p>An entry is created automatically when a Dean issues an official endorsement
 * letter - the Proponent never uploads it. The row points at the stored PDF of the
 * endorsement request and is owned by the Proponent it was issued to, which is what
 * the authorization of the preview and download endpoints is resolved from.
 *
 * <p>{@code endorsementRequestId} is unique in the database, so a retried or
 * concurrent approval can never create a second certification for the same letter.
 */
@Entity
@Table(name = "certifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Certification {

    public static final String DOCUMENT_TYPE_ENDORSEMENT_LETTER = "ENDORSEMENT_LETTER";
    public static final String STATUS_ISSUED = "ISSUED";
    public static final String STATUS_SUPERSEDED = "SUPERSEDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "proponent_id", nullable = false)
    private User proponent;

    @Column(name = "endorsement_request_id")
    private Long endorsementRequestId;

    @Column(name = "proposal_id")
    private Long proposalId;

    @Column(name = "document_type", nullable = false)
    private String documentType = DOCUMENT_TYPE_ENDORSEMENT_LETTER;

    @Column(nullable = false)
    private String title;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "endorsement_date")
    private LocalDate endorsementDate;

    @Column(name = "issuing_dean_name")
    private String issuingDeanName;

    @Column(nullable = false)
    private String status = STATUS_ISSUED;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
