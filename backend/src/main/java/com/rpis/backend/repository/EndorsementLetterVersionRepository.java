package com.rpis.backend.repository;

import com.rpis.backend.model.EndorsementLetterVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EndorsementLetterVersionRepository
        extends JpaRepository<EndorsementLetterVersion, Long> {

    /** Every preserved version of one request, newest first. */
    List<EndorsementLetterVersion> findByEndorsementRequestIdOrderByVersionNumberDesc(
            Long endorsementRequestId);

    Optional<EndorsementLetterVersion> findByEndorsementRequestIdAndVersionNumber(
            Long endorsementRequestId, Integer versionNumber);

    /** One version, only when it belongs to the request named in the route. */
    Optional<EndorsementLetterVersion> findByIdAndEndorsementRequestId(
            Long id, Long endorsementRequestId);
}
