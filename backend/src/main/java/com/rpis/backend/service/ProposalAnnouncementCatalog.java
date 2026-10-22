package com.rpis.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Canonical catalog of the information a Proposal Announcement can carry besides
 * its name, dates and status: the MSUN RIIDE 2025–2028 priority-agenda programs
 * and the standard announcement reminders.
 * <p>
 * The identifiers listed here are the stable keys persisted with every
 * announcement — never array positions and never the display labels. They are
 * shared with the Create/Edit Proposal Announcement form
 * ({@code src/utils/announcementOptions.js}) and the RIIDE program keys already
 * used by the proponent proposal form ({@code ProponentSubmitProp.vue}:
 * {@code dagat}, {@code punla}, {@code kalikasan}, {@code negosyo},
 * {@code tanglaw}).
 * <p>
 * Unknown or duplicated identifiers sent by a client are dropped instead of
 * being persisted, so an announcement can never reference a program or reminder
 * that does not exist.
 */
public final class ProposalAnnouncementCatalog {

    /** The five RIIDE priority-agenda programs, in display order. */
    public static final List<String> PROGRAM_KEYS =
            List.of("dagat", "punla", "kalikasan", "negosyo", "tanglaw");

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

    /** Keeps only known RIIDE program keys, de-duplicated, in canonical order. */
    public static List<String> normalizeProgramKeys(List<String> programKeys) {
        return normalize(programKeys, PROGRAM_KEYS);
    }

    /** Keeps only known reminder ids, de-duplicated, in canonical order. */
    public static List<String> normalizeReminderIds(List<String> reminderIds) {
        return normalize(reminderIds, REMINDER_IDS);
    }

    /**
     * Normalizes a client-supplied selection: trims, lower-cases, drops unknown
     * identifiers and returns the result in the catalog's canonical order so the
     * persisted value and the API response are stable across round-trips.
     */
    private static List<String> normalize(List<String> values, List<String> allowed) {
        if (values == null) {
            return new ArrayList<>();
        }
        Set<String> requested = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null) {
                requested.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        List<String> normalized = new ArrayList<>();
        for (String allowedKey : allowed) {
            if (requested.contains(allowedKey)) {
                normalized.add(allowedKey);
            }
        }
        normalized.sort(Comparator.comparingInt(allowed::indexOf));
        return normalized;
    }
}
