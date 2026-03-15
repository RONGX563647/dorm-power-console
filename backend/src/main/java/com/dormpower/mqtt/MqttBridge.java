package com.dormpower.mqtt;

import com.dormpower.config.MqttConfig;
import com.dormpower.kafka.TelemetryProducer;
import com.dormpower.kafka.CommandAckProducer;
import com.dormpower.model.CommandAckMessage;
import com.dormpower.model.CommandRecord;
import com.dormpower.model.Device;
import com.dormpower.model.StripStatus;
import com.dormpower.model.Telemetry;
import com.dormpower.model.DeviceStatusHistory;
import com.dormpower.repository.CommandRecordRepository;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.StripStatusRepository;
import com.dormpower.repository.TelemetryRepository;
import com.dormpower.repository.DeviceStatusHistoryRepository;
import com.dormpower.service.CommandService;
import com.dormpower.service.WebSocketNotificationService;
import com.dormpower.service.AlertService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * MQTT桥接类
 *
 * 管理与MQTT代理的连接，处理设备消息的接收和命令的发送。
 * 支持设备状态更新、遥测数据接收和命令执行结果处理。
 *
 * 优化版本：
 * - 遥测数据通过 Kafka 异步处理，提高吞吐量
 * - 设备状态通过 Kafka 广播，支持多实例部署
 */
@Component
public class MqttBridge {

    @Autowired
    private MqttConfig mqttConfig;

    @Autowired
    private WebSocketNotificationService webSocketNotificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private StripStatusRepository stripStatusRepository;

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Autowired
    private CommandRecordRepository commandRecordRepository;

    @Autowired
    private CommandService commandService;

    @Autowired
    private DeviceStatusHistoryRepository deviceStatusHistoryRepository;

    @Autowired
    private AlertService alertService;

    @Autowired(required = false)
    private TelemetryProducer telemetryProducer;

    @Autowired(required = false)
    private CommandAckProducer commandAckProducer;

