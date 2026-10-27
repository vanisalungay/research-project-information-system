package com.rpis.backend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read model of an endorsement request. It is built by the service from the entity
 * so the API never exposes the stored file paths of the issued letter or the
 * signature, and never exposes another account's internal data.
 *
 * <p>{@code canApprove} tells the Dean's UI whether the Approve action may be
 * offered; the backend re-checks every rule when the action is actually submitted.
 * {@code letterTemplateAvailable} is false for a Call for Proposal whose funding
 * program has no official endorsement-letter wording configured yet, so the UI can
 * explain why a request cannot be endorsed instead of silently reusing the IFP
 * letter.
 */
@Data
public class EndorsementRequestResponse {

    private Long id;
    private String referenceNumber;
    private String status;

    private String proposalTitle;
    private String college;
    private String department;
    private String researchSummary;
    private String notes;

    private Long proposalAnnouncementId;
    private String proposalAnnouncementName;
    private String fundingProgram;
    private String fundingProgramLabel;
    private boolean letterTemplateAvailable;

    private Long proponentId;
    private String proponentName;
    private String proponentEmail;

    private Long assignedDeanId;
    private String assignedDeanName;

    private LocalDateTime createdAt;

    private Long decidedByUserId;
    private String decidedByName;
    private String decidedByDesignation;
    private LocalDateTime decidedAt;
    private LocalDate endorsementDate;
    private String rejectionReason;

    private String letterDocumentCode;
    private String letterFileName;
    private Integer letterVersion;
    private boolean hasLetter;
    private boolean hasSignature;

    private Long proposalId;
    private Long certificationId;

    private boolean canApprove;
    private boolean canReject;
}
