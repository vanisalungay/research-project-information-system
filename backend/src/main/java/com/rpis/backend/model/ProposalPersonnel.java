package com.rpis.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "proposal_personnel")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProposalPersonnel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    @JsonIgnore
    private Proposal proposal;

    private String position;

    @Column(name = "time_devoted")
    private String timeDevoted;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;
}
