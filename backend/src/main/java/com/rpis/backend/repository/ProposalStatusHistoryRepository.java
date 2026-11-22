package com.rpis.backend.repository;

import com.rpis.backend.model.ProposalStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProposalStatusHistoryRepository extends JpaRepository<ProposalStatusHistory, Long> {

    /**
     * One proposal's audited status transitions, newest first. The id tie-breaker keeps
     * events recorded in the same transaction (or the same second) in a stable order.
     */
    List<ProposalStatusHistory> findByProposalIdOrderByChangedAtDescIdDesc(Long proposalId);
}
