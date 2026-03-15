package com.dormpower.kafka;

import com.dormpower.model.Notification;
import com.dormpower.model.NotificationMessage;
import com.dormpower.repository.NotificationRepository;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 通知消息 Kafka 消费者
 *
 * 功能：
 * 1. 批量消费通知消息
 * 2. 批量写入数据库
 * 3. 监控指标收集
 *
 * @author dormpower team
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class NotificationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private Counter processedCounter;
    private Counter errorCounter;
    private Timer processTimer;

    @PostConstruct
    public void initMetrics() {
        if (meterRegistry != null) {
            processedCounter = Counter.builder("kafka.consumer.messages")
                .tag("topic", "dorm.notification")
                .tag("status", "processed")
                .description("Number of processed notification messages")
                .register(meterRegistry);

            errorCounter = Counter.builder("kafka.consumer.messages")
                .tag("topic", "dorm.notification")
                .tag("status", "error")
                .description("Number of failed notification messages")
                .register(meterRegistry);

            processTimer = Timer.builder("kafka.consumer.process.time")
                .tag("topic", "dorm.notification")
                .description("Time taken to process notification batch")
                .register(meterRegistry);

            logger.info("Notification consumer metrics initialized");
        }
    }

    /**
     * 批量消费通知消息
     *
     * @param records Kafka 消息记录
     * @param acknowledgment 手动提交偏移量
     */
    @KafkaListener(
        topics = "dorm.notification",
        groupId = "dorm-power-notification",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeNotificationBatch(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        if (records.isEmpty()) {
            return;
        }

        logger.debug("Received {} notification records", records.size());

        Timer.Sample sample = Timer.start(meterRegistry);
        int processedCount = 0;
        int errorCount = 0;

        try {
            List<Notification> notifications = new ArrayList<>(records.size());

            for (ConsumerRecord<String, String> record : records) {
                try {
                    NotificationMessage message = objectMapper.readValue(record.value(), NotificationMessage.class);
                    notifications.add(message.toEntity());
                    processedCount++;
                } catch (Exception e) {
                    logger.error("Failed to parse notification message: {}", e.getMessage());
                    errorCount++;
                }
            }

            // 批量写入数据库
            if (!notifications.isEmpty()) {
                notificationRepository.saveAll(notifications);
                logger.info("Batch saved {} notifications", notifications.size());
            }

            acknowledgment.acknowledge();

            // 记录监控指标
            if (processedCounter != null) {
                processedCounter.increment(processedCount);
            }
            if (errorCounter != null) {
                errorCounter.increment(errorCount);
            }

        } catch (Exception e) {
            logger.error("Failed to process notification batch: {}", e.getMessage(), e);
            if (errorCounter != null) {
                errorCounter.increment(records.size());
            }
        } finally {
            if (processTimer != null) {
                sample.stop(processTimer);
            }
        }
    }
}