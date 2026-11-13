package com.rpis.backend.repository;

import com.rpis.backend.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for the per-administrator activity history.
 *
 * <p>Every finder is keyed by {@code userId}, which the service takes from the
 * authenticated principal - there is deliberately no "find everything" query the
 * page could reach, so one administrator can never read another's history.
 * There is no update or delete method either: the table is append-only.
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long>,
        JpaSpecificationExecutor<ActivityLog> {

    /** One administrator's entries, newest first (the page's default order). */
    @Query("select a from ActivityLog a where a.userId = :userId "
            + "order by a.createdAt desc, a.id desc")
    List<ActivityLog> findForUser(@Param("userId") Long userId);

    /** A single entry, and only when it belongs to the given administrator. */
    Optional<ActivityLog> findByIdAndUserId(Long id, Long userId);

    /** The modules this administrator actually has entries for, for the filter list. */
    @Query("select distinct a.module from ActivityLog a where a.userId = :userId "
            + "order by a.module asc")
    List<String> findDistinctModulesForUser(@Param("userId") Long userId);
}
