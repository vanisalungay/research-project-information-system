package com.rpis.backend.repository;

import com.rpis.backend.model.QuarterlyProgressReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface QuarterlyProgressReportRepository extends JpaRepository<QuarterlyProgressReport, Long> {

    List<QuarterlyProgressReport> findByProposalIdOrderByCreatedAtDesc(Long proposalId);

    List<QuarterlyProgressReport> findByProposalIdOrderByCreatedAtAsc(Long proposalId);

    /**
     * Submitted reports of a set of proposals, newest submission first. Used by the
     * OVCAF read-only listing; the caller has already restricted the proposal ids to
     * the projects OVCAF is authorized to see.
     */
    List<QuarterlyProgressReport> findByProposalIdInAndStatusOrderBySubmittedAtDescIdDesc(
            Collection<Long> proposalIds, String status);
}
