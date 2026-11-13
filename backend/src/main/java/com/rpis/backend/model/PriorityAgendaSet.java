package com.rpis.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A Priority Agenda framework / agenda set: the framework or program name (for
 * example {@code MSUN RIIDE}) together with the coverage period (start year and
 * end year) it applies to.
 * <p>
 * Section 05 of the Create Proposal form used to hard-code the framework label
 * ("based on MSUN RIIDE 2025-2028") and the list of agendas. Both are now master
 * data: an administrator creates a set, configures its framework name and coverage
 * period, adds the agenda items and activates it. Exactly one set may be active for
 * new submissions at a time, which is enforced by the partial unique index
 * {@code uk_priority_agenda_set_single_active} (documented in
 * {@code backend/migration-add-priority-agendas.sql} and created idempotently by
 * {@code PriorityAgendaInitializer}).
 * <p>
 * A set is never deleted - it is deactivated, so historical proposals keep
 * referencing the agenda items they were submitted under.
 */
@Entity
@Table(name = "priority_agenda_sets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriorityAgendaSet {

    /** Longest accepted framework name (mirrors the column). */
    public static final int FRAMEWORK_NAME_MAX_LENGTH = 150;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "framework_name", nullable = false, length = FRAMEWORK_NAME_MAX_LENGTH)
    private String frameworkName;

    @Column(name = "coverage_start_year", nullable = false)
    private Integer coverageStartYear;

    @Column(name = "coverage_end_year", nullable = false)
    private Integer coverageEndYear;

    /**
     * True for the set new proposals select their agendas from. At most one row may
     * be active, guaranteed by the partial unique index on {@code active = TRUE}.
     */
    @Column(name = "active", nullable = false)
    private Boolean active = false;

    /**
     * The agenda items of this set, in display order. Deactivated items stay in the
     * list (so an administrator can see and re-activate them) and are simply not
     * offered to new proposals.
     */
    @OneToMany(mappedBy = "agendaSet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<PriorityAgenda> agendas = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
