package com.rpis.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One entry of an administrator's activity history.
 *
 * <p>An entry answers four questions about one action the signed-in RPS
 * administrator actually performed: what was done ({@link #action} and
 * {@link #description}), in which part of RPIS ({@link #module}), with what
 * supporting information ({@link #details}), and when ({@link #createdAt}).
 *
 * <p>The actor is always taken from the authenticated principal, never from a
 * request body, so an entry cannot be attributed to somebody else. Rows are only
 * ever inserted: no service updates or deletes them, and no endpoint exposes
 * either operation.
 *
 * <p>Nothing sensitive is stored here. A password, a temporary password, an
 * authentication token or a signature never reaches {@link #details} - the
 * recording methods take plain descriptive text and the callers pass only the
 * account name, the module object's name, or a status change.
 */
@Entity
@Table(name = "activity_logs", indexes = {
        @Index(name = "idx_activity_logs_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_activity_logs_module", columnList = "module"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {

    // ---- Actions -----------------------------------------------------------------
    public static final String ACTION_ACCOUNT_CREATED = "ACCOUNT_CREATED";
    public static final String ACTION_ACCOUNT_UPDATED = "ACCOUNT_UPDATED";
    public static final String ACTION_ACCOUNT_APPROVED = "ACCOUNT_APPROVED";
    public static final String ACTION_ACCOUNT_REJECTED = "ACCOUNT_REJECTED";
    public static final String ACTION_ROLE_CHANGED = "ROLE_CHANGED";
    public static final String ACTION_PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String ACTION_PROFILE_UPDATED = "PROFILE_UPDATED";
    public static final String ACTION_CALL_CREATED = "CALL_CREATED";
    public static final String ACTION_CALL_UPDATED = "CALL_UPDATED";
    public static final String ACTION_CALL_OPENED = "CALL_OPENED";
    public static final String ACTION_CALL_CLOSED = "CALL_CLOSED";
    public static final String ACTION_SETTINGS_UPDATED = "SETTINGS_UPDATED";

    // ---- Modules -----------------------------------------------------------------
    public static final String MODULE_USER_ACCOUNTS = "User Accounts";
    public static final String MODULE_PROFILE = "Profile";
    public static final String MODULE_CALLS_FOR_PROPOSAL = "Calls for Proposal";
    public static final String MODULE_SYSTEM_SETTINGS = "System Settings";

    // ---- Status ------------------------------------------------------------------
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The administrator who performed the action - the actor, never the affected account. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** The actor's name as it stood when the action was performed. */
    @Column(name = "user_name")
    private String userName;

    /** Stable identifier of the action, e.g. {@link #ACTION_ACCOUNT_CREATED}. */
    @Column(name = "action", nullable = false)
    private String action;

    /** The readable line the Activity Log table shows. */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /** The RPIS module the action happened in. */
    @Column(name = "module", nullable = false)
    private String module;

    /** The supporting information the View Details panel shows. */
    @Column(columnDefinition = "TEXT")
    private String details;

    /** {@link #STATUS_SUCCESS} or {@link #STATUS_FAILED}. */
    @Column(name = "status", nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
