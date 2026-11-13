package com.rpis.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ovcaf_validations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OvcafValidation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private User validatedBy;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "decision", nullable = false)
    private String decision; // APPROVED_ENDORSED, RETURNED_FOR_REVISION, REJECTED

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    // Validation Checklist
    @Column(name = "budget_complete")
    private Boolean budgetComplete = false;

    @Column(name = "budget_computation_correct")
    private Boolean budgetComputationCorrect = false;

    @Column(name = "supporting_docs_complete")
    private Boolean supportingDocsComplete = false;

    @Column(name = "expenses_reasonable")
    private Boolean expensesReasonable = false;

    @Column(name = "funding_source_identified")
    private Boolean fundingSourceIdentified = false;

    @Column(name = "compliance_verified")
    private Boolean complianceVerified = false;

    // Budget authorization fields
    @Column(name = "budget_endorsed")
    private Boolean budgetEndorsed = false;

    @Column(name = "budget_endorsed_at")
    private LocalDateTime budgetEndorsedAt;

    @Column(name = "budget_authorized")
    private Boolean budgetAuthorized = false;

    @Column(name = "budget_authorized_at")
    private LocalDateTime budgetAuthorizedAt;

    @Column(name = "funds_released")
    private Boolean fundsReleased = false;

    @Column(name = "funds_released_at")
    private LocalDateTime fundsReleasedAt;

    @Column(name = "released_by")
    private Long releasedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}