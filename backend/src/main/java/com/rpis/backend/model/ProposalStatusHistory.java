package com.rpis.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * One recorded status transition of a proposal: who changed it, from which status
 * to which, when and why.
 *
 * <p>The table is created by {@code backend/migration-03-db-audit-status-history.sql}
 * (and by Hibernate in the disposable test profile). That migration deliberately
 * created the table without reconstructing history; from then on every transition
 * made by the application must add a row here, transactionally with the status change.
 */
@Entity
@Table(name = "proposal_status_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProposalStatusHistory {

    /** The application made this change (the migration's baseline rows use BASELINE_SEED). */
    public static final String SOURCE_APPLICATION = "APPLICATION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Proposal proposal;

    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status", nullable = false)
    private String toStatus;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_role")
    private String actorRole;

    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "source", nullable = false)
    private String source = SOURCE_APPLICATION;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();
}
