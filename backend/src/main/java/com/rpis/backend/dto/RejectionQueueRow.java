package com.rpis.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * One row of the ORPS Rejected Proposals queue. The two workflow timestamps are the
 * actual audited events, not the proposal's last-modified time:
 * <ul>
 *   <li>{@code routedAt} - when the rejecting office returned the proposal to ORPS;</li>
 *   <li>{@code processedAt} - when ORPS forwarded the finalized rejection.</li>
 * </ul>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RejectionQueueRow {

    private Long id;
    private String documentId;
    private String projectTitle;
    private String projectLeader;
    private String college;
    private String programTitle;
    private String status;
    private String returnKind;
    private String returnedByOffice;
    private String returnRemarks;
    private String remarks;

    private LocalDateTime submittedAt;
    private LocalDateTime returnedAt;
    private LocalDateTime routedAt;
    private LocalDateTime processedAt;
}
