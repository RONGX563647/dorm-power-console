package com.dormpower.repository;

import com.dormpower.model.Telemetry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 遥测数据批量写入仓库
 *
 * 使用 JPA EntityManager 实现批量插入，
 * 显著提高大量数据写入性能。
 *
 * 性能优化：
 * 1. 批量插入（每 50 条刷新一次）
 * 2. 清除持久化上下文，避免内存溢出
 * 3. 使用事务保证原子性
 *
 * @author dormpower team
 * @version 1.0
 */
@Repository
public class TelemetryBulkRepository {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryBulkRepository.class);

    /**
     * 批量插入大小
     */
    private static final int BATCH_SIZE = 50;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 批量插入遥测数据
     *
     * @param telemetryList 遥测数据列表
     */
    @Transactional
    public void batchInsert(List<Telemetry> telemetryList) {
        if (telemetryList == null || telemetryList.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        int count = 0;

        for (Telemetry telemetry : telemetryList) {
            entityManager.persist(telemetry);
            count++;

            // 每 BATCH_SIZE 条刷新一次
            if (count % BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
                logger.debug("Flushed {} telemetry records", count);
            }
        }

        // 最后刷新剩余数据
        entityManager.flush();
        entityManager.clear();

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("Batch inserted {} telemetry records in {}ms", count, elapsed);
    }

    /**
     * 批量插入遥测数据（带自定义批次大小）
     *
     * @param telemetryList 遥测数据列表
     * @param batchSize 批次大小
     */
    @Transactional
    public void batchInsert(List<Telemetry> telemetryList, int batchSize) {
        if (telemetryList == null || telemetryList.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        int count = 0;

        for (Telemetry telemetry : telemetryList) {
            entityManager.persist(telemetry);
            count++;

            if (count % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        entityManager.flush();
        entityManager.clear();

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("Batch inserted {} telemetry records with batch size {} in {}ms", count, batchSize, elapsed);
    }

    /**
     * 批量更新设备最后在线时间
     *
     * @param deviceIds 设备 ID 列表
     * @param timestamp 时间戳
     */
    @Transactional
    public void batchUpdateLastSeen(List<String> deviceIds, long timestamp) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return;
        }

        String jpql = "UPDATE Device d SET d.lastSeenTs = :ts, d.online = true WHERE d.id IN :ids";
        int updated = entityManager.createQuery(jpql)
            .setParameter("ts", timestamp)
            .setParameter("ids", deviceIds)
            .executeUpdate();

        logger.debug("Updated last seen time for {} devices", updated);
    }
}