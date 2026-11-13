package com.rpis.backend.repository;

import com.rpis.backend.model.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {

    /** The Proponent's own certification documents, newest first. */
    List<Certification> findByProponentIdOrderByCreatedAtDesc(Long proponentId);

    /**
     * The certification issued for an endorsement request. Combined with the unique
     * constraint on the column this is what makes issuance idempotent: a retried
     * approval finds the existing entry instead of creating a second one.
     */
    Optional<Certification> findByEndorsementRequestId(Long endorsementRequestId);

    boolean existsByEndorsementRequestId(Long endorsementRequestId);
}
