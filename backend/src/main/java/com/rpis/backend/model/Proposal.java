package com.rpis.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "proposals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "proponent_id")
    private User proponent;

    @Column(name = "program_title")
    private String programTitle;

    @Column(name = "project_title", nullable = false)
    private String projectTitle;

    @Column(name = "project_leader")
    private String projectLeader;

    private String duration;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    private String college;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "cooperating_agencies", columnDefinition = "TEXT")
    private String cooperatingAgencies;

    @Column(name = "research_type")
    private String researchType;

    @Column(name = "innovation_goals", columnDefinition = "TEXT")
    private String innovationGoals;

    @Column(name = "sector_relevance", columnDefinition = "TEXT")
    private String sectorRelevance;

    @Column(columnDefinition = "TEXT")
    private String sdg;

    @Column(name = "executive_summary", columnDefinition = "TEXT")
    private String executiveSummary;

    @Column(columnDefinition = "TEXT")
    private String rationale;

    @Column(columnDefinition = "TEXT")
    private String framework;

    @Column(name = "objectives_general", columnDefinition = "TEXT")
    private String objectivesGeneral;

    @Column(name = "objectives_specific", columnDefinition = "TEXT")
    private String objectivesSpecific;

    @Column(columnDefinition = "TEXT")
    private String review;

    @Column(name = "review_file_name")
    private String reviewFileName;

    @Column(columnDefinition = "TEXT")
    private String methodology;

    @Column(name = "roadmap_file_name")
    private String roadmapFileName;

    @Column(name = "technology_trl")
    private String technologyTrl;

    @Column(columnDefinition = "TEXT")
    private String outputs;

    @Column(columnDefinition = "TEXT")
    private String outcomes;

    @Column(name = "impact_economic", columnDefinition = "TEXT")
    private String impactEconomic;

    @Column(name = "impact_social", columnDefinition = "TEXT")
    private String impactSocial;

    @Column(columnDefinition = "TEXT")
    private String beneficiaries;

    @Column(name = "beneficiaries_file_name")
    private String beneficiariesFileName;

    @Column(columnDefinition = "TEXT")
    private String sustainability;

    @Column(name = "gad_score")
    private BigDecimal gadScore;

    @Column(name = "gad_file_name")
    private String gadFileName;

    @Column(columnDefinition = "TEXT")
    private String risks;

    @Column(name = "references_text", columnDefinition = "TEXT")
    private String referencesText;

    @Column(name = "other_projects_number")
    private String otherProjectsNumber;

    private String status = "DRAFT";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProposalSite> sites = new ArrayList<>();

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProposalLogFrame> logFrames = new ArrayList<>();

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProposalPersonnel> personnel = new ArrayList<>();

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProposalBudget> budget = new ArrayList<>();

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProposalOtherProject> otherProjects = new ArrayList<>();

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProposalPriorityAgenda> priorityAgendas = new ArrayList<>();

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProposalLimitation> limitations = new ArrayList<>();
}
