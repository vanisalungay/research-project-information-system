package com.rpis.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Filter values that actually occur in OVCAF's authorized submitted reports. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OvcafQuarterlyReportFilterOptions {

    private List<String> periods;
    private List<String> colleges;
    private List<String> programs;
    private List<ProjectOption> projects;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectOption {
        private Long id;
        private String documentId;
        private String projectTitle;
    }
}
