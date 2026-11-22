package com.rpis.backend.dto;

import com.rpis.backend.model.Proposal;
import lombok.Data;

import java.util.List;

/** One page of proposals for the generic list screens. */
@Data
public class ProposalPageResponse {

    private List<Proposal> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
}
