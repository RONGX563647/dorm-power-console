package com.dormpower.repository;

import com.dormpower.model.SystemMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统监控指标仓库接口
 */
@Repository
public interface SystemMetricsRepository extends JpaRepository<SystemMetrics, Long> {

    List<SystemMetrics> findByMetricTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
            String metricType, LocalDateTime start, LocalDateTime end);

    List<SystemMetrics> findByMetricTypeOrderByCreatedAtDesc(String metricType);

    @Query("SELECT m FROM SystemMetrics m WHERE m.metricType = :type ORDER BY m.createdAt DESC")
    List<SystemMetrics> findLatestByType(@Param("type") String type, org.springframework.data.domain.Pageable pageable);

    void deleteByCreatedAtBefore(LocalDateTime date);

    @Query("SELECT AVG(m.metricValue) FROM SystemMetrics m WHERE m.metricType = :type AND m.createdAt >= :start")
    Double calculateAverage(@Param("type") String type, @Param("start") LocalDateTime start);

    @Query("SELECT MAX(m.metricValue) FROM SystemMetrics m WHERE m.metricType = :type AND m.createdAt >= :start")
    Double findMaxValue(@Param("type") String type, @Param("start") LocalDateTime start);
}
