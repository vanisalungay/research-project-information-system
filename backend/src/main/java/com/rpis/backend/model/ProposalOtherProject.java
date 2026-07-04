package com.rpis.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "proposal_other_projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProposalOtherProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    @JsonIgnore
    private Proposal proposal;

    private String title;
    private String agency;
    private String involvement;
}
