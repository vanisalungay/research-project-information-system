package com.rpis.backend.dto;

import lombok.Data;

/**
 * A reviewer's remark for one specific proposal section.
 * <p>
 * Used both when a reviewing office returns a proposal for revision and when RPS
 * forwards the revision request to the Proponent. The {@code sectionKey} is the
 * canonical proposal section number (01-27) and must match the catalog in
 * {@code ProposalSectionCatalog} so a remark can never be stored against a
 * non-existent section.
 */
@Data
public class SectionRemarkDTO {

    /** Canonical section key, e.g. "12" for Methodology. */
    private String sectionKey;

    /** Optional human-readable section label; the server catalog is the source of truth. */
    private String sectionLabel;

    /** The reviewer's remark for this section. Blank remarks are ignored. */
    private String remark;

    /**
     * Optional stable field path inside the section, e.g. {@code projectTitle} or
     * {@code personnel[1].time}. When set, the remark is attached to that specific
     * field instead of the section as a whole.
     */
    private String fieldKey;

    /** Optional human-readable field label. */
    private String fieldLabel;

    /** Optional zero-based position of the repeated entry the remark refers to. */
    private Integer entryIndex;

    /** Optional best-effort id of the repeated entry the remark refers to. */
    private Long entryId;

    /** Optional JSON snapshot of the repeated entry as reviewed. */
    private String entrySnapshot;
}
