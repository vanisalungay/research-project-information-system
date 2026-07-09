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