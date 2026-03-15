package com.dormpower.kafka;

import com.dormpower.model.CommandAckMessage;
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
 * 命令确认 Kafka 生产者
 *
 * 功能：
 * 1. 异步发送命令确认消息到 Kafka
 * 2. 解耦 MQTT 接收与命令状态更新
 *
 * @author dormpower team
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class CommandAckProducer {

    private static final Logger logger = LoggerFactory.getLogger(CommandAckProducer.class);

    private static final String COMMAND_ACK_TOPIC = "dorm.command.ack";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${kafka.command.enabled:true}")
    private boolean commandKafkaEnabled;

    /**
     * 发送命令确认消息
     *
     * @param message 命令确认消息
     */
    public void sendCommandAck(CommandAckMessage message) {
        if (!commandKafkaEnabled) {
            logger.debug("Command Kafka is disabled, skipping message");
            return;
        }

        try {
            String messageJson = objectMapper.writeValueAsString(message);
            String key = message.getDeviceId();

            kafkaTemplate.send(COMMAND_ACK_TOPIC, key, messageJson)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to send command ack message: {}", ex.getMessage());
                    } else {
                        logger.debug("Command ack message sent to Kafka: topic={}, key={}, partition={}",
                            COMMAND_ACK_TOPIC, key, result.getRecordMetadata().partition());
                    }
                });

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize command ack message: {}", e.getMessage());
        }
    }

    /**
     * 发送命令确认（简化版）
     *
     * @param deviceId 设备 ID
     * @param cmdId 命令 ID
     * @param status 状态
     * @param message 消息
     */
    public void sendCommandAck(String deviceId, String cmdId, String status, String message) {
        CommandAckMessage ackMessage = new CommandAckMessage();
        ackMessage.setDeviceId(deviceId);
        ackMessage.setCmdId(cmdId);
        ackMessage.setStatus(status);
        ackMessage.setMessage(message);

        sendCommandAck(ackMessage);
    }
}