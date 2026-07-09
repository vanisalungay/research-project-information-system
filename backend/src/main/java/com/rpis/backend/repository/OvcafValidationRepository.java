package com.rpis.backend.repository;

import com.rpis.backend.model.OvcafValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OvcafValidationRepository extends JpaRepository<OvcafValidation, Long> {

    List<OvcafValidation> findByProposalId(Long proposalId);

    Optional<OvcafValidation> findTopByProposalIdOrderByCreatedAtDesc(Long proposalId);

    List<OvcafValidation> findByDecision(String decision);

    List<OvcafValidation> findByValidatedById(Long validatedById);

    @Query("SELECT ov FROM OvcafValidation ov WHERE ov.budgetEndorsed = true")
    List<OvcafValidation> findEndorsedValidations();

    @Query("SELECT ov FROM OvcafValidation ov WHERE ov.budgetAuthorized = true")
    List<OvcafValidation> findAuthorizedValidations();

    @Query("SELECT ov FROM OvcafValidation ov WHERE ov.fundsReleased = true")
    List<OvcafValidation> findReleasedFunds();

    @Query("SELECT COUNT(ov) FROM OvcafValidation ov WHERE ov.decision = :decision")
    long countByDecision(@Param("decision") String decision);

    @Query("SELECT COUNT(ov) FROM OvcafValidation ov WHERE ov.budgetEndorsed = true")
    long countEndorsed();

    @Query("SELECT COUNT(ov) FROM OvcafValidation ov WHERE ov.budgetAuthorized = true")
    long countAuthorized();

    @Query("SELECT COUNT(ov) FROM OvcafValidation ov WHERE ov.fundsReleased = true")
    long countReleased();
}