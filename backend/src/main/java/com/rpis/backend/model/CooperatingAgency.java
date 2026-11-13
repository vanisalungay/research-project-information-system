package com.rpis.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One cooperating agency of the shared RPIS directory (section 02 of the Create
 * Proposal form).
 * <p>
 * The directory is a small lookup table: every row is reusable by any number of
 * proposals and an agency is never duplicated per proposal, nor deleted when a
 * proponent unchecks it from their own proposal. Duplicate rows are prevented by
 * the case-insensitive unique index {@code uk_cooperating_agencies_name_lower},
 * created idempotently by {@code CooperatingAgencyInitializer} and documented in
 * {@code backend/migration-add-cooperating-agencies.sql}.
 */
@Entity
@Table(name = "cooperating_agencies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CooperatingAgency {

    /** Longest accepted agency name (mirrors the column). */
    public static final int NAME_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
