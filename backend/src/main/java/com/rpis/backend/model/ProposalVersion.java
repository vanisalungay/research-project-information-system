package com.rpis.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Immutable snapshot of a single proposal submission/revision.
 * <p>
 * Each time a proposal is submitted (original submission or a revision after a
 * "returned for revision"), a new row is appended here. The live {@link Proposal}
 * row always reflects the latest/current version, while this table preserves the
 * complete, non-overwriting history of every submission.
 */
@Entity
@Table(name = "proposal_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProposalVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    @JsonIgnore
    private Proposal proposal;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "document_id")
    private String documentId;

    @Column(name = "status")
    private String status;

    @ManyToOne
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    @Column(name = "submitted_by_name")
    private String submittedByName;

    @Column(name = "submission_date")
    private LocalDateTime submissionDate;

    @Column(name = "revision_date")
    private LocalDateTime revisionDate;

    @Column(name = "revision_remarks", columnDefinition = "TEXT")
    private String revisionRemarks;

    /** Full JSON snapshot of the proposal details + files + child collections. */
    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "is_current")
    private Boolean isCurrent = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
