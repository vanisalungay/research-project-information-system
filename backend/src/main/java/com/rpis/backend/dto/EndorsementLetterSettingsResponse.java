package com.rpis.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** Read model of the endorsement letter settings. */
@Data
public class EndorsementLetterSettingsResponse {

    private String directorName;
    private String directorSurname;
    private String directorDesignation;
    private String officeName;
    private String universityName;

    /** False while the Director's name/surname are missing, which blocks issuance. */
    private boolean complete;

    private String updatedByName;
    private LocalDateTime updatedAt;
}
