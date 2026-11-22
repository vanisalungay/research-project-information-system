package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalStatusHistory;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.ProposalStatusHistoryRepository;
import com.rpis.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Read access to a proposal's audited status transitions.
 *
 * <p>Rows are returned newest first ({@code changed_at DESC, id DESC}) so the same order
 * is used by every client. Access is resolved exactly like the other proposal read
 * endpoints: the proposal is loaded first and {@link CurrentUser#requireAccessTo} is
 * applied, so a Proponent can only read their own proposal's history.
 */
@Service
@RequiredArgsConstructor
public class ProposalStatusHistoryService {

    private final ProposalStatusHistoryRepository statusHistoryRepository;
    private final ProposalRepository proposalRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<ProposalStatusHistory> getHistoryForCurrentUser(Long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found."));
        currentUser.requireAccessTo(proposal);
        return statusHistoryRepository.findByProposalIdOrderByChangedAtDescIdDesc(proposalId);
    }
}
