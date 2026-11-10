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
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    /**
     * Endorsement document (Certification &amp; Endorsement step, "Endorsed By") upload.
     * <p>
     * The Dean's signed endorsement is stored by the service and recorded on the
     * proposal, so the proposal returned here is the one the form keeps working with and
     * the document stays associated with that exact proposal. The same checks the form
     * performs in the browser (PDF only, at most 10 MB) are repeated server-side, plus
     * opening the document, so a renamed image or a corrupted PDF is answered with the
     * same 400 the form shows.
     */
    @PostMapping(value = "/{id}/endorsement-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Proposal> uploadEndorsementDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        Proposal saved = proposalService.attachEndorsementDocument(id, file);
        return ResponseEntity.ok(saved);
    }

    /**
     * "Submitted By" signature upload of the Certification &amp; Endorsement step.
     * <p>
     * The proponent's signature (PNG, JPEG or PDF) is stored by the service and recorded
     * on the proposal, so the proposal returned here is the one the form keeps working
     * with and the signature stays associated with that exact proposal. The same checks
     * the form performs in the browser (image/PDF, at most 10 MB) are repeated
     * server-side, plus opening the image or PDF bytes, so a renamed or corrupted file is
     * answered with the same 400 the form shows.
     */
    @PostMapping(value = "/{id}/submitted-by-signature", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Proposal> uploadSubmittedBySignature(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        Proposal saved = proposalService.attachSubmittedBySignature(id, file);
        return ResponseEntity.ok(saved);
    }

    /**
     * The only statuses a reviewer may set through this generic route. Every other
     * transition is owned by its workflow endpoint (OC approval + SO, OVCAF budget
     * validation, revisions), so the generic route cannot be used to bypass them -
     * in particular it can no longer move a proposal straight to APPROVED or
     * FOR_OVCAF_APPROVAL without the OC approval/SO record.
     */
    private static final Set<String> SELF_SERVICE_STATUSES = Set.of("REC_APPROVED", "READY_FOR_RELEASE");

    @PutMapping("/{id}/status")
    public ResponseEntity<Proposal> updateProposalStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        String normalised = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!SELF_SERVICE_STATUSES.contains(normalised)) {
            throw new IllegalArgumentException(
                    "This status can only be set through its dedicated workflow action.");
        }
        Proposal updated = proposalService.updateProposalStatus(id, normalised);
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

    /**
     * Stage 1 of the OC -&gt; OVCAF workflow: the Chancellor's Office records the
     * issued Special Order (SO) and routes the proposal to budget evaluation (or to
     * implementation when no budget is required).
     *
     * <p>The whole decision - validation, SO persistence, status transition and
     * notification - happens in one service transaction. A failure leaves the
     * proposal at its previous stage and records nothing; the response says why so
     * the form can show the real reason instead of a generic failure.
     */
    @PutMapping("/{id}/approve-with-so")
    public ResponseEntity<?> approveWithSpecialOrder(
            @PathVariable Long id,
            @RequestParam("soNumber") String soNumber,
            @RequestParam(value = "soFile", required = false) MultipartFile soFile,
            @RequestParam("needsBudget") Boolean needsBudget,
            @RequestParam(value = "chancellorNotes", required = false) String chancellorNotes) {
        try {
            Proposal saved = proposalService.approveWithSpecialOrder(
                    id, soNumber, soFile, needsBudget, chancellorNotes);

            Map<String, Object> response = new HashMap<>();
            response.put("proposal", saved);
            response.put("message", Boolean.TRUE.equals(saved.getNeedsBudget())
                    ? "Proposal approved with SO. Forwarded to Finance Office / OVCAF for budget endorsement."
                    : "Proposal approved with SO for immediate implementation.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Invalid approval request."));
        } catch (IllegalStateException e) {
            // The proposal is no longer awaiting the Chancellor's final approval (already
            // approved, or advanced by a concurrent request). Nothing was changed.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage()
                            : "This proposal is not awaiting final approval."));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getReason() != null ? e.getReason() : "Approval failed."));
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
