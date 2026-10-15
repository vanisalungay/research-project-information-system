package com.rpis.backend.repository;

import com.rpis.backend.model.ProposalSectionRemark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProposalSectionRemarkRepository extends JpaRepository<ProposalSectionRemark, Long> {

    /** Full remark history of a proposal, newest round first. */
    List<ProposalSectionRemark> findByProposalIdOrderByRoundDescIdAsc(Long proposalId);

    /** Remarks of a single revision round. */
    List<ProposalSectionRemark> findByProposalIdAndRoundOrderByIdAsc(Long proposalId, Integer round);

    /** Remarks in the given status (used to resolve the currently open round). */
    List<ProposalSectionRemark> findByProposalIdAndStatus(Long proposalId, String status);

    /** Highest revision round recorded for a proposal, or {@code null} when none. */
    @Query("SELECT MAX(r.round) FROM ProposalSectionRemark r WHERE r.proposal.id = :proposalId")
    Integer findMaxRound(@Param("proposalId") Long proposalId);
}
