package com.rpis.backend.dto;

import lombok.Data;

import java.util.List;

/** One page of OVCAF-visible proposals plus the values its filters can offer. */
@Data
public class OvcafProposalPageResponse {

    private List<OvcafProposalResponse> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
    private OvcafProposalFilterOptions filterOptions;
}
