package com.dormpower.scheduler;

import com.dormpower.service.MonitoringService;
import com.dormpower.service.SystemConfigService;
import com.dormpower.service.DataBackupService;
import com.dormpower.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 系统监控定时任务
 */
@Component
public class SystemMetricsScheduler {

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private DataBackupService dataBackupService;

    @Autowired
    private SystemLogService systemLogService;

    /**
     * 每5分钟收集一次系统指标
     */
    @Scheduled(fixedRate = 300000)
    public void collectSystemMetrics() {
        String enabled = systemConfigService.getConfigValue("monitor.enabled", "true");
        if ("true".equalsIgnoreCase(enabled)) {
            monitoringService.collectSystemMetrics();
        }
    }

    /**
     * 每天凌晨2点执行自动备份
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoBackup() {
        String enabled = systemConfigService.getConfigValue("backup.enabled", "true");
        if ("true".equalsIgnoreCase(enabled)) {
            try {
                dataBackupService.createDatabaseBackup("自动备份", "system");
                systemLogService.info("BACKUP", "Auto backup completed", "SystemScheduler");
            } catch (Exception e) {
                systemLogService.error("BACKUP", "Auto backup failed", "SystemScheduler", e.getMessage());
            }
        }
    }

    /**
     * 每天凌晨3点清理过期数据
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldData() {
        // 清理过期日志
        try {
            int logRetentionDays = Integer.parseInt(systemConfigService.getConfigValue("log.retention_days", "30"));
            systemLogService.cleanupOldLogs(logRetentionDays);
        } catch (Exception e) {
            systemLogService.error("CLEANUP", "Failed to cleanup old logs", "SystemScheduler", e.getMessage());
        }

        // 清理过期备份
        try {
            int backupRetentionDays = Integer.parseInt(systemConfigService.getConfigValue("backup.retention_days", "30"));
            dataBackupService.cleanupOldBackups(backupRetentionDays);
        } catch (Exception e) {
            systemLogService.error("CLEANUP", "Failed to cleanup old backups", "SystemScheduler", e.getMessage());
        }

        // 清理过期监控指标
        try {
            int metricsRetentionDays = Integer.parseInt(systemConfigService.getConfigValue("monitor.metrics_retention_days", "7"));
            monitoringService.cleanupOldMetrics(metricsRetentionDays);
        } catch (Exception e) {
            systemLogService.error("CLEANUP", "Failed to cleanup old metrics", "SystemScheduler", e.getMessage());
        }
    }
}
