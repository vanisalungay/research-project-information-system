package com.rpis.backend.repository;

import com.rpis.backend.model.ProposalAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProposalAnnouncementRepository extends JpaRepository<ProposalAnnouncement, Long> {

    List<ProposalAnnouncement> findByStatus(String status);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * The announcement currently accepting submissions: status is ACTIVE and today falls
     * within [startDate, endDate] (inclusive on both ends).
     */
    Optional<ProposalAnnouncement> findFirstByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
            String status, LocalDate endDate, LocalDate startDate);

    /**
     * Every Call for Proposal currently accepting submissions - the same rule as the
     * single active announcement, but the whole set, because several submission
     * windows may overlap. Newest window first, then the most recently created one,
     * so the proponent's list is stable and predictable.
     */
    List<ProposalAnnouncement> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDescIdDesc(
            String status, LocalDate endDate, LocalDate startDate);

    /**
     * The published calls that have not passed their deadline yet: open for
     * submissions today or opening later (scheduled). Calls whose deadline is before
     * {@code today} are never returned, so an expired call can neither be offered as
     * open nor as upcoming. The order here is only a stable base - the service puts
     * the open calls first and the upcoming ones by opening date.
     */
    List<ProposalAnnouncement> findByStatusAndEndDateGreaterThanEqualOrderByStartDateAscIdAsc(
            String status, LocalDate today);

    /**
     * Full announcement list for the ORPS Proposal Announcements page, newest first:
     * by Start Date descending, then by creation order (most recently created first)
     * so that announcements sharing the same Start Date keep a stable, predictable
     * order instead of relying on an arbitrary database row order.
     */
    List<ProposalAnnouncement> findAllByOrderByStartDateDescCreatedAtDescIdDesc();
}
