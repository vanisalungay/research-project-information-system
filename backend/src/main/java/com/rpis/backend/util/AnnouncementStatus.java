package com.rpis.backend.util;

import com.rpis.backend.model.ProposalAnnouncement;

import java.time.LocalDate;

/**
 * The status proponents actually see for a Call for Proposal: DRAFT, SCHEDULED,
 * OPEN or CLOSED.
 * <p>
 * It is derived from the stored status (DRAFT / ACTIVE / CLOSED) plus the saved
 * submission window, and mirrors {@code src/utils/announcementStatus.js} so the
 * notification rules, the Proponent notification badges and the Call for Proposal
 * pages all agree.
 * <p>
 * Kept as a dependency-free helper (no Spring bean) so both
 * {@code ProposalAnnouncementService} and {@code NotificationService} can apply the
 * exact same rule without a circular bean dependency.
 */
public final class AnnouncementStatus {

    public static final String DISPLAY_DRAFT = "DRAFT";
    public static final String DISPLAY_SCHEDULED = "SCHEDULED";
    public static final String DISPLAY_OPEN = "OPEN";
    public static final String DISPLAY_CLOSED = "CLOSED";

    private AnnouncementStatus() {
    }

    /**
     * The status proponents see for an announcement as of the given day. A null
     * announcement has no status (used when a notification points at a call that no
     * longer exists).
     */
    public static String displayStatus(ProposalAnnouncement announcement, LocalDate today) {
        if (announcement == null) {
            return null;
        }
        if (ProposalAnnouncement.STATUS_DRAFT.equals(announcement.getStatus())) {
            return DISPLAY_DRAFT;
        }
        if (ProposalAnnouncement.STATUS_CLOSED.equals(announcement.getStatus())) {
            return DISPLAY_CLOSED;
        }
        if (announcement.getStartDate() != null && announcement.getStartDate().isAfter(today)) {
            return DISPLAY_SCHEDULED;
        }
        if (announcement.getEndDate() != null && announcement.getEndDate().isBefore(today)) {
            return DISPLAY_CLOSED;
        }
        return DISPLAY_OPEN;
    }

    /** The status proponents see for an announcement as of today. */
    public static String displayStatus(ProposalAnnouncement announcement) {
        return displayStatus(announcement, LocalDate.now());
    }

    /** True only for the statuses that make an announcement eligible for an update notification. */
    public static boolean isOpenOrClosed(String displayStatus) {
        return DISPLAY_OPEN.equals(displayStatus) || DISPLAY_CLOSED.equals(displayStatus);
    }
}
