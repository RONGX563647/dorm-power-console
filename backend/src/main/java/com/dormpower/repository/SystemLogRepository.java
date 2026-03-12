package com.dormpower.repository;

import com.dormpower.model.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统日志仓库接口
 */
@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    Page<SystemLog> findByLevel(String level, Pageable pageable);

    Page<SystemLog> findByType(String type, Pageable pageable);

    Page<SystemLog> findByUsername(String username, Pageable pageable);

    @Query("SELECT l FROM SystemLog l WHERE l.createdAt BETWEEN :start AND :end")
    Page<SystemLog> findByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT l FROM SystemLog l WHERE l.level = :level AND l.createdAt BETWEEN :start AND :end")
    Page<SystemLog> findByLevelAndTimeRange(@Param("level") String level, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT l FROM SystemLog l WHERE l.type = :type AND l.createdAt BETWEEN :start AND :end")
    Page<SystemLog> findByTypeAndTimeRange(@Param("type") String type, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT l FROM SystemLog l WHERE l.message LIKE %:keyword% OR l.details LIKE %:keyword%")
    Page<SystemLog> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    void deleteByCreatedAtBefore(LocalDateTime date);

    @Query("SELECT l.type, COUNT(l) FROM SystemLog l WHERE l.createdAt >= :start GROUP BY l.type")
    List<Object[]> countByTypeSince(@Param("start") LocalDateTime start);

    @Query("SELECT l.level, COUNT(l) FROM SystemLog l WHERE l.createdAt >= :start GROUP BY l.level")
    List<Object[]> countByLevelSince(@Param("start") LocalDateTime start);
}
