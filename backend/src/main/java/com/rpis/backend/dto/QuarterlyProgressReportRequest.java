package com.rpis.backend.dto;

import com.rpis.backend.model.QuarterlyObjective;
import com.rpis.backend.model.QuarterlyOutput;
import lombok.Data;

import java.util.List;

@Data
public class QuarterlyProgressReportRequest {

    private Long proposalId;
    private String period; // Q1, Q2, Q3, Q4
    private Integer year;
    private String status; // DRAFT or SUBMITTED

    private String projectTitle;
    private String projectLeader;
    private String leaderGender;
    private String baseStation;
    private String sitesOfImplementation;
    private String projectDuration;
    private String projectStartDate; // ISO date string (yyyy-MM-dd)
    private String projectEndDate;   // ISO date string (yyyy-MM-dd)

    private String catchUpPlan;
    private String problemsConcerns;
    private String suggestedSolutions;

    private List<QuarterlyObjective> objectives;
    private List<QuarterlyOutput> outputs;
}
