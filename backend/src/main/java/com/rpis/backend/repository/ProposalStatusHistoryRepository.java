package com.rpis.backend.repository;

import com.rpis.backend.model.ProposalStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProposalStatusHistoryRepository extends JpaRepository<ProposalStatusHistory, Long> {

    /**
     * One proposal's audited status transitions, newest first. The id tie-breaker keeps
     * events recorded in the same transaction (or the same second) in a stable order.
     */
    List<ProposalStatusHistory> findByProposalIdOrderByChangedAtDescIdDesc(Long proposalId);

    /**
     * The real receipt event(s) for a set of proposals: the application-recorded
     * transition into FOR_OVCAF_APPROVAL. Baseline seed rows are not events and are
     * excluded. Ordered oldest first so the first row per proposal is the moment OVCAF
     * received it.
     */
    @Query("SELECT h FROM ProposalStatusHistory h WHERE h.proposal.id IN :proposalIds "
            + "AND h.toStatus = 'FOR_OVCAF_APPROVAL' AND h.source <> 'BASELINE_SEED' "
            + "ORDER BY h.changedAt ASC, h.id ASC")
    List<ProposalStatusHistory> findOvcafReceiptEvents(@Param("proposalIds") Collection<Long> proposalIds);

    /** All audited rows of a set of proposals, oldest first, for one batch read. */
    List<ProposalStatusHistory> findByProposalIdInOrderByChangedAtAscIdAsc(
            Collection<Long> proposalIds);
}
