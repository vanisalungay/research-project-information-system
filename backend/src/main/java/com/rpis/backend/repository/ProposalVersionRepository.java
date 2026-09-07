package com.rpis.backend.repository;

import com.rpis.backend.model.ProposalVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProposalVersionRepository extends JpaRepository<ProposalVersion, Long> {

    List<ProposalVersion> findByProposalIdOrderByVersionNumberAsc(Long proposalId);

    Optional<ProposalVersion> findTopByProposalIdOrderByVersionNumberDesc(Long proposalId);

    Optional<ProposalVersion> findByProposalIdAndVersionNumber(Long proposalId, Integer versionNumber);

    boolean existsByProposalId(Long proposalId);
}
