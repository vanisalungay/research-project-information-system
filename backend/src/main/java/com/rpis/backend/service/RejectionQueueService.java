package com.rpis.backend.service;

import com.rpis.backend.dto.RejectionQueueRow;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalStatusHistory;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.ProposalStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Read model for the ORPS Rejected Proposals tab.
 *
 * <p>The tab contains both shapes of a rejection:
 * <ul>
 *   <li>a reviewing office handed the proposal to ORPS ({@code RETURNED_TO_RPS} with
 *       {@code return_kind = REJECTION});</li>
 *   <li>ORPS has already finalized it ({@code REJECTED}).</li>
 * </ul>
 * Ordinary revision returns also use {@code RETURNED_TO_RPS}, so the return kind - not
 * the status alone - decides whether a row belongs here.
 *
 * <p>The Date Processed value comes from the audited status history: the transition into
 * {@code RETURNED_TO_RPS} for a pending hand-off, and the transition into
 * {@code REJECTED} for a finalized one. No unrelated timestamp is substituted.
 */
@Service
@RequiredArgsConstructor
public class RejectionQueueService {

    private static final String STATUS_RETURNED_TO_RPS = "RETURNED_TO_RPS";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String RETURN_KIND_REJECTION = "REJECTION";
    private static final String BASELINE_SOURCE = "BASELINE_SEED";

    private final ProposalRepository proposalRepository;
    private final ProposalStatusHistoryRepository statusHistoryRepository;

    @Transactional(readOnly = true)
    public List<RejectionQueueRow> list() {
        List<Proposal> proposals = proposalRepository
                .findByStatusIn(List.of(STATUS_RETURNED_TO_RPS, STATUS_REJECTED)).stream()
                .filter(RejectionQueueService::isRejectionRow)
                .toList();
        if (proposals.isEmpty()) {
            return List.of();
        }

        Map<Long, List<ProposalStatusHistory>> historyByProposal = new HashMap<>();
        List<Long> ids = proposals.stream().map(Proposal::getId).filter(Objects::nonNull).toList();
        for (ProposalStatusHistory row : statusHistoryRepository
                .findByProposalIdInOrderByChangedAtAscIdAsc(ids)) {
            if (row.getProposal() == null || row.getProposal().getId() == null) {
                continue;
            }
            if (BASELINE_SOURCE.equalsIgnoreCase(String.valueOf(row.getSource()))) {
                continue;
            }
            historyByProposal
                    .computeIfAbsent(row.getProposal().getId(), key -> new ArrayList<>())
                    .add(row);
        }

        List<RejectionQueueRow> rows = new ArrayList<>();
        for (Proposal proposal : proposals) {
            rows.add(toRow(proposal, historyByProposal.getOrDefault(proposal.getId(), List.of())));
        }
        rows.sort(Comparator
                .comparing(RejectionQueueService::effectiveDate,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RejectionQueueRow::getId,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        return rows;
    }

    private static boolean isRejectionRow(Proposal proposal) {
        String status = normalise(proposal.getStatus());
        if (STATUS_REJECTED.equals(status)) {
            return true;
        }
        return STATUS_RETURNED_TO_RPS.equals(status)
                && RETURN_KIND_REJECTION.equals(normalise(proposal.getReturnKind()));
    }

    private static RejectionQueueRow toRow(Proposal proposal, List<ProposalStatusHistory> history) {
        RejectionQueueRow row = new RejectionQueueRow();
        row.setId(proposal.getId());
        row.setDocumentId(proposal.getDocumentId() != null
                ? proposal.getDocumentId() : proposal.getProposalCode());
        row.setProjectTitle(proposal.getProjectTitle());
        row.setProjectLeader(proposal.getProjectLeader() != null
                ? proposal.getProjectLeader()
                : proposal.getProponent() != null ? proposal.getProponent().getName() : null);
        row.setCollege(proposal.getCollege());
        row.setProgramTitle(proposal.getProgramTitle());
        row.setStatus(normalise(proposal.getStatus()));
        row.setReturnKind(proposal.getReturnKind());
        row.setReturnedByOffice(proposal.getReturnedByOffice());
        row.setReturnRemarks(proposal.getReturnRemarks());
        row.setRemarks(proposal.getRemarks());
        row.setSubmittedAt(proposal.getSubmittedAt());
        row.setReturnedAt(proposal.getReturnedAt());
        row.setRoutedAt(latestTransition(history, STATUS_RETURNED_TO_RPS));
        if (row.getRoutedAt() == null) {
            row.setRoutedAt(proposal.getReturnedAt());
        }
        row.setProcessedAt(latestTransition(history, STATUS_REJECTED));
        // Legacy finalized rows predate the audited history table. There is no
        // transition row to read, so the record's own last-update timestamp is the only
        // stored evidence of when the decision was finalized. It is used only for a
        // REJECTED row that has no audited transition; nothing is invented for a record
        // with no timestamp at all.
        if (row.getProcessedAt() == null && STATUS_REJECTED.equals(row.getStatus())) {
            row.setProcessedAt(proposal.getUpdatedAt());
        }
        return row;
    }

    private static LocalDateTime latestTransition(List<ProposalStatusHistory> history, String toStatus) {
        LocalDateTime latest = null;
        for (ProposalStatusHistory row : history) {
            if (!toStatus.equals(normalise(row.getToStatus())) || row.getChangedAt() == null) {
                continue;
            }
            if (latest == null || row.getChangedAt().isAfter(latest)) {
                latest = row.getChangedAt();
            }
        }
        return latest;
    }

    private static LocalDateTime effectiveDate(RejectionQueueRow row) {
        return row.getProcessedAt() != null ? row.getProcessedAt() : row.getRoutedAt();
    }

    private static String normalise(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
