package com.rpis.backend.repository;

import com.rpis.backend.model.EndorsementRequestEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EndorsementRequestEventRepository extends JpaRepository<EndorsementRequestEvent, Long> {

    /** The audit trail of one request, oldest first. */
    List<EndorsementRequestEvent> findByEndorsementRequestIdOrderByCreatedAtAsc(Long endorsementRequestId);
}
