package com.dormpower.kafka;

import com.dormpower.model.SystemTaskMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 系统任务 Kafka 生产者
 *
 * 功能：
 * 1. 异步发送系统任务消息到 Kafka
 * 2. 解耦任务调度与任务执行
 *
 * @author dormpower team
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class SystemTaskProducer {

    private static final Logger logger = LoggerFactory.getLogger(SystemTaskProducer.class);

    private static final String SYSTEM_TASK_TOPIC = "dorm.system.task";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${kafka.system-task.enabled:true}")
    private boolean systemTaskKafkaEnabled;

    /**
     * 发送系统任务消息
     *
     * @param message 系统任务消息
     */
    public void sendSystemTask(SystemTaskMessage message) {
        if (!systemTaskKafkaEnabled) {
            logger.debug("System task Kafka is disabled, skipping message");
            return;
        }

        try {
            String messageJson = objectMapper.writeValueAsString(message);
            String key = message.getTaskType();

            kafkaTemplate.send(SYSTEM_TASK_TOPIC, key, messageJson)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to send system task message: {}", ex.getMessage());
                    } else {
                        logger.info("System task message sent to Kafka: type={}, partition={}",
                            message.getTaskType(), result.getRecordMetadata().partition());
                    }
                });

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize system task message: {}", e.getMessage());
        }
    }

    /**
     * 发送备份任务
     *
     * @param description 描述
     * @param retentionDays 保留天数
     */
    public void sendBackupTask(String description, int retentionDays) {
        SystemTaskMessage message = new SystemTaskMessage(
            "BACKUP",
            description,
            java.util.Map.of("retentionDays", retentionDays)
        );
        message.setSource("SCHEDULER");

        sendSystemTask(message);
    }

    /**
     * 发送清理任务
     *
     * @param targetType 清理目标类型：LOGS, BACKUPS, METRICS
     * @param retentionDays 保留天数
     */
    public void sendCleanupTask(String targetType, int retentionDays) {
        SystemTaskMessage message = new SystemTaskMessage(
            "CLEANUP_" + targetType,
            "清理过期" + targetType,
            java.util.Map.of("retentionDays", retentionDays, "targetType", targetType)
        );
        message.setSource("SCHEDULER");

        sendSystemTask(message);
    }

    /**
     * 发送指标收集任务
     */
    public void sendMetricsCollectionTask() {
        SystemTaskMessage message = new SystemTaskMessage(
            "METRICS",
            "收集系统指标",
            java.util.Map.of()
        );
        message.setSource("SCHEDULER");

        sendSystemTask(message);
    }
}