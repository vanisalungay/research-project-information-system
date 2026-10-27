package com.rpis.backend.service;

import com.rpis.backend.dto.ActivePriorityAgendaResponse;
import com.rpis.backend.dto.PriorityAgendaItemRequest;
import com.rpis.backend.dto.PriorityAgendaSetRequest;
import com.rpis.backend.model.PriorityAgenda;
import com.rpis.backend.model.PriorityAgendaSet;
import com.rpis.backend.repository.PriorityAgendaRepository;
import com.rpis.backend.repository.PriorityAgendaSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Master data of the Priority Agenda feature (section 05 of the Create Proposal
 * form).
 * <p>
 * The framework label and the agenda list used to be hard-coded in the frontend
 * ("based on MSUN RIIDE 2025-2028" plus five fixed programs). Both are now stored
 * here: an administrator maintains the agenda sets and their items, and the
 * Proponent form reads the active configuration from the database, so a new
 * framework and coverage period can be configured without any source-code change or
 * redeployment.
 * <p>
 * <b>One active set.</b> {@link #activateSet(Long)} deactivates every other set in
 * the same transaction; the partial unique index
 * {@code uk_priority_agenda_set_single_active} makes a second active row impossible
 * even under concurrent requests.
 * <p>
 * <b>History is never destroyed.</b> Sets and items are only deactivated - never
 * deleted - and the stable {@code agendaKey} is immutable once created, so a
 * proposal submitted under an agenda that is no longer offered keeps resolving to
 * its name and value.
 */
@Service
@RequiredArgsConstructor
public class PriorityAgendaService {

    /** Accepted shape of a stable agenda key, e.g. {@code dagat}, {@code blue-economy}. */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    /** Sanity bounds for a coverage year. */
    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 3000;

    private final PriorityAgendaSetRepository agendaSetRepository;
    private final PriorityAgendaRepository agendaRepository;

    // ==================== READ (Proponent form) ====================

    /**
     * The configuration a new proposal selects from: the active set and its active
     * items. Every field is null / empty when no set is active, which the form renders
     * as its empty state (never as an error, and never by falling back to hard-coded
     * agendas).
     */
    @Transactional(readOnly = true)
    public ActivePriorityAgendaResponse getActiveConfiguration() {
        ActivePriorityAgendaResponse response = new ActivePriorityAgendaResponse();
        Optional<PriorityAgendaSet> activeSet = agendaSetRepository.findFirstByActiveTrue();
        if (activeSet.isEmpty()) {
            return response;
        }
        PriorityAgendaSet set = activeSet.get();
        response.setAgendaSetId(set.getId());
        response.setFrameworkName(set.getFrameworkName());
        response.setCoverageStartYear(set.getCoverageStartYear());
        response.setCoverageEndYear(set.getCoverageEndYear());
        response.setLabel(formatLabel(set));
        for (PriorityAgenda item :
                agendaRepository.findByAgendaSetIdAndActiveTrueOrderByDisplayOrderAscIdAsc(set.getId())) {
            response.getAgendas().add(toItem(item));
        }
        return response;
    }

    /**
     * Resolves display metadata for arbitrary stable keys, including the keys of a
     * deactivated set or item, so a historical proposal keeps showing the agenda name
     * it was submitted under. Unknown keys are simply absent from the result.
     */
    @Transactional(readOnly = true)
    public List<ActivePriorityAgendaResponse.Item> lookup(Collection<String> agendaKeys) {
        List<ActivePriorityAgendaResponse.Item> items = new ArrayList<>();
        Set<String> requested = distinctLowercased(agendaKeys);
        if (requested.isEmpty()) {
            return items;
        }
        // De-duplicated by key: the same key can only exist once across the catalogue.
        Map<String, ActivePriorityAgendaResponse.Item> byKey = new LinkedHashMap<>();
        for (PriorityAgenda item : agendaRepository.findByAgendaKeyInIgnoreCase(requested)) {
            byKey.putIfAbsent(item.getAgendaKey().toLowerCase(Locale.ROOT), toItem(item));
        }
        // Returned in the order the caller asked for, so a printout keeps its order.
        for (String key : requested) {
            ActivePriorityAgendaResponse.Item item = byKey.get(key);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    /** The stable keys new proposals and new calls may currently select. */
    @Transactional(readOnly = true)
    public Set<String> activeAgendaKeys() {
        Optional<PriorityAgendaSet> activeSet = agendaSetRepository.findFirstByActiveTrue();
        if (activeSet.isEmpty()) {
            return Set.of();
        }
        Set<String> keys = new LinkedHashSet<>();
        for (PriorityAgenda item : agendaRepository
                .findByAgendaSetIdAndActiveTrueOrderByDisplayOrderAscIdAsc(activeSet.get().getId())) {
            keys.add(item.getAgendaKey());
        }
        return keys;
    }

    /**
     * Every key that may appear in a proposal submission: the active set's active
     * items plus the keys the proposal already stores. The second part is what keeps a
     * revision of a proposal submitted under an older (now deactivated) agenda working
     * without ever letting a client introduce an unknown or inactive key.
     */
    @Transactional(readOnly = true)
    public Set<String> allowedProposalKeys(Collection<String> alreadyStoredKeys) {
        Set<String> allowed = new LinkedHashSet<>(activeAgendaKeys());
        if (alreadyStoredKeys != null) {
            for (String key : alreadyStoredKeys) {
                if (key != null && !key.isBlank()) {
                    allowed.add(key.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return allowed;
    }

    /** The ready-to-display heading of a set, e.g. {@code (based on MSUN RIIDE 2025–2028)}. */
    public static String formatLabel(PriorityAgendaSet set) {
        if (set == null || set.getFrameworkName() == null || set.getFrameworkName().isBlank()) {
            return "";
        }
        if (set.getCoverageStartYear() == null || set.getCoverageEndYear() == null) {
            return "(based on " + set.getFrameworkName().trim() + ")";
        }
        return "(based on " + set.getFrameworkName().trim() + " "
                + set.getCoverageStartYear() + "\u2013" + set.getCoverageEndYear() + ")";
    }

    // ==================== READ (admin) ====================

    /** Every set with its items (active and inactive), the active set first. */
    @Transactional(readOnly = true)
    public List<PriorityAgendaSet> getAllSets() {
        List<PriorityAgendaSet> sets =
                agendaSetRepository.findAllByOrderByActiveDescCoverageStartYearDescIdDesc();
        // Force the items to load inside the transaction, so the API response is complete
        // even when the persistence context is closed before the response is serialized.
        for (PriorityAgendaSet set : sets) {
            set.getAgendas().size();
        }
        return sets;
    }

    @Transactional(readOnly = true)
    public PriorityAgendaSet getSet(Long id) {
        PriorityAgendaSet set = agendaSetRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Priority agenda set not found: " + id));
        set.getAgendas().size();
        return set;
    }

    // ==================== WRITE (admin) ====================

    /** Creates a framework / agenda set. */
    @Transactional
    public PriorityAgendaSet createSet(PriorityAgendaSetRequest request) {
        String frameworkName = requireFrameworkName(request);
        Integer startYear = requireYear(request != null ? request.getCoverageStartYear() : null, "start");
        Integer endYear = requireYear(request != null ? request.getCoverageEndYear() : null, "end");
        requireValidPeriod(startYear, endYear);
        requireUniquePeriod(frameworkName, startYear, endYear, null);

        PriorityAgendaSet set = new PriorityAgendaSet();
        set.setFrameworkName(frameworkName);
        set.setCoverageStartYear(startYear);
        set.setCoverageEndYear(endYear);
        set.setActive(Boolean.FALSE);
        PriorityAgendaSet saved = agendaSetRepository.save(set);

        if (Boolean.TRUE.equals(request != null ? request.getActive() : null)) {
            applyActive(saved);
        }
        return getSet(saved.getId());
    }

    /** Updates the framework name / coverage period of a set. */
    @Transactional
    public PriorityAgendaSet updateSet(Long id, PriorityAgendaSetRequest request) {
        PriorityAgendaSet set = getSet(id);
        String frameworkName = requireFrameworkName(request);
        Integer startYear = requireYear(request != null ? request.getCoverageStartYear() : null, "start");
        Integer endYear = requireYear(request != null ? request.getCoverageEndYear() : null, "end");
        requireValidPeriod(startYear, endYear);
        requireUniquePeriod(frameworkName, startYear, endYear, id);

        set.setFrameworkName(frameworkName);
        set.setCoverageStartYear(startYear);
        set.setCoverageEndYear(endYear);
        agendaSetRepository.save(set);

        if (Boolean.TRUE.equals(request != null ? request.getActive() : null)) {
            applyActive(set);
        }
        return getSet(id);
    }

    /**
     * Makes one set the active one and deactivates every other set in the same
     * transaction. The previous set and all of its items are kept untouched, so
     * proposals submitted under it keep their historical selection.
     */
    @Transactional
    public PriorityAgendaSet activateSet(Long id) {
        PriorityAgendaSet set = getSet(id);
        applyActive(set);
        return getSet(id);
    }

    /** Adds an agenda item to a set. */
    @Transactional
    public PriorityAgenda addItem(Long setId, PriorityAgendaItemRequest request) {
        PriorityAgendaSet set = getSet(setId);
        String name = requireItemName(request);
        if (agendaRepository.existsByAgendaSetIdAndNameIgnoreCase(setId, name)) {
            throw new IllegalStateException(
                    "\"" + name + "\" is already an agenda of " + set.getFrameworkName() + ".");
        }
        String key = resolveKey(setId, request != null ? request.getAgendaKey() : null, name);

        PriorityAgenda item = new PriorityAgenda();
        item.setAgendaSet(set);
        item.setAgendaKey(key);
        item.setName(name);
        item.setSubLabel(trimToNull(request != null ? request.getSubLabel() : null,
                PriorityAgenda.SUB_LABEL_MAX_LENGTH, "Sub-label"));
        item.setDescription(trimToNull(request != null ? request.getDescription() : null, null, null));
        item.setDisplayOrder(request != null && request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : nextDisplayOrder(setId));
        item.setActive(request == null || request.getActive() == null || request.getActive());
        return agendaRepository.save(item);
    }

    /**
     * Updates an agenda item. The stable key is never editable; {@code active} is how
     * an item is retired without deleting it (and without touching any proposal).
     */
    @Transactional
    public PriorityAgenda updateItem(Long id, PriorityAgendaItemRequest request) {
        PriorityAgenda item = agendaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Priority agenda not found: " + id));
        Long setId = item.getAgendaSet().getId();
        String name = requireItemName(request);
        if (agendaRepository.existsByAgendaSetIdAndNameIgnoreCaseAndIdNot(setId, name, id)) {
            throw new IllegalStateException("\"" + name + "\" is already an agenda of this set.");
        }

        item.setName(name);
        item.setSubLabel(trimToNull(request != null ? request.getSubLabel() : null,
                PriorityAgenda.SUB_LABEL_MAX_LENGTH, "Sub-label"));
        item.setDescription(trimToNull(request != null ? request.getDescription() : null, null, null));
        if (request != null && request.getDisplayOrder() != null) {
            item.setDisplayOrder(request.getDisplayOrder());
        }
        if (request != null && request.getActive() != null) {
            item.setActive(request.getActive());
        }
        return agendaRepository.save(item);
    }

    // ==================== INTERNALS ====================

    /**
     * Activates one set, deactivating all others first and flushing so the partial
     * unique index never sees two active rows at the same moment.
     */
    private void applyActive(PriorityAgendaSet target) {
        boolean changed = false;
        for (PriorityAgendaSet set : agendaSetRepository.findAll()) {
            if (!set.getId().equals(target.getId()) && Boolean.TRUE.equals(set.getActive())) {
                set.setActive(false);
                changed = true;
            }
        }
        if (changed) {
            agendaSetRepository.flush();
        }
        if (!Boolean.TRUE.equals(target.getActive())) {
            target.setActive(true);
        }
        agendaSetRepository.saveAndFlush(target);
    }

    private static ActivePriorityAgendaResponse.Item toItem(PriorityAgenda item) {
        ActivePriorityAgendaResponse.Item dto = new ActivePriorityAgendaResponse.Item();
        dto.setId(item.getId());
        dto.setAgendaKey(item.getAgendaKey());
        dto.setName(item.getName());
        dto.setSubLabel(item.getSubLabel());
        dto.setDescription(item.getDescription());
        dto.setDisplayOrder(item.getDisplayOrder());
        return dto;
    }

    private String requireFrameworkName(PriorityAgendaSetRequest request) {
        String raw = request != null ? request.getFrameworkName() : null;
        String name = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Framework name is required.");
        }
        if (name.length() > PriorityAgendaSet.FRAMEWORK_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Framework name must be at most "
                    + PriorityAgendaSet.FRAMEWORK_NAME_MAX_LENGTH + " characters.");
        }
        return name;
    }

    private static Integer requireYear(Integer year, String which) {
        if (year == null) {
            throw new IllegalArgumentException("Coverage " + which + " year is required.");
        }
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new IllegalArgumentException("Coverage " + which + " year must be between "
                    + MIN_YEAR + " and " + MAX_YEAR + ".");
        }
        return year;
    }

    private static void requireValidPeriod(Integer startYear, Integer endYear) {
        if (endYear < startYear) {
            throw new IllegalArgumentException(
                    "Coverage end year must not be earlier than the start year.");
        }
    }

    private void requireUniquePeriod(String frameworkName, Integer startYear, Integer endYear, Long id) {
        boolean exists = id == null
                ? agendaSetRepository.existsByFrameworkNameIgnoreCaseAndCoverageStartYearAndCoverageEndYear(
                        frameworkName, startYear, endYear)
                : agendaSetRepository.existsByFrameworkNameIgnoreCaseAndCoverageStartYearAndCoverageEndYearAndIdNot(
                        frameworkName, startYear, endYear, id);
        if (exists) {
            throw new IllegalStateException(frameworkName + " " + startYear + "-" + endYear
                    + " already exists. Deactivate it or edit the existing set instead.");
        }
    }

    private String requireItemName(PriorityAgendaItemRequest request) {
        String raw = request != null ? request.getName() : null;
        String name = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Agenda name is required.");
        }
        if (name.length() > PriorityAgenda.NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Agenda name must be at most "
                    + PriorityAgenda.NAME_MAX_LENGTH + " characters.");
        }
        return name;
    }

    /**
     * Uses the administrator-supplied key when it is valid, otherwise derives a unique
     * one from the name. A derived key that is already taken gets a numeric suffix, so
     * two agendas of the same set can never share the identifier every proposal stores.
     */
    private String resolveKey(Long setId, String rawKey, String name) {
        String key = rawKey == null ? "" : rawKey.trim().toLowerCase(Locale.ROOT);
        if (!key.isEmpty()) {
            if (!KEY_PATTERN.matcher(key).matches() || key.length() > PriorityAgenda.KEY_MAX_LENGTH) {
                throw new IllegalArgumentException(
                        "Agenda key may only contain lowercase letters, digits and hyphens (max "
                                + PriorityAgenda.KEY_MAX_LENGTH + " characters).");
            }
            if (agendaRepository.existsByAgendaSetIdAndAgendaKeyIgnoreCase(setId, key)) {
                throw new IllegalStateException(
                        "\"" + key + "\" is already used by another agenda of this set.");
            }
            return key;
        }
        String base = slugify(name);
        if (base.isEmpty()) {
            base = "agenda";
        }
        if (base.length() > PriorityAgenda.KEY_MAX_LENGTH) {
            base = base.substring(0, PriorityAgenda.KEY_MAX_LENGTH);
        }
        String candidate = base;
        int suffix = 2;
        while (agendaRepository.existsByAgendaSetIdAndAgendaKeyIgnoreCase(setId, candidate)) {
            String tail = "-" + suffix++;
            int maxBase = Math.max(1, PriorityAgenda.KEY_MAX_LENGTH - tail.length());
            candidate = (base.length() > maxBase ? base.substring(0, maxBase) : base) + tail;
        }
        return candidate;
    }

    /** Lowercase, non-alphanumeric runs collapsed to single hyphens. */
    static String slugify(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
    }

    private int nextDisplayOrder(Long setId) {
        int max = -1;
        for (PriorityAgenda item : agendaRepository.findByAgendaSetIdOrderByDisplayOrderAscIdAsc(setId)) {
            if (item.getDisplayOrder() != null && item.getDisplayOrder() > max) {
                max = item.getDisplayOrder();
            }
        }
        return max + 1;
    }

    private static String trimToNull(String value, Integer maxLength, String label) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (maxLength != null && trimmed.length() > maxLength) {
            throw new IllegalArgumentException(
                    (label == null ? "Value" : label) + " must be at most " + maxLength + " characters.");
        }
        return trimmed;
    }

    private static Set<String> distinctLowercased(Collection<String> values) {
        Set<String> result = new TreeSet<>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                result.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }
}
