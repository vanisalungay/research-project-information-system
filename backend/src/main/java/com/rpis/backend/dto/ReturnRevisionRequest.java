package com.rpis.backend.dto;

import lombok.Data;

import java.util.List;

/**
 * Payload for an authorized reviewing office returning a proposal for revision.
 * The return is always routed to RPS (never directly to the Proponent).
 */
@Data
public class ReturnRevisionRequest {

    private Long returnedById;

    private String returnedByName;

    /** Office/role that returned the proposal, e.g. REC, OVCRIGE. */
    private String returnedByOffice;

    /** The reviewing office's revision remarks (preserved verbatim). */
    private String remarks;

    /**
     * Optional section-specific remarks. Each entry targets one proposal section so
     * the Proponent can see exactly which part of the proposal must be corrected.
     * Blank entries are ignored; unknown section keys are rejected.
     */
    private List<SectionRemarkDTO> sectionRemarks;
}
