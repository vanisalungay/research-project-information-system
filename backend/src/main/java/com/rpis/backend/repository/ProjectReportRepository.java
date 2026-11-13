package com.rpis.backend.repository;

import com.rpis.backend.model.ProjectReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectReportRepository extends JpaRepository<ProjectReport, Long> {
    
    List<ProjectReport> findByProposalId(Long proposalId);
    
    List<ProjectReport> findByProposalIdAndReportType(Long proposalId, String reportType);
    
    List<ProjectReport> findByReviewStatus(String reviewStatus);
    
    List<ProjectReport> findBySubmittedBy(Long submittedBy);
}