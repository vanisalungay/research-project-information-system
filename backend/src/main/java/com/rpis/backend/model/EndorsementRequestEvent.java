package com.rpis.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One entry of the append-only audit trail of an endorsement request.
 *
 * <p>Rows are only ever inserted - the service never updates or deletes them - so
 * the full history of a request (who created it, which Dean it was assigned to,
 * who approved or rejected it, when the letter was generated or reissued) stays
 * readable. The actor is always recorded from the authenticated principal, never
 * from the request body.
 *
 * <p>A row whose {@link #endorsementRequestId} is null is an account-level event
 * (a Dean role being assigned or revoked, or a Dean's college being changed).
 */
@Entity
@Table(name = "endorsement_request_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndorsementRequestEvent {

    public static final String TYPE_CREATED = "CREATED";
    public static final String TYPE_ASSIGNED = "ASSIGNED";
    public static final String TYPE_APPROVED = "APPROVED";
    public static final String TYPE_REJECTED = "REJECTED";
    public static final String TYPE_LETTER_GENERATED = "LETTER_GENERATED";
    public static final String TYPE_LETTER_REISSUED = "LETTER_REISSUED";
    public static final String TYPE_CERTIFICATION_CREATED = "CERTIFICATION_CREATED";
    public static final String TYPE_PROPOSAL_LINKED = "PROPOSAL_LINKED";
    public static final String TYPE_DEAN_ROLE_ASSIGNED = "DEAN_ROLE_ASSIGNED";
    public static final String TYPE_DEAN_ROLE_REVOKED = "DEAN_ROLE_REVOKED";
    public static final String TYPE_DEAN_SCOPE_CHANGED = "DEAN_SCOPE_CHANGED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The request this event belongs to. Null for account-level events (Dean role
     * assigned or revoked, Dean college changed), which are not tied to one request.
     */
    @Column(name = "endorsement_request_id")
    private Long endorsementRequestId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "actor_role")
    private String actorRole;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
