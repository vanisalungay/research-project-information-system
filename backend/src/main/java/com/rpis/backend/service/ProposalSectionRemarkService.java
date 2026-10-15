package com.rpis.backend.service;

import com.rpis.backend.dto.FieldRemarkRequest;
import com.rpis.backend.dto.ProposalSectionRemarksResponse;
import com.rpis.backend.dto.SectionRemarkDTO;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalSectionRemark;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalSectionRemarkRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Persistence and read model for section- AND field-level revision remarks.
 * <p>
 * <b>Rounds.</b> One revision cycle = one round. Every write (a reviewer saving a
 * remark beside a field, a reviewing office returning a proposal, RPS forwarding
 * it) appends to the round that is currently open, creating round 1 only when no
 * round is open. The round is closed when the Proponent resubmits the revised
 * proposal ({@link #closeActiveRound(Proposal)}), so a later review cycle starts a
 * new round while every earlier round is retained as history — nothing is ever
 * overwritten or deleted.
 * <p>
 * <b>Field identity.</b> A remark stores a stable data path ({@code field_key}),
 * never just a display label, plus a best-effort {@code entry_id}, the entry's
 * {@code entry_snapshot} and its {@code entry_index} for repeated entries. The
 * frontend uses that ladder to re-align a remark after rows are added, removed or
 * reordered.
 * <p>
 * <b>Authorization.</b> Only authenticated reviewer roles (RPS Admin/Staff,
 * OVCRIGE, OVCAF, REC, OC) may write remarks, and the reviewer's identity is taken
 * from the security context rather than from the request body.
 */
@Service
@RequiredArgsConstructor
public class ProposalSectionRemarkService {

    /** Upper bound per remark, mirroring the reviewer text areas. */
    public static final int MAX_REMARK_LENGTH = 5000;

    /** Upper bound for a field key / label. */
    public static final int MAX_FIELD_KEY_LENGTH = 120;
    public static final int MAX_FIELD_LABEL_LENGTH = 255;

    /**
     * Accepted field-key shape: dotted paths with optional zero-based indexes, e.g.
     * {@code projectTitle}, {@code personnel[1]}, {@code personnel[1].time},
     * {@code priorityAgenda.kalikasan.value}.
     */
    private static final Pattern FIELD_KEY_PATTERN =
            Pattern.compile("^[A-Za-z][A-Za-z0-9_]*(\\[\\d+\\])?(\\.[A-Za-z][A-Za-z0-9_]*(\\[\\d+\\])?)*$");

    private final ProposalSectionRemarkRepository sectionRemarkRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    /** Reviewer identity resolved from the security context. */
    private record Actor(String role, Long userId, String name) {
    }

    // ======================== WRITES ========================

    /**
     * Creates or updates the remark for one field of a proposal. Saving the same
     * field again updates the existing remark of the open round instead of
     * duplicating it, so a reviewer can edit their feedback freely.
     */
    @Transactional
    public ProposalSectionRemarksResponse.Item upsertFieldRemark(Proposal proposal, FieldRemarkRequest request) {
        requireReviewer();
        if (request == null || request.getRemark() == null || request.getRemark().isBlank()) {
            throw new IllegalArgumentException("A remark text is required.");
        }

        String sectionKey = ProposalSectionCatalog.normalize(request.getSectionKey());
        if (!ProposalSectionCatalog.isValid(sectionKey)) {
            throw new IllegalArgumentException(
                    "Unknown proposal section for remark: " + request.getSectionKey());
        }
        String fieldKey = normalizeFieldKey(request.getFieldKey());
        Actor actor = resolveActor();

        List<ProposalSectionRemark> active = activeRemarks(proposal);
        int round = roundFor(proposal, active);

        ProposalSectionRemark entity = active.stream()
                .filter(row -> sameField(row, sectionKey, fieldKey, request.getEntryIndex()))
                .findFirst()
                .orElseGet(() -> {
                    ProposalSectionRemark created = new ProposalSectionRemark();
                    created.setProposal(proposal);
                    created.setSectionKey(sectionKey);
                    created.setFieldKey(fieldKey);
                    created.setRound(round);
                    created.setStatus(ProposalSectionRemark.STATUS_ACTIVE);
                    return created;
                });

        entity.setSectionLabel(ProposalSectionCatalog.labelFor(sectionKey));
        entity.setFieldLabel(truncate(
                request.getFieldLabel() != null && !request.getFieldLabel().isBlank()
                        ? request.getFieldLabel().trim()
                        : null,
                MAX_FIELD_LABEL_LENGTH));
        entity.setEntryIndex(request.getEntryIndex());
        entity.setEntryId(request.getEntryId());
        entity.setEntrySnapshot(truncate(request.getEntrySnapshot(), MAX_REMARK_LENGTH));
        entity.setRemark(truncate(request.getRemark().trim(), MAX_REMARK_LENGTH));
        entity.setReviewerRole(actor.role());
        entity.setReviewerId(actor.userId());
        entity.setReviewerName(actor.name());

        return toItem(sectionRemarkRepository.save(entity));
    }

    /** Removes one remark (used when a reviewer clears their feedback for a field). */
    @Transactional
    public void deleteRemark(Long proposalId, Long remarkId) {
        requireReviewer();
        ProposalSectionRemark remark = sectionRemarkRepository.findById(remarkId)
                .orElseThrow(() -> new IllegalArgumentException("Remark not found: " + remarkId));
        if (remark.getProposal() == null || !proposalId.equals(remark.getProposal().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "That remark does not belong to this proposal.");
        }
        sectionRemarkRepository.delete(remark);
    }

    /**
     * Records the remarks a reviewing office submits when returning a proposal. They
     * are appended to the open round so field remarks the reviewer already saved for
     * this cycle stay visible to the Proponent.
     */
    @Transactional
    public void recordReturnRemarks(Proposal proposal, List<SectionRemarkDTO> remarks,
                                    String reviewerRole, Long reviewerId, String reviewerName) {
        appendAll(proposal, remarks, reviewerRole, reviewerId, reviewerName);
    }

    /** RPS appends its own remarks (section- or field-level) to the open round. */
    @Transactional
    public void recordForwardRemarks(Proposal proposal, List<SectionRemarkDTO> remarks,
                                     String reviewerRole, Long reviewerId, String reviewerName) {
        appendAll(proposal, remarks, reviewerRole, reviewerId, reviewerName);
    }

    /**
     * Closes the open round. Called when the Proponent resubmits a revised proposal:
     * the round the reviewer was working on becomes history and the next review cycle
     * starts a fresh round, so nothing is overwritten.
     */
    @Transactional
    public void closeActiveRound(Proposal proposal) {
        if (proposal == null || proposal.getId() == null) {
            return;
        }
        List<ProposalSectionRemark> active = activeRemarks(proposal);
        if (active.isEmpty()) {
            return;
        }
        for (ProposalSectionRemark row : active) {
            row.setStatus(ProposalSectionRemark.STATUS_SUPERSEDED);
        }
        sectionRemarkRepository.saveAll(active);
    }

    // ======================== READ ========================

    /** Full remark history of a proposal, grouped by revision round (newest first). */
    @Transactional(readOnly = true)
    public ProposalSectionRemarksResponse getRemarksForProposal(Long proposalId) {
        List<ProposalSectionRemark> all = sectionRemarkRepository
                .findByProposalIdOrderByRoundDescIdAsc(proposalId);

        ProposalSectionRemarksResponse response = new ProposalSectionRemarksResponse();
        response.setProposalId(proposalId);

        Integer activeRound = null;
        ProposalSectionRemarksResponse.Round current = null;

        for (ProposalSectionRemark remark : all) {
            int roundNumber = remark.getRound() != null ? remark.getRound() : 1;
            if (current == null || current.getRound() == null || current.getRound() != roundNumber) {
                current = new ProposalSectionRemarksResponse.Round();
                current.setRound(roundNumber);
                current.setActive(ProposalSectionRemark.STATUS_ACTIVE.equals(remark.getStatus()));
                response.getRounds().add(current);
            }
            if (ProposalSectionRemark.STATUS_ACTIVE.equals(remark.getStatus()) && activeRound == null) {
                activeRound = roundNumber;
            }
            current.getRemarks().add(toItem(remark));
        }

        response.setActiveRound(activeRound);
        return response;
    }

    // ======================== INTERNALS ========================

    /**
     * Appends the submitted remarks to the open round (creating round 1 when no round
     * is open). A remark for the same field replaces the existing one instead of
     * duplicating it.
     */
    private void appendAll(Proposal proposal, List<SectionRemarkDTO> remarks,
                           String fallbackRole, Long fallbackId, String fallbackName) {
        List<SectionRemarkDTO> usable = normalize(remarks);
        if (usable.isEmpty()) {
            return;
        }
        requireReviewer();
        Actor actor = resolveActor(fallbackRole, fallbackId, fallbackName);
        List<ProposalSectionRemark> active = activeRemarks(proposal);
        int round = roundFor(proposal, active);

        for (SectionRemarkDTO dto : usable) {
            ProposalSectionRemark entity = active.stream()
                    .filter(row -> sameField(row, dto.getSectionKey(), dto.getFieldKey(), dto.getEntryIndex()))
                    .findFirst()
                    .orElseGet(() -> {
                        ProposalSectionRemark created = new ProposalSectionRemark();
                        created.setProposal(proposal);
                        created.setSectionKey(dto.getSectionKey());
                        created.setFieldKey(dto.getFieldKey());
                        created.setRound(round);
                        created.setStatus(ProposalSectionRemark.STATUS_ACTIVE);
                        active.add(created);
                        return created;
                    });
            entity.setSectionKey(dto.getSectionKey());
            entity.setSectionLabel(ProposalSectionCatalog.labelFor(dto.getSectionKey()));
            entity.setFieldLabel(truncate(dto.getFieldLabel(), MAX_FIELD_LABEL_LENGTH));
            entity.setEntryIndex(dto.getEntryIndex());
            entity.setEntryId(dto.getEntryId());
            entity.setEntrySnapshot(truncate(dto.getEntrySnapshot(), MAX_REMARK_LENGTH));
            entity.setRemark(truncate(dto.getRemark(), MAX_REMARK_LENGTH));
            entity.setReviewerRole(actor.role());
            entity.setReviewerId(actor.userId());
            entity.setReviewerName(actor.name());
        }

        List<ProposalSectionRemark> toSave = active.stream()
                .filter(row -> row.getId() == null || row.getRemark() != null)
                .toList();
        sectionRemarkRepository.saveAll(toSave);
    }

    /** The open (ACTIVE) round of a proposal, or an empty list when none is open. */
    private List<ProposalSectionRemark> activeRemarks(Proposal proposal) {
        if (proposal == null || proposal.getId() == null) {
            return new java.util.ArrayList<>();
        }
        return new java.util.ArrayList<>(sectionRemarkRepository
                .findByProposalIdAndStatus(proposal.getId(), ProposalSectionRemark.STATUS_ACTIVE));
    }

    /**
     * The round new remarks belong to: the open round when there is one, otherwise the
     * next unused round number. A round is only closed by
     * {@link #closeActiveRound(Proposal)} (i.e. when the Proponent resubmits), which
     * keeps everything a reviewer saved for this cycle together — including remarks
     * saved before the proposal was returned.
     */
    private int roundFor(Proposal proposal, List<ProposalSectionRemark> active) {
        if (!active.isEmpty() && active.get(0).getRound() != null) {
            return active.get(0).getRound();
        }
        Integer maxRound = sectionRemarkRepository.findMaxRound(proposal.getId());
        return (maxRound != null ? maxRound : 0) + 1;
    }

    /**
     * True when an existing remark addresses the same field of the same section. The
     * position of a repeated entry is part of the identity, so two remarks on the same
     * column of different rows stay separate.
     */
    private boolean sameField(ProposalSectionRemark row, String sectionKey, String fieldKey,
                              Integer entryIndex) {
        if (!java.util.Objects.equals(row.getSectionKey(), sectionKey)) {
            return false;
        }
        if (!java.util.Objects.equals(row.getFieldKey(), fieldKey)) {
            return false;
        }
        if (fieldKey == null) {
            // One section-level remark per section per round.
            return true;
        }
        return java.util.Objects.equals(row.getEntryIndex(), entryIndex);
    }

    /** Validates and trims the submitted remarks; unknown sections are rejected. */
    private List<SectionRemarkDTO> normalize(List<SectionRemarkDTO> remarks) {
        List<SectionRemarkDTO> result = new java.util.ArrayList<>();
        if (remarks == null) {
            return result;
        }
        for (SectionRemarkDTO dto : remarks) {
            if (dto == null || dto.getRemark() == null || dto.getRemark().isBlank()) {
                continue;
            }
            String sectionKey = ProposalSectionCatalog.normalize(dto.getSectionKey());
            if (!ProposalSectionCatalog.isValid(sectionKey)) {
                throw new IllegalArgumentException(
                        "Unknown proposal section for remark: " + dto.getSectionKey());
            }
            SectionRemarkDTO normalized = new SectionRemarkDTO();
            normalized.setSectionKey(sectionKey);
            normalized.setSectionLabel(ProposalSectionCatalog.labelFor(sectionKey));
            normalized.setFieldKey(normalizeFieldKey(dto.getFieldKey()));
            normalized.setFieldLabel(truncate(dto.getFieldLabel(), MAX_FIELD_LABEL_LENGTH));
            normalized.setEntryIndex(dto.getEntryIndex());
            normalized.setEntryId(dto.getEntryId());
            normalized.setEntrySnapshot(truncate(dto.getEntrySnapshot(), MAX_REMARK_LENGTH));
            normalized.setRemark(truncate(dto.getRemark(), MAX_REMARK_LENGTH));
            result.add(normalized);
        }
        return result;
    }

    /** True when the payload contains at least one remark that will be persisted. */
    public boolean hasUsableRemarks(List<SectionRemarkDTO> remarks) {
        return !normalize(remarks).isEmpty();
    }

    /** Validates a stable field path; blank becomes {@code null} (section-level remark). */
    private String normalizeFieldKey(String fieldKey) {
        if (fieldKey == null || fieldKey.isBlank()) {
            return null;
        }
        String value = fieldKey.trim();
        if (value.length() > MAX_FIELD_KEY_LENGTH || !FIELD_KEY_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid field identifier for remark: " + fieldKey);
        }
        return value;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    /** Only authenticated reviewer roles may write remarks. */
    private void requireReviewer() {
        if (!currentUser.isPrivileged()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only authorized reviewers may manage revision remarks.");
        }
    }

    /** Reviewer identity from the security context, falling back to the payload. */
    private Actor resolveActor() {
        return resolveActor(null, null, null);
    }

    private Actor resolveActor(String fallbackRole, Long fallbackId, String fallbackName) {
        Long userId = currentUser.getUserId();
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                return new Actor(user.getRole(), user.getId(), user.getName());
            }
        }
        return new Actor(fallbackRole, fallbackId, fallbackName);
    }

    private ProposalSectionRemarksResponse.Item toItem(ProposalSectionRemark remark) {
        ProposalSectionRemarksResponse.Item item = new ProposalSectionRemarksResponse.Item();
        item.setId(remark.getId());
        item.setSectionKey(remark.getSectionKey());
        item.setSectionLabel(remark.getSectionLabel());
        item.setFieldKey(remark.getFieldKey());
        item.setFieldLabel(remark.getFieldLabel());
        item.setEntryIndex(remark.getEntryIndex());
        item.setEntryId(remark.getEntryId());
        item.setEntrySnapshot(remark.getEntrySnapshot());
        item.setRemark(remark.getRemark());
        item.setReviewerRole(remark.getReviewerRole());
        item.setReviewerName(remark.getReviewerName());
        item.setReviewerId(remark.getReviewerId());
        item.setCreatedAt(remark.getCreatedAt());
        return item;
    }
}
