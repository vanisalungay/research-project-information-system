package com.rpis.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * The institutional wording of the official endorsement letter: the office and the
 * university it is addressed with. They are held as a single administrative setting
 * (row id = 1) rather than hardcoded, and default to the official wording.
 *
 * <p><b>The ORPS Director is no longer part of the workflow.</b> The letter is addressed
 * to the office, so the Director's name and surname are not required by anything: the
 * fields below are legacy data that is preserved (nothing is dropped from the database),
 * and no screen, endpoint or validation asks for them any more.
 */
@Entity
@Table(name = "endorsement_letter_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndorsementLetterSettings {

    /** The single settings row. */
    public static final Long SETTINGS_ID = 1L;

    public static final String DEFAULT_DIRECTOR_DESIGNATION = "Director";
    public static final String DEFAULT_OFFICE_NAME =
            "Office of Research and Publication Services (ORPS)";
    public static final String DEFAULT_UNIVERSITY_NAME = "Mindanao State University at Naawan";

    @Id
    private Long id = SETTINGS_ID;

    /**
     * Given name(s) of the ORPS Director. Legacy: nothing reads this any more (the letter
     * is addressed to the office). The column and its stored values are kept as they are.
     */
    @Column(name = "director_name")
    private String directorName;

    /**
     * Surname of the ORPS Director, formerly used in the salutation. Legacy, like
     * {@link #directorName}: preserved, never required.
     */
    @Column(name = "director_surname")
    private String directorSurname;

    @Column(name = "director_designation", nullable = false)
    private String directorDesignation = DEFAULT_DIRECTOR_DESIGNATION;

    @Column(name = "office_name", nullable = false)
    private String officeName = DEFAULT_OFFICE_NAME;

    @Column(name = "university_name", nullable = false)
    private String universityName = DEFAULT_UNIVERSITY_NAME;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(name = "updated_by_name")
    private String updatedByName;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // The "is the Director configured?" check is gone with the ORPS Director requirement:
    // the letter is addressed to the office, so nothing here has to be complete before a
    // document can be issued.
}
