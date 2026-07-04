package com.rpis.backend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ProposalRequest {
    private Long proponentId;
    private String programTitle;
    private String projectTitle;
    private String projectLeader;
    private String duration;
    private String startDate; // Keep as String to avoid parsing exceptions, convert in service
    private String endDate;
    private String college;
    private String address;
    private String cooperatingAgencies;
    private List<SiteDTO> sites;
    private String researchType;
    private Map<String, PriorityAgendaDTO> priorityAgenda;
    private String innovationGoals;
    private String sectorRelevance;
    private String sdg;
    private String executiveSummary;
    private String rationale;
    private String framework;
    private String objectivesGeneral;
    private String objectivesSpecific;
    private String review;
    private String reviewFileName;
    private String methodology;
    private String roadmapFileName;
    private String technologyTrl;
    private String outputs;
    private String outcomes;
    private String impactEconomic;
    private String impactSocial;
    private String beneficiaries;
    private String beneficiariesFileName;
    private String sustainability;
    private BigDecimal gadScore;
    private String gadFileName;
    private List<LimitationDTO> limitationsTable;
    private String risks;
    private List<LogFrameDTO> logFrame;
    private List<ReferenceDTO> references;
    private List<PersonnelDTO> personnel;
    private List<BudgetDTO> budget;
    private String otherProjectsNumber;
    private List<OtherProjectDTO> otherProjects;
    private String status;

    @Data
    public static class SiteDTO {
        private String country;
        private String region;
        private String province;
        private String district;
        private String municipality;
        private String barangay;
    }

    @Data
    public static class PriorityAgendaDTO {
        private Boolean selected;
        private String value;
    }

    @Data
    public static class LimitationDTO {
        private String limitation;
        private String remarks;
    }

    @Data
    public static class LogFrameDTO {
        private String outcome;
        private String output;
    }

    @Data
    public static class ReferenceDTO {
        private String author;
        private String title;
        private String year;
    }

    @Data
    public static class PersonnelDTO {
        private String position;
        private String time;
        private String responsibilities;
    }

    @Data
    public static class BudgetDTO {
        private String agency;
        private String ps;
        private String mooe;
        private String eo;
        private String total;
    }

    @Data
    public static class OtherProjectDTO {
        private String title;
        private String agency;
        private String involvement;
    }
}
