package com.rpis.backend.dto;

import lombok.Data;

import java.util.List;

/** One page of submitted reports visible to OVCAF. */
@Data
public class OvcafQuarterlyReportPageResponse {

    private List<OvcafQuarterlyReportRow> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
    private OvcafQuarterlyReportFilterOptions filterOptions;
}
