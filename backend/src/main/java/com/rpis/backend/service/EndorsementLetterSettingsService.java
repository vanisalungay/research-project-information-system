package com.rpis.backend.service;

import com.rpis.backend.model.EndorsementLetterSettings;
import com.rpis.backend.repository.EndorsementLetterSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The institutional facts the official endorsement letter is addressed with.
 *
 * <p>They live in one administrative settings row instead of being hardcoded. The row
 * is created on first read, seeded from the {@code rpis.endorsement.*} properties of a
 * deployment when they are set, and falls back to the official defaults otherwise.
 *
 * <p><b>The ORPS Director is no longer part of the workflow.</b> The settings are read
 * for the institutional wording only - the office and the university, both of which
 * have official defaults - so nothing here can block an endorsement or the issuance of
 * a letter, and the letter is addressed to the office rather than to a named person.
 * The row and any historical values are kept so existing documents keep their wording;
 * there is no screen or endpoint that edits it any more.
 */
@Service
public class EndorsementLetterSettingsService {

    private final EndorsementLetterSettingsRepository settingsRepository;

    private final String seedDirectorName;
    private final String seedDirectorSurname;
    private final String seedDirectorDesignation;
    private final String seedOfficeName;
    private final String seedUniversityName;

    public EndorsementLetterSettingsService(
            EndorsementLetterSettingsRepository settingsRepository,
            @Value("${rpis.endorsement.director-name:}") String seedDirectorName,
            @Value("${rpis.endorsement.director-surname:}") String seedDirectorSurname,
            @Value("${rpis.endorsement.director-designation:}") String seedDirectorDesignation,
            @Value("${rpis.endorsement.office-name:}") String seedOfficeName,
            @Value("${rpis.endorsement.university-name:}") String seedUniversityName) {
        this.settingsRepository = settingsRepository;
        this.seedDirectorName = seedDirectorName;
        this.seedDirectorSurname = seedDirectorSurname;
        this.seedDirectorDesignation = seedDirectorDesignation;
        this.seedOfficeName = seedOfficeName;
        this.seedUniversityName = seedUniversityName;
    }

    /**
     * The settings row, created with the official defaults when it is absent.
     *
     * <p>This is the only read path and it never fails: the values it carries are
     * wording, not a requirement, so any caller can generate a document with it.
     */
    @Transactional
    public EndorsementLetterSettings getSettings() {
        return settingsRepository.findById(EndorsementLetterSettings.SETTINGS_ID)
                .orElseGet(this::createDefaultSettings);
    }

    private EndorsementLetterSettings createDefaultSettings() {
        EndorsementLetterSettings settings = new EndorsementLetterSettings();
        settings.setId(EndorsementLetterSettings.SETTINGS_ID);
        settings.setDirectorName(blankToNull(seedDirectorName));
        settings.setDirectorSurname(blankToNull(seedDirectorSurname));
        settings.setDirectorDesignation(orDefault(seedDirectorDesignation,
                EndorsementLetterSettings.DEFAULT_DIRECTOR_DESIGNATION));
        settings.setOfficeName(orDefault(seedOfficeName,
                EndorsementLetterSettings.DEFAULT_OFFICE_NAME));
        settings.setUniversityName(orDefault(seedUniversityName,
                EndorsementLetterSettings.DEFAULT_UNIVERSITY_NAME));
        return settingsRepository.save(settings);
    }

    // The settings are read-only now. The ORPS Director is no longer configured in this
    // workflow, so there is no screen or endpoint that writes this row; the stored values
    // (and the historical letters that carry them) are preserved.

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String orDefault(String value, String fallback) {
        String trimmed = blankToNull(value);
        return trimmed == null ? fallback : trimmed;
    }
}
