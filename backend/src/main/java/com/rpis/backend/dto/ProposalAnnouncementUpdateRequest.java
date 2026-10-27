package com.rpis.backend.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * Update payload of an existing Call for Proposal.
 *
 * <p>ORPS may only move the submission window and change the status of a call that already
 * exists. The announcement name, its RIIDE priority agenda, its announcement reminders and
 * its additional announcement are part of the published announcement: they are set when the
 * call is created and stay exactly as they were saved.</p>
 *
 * <p>This dedicated DTO is the allow-list of that rule. The restricted fields are not
 * declared here, so they are never bound from the request body - a client that sends them
 * cannot change them, and the service never has to ignore or discard anything.</p>
 */
@Data
public class ProposalAnnouncementUpdateRequest {

    /** New opening date of the submission window. Omitted keeps the stored opening date. */
    private LocalDate startDate;

    /** New submission deadline. Omitted keeps the stored deadline. */
    private LocalDate endDate;

    /** New stored status (DRAFT / ACTIVE / CLOSED). Omitted means "keep the stored status". */
    private String status;
}
