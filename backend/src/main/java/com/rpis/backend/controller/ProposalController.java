package com.rpis.backend.controller;

import com.rpis.backend.dto.DuplicateProposalRequest;
import com.rpis.backend.dto.FieldRemarkRequest;
import com.rpis.backend.dto.ForwardRevisionRequest;
import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.dto.ProposalSectionRemarksResponse;
import com.rpis.backend.dto.ProposalVersionResponse;
import com.rpis.backend.dto.ReturnRevisionRequest;
import com.rpis.backend.model.CooperatingAgency;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalReview;
import com.rpis.backend.service.CooperatingAgencyService;
import com.rpis.backend.service.ProposalReviewService;
import com.rpis.backend.service.ProposalSectionRemarkService;
import com.rpis.backend.service.ProposalService;
import com.rpis.backend.service.ProposalVersionService;
import com.rpis.backend.storage.UploadStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;
    private final ProposalReviewService reviewService;
    private final ProposalVersionService proposalVersionService;
    private final ProposalSectionRemarkService sectionRemarkService;
    private final CooperatingAgencyService cooperatingAgencyService;
    private final UploadStorage uploadStorage;

    @GetMapping
    public ResponseEntity<List<Proposal>> getProposals(
            @RequestParam(required = false) Long proponentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String[] statusIn) {
        // Ownership is resolved from the authenticated principal inside the service;
        // a Proponent can never use proponentId/status params to read another user's
        // proposals.
        List<String> statuses = (statusIn != null && statusIn.length > 0) ? List.of(statusIn) : null;
        return ResponseEntity.ok(proposalService.getProposalsForCurrentUser(proponentId, status, statuses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Proposal> getProposalById(@PathVariable Long id) {
        return ResponseEntity.ok(proposalService.getProposalByIdForCurrentUser(id));
    }

    /**
     * The cooperating agencies (section 02) selected on a proposal, in directory
     * order. The Create Proposal and Revision forms read this to restore the
     * checklist when a saved proposal is edited. Ownership is enforced by the
     * service, so a Proponent can only read the selection of their own proposal.
     */
    @GetMapping("/{id}/cooperating-agencies")
    public ResponseEntity<List<CooperatingAgency>> getCooperatingAgencies(@PathVariable Long id) {
        proposalService.getProposalByIdForCurrentUser(id);
        return ResponseEntity.ok(cooperatingAgencyService.getSelection(id));
    }

    // ========== SUBMISSION & REVISION HISTORY ==========

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<ProposalVersionResponse>> getProposalVersions(@PathVariable Long id) {
        // Ownership check before exposing revision history.
        proposalService.getProposalByIdForCurrentUser(id);
        return ResponseEntity.ok(proposalVersionService.getVersions(id));
    }

    @GetMapping("/{id}/versions/{versionNumber}")
    public ResponseEntity<ProposalVersionResponse> getProposalVersion(
            @PathVariable Long id,
            @PathVariable Integer versionNumber) {
        proposalService.getProposalByIdForCurrentUser(id);
        return ResponseEntity.ok(proposalVersionService.getVersion(id, versionNumber));
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

    /**
     * Technology Map image (section 13 of the Create Proposal form) upload.
     * <p>
     * The image is stored by the service and recorded on the proposal, so the
     * proposal returned here is the one the form keeps working with. The same
     * checks the form performs in the browser (PNG/JPEG only, at most 10 MB) are
     * repeated server-side and reported as a 400 with the same message.
     */
    @PostMapping(value = "/{id}/technology-map", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Proposal> uploadTechnologyMap(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        Proposal saved = proposalService.attachTechnologyMap(id, file);
        return ResponseEntity.ok(saved);
    }

    /**
     * Line-Item Budget file (section 25 of the Create Proposal form) upload.
     * <p>
     * The filled-out budget is stored by the service and recorded on the proposal, so
     * the proposal returned here is the one the form keeps working with. The same
     * checks the form performs in the browser (PDF or DOCX only, at most 10 MB) are
     * repeated server-side and reported as a 400 with the same message.
     */
    @PostMapping(value = "/{id}/budget-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Proposal> uploadBudgetFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        Proposal saved = proposalService.attachBudgetFile(id, file);
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

    /** The folder Special Order documents are stored in, inside the storage root. */
    private static final String SPECIAL_ORDERS_FOLDER = "special-orders";

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
                // The storage layer generates the name and returns the path relative to
                // its configured root, so the file is found again from any instance.
                String stored = uploadStorage.store(soFile.getBytes(), SPECIAL_ORDERS_FOLDER,
                        soFile.getOriginalFilename());

                proposal.setSoFileName(soFile.getOriginalFilename());
                proposal.setSoFilePath(stored);
            }

            // Set SO number and metadata
            proposal.setSoNumber(soNumber);
            proposal.setSoUploadedAt(LocalDateTime.now());
            proposal.setNeedsBudget(needsBudget);

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
            @RequestBody(required = false) ReturnRevisionRequest request) {
        String remarks = request != null ? request.getRemarks() : null;
        String office = request != null ? request.getReturnedByOffice() : null;
        Long userId = request != null ? request.getReturnedById() : null;
        String userName = request != null ? request.getReturnedByName() : null;
        Proposal updated = proposalService.returnForRevision(id, remarks, office, userId, userName,
                request != null ? request.getSectionRemarks() : null);
        return ResponseEntity.ok(updated);
    }

    /**
     * Section- and field-level revision remarks of a proposal, grouped by revision
     * round. Ownership is enforced by the service, so a Proponent only ever sees the
     * remarks of their own proposal while authorized reviewers keep their existing
     * access.
     */
    @GetMapping({ "/{id}/field-remarks", "/{id}/section-remarks" })
    public ResponseEntity<ProposalSectionRemarksResponse> getSectionRemarks(@PathVariable Long id) {
        proposalService.getProposalByIdForCurrentUser(id);
        return ResponseEntity.ok(sectionRemarkService.getRemarksForProposal(id));
    }

    /**
     * Creates or updates the remark a reviewer leaves beside one specific proposal
     * field (or repeated entry). Persisted immediately so the remark survives
     * navigation and refresh, and so it is available when the proposal is returned
     * for revision. Only authorized reviewer roles may call this.
     */
    @PutMapping("/{id}/field-remarks")
    public ResponseEntity<ProposalSectionRemarksResponse.Item> saveFieldRemark(
            @PathVariable Long id,
            @RequestBody FieldRemarkRequest request) {
        Proposal proposal = proposalService.getProposalByIdForCurrentUser(id);
        return ResponseEntity.ok(sectionRemarkService.upsertFieldRemark(proposal, request));
    }

    /** Removes a single field remark. Only authorized reviewer roles may call this. */
    @DeleteMapping("/{id}/field-remarks/{remarkId}")
    public ResponseEntity<Void> deleteFieldRemark(
            @PathVariable Long id,
            @PathVariable Long remarkId) {
        proposalService.getProposalByIdForCurrentUser(id);
        sectionRemarkService.deleteRemark(id, remarkId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates a new DRAFT proposal by reusing the reusable content of an existing
     * proposal, under the Call for Proposal the proponent selected in the picker. The
     * original's status does not matter: the copy starts the normal workflow from
     * scratch and inherits no approval, review or tracking record. The call is
     * re-validated (it must exist and still be open), so no copy is created without a
     * valid selection. Authorization and identifier allocation are enforced
     * server-side.
     */
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<Proposal> duplicateProposal(
            @PathVariable Long id,
            @RequestBody(required = false) DuplicateProposalRequest request) {
        Proposal duplicate = proposalService.duplicateProposal(
                id, request != null ? request.getProposalAnnouncementId() : null);
        return new ResponseEntity<>(duplicate, HttpStatus.CREATED);
    }

    /**
     * RPS-only: set the revision deadline and forward the revision request to the
     * Proponent. Enforced at the HTTP layer via SecurityConfig as well.
     */
    @PutMapping("/{id}/forward-revision")
    public ResponseEntity<Proposal> forwardRevisionToProponent(
            @PathVariable Long id,
            @RequestBody(required = false) ForwardRevisionRequest request) {
        Proposal updated = proposalService.forwardRevisionToProponent(
                id,
                request != null ? request.getDeadline() : null,
                request != null ? request.getNotes() : null,
                request != null ? request.getForwardedById() : null,
                request != null ? request.getForwardedByName() : null,
                request != null ? request.getRemarks() : null,
                request != null ? request.getSectionRemarks() : null);
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
        // Ownership check before exposing reviews (Proponents see only their own).
        proposalService.getProposalByIdForCurrentUser(id);
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

    // Maps Proposal Announcement gatekeeper rejections (closed submissions window) to
    // a readable 403 response instead of a generic 500 error.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleSubmissionsClosed(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    // Invalid request data (e.g. a remark submitted for an unknown proposal section)
    // fails cleanly with a readable 400 instead of a generic 500 error. The same
    // handler reports an invalid or oversized Technology Map image (see
    // ProposalService.attachTechnologyMap).
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage() != null ? ex.getMessage() : "Invalid request."));
    }

    // Safety net for the proposal/revision identifier unique constraints. The
    // sequence allocator makes collisions structurally impossible, but if one is
    // ever detected the request fails cleanly with 409 and no partial record is
    // committed (record creation happens in a single transaction).
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIdentifierConflict(
            org.springframework.dao.DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "A conflicting proposal or revision identifier was generated. Please retry."));
    }
}
