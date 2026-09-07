package com.rpis.backend.config;

import com.rpis.backend.service.ProposalVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * On application startup, backfills a "Version 1 – Original Submission" snapshot
 * for proposals that were submitted before the version-history feature existed.
 * Runs once after the application context is ready and is idempotent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProposalVersionBackfillRunner {

    private final ProposalVersionService proposalVersionService;

    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        try {
            proposalVersionService.backfillExistingSubmissions();
            proposalVersionService.reconcileVersionGaps();
        } catch (Exception e) {
            log.warn("Failed to reconcile proposal version history: {}", e.getMessage());
        }
    }
}
