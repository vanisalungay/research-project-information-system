package com.rpis.backend.controller;

import com.rpis.backend.dto.ActivityLogPageResponse;
import com.rpis.backend.model.ActivityLog;
import com.rpis.backend.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * The signed-in RPS administrator's own activity history.
 *
 * <p>Read-only by design. There is no POST, PUT or DELETE route, so an activity
 * record cannot be created, altered or removed through the API - entries are
 * written only by the business services as their operations succeed.
 *
 * <p>Every response is scoped to the authenticated principal inside
 * {@link ActivityLogService}: the controller accepts no user id at all, so there is
 * nothing a caller could change to reach another administrator's history.
 * {@code SecurityConfig} additionally restricts this whole path to
 * {@code RPS_ADMIN}.
 */
@RestController
@RequestMapping("/api/activity-logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    /** One page of the caller's history, newest first, with the table's filters applied. */
    @GetMapping
    public ResponseEntity<ActivityLogPageResponse> getActivityLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(
                activityLogService.page(search, module, status, from, to, page, size));
    }

    /** The modules the caller has entries for, so the filter lists real values. */
    @GetMapping("/modules")
    public ResponseEntity<List<String>> getModules() {
        return ResponseEntity.ok(activityLogService.modules());
    }

    /** One entry, for the View Details panel. Somebody else's entry answers 404. */
    @GetMapping("/{id}")
    public ResponseEntity<ActivityLog> getActivity(@PathVariable Long id) {
        return ResponseEntity.ok(activityLogService.find(id));
    }
}
