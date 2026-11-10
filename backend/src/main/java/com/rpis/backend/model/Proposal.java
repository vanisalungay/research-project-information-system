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

    // Proposal ID and Revision tracking
    @Column(name = "proposal_code", unique = true)
    private String proposalCode; // 3-digit continuous counter: '001', '002', etc.

    @Column(name = "revision_number")
    private Integer revisionNumber = 0; // 0 = initial submission, 1 = first revision, etc.

    @Column(name = "document_id", unique = true)
    private String documentId; // Full format: MSUN-ORPS-DA-001-2026-REV00

    @ManyToOne
    @JoinColumn(name = "proponent_id")
    private User proponent;

    // The Proposal Announcement (submission window) this proposal was created /
    // submitted under. Used to enforce that announcements with submitted proposals
    // cannot be deleted.
    @Column(name = "proposal_announcement_id")
    private Long proposalAnnouncementId;

    /**
     * The Dean endorsement this proposal was submitted under, when the Proponent
     * arrived through the endorsement workflow. A stable foreign-key association -
     * never a title match - so two proposals with the same title can never be
     * confused. Null for a proposal written without an endorsement, which the
     * current application still allows.
     */
    @Column(name = "endorsement_request_id")
    private Long endorsementRequestId;

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

    /**
     * Structured address of the "Address *" section of the Create Proposal form
     * (section 01). The combined {@link #address} text above is derived from
     * these parts plus the contact, so every existing screen keeps working, while
     * the parts stay individually reviewable and reportable.
     */
    @Column(name = "address_country", length = 120)
    private String addressCountry;

    @Column(name = "address_region", length = 120)
    private String addressRegion;

    @Column(name = "address_province", length = 120)
    private String addressProvince;

    @Column(name = "address_district", length = 120)
    private String addressDistrict;

    @Column(name = "address_municipality", length = 120)
    private String addressMunicipality;

    @Column(name = "address_barangay", length = 120)
    private String addressBarangay;

    /** Selected contact type of the "Contact *" section (CONTACT_NUMBER / EMAIL_ADDRESS). */
    @Column(name = "contact_type", length = 30)
    private String contactType;

    @Column(name = "contact_value", length = 255)
    private String contactValue;

    @Column(name = "cooperating_agencies", columnDefinition = "TEXT")
    private String cooperatingAgencies;

    @Column(name = "research_type")
    private String researchType;

    @Column(name = "innovation_goals", columnDefinition = "TEXT")
    private String innovationGoals;

    @Column(name = "sector_relevance", columnDefinition = "TEXT")
    private String sectorRelevance;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = SdgListConverter.class)
    private List<String> sdg = new ArrayList<>();

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

    @Column(name = "project_leader_sex", length = 20)
    private String projectLeaderSex;

    @Column(name = "limitations_text", columnDefinition = "TEXT")
    private String limitationsText;

    @Column(columnDefinition = "TEXT")
    private String methodology;

    /**
     * Display name of the Technology Map image (section 13 of the Create Proposal
     * form) - the file name the proponent selected.
     */
    @Column(name = "roadmap_file_name")
    private String roadmapFileName;

    /**
     * Stored path of the Technology Map image, relative to the "uploads" directory
     * (e.g. {@code technology-maps/<uuid>_technology-map.png}). Kept apart from
     * {@link #roadmapFileName} so the screens can keep showing the proponent's own
     * file name while the authorized download route resolves the real file.
     */
    @Column(name = "roadmap_file_path", length = 500)
    private String roadmapFilePath;

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

    @Column(name = "beneficiaries_file_path", length = 500)
    private String beneficiariesFilePath;

    /**
     * Display name of the endorsement document uploaded in the Certification &amp;
     * Endorsement step ("Endorsed By") - the file name the proponent selected. The
     * document is a PDF (validated on both layers); this column is NULL for every
     * proposal that was submitted before the document could be uploaded, which keeps
     * older records working unchanged.
     */
    @Column(name = "endorsement_file_name")
    private String endorsementFileName;

    /**
     * Stored path of that endorsement document, relative to the "uploads" directory
     * (e.g. {@code endorsement-documents/<uuid>_endorsement-document.pdf}). Kept apart
     * from {@link #endorsementFileName} so the screens can keep showing the proponent's
     * own file name while the authorized download route resolves the real file.
     */
    @Column(name = "endorsement_file_path", length = 500)
    private String endorsementFilePath;

    /**
     * "Submitted By" section of the Certification &amp; Endorsement step: the proponent's
     * name, designation and the date entered in the form. Persisted with the draft, so
     * reopening it restores exactly what was typed instead of starting over.
     */
    @Column(name = "submitted_by_name")
    private String submittedByName;

    @Column(name = "submitted_by_designation")
    private String submittedByDesignation;

    @Column(name = "submitted_by_date", length = 50)
    private String submittedByDate;

    /**
     * Display name of the proponent's signature uploaded in that step - the file name the
     * proponent selected. The bytes never travel in the proposal DTO; they are uploaded to
     * the proposal's own endpoint (POST /api/proposals/{id}/submitted-by-signature), which
     * validates the image/PDF, stores it and sets both this name and the path below.
     */
    @Column(name = "submitted_by_file_name")
    private String submittedByFileName;

    /**
     * Stored path of that signature, relative to the "uploads" directory (e.g.
     * {@code submitted-by-signatures/<uuid>_signature.png}). Kept apart from
     * {@link #submittedByFileName} so the screens can keep showing the proponent's own
     * file name while the authorized download route resolves the real file.
     */
    @Column(name = "submitted_by_file_path", length = 500)
    private String submittedByFilePath;

    /**
     * "Endorsed By" section of the Certification &amp; Endorsement step: the endorser's
     * name, designation and date. Persisted with the draft, so a reopened draft (or a
     * reviewer opening the submitted proposal) sees the same certification.
     */
    @Column(name = "endorsed_by_name")
    private String endorsedByName;

    @Column(name = "endorsed_by_designation")
    private String endorsedByDesignation;

    @Column(name = "endorsed_by_date", length = 50)
    private String endorsedByDate;

    @Column(columnDefinition = "TEXT")
    private String sustainability;

    @Column(name = "gad_score")
    private BigDecimal gadScore;

    @Column(name = "gad_file_name")
    private String gadFileName;

    /**
     * Section 19 - the interactive GAD Checklist 2 (responses, comments and the derived
     * total), stored as JSON. Null for a proposal that predates the checklist, whose uploaded
     * GAD document is still kept in {@link #gadFileName}.
     */
    @Convert(converter = GadChecklistDataConverter.class)
    @Column(name = "gad_checklist_json", columnDefinition = "TEXT")
    private GadChecklistData gadChecklist;

    @Column(columnDefinition = "TEXT")
    private String risks;

    /**
     * Section 21 - the structured Risk Management Plan: one entry per identified risk,
     * each with its likelihood, impact and mitigation measure. Stored as a JSON array in
     * {@code risk_management_json} and exposed as a real JSON array to the client, so the
     * Create Proposal form, the revision form and every review page read the same rows.
     *
     * <p>{@link #risks} above keeps the legacy single-text plan: it is the readable
     * rendering of these entries while they exist (so screens and exports that predate the
     * table keep working) and the untouched hand-written text of a proposal that was saved
     * before the table existed. Empty for an old proposal, which the views then display
     * from {@code risks}.</p>
     */
    @Convert(converter = RiskManagementListConverter.class)
    @Column(name = "risk_management_json", columnDefinition = "TEXT")
    private List<RiskManagementEntry> riskManagement = new ArrayList<>();

    @Column(name = "references_text", columnDefinition = "TEXT")
    private String referencesText;

    /**
     * The structured literature-cited entries of the reference builder (a JSON array
     * of {type, fields, text}) behind {@link #referencesText}. Null for a proposal
     * saved before the builder existed or written by hand; the plain text above stays
     * the value every screen reads.
     */
    @Column(name = "references_json", columnDefinition = "TEXT")
    private String referencesJson;

    @Column(name = "other_projects_number")
    private String otherProjectsNumber;

    private String status = "DRAFT";

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_by_position")
    private String reviewedByPosition;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    // Special Order (SO) fields issued by Office of the Chancellor
    @Column(name = "so_number")
    private String soNumber;

    @Column(name = "so_file_name")
    private String soFileName;

    @Column(name = "so_file_path")
    private String soFilePath;

    @Column(name = "so_uploaded_at")
    private LocalDateTime soUploadedAt;

    // Budget requirement determined by the Office of the Chancellor during final
    // approval. When true the proposal must go through the OVCAF funding step;
    // when false it is approved for immediate implementation (no OVCAF step).
    @Column(name = "needs_budget")
    private Boolean needsBudget;

    // ===== Return-for-Revision routing metadata =====
    // Captured when an authorized reviewing office returns the proposal for
    // revision. The return is routed to RPS first, never directly to the Proponent.

    @Column(name = "returned_by_office")
    private String returnedByOffice;

    @Column(name = "returned_by_name")
    private String returnedByName;

    @Column(name = "returned_by_user_id")
    private Long returnedByUserId;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    // The original revision remarks from the reviewing office (immutable; never
    // overwritten by RPS when it forwards the request).
    @Column(name = "return_remarks", columnDefinition = "TEXT")
    private String returnRemarks;

    // Set only by RPS when forwarding the revision request to the Proponent.
    @Column(name = "revision_deadline")
    private LocalDateTime revisionDeadline;

    @Column(name = "revision_forwarded_at")
    private LocalDateTime revisionForwardedAt;

    @Column(name = "revision_forwarded_by_name")
    private String revisionForwardedByName;

    @Column(name = "revision_forwarded_by_user_id")
    private Long revisionForwardedByUserId;

    // Optional RPS note appended when forwarding (kept separate so the original
    // office remarks are never replaced).
    @Column(name = "revision_notes", columnDefinition = "TEXT")
    private String revisionNotes;

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
