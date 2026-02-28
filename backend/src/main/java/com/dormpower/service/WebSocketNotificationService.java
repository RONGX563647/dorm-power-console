package com.dormpower.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dormpower.websocket.WebSocketManager;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket通知服务
 * 用于在业务逻辑中发送WebSocket通知
 */
@Service
public class WebSocketNotificationService {

    private final WebSocketManager webSocketManager = WebSocketManager.getInstance();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送设备状态更新通知
     * @param deviceId 设备ID
     * @param status 设备状态
     */
    public void notifyDeviceStatusUpdate(String deviceId, Map<String, Object> status) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "device_status_update");
            message.put("deviceId", deviceId);
            message.put("data", status);
            message.put("timestamp", System.currentTimeMillis());
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            webSocketManager.sendToDeviceSubscribers(deviceId, jsonMessage);
        } catch (Exception e) {
            System.err.println("Failed to send device status update notification: " + e.getMessage());
        }
    }

    /**
     * 发送遥测数据更新通知
     * @param deviceId 设备ID
     * @param telemetry 遥测数据
     */
    public void notifyTelemetryUpdate(String deviceId, Map<String, Object> telemetry) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "telemetry_update");
            message.put("deviceId", deviceId);
            message.put("data", telemetry);
            message.put("timestamp", System.currentTimeMillis());
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            webSocketManager.sendToDeviceSubscribers(deviceId, jsonMessage);
        } catch (Exception e) {
            System.err.println("Failed to send telemetry update notification: " + e.getMessage());
        }
    }

    /**
     * 发送命令执行结果通知
     * @param deviceId 设备ID
     * @param commandId 命令ID
     * @param result 执行结果
     */
    public void notifyCommandResult(String deviceId, String commandId, Map<String, Object> result) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "command_result");
            message.put("deviceId", deviceId);
            message.put("commandId", commandId);
            message.put("data", result);
            message.put("timestamp", System.currentTimeMillis());
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            webSocketManager.sendToDeviceSubscribers(deviceId, jsonMessage);
        } catch (Exception e) {
            System.err.println("Failed to send command result notification: " + e.getMessage());
        }
    }

    /**
     * 发送异常告警通知
     * @param deviceId 设备ID
     * @param anomaly 异常信息
     */
    public void notifyAnomaly(String deviceId, Map<String, Object> anomaly) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "anomaly_alert");
            message.put("deviceId", deviceId);
            message.put("data", anomaly);
            message.put("timestamp", System.currentTimeMillis());
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            webSocketManager.sendToDeviceSubscribers(deviceId, jsonMessage);
        } catch (Exception e) {
            System.err.println("Failed to send anomaly notification: " + e.getMessage());
        }
    }

    /**
     * 广播系统通知
     * @param notification 通知内容
     */
    public void broadcastSystemNotification(Map<String, Object> notification) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "system_notification");
            message.put("data", notification);
            message.put("timestamp", System.currentTimeMillis());
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            webSocketManager.broadcast(jsonMessage);
        } catch (Exception e) {
            System.err.println("Failed to broadcast system notification: " + e.getMessage());
        }
    }

    /**
     * 广播设备状态更新（支持JsonNode）
     * @param deviceId 设备ID
     * @param status 设备状态
     */
    public void broadcastDeviceStatus(String deviceId, JsonNode status) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "DEVICE_STATUS");
            message.put("deviceId", deviceId);
            message.put("payload", status);
            message.put("timestamp", System.currentTimeMillis());
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            webSocketManager.sendToDeviceSubscribers(deviceId, jsonMessage);
        } catch (Exception e) {
            System.err.println("Failed to broadcast device status: " + e.getMessage());
        }
    }

    /**
     * 广播遥测数据（支持JsonNode）
     * @param deviceId 设备ID
     * @param telemetry 遥测数据
     */
    public void broadcastTelemetry(String deviceId, JsonNode telemetry) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "TELEMETRY");
            message.put("deviceId", deviceId);
            message.put("payload", telemetry);
            message.put("timestamp", System.currentTimeMillis());
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            webSocketManager.sendToDeviceSubscribers(deviceId, jsonMessage);
        } catch (Exception e) {
            System.err.println("Failed to broadcast telemetry: " + e.getMessage());
        }
    }

    /**
     * 广播命令执行结果（支持JsonNode）
     * @param cmdId 命令ID
     * @param state 执行状态
     * @param result 执行结果
     */
    public void broadcastCommandResult(String cmdId, String state, JsonNode result) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "CMD_ACK");
            message.put("cmdId", cmdId);
            message.put("state", state);
            message.put("payload", result);
            message.put("timestamp", System.currentTimeMillis());
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            webSocketManager.broadcast(jsonMessage);
        } catch (Exception e) {
            System.err.println("Failed to broadcast command result: " + e.getMessage());
        }
    }

    /**
     * 广播告警（支持JsonNode）
     * @param deviceId 设备ID
     * @param alert 告警信息
     */
    public void broadcastAlert(String deviceId, JsonNode alert) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "ALERT");
            message.put("deviceId", deviceId);
            message.put("payload", alert);
            message.put("timestamp", System.currentTimeMillis());
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            webSocketManager.sendToDeviceSubscribers(deviceId, jsonMessage);
        } catch (Exception e) {
            System.err.println("Failed to broadcast alert: " + e.getMessage());
        }
    }

}
