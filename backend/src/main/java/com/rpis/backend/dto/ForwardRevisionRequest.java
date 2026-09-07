package com.rpis.backend.dto;

import lombok.Data;

/**
 * Payload for RPS forwarding a revision request to the Proponent. Only RPS may
 * set the revision deadline and forward the request.
 */
@Data
public class ForwardRevisionRequest {

    private Long forwardedById;

    private String forwardedByName;

    /** Revision deadline for the Proponent's revised submission (set by RPS). Accepts ISO or yyyy-MM-dd. */
    private String deadline;

    /** Optional RPS note appended to the revision request. */
    private String notes;

    /**
     * Only used when RPS itself is returning during its own review (no prior
     * office return). For office returns the original remarks are already stored
     * in {@code return_remarks} and are preserved unchanged.
     */
    private String remarks;
}
