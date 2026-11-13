package com.rpis.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Join row of the many-to-many relation between a proposal and the shared
 * cooperating agency directory ({@code proposal_cooperating_agencies}).
 * <p>
 * One row per selected agency, so a proposal can name any number of agencies and
 * each {@link CooperatingAgency} row is reused by every proposal that selects it.
 * The unique constraint on {@code (proposal_id, cooperating_agency_id)} makes a
 * duplicated selection impossible. Rows are replaced when the proposal is saved
 * and removed when the proposal is deleted; the agency directory itself is never
 * touched by either operation.
 */
@Entity
@Table(name = "proposal_cooperating_agencies", uniqueConstraints = @UniqueConstraint(
        name = "uk_proposal_cooperating_agency", columnNames = { "proposal_id", "cooperating_agency_id" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProposalCooperatingAgency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposal_id", nullable = false)
    @JsonIgnore
    private Proposal proposal;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cooperating_agency_id", nullable = false)
    private CooperatingAgency agency;
}
