package com.rpis.backend.dto;

import com.rpis.backend.model.ProposalBudget;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OvcafProposalResponse {

    private Long id;
    private String documentId;
    private String projectTitle;
    private String projectLeader;
    private String proponentName;
    private Long proponentId;
    private String college;
    private String status;
    private String duration;
    private LocalDate startDate;
    private LocalDate endDate;
    private String executiveSummary;
    private String remarks;

    // Budget information
    private List<ProposalBudget> budgetBreakdown;
    private BigDecimal totalBudget;

    // Section 25: the Line-Item Budget Requirement is an uploaded document. The name is
    // shown to OVCAF and the stored path is what the authorized /api/files route serves.
    private String beneficiariesFileName;
    private String beneficiariesFilePath;

    // The Special Order issued by the Office of the Chancellor. Implementation
    // approval must not be recorded without it, and the reviewer needs to see whether
    // it exists before endorsing the budget.
    private String soNumber;
    private String soFileName;
    private LocalDateTime soUploadedAt;

    // OVCAF specific fields
    private String fundingSource;
    private String ovcafDecision;
    private String ovcafRemarks;
    private Boolean budgetEndorsed;
    private Boolean budgetAuthorized;
    private Boolean fundsReleased;
    private LocalDateTime fundsReleasedAt;

    // Validation checklist
    private Boolean budgetComplete;
    private Boolean budgetComputationCorrect;
    private Boolean supportingDocsComplete;
    private Boolean expensesReasonable;
    private Boolean fundingSourceIdentified;
    private Boolean complianceVerified;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime receivedAt; // When proposal was forwarded to OVCAF
}