package com.rpis.backend.dto;

import lombok.Data;

/**
 * Payload for OVCRIGE rejecting a proposal and returning it to ORPS (RPS) for
 * processing. The remarks are optional; when present they are recorded on the
 * proposal, in its audited status history and in the ORPS notification.
 *
 * <p>OVCRIGE never notifies the Proponent directly: the proposal is closed as
 * rejected only when ORPS forwards the rejection.</p>
 */
@Data
public class RejectToRpsRequest {

    private String remarks;
}
