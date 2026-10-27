package com.rpis.backend.repository;

import com.rpis.backend.model.ProposalAnnouncementUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProposalAnnouncementUpdateRepository
        extends JpaRepository<ProposalAnnouncementUpdate, Long> {

    /**
     * The recorded changes of one Call for Proposal, oldest first (chronological
     * history): by announcement, then by when it was recorded so several updates made
     * on the same day still keep their real order.
     */
    List<ProposalAnnouncementUpdate> findByAnnouncementIdOrderByCreatedAtAscIdAsc(Long announcementId);
}
