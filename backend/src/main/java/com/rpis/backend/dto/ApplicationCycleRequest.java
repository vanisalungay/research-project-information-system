package com.rpis.backend.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * Request payload for creating/updating an Application Cycle.
 */
@Data
public class ApplicationCycleRequest {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    /** DRAFT | ACTIVE | CLOSED. Defaults to DRAFT when omitted. */
    private String status;
}
