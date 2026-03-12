package com.dormpower.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 遥测数据 Kafka 生产者
 *
 * 功能：
 * 1. 将 MQTT 接收的遥测数据发送到 Kafka
 * 2. 异步发送，不阻塞 MQTT 消息处理
 * 3. 支持消息压缩和批量发送
 *
 * @author dormpower team
 * @version 1.0
 */
@Component
public class TelemetryProducer {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryProducer.class);

    private static final String TELEMETRY_TOPIC = "dorm.telemetry";
    private static final String STATUS_TOPIC = "dorm.device.status";
    private static final String ALERT_TOPIC = "dorm.alert";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${kafka.enabled:true}")
    private boolean kafkaEnabled;

    /**
     * 发送遥测数据
     *
     * @param deviceId 设备 ID
     * @param ts 时间戳
     * @param powerW 功率
     * @param voltageV 电压
     * @param currentA 电流
     */
    public void sendTelemetry(String deviceId, long ts, double powerW, double voltageV, double currentA) {
        if (!kafkaEnabled) {
            logger.debug("Kafka is disabled, skipping telemetry message");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("ts", ts);
        data.put("powerW", powerW);
        data.put("voltageV", voltageV);
        data.put("currentA", currentA);
        data.put("timestamp", System.currentTimeMillis());

        sendAsync(TELEMETRY_TOPIC, deviceId, data);
    }

    /**
     * 发送设备状态数据
     *
     * @param deviceId 设备 ID
     * @param online 是否在线
     * @param totalPowerW 总功率
     * @param voltageV 电压
     * @param currentA 电流
     * @param socketsJson 插座状态 JSON
     */
    public void sendDeviceStatus(String deviceId, boolean online, double totalPowerW,
                                  double voltageV, double currentA, String socketsJson) {
        if (!kafkaEnabled) {
            logger.debug("Kafka is disabled, skipping device status message");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("online", online);
        data.put("totalPowerW", totalPowerW);
        data.put("voltageV", voltageV);
        data.put("currentA", currentA);
        data.put("socketsJson", socketsJson);
        data.put("timestamp", System.currentTimeMillis());

        sendAsync(STATUS_TOPIC, deviceId, data);
    }

    /**
     * 发送告警数据
     *
     * @param deviceId 设备 ID
     * @param alertType 告警类型
     * @param alertValue 告警值
     * @param threshold 阈值
     */
    public void sendAlert(String deviceId, String alertType, double alertValue, double threshold) {
        if (!kafkaEnabled) {
            logger.debug("Kafka is disabled, skipping alert message");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("alertType", alertType);
        data.put("alertValue", alertValue);
        data.put("threshold", threshold);
        data.put("timestamp", System.currentTimeMillis());

        sendAsync(ALERT_TOPIC, deviceId, data);
    }

    /**
     * 异步发送消息到 Kafka
     *
     * @param topic 主题
     * @param key 消息 key（用于分区）
     * @param data 数据
     */
    private void sendAsync(String topic, String key, Map<String, Object> data) {
        try {
            String message = objectMapper.writeValueAsString(data);

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, message);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send message to Kafka: topic={}, key={}, error={}",
                            topic, key, ex.getMessage());
                } else {
                    logger.debug("Message sent to Kafka: topic={}, key={}, partition={}",
                            topic, key, result.getRecordMetadata().partition());
                }
            });
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize message: {}", e.getMessage());
        }
    }

    /**
     * 同步发送消息（用于关键场景）
     *
     * @param topic 主题
     * @param key 消息 key
     * @param data 数据
     * @return 是否发送成功
     */
    public boolean sendSync(String topic, String key, Map<String, Object> data) {
        try {
            String message = objectMapper.writeValueAsString(data);
            kafkaTemplate.send(topic, key, message).get();
            return true;
        } catch (Exception e) {
            logger.error("Failed to send message synchronously: {}", e.getMessage());
            return false;
        }
    }
}