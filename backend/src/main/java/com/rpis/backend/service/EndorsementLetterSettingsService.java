package com.rpis.backend.service;

import com.rpis.backend.dto.EndorsementLetterSettingsRequest;
import com.rpis.backend.dto.EndorsementLetterSettingsResponse;
import com.rpis.backend.model.EndorsementLetterSettings;
import com.rpis.backend.repository.EndorsementLetterSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * The institutional facts the official endorsement letter is addressed with.
 *
 * <p>They live in one administrative settings row instead of being hardcoded,
 * because the ORPS Director's name and surname are not available anywhere in the
 * repository: inventing them would put a fabricated recipient on an official
 * university document. The row is created on first read (optionally seeded from the
 * {@code rpis.endorsement.*} properties of a deployment) and an RPS_ADMIN maintains
 * it. {@link #requireComplete()} refuses to issue a letter while the Director is not
 * configured.
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

    /** The settings row, created with the documented defaults when absent. */
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

    /** Saves the settings. Only an RPS_ADMIN reaches this (see SecurityConfig). */
    @Transactional
    public EndorsementLetterSettings update(EndorsementLetterSettingsRequest request,
            Long actorId, String actorName) {
        EndorsementLetterSettings settings = getSettings();

        settings.setDirectorName(blankToNull(request.getDirectorName()));
        settings.setDirectorSurname(blankToNull(request.getDirectorSurname()));
        settings.setDirectorDesignation(orDefault(request.getDirectorDesignation(),
                EndorsementLetterSettings.DEFAULT_DIRECTOR_DESIGNATION));
        settings.setOfficeName(orDefault(request.getOfficeName(),
                EndorsementLetterSettings.DEFAULT_OFFICE_NAME));
        settings.setUniversityName(orDefault(request.getUniversityName(),
                EndorsementLetterSettings.DEFAULT_UNIVERSITY_NAME));
        settings.setUpdatedByUserId(actorId);
        settings.setUpdatedByName(actorName);
        settings.setUpdatedAt(LocalDateTime.now());

        return settingsRepository.save(settings);
    }

    /**
     * The settings, or an explicit failure when the official letter cannot be
     * addressed yet. Called before any letter is generated.
     */
    @Transactional
    public EndorsementLetterSettings requireComplete() {
        EndorsementLetterSettings settings = getSettings();
        if (!settings.isComplete()) {
            throw new IllegalStateException(
                    "The Office of Research and Publication Services (ORPS) Director is not "
                            + "configured yet, so the official endorsement letter cannot be issued. "
                            + "An administrator must supply the Director's name and surname in "
                            + "Endorsement Letter Settings.");
        }
        return settings;
    }

    public EndorsementLetterSettingsResponse toResponse(EndorsementLetterSettings settings) {
        EndorsementLetterSettingsResponse response = new EndorsementLetterSettingsResponse();
        response.setDirectorName(settings.getDirectorName());
        response.setDirectorSurname(settings.getDirectorSurname());
        response.setDirectorDesignation(settings.getDirectorDesignation());
        response.setOfficeName(settings.getOfficeName());
        response.setUniversityName(settings.getUniversityName());
        response.setComplete(settings.isComplete());
        response.setUpdatedByName(settings.getUpdatedByName());
        response.setUpdatedAt(settings.getUpdatedAt());
        return response;
    }

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
