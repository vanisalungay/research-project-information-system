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
    private String projectLeaderSex;
    private String duration;
    private String startDate; // Keep as String to avoid parsing exceptions, convert in service
    private String endDate;
    private String college;
    private String address;
    // Structured address of the "Address *" section (Create Proposal form,
    // section 01). Sent only by clients that have the split fields - a client
    // that predates them leaves them null and the saved address is preserved.
    private String addressCountry;
    private String addressRegion;
    private String addressProvince;
    private String addressDistrict;
    private String addressMunicipality;
    private String addressBarangay;
    // "Contact *" section: the chosen contact type (CONTACT_NUMBER or
    // EMAIL_ADDRESS) and its value.
    private String contactType;
    private String contactValue;
    // "Cooperating Agencies" section: the free-text value (kept for clients that
    // predate the checklist) and the ids of the agencies selected from the shared
    // directory. Null ids mean "this client does not use the directory" and leave
    // the stored selection untouched.
    private String cooperatingAgencies;
    private List<Long> cooperatingAgencyIds;
    private List<SiteDTO> sites;
    private String researchType;
    private Map<String, PriorityAgendaDTO> priorityAgenda;
    private String innovationGoals;
    private String sectorRelevance;
    private List<String> sdg;
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
    private String limitationsText;
    private String risks;
    private List<LogFrameDTO> logFrame;
    // Section 23 (Literature Cited): the plain-text rendering of every reference and,
    // when the form used the reference builder, the structured entries (type + values)
    // as JSON so the list can be reopened for editing and the italics survive. A null
    // value means "this client does not edit the section" and leaves the stored value
    // untouched, so a partial update can never wipe the literature cited.
    private String referencesText;
    private String referencesJson;
    private List<ReferenceDTO> references;
    private List<PersonnelDTO> personnel;
    private List<BudgetDTO> budget;
    private String otherProjectsNumber;
    private List<OtherProjectDTO> otherProjects;
    // The Call for Proposal (proposal announcement) the proponent selected in step 1
    // of the Create Proposal workflow. When it is sent, the proposal is attached to
    // that call - and only to it - instead of the implicit "currently active"
    // announcement. Null keeps the previous behaviour for clients that do not use the
    // selection step.
    private Long proposalAnnouncementId;
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
