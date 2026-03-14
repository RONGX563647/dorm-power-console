package com.dormpower.mqtt;

import com.dormpower.model.CommandRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MQTT命令下发单元测试
 *
 * 测试用例覆盖：
 * - TC-MQTT-001: 正常命令下发（MQTT发布成功）
 * - TC-MQTT-002: 设备确认成功
 * - TC-MQTT-003: 设备确认失败
 * - TC-MQTT-004: MQTT断开
 *
 * @author dormpower team
 * @version 1.0
 */
public class MqttCommandTest {

    // 测试数据常量
    private static final String DEVICE_ID_1 = "device-001";
    private static final String CMD_ID_1 = "cmd-1234567890";

    // 被测对象
    private TestableMqttBridge mqttBridge;

    // 测试替身
    private SimpleMqttClient mqttClient;
    private SimpleCommandRecordStore commandRecordStore;
    private SimpleWebSocketNotifier webSocketNotifier;

    @BeforeEach
    void setUp() {
        mqttClient = new SimpleMqttClient();
        commandRecordStore = new SimpleCommandRecordStore();
        webSocketNotifier = new SimpleWebSocketNotifier();

        mqttBridge = new TestableMqttBridge(
                mqttClient,
                commandRecordStore,
                webSocketNotifier
        );
    }

    /**
     * 可测试的MQTT桥接类
     * 复制原桥接的核心逻辑，使用测试替身
     */
    static class TestableMqttBridge {
        private final SimpleMqttClient mqttClient;
        private final SimpleCommandRecordStore commandRecordStore;
        private final SimpleWebSocketNotifier webSocketNotifier;
        private final ObjectMapper objectMapper = new ObjectMapper();
        private boolean connected = false;

        public TestableMqttBridge(
                SimpleMqttClient mqttClient,
                SimpleCommandRecordStore commandRecordStore,
                SimpleWebSocketNotifier webSocketNotifier) {
            this.mqttClient = mqttClient;
            this.commandRecordStore = commandRecordStore;
            this.webSocketNotifier = webSocketNotifier;
            this.connected = mqttClient.isConnected();
        }

