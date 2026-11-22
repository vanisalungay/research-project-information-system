package com.rpis.backend.controller;

import com.rpis.backend.dto.RejectionQueueRow;
import com.rpis.backend.service.RejectionQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ORPS Rejected Proposals queue. SecurityConfig restricts this read to RPS_ADMIN and
 * RPS_STAFF; the rows carry the actual hand-off and finalization timestamps.
 */
@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class RejectionQueueController {

    private final RejectionQueueService rejectionQueueService;

    @GetMapping("/rejections")
    public ResponseEntity<List<RejectionQueueRow>> getRejections() {
        return ResponseEntity.ok(rejectionQueueService.list());
    }
}
