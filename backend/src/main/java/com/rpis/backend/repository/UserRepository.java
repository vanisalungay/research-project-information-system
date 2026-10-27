package com.rpis.backend.repository;

import com.rpis.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByStatus(String status);

    List<User> findByRoleAndStatus(String role, String status);

    /**
     * The Dean accounts of one college, by name. Used to route an endorsement
     * request to the Dean authorized for its college.
     */
    List<User> findByRoleAndAssignedCollegeIgnoreCaseAndStatusOrderByNameAsc(
            String role, String assignedCollege, String status);

    /** Dean accounts with no college configured yet (institution-wide fallback). */
    List<User> findByRoleAndAssignedCollegeIsNullAndStatusOrderByNameAsc(String role, String status);

    /** Every Dean account, by name. */
    List<User> findByRoleOrderByNameAsc(String role);
}
