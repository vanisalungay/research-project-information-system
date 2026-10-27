package com.rpis.backend.dto;

import lombok.Data;

/** Administrative payload for the institutional facts printed on the letter. */
@Data
public class EndorsementLetterSettingsRequest {

    /** Given name(s) of the ORPS Director. Required to issue a letter. */
    private String directorName;

    /** Surname of the ORPS Director (salutation). Required to issue a letter. */
    private String directorSurname;

    /** Designation of the Director, e.g. "Director". */
    private String directorDesignation;

    /** Office the Director heads, e.g. "Office of Research and Publication Services (ORPS)". */
    private String officeName;

    /** Name of the university. */
    private String universityName;
}