        /**
         * 发布命令
         */
        public boolean publishCommand(String deviceId, Object payload) {
            if (!connected || mqttClient == null || !mqttClient.isConnected()) {
                return false;
            }

            try {
                String jsonPayload = objectMapper.writeValueAsString(payload);
                String topic = "dorm/cmd/" + deviceId;
                mqttClient.publish(topic, jsonPayload);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * 处理ACK消息
         */
        public void handleAckMessage(String deviceId, JsonNode payload) {
            String cmdId = payload.has("cmdId") ? payload.get("cmdId").asText() : "";
            String status = payload.has("status") ? payload.get("status").asText() : "success";
            String message = payload.has("message") ? payload.get("message").asText() : "";

            // 更新命令状态
            CommandRecord cmd = commandRecordStore.findById(cmdId);
            if (cmd != null) {
                cmd.setState(status);
                cmd.setMessage(message);
                cmd.setUpdatedAt(System.currentTimeMillis());
                if ("success".equals(status) || "failed".equals(status)) {
                    cmd.setDurationMs((int) (System.currentTimeMillis() - cmd.getCreatedAt()));
                }
                commandRecordStore.save(cmd);
            }

            // 发送WebSocket通知
            webSocketNotifier.broadcastCommandResult(cmdId, status, payload);
        }

        public boolean isConnected() {
            return connected && mqttClient != null && mqttClient.isConnected();
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
            mqttClient.setConnected(connected);
        }
    }

    /**
     * MQTT命令下发测试
     */
    @Nested
    @DisplayName("MQTT命令下发测试")
    class MqttCommandPublishTests {

        /**
         * TC-MQTT-001: 正常命令下发
         *
         * 测试场景：正常命令下发
         * 输入：有效命令
         * 预期输出：MQTT发布成功
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-MQTT-001: 正常命令下发-MQTT发布成功")
        void testPublishCommand_Success() {
            // Arrange: 设置MQTT连接正常
            mqttBridge.setConnected(true);
            Map<String, Object> payload = new HashMap<>();
            payload.put("cmdId", CMD_ID_1);
            payload.put("action", "toggle");
            payload.put("socket", 1);

            // Act: 发布命令
            boolean result = mqttBridge.publishCommand(DEVICE_ID_1, payload);

            // Assert: 验证MQTT发布成功
            assertTrue(result, "命令发布应成功");
            assertTrue(mqttClient.getLastPublishedTopic().contains(DEVICE_ID_1), "主题应包含设备ID");
            assertNotNull(mqttClient.getLastPublishedPayload(), "应有发布内容");
        }

        /**
         * TC-MQTT-002: 设备确认成功
         *
         * 测试场景：设备确认成功
         * 输入：status=success
         * 预期输出：命令状态更新为success
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-MQTT-002: 设备确认成功-命令状态更新为success")
        void testDeviceAckSuccess_StatusUpdatedToSuccess() {
            // Arrange: 创建待处理命令
            CommandRecord pendingCmd = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "pending");
            commandRecordStore.save(pendingCmd);

            // 构造ACK消息
            String ackJson = "{\"cmdId\":\"" + CMD_ID_1 + "\",\"status\":\"success\",\"message\":\"Command executed\"}";
            JsonNode ackPayload;
            try {
                ackPayload = new ObjectMapper().readTree(ackJson);
            } catch (Exception e) {
                fail("Failed to parse JSON");
                return;
            }

            // Act: 处理ACK消息
            mqttBridge.handleAckMessage(DEVICE_ID_1, ackPayload);

            // Assert: 验证命令状态更新为success
            CommandRecord updatedCmd = commandRecordStore.findById(CMD_ID_1);
            assertNotNull(updatedCmd, "命令应存在");
            assertEquals("success", updatedCmd.getState(), "状态应为success");
            assertEquals("Command executed", updatedCmd.getMessage(), "消息应正确");
            assertNotNull(updatedCmd.getDurationMs(), "执行时长应被设置");

            // 验证WebSocket通知已发送
            assertEquals(1, webSocketNotifier.getCommandResultCount(), "应发送WebSocket通知");
        }

        /**
         * TC-MQTT-003: 设备确认失败
         *
         * 测试场景：设备确认失败
         * 输入：status=failed
         * 预期输出：命令状态更新为failed
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-MQTT-003: 设备确认失败-命令状态更新为failed")
        void testDeviceAckFailed_StatusUpdatedToFailed() {
            // Arrange: 创建待处理命令
            CommandRecord pendingCmd = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "pending");
            commandRecordStore.save(pendingCmd);

            // 构造ACK消息（失败）
            String ackJson = "{\"cmdId\":\"" + CMD_ID_1 + "\",\"status\":\"failed\",\"message\":\"Device error\"}";
            JsonNode ackPayload;
            try {
                ackPayload = new ObjectMapper().readTree(ackJson);
            } catch (Exception e) {
                fail("Failed to parse JSON");
                return;
            }

            // Act: 处理ACK消息
            mqttBridge.handleAckMessage(DEVICE_ID_1, ackPayload);

            // Assert: 验证命令状态更新为failed
            CommandRecord updatedCmd = commandRecordStore.findById(CMD_ID_1);
            assertNotNull(updatedCmd, "命令应存在");
            assertEquals("failed", updatedCmd.getState(), "状态应为failed");
            assertEquals("Device error", updatedCmd.getMessage(), "消息应正确");
            assertNotNull(updatedCmd.getDurationMs(), "执行时长应被设置");
        }

        /**
         * TC-MQTT-004: MQTT断开
         *
         * 测试场景：MQTT断开
         * 输入：连接断开
         * 预期输出：返回false
         * 优先级：P1
         */
        @Test
        @DisplayName("TC-MQTT-004: MQTT断开-返回false")
        void testMqttDisconnected_ReturnsFalse() {
            // Arrange: 设置MQTT断开
            mqttBridge.setConnected(false);
            Map<String, Object> payload = new HashMap<>();
            payload.put("cmdId", CMD_ID_1);
            payload.put("action", "toggle");

            // Act: 尝试发布命令
            boolean result = mqttBridge.publishCommand(DEVICE_ID_1, payload);

            // Assert: 验证返回false
            assertFalse(result, "MQTT断开时应返回false");
            assertNull(mqttClient.getLastPublishedPayload(), "不应有发布内容");
        }
    }

    /**
     * 边界条件测试
     */
    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        /**
         * 测试命令不存在时的ACK处理
         */
        @Test
        @DisplayName("ACK处理-命令不存在")
        void testAckHandling_CommandNotExist() {
            // Arrange: 不创建命令，直接处理ACK
            String ackJson = "{\"cmdId\":\"non_existent_cmd\",\"status\":\"success\",\"message\":\"OK\"}";
            JsonNode ackPayload;
            try {
                ackPayload = new ObjectMapper().readTree(ackJson);
            } catch (Exception e) {
                fail("Failed to parse JSON");
                return;
            }

            // Act: 处理ACK消息（不应抛出异常）
            assertDoesNotThrow(() -> mqttBridge.handleAckMessage(DEVICE_ID_1, ackPayload),
                    "命令不存在时不应抛出异常");

            // 验证WebSocket通知仍会发送
            assertEquals(1, webSocketNotifier.getCommandResultCount(), "仍应发送WebSocket通知");
        }

        /**
         * 测试MQTT重新连接
         */
        @Test
        @DisplayName("MQTT重新连接-命令发布成功")
        void testMqttReconnect_CommandPublishSuccess() {
            // Arrange: 初始断开
            mqttBridge.setConnected(false);
            Map<String, Object> payload = new HashMap<>();
            payload.put("cmdId", CMD_ID_1);
            payload.put("action", "toggle");

            // 验证断开时发布失败
            assertFalse(mqttBridge.publishCommand(DEVICE_ID_1, payload));

            // Act: 重新连接后发布
            mqttBridge.setConnected(true);
            boolean result = mqttBridge.publishCommand(DEVICE_ID_1, payload);

            // Assert: 验证发布成功
            assertTrue(result, "重新连接后应发布成功");
        }

        /**
         * 测试空payload
         */
        @Test
        @DisplayName("空payload-应正常处理")
        void testEmptyPayload_HandledCorrectly() {
            // Arrange
            mqttBridge.setConnected(true);
            Map<String, Object> emptyPayload = new HashMap<>();

            // Act: 发布空payload
            boolean result = mqttBridge.publishCommand(DEVICE_ID_1, emptyPayload);

            // Assert: 应正常处理
            assertTrue(result, "空payload应正常处理");
        }

        /**
         * 测试ACK状态为pending
         */
        @Test
        @DisplayName("ACK状态为pending-状态更新正确")
        void testAckStatusPending_StatusUpdated() {
            // Arrange: 创建待处理命令
            CommandRecord pendingCmd = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "pending");
            commandRecordStore.save(pendingCmd);

            String ackJson = "{\"cmdId\":\"" + CMD_ID_1 + "\",\"status\":\"pending\",\"message\":\"Processing\"}";
            JsonNode ackPayload;
            try {
                ackPayload = new ObjectMapper().readTree(ackJson);
            } catch (Exception e) {
                fail("Failed to parse JSON");
                return;
            }

            // Act: 处理ACK消息
            mqttBridge.handleAckMessage(DEVICE_ID_1, ackPayload);

            // Assert: 状态应更新为pending
            CommandRecord updatedCmd = commandRecordStore.findById(CMD_ID_1);
            assertEquals("pending", updatedCmd.getState(), "状态应为pending");
            assertNull(updatedCmd.getDurationMs(), "pending状态不应设置执行时长");
        }

        /**
         * 测试多次发布命令
         */
        @Test
        @DisplayName("多次发布命令-全部成功")
        void testMultiplePublish_AllSuccess() {
            // Arrange
            mqttBridge.setConnected(true);

            // Act: 发布多个命令
            for (int i = 0; i < 5; i++) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("cmdId", "cmd-" + i);
                payload.put("action", "toggle");
                boolean result = mqttBridge.publishCommand(DEVICE_ID_1, payload);
                assertTrue(result, "命令" + i + "应发布成功");
            }

            // Assert: 验证发布次数
            assertEquals(5, mqttClient.getPublishCount(), "应发布5次");
        }
    }

    // ==================== 辅助方法：创建测试数据 ====================

    /**
     * 创建命令记录实体
     */
    private CommandRecord createCommandRecord(String cmdId, String deviceId, String action, Integer socket, String state) {
        CommandRecord record = new CommandRecord();
        record.setCmdId(cmdId);
        record.setDeviceId(deviceId);
        record.setAction(action);
        record.setSocket(socket);
        record.setState(state);
        record.setMessage("");
        record.setCreatedAt(System.currentTimeMillis());
        record.setUpdatedAt(System.currentTimeMillis());
        record.setExpiresAt(System.currentTimeMillis() + 30000);
        record.setPayloadJson("{\"action\":\"" + action + "\"}");
        return record;
    }

    // ==================== 简单存储类 ====================

    /**
     * 简单MQTT客户端
     */
    static class SimpleMqttClient {
        private boolean connected = true;
        private String lastPublishedTopic;
        private String lastPublishedPayload;
        private int publishCount = 0;

        public boolean isConnected() {
            return connected;
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
        }

        public void publish(String topic, String payload) {
            this.lastPublishedTopic = topic;
            this.lastPublishedPayload = payload;
            this.publishCount++;
        }

        public String getLastPublishedTopic() {
            return lastPublishedTopic;
        }

        public String getLastPublishedPayload() {
            return lastPublishedPayload;
        }

        public int getPublishCount() {
            return publishCount;
        }
    }

    /**
     * 简单命令记录存储
     */
    static class SimpleCommandRecordStore {
        private final Map<String, CommandRecord> records = new HashMap<>();

        public CommandRecord save(CommandRecord record) {
            records.put(record.getCmdId(), record);
            return record;
        }

        public CommandRecord findById(String cmdId) {
            return records.get(cmdId);
        }
    }

    /**
     * 简单WebSocket通知器
     */
    static class SimpleWebSocketNotifier {
        private int commandResultCount = 0;
        private JsonNode lastCommandResult;

        public void broadcastCommandResult(String cmdId, String status, JsonNode payload) {
            commandResultCount++;
            lastCommandResult = payload;
        }

        public int getCommandResultCount() {
            return commandResultCount;
        }

        public JsonNode getLastCommandResult() {
            return lastCommandResult;
        }
    }
}