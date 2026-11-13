package com.rpis.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "proposal_id", nullable = false)
    private Long proposalId;
    
    @Column(name = "report_type", nullable = false)
    private String reportType; // QUARTERLY_PROGRESS, FINANCIAL
    
    @Column(name = "period", nullable = false)
    private String period; // Q1, Q2, Q3, Q4, FINAL
    
    @Column(name = "file_name")
    private String fileName;
    
    @Column(name = "file_path")
    private String filePath;
    
    @Column(name = "remarks", length = 1000)
    private String remarks;
    
    @Column(name = "submitted_by")
    private Long submittedBy;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "reviewed_by")
    private Long reviewedBy;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "review_status")
    private String reviewStatus; // PENDING, APPROVED, RETURNED
    
    @Column(name = "review_comments", length = 1000)
    private String reviewComments;
    
    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        reviewStatus = "PENDING";
    }
}