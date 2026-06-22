package com.rpis.backend.repository;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProposalReviewRepository extends JpaRepository<ProposalReview, Long> {
    List<ProposalReview> findByProposal(Proposal proposal);
}
