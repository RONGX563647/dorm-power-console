package com.dormpower.repository;

import com.dormpower.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUsernameOrderByTsDesc(String username, Pageable pageable);

    Page<AuditLog> findByModuleOrderByTsDesc(String module, Pageable pageable);

    Page<AuditLog> findByActionOrderByTsDesc(String action, Pageable pageable);

    Page<AuditLog> findByTargetIdOrderByTsDesc(String targetId, Pageable pageable);

    Page<AuditLog> findByStatusOrderByTsDesc(String status, Pageable pageable);

    Page<AuditLog> findAllByOrderByTsDesc(Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.ts BETWEEN :startTime AND :endTime ORDER BY a.ts DESC")
    Page<AuditLog> findByTimeRange(long startTime, long endTime, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.username = :username AND a.ts BETWEEN :startTime AND :endTime ORDER BY a.ts DESC")
    Page<AuditLog> findByUsernameAndTimeRange(String username, long startTime, long endTime, Pageable pageable);

    @Query("SELECT a.module, COUNT(a) as cnt FROM AuditLog a WHERE a.ts > :since GROUP BY a.module ORDER BY cnt DESC")
    List<Object[]> countByModuleSince(long since);

    @Query("SELECT a.action, COUNT(a) as cnt FROM AuditLog a WHERE a.module = :module AND a.ts > :since GROUP BY a.action ORDER BY cnt DESC")
    List<Object[]> countByActionInModuleSince(String module, long since);

    @Query("SELECT a.username, COUNT(a) as cnt FROM AuditLog a WHERE a.ts > :since GROUP BY a.username ORDER BY cnt DESC")
    List<Object[]> findTopActiveUsers(long since, Pageable pageable);

    long deleteByTsBefore(long ts);
}
