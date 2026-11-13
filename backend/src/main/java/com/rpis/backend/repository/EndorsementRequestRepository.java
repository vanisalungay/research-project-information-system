package com.rpis.backend.repository;

import com.rpis.backend.model.EndorsementRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EndorsementRequestRepository extends JpaRepository<EndorsementRequest, Long> {

    /** The authenticated Proponent's own requests, newest first. */
    List<EndorsementRequest> findByProponentIdOrderByCreatedAtDesc(Long proponentId);

    /** The requests routed to one Dean, newest first. */
    List<EndorsementRequest> findByAssignedDeanIdOrderByCreatedAtDesc(Long deanId);

    /** Requests that have no Dean configured yet (unassigned college). */
    List<EndorsementRequest> findByAssignedDeanIdIsNullOrderByCreatedAtDesc();

    /**
     * Loads one request with a row-level write lock so two simultaneous approvals
     * (or an approval racing a rejection) are serialized: the second transaction
     * waits, then sees the status the first one committed and is refused. Same
     * pattern as {@code ProposalRepository.findByIdForUpdate}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from EndorsementRequest r where r.id = :id")
    Optional<EndorsementRequest> findByIdForUpdate(@Param("id") Long id);

    /** The endorsement the given proposal was submitted under, if any. */
    Optional<EndorsementRequest> findByProposalId(Long proposalId);

    /** True when the endorsement is already backing a proposal. */
    boolean existsByProposalId(Long proposalId);

    List<EndorsementRequest> findByProposalAnnouncementId(Long proposalAnnouncementId);

    long countByStatus(String status);

    long countByCollegeIgnoreCaseAndStatus(String college, String status);
}
