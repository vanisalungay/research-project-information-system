package com.rpis.backend.dto;

import lombok.Data;

/**
 * Explicit request to link an approved endorsement to the proposal the Proponent
 * continued to. The proposal id is validated by the service: the caller must own
 * the proposal, the endorsement must be approved, and neither may already be
 * linked to something else.
 */
@Data
public class EndorsementLinkRequest {
    private Long proposalId;
}
