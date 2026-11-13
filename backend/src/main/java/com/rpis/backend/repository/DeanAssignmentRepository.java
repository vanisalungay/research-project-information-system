package com.rpis.backend.repository;

import com.rpis.backend.model.DeanAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Read/write access to the Dean appointment history.
 *
 * <p>Nothing here deletes a row: an appointment is closed by flipping its
 * {@code status} to {@code REVOKED} and stamping the revocation, so the previous Dean's
 * record survives as history.
 */
@Repository
public interface DeanAssignmentRepository extends JpaRepository<DeanAssignment, Long> {

    /** Every appointment, newest first (the "All" view of the Dean Role Management page). */
    List<DeanAssignment> findAllByOrderByDateAssignedDesc();

    /** The appointments in one status, newest first. */
    List<DeanAssignment> findByStatusOrderByDateAssignedDesc(String status);

    /** One account's appointments, newest first. */
    List<DeanAssignment> findByUserIdOrderByDateAssignedDesc(Long userId);

    /** One account's appointments in a given status (used to find its ACTIVE one). */
    List<DeanAssignment> findByUserIdAndStatus(Long userId, String status);

    /** The appointments of one college in a given status, case-insensitively on the college. */
    List<DeanAssignment> findByStatusAndAssignedCollegeIgnoreCase(String status, String college);

    /** The appointments with no college configured (institution-wide fallback). */
    List<DeanAssignment> findByStatusAndAssignedCollegeIsNull(String status);

    /** How many appointments an account holds in a status (0 or 1 for ACTIVE). */
    long countByUserIdAndStatus(Long userId, String status);
}
