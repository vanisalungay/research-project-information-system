package com.rpis.backend.dto;

import lombok.Data;

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
}
