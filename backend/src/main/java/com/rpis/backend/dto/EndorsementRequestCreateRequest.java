package com.rpis.backend.dto;

import lombok.Data;

/**
 * Payload of a Proponent's Request for Endorsement. It carries only the
 * preliminary information needed to endorse a project - the full research proposal
 * is not filled in at this stage.
 *
 * <p>The Proponent and the requesting identity are never part of this payload: the
 * service derives them from the authenticated account, so a caller can neither
 * submit on behalf of someone else nor choose who reviews the request.
 */
@Data
public class EndorsementRequestCreateRequest {

    /** Title of the proposal/program being endorsed. Required. */
    private String proposalTitle;

    /** The Call for Proposal (announcement) being applied to. Required. */
    private Long proposalAnnouncementId;

    /** Implementing college/department. Required by the organizational model. */
    private String college;

    /** Optional finer-grained department/unit. */
    private String department;

    /** Optional brief research description or summary. */
    private String researchSummary;

    /** Optional additional notes for the Dean. */
    private String notes;
}
