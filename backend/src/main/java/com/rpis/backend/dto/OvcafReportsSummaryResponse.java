package com.rpis.backend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class OvcafReportsSummaryResponse {

    private BigDecimal totalValidatedBudget;
    private BigDecimal averageProjectBudget;
    private double endorsementRate;

    private Map<String, Long> statusCounts;
    private Map<String, BigDecimal> statusBudgets;

    private List<CollegeSummary> collegeSummary;

    private long totalProposals;
    private long approvedCount;
    private long pendingCount;
    private long returnedCount;
    private long rejectedCount;

    @Data
    public static class CollegeSummary {
        private String college;
        private long proposalCount;
        private BigDecimal totalBudget;
    }
}