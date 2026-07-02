package com.rpis.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "proposal_log_frames")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProposalLogFrame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    @JsonIgnore
    private Proposal proposal;

    @Column(columnDefinition = "TEXT")
    private String outcome;

    @Column(columnDefinition = "TEXT")
    private String output;
}
