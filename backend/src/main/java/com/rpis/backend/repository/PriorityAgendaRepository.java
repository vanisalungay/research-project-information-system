package com.rpis.backend.repository;

import com.rpis.backend.model.PriorityAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriorityAgendaRepository extends JpaRepository<PriorityAgenda, Long> {

    /** Every item of a set, in display order (the admin management list). */
    List<PriorityAgenda> findByAgendaSetIdOrderByDisplayOrderAscIdAsc(Long agendaSetId);

    /** Only the items new proposals may select, in display order (the proponent form). */
    List<PriorityAgenda> findByAgendaSetIdAndActiveTrueOrderByDisplayOrderAscIdAsc(Long agendaSetId);

    /**
     * Resolves the display metadata of arbitrary keys, including the keys of a
     * deactivated set or item, so a historical proposal keeps showing the agenda name
     * it was submitted under.
     */
    List<PriorityAgenda> findByAgendaKeyInIgnoreCase(Collection<String> agendaKeys);

    /** Duplicate check for the stable key inside one set (create). */
    boolean existsByAgendaSetIdAndAgendaKeyIgnoreCase(Long agendaSetId, String agendaKey);

    /** Duplicate check for the display name inside one set (create). */
    boolean existsByAgendaSetIdAndNameIgnoreCase(Long agendaSetId, String name);

    /** Duplicate check for the display name inside one set (update, excluding self). */
    boolean existsByAgendaSetIdAndNameIgnoreCaseAndIdNot(Long agendaSetId, String name, Long id);

    /** Used to derive a unique key when the administrator does not supply one. */
    Optional<PriorityAgenda> findFirstByAgendaSetIdAndAgendaKeyIgnoreCase(Long agendaSetId, String agendaKey);
}
