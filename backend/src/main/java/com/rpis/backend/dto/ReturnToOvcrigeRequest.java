package com.rpis.backend.dto;

import lombok.Data;

/**
 * Payload for the Chancellor's Office returning a proposal to OVCRIGE. The remarks
 * are optional; when present they are recorded on the proposal and in its audited
 * status history.
 */
@Data
public class ReturnToOvcrigeRequest {

    private String remarks;
}
