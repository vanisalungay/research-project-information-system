package com.rpis.backend.repository;

import com.rpis.backend.model.EndorsementLetterSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EndorsementLetterSettingsRepository extends JpaRepository<EndorsementLetterSettings, Long> {
}
