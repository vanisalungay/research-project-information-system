package com.rpis.backend.config;

import com.rpis.backend.service.ProposalVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * On application startup, backfills a "Version 1 – Original Submission" snapshot
 * for proposals that were submitted before the version-history feature existed.
 *
 * <p>Both steps are idempotent, but they read the whole proposal table and write
 * wherever they find a gap. With several instances starting together behind a load
 * balancer that is duplicated work on the shared database, so the runner is off by
 * default and enabled deliberately (for example for a single maintenance instance)
 * through {@code rpis.bootstrap.proposal-version-backfill.enabled}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProposalVersionBackfillRunner {

    private final ProposalVersionService proposalVersionService;

    @Value("${rpis.bootstrap.proposal-version-backfill.enabled:false}")
    private boolean enabled;

    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        if (!enabled) {
            log.info("Proposal version backfill is disabled (rpis.bootstrap.proposal-version-backfill.enabled=false).");
            return;
        }
        try {
            proposalVersionService.backfillExistingSubmissions();
            proposalVersionService.reconcileVersionGaps();
        } catch (Exception e) {
            log.warn("Failed to reconcile proposal version history: {}", e.getMessage());
        }
    }
}

