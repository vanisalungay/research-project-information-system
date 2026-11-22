package com.rpis.backend.controller;

import com.rpis.backend.model.ProposalStatusHistory;
import com.rpis.backend.service.ProposalStatusHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class ProposalStatusHistoryController {

    private final ProposalStatusHistoryService statusHistoryService;

    /**
     * The proposal's audited status transitions, newest first. Ownership/role access is
     * enforced by the service before any row is read, so this route is safe for every
     * role that can already open the proposal.
     */
    @GetMapping("/{id}/status-history")
    public ResponseEntity<List<ProposalStatusHistory>> getStatusHistory(@PathVariable Long id) {
        return ResponseEntity.ok(statusHistoryService.getHistoryForCurrentUser(id));
    }
}
