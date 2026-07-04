package com.rpis.backend.controller;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalReview;
import com.rpis.backend.service.ProposalReviewService;
import com.rpis.backend.service.ProposalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;
    private final ProposalReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<Proposal>> getProposals(
            @RequestParam(required = false) Long proponentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String[] statusIn) {
        if (proponentId != null) {
            return ResponseEntity.ok(proposalService.getProposalsByProponent(proponentId));
        } else if (status != null) {
            return ResponseEntity.ok(proposalService.getProposalsByStatus(status));
        } else if (statusIn != null && statusIn.length > 0) {
            return ResponseEntity.ok(proposalService.getProposalsByStatusIn(List.of(statusIn)));
        }
        return ResponseEntity.ok(proposalService.getAllProposals());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Proposal> getProposalById(@PathVariable Long id) {
        return ResponseEntity.ok(proposalService.getProposalById(id));
    }

    @PostMapping
    public ResponseEntity<Proposal> createProposal(@RequestBody ProposalRequest request) {
        Proposal saved = proposalService.saveProposal(request, null);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Proposal> updateProposal(
            @PathVariable Long id,
            @RequestBody ProposalRequest request) {
        Proposal saved = proposalService.saveProposal(request, id);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Proposal> updateProposalStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        Proposal updated = proposalService.updateProposalStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    // ========== FLOW ACTIONS ==========

    @PutMapping("/{id}/endorse")
    public ResponseEntity<Proposal> endorseProposal(@PathVariable Long id) {
        Proposal updated = proposalService.updateProposalStatus(id, "ENDORSED");
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/forward-to-rec")
    public ResponseEntity<Proposal> forwardToRec(@PathVariable Long id) {
        Proposal updated = proposalService.updateProposalStatus(id, "UNDER_REVIEW");
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/forward-to-oc")
    public ResponseEntity<Proposal> forwardToOc(@PathVariable Long id) {
        Proposal updated = proposalService.updateProposalStatus(id, "FOR_OC_APPROVAL");
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/final-approve")
    public ResponseEntity<Proposal> finalApprove(@PathVariable Long id) {
        Proposal updated = proposalService.updateProposalStatus(id, "APPROVED");
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/return-revision")
    public ResponseEntity<Proposal> returnForRevision(@PathVariable Long id) {
        Proposal updated = proposalService.updateProposalStatus(id, "REVISION");
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Proposal> rejectProposal(@PathVariable Long id) {
        Proposal updated = proposalService.updateProposalStatus(id, "REJECTED");
        return ResponseEntity.ok(updated);
    }

    // ========== REVIEWS ==========

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ProposalReview> submitReview(
            @PathVariable Long id,
            @RequestParam Long reviewerId,
            @RequestBody ProposalReview review) {
        ProposalReview submitted = reviewService.submitReview(id, reviewerId, review);
        return new ResponseEntity<>(submitted, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ProposalReview>> getProposalReviews(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getReviewsForProposal(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProposal(@PathVariable Long id) {
        proposalService.deleteProposal(id);
        return ResponseEntity.noContent().build();
    }
}
