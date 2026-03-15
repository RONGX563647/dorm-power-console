package com.dormpower.kafka;

import com.dormpower.model.CommandAckMessage;
import com.dormpower.model.StripStatus;
import com.dormpower.repository.StripStatusRepository;
import com.dormpower.service.CommandService;
import com.dormpower.service.WebSocketNotificationService;
import com.fasterxml.jackson.databind.JsonNode;
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
 * 命令确认 Kafka 消费者
 *
 * 功能：
 * 1. 消费命令确认消息
 * 2. 更新命令状态
 * 3. 更新插座状态
 * 4. 发送 WebSocket 通知
 *
 * @author dormpower team
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class CommandAckConsumer {

    private static final Logger logger = LoggerFactory.getLogger(CommandAckConsumer.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommandService commandService;

    @Autowired
    private StripStatusRepository stripStatusRepository;

    @Autowired
    private WebSocketNotificationService webSocketNotificationService;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private Counter processedCounter;
    private Counter errorCounter;
    private Timer processTimer;

    @PostConstruct
    public void initMetrics() {
        if (meterRegistry != null) {
            processedCounter = Counter.builder("kafka.consumer.messages")
                .tag("topic", "dorm.command.ack")
                .tag("status", "processed")
                .description("Number of processed command ack messages")
                .register(meterRegistry);

            errorCounter = Counter.builder("kafka.consumer.messages")
                .tag("topic", "dorm.command.ack")
                .tag("status", "error")
                .description("Number of failed command ack messages")
                .register(meterRegistry);

            processTimer = Timer.builder("kafka.consumer.process.time")
                .tag("topic", "dorm.command.ack")
                .description("Time taken to process command ack batch")
                .register(meterRegistry);

            logger.info("Command ack consumer metrics initialized");
        }
    }

    /**
     * 消费命令确认消息
     *
     * @param record Kafka 消息记录
     * @param acknowledgment 手动提交偏移量
     */
    @KafkaListener(
        topics = "dorm.command.ack",
        groupId = "dorm-power-command-ack",
        containerFactory = "singleKafkaListenerContainerFactory"
    )
    public void consumeCommandAck(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        logger.debug("Received command ack record: {}", record.key());

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            CommandAckMessage message = objectMapper.readValue(record.value(), CommandAckMessage.class);

            // 1. 更新命令状态
            commandService.updateCommandState(message.getCmdId(), message.getStatus(), message.getMessage());
            logger.debug("Updated command state: cmdId={}, status={}", message.getCmdId(), message.getStatus());

            // 2. 更新插座状态（如果有插座信息）
            if (message.getSocket() != null && message.getSocketState() != null) {
                updateSocketStatus(message.getDeviceId(), message.getSocket(), message.getSocketState());
            }

            // 3. 发送 WebSocket 通知
            JsonNode resultNode = objectMapper.valueToTree(
                java.util.Map.of(
                    "cmdId", message.getCmdId(),
                    "status", message.getStatus(),
                    "message", message.getMessage() != null ? message.getMessage() : ""
                )
            );
            webSocketNotificationService.broadcastCommandResult(
                message.getCmdId(),
                message.getStatus(),
                resultNode
            );

            acknowledgment.acknowledge();

            if (processedCounter != null) {
                processedCounter.increment();
            }

        } catch (Exception e) {
            logger.error("Failed to process command ack message: {}", e.getMessage(), e);
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
     * 更新插座状态
     */
    private void updateSocketStatus(String deviceId, int socketId, String socketState) {
        try {
            StripStatus stripStatus = stripStatusRepository.findByDeviceId(deviceId);
            if (stripStatus != null) {
                JsonNode sockets = objectMapper.readTree(stripStatus.getSocketsJson());
                if (sockets.isArray() && socketId > 0 && socketId <= sockets.size()) {
                    // 更新插座状态
                    com.fasterxml.jackson.databind.node.ObjectNode socketNode =
                        (com.fasterxml.jackson.databind.node.ObjectNode) sockets.get(socketId - 1);
                    socketNode.put("on", "on".equals(socketState));
                    stripStatus.setSocketsJson(objectMapper.writeValueAsString(sockets));
                    stripStatusRepository.save(stripStatus);
                    logger.debug("Updated socket status: deviceId={}, socket={}, state={}",
                        deviceId, socketId, socketState);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to update socket status: {}", e.getMessage());
        }
    }
}