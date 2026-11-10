package com.rpis.backend.dto;

import com.rpis.backend.model.GadChecklistData;
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
    /**
     * The endorsement document the proponent uploads in the Certification &amp;
     * Endorsement step ("Endorsed By"). The bytes never travel in this DTO: they are
     * uploaded to the proposal's own endpoint
     * (POST /api/proposals/{id}/endorsement-document), which validates the PDF, stores it
     * and sets both the display name and the stored path. This field is accepted so an
     * API client that manages the document itself can keep the display name in step;
     * a null value (what the form sends) leaves the stored name untouched.
     */
    private String endorsementFileName;
    /*
     * "Submitted By" / "Endorsed By" of the Certification & Endorsement step. Only the
     * values the proponent types travel here: the uploaded signature and the endorsement
     * PDF have their own endpoints, which set the display name and stored path themselves,
     * so a client can never point a proposal at an arbitrary stored file. A null value
     * means "this client does not edit the section" and leaves the stored value untouched
     * (the create flow sends null when nothing was removed).
     */
    private String submittedByName;
    private String submittedByDesignation;
    private String submittedByDate;
    private String endorsedByName;
    private String endorsedByDesignation;
    private String endorsedByDate;
    private String sustainability;
    private BigDecimal gadScore;
    private String gadFileName;
    // Section 19 (GAD Checklist 2): the responses and comments the proponent entered. The
    // backend recalculates the total, its interpretation and the completion state from the
    // responses, so this field is never trusted for the score. Null means "this client does
    // not edit the checklist" (e.g. a legacy client) and leaves the stored value untouched.
    private GadChecklistData gadChecklist;
    private List<LimitationDTO> limitationsTable;
    private String limitationsText;
    private String risks;
    // Section 21 (Risk Management Plan): the structured table the Create Proposal and
    // Revision forms now use - one entry per identified risk with its likelihood, impact
    // and mitigation measure. A null value means "this client does not edit the section"
    // (a legacy client, or the RPS forward flows) and leaves the stored rows untouched;
    // only the entries actually sent are replaced. The legacy `risks` text above is kept
    // (and derived from these entries when they exist) so every screen and export that
    // predates the table keeps working.
    private List<RiskManagementDTO> riskManagement;
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

    /**
     * One row of the structured Risk Management Plan (section 21). {@code likelihood} and
     * {@code impact} carry one of High / Medium / Low; blank text fields and out-of-range
     * levels are rejected by {@code ProposalService#validateRiskManagement}, never stored.
     */
    @Data
    public static class RiskManagementDTO {
        private String identifiedRisks;
        private String likelihood;
        private String impact;
        private String mitigationMeasure;
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
