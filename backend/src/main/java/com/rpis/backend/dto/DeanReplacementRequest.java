package com.rpis.backend.dto;

import lombok.Data;

/**
 * Administrative payload for replacing the sitting Dean of one college with a new
 * appointment.
 *
 * <p>Used by the Manage Dean Role screen. The previous Dean is never overwritten: the
 * backend closes their appointment as {@code REVOKED} (keeping the college, the
 * designation, the dates and the acting administrator) and opens a new {@code ACTIVE}
 * appointment for the incoming Dean in one transaction, so the college can never be left
 * with two active Deans or with none.
 *
 * <p>Only an RPS_ADMIN may submit it (enforced by SecurityConfig and re-checked in the
 * service).
 */
@Data
public class DeanReplacementRequest {

    /**
     * The ACTIVE appointment being replaced. This is the authority for the college and
     * designation: both are copied from that stored appointment, so the replacement
     * screen cannot change either of them.
     */
    private Long assignmentId;

    /** The existing account that becomes the new Dean. Required. */
    private Long userId;
}
