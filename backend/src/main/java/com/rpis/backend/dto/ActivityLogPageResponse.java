package com.rpis.backend.dto;

import com.rpis.backend.model.ActivityLog;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * One page of activity-log entries plus the counters the table and its pager need.
 *
 * <p>A typed page rather than Spring Data's {@code Page} so the JSON the browser
 * receives is a stable contract instead of the serialization of an internal type.
 */
@Data
public class ActivityLogPageResponse {

    private List<ActivityLog> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static ActivityLogPageResponse of(Page<ActivityLog> page) {
        ActivityLogPageResponse response = new ActivityLogPageResponse();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }
}
