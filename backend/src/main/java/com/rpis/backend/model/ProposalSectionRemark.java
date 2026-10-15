package com.rpis.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A reviewer's remark for one specific section of a proposal.
 * <p>
 * Each row is bound to exactly one {@link Proposal} and one proposal section
 * ({@code section_key}, e.g. {@code "12"} = Methodology). Remarks are grouped into
 * revision <em>rounds</em>: when an authorized reviewing office returns a proposal
 * for revision, a new round is started and every previously active remark is
 * demoted to {@link #STATUS_SUPERSEDED} instead of being overwritten, so the full
 * feedback history of every revision cycle is retained.
 * <p>
 * Rows are never deleted by the proposal submission/revision flow, so the remarks a
 * Proponent is working from survive editing and resubmission.
 */
@Entity
@Table(name = "proposal_section_remarks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProposalSectionRemark {

    /** The remark belongs to the current (open) revision round. */
    public static final String STATUS_ACTIVE = "ACTIVE";

    /** The remark belongs to a completed revision round and is kept for history. */
    public static final String STATUS_SUPERSEDED = "SUPERSEDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposal_id", nullable = false)
    @JsonIgnore
    private Proposal proposal;

    /** Canonical section key, e.g. "01" ... "27". */
    @Column(name = "section_key", nullable = false, length = 10)
    private String sectionKey;

    /** Human-readable section label captured with the remark. */
    @Column(name = "section_label", length = 150)
    private String sectionLabel;

    /**
     * Stable path of the field inside the section, e.g. {@code projectTitle} or
     * {@code personnel[1].time}. {@code null} means the remark is a section-level
     * remark (the original behaviour).
     */
    @Column(name = "field_key", length = 120)
    private String fieldKey;

    /** Human-readable field label captured with the remark. */
    @Column(name = "field_label", length = 255)
    private String fieldLabel;

    /**
     * Zero-based position of the repeated entry that was reviewed. Only a last
     * resort for re-aligning the remark, because the backend regenerates child row
     * ids (and therefore ordering) on every submission.
     */
    @Column(name = "entry_index")
    private Integer entryIndex;

    /** Best-effort stable id of the repeated entry at review time. */
    @Column(name = "entry_id")
    private Long entryId;

    /** JSON snapshot of the repeated entry as reviewed (used to re-align remarks). */
    @Column(name = "entry_snapshot", columnDefinition = "TEXT")
    private String entrySnapshot;

    @Column(name = "remark", columnDefinition = "TEXT", nullable = false)
    private String remark;

    /** 1-based revision round this remark belongs to. */
    @Column(name = "round", nullable = false)
    private Integer round;

    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_ACTIVE;

    /** Office/role that authored the remark, e.g. REC, OVCRIGE, RPS. */
    @Column(name = "reviewer_role", length = 50)
    private String reviewerRole;

    @Column(name = "reviewer_name", length = 255)
    private String reviewerName;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
