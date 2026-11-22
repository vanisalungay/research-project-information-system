package com.rpis.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The filter values that actually occur in the caller's OVCAF-visible proposals.
 * They are derived from the records, never from a hardcoded list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OvcafProposalFilterOptions {

    private List<String> statuses;
    private List<String> colleges;
    private List<String> programs;
}
