package com.rpis.backend.dto;

import lombok.Data;

/**
 * Request payload for adding an agency to the shared cooperating agency
 * directory ({@code POST /api/cooperating-agencies}).
 */
@Data
public class CooperatingAgencyRequest {

    /** Name of the agency to add; trimmed and validated by the service. */
    private String name;
}
