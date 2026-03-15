package com.dormpower.scheduler;

import com.dormpower.kafka.SystemTaskProducer;
import com.dormpower.service.MonitoringService;
import com.dormpower.service.SystemConfigService;
import com.dormpower.service.DataBackupService;
import com.dormpower.service.SystemLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 系统监控定时任务
 *
 * v2.0 优化：
 * - 支持 Kafka 异步任务处理
 * - 解耦任务调度与任务执行
 * - 提高系统可扩展性
 *
 * @author dormpower team
 * @version 2.0
 */
@Component
public class SystemMetricsScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SystemMetricsScheduler.class);

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private DataBackupService dataBackupService;

    @Autowired
    private SystemLogService systemLogService;

    @Autowired(required = false)
    private SystemTaskProducer systemTaskProducer;

    @Value("${kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Value("${kafka.system-task.enabled:true}")
    private boolean systemTaskKafkaEnabled;

    /**
     * 每5分钟收集一次系统指标
     */
    @Scheduled(fixedRate = 300000)
    public void collectSystemMetrics() {
        String enabled = systemConfigService.getConfigValue("monitor.enabled", "true");
        if (!"true".equalsIgnoreCase(enabled)) {
            return;
        }

        // 优先使用 Kafka 异步处理
        if (kafkaEnabled && systemTaskKafkaEnabled && systemTaskProducer != null) {
            systemTaskProducer.sendMetricsCollectionTask();
            logger.debug("Metrics collection task sent to Kafka");
            return;
        }

        // 降级：直接执行
        monitoringService.collectSystemMetrics();
    }

    /**
     * 每天凌晨2点执行自动备份
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoBackup() {
        String enabled = systemConfigService.getConfigValue("backup.enabled", "true");
        if (!"true".equalsIgnoreCase(enabled)) {
            return;
        }

        int retentionDays = Integer.parseInt(systemConfigService.getConfigValue("backup.retention_days", "30"));

        // 优先使用 Kafka 异步处理
        if (kafkaEnabled && systemTaskKafkaEnabled && systemTaskProducer != null) {
            systemTaskProducer.sendBackupTask("自动备份", retentionDays);
            logger.info("Backup task sent to Kafka");
            return;
        }

        // 降级：直接执行
        try {
            dataBackupService.createDatabaseBackup("自动备份", "system");
            systemLogService.info("BACKUP", "Auto backup completed", "SystemScheduler");
        } catch (Exception e) {
            systemLogService.error("BACKUP", "Auto backup failed", "SystemScheduler", e.getMessage());
        }
    }

    /**
     * 每天凌晨3点清理过期数据
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldData() {
        int logRetentionDays = Integer.parseInt(systemConfigService.getConfigValue("log.retention_days", "30"));
        int backupRetentionDays = Integer.parseInt(systemConfigService.getConfigValue("backup.retention_days", "30"));
        int metricsRetentionDays = Integer.parseInt(systemConfigService.getConfigValue("monitor.metrics_retention_days", "7"));

        // 优先使用 Kafka 异步处理
        if (kafkaEnabled && systemTaskKafkaEnabled && systemTaskProducer != null) {
            systemTaskProducer.sendCleanupTask("LOGS", logRetentionDays);
            systemTaskProducer.sendCleanupTask("BACKUPS", backupRetentionDays);
            systemTaskProducer.sendCleanupTask("METRICS", metricsRetentionDays);
            logger.info("Cleanup tasks sent to Kafka");
            return;
        }

        // 降级：直接执行
        cleanupLogs(logRetentionDays);
        cleanupBackups(backupRetentionDays);
        cleanupMetrics(metricsRetentionDays);
    }

    /**
     * 清理过期日志
     */
    private void cleanupLogs(int retentionDays) {
        try {
            systemLogService.cleanupOldLogs(retentionDays);
        } catch (Exception e) {
            systemLogService.error("CLEANUP", "Failed to cleanup old logs", "SystemScheduler", e.getMessage());
        }
    }

    /**
     * 清理过期备份
     */
    private void cleanupBackups(int retentionDays) {
        try {
            dataBackupService.cleanupOldBackups(retentionDays);
        } catch (Exception e) {
            systemLogService.error("CLEANUP", "Failed to cleanup old backups", "SystemScheduler", e.getMessage());
        }
    }

    /**
     * 清理过期监控指标
     */
    private void cleanupMetrics(int retentionDays) {
        try {
            monitoringService.cleanupOldMetrics(retentionDays);
        } catch (Exception e) {
            systemLogService.error("CLEANUP", "Failed to cleanup old metrics", "SystemScheduler", e.getMessage());
        }
    }
}