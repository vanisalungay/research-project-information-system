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
 * Proposal Announcement: defines the date window (start date to end date) during
 * which proponents are allowed to submit new proposals.
 */
@Entity
@Table(name = "proposal_announcements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProposalAnnouncement {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CLOSED = "CLOSED";

    /** Internally Funded Project: the default funding program of a Call for Proposal. */
    public static final String FUNDING_PROGRAM_IFP = "IFP";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String status = STATUS_DRAFT;

    /**
     * Stable MSUN RIIDE 2025–2028 priority-agenda program keys selected for this
     * call (for example {@code dagat} or {@code punla}), stored as a JSON array in
     * a TEXT column. An announcement may carry no selected agenda.
     */
    @Convert(converter = StringListConverter.class)
    @Column(name = "priority_agendas", columnDefinition = "TEXT")
    private List<String> priorityAgendas = new ArrayList<>();

    /**
     * Stable ids of the standard announcement reminders selected for this call,
     * stored as a JSON array in a TEXT column. A newly created announcement
     * defaults to every standard reminder; the saved selection is always restored
     * when the announcement is edited.
     */
    @Convert(converter = StringListConverter.class)
    @Column(name = "reminder_ids", columnDefinition = "TEXT")
    private List<String> reminderIds = new ArrayList<>();

    /**
     * Optional free-text announcement body (plain text, line breaks preserved).
     * Rendered as text by the UI, never as HTML.
     */
    @Column(name = "additional_announcement", columnDefinition = "TEXT")
    private String additionalAnnouncement;

    /**
     * Funding program (call type) of this Call for Proposal, as a stable key. The
     * endorsement workflow uses it to choose the official endorsement-letter
     * template: the only template issued today is the Internally Funded Project
     * Proposal (IFP) letter, so a call for any other program cannot be endorsed
     * until its own official wording is configured (see
     * {@code EndorsementLetterTemplates}).
     *
     * <p>The column carries the documented default so a row inserted without the
     * field (a legacy client, a maintenance script, an existing record) is treated as
     * an IFP call instead of failing the not-null constraint.
     */
    @Column(name = "funding_program", nullable = false,
            columnDefinition = "VARCHAR(40) NOT NULL DEFAULT 'IFP'")
    private String fundingProgram = FUNDING_PROGRAM_IFP;

    /**
     * Computed (not persisted) flag set when loading the announcement list:
     * true when at least one proponent has already submitted a proposal under this
     * announcement. Used by the RPS Staff UI to disable the Delete action.
     */
    @Transient
    private Boolean hasSubmittedProposals;

    /**
     * Computed (not persisted) history of the significant published changes recorded
     * for this call, oldest first. Populated by the service for the Proponent-facing
     * Call for Proposals details view so a deadline extension never hides the
     * original deadline or the earlier announcements.
     */
    @Transient
    private List<ProposalAnnouncementUpdate> updates = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
