package com.rpis.backend.repository;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findByProponent(User proponent);

    // Owner lookup by id, used to authorize attachment downloads without trusting
    // a client-supplied owner id.
    List<Proposal> findByProponentId(Long proponentId);

    List<Proposal> findByStatus(String status);

    // Owner-scoped queries used to keep a Proponent's view limited to their own
    // records while still honoring the status/statusIn filters.
    List<Proposal> findByProponentAndStatus(User proponent, String status);

    List<Proposal> findByProponentAndStatusIn(User proponent, List<String> statuses);

    @Query("SELECT p FROM Proposal p WHERE p.status IN :statuses")
    List<Proposal> findByStatusIn(@Param("statuses") List<String> statuses);

    /**
     * Loads a proposal while holding a row-level write lock for the duration of the
     * transaction. Used by the revision workflow so that two simultaneous revision
     * resubmissions of the same proposal are serialized and cannot allocate the
     * same revision number.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Proposal p WHERE p.id = :id")
    Optional<Proposal> findByIdForUpdate(@Param("id") Long id);

    /**
     * Atomically allocates the next continuous proposal code from a PostgreSQL
     * sequence. Safe across concurrent requests and multiple backend instances;
     * replaces the previous (racy) MAX()+1 approach. Declared as {@link Number} so
     * the result maps cleanly regardless of the driver's bigint representation.
     */
    @Query(value = "SELECT nextval('proposal_code_seq')", nativeQuery = true)
    Number nextProposalCodeSequence();

    // Approved-for-implementation projects that already have an issued Special
    // Order (SO) — these are the only projects eligible for Quarterly Progress Reports.
    @Query("SELECT p FROM Proposal p WHERE p.soNumber IS NOT NULL AND p.soNumber <> '' AND p.status IN :statuses ORDER BY p.updatedAt DESC")
    List<Proposal> findEligibleForQuarterlyReport(@Param("statuses") List<String> statuses);

    // Distinct announcement IDs that have at least one submitted (non-DRAFT)
    // proposal. Used to block deletion of announcements that are in use.
    @Query("SELECT DISTINCT p.proposalAnnouncementId FROM Proposal p WHERE p.proposalAnnouncementId IS NOT NULL AND p.status <> 'DRAFT'")
    List<Long> findAnnouncementIdsWithSubmittedProposals();
}
