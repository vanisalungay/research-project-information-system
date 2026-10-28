package com.rpis.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A Faculty Proponent's request for a Dean's / Department Chair's endorsement.
 *
 * <p>It carries only the preliminary information needed to endorse a project -
 * the Program Title and the Project Title, the Call for Proposal it answers and the
 * organizational unit - because it is raised <em>before</em> the full proposal
 * submission. The two titles are independent values: the Program Title is optional
 * (the programme the project belongs to) and the Project Title is the required title
 * of the project itself. A request is assigned to the Dean authorized for its college
 * and moves through the three business states {@code PENDING} → {@code APPROVED} /
 * {@code REJECTED}.
 *
 * <p>The approving identity is never taken from the client: {@link #decidedByUserId}
 * is written from the authenticated Dean's token (see
 * {@code EndorsementRequestService}). The issued letter and the signature used for
 * it are referenced here as stored files under {@code uploads/endorsements/}, so the
 * issued document is immutable even if the Dean later replaces the signature on
 * their account.
 */
@Entity
@Table(name = "endorsement_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndorsementRequest {

    /** Waiting for the authorized Dean's review. */
    public static final String STATUS_PENDING = "PENDING";
    /** Approved and the official endorsement letter has been issued. */
    public static final String STATUS_APPROVED = "APPROVED";
    /** Rejected by the authorized Dean. */
    public static final String STATUS_REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique reference, format {@code MSUN-ORPS-END-<code>-<year>}. */
    @Column(name = "reference_number", nullable = false, unique = true)
    private String referenceNumber;

    @ManyToOne
    @JoinColumn(name = "proponent_id", nullable = false)
    private User proponent;

    /** The Dean the request was routed to; null while no Dean is configured. */
    @ManyToOne
    @JoinColumn(name = "assigned_dean_id")
    private User assignedDean;

    @Column(nullable = false)
    private String college;

    private String department;

    /**
     * The Program Title the Proponent selected or created - the programme the project
     * belongs to. It is <em>optional</em>, exactly like the Create Proposal form, so it
     * is nullable and it never substitutes for {@link #projectTitle}.
     */
    @Column(name = "program_title")
    private String programTitle;

    /**
     * The title of the specific project being endorsed. Required.
     *
     * <p>The column keeps its historic name {@code proposal_title}. It has always held
     * the project's title - it is what the official endorsement letter prints and what
     * the Certifications entry is named after - so renaming the column would rewrite
     * every existing record for no behavioural gain. The API exposes this value as
     * {@code projectTitle}.
     */
    @Column(name = "proposal_title", nullable = false)
    private String projectTitle;

    @Column(name = "proposal_announcement_id", nullable = false)
    private Long proposalAnnouncementId;

    @Column(name = "research_summary", columnDefinition = "TEXT")
    private String researchSummary;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private String status = STATUS_PENDING;

    /** Id of the authenticated Dean who decided. Never accepted from the client. */
    @Column(name = "decided_by_user_id")
    private Long decidedByUserId;

    @Column(name = "decided_by_name")
    private String decidedByName;

    @Column(name = "decided_by_designation")
    private String decidedByDesignation;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "endorsement_date")
    private LocalDate endorsementDate;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /** Document code printed on the issued letter; unique across the university. */
    @Column(name = "letter_document_code", unique = true)
    private String letterDocumentCode;

    @Column(name = "letter_file_name")
    private String letterFileName;

    @Column(name = "letter_file_path", length = 500)
    private String letterFilePath;

    /** 1 for the first issue; incremented by an explicit, authorized reissue. */
    @Column(name = "letter_version")
    private Integer letterVersion;

    @Column(name = "letter_generated_at")
    private LocalDateTime letterGeneratedAt;

    /** Display name of the signature image the letter was signed with. */
    @Column(name = "signature_file_name")
    private String signatureFileName;

    /** Stored copy of that signature image, relative to {@code uploads}. */
    @Column(name = "signature_file_path", length = 500)
    private String signatureFilePath;

    /** The proposal the Proponent continued to, once it exists. */
    @Column(name = "proposal_id")
    private Long proposalId;

    /** The Certifications entry that carries the issued letter. */
    @Column(name = "certification_id")
    private Long certificationId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** True once the official letter has been issued (the PDF exists on disk). */
    @Transient
    public boolean hasIssuedLetter() {
        return letterFilePath != null && !letterFilePath.isBlank();
    }
}
