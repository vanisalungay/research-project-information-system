package com.rpis.backend.dto;

import lombok.Data;

/**
 * Payload for saving (creating or updating) a single field-level remark from a
 * reviewer page. One request = one field, so a reviewer can persist a remark
 * beside the field without leaving the page.
 * <p>
 * {@code fieldKey} is a stable data path, never a display label, e.g.
 * {@code projectTitle}, {@code personnel[1]} (a whole repeated entry) or
 * {@code personnel[1].time} (a cell of a repeated entry). Leaving it blank stores
 * a section-level remark, which preserves the original behaviour.
 */
@Data
public class FieldRemarkRequest {

    /** Canonical section key, e.g. "01". Required. */
    private String sectionKey;

    /** Stable field path inside the section. Blank = section-level remark. */
    private String fieldKey;

    /** Human-readable field label (the server resolves it when omitted). */
    private String fieldLabel;

    /** Zero-based position of the repeated entry the remark refers to. */
    private Integer entryIndex;

    /** Best-effort id of the repeated entry the remark refers to. */
    private Long entryId;

    /** JSON snapshot of the repeated entry as reviewed. */
    private String entrySnapshot;

    /** The reviewer's remark. Required and non-blank. */
    private String remark;
}