    @Value("${kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Value("${kafka.command.enabled:true}")
    private boolean commandKafkaEnabled;

    private MqttClient mqttClient;
    private boolean connected = false;

    @PostConstruct
    public void init() {
        if (!mqttConfig.isEnabled()) {
            System.out.println("MQTT bridge is disabled");
            return;
        }

        try {
            mqttClient = mqttConfig.createMqttClient();
            if (mqttClient != null) {
                setupCallback();
                subscribeTopics();
                connected = true;
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize MQTT bridge: " + e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
                System.out.println("MQTT bridge stopped");
            } catch (MqttException e) {
                System.err.println("Error stopping MQTT bridge: " + e.getMessage());
            }
        }
    }

    private void setupCallback() {
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                connected = false;
                System.err.println("MQTT connection lost: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String payload = new String(message.getPayload());
                System.out.println("MQTT message arrived on topic " + topic + ": " + payload);
                handleMessage(topic, payload);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                System.out.println("MQTT message delivery complete");
            }
        });
    }

    private void subscribeTopics() throws MqttException {
        String prefix = mqttConfig.getTopicPrefix();
        
        String[] singleLevelTopics = {
            prefix + "/+/status",
            prefix + "/+/telemetry",
            prefix + "/+/ack",
            prefix + "/+/event"
        };
        
        String[] twoLevelTopics = {
            prefix + "/+/+/status",
            prefix + "/+/+/telemetry",
            prefix + "/+/+/ack",
            prefix + "/+/+/event"
        };

        for (String topic : singleLevelTopics) {
            mqttClient.subscribe(topic, 1);
            System.out.println("Subscribed to MQTT topic: " + topic);
        }
        
        for (String topic : twoLevelTopics) {
            mqttClient.subscribe(topic, 1);
            System.out.println("Subscribed to MQTT topic: " + topic);
        }
    }

    private void handleMessage(String topic, String payload) {
        try {
            TopicInfo topicInfo = parseTopic(topic);
            if (topicInfo == null) {
                System.err.println("Failed to parse topic: " + topic);
                return;
            }

            JsonNode jsonNode = objectMapper.readTree(payload);
            String deviceId = topicInfo.deviceId;
            String msgType = topicInfo.msgType;

            switch (msgType) {
                case "status":
                    handleStatusMessage(deviceId, jsonNode);
                    break;
                case "telemetry":
                    handleTelemetryMessage(deviceId, jsonNode);
                    break;
                case "ack":
                    handleAckMessage(deviceId, jsonNode);
                    break;
                case "event":
                    handleEventMessage(deviceId, jsonNode);
                    break;
                default:
                    System.out.println("Unknown message type: " + msgType);
            }
        } catch (Exception e) {
            System.err.println("Error handling MQTT message: " + e.getMessage());
        }
    }

    private void handleStatusMessage(String deviceId, JsonNode payload) {
        System.out.println("Handling status message for device: " + deviceId);

        long now = System.currentTimeMillis() / 1000;
        boolean online = payload.has("online") ? payload.get("online").asBoolean() : true;
        double totalPowerW = payload.has("total_power_w") ? payload.get("total_power_w").asDouble() : 0.0;
        double voltageV = payload.has("voltage_v") ? payload.get("voltage_v").asDouble() : 220.0;
        double currentA = payload.has("current_a") ? payload.get("current_a").asDouble() : 0.0;
        String socketsJson = payload.has("sockets") ? payload.get("sockets").toString() : "[]";

        // 优先使用 Kafka 发送（支持多实例广播）
        if (kafkaEnabled && telemetryProducer != null) {
            telemetryProducer.sendDeviceStatus(deviceId, online, totalPowerW, voltageV, currentA, socketsJson);
        } else {
            // 降级：直接处理
            deviceRepository.findById(deviceId).ifPresent(device -> {
                device.setOnline(online);
                device.setLastSeenTs(now);
                deviceRepository.save(device);
            });

            StripStatus status = stripStatusRepository.findByDeviceId(deviceId);
            if (status == null) {
                status = new StripStatus();
                status.setDeviceId(deviceId);
            }

            status.setTs(now);
            status.setOnline(online);
            status.setTotalPowerW(totalPowerW);
            status.setVoltageV(voltageV);
            status.setCurrentA(currentA);
            status.setSocketsJson(socketsJson);
            stripStatusRepository.save(status);

            // 记录设备状态历史
            DeviceStatusHistory history = new DeviceStatusHistory();
            history.setId("history_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000));
            history.setDeviceId(deviceId);
            history.setOnline(online);
            history.setTotalPowerW(totalPowerW);
            history.setVoltageV(voltageV);
            history.setCurrentA(currentA);
            history.setSocketsJson(socketsJson);
            history.setTs(now);
            history.setCreatedAt(now);
            deviceStatusHistoryRepository.save(history);

            webSocketNotificationService.broadcastDeviceStatus(deviceId, payload);
        }
    }

    private void handleTelemetryMessage(String deviceId, JsonNode payload) {
        System.out.println("Handling telemetry message for device: " + deviceId);

        long now = System.currentTimeMillis() / 1000;

        double powerW = payload.has("power_w") ? payload.get("power_w").asDouble() : 0.0;
        double voltageV = payload.has("voltage_v") ? payload.get("voltage_v").asDouble() : 220.0;
        double currentA = payload.has("current_a") ? payload.get("current_a").asDouble() : 0.0;
        long ts = payload.has("ts") ? payload.get("ts").asLong() : now;

        // 优先使用 Kafka 发送（异步处理，高吞吐）
        if (kafkaEnabled && telemetryProducer != null) {
            telemetryProducer.sendTelemetry(deviceId, ts, powerW, voltageV, currentA);
            // Kafka 消费者会处理 WebSocket 通知和告警检查
        } else {
            // 降级：直接写入数据库（低吞吐场景）
            Telemetry telemetry = new Telemetry();
            telemetry.setDeviceId(deviceId);
            telemetry.setTs(ts);
            telemetry.setPowerW(powerW);
            telemetry.setVoltageV(voltageV);
            telemetry.setCurrentA(currentA);
            telemetryRepository.save(telemetry);

            // 检查告警
            alertService.checkAndGenerateAlert(deviceId, "power", powerW);
            alertService.checkAndGenerateAlert(deviceId, "voltage", voltageV);
            alertService.checkAndGenerateAlert(deviceId, "current", currentA);

            deviceRepository.findById(deviceId).ifPresent(device -> {
                device.setLastSeenTs(now);
                deviceRepository.save(device);
            });

            // 直接 WebSocket 通知
            webSocketNotificationService.broadcastTelemetry(deviceId, payload);
        }
    }

    private void handleAckMessage(String deviceId, JsonNode payload) {
        System.out.println("Handling ack message for device: " + deviceId);

        String cmdId = payload.has("cmdId") ? payload.get("cmdId").asText() : "";
        String status = payload.has("status") ? payload.get("status").asText() : "success";
        String message = payload.has("message") ? payload.get("message").asText() : "";

        // 优先使用 Kafka 异步处理命令确认
        if (kafkaEnabled && commandKafkaEnabled && commandAckProducer != null) {
            CommandAckMessage ackMessage = new CommandAckMessage();
            ackMessage.setDeviceId(deviceId);
            ackMessage.setCmdId(cmdId);
            ackMessage.setStatus(status);
            ackMessage.setMessage(message);

            if (payload.has("socket")) {
                ackMessage.setSocket(payload.get("socket").asInt());
            }
            if (payload.has("state")) {
                ackMessage.setSocketState(payload.get("state").asText());
            }

            commandAckProducer.sendCommandAck(ackMessage);
            return;
        }

        // 降级：直接处理
        commandService.updateCommandState(cmdId, status, message);

        if (payload.has("socket") && payload.has("state")) {
            int socketId = payload.get("socket").asInt();

            StripStatus stripStatus = stripStatusRepository.findByDeviceId(deviceId);
            if (stripStatus != null) {
                try {
                    JsonNode sockets = objectMapper.readTree(stripStatus.getSocketsJson());
                    if (sockets.isArray() && socketId > 0 && socketId <= sockets.size()) {
                        ((com.fasterxml.jackson.databind.node.ArrayNode) sockets).get(socketId - 1);
                    }
                    stripStatusRepository.save(stripStatus);
                } catch (Exception e) {
                    System.err.println("Error updating socket status: " + e.getMessage());
                }
            }
        }

        webSocketNotificationService.broadcastCommandResult(cmdId, status, payload);
    }

    private void handleEventMessage(String deviceId, JsonNode payload) {
        System.out.println("Handling event message for device: " + deviceId);
        
        webSocketNotificationService.broadcastAlert(deviceId, payload);
    }

    private TopicInfo parseTopic(String topic) {
        String prefix = mqttConfig.getTopicPrefix();
        if (!topic.startsWith(prefix + "/")) {
            return null;
        }

        String[] parts = topic.substring(prefix.length() + 1).split("/");
        
        if (parts.length == 2) {
            return new TopicInfo(parts[0], parts[1]);
        }
        
        if (parts.length == 3) {
            return new TopicInfo(parts[0] + "/" + parts[1], parts[2]);
        }

        return null;
    }

    public boolean publishCommand(String deviceId, Object payload) {
        if (!connected || mqttClient == null || !mqttClient.isConnected()) {
            System.err.println("MQTT not connected, cannot publish command");
            return false;
        }

        try {
            String topic = mqttConfig.buildTopic(deviceId, "cmd");
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            MqttMessage message = new MqttMessage(jsonPayload.getBytes());
            message.setQos(1);
            mqttClient.publish(topic, message);
            
            System.out.println("Published command to topic " + topic + ": " + jsonPayload);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to publish command: " + e.getMessage());
            return false;
        }
    }

    public boolean isConnected() {
        return connected && mqttClient != null && mqttClient.isConnected();
    }

    private static class TopicInfo {
        final String deviceId;
        final String msgType;

        TopicInfo(String deviceId, String msgType) {
            this.deviceId = deviceId;
            this.msgType = msgType;
        }
    }
}
