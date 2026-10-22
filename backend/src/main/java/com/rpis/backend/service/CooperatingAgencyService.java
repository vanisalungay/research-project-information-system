package com.rpis.backend.service;

import com.rpis.backend.model.CooperatingAgency;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalCooperatingAgency;
import com.rpis.backend.repository.CooperatingAgencyRepository;
import com.rpis.backend.repository.ProposalCooperatingAgencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The shared Cooperating Agency directory of section 02 of the Create Proposal
 * form.
 * <p>
 * The directory is university-wide: an agency added by one proponent is offered to
 * every other proponent on their next proposal, and an agency is never deleted
 * because somebody unchecked it. A proposal stores only the ids of the agencies it
 * selected ({@link ProposalCooperatingAgency}); the free-text column the existing
 * screens read is derived from those ids by {@link #describeSelection(List)}, so
 * the two can never drift apart.
 * <p>
 * Duplicates are prevented twice: this service answers the common case with a
 * helpful message, and the case-insensitive unique index on {@code LOWER(name)}
 * makes it impossible for two simultaneous requests to store the same agency.
 */
@Service
@RequiredArgsConstructor
public class CooperatingAgencyService {

    private final CooperatingAgencyRepository cooperatingAgencyRepository;
    private final ProposalCooperatingAgencyRepository proposalCooperatingAgencyRepository;

    /** The whole directory, in the order the Create Proposal checklist shows it. */
    public List<CooperatingAgency> getAllAgencies() {
        return cooperatingAgencyRepository.findAllOrdered();
    }

    /**
     * Adds an agency to the shared directory.
     * <p>
     * The name is trimmed and its inner whitespace collapsed before it is stored,
     * so " DOST " and "DOST" cannot become two rows. An existing name - compared
     * case-insensitively - is reported as a conflict instead of being inserted
     * again, and a request that loses a race against another proponent's identical
     * add is reported the same way.
     *
     * @param rawName name as typed by the proponent
     * @return the stored agency
     * @throws IllegalArgumentException when the name is missing or too long
     * @throws IllegalStateException    when the agency already exists
     */
    @Transactional
    public CooperatingAgency addAgency(String rawName) {
        String name = normalizeName(rawName);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Agency name is required.");
        }
        if (name.length() > CooperatingAgency.NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Agency name must be at most " + CooperatingAgency.NAME_MAX_LENGTH + " characters.");
        }

        if (cooperatingAgencyRepository.findFirstByNameIgnoreCase(name).isPresent()) {
            throw new IllegalStateException(duplicateMessage(name));
        }

        try {
            return cooperatingAgencyRepository.saveAndFlush(new CooperatingAgency(null, name, null));
        } catch (DataIntegrityViolationException e) {
            // Another request stored the same agency between the check above and this
            // insert: the case-insensitive unique index rejected this row, so nothing
            // was saved and the caller gets the same "already exists" answer.
            throw new IllegalStateException(duplicateMessage(name));
        }
    }

    /** The agencies selected on a proposal, in the directory's (name) order. */
    public List<CooperatingAgency> getSelection(Long proposalId) {
        if (proposalId == null) {
            return List.of();
        }
        return proposalCooperatingAgencyRepository.findByProposalIdOrderByAgencyNameAsc(proposalId).stream()
                .map(ProposalCooperatingAgency::getAgency)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Replaces the cooperating agencies of a proposal with the given directory ids.
     * <p>
     * The ids are validated against the directory - an unknown id is rejected
     * instead of being trusted - and the previous selection is replaced, so
     * unchecking an agency only removes the link to this proposal. The returned
     * agencies are what the caller stores in the proposal's text column.
     *
     * @param proposal  proposal that was already saved (it must have an id)
     * @param agencyIds ids selected in the checklist; may be empty
     * @return the resolved agencies, in directory order
     * @throws IllegalArgumentException when the proposal has no id yet or an id is unknown
     */
    @Transactional
    public List<CooperatingAgency> replaceSelection(Proposal proposal, List<Long> agencyIds) {
        if (proposal == null || proposal.getId() == null) {
            throw new IllegalArgumentException(
                    "A proposal must be saved before its cooperating agencies can be stored.");
        }
        List<CooperatingAgency> selected = resolve(distinctIds(agencyIds));

        proposalCooperatingAgencyRepository.deleteByProposalId(proposal.getId());
        // Flush the delete before the new rows are inserted: the unique
        // (proposal_id, cooperating_agency_id) constraint must not see both at once.
        proposalCooperatingAgencyRepository.flush();
        for (CooperatingAgency agency : selected) {
            proposalCooperatingAgencyRepository.save(new ProposalCooperatingAgency(null, proposal, agency));
        }
        return selected;
    }

    /**
     * Copies the selection of one proposal onto another. Used by "Make a Copy": the
     * duplicate references the very same directory rows, so the agencies are neither
     * duplicated nor removed from the original proposal.
     */
    @Transactional
    public void copySelection(Long sourceProposalId, Proposal copy) {
        if (sourceProposalId == null || copy == null || copy.getId() == null) {
            return;
        }
        for (CooperatingAgency agency : getSelection(sourceProposalId)) {
            proposalCooperatingAgencyRepository.save(new ProposalCooperatingAgency(null, copy, agency));
        }
    }

    /** Removes the selection of a proposal that is being deleted. */
    @Transactional
    public void deleteSelection(Long proposalId) {
        if (proposalId == null) {
            return;
        }
        proposalCooperatingAgencyRepository.deleteByProposalId(proposalId);
    }

    /**
     * The names of the given agencies, in the given (directory) order, as the
     * proposal's "Cooperating Agencies" text column stores them. That column is kept
     * for every existing screen, export and printout.
     */
    public static String describeSelection(List<CooperatingAgency> agencies) {
        if (agencies == null) {
            return "";
        }
        return agencies.stream()
                .map(CooperatingAgency::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining(", "));
    }

    /** Trims the name and collapses inner whitespace runs to single spaces. */
    static String normalizeName(String rawName) {
        if (rawName == null) {
            return "";
        }
        return rawName.trim().replaceAll("\\s+", " ");
    }

    /** Resolves the ids the client sent against the directory; unknown ids are rejected. */
    private List<CooperatingAgency> resolve(List<Long> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, CooperatingAgency> byId = cooperatingAgencyRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(CooperatingAgency::getId, Function.identity()));
        List<CooperatingAgency> selected = new ArrayList<>();
        for (Long id : ids) {
            CooperatingAgency agency = byId.get(id);
            if (agency == null) {
                throw new IllegalArgumentException("Unknown cooperating agency: " + id);
            }
            selected.add(agency);
        }
        selected.sort(Comparator.comparing(CooperatingAgency::getName, String.CASE_INSENSITIVE_ORDER));
        return selected;
    }

    /** The submitted ids, de-duplicated and with nulls dropped. */
    private static List<Long> distinctIds(List<Long> agencyIds) {
        if (agencyIds == null) {
            return List.of();
        }
        return new ArrayList<>(
                agencyIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private static String duplicateMessage(String name) {
        return name + " is already in the cooperating agencies list.";
    }
}
