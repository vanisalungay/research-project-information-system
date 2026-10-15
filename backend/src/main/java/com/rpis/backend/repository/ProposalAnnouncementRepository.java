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
     * Full announcement list for the ORPS Proposal Announcements page, newest first:
     * by Start Date descending, then by creation order (most recently created first)
     * so that announcements sharing the same Start Date keep a stable, predictable
     * order instead of relying on an arbitrary database row order.
     */
    List<ProposalAnnouncement> findAllByOrderByStartDateDescCreatedAtDescIdDesc();
}
