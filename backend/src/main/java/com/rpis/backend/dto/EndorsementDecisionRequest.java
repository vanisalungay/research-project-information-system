package com.rpis.backend.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * Payload of a Dean's decision on an endorsement request.
 *
 * <p>The approving identity is deliberately absent: it is resolved from the
 * authenticated Dean's token by the service, so the client can neither name another
 * person as the endorser nor sign on their behalf. Only the facts the Dean confirms
 * on screen are accepted - the endorsement details for an approval, or the reason
 * for a rejection.
 */
@Data
public class EndorsementDecisionRequest {

    /** Full name of the endorser as it must appear on the letter. */
    private String endorserName;

    /** Designation / title of the endorser, e.g. "Dean" or "Department Chair". */
    private String endorserDesignation;

    /** Date of the endorsement printed on the letter. */
    private LocalDate endorsementDate;

    /** Reason for a rejection. Required when rejecting. */
    private String rejectionReason;
}
