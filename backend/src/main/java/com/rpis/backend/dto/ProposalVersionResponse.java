package com.rpis.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Lightweight representation of a proposal version for the API.
 * The full snapshot (proposal details, files, child collections) is returned in
 * {@code details} only when a specific version is requested.
 */
@Data
public class ProposalVersionResponse {
    private Long id;
    private Long proposalId;
    private Integer versionNumber;
    private String documentId;
    private String status;
    private Long submittedById;
    private String submittedByName;
    private LocalDateTime submissionDate;
    private LocalDateTime revisionDate;
    private String revisionRemarks;
    private Boolean isCurrent;
    private LocalDateTime createdAt;
    private Object details;
}
