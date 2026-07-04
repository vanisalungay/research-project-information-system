package com.rpis.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "proposal_reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProposalReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    @JsonIgnore
    private Proposal proposal;

    @ManyToOne
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "reviewer_role")
    private String reviewerRole;

    // Soundness of Proposal (20%)
    @Column(name = "soundness_sector_need_score")
    private Integer soundnessSectorNeedScore;

    @Column(name = "soundness_effective_solution_score")
    private Integer soundnessEffectiveSolutionScore;

    @Column(name = "soundness_reasonable_budget_score")
    private Integer soundnessReasonableBudgetScore;

    @Column(name = "soundness_doable_workplan_score")
    private Integer soundnessDoableWorkplanScore;

    // Suitability of Output (30%)
    @Column(name = "suitability_cost_effective_score")
    private Integer suitabilityCostEffectiveScore;

    @Column(name = "suitability_partners_identified_score")
    private Integer suitabilityPartnersIdentifiedScore;

    @Column(name = "suitability_commercialization_score")
    private Integer suitabilityCommercializationScore;

    @Column(name = "suitability_timely_utilization_score")
    private Integer suitabilityTimelyUtilizationScore;

    // Significance of Outcome (30%)
    @Column(name = "significance_economic_impact_score")
    private Integer significanceEconomicImpactScore;

    @Column(name = "significance_social_impact_score")
    private Integer significanceSocialImpactScore;

    @Column(name = "significance_environmental_impact_score")
    private Integer significanceEnvironmentalImpactScore;

    @Column(name = "significance_sustainability_score")
    private Integer significanceSustainabilityScore;

    // Competence of Proponent (20%)
    @Column(name = "competence_alignment_score")
    private Integer competenceAlignmentScore;

    @Column(name = "competence_collaboration_score")
    private Integer competenceCollaborationScore;

    @Column(name = "competence_deliverables_understanding_score")
    private Integer competenceDeliverablesUnderstandingScore;

    // Comments & Decision
    @Column(name = "overall_comments", columnDefinition = "TEXT")
    private String overallComments;

    private String decision;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
