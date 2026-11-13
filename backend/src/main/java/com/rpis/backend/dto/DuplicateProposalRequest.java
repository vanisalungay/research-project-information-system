package com.rpis.backend.dto;

import lombok.Data;

/**
 * Body of {@code POST /api/proposals/{id}/duplicate} ("Make a Copy").
 * <p>
 * The proponent selects the Call for Proposal the copy belongs to before the draft is
 * created, so the copy is filed under that call - the same rule every other proposal
 * in RPIS follows. The call is re-validated server-side (it must exist and still be
 * open for submissions); nothing is created without it.
 */
@Data
public class DuplicateProposalRequest {

    /** The Call for Proposal the copy belongs to (required). */
    private Long proposalAnnouncementId;
}
