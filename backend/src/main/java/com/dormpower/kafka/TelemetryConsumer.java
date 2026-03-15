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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
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
 * 5. 监控指标收集
 *
 * 优化版本 v2.0：
 * - 设备状态批量更新（使用 saveAll 和 JPQL 批量更新）
 * - 设备最后在线时间批量更新（使用 JPQL 批量更新）
 * - 添加 Micrometer 监控指标
 *
 * 仅在 kafka.enabled=true 时启用
 *
 * @author dormpower team
 * @version 2.0
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

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Value("${kafka.batch-size:100}")
    private int batchSize;

    // 监控指标
    private Counter telemetryProcessedCounter;
    private Counter telemetryErrorCounter;
    private Timer telemetryProcessTimer;
    private Counter statusProcessedCounter;
    private Counter statusErrorCounter;
    private Timer statusProcessTimer;
    private Counter alertProcessedCounter;
    private Counter alertErrorCounter;

    @PostConstruct
    public void initMetrics() {
        if (meterRegistry != null) {
            telemetryProcessedCounter = Counter.builder("kafka.consumer.messages")
                .tag("topic", "dorm.telemetry")
                .tag("status", "processed")
                .description("Number of processed telemetry messages")
                .register(meterRegistry);

            telemetryErrorCounter = Counter.builder("kafka.consumer.messages")
                .tag("topic", "dorm.telemetry")
                .tag("status", "error")
                .description("Number of failed telemetry messages")
                .register(meterRegistry);

            telemetryProcessTimer = Timer.builder("kafka.consumer.process.time")
                .tag("topic", "dorm.telemetry")
                .description("Time taken to process telemetry batch")
                .register(meterRegistry);

            statusProcessedCounter = Counter.builder("kafka.consumer.messages")
                .tag("topic", "dorm.device.status")
                .tag("status", "processed")
                .description("Number of processed status messages")
                .register(meterRegistry);

            statusErrorCounter = Counter.builder("kafka.consumer.messages")
                .tag("topic", "dorm.device.status")
                .tag("status", "error")
                .description("Number of failed status messages")
                .register(meterRegistry);

            statusProcessTimer = Timer.builder("kafka.consumer.process.time")
                .tag("topic", "dorm.device.status")
                .description("Time taken to process status batch")
                .register(meterRegistry);

            alertProcessedCounter = Counter.builder("kafka.consumer.messages")
                .tag("topic", "dorm.alert")
                .tag("status", "processed")
                .description("Number of processed alert messages")
                .register(meterRegistry);

            alertErrorCounter = Counter.builder("kafka.consumer.messages")
                .tag("topic", "dorm.alert")
                .tag("status", "error")
                .description("Number of failed alert messages")
                .register(meterRegistry);

            logger.info("Kafka consumer metrics initialized");
        }
    }

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

        Timer.Sample sample = Timer.start(meterRegistry);
        int processedCount = 0;
        int errorCount = 0;

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
                    processedCount++;

                    // 异步发送 WebSocket 通知
                    wsNotifications.add(notifyTelemetryAsync(telemetry));

                    // 异步检查告警
                    checkAlertsAsync(telemetry);

                } catch (Exception e) {
                    logger.error("Failed to parse telemetry message: {}", e.getMessage());
                    errorCount++;
                }
            }

            // 2. 批量写入数据库
            if (!telemetryList.isEmpty()) {
                telemetryBulkRepository.batchInsert(telemetryList);
                logger.info("Batch inserted {} telemetry records", telemetryList.size());
            }

            // 3. 批量更新设备最后在线时间（优化：使用 JPQL 批量更新）
            batchUpdateDeviceLastSeen(telemetryList);

            // 4. 等待 WebSocket 通知完成
            CompletableFuture.allOf(wsNotifications.toArray(new CompletableFuture[0])).join();

            // 5. 手动提交偏移量
            acknowledgment.acknowledge();

            // 6. 记录监控指标
            if (telemetryProcessedCounter != null) {
                telemetryProcessedCounter.increment(processedCount);
            }
            if (telemetryErrorCounter != null) {
                telemetryErrorCounter.increment(errorCount);
            }

        } catch (Exception e) {
            logger.error("Failed to process telemetry batch: {}", e.getMessage(), e);
            // 不提交偏移量，让 Kafka 重新投递
            if (telemetryErrorCounter != null) {
                telemetryErrorCounter.increment(records.size());
            }
        } finally {
            if (telemetryProcessTimer != null) {
                sample.stop(telemetryProcessTimer);
            }
        }
    }

    /**
     * 批量消费设备状态数据（优化版本：批量更新数据库）
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

        Timer.Sample sample = Timer.start(meterRegistry);
        int processedCount = 0;
        int errorCount = 0;

        try {
            // 收集需要更新的数据
            Map<String, Device> deviceUpdates = new HashMap<>();
            Map<String, StripStatus> statusUpdates = new HashMap<>();
            List<CompletableFuture<Void>> wsNotifications = new ArrayList<>();
            long now = System.currentTimeMillis() / 1000;

            for (ConsumerRecord<String, String> record : records) {
                try {
                    JsonNode json = objectMapper.readTree(record.value());
                    String deviceId = json.get("deviceId").asText();

                    // 收集设备更新
                    deviceRepository.findById(deviceId).ifPresent(device -> {
                        device.setOnline(json.get("online").asBoolean());
                        device.setLastSeenTs(now);
                        deviceUpdates.put(deviceId, device);
                    });

                    // 收集插座状态更新
                    StripStatus status = statusUpdates.get(deviceId);
                    if (status == null) {
                        status = stripStatusRepository.findByDeviceId(deviceId);
                    }
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
                    status.setTs(now);
                    statusUpdates.put(deviceId, status);

                    // 收集 WebSocket 通知
                    wsNotifications.add(notifyDeviceStatusAsync(deviceId, json));

                    processedCount++;

                } catch (Exception e) {
                    logger.error("Failed to process status message: {}", e.getMessage());
                    errorCount++;
                }
            }

            // 批量保存设备状态
            if (!deviceUpdates.isEmpty()) {
                deviceRepository.saveAll(deviceUpdates.values());
                logger.debug("Batch updated {} devices", deviceUpdates.size());
            }

            // 批量保存插座状态
            if (!statusUpdates.isEmpty()) {
                stripStatusRepository.saveAll(statusUpdates.values());
                logger.debug("Batch updated {} strip statuses", statusUpdates.size());
            }

            // 等待 WebSocket 通知完成
            CompletableFuture.allOf(wsNotifications.toArray(new CompletableFuture[0])).join();

            acknowledgment.acknowledge();

            // 记录监控指标
            if (statusProcessedCounter != null) {
                statusProcessedCounter.increment(processedCount);
            }
            if (statusErrorCounter != null) {
                statusErrorCounter.increment(errorCount);
            }

        } catch (Exception e) {
            logger.error("Failed to process status batch: {}", e.getMessage(), e);
            if (statusErrorCounter != null) {
                statusErrorCounter.increment(records.size());
            }
        } finally {
            if (statusProcessTimer != null) {
                sample.stop(statusProcessTimer);
            }
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

        int processedCount = 0;
        int errorCount = 0;

        try {
            for (ConsumerRecord<String, String> record : records) {
                try {
                    JsonNode json = objectMapper.readTree(record.value());
                    String deviceId = json.get("deviceId").asText();
                    String alertType = json.get("alertType").asText();
                    double alertValue = json.get("alertValue").asDouble();

                    // 调用告警服务
                    alertService.checkAndGenerateAlert(deviceId, alertType, alertValue);
                    processedCount++;

                } catch (Exception e) {
                    logger.error("Failed to process alert message: {}", e.getMessage());
                    errorCount++;
                }
            }

            acknowledgment.acknowledge();

            // 记录监控指标
            if (alertProcessedCounter != null) {
                alertProcessedCounter.increment(processedCount);
            }
            if (alertErrorCounter != null) {
                alertErrorCounter.increment(errorCount);
            }

        } catch (Exception e) {
            logger.error("Failed to process alert batch: {}", e.getMessage(), e);
            if (alertErrorCounter != null) {
                alertErrorCounter.increment(records.size());
            }
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
     * 异步发送设备状态 WebSocket 通知
     */
    @Async
    protected CompletableFuture<Void> notifyDeviceStatusAsync(String deviceId, JsonNode status) {
        try {
            webSocketNotificationService.broadcastDeviceStatus(deviceId, status);
        } catch (Exception e) {
            logger.debug("Failed to send device status notification: {}", e.getMessage());
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
     * 批量更新设备最后在线时间（优化：使用 JPQL 批量更新）
     */
    private void batchUpdateDeviceLastSeen(List<Telemetry> telemetryList) {
        if (telemetryList.isEmpty()) {
            return;
        }

        // 按设备ID去重，获取每个设备的最新时间戳
        Map<String, Long> deviceLastSeen = telemetryList.stream()
            .collect(Collectors.groupingBy(
                Telemetry::getDeviceId,
                Collectors.collectingAndThen(
                    Collectors.maxBy((t1, t2) -> Long.compare(t1.getTs(), t2.getTs())),
                    opt -> opt.map(Telemetry::getTs).orElse(System.currentTimeMillis() / 1000)
                )
            ));

        // 使用 TelemetryBulkRepository 的批量更新方法
        telemetryBulkRepository.batchUpdateLastSeen(
            new ArrayList<>(deviceLastSeen.keySet()),
            System.currentTimeMillis() / 1000
        );

        logger.debug("Batch updated last seen time for {} devices", deviceLastSeen.size());
    }
}