package com.rpis.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * The institutional facts the official endorsement letter is addressed with. They
 * are held as a single administrative setting (row id = 1) rather than hardcoded,
 * because the Director's name and surname are not available anywhere in the
 * repository and must be supplied by an authorized administrator - inventing them
 * would put a fabricated recipient on an official university document.
 *
 * <p>The office name, the designation and the university name default to the
 * wording of the official template (and the university name is the one already used
 * throughout RPIS).
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

    /** Given name(s) of the ORPS Director the letter is addressed to. */
    @Column(name = "director_name")
    private String directorName;

    /** Surname of the ORPS Director, used in the salutation ("Dear Director <Surname>:"). */
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

    /** True when every field the official letter needs is present. */
    @Transient
    public boolean isComplete() {
        return directorName != null && !directorName.isBlank()
                && directorSurname != null && !directorSurname.isBlank();
    }
}
