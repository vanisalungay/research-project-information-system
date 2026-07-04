package com.rpis.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "proposal_limitations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProposalLimitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    @JsonIgnore
    private Proposal proposal;

    @Column(columnDefinition = "TEXT")
    private String limitation;

    @Column(columnDefinition = "TEXT")
    private String remarks;
}
