package com.rpis.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * One Priority Agenda item of a {@link PriorityAgendaSet} (section 05 of the Create
 * Proposal form).
 * <p>
 * {@code agendaKey} is the stable identifier persisted with every proposal in
 * {@code proposal_priority_agendas.agenda_key} (for example {@code dagat}). It is
 * generated once when the item is created and is never editable afterwards: it is
 * the link between the master data and every historical proposal, so renaming the
 * item must never break an existing record. The display {@code name} and the
 * {@code subLabel} prompt ("Commodity", "Priority Topic", "Sector") can be edited
 * freely, and {@code active} decides whether new proposals may select the item.
 * <p>
 * Items are never deleted - only deactivated - so a proposal submitted under an
 * agenda that is no longer offered keeps showing exactly what was selected.
 */
@Entity
@Table(name = "priority_agendas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriorityAgenda {

    /** Longest accepted agenda name (mirrors the column). */
    public static final int NAME_MAX_LENGTH = 150;

    /**
     * Longest accepted stable key (mirrors the column). It matches
     * {@code proposal_priority_agendas.agenda_key} (VARCHAR(50)) so a key that is
     * accepted here can always be persisted on a proposal.
     */
    public static final int KEY_MAX_LENGTH = 50;

    /** Longest accepted sub-label prompt (mirrors the column). */
    public static final int SUB_LABEL_MAX_LENGTH = 80;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agenda_set_id", nullable = false)
    @JsonIgnore
    private PriorityAgendaSet agendaSet;

    /** Stable identifier persisted with every proposal that selects this item. */
    @Column(name = "agenda_key", nullable = false, length = KEY_MAX_LENGTH)
    private String agendaKey;

    /** Display name, for example "DAGAT Program". */
    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    /** Prompt shown beside the value input, for example "Commodity". */
    @Column(name = "sub_label", length = SUB_LABEL_MAX_LENGTH)
    private String subLabel;

    /** Optional longer description (the RIIDE expansion, for example). */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Order the items are shown in; lower first, ties broken by id. */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /** Whether new proposals may select this item. */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
