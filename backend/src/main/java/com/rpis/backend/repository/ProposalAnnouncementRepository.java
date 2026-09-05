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
     * Overlap detection. Two date ranges overlap when
     * existing.startDate <= new.endDate AND existing.endDate >= new.startDate.
     */
    List<ProposalAnnouncement> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate endDate, LocalDate startDate);

    /**
     * Same overlap detection but excluding a given announcement (used on updates so an
     * announcement does not conflict with its own existing range).
     */
    List<ProposalAnnouncement> findByIdNotAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long id, LocalDate endDate, LocalDate startDate);

    /**
     * The announcement currently accepting submissions: status is ACTIVE and today falls
     * within [startDate, endDate] (inclusive on both ends).
     */
    Optional<ProposalAnnouncement> findFirstByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
            String status, LocalDate endDate, LocalDate startDate);
}
