package com.dormpower.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 消息聚合器
 *
 * 功能：
 * 1. 聚合同一设备的多次更新，减少发送次数
 * 2. 每 100ms 刷新一次聚合的消息
 * 3. 降低高频更新场景下的网络开销
 *
 * @author dormpower team
 * @version 1.0
 */
@Component
public class MessageAggregator {

    private static final Logger logger = LoggerFactory.getLogger(MessageAggregator.class);

    private final WebSocketManager webSocketManager = WebSocketManager.getInstance();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 待发送消息：deviceId -> 最新消息
    private final ConcurrentHashMap<String, AggregatedMessage> pendingMessages = new ConcurrentHashMap<>();

    // 是否启用消息聚合
    private volatile boolean enabled = true;

    /**
     * 添加遥测消息到聚合队列
     */
    public void addTelemetryMessage(String deviceId, Map<String, Object> data) {
        if (!enabled) {
            // 未启用聚合，直接发送
            sendDirectly(deviceId, "TELEMETRY", data);
            return;
        }

        AggregatedMessage message = new AggregatedMessage();
        message.deviceId = deviceId;
        message.type = "TELEMETRY";
        message.data = data;

        pendingMessages.put(deviceId + ":TELEMETRY", message);
    }

    /**
     * 添加设备状态消息到聚合队列
     */
    public void addStatusMessage(String deviceId, Map<String, Object> data) {
        if (!enabled) {
            sendDirectly(deviceId, "DEVICE_STATUS", data);
            return;
        }

        AggregatedMessage message = new AggregatedMessage();
        message.deviceId = deviceId;
        message.type = "DEVICE_STATUS";
        message.data = data;

        pendingMessages.put(deviceId + ":DEVICE_STATUS", message);
    }

    /**
     * 添加告警消息（告警不聚合，直接发送）
     */
    public void addAlertMessage(String deviceId, Map<String, Object> data) {
        sendDirectly(deviceId, "ALERT", data);
    }

    /**
     * 定时刷新聚合的消息（每 100ms）
     */
    @Scheduled(fixedRate = 100)
    public void flushAggregatedMessages() {
        if (pendingMessages.isEmpty()) {
            return;
        }

        // 取出所有待发送消息
        Map<String, AggregatedMessage> toSend = new HashMap<>(pendingMessages);
        pendingMessages.clear();

        if (toSend.isEmpty()) {
            return;
        }

        logger.debug("Flushing {} aggregated messages", toSend.size());

        // 按设备分组发送
        Map<String, List<AggregatedMessage>> byDevice = new HashMap<>();
        toSend.values().forEach(msg -> {
            byDevice.computeIfAbsent(msg.deviceId, k -> new ArrayList<>()).add(msg);
        });

        // 发送每个设备的消息
        byDevice.forEach((deviceId, messages) -> {
            if (messages.size() == 1) {
                // 单条消息直接发送
                AggregatedMessage msg = messages.get(0);
                sendDirectly(msg.deviceId, msg.type, msg.data);
            } else {
                // 多条消息合并发送
                sendAggregated(deviceId, messages);
            }
        });
    }

    /**
     * 直接发送消息（不走聚合）
     */
    private void sendDirectly(String deviceId, String type, Map<String, Object> data) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", type);
            message.put("deviceId", deviceId);
            message.put("payload", data);
            message.put("timestamp", System.currentTimeMillis());

            String json = objectMapper.writeValueAsString(message);
            webSocketManager.sendToDeviceSubscribers(deviceId, json);
        } catch (Exception e) {
            logger.error("Failed to send message directly: {}", e.getMessage());
        }
    }

    /**
     * 发送聚合后的消息
     */
    private void sendAggregated(String deviceId, List<AggregatedMessage> messages) {
        try {
            Map<String, Object> aggregated = new HashMap<>();
            aggregated.put("type", "AGGREGATED");
            aggregated.put("deviceId", deviceId);
            aggregated.put("count", messages.size());
            aggregated.put("timestamp", System.currentTimeMillis());

            // 合并数据（取最新的值）
            Map<String, Object> mergedData = new HashMap<>();
            for (AggregatedMessage msg : messages) {
                mergedData.putAll(msg.data);
            }
            aggregated.put("payload", mergedData);

            String json = objectMapper.writeValueAsString(aggregated);
            webSocketManager.sendToDeviceSubscribers(deviceId, json);
        } catch (Exception e) {
            logger.error("Failed to send aggregated message: {}", e.getMessage());
        }
    }

    /**
     * 获取待发送消息数量
     */
    public int getPendingCount() {
        return pendingMessages.size();
    }

    /**
     * 启用/禁用消息聚合
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        logger.info("Message aggregation enabled: {}", enabled);
    }

    /**
     * 聚合消息结构
     */
    private static class AggregatedMessage {
        String deviceId;
        String type;
        Map<String, Object> data;
    }
}