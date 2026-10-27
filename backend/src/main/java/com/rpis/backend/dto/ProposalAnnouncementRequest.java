package com.rpis.backend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Request payload for creating/updating a Proposal Announcement.
 */
@Data
public class ProposalAnnouncementRequest {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    /** DRAFT | ACTIVE | CLOSED. Defaults to DRAFT when omitted. */
    private String status;

    /**
     * Stable RIIDE priority-agenda program keys (for example {@code dagat}).
     * Optional: an announcement may be created with no agenda selected.
     */
    private List<String> priorityAgendas;

    /**
     * Stable ids of the standard announcement reminders. {@code null} on create
     * means "use the standard reminder defaults"; {@code null} on update means
     * "keep the saved selection".
     */
    private List<String> reminderIds;

    /**
     * Optional free-text announcement body.
     */
    private String additionalAnnouncement;

    /**
     * Funding program (call type) of this Call for Proposal, as a stable key.
     * Omitted or blank means the default Internally Funded Project (IFP) program, so
     * every existing client keeps working. The endorsement workflow uses it to choose
     * the official endorsement-letter template.
     */
    private String fundingProgram;
}
