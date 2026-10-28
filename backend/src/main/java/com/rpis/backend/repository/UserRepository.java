package com.rpis.backend.repository;

import com.rpis.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    /**
     * The accounts in any of the given roles.
     *
     * <p>Exists so that a proposal status change can fetch only the accounts it
     * actually notifies, in one query. The previous implementation loaded every user
     * in the system for every status change (see
     * {@code ProposalService.notifySubscribedRoles}), which made the cost of a
     * submission grow with the size of the whole user table. Backed by
     * {@code idx_users_role} (see migration-add-performance-indexes.sql).
     */
    List<User> findByRoleIn(List<String> roles);

    /**
     * Counts the accounts whose stored password is still plain text.
     *
     * <p>This is the database-side equivalent of {@code PasswordService.isHashed}
     * negated, and it exists so the size of the legacy migration can be measured
     * without loading the table. The previous implementation fetched every account with
     * {@code findAll()} and filtered in memory, which made the count cost as much memory
     * as the table it was counting.
     *
     * <p>The regular expression is deliberately the same as
     * {@code PasswordService.BCRYPT_HASH}, and {@code PasswordPlainTextCountTest} asserts
     * that the two agree, so the query and the migration cannot drift apart.
     *
     * <p>{@code !=} is used rather than {@code &lt;&gt;} only to keep the statement free
     * of angle brackets; PostgreSQL treats the two identically.
     */
    @Query(value = "SELECT COUNT(*) FROM users "
            + "WHERE password IS NOT NULL "
            + "AND btrim(password) != '' "
            + "AND password !~ '^\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}$'",
            nativeQuery = true)
    long countPlainTextPasswords();
}
