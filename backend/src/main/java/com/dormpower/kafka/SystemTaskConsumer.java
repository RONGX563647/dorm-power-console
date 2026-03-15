package com.dormpower.kafka;

import com.dormpower.model.SystemTaskMessage;
import com.dormpower.service.DataBackupService;
import com.dormpower.service.MonitoringService;
import com.dormpower.service.SystemLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 系统任务 Kafka 消费者
 *
 * 功能：
 * 1. 消费系统任务消息
 * 2. 执行备份、清理、指标收集等任务
 * 3. 监控指标收集
 *
 * @author dormpower team
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class SystemTaskConsumer {

    private static final Logger logger = LoggerFactory.getLogger(SystemTaskConsumer.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private DataBackupService dataBackupService;

    @Autowired(required = false)
    private MonitoringService monitoringService;

    @Autowired(required = false)
    private SystemLogService systemLogService;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private Counter processedCounter;
    private Counter errorCounter;
    private Timer processTimer;

    @PostConstruct
    public void initMetrics() {
        if (meterRegistry != null) {
            processedCounter = Counter.builder("kafka.consumer.messages")
                .tag("topic", "dorm.system.task")
                .tag("status", "processed")
                .description("Number of processed system task messages")
                .register(meterRegistry);

            errorCounter = Counter.builder("kafka.consumer.messages")
                .tag("topic", "dorm.system.task")
                .tag("status", "error")
                .description("Number of failed system task messages")
                .register(meterRegistry);

            processTimer = Timer.builder("kafka.consumer.process.time")
                .tag("topic", "dorm.system.task")
                .description("Time taken to process system task")
                .register(meterRegistry);

            logger.info("System task consumer metrics initialized");
        }
    }

    /**
     * 消费系统任务消息
     *
     * @param record Kafka 消息记录
     * @param acknowledgment 手动提交偏移量
     */
    @KafkaListener(
        topics = "dorm.system.task",
        groupId = "dorm-power-system-task",
        containerFactory = "singleKafkaListenerContainerFactory"
    )
    public void consumeSystemTask(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        logger.info("Received system task: {}", record.key());

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            SystemTaskMessage message = objectMapper.readValue(record.value(), SystemTaskMessage.class);

            // 根据任务类型执行不同的处理
            processTask(message);

            acknowledgment.acknowledge();

            if (processedCounter != null) {
                processedCounter.increment();
            }

            logger.info("System task completed: type={}", message.getTaskType());

        } catch (Exception e) {
            logger.error("Failed to process system task: {}", e.getMessage(), e);
            if (errorCounter != null) {
                errorCounter.increment();
            }
        } finally {
            if (processTimer != null) {
                sample.stop(processTimer);
            }
        }
    }

    /**
     * 处理系统任务
     */
    private void processTask(SystemTaskMessage message) {
        String taskType = message.getTaskType();

        switch (taskType) {
            case "BACKUP":
                handleBackupTask(message);
                break;
            case "CLEANUP_LOGS":
                handleCleanupLogsTask(message);
                break;
            case "CLEANUP_BACKUPS":
                handleCleanupBackupsTask(message);
                break;
            case "CLEANUP_METRICS":
                handleCleanupMetricsTask(message);
                break;
            case "METRICS":
                handleMetricsTask(message);
                break;
            default:
                logger.warn("Unknown task type: {}", taskType);
        }
    }

    /**
     * 处理备份任务
     */
    private void handleBackupTask(SystemTaskMessage message) {
        if (dataBackupService == null) {
            logger.warn("DataBackupService not available, skipping backup task");
            return;
        }

        try {
            int retentionDays = getParameter(message, "retentionDays", 30);
            dataBackupService.createDatabaseBackup(message.getDescription(), message.getSource());

            if (systemLogService != null) {
                systemLogService.info("BACKUP", "Backup completed: " + message.getDescription(), "SystemTaskConsumer");
            }

            logger.info("Backup task completed: retentionDays={}", retentionDays);

        } catch (Exception e) {
            logger.error("Backup task failed: {}", e.getMessage(), e);
            if (systemLogService != null) {
                systemLogService.error("BACKUP", "Backup failed: " + e.getMessage(), "SystemTaskConsumer", null);
            }
        }
    }

    /**
     * 处理日志清理任务
     */
    private void handleCleanupLogsTask(SystemTaskMessage message) {
        if (systemLogService == null) {
            logger.warn("SystemLogService not available, skipping logs cleanup task");
            return;
        }

        try {
            int retentionDays = getParameter(message, "retentionDays", 30);
            systemLogService.cleanupOldLogs(retentionDays);

            logger.info("Logs cleanup task completed: retentionDays={}", retentionDays);

        } catch (Exception e) {
            logger.error("Logs cleanup task failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理备份清理任务
     */
    private void handleCleanupBackupsTask(SystemTaskMessage message) {
        if (dataBackupService == null) {
            logger.warn("DataBackupService not available, skipping backups cleanup task");
            return;
        }

        try {
            int retentionDays = getParameter(message, "retentionDays", 30);
            dataBackupService.cleanupOldBackups(retentionDays);

            logger.info("Backups cleanup task completed: retentionDays={}", retentionDays);

        } catch (Exception e) {
            logger.error("Backups cleanup task failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理指标清理任务
     */
    private void handleCleanupMetricsTask(SystemTaskMessage message) {
        if (monitoringService == null) {
            logger.warn("MonitoringService not available, skipping metrics cleanup task");
            return;
        }

        try {
            int retentionDays = getParameter(message, "retentionDays", 7);
            monitoringService.cleanupOldMetrics(retentionDays);

            logger.info("Metrics cleanup task completed: retentionDays={}", retentionDays);

        } catch (Exception e) {
            logger.error("Metrics cleanup task failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理指标收集任务
     */
    private void handleMetricsTask(SystemTaskMessage message) {
        if (monitoringService == null) {
            logger.warn("MonitoringService not available, skipping metrics task");
            return;
        }

        try {
            monitoringService.collectSystemMetrics();

            logger.info("Metrics collection task completed");

        } catch (Exception e) {
            logger.error("Metrics collection task failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取任务参数
     */
    private int getParameter(SystemTaskMessage message, String key, int defaultValue) {
        if (message.getParameters() == null) {
            return defaultValue;
        }

        Object value = message.getParameters().get(key);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}