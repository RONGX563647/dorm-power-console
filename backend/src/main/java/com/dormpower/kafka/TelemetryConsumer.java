package com.dormpower.kafka;

import com.dormpower.model.Device;
import com.dormpower.model.StripStatus;
import com.dormpower.model.Telemetry;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.StripStatusRepository;
import com.dormpower.repository.TelemetryBulkRepository;
import com.dormpower.service.AlertService;
import com.dormpower.service.WebSocketNotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 遥测数据 Kafka 消费者
 *
 * 功能：
 * 1. 批量消费遥测数据
 * 2. 批量写入数据库
 * 3. 触发 WebSocket 通知
 * 4. 触发告警检查
 *
 * 仅在 kafka.enabled=true 时启用
 *
 * @author dormpower team
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class TelemetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryConsumer.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TelemetryBulkRepository telemetryBulkRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private StripStatusRepository stripStatusRepository;

    @Autowired
    private WebSocketNotificationService webSocketNotificationService;

    @Autowired
    private AlertService alertService;

    @Value("${kafka.batch-size:100}")
    private int batchSize;

    /**
     * 批量消费遥测数据
     *
     * @param records Kafka 消息记录
     * @param acknowledgment 手动提交偏移量
     */
    @KafkaListener(
        topics = "dorm.telemetry",
        groupId = "dorm-power-telemetry",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeTelemetryBatch(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        if (records.isEmpty()) {
            return;
        }

        logger.debug("Received {} telemetry records", records.size());

        try {
            // 1. 解析消息
            List<Telemetry> telemetryList = new ArrayList<>(records.size());
            List<CompletableFuture<Void>> wsNotifications = new ArrayList<>();

            for (ConsumerRecord<String, String> record : records) {
                try {
                    JsonNode json = objectMapper.readTree(record.value());

                    Telemetry telemetry = new Telemetry();
                    telemetry.setDeviceId(json.get("deviceId").asText());
                    telemetry.setTs(json.get("ts").asLong());
                    telemetry.setPowerW(json.get("powerW").asDouble());
                    telemetry.setVoltageV(json.get("voltageV").asDouble());
                    telemetry.setCurrentA(json.get("currentA").asDouble());

                    telemetryList.add(telemetry);

                    // 异步发送 WebSocket 通知
                    wsNotifications.add(notifyTelemetryAsync(telemetry));

                    // 异步检查告警
                    checkAlertsAsync(telemetry);

                } catch (Exception e) {
                    logger.error("Failed to parse telemetry message: {}", e.getMessage());
                }
            }

            // 2. 批量写入数据库
            if (!telemetryList.isEmpty()) {
                telemetryBulkRepository.batchInsert(telemetryList);
                logger.info("Batch inserted {} telemetry records", telemetryList.size());
            }

            // 3. 更新设备最后在线时间
            updateDeviceLastSeen(telemetryList);

            // 4. 等待 WebSocket 通知完成
            CompletableFuture.allOf(wsNotifications.toArray(new CompletableFuture[0])).join();

            // 5. 手动提交偏移量
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process telemetry batch: {}", e.getMessage(), e);
            // 不提交偏移量，让 Kafka 重新投递
        }
    }

    /**
     * 批量消费设备状态数据
     */
    @KafkaListener(
        topics = "dorm.device.status",
        groupId = "dorm-power-status",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeStatusBatch(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        if (records.isEmpty()) {
            return;
        }

        logger.debug("Received {} device status records", records.size());

        try {
            for (ConsumerRecord<String, String> record : records) {
                try {
                    JsonNode json = objectMapper.readTree(record.value());
                    String deviceId = json.get("deviceId").asText();

                    // 更新设备状态
                    deviceRepository.findById(deviceId).ifPresent(device -> {
                        device.setOnline(json.get("online").asBoolean());
                        device.setLastSeenTs(System.currentTimeMillis() / 1000);
                        deviceRepository.save(device);
                    });

                    // 更新插座状态
                    StripStatus status = stripStatusRepository.findByDeviceId(deviceId);
                    if (status == null) {
                        status = new StripStatus();
                        status.setDeviceId(deviceId);
                    }
                    status.setOnline(json.get("online").asBoolean());
                    status.setTotalPowerW(json.get("totalPowerW").asDouble());
                    status.setVoltageV(json.get("voltageV").asDouble());
                    status.setCurrentA(json.get("currentA").asDouble());
                    if (json.has("socketsJson")) {
                        status.setSocketsJson(json.get("socketsJson").asText());
                    }
                    status.setTs(System.currentTimeMillis() / 1000);
                    stripStatusRepository.save(status);

                    // WebSocket 通知
                    webSocketNotificationService.broadcastDeviceStatus(deviceId, json);

                } catch (Exception e) {
                    logger.error("Failed to process status message: {}", e.getMessage());
                }
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process status batch: {}", e.getMessage(), e);
        }
    }

    /**
     * 批量消费告警数据
     */
    @KafkaListener(
        topics = "dorm.alert",
        groupId = "dorm-power-alert",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeAlertBatch(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        if (records.isEmpty()) {
            return;
        }

        logger.debug("Received {} alert records", records.size());

        try {
            for (ConsumerRecord<String, String> record : records) {
                try {
                    JsonNode json = objectMapper.readTree(record.value());
                    String deviceId = json.get("deviceId").asText();
                    String alertType = json.get("alertType").asText();
                    double alertValue = json.get("alertValue").asDouble();
                    double threshold = json.get("threshold").asDouble();

                    // 调用告警服务
                    alertService.checkAndGenerateAlert(deviceId, alertType, alertValue);

                } catch (Exception e) {
                    logger.error("Failed to process alert message: {}", e.getMessage());
                }
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process alert batch: {}", e.getMessage(), e);
        }
    }

    /**
     * 异步发送 WebSocket 通知
     */
    @Async
    protected CompletableFuture<Void> notifyTelemetryAsync(Telemetry telemetry) {
        try {
            webSocketNotificationService.broadcastTelemetry(
                telemetry.getDeviceId(),
                objectMapper.valueToTree(Map.of(
                    "ts", telemetry.getTs(),
                    "powerW", telemetry.getPowerW(),
                    "voltageV", telemetry.getVoltageV(),
                    "currentA", telemetry.getCurrentA()
                ))
            );
        } catch (Exception e) {
            logger.debug("Failed to send WebSocket notification: {}", e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步检查告警
     */
    @Async
    protected void checkAlertsAsync(Telemetry telemetry) {
        try {
            alertService.checkAndGenerateAlert(telemetry.getDeviceId(), "power", telemetry.getPowerW());
            alertService.checkAndGenerateAlert(telemetry.getDeviceId(), "voltage", telemetry.getVoltageV());
            alertService.checkAndGenerateAlert(telemetry.getDeviceId(), "current", telemetry.getCurrentA());
        } catch (Exception e) {
            logger.debug("Failed to check alerts: {}", e.getMessage());
        }
    }

    /**
     * 更新设备最后在线时间（批量）
     */
    private void updateDeviceLastSeen(List<Telemetry> telemetryList) {
        // 按设备ID去重
        Map<String, Long> deviceLastSeen = telemetryList.stream()
            .collect(Collectors.groupingBy(
                Telemetry::getDeviceId,
                Collectors.collectingAndThen(
                    Collectors.maxBy((t1, t2) -> Long.compare(t1.getTs(), t2.getTs())),
                    opt -> opt.map(Telemetry::getTs).orElse(System.currentTimeMillis() / 1000)
                )
            ));

        // 批量更新
        deviceLastSeen.forEach((deviceId, ts) -> {
            deviceRepository.findById(deviceId).ifPresent(device -> {
                device.setLastSeenTs(ts);
                device.setOnline(true);
                deviceRepository.save(device);
            });
        });
    }
}