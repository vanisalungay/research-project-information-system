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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @PutMapping("/{id}/forward-to-ovcaf")
    public ResponseEntity<Proposal> forwardToOvcaf(@PathVariable Long id) {
        Proposal updated = proposalService.updateProposalStatus(id, "FOR_OVCAF_APPROVAL");
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

    private static final String SO_UPLOAD_DIR = "uploads/special-orders/";

    /**
     * Approve proposal with Special Order (SO) document upload.
     * OC grants final approval, issues SO, and determines budget routing.
     */
    @PutMapping("/{id}/approve-with-so")
    public ResponseEntity<?> approveWithSpecialOrder(
            @PathVariable Long id,
            @RequestParam("soNumber") String soNumber,
            @RequestParam(value = "soFile", required = false) MultipartFile soFile,
            @RequestParam("needsBudget") Boolean needsBudget,
            @RequestParam(value = "chancellorNotes", required = false) String chancellorNotes) {

        try {
            Proposal proposal = proposalService.getProposalById(id);

            // Save SO file if provided
            if (soFile != null && !soFile.isEmpty()) {
                Path uploadPath = Paths.get(SO_UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String originalFilename = soFile.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf("."))
                        : "";
                String uniqueFilename = UUID.randomUUID().toString() + "_SO" + extension;

                Path filePath = Paths.get(SO_UPLOAD_DIR, uniqueFilename);
                Files.write(filePath, soFile.getBytes());

                proposal.setSoFileName(originalFilename);
                proposal.setSoFilePath(filePath.toString());
            }

            // Set SO number and metadata
            proposal.setSoNumber(soNumber);
            proposal.setSoUploadedAt(LocalDateTime.now());

            // Save chancellor notes if provided
            if (chancellorNotes != null && !chancellorNotes.isBlank()) {
                proposal.setRemarks(chancellorNotes);
            }

            // Route based on budget requirement
            if (needsBudget) {
                proposal.setStatus("FOR_OVCAF_APPROVAL");
            } else {
                proposal.setStatus("APPROVED");
            }

            Proposal saved = proposalService.saveProposalDirect(proposal);

            Map<String, Object> response = new HashMap<>();
            response.put("proposal", saved);
            response.put("message", needsBudget
                    ? "Proposal approved with SO. Forwarded to Finance Office / OVCAF for budget endorsement."
                    : "Proposal approved with SO for immediate implementation.");
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload SO file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Approval failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/return-revision")
    public ResponseEntity<Proposal> returnForRevision(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        Proposal updated = proposalService.returnForRevision(id, remarks);
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

    @PutMapping("/{id}/reviewer-info")
    public ResponseEntity<Proposal> updateReviewerInfo(
            @PathVariable Long id,
            @RequestParam String reviewedBy,
            @RequestParam String reviewedByPosition) {
        Proposal updated = proposalService.updateReviewerInfo(id, reviewedBy, reviewedByPosition);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProposal(@PathVariable Long id) {
        proposalService.deleteProposal(id);
        return ResponseEntity.noContent().build();
    }

    // Maps Application Cycle gatekeeper rejections (closed submissions window) to
    // a readable 403 response instead of a generic 500 error.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleSubmissionsClosed(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
}
