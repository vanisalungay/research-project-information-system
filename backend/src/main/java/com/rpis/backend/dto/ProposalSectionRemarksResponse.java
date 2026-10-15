package com.rpis.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Read model for a proposal's section-specific revision remarks.
 * <p>
 * Remarks are grouped by revision round so the Proponent can see the feedback for
 * the current round beside the relevant sections while every earlier round remains
 * available as history (nothing is overwritten when a reviewer requests another
 * round of revisions).
 */
@Data
public class ProposalSectionRemarksResponse {

    private Long proposalId;

    /** Highest (current) revision round, or {@code null} when no remarks exist yet. */
    private Integer activeRound;

    /** Rounds ordered newest first. */
    private List<Round> rounds = new ArrayList<>();

    @Data
    public static class Round {

        private Integer round;

        /** True for the round the Proponent must currently address. */
        private boolean active;

        private List<Item> remarks = new ArrayList<>();
    }

    @Data
    public static class Item {

        private Long id;
        private String sectionKey;
        private String sectionLabel;
        /** Stable field path; {@code null} for a section-level remark. */
        private String fieldKey;
        private String fieldLabel;
        private Integer entryIndex;
        private Long entryId;
        private String entrySnapshot;
        private String remark;
        private String reviewerRole;
        private String reviewerName;
        private Long reviewerId;
        private LocalDateTime createdAt;
    }
}
