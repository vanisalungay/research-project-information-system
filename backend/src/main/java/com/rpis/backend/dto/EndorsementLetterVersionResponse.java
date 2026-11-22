package com.rpis.backend.dto;

import com.rpis.backend.model.EndorsementLetterVersion;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * One preserved endorsement letter version, as the Dean's review screen lists it. The
 * current version is the one filed in the Proponent's Certifications; older versions are
 * kept for the record and remain viewable / downloadable.
 */
@Data
public class EndorsementLetterVersionResponse {

    private Long id;
    private Integer version;
    private String documentCode;
    private String fileName;
    private LocalDateTime generatedAt;
    private String generatedByName;
    private boolean current;

    public static EndorsementLetterVersionResponse of(EndorsementLetterVersion version,
            boolean current) {
        EndorsementLetterVersionResponse response = new EndorsementLetterVersionResponse();
        response.id = version.getId();
        response.version = version.getVersionNumber();
        response.documentCode = version.getDocumentCode();
        response.fileName = version.getFileName();
        response.generatedAt = version.getGeneratedAt();
        response.generatedByName = version.getGeneratedByName();
        response.current = current;
        return response;
    }
}
