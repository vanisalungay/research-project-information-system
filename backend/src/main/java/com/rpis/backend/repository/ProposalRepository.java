package com.rpis.backend.repository;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findByProponent(User proponent);

    List<Proposal> findByStatus(String status);

    @Query("SELECT p FROM Proposal p WHERE p.status IN :statuses")
    List<Proposal> findByStatusIn(@Param("statuses") List<String> statuses);
}