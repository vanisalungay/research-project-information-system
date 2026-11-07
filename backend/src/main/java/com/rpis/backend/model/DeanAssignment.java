package com.rpis.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One appointment of a Dean / Department Chair to a college or department.
 *
 * <p>The account alone cannot carry the history: {@code users.assigned_college} holds the
 * <em>current</em> scope of the sitting Dean and is overwritten every time leadership
 * changes, so overwriting it would erase the previous appointment. This entity keeps every
 * appointment as its own row instead - the sitting Dean is the one row with
 * {@link #STATUS_ACTIVE}, and every predecessor is retained with {@link #STATUS_REVOKED},
 * its {@link #dateRevoked} and the administrator ({@link #revokedBy}) who replaced it.
 *
 * <p>Rows are never deleted. A revoked appointment is history: the former Dean's account,
 * its activity records and this row all stay readable, which is what a succession audit
 * needs. The dean's name and e-mail are snapshotted here as well as referenced by
 * {@link #userId}, so the historical row reads correctly even if the account is later
 * renamed.
 */
@Entity
@Table(name = "dean_assignments", indexes = {
        @Index(name = "idx_dean_assignments_user", columnList = "user_id"),
        @Index(name = "idx_dean_assignments_status", columnList = "status"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeanAssignment {

    /** The sitting appointment. At most one ACTIVE row exists per college. */
    public static final String STATUS_ACTIVE = "ACTIVE";

    /** A former appointment, retained as history. Never deleted. */
    public static final String STATUS_REVOKED = "REVOKED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The Dean's account (dean/user reference). The account itself is never deleted. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** The Dean's full name as it stood when the appointment was made. */
    @Column(name = "dean_name")
    private String deanName;

    /** The Dean's institutional e-mail as it stood when the appointment was made. */
    @Column(name = "dean_email")
    private String deanEmail;

    /** The college / department the appointment is scoped to. Null = institution-wide. */
    @Column(name = "assigned_college")
    private String assignedCollege;

    /** Designation / title, e.g. "Dean" or "Department Chair". */
    private String designation;

    /** {@link #STATUS_ACTIVE} or {@link #STATUS_REVOKED}. */
    @Column(nullable = false)
    private String status;

    /** When this appointment took effect. */
    @Column(name = "date_assigned", nullable = false)
    private LocalDateTime dateAssigned;

    /** When this appointment ended, or null while it is still active. */
    @Column(name = "date_revoked")
    private LocalDateTime dateRevoked;

    /** The administrator who made the appointment, and their name at the time. */
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_by_name")
    private String createdByName;

    /** The administrator who revoked the appointment, and their name at the time. */
    @Column(name = "revoked_by")
    private Long revokedBy;

    @Column(name = "revoked_by_name")
    private String revokedByName;
}
