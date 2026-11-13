package com.rpis.backend.repository;

import com.rpis.backend.model.PriorityAgendaSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriorityAgendaSetRepository extends JpaRepository<PriorityAgendaSet, Long> {

    /**
     * The set new proposals select their agendas from, if any. The database
     * guarantees there can never be more than one (partial unique index
     * {@code uk_priority_agenda_set_single_active}), so a single optional is exact.
     */
    Optional<PriorityAgendaSet> findFirstByActiveTrue();

    /**
     * The full set list for the admin management page: the active set first, then the
     * newest coverage period, then the most recently created one, so the table order
     * is stable and predictable.
     */
    List<PriorityAgendaSet> findAllByOrderByActiveDescCoverageStartYearDescIdDesc();

    /** Duplicate check for a framework name + coverage period (create). */
    boolean existsByFrameworkNameIgnoreCaseAndCoverageStartYearAndCoverageEndYear(
            String frameworkName, Integer coverageStartYear, Integer coverageEndYear);

    /** Duplicate check for a framework name + coverage period (update, excluding self). */
    boolean existsByFrameworkNameIgnoreCaseAndCoverageStartYearAndCoverageEndYearAndIdNot(
            String frameworkName, Integer coverageStartYear, Integer coverageEndYear, Long id);
}
