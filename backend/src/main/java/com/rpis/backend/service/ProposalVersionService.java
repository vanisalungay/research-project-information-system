package com.rpis.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpis.backend.dto.ProposalVersionResponse;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalVersion;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.ProposalVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maintains the immutable submission & revision history of every proposal.
 * Each submission appends a new version row; previous versions are never
 * deleted or overwritten.
 */
@Service
@RequiredArgsConstructor
public class ProposalVersionService {

    private final ProposalVersionRepository versionRepository;
    private final ProposalRepository proposalRepository;
    private final ObjectMapper objectMapper;

    /** Statuses that indicate the proposal was returned and is being resubmitted. */
    public boolean isRevisionStatus(String status) {
        return "RPS_RETURNED".equals(status)
                || "REC_REVISION".equals(status)
                || "REVISION".equals(status);
    }

    public boolean isSubmissionStatus(String status) {
        return "SUBMITTED".equalsIgnoreCase(status);
    }

    @Transactional(readOnly = true)
    public List<ProposalVersionResponse> getVersions(Long proposalId) {
        return versionRepository.findByProposalIdOrderByVersionNumberAsc(proposalId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProposalVersionResponse getVersion(Long proposalId, Integer versionNumber) {
        ProposalVersion version = versionRepository
                .findByProposalIdAndVersionNumber(proposalId, versionNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Version not found: proposal " + proposalId + " version " + versionNumber));

        ProposalVersionResponse response = toResponse(version);
        response.setDetails(parseDetails(version.getDetailsJson()));
        return response;
    }

    /**
     * Append a new version snapshot for a just-completed submission (original or
     * revision). The new version is marked as the current one; any previous current
     * version is demoted.
     */
    @Transactional
    public void recordSubmission(Proposal proposal, boolean isRevision, String revisionRemarks) {
        int versionNumber = (proposal.getRevisionNumber() != null ? proposal.getRevisionNumber() : 0) + 1;

        for (ProposalVersion existing : versionRepository.findByProposalIdOrderByVersionNumberAsc(proposal.getId())) {
            if (Boolean.TRUE.equals(existing.getIsCurrent())) {
                existing.setIsCurrent(false);
                versionRepository.save(existing);
            }
        }

        ProposalVersion version = new ProposalVersion();
        version.setProposal(proposal);
        version.setVersionNumber(versionNumber);
        version.setDocumentId(proposal.getDocumentId());
        version.setStatus(proposal.getStatus());
        version.setSubmittedBy(proposal.getProponent());
        version.setSubmittedByName(submitterName(proposal));
        version.setSubmissionDate(LocalDateTime.now());
        version.setRevisionDate(isRevision ? LocalDateTime.now() : null);
        version.setRevisionRemarks(revisionRemarks);
        version.setDetailsJson(buildDetailsJson(proposal));
        version.setIsCurrent(true);

        versionRepository.save(version);
    }

    /**
     * Backfill a baseline "previous submission" snapshot for proposals that were
     * already mid-lifecycle before this feature existed. Runs only when no version
     * rows exist yet, and is called before the live row is overwritten so the
     * prior content is preserved.
     */
    @Transactional
    public void ensureBaseline(Proposal proposal) {
        if (versionRepository.existsByProposalId(proposal.getId())) {
            return;
        }

        int versionNumber = (proposal.getRevisionNumber() != null ? proposal.getRevisionNumber() : 0) + 1;

        ProposalVersion version = new ProposalVersion();
        version.setProposal(proposal);
        version.setVersionNumber(versionNumber);
        version.setDocumentId(proposal.getDocumentId());
        version.setStatus("SUBMITTED");
        version.setSubmittedBy(proposal.getProponent());
        version.setSubmittedByName(submitterName(proposal));
        version.setSubmissionDate(proposal.getCreatedAt() != null ? proposal.getCreatedAt() : LocalDateTime.now());
        // If this proposal was already revised (in-place) before the history feature
        // existed, the snapshot we are preserving is itself a revision — record an
        // approximate revision date so it is labelled consistently (e.g. "Revised on").
        boolean alreadyRevised = proposal.getRevisionNumber() != null && proposal.getRevisionNumber() > 0;
        version.setRevisionDate(alreadyRevised
                ? (proposal.getUpdatedAt() != null ? proposal.getUpdatedAt() : LocalDateTime.now())
                : null);
        version.setRevisionRemarks(null);
        version.setDetailsJson(buildDetailsJson(proposal));
        version.setIsCurrent(false);

        versionRepository.save(version);
    }

    /**
     * One-time backfill for proposals that were submitted before this feature
     * existed. Creates a "Version 1 – Original Submission" snapshot for any
     * proposal that has already passed submission (status != DRAFT) but has no
     * version rows yet. Idempotent: proposals that already have history are
     * skipped, as are proposals that have already been revised in-place (their
     * original content is no longer recoverable).
     */
    @Transactional
    public void backfillExistingSubmissions() {
        for (Proposal proposal : proposalRepository.findAll()) {
            backfillProposal(proposal);
        }
    }

    private void backfillProposal(Proposal proposal) {
        if (versionRepository.existsByProposalId(proposal.getId())) {
            return;
        }

        String status = proposal.getStatus();
        if (status == null || "DRAFT".equalsIgnoreCase(status)) {
            return;
        }

        // Skip proposals that were already revised in-place before this feature.
        if (proposal.getRevisionNumber() != null && proposal.getRevisionNumber() > 0) {
            return;
        }

        ProposalVersion version = new ProposalVersion();
        version.setProposal(proposal);
        version.setVersionNumber(1);
        version.setDocumentId(proposal.getDocumentId());
        version.setStatus(status);
        version.setSubmittedBy(proposal.getProponent());
        version.setSubmittedByName(submitterName(proposal));
        version.setSubmissionDate(proposal.getCreatedAt() != null ? proposal.getCreatedAt() : LocalDateTime.now());
        version.setRevisionDate(null);
        version.setRevisionRemarks(null);
        version.setDetailsJson(buildDetailsJson(proposal));
        version.setIsCurrent(true);

        versionRepository.save(version);
    }

    /**
     * Reconciles the history of every submitted proposal so the version sequence is
     * complete and chronological (Version 1 .. Version revisionNumber+1). Any version
     * that was never recorded (e.g. a proposal first submitted before the history
     * feature existed) is filled in with a placeholder, so no original submission or
     * earlier revision ever appears to be missing. Idempotent.
     */
    @Transactional
    public void reconcileVersionGaps() {
        for (Proposal proposal : proposalRepository.findAll()) {
            reconcileProposal(proposal);
        }
    }

    private void reconcileProposal(Proposal proposal) {
        String status = proposal.getStatus();
        if (status == null || "DRAFT".equalsIgnoreCase(status)) {
            return;
        }

        int expectedMax = (proposal.getRevisionNumber() != null ? proposal.getRevisionNumber() : 0) + 1;
        if (expectedMax < 1) {
            expectedMax = 1;
        }

        List<ProposalVersion> existing = versionRepository.findByProposalIdOrderByVersionNumberAsc(proposal.getId());
        Set<Integer> existingNumbers = existing.stream()
                .map(ProposalVersion::getVersionNumber)
                .collect(Collectors.toSet());

        for (int v = 1; v <= expectedMax; v++) {
            if (existingNumbers.contains(v)) {
                continue;
            }

            if (existing.isEmpty() && v == expectedMax) {
                // Nothing was ever recorded and this is the latest version: snapshot the
                // current state so the current version is a real, complete record.
                insertSnapshot(proposal, v, true);
            } else {
                // A historical version predates the feature: insert a placeholder so the
                // sequence remains complete and chronological.
                insertPlaceholder(proposal, v, false);
            }
        }
    }

    private void insertSnapshot(Proposal proposal, int versionNumber, boolean isCurrent) {
        ProposalVersion version = new ProposalVersion();
        version.setProposal(proposal);
        version.setVersionNumber(versionNumber);
        version.setDocumentId(proposal.getDocumentId());
        version.setStatus(proposal.getStatus());
        version.setSubmittedBy(proposal.getProponent());
        version.setSubmittedByName(submitterName(proposal));
        version.setSubmissionDate(proposal.getCreatedAt() != null ? proposal.getCreatedAt() : LocalDateTime.now());
        version.setRevisionDate(versionNumber > 1 && proposal.getUpdatedAt() != null
                ? proposal.getUpdatedAt()
                : null);
        version.setRevisionRemarks(null);
        version.setDetailsJson(buildDetailsJson(proposal));
        version.setIsCurrent(isCurrent);
        versionRepository.save(version);
    }

    private void insertPlaceholder(Proposal proposal, int versionNumber, boolean isCurrent) {
        ProposalVersion version = new ProposalVersion();
        version.setProposal(proposal);
        version.setVersionNumber(versionNumber);
        version.setDocumentId(deriveDocumentId(proposal, versionNumber));
        version.setStatus(proposal.getStatus());
        version.setSubmittedBy(proposal.getProponent());
        version.setSubmittedByName(submitterName(proposal));
        version.setSubmissionDate(proposal.getCreatedAt() != null ? proposal.getCreatedAt() : LocalDateTime.now());
        version.setRevisionDate(null);
        version.setRevisionRemarks(versionNumber == 1
                ? "Original submission content was not recorded (predates the version history feature)."
                : "Revision content was not recorded (predates the version history feature).");
        version.setDetailsJson(null);
        version.setIsCurrent(isCurrent);
        versionRepository.save(version);
    }

    private String deriveDocumentId(Proposal proposal, int versionNumber) {
        if (proposal.getProposalCode() == null) {
            return null;
        }
        int revision = versionNumber - 1;
        int year = proposal.getCreatedAt() != null ? proposal.getCreatedAt().getYear() : Year.now().getValue();
        return String.format("MSUN-ORPS-DA-%s-%d-REV%02d", proposal.getProposalCode(), year, revision);
    }

    private ProposalVersionResponse toResponse(ProposalVersion version) {
        ProposalVersionResponse response = new ProposalVersionResponse();
        response.setId(version.getId());
        response.setProposalId(version.getProposal() != null ? version.getProposal().getId() : null);
        response.setVersionNumber(version.getVersionNumber());
        response.setDocumentId(version.getDocumentId());
        response.setStatus(version.getStatus());
        response.setSubmittedById(version.getSubmittedBy() != null ? version.getSubmittedBy().getId() : null);
        response.setSubmittedByName(version.getSubmittedByName());
        response.setSubmissionDate(version.getSubmissionDate());
        response.setRevisionDate(version.getRevisionDate());
        response.setRevisionRemarks(version.getRevisionRemarks());
        response.setIsCurrent(version.getIsCurrent());
        response.setCreatedAt(version.getCreatedAt());
        return response;
    }

    private Object parseDetails(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildDetailsJson(Proposal proposal) {
        try {
            return objectMapper.writeValueAsString(buildDetailsSnapshot(proposal));
        } catch (Exception e) {
            return "{}";
        }
    }

    private String submitterName(Proposal proposal) {
        User proponent = proposal.getProponent();
        if (proponent != null && proponent.getName() != null && !proponent.getName().isBlank()) {
            return proponent.getName();
        }
        return proposal.getProjectLeader();
    }

    /**
     * Builds a complete snapshot of the proposal at this point in time: every scalar
     * field, the uploaded file names, and all child collections.
     */
    private Map<String, Object> buildDetailsSnapshot(Proposal p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("proposalCode", p.getProposalCode());
        m.put("programTitle", p.getProgramTitle());
        m.put("projectTitle", p.getProjectTitle());
        m.put("projectLeader", p.getProjectLeader());
        m.put("duration", p.getDuration());
        m.put("startDate", p.getStartDate() != null ? p.getStartDate().toString() : null);
        m.put("endDate", p.getEndDate() != null ? p.getEndDate().toString() : null);
        m.put("college", p.getCollege());
        m.put("address", p.getAddress());
        m.put("cooperatingAgencies", p.getCooperatingAgencies());
        m.put("researchType", p.getResearchType());
        m.put("innovationGoals", p.getInnovationGoals());
        m.put("sectorRelevance", p.getSectorRelevance());
        m.put("sdg", p.getSdg());
        m.put("executiveSummary", p.getExecutiveSummary());
        m.put("rationale", p.getRationale());
        m.put("framework", p.getFramework());
        m.put("objectivesGeneral", p.getObjectivesGeneral());
        m.put("objectivesSpecific", p.getObjectivesSpecific());
        m.put("review", p.getReview());
        m.put("methodology", p.getMethodology());
        m.put("technologyTrl", p.getTechnologyTrl());
        m.put("outputs", p.getOutputs());
        m.put("outcomes", p.getOutcomes());
        m.put("impactEconomic", p.getImpactEconomic());
        m.put("impactSocial", p.getImpactSocial());
        m.put("beneficiaries", p.getBeneficiaries());
        m.put("sustainability", p.getSustainability());
        m.put("gadScore", p.getGadScore());
        m.put("risks", p.getRisks());
        m.put("referencesText", p.getReferencesText());
        m.put("otherProjectsNumber", p.getOtherProjectsNumber());

        // Uploaded / attached files (file names preserved per version).
        m.put("reviewFileName", p.getReviewFileName());
        m.put("roadmapFileName", p.getRoadmapFileName());
        m.put("beneficiariesFileName", p.getBeneficiariesFileName());
        m.put("gadFileName", p.getGadFileName());

        // Child collections (their `proposal` back-reference is @JsonIgnore).
        m.put("sites", p.getSites());
        m.put("logFrames", p.getLogFrames());
        m.put("personnel", p.getPersonnel());
        m.put("budget", p.getBudget());
        m.put("otherProjects", p.getOtherProjects());
        m.put("priorityAgendas", p.getPriorityAgendas());
        m.put("limitations", p.getLimitations());

        return m;
    }
}
