package com.rpis.backend.dto;

import lombok.Data;

@Data
public class OvcafValidationRequest {

    private Long validatedById;

    // Decision: APPROVED_ENDORSED, RETURNED_FOR_REVISION, REJECTED
    private String decision;

    private String remarks;

    // Validation Checklist
    private Boolean budgetComplete;
    private Boolean budgetComputationCorrect;
    private Boolean supportingDocsComplete;
    private Boolean expensesReasonable;
    private Boolean fundingSourceIdentified;
    private Boolean complianceVerified;
}