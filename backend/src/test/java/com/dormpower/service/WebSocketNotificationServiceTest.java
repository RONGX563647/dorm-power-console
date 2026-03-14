package com.dormpower.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dormpower.websocket.WebSocketManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * WebSocket通知服务单元测试
 *
 * 测试用例覆盖：
 * - TC-WS-001: 设备状态更新通知
 * - TC-WS-002: 遥测数据更新通知
 * - TC-WS-003: 命令执行结果通知
 * - TC-WS-004: 异常告警通知
 * - TC-WS-005: 系统广播通知
 * - TC-WS-006: 广播设备状态（JsonNode）
 * - TC-WS-007: 广播遥测数据（JsonNode）
 * - TC-WS-008: 广播命令结果（JsonNode）
 * - TC-WS-009: 广播告警通知
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class WebSocketNotificationServiceTest {

    @Mock
    private WebSocketManager webSocketManager;

    private WebSocketNotificationService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        service = new WebSocketNotificationService();
        // 使用反射注入 mock 的 WebSocketManager
        var field = WebSocketNotificationService.class.getDeclaredField("webSocketManager");
        field.setAccessible(true);
        field.set(service, webSocketManager);
    }

    // ==================== TC-WS-001: 设备状态更新通知 ====================

    @Nested
    @DisplayName("TC-WS-001: 设备状态更新通知")
    class NotifyDeviceStatusUpdateTests {

        @Test
        @DisplayName("TC-WS-001-01: 发送设备状态更新通知成功")
        void testNotifyDeviceStatusUpdate_Success() {
            // Given
            String deviceId = "device_001";
            Map<String, Object> status = new HashMap<>();
            status.put("online", true);
            status.put("power", 100.5);

            // When
            service.notifyDeviceStatusUpdate(deviceId, status);

            // Then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(webSocketManager).sendToDeviceSubscribers(eq(deviceId), messageCaptor.capture());

            String message = messageCaptor.getValue();
            assertTrue(message.contains("\"type\":\"device_status_update\""));
            assertTrue(message.contains("\"deviceId\":\"device_001\""));
            assertTrue(message.contains("\"online\":true"));
            assertTrue(message.contains("\"power\":100.5"));
            assertTrue(message.contains("\"timestamp\""));
        }

        @Test
        @DisplayName("TC-WS-001-02: 空状态数据仍能发送")
        void testNotifyDeviceStatusUpdate_EmptyStatus() {
            // Given
            String deviceId = "device_002";
            Map<String, Object> status = new HashMap<>();

            // When
            service.notifyDeviceStatusUpdate(deviceId, status);

            // Then
            verify(webSocketManager).sendToDeviceSubscribers(eq(deviceId), any(String.class));
        }

        @Test
        @DisplayName("TC-WS-001-03: WebSocket异常不影响业务流程")
        void testNotifyDeviceStatusUpdate_WebSocketException_NoThrow() {
            // Given
            String deviceId = "device_003";
            Map<String, Object> status = new HashMap<>();
            status.put("online", true);

            doThrow(new RuntimeException("WebSocket connection error"))
                    .when(webSocketManager).sendToDeviceSubscribers(any(), any());

            // When & Then - 不应抛出异常
            assertDoesNotThrow(() -> service.notifyDeviceStatusUpdate(deviceId, status));
        }
    }

    // ==================== TC-WS-002: 遥测数据更新通知 ====================

    @Nested
    @DisplayName("TC-WS-002: 遥测数据更新通知")
    class NotifyTelemetryUpdateTests {

        @Test
        @DisplayName("TC-WS-002-01: 发送遥测数据更新通知成功")
        void testNotifyTelemetryUpdate_Success() {
            // Given
            String deviceId = "device_001";
            Map<String, Object> telemetry = new HashMap<>();
            telemetry.put("voltage", 220.0);
            telemetry.put("current", 0.5);
            telemetry.put("power", 110.0);

            // When
            service.notifyTelemetryUpdate(deviceId, telemetry);

            // Then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(webSocketManager).sendToDeviceSubscribers(eq(deviceId), messageCaptor.capture());

            String message = messageCaptor.getValue();
            assertTrue(message.contains("\"type\":\"telemetry_update\""));
            assertTrue(message.contains("\"voltage\":220.0"));
            assertTrue(message.contains("\"current\":0.5"));
        }

        @Test
        @DisplayName("TC-WS-002-02: 嵌套数据结构正确序列化")
        void testNotifyTelemetryUpdate_NestedData() {
            // Given
            String deviceId = "device_001";
            Map<String, Object> telemetry = new HashMap<>();
            Map<String, Object> sockets = new HashMap<>();
            sockets.put("socket1", true);
            sockets.put("socket2", false);
            telemetry.put("sockets", sockets);

            // When
            service.notifyTelemetryUpdate(deviceId, telemetry);

            // Then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(webSocketManager).sendToDeviceSubscribers(eq(deviceId), messageCaptor.capture());

            String message = messageCaptor.getValue();
            assertTrue(message.contains("\"socket1\":true"));
            assertTrue(message.contains("\"socket2\":false"));
        }
    }

    // ==================== TC-WS-003: 命令执行结果通知 ====================

    @Nested
    @DisplayName("TC-WS-003: 命令执行结果通知")
    class NotifyCommandResultTests {

        @Test
        @DisplayName("TC-WS-003-01: 发送命令执行结果通知成功")
        void testNotifyCommandResult_Success() {
            // Given
            String deviceId = "device_001";
            String commandId = "cmd_12345";
            Map<String, Object> result = new HashMap<>();
            result.put("status", "completed");
            result.put("message", "Command executed successfully");

            // When
            service.notifyCommandResult(deviceId, commandId, result);

            // Then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(webSocketManager).sendToDeviceSubscribers(eq(deviceId), messageCaptor.capture());

            String message = messageCaptor.getValue();
            assertTrue(message.contains("\"type\":\"command_result\""));
            assertTrue(message.contains("\"commandId\":\"cmd_12345\""));
            assertTrue(message.contains("\"status\":\"completed\""));
        }

        @Test
        @DisplayName("TC-WS-003-02: 命令失败结果通知")
        void testNotifyCommandResult_Failed() {
            // Given
            String deviceId = "device_001";
            String commandId = "cmd_12346";
            Map<String, Object> result = new HashMap<>();
            result.put("status", "failed");
            result.put("error", "Device timeout");

            // When
            service.notifyCommandResult(deviceId, commandId, result);

            // Then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(webSocketManager).sendToDeviceSubscribers(eq(deviceId), messageCaptor.capture());

            String message = messageCaptor.getValue();
            assertTrue(message.contains("\"status\":\"failed\""));
            assertTrue(message.contains("\"error\":\"Device timeout\""));
        }
    }

    // ==================== TC-WS-004: 异常告警通知 ====================

    @Nested
    @DisplayName("TC-WS-004: 异常告警通知")
    class NotifyAnomalyTests {

        @Test
        @DisplayName("TC-WS-004-01: 发送异常告警通知成功")
        void testNotifyAnomaly_Success() {
            // Given
            String deviceId = "device_001";
            Map<String, Object> anomaly = new HashMap<>();
            anomaly.put("type", "overload");
            anomaly.put("value", 2500.0);
            anomaly.put("threshold", 2000.0);
            anomaly.put("severity", "high");

            // When
            service.notifyAnomaly(deviceId, anomaly);

            // Then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(webSocketManager).sendToDeviceSubscribers(eq(deviceId), messageCaptor.capture());

            String message = messageCaptor.getValue();
            assertTrue(message.contains("\"type\":\"anomaly_alert\""));
            assertTrue(message.contains("\"severity\":\"high\""));
            assertTrue(message.contains("\"overload\""));
        }

        @Test
        @DisplayName("TC-WS-004-02: 低级别告警通知")
        void testNotifyAnomaly_LowSeverity() {
            // Given
            String deviceId = "device_002";
            Map<String, Object> anomaly = new HashMap<>();
            anomaly.put("type", "voltage_fluctuation");
            anomaly.put("severity", "low");

            // When
            service.notifyAnomaly(deviceId, anomaly);

            // Then
            verify(webSocketManager).sendToDeviceSubscribers(eq(deviceId), any(String.class));
        }
    }

    // ==================== TC-WS-005: 系统广播通知 ====================

    @Nested
    @DisplayName("TC-WS-005: 系统广播通知")
    class BroadcastSystemNotificationTests {

        @Test
        @DisplayName("TC-WS-005-01: 广播系统通知成功")
        void testBroadcastSystemNotification_Success() {
            // Given
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", "系统维护通知");
            notification.put("message", "系统将于今晚22:00进行维护");
            notification.put("level", "warning");

            // When
            service.broadcastSystemNotification(notification);

            // Then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(webSocketManager).broadcast(messageCaptor.capture());

            String message = messageCaptor.getValue();
            assertTrue(message.contains("\"type\":\"system_notification\""));
            assertTrue(message.contains("\"系统维护通知\""));
            assertTrue(message.contains("\"level\":\"warning\""));
        }

        @Test
        @DisplayName("TC-WS-005-02: 紧急系统通知")
        void testBroadcastSystemNotification_Urgent() {
            // Given
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", "紧急通知");
            notification.put("level", "critical");

            // When
            service.broadcastSystemNotification(notification);

            // Then
            verify(webSocketManager).broadcast(any(String.class));
        }
    }

    // ==================== TC-WS-006: 广播设备状态（JsonNode） ====================

    @Nested
    @DisplayName("TC-WS-006: 广播设备状态（JsonNode）")
    class BroadcastDeviceStatusTests {

        @Test
        @DisplayName("TC-WS-006-01: 广播设备状态成功")
        void testBroadcastDeviceStatus_Success() throws Exception {
            // Given
            String deviceId = "device_001";
            String json = "{\"online\":true,\"power\":100.5,\"voltage\":220.0}";
            JsonNode status = objectMapper.readTree(json);

            // When
            service.broadcastDeviceStatus(deviceId, status);

            // Then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(webSocketManager).sendToDeviceSubscribers(eq(deviceId), messageCaptor.capture());

            String message = messageCaptor.getValue();
            assertTrue(message.contains("\"type\":\"DEVICE_STATUS\""));
            assertTrue(message.contains("\"deviceId\":\"device_001\""));
            assertTrue(message.contains("\"payload\""));
        }

        @Test
        @DisplayName("TC-WS-006-02: 空JsonNode不抛异常")
        void testBroadcastDeviceStatus_EmptyNode() throws Exception {
            // Given
            String deviceId = "device_002";
            JsonNode status = objectMapper.readTree("{}");

            // When & Then
            assertDoesNotThrow(() -> service.broadcastDeviceStatus(deviceId, status));
        }
    }

    // ==================== TC-WS-007: 广播遥测数据（JsonNode） ====================

    @Nested
    @DisplayName("TC-WS-007: 广播遥测数据（JsonNode）")
    class BroadcastTelemetryTests {

        @Test
        @DisplayName("TC-WS-007-01: 广播遥测数据成功")
        void testBroadcastTelemetry_Success() throws Exception {
            // Given
            String deviceId = "device_001";
            String json = "{\"voltage\":220.0,\"current\":0.5,\"power\":110.0}";
            JsonNode telemetry = objectMapper.readTree(json);

            // When
            service.broadcastTelemetry(deviceId, telemetry);

            // Then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(webSocketManager).sendToDeviceSubscribers(eq(deviceId), messageCaptor.capture());

            String message = messageCaptor.getValue();
            assertTrue(message.contains("\"type\":\"TELEMETRY\""));
            assertTrue(message.contains("\"payload\""));
            assertTrue(message.contains("\"voltage\":220.0"));
        }
    }

    // ==================== TC-WS-008: 广播命令结果（JsonNode） ====================

    @Nested
    @DisplayName("TC-WS-008: 广播命令结果（JsonNode）")
    class BroadcastCommandResultTests {

        @Test
        @DisplayName("TC-WS-008-01: 广播命令成功结果")
        void testBroadcastCommandResult_Success() throws Exception {
            // Given
            String cmdId = "cmd_12345";
            String state = "SUCCESS";
            String json = "{\"message\":\"Command executed\"}";
            JsonNode result = objectMapper.readTree(json);

            // When
            service.broadcastCommandResult(cmdId, state, result);

            // Then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(webSocketManager).broadcast(messageCaptor.capture());

            String message = messageCaptor.getValue();
            assertTrue(message.contains("\"type\":\"CMD_ACK\""));
            assertTrue(message.contains("\"cmdId\":\"cmd_12345\""));
            assertTrue(message.contains("\"state\":\"SUCCESS\""));
        }

        @Test
        @DisplayName("TC-WS-008-02: 广播命令失败结果")
        void testBroadcastCommandResult_Failed() throws Exception {
            // Given
            String cmdId = "cmd_12346";
            String state = "FAILED";
            String json = "{\"error\":\"Device not responding\"}";
            JsonNode result = objectMapper.readTree(json);

            // When
            service.broadcastCommandResult(cmdId, state, result);

            // Then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(webSocketManager).broadcast(messageCaptor.capture());

            String message = messageCaptor.getValue();
            assertTrue(message.contains("\"state\":\"FAILED\""));
            assertTrue(message.contains("\"error\":\"Device not responding\""));
        }

        @Test
        @DisplayName("TC-WS-008-03: 广播命令超时结果")
        void testBroadcastCommandResult_Timeout() throws Exception {
            // Given
            String cmdId = "cmd_12347";
            String state = "TIMEOUT";
            JsonNode result = objectMapper.readTree("{}");

            // When
            service.broadcastCommandResult(cmdId, state, result);

            // Then
            verify(webSocketManager).broadcast(any(String.class));
        }
    }

    // ==================== TC-WS-009: 广播告警通知 ====================

    @Nested
    @DisplayName("TC-WS-009: 广播告警通知")
    class BroadcastAlertTests {

        @Test
        @DisplayName("TC-WS-009-01: 广播告警（JsonNode）成功")
        void testBroadcastAlert_JsonNode_Success() throws Exception {
            // Given
            String deviceId = "device_001";
            String json = "{\"type\":\"overload\",\"value\":2500.0}";
            JsonNode alert = objectMapper.readTree(json);

            // When
            service.broadcastAlert(deviceId, alert);

            // Then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(webSocketManager).sendToDeviceSubscribers(eq(deviceId), messageCaptor.capture());

            String message = messageCaptor.getValue();
            assertTrue(message.contains("\"type\":\"ALERT\""));
            assertTrue(message.contains("\"payload\""));
        }

        @Test
        @DisplayName("TC-WS-009-02: 广播告警（Map）成功")
        void testBroadcastAlert_Map_Success() {
            // Given
            String deviceId = "device_002";
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "overcurrent");
            alert.put("value", 15.0);
            alert.put("threshold", 10.0);

            // When
            service.broadcastAlert(deviceId, alert);

            // Then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(webSocketManager).sendToDeviceSubscribers(eq(deviceId), messageCaptor.capture());

            String message = messageCaptor.getValue();
            assertTrue(message.contains("\"type\":\"ALERT\""));
            assertTrue(message.contains("\"overcurrent\""));
        }

        @Test
        @DisplayName("TC-WS-009-03: 两种告警方法产生相同消息格式")
        void testBroadcastAlert_ConsistentFormat() throws Exception {
            // Given
            String deviceId = "device_003";
            Map<String, Object> alertMap = new HashMap<>();
            alertMap.put("type", "test");
            alertMap.put("value", 100);

            String json = "{\"type\":\"test\",\"value\":100}";
            JsonNode alertNode = objectMapper.readTree(json);

            // When
            service.broadcastAlert(deviceId, alertMap);
            service.broadcastAlert(deviceId, alertNode);

            // Then
            ArgumentCaptor<String> captor1 = ArgumentCaptor.forClass(String.class);

            verify(webSocketManager, times(2)).sendToDeviceSubscribers(eq(deviceId), captor1.capture());

            // 两次调用的消息都应包含相同的关键字段
            String message1 = captor1.getAllValues().get(0);
            String message2 = captor1.getAllValues().get(1);

            assertTrue(message1.contains("\"type\":\"ALERT\""));
            assertTrue(message2.contains("\"type\":\"ALERT\""));
        }
    }

    // ==================== 异常处理测试 ====================

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("WebSocket发送异常不抛出")
        void testWebSocketException_NoThrow() {
            // Given
            doThrow(new RuntimeException("Connection lost"))
                    .when(webSocketManager).sendToDeviceSubscribers(any(), any());

            // When & Then - 所有方法都不应抛出异常
            assertDoesNotThrow(() -> service.notifyDeviceStatusUpdate("device_001", new HashMap<>()));
            assertDoesNotThrow(() -> service.notifyTelemetryUpdate("device_001", new HashMap<>()));
            assertDoesNotThrow(() -> service.notifyCommandResult("device_001", "cmd_001", new HashMap<>()));
            assertDoesNotThrow(() -> service.notifyAnomaly("device_001", new HashMap<>()));
        }

        @Test
        @DisplayName("WebSocket广播异常不抛出")
        void testBroadcastException_NoThrow() {
            // Given
            doThrow(new RuntimeException("Broadcast failed"))
                    .when(webSocketManager).broadcast(any());

            // When & Then
            assertDoesNotThrow(() -> service.broadcastSystemNotification(new HashMap<>()));
        }
    }

    // ==================== 时间戳测试 ====================

    @Nested
    @DisplayName("时间戳测试")
    class TimestampTests {

        @Test
        @DisplayName("消息包含有效时间戳")
        void testMessageContainsValidTimestamp() {
            // Given
            String deviceId = "device_001";
            Map<String, Object> status = new HashMap<>();
            status.put("online", true);

            long beforeTime = System.currentTimeMillis();

            // When
            service.notifyDeviceStatusUpdate(deviceId, status);

            long afterTime = System.currentTimeMillis();

            // Then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(webSocketManager).sendToDeviceSubscribers(eq(deviceId), messageCaptor.capture());

            String message = messageCaptor.getValue();
            assertTrue(message.contains("\"timestamp\""));

            // 提取时间戳值
            int timestampIndex = message.indexOf("\"timestamp\":");
            String timestampPart = message.substring(timestampIndex + 12);
            long timestamp = Long.parseLong(timestampPart.split("[,}]")[0]);

            assertTrue(timestamp >= beforeTime && timestamp <= afterTime,
                    "时间戳应在调用前后时间范围内");
        }
    }
}