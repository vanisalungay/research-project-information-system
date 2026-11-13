package com.rpis.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Canonical rules of the extra information a Proposal Announcement carries besides
 * its name, dates and status: the priority-agenda program keys selected for the call
 * and the standard announcement reminders.
 * <p>
 * The agenda keys are <b>not</b> hard-coded any more. They are the stable
 * identifiers of the Priority Agenda master data (section 05 of the Create Proposal
 * form), resolved by {@link PriorityAgendaService} and passed in as
 * {@code allowedKeys}, so a framework an administrator configures becomes selectable
 * on a new call without any code change. The standard reminders stay static - they
 * are not agendas.
 * <p>
 * Unknown or duplicated identifiers sent by a client are dropped instead of being
 * persisted, so a call can never reference a program or reminder that does not exist.
 */
public final class ProposalAnnouncementCatalog {

    /** The three standard announcement reminders, in display order. */
    public static final List<String> REMINDER_IDS =
            List.of("research-plan", "eligible-submitters", "collaboration");

    private ProposalAnnouncementCatalog() {
    }

    /**
     * Every standard reminder is selected for a newly created announcement; the
     * client may still deselect individual reminders before saving.
     */
    public static List<String> defaultReminderIds() {
        return new ArrayList<>(REMINDER_IDS);
    }

    /**
     * Keeps only the allowed agenda keys (the currently active Priority Agenda
     * items), de-duplicated and in the order the master data lists them.
     */
    public static List<String> normalizeProgramKeys(List<String> programKeys, List<String> allowedKeys) {
        return normalize(programKeys, allowedKeys);
    }

    /** Keeps only known reminder ids, de-duplicated, in canonical order. */
    public static List<String> normalizeReminderIds(List<String> reminderIds) {
        return normalize(reminderIds, REMINDER_IDS);
    }

    /**
     * Normalizes a client-supplied selection: trims, lower-cases, drops unknown
     * identifiers and returns the result in the allowed order so the persisted value
     * and the API response are stable across round-trips.
     */
    private static List<String> normalize(List<String> values, List<String> allowed) {
        if (values == null) {
            return new ArrayList<>();
        }
        List<String> safeAllowed = allowed == null ? List.of() : allowed;
        Set<String> requested = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null) {
                requested.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        List<String> normalized = new ArrayList<>();
        for (String allowedKey : safeAllowed) {
            if (allowedKey != null && requested.contains(allowedKey.toLowerCase(Locale.ROOT))) {
                normalized.add(allowedKey);
            }
        }
        return normalized;
    }
}
