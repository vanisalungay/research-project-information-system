package com.rpis.backend.repository;

import com.rpis.backend.model.ProposalCooperatingAgency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProposalCooperatingAgencyRepository extends JpaRepository<ProposalCooperatingAgency, Long> {

    /** The agencies selected on one proposal, in the directory's (name) order. */
    List<ProposalCooperatingAgency> findByProposalIdOrderByAgencyNameAsc(Long proposalId);

    /**
     * Removes every selection of a proposal. Used when the selection is replaced
     * (the join rows are recreated from the submitted ids) and when the proposal
     * itself is deleted, so no orphan row can keep an agency referenced.
     */
    void deleteByProposalId(Long proposalId);
}
