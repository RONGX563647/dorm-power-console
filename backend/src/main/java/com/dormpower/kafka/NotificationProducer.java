package com.dormpower.kafka;

import com.dormpower.model.NotificationMessage;
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
 * 通知消息 Kafka 生产者
 *
 * 功能：
 * 1. 异步发送通知消息到 Kafka
 * 2. 支持多种通知类型
 * 3. 解耦通知创建与数据库写入
 *
 * @author dormpower team
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class NotificationProducer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationProducer.class);

    private static final String NOTIFICATION_TOPIC = "dorm.notification";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${kafka.notification.enabled:true}")
    private boolean notificationKafkaEnabled;

    /**
     * 发送通知消息
     *
     * @param message 通知消息
     */
    public void sendNotification(NotificationMessage message) {
        if (!notificationKafkaEnabled) {
            logger.debug("Notification Kafka is disabled, skipping message");
            return;
        }

        try {
            String messageJson = objectMapper.writeValueAsString(message);
            String key = message.getUsername() != null ? message.getUsername() : "system";

            kafkaTemplate.send(NOTIFICATION_TOPIC, key, messageJson)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to send notification message: {}", ex.getMessage());
                    } else {
                        logger.debug("Notification message sent to Kafka: topic={}, key={}, partition={}",
                            NOTIFICATION_TOPIC, key, result.getRecordMetadata().partition());
                    }
                });

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize notification message: {}", e.getMessage());
        }
    }

    /**
     * 发送告警通知
     *
     * @param title 标题
     * @param content 内容
     * @param username 用户名
     * @param sourceId 来源 ID
     */
    public void sendAlertNotification(String title, String content, String username, String sourceId) {
        NotificationMessage message = new NotificationMessage();
        message.setTitle(title);
        message.setContent(content);
        message.setType("ALERT");
        message.setPriority("HIGH");
        message.setUsername(username);
        message.setSource("ALERT");
        message.setSourceId(sourceId);

        sendNotification(message);
    }

    /**
     * 发送系统通知
     *
     * @param title 标题
     * @param content 内容
     * @param priority 优先级
     */
    public void sendSystemNotification(String title, String content, String priority) {
        NotificationMessage message = new NotificationMessage();
        message.setTitle(title);
        message.setContent(content);
        message.setType("SYSTEM");
        message.setPriority(priority != null ? priority : "NORMAL");
        message.setSource("SYSTEM");

        sendNotification(message);
    }

    /**
     * 发送邮件通知
     *
     * @param title 标题
     * @param content 内容
     * @param username 用户名
     */
    public void sendEmailNotification(String title, String content, String username) {
        NotificationMessage message = new NotificationMessage();
        message.setTitle(title);
        message.setContent(content);
        message.setType("EMAIL");
        message.setPriority("NORMAL");
        message.setUsername(username);
        message.setSource("EMAIL");

        sendNotification(message);
    }
}