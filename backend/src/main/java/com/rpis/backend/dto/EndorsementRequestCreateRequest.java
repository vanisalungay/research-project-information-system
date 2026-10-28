package com.rpis.backend.dto;

import lombok.Data;

/**
 * Payload of a Proponent's Request for Endorsement. It carries only the
 * preliminary information needed to endorse a project - the full research proposal
 * is not filled in at this stage.
 *
 * <p>Program Title and Project Title are two independent values, exactly as on the
 * Create Proposal form: {@code programTitle} is optional (the programme the project
 * belongs to) and {@code projectTitle} is required (the title of the project itself).
 * Neither is ever derived from the other.
 *
 * <p>The Proponent and the requesting identity are never part of this payload: the
 * service derives them from the authenticated account, so a caller can neither
 * submit on behalf of someone else nor choose who reviews the request.
 */
@Data
public class EndorsementRequestCreateRequest {

    /**
     * The Program Title the Proponent selected or created. Optional - the field may be
     * left empty, exactly like the Create Proposal form.
     */
    private String programTitle;

    /** Title of the specific project being endorsed. Required. */
    private String projectTitle;

    /**
     * Deprecated alias of {@link #projectTitle}, kept so a client built against the
     * previous API version - which sent one combined title - keeps working. It is only
     * read when {@link #projectTitle} is absent, and it never carries a program title.
     */
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
