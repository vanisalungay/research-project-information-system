package com.rpis.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Structured Quarterly Progress Report for an approved, SO-issued project.
 *
 * <p>This is the digital equivalent of the "MSUN Internally-Funded Project
 * Quarterly Progress Report" form. Each report is stored as its own row so the
 * history of every quarter is preserved (never overwritten).</p>
 */
@Entity
@Table(name = "quarterly_progress_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuarterlyProgressReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proposal_id", nullable = false)
    private Long proposalId;

    @Column(name = "period", nullable = false)
    private String period; // Q1, Q2, Q3, Q4

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "status", nullable = false)
    private String status = "DRAFT"; // DRAFT, SUBMITTED

    // ===== Header fields (mirrors the official form) =====
    @Column(name = "project_title")
    private String projectTitle;

    @Column(name = "project_leader")
    private String projectLeader;

    @Column(name = "leader_gender")
    private String leaderGender;

    @Column(name = "base_station", columnDefinition = "TEXT")
    private String baseStation;

    @Column(name = "sites_of_implementation", columnDefinition = "TEXT")
    private String sitesOfImplementation;

    @Column(name = "project_duration")
    private String projectDuration;

    @Column(name = "project_start_date")
    private LocalDate projectStartDate;

    @Column(name = "project_end_date")
    private LocalDate projectEndDate;

    // ===== Major Accomplishments =====
    @Column(name = "catch_up_plan", columnDefinition = "TEXT")
    private String catchUpPlan;

    @Column(name = "problems_concerns", columnDefinition = "TEXT")
    private String problemsConcerns;

    @Column(name = "suggested_solutions", columnDefinition = "TEXT")
    private String suggestedSolutions;

    @Column(name = "objectives_json", columnDefinition = "TEXT")
    @Convert(converter = ObjectiveListConverter.class)
    private List<QuarterlyObjective> objectives = new ArrayList<>();

    @Column(name = "outputs_json", columnDefinition = "TEXT")
    @Convert(converter = OutputListConverter.class)
    private List<QuarterlyOutput> outputs = new ArrayList<>();

    // ===== Ownership / lifecycle =====
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "submitted_by")
    private Long submittedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
}
