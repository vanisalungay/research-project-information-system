package com.rpis.backend.repository;

import com.rpis.backend.model.CooperatingAgency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CooperatingAgencyRepository extends JpaRepository<CooperatingAgency, Long> {

    /**
     * The whole directory for the Create Proposal checklist, in the order it is
     * shown. Ordering by the lower-cased name keeps the seeded agencies in their
     * documented order on every database collation (a plain name order would put
     * "Department of the Interior ..." after "Department of Trade and Industry"
     * under a byte-wise collation) and slots a custom agency in alphabetically.
     */
    @Query("SELECT a FROM CooperatingAgency a ORDER BY LOWER(a.name) ASC, a.id ASC")
    List<CooperatingAgency> findAllOrdered();

    /** Case-insensitive lookup used to reject a duplicate agency name. */
    Optional<CooperatingAgency> findFirstByNameIgnoreCase(String name);
}
