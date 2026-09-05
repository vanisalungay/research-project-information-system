package com.rpis.backend.repository;

import com.rpis.backend.model.ApplicationCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationCycleRepository extends JpaRepository<ApplicationCycle, Long> {

    List<ApplicationCycle> findByStatus(String status);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Overlap detection. Two date ranges overlap when
     * existing.startDate <= new.endDate AND existing.endDate >= new.startDate.
     */
    List<ApplicationCycle> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate endDate, LocalDate startDate);

    /**
     * Same overlap detection but excluding a given cycle (used on updates so a
     * cycle does not conflict with its own existing range).
     */
    List<ApplicationCycle> findByIdNotAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long id, LocalDate endDate, LocalDate startDate);

    /**
     * The cycle currently accepting submissions: status is ACTIVE and today falls
     * within [startDate, endDate] (inclusive on both ends).
     */
    Optional<ApplicationCycle> findFirstByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
            String status, LocalDate endDate, LocalDate startDate);
}
