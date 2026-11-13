package com.rpis.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One recorded, public-facing change to a Call for Proposal (proposal announcement).
 * <p>
 * A Call for Proposal keeps only its <em>current</em> values, so a deadline
 * extension would otherwise erase the original deadline. Every significant change an
 * administrator makes is appended here instead of overwriting the previous values:
 * what changed (type), the previous and new values, when it was announced, who made
 * it and the optional public explanation. The Proponent-facing Call for Proposal
 * details show this history in chronological order.
 * <p>
 * Rows are only ever written for genuine, recorded changes - nothing is fabricated
 * for calls that existed before the history was introduced.
 */
@Entity
@Table(name = "proposal_announcement_updates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProposalAnnouncementUpdate {

    /** A call was published (created as ACTIVE, activated, or a draft made ACTIVE). */
    public static final String TYPE_PUBLISHED = "PUBLISHED";
    /** The submission deadline was moved later than it was before. */
    public static final String TYPE_DEADLINE_EXTENDED = "DEADLINE_EXTENDED";
    /** The submission deadline moved (earlier, or a window change that is not an extension). */
    public static final String TYPE_DEADLINE_CHANGED = "DEADLINE_CHANGED";
    /** The stored status changed (for example ACTIVE -> CLOSED) without a deadline change. */
    public static final String TYPE_STATUS_CHANGED = "STATUS_CHANGED";
    /** Another significant detail changed (name, agenda selection, reminders, announcement text). */
    public static final String TYPE_DETAILS_UPDATED = "DETAILS_UPDATED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The Call for Proposal this change belongs to. A real relation (not a bare id)
     * so the history follows its call: deleting the call removes its history, and the
     * database keeps the reference honest. Never serialized (the update is always
     * delivered inside its announcement).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ProposalAnnouncement announcement;

    @Column(name = "update_type", nullable = false)
    private String updateType;

    @Column(name = "previous_status")
    private String previousStatus;

    @Column(name = "previous_start_date")
    private LocalDate previousStartDate;

    @Column(name = "previous_end_date")
    private LocalDate previousEndDate;

    @Column(name = "new_status")
    private String newStatus;

    @Column(name = "new_start_date")
    private LocalDate newStartDate;

    @Column(name = "new_end_date")
    private LocalDate newEndDate;

    /** The day the change was announced / takes effect (the public announcement date). */
    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    /** Optional public-facing announcement or explanation provided by the administrator. */
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "actor_id")
    private Long actorId;

    /** The name of the administrator/source responsible, captured when the change was made. */
    @Column(name = "actor_name")
    private String actorName;

    /** The role of the administrator/source responsible (for example RPS_ADMIN). */
    @Column(name = "actor_role")
    private String actorRole;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
