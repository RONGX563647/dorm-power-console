package com.dormpower.repository;

import com.dormpower.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 审计日志仓库
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 按用户 ID 查询审计日志
     */
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    /**
     * 按操作类型查询审计日志
     */
    Page<AuditLog> findByAction(String action, Pageable pageable);

    /**
     * 按资源查询审计日志
     */
    Page<AuditLog> findByResourceContaining(String resource, Pageable pageable);

    /**
     * 按时间范围查询审计日志
     */
    Page<AuditLog> findByTimestampBetween(Instant start, Instant end, Pageable pageable);

    /**
     * 按用户和操作类型查询
     */
    Page<AuditLog> findByUserIdAndAction(Long userId, String action, Pageable pageable);

    /**
     * 查询最近的操作日志
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId ORDER BY a.timestamp DESC")
    Page<AuditLog> findRecentLogs(@Param("userId") Long userId, Pageable pageable);

    /**
     * 统计指定时间范围内的操作次数
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.timestamp BETWEEN :start AND :end")
    long countByTimestampBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 按操作类型统计
     */
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a WHERE a.timestamp BETWEEN :start AND :end GROUP BY a.action")
    List<Object[]> countByActionBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 删除指定时间之前的日志 (用于日志清理)
     */
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :timestamp")
    int deleteByTimestampBefore(@Param("timestamp") Instant timestamp);
}
