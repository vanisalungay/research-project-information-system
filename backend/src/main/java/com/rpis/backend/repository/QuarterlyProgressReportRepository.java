package com.rpis.backend.repository;

import com.rpis.backend.model.QuarterlyProgressReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuarterlyProgressReportRepository extends JpaRepository<QuarterlyProgressReport, Long> {

    List<QuarterlyProgressReport> findByProposalIdOrderByCreatedAtDesc(Long proposalId);
}
