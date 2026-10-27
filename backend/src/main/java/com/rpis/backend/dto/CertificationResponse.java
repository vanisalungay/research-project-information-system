package com.rpis.backend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read model of one document in a Proponent's Certifications section. The stored
 * file path is never exposed; the document is fetched through the authorized
 * certification document endpoint instead.
 */
@Data
public class CertificationResponse {

    private Long id;
    private String documentType;
    private String documentTypeLabel;
    private String title;
    private String proposalTitle;
    private String fileName;
    private LocalDate endorsementDate;
    private String issuingDeanName;
    private String status;
    private LocalDateTime createdAt;

    private Long endorsementRequestId;
    private String endorsementReferenceNumber;
    private Long proposalId;

    /** False when the stored file is missing, so the UI hides the actions. */
    private boolean documentAvailable;
}
