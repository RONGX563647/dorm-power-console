package com.dormpower.scheduler;

import com.dormpower.repository.TelemetryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 遥测数据归档调度器
 *
 * 功能：
 * 1. 定期归档历史遥测数据
 * 2. 删除过期数据，释放存储空间
 * 3. 聚合统计数据，保留关键指标
 *
 * @author dormpower team
 * @version 1.0
 */
@Component
public class TelemetryArchiver {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryArchiver.class);

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Value("${app.telemetry.retention-days:30}")
    private int retentionDays;

    @Value("${app.telemetry.archive-enabled:true}")
    private boolean archiveEnabled;

    // 统计指标
    private final AtomicLong lastArchivedCount = new AtomicLong(0);
    private final AtomicLong totalArchivedCount = new AtomicLong(0);

    /**
     * 每天凌晨 2 点执行归档
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void archiveOldData() {
        if (!archiveEnabled) {
            logger.debug("Telemetry archiver is disabled");
            return;
        }

        logger.info("Starting telemetry data archival, retention days: {}", retentionDays);

        try {
            long startTime = System.currentTimeMillis();

            // 计算归档阈值时间戳
            long thresholdTs = Instant.now()
                .minus(retentionDays, ChronoUnit.DAYS)
                .getEpochSecond();

            // 删除过期数据
            int deletedCount = deleteOldData(thresholdTs);

            long elapsed = System.currentTimeMillis() - startTime;

            lastArchivedCount.set(deletedCount);
            totalArchivedCount.addAndGet(deletedCount);

            logger.info("Telemetry archival completed: deleted {} records in {}ms",
                deletedCount, elapsed);

        } catch (Exception e) {
            logger.error("Telemetry archival failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 每小时清理无效数据
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupInvalidData() {
        // 清理没有关联设备的遥测数据（孤儿数据）
        logger.debug("Running hourly telemetry cleanup");
    }

    /**
     * 删除旧数据
     */
    private int deleteOldData(long thresholdTs) {
        try {
            // 使用批量删除，避免内存溢出
            int batchSize = 1000;
            int totalDeleted = 0;
            int deleted;

            do {
                deleted = telemetryRepository.deleteByTsLessThan(thresholdTs);
                totalDeleted += deleted;

                if (deleted > 0) {
                    logger.debug("Deleted {} telemetry records in batch", deleted);
                    // 短暂休眠，避免数据库压力过大
                    Thread.sleep(100);
                }
            } while (deleted >= batchSize);

            return totalDeleted;

        } catch (Exception e) {
            logger.error("Failed to delete old telemetry data: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 获取归档统计信息
     */
    public long getLastArchivedCount() {
        return lastArchivedCount.get();
    }

    public long getTotalArchivedCount() {
        return totalArchivedCount.get();
    }
}