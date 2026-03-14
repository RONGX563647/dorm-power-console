package com.dormpower.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * WebSocket处理器单元测试
 *
 * 测试用例覆盖：
 * - TC-WS-001: 正常连接
 * - TC-WS-002: 订阅设备
 * - TC-WS-003: 接收推送
 * - TC-WS-004: 心跳检测
 * - TC-WS-005: 取消订阅
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class WebSocketHandlerTest {

    private WebSocketHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        handler = new WebSocketHandler();
        objectMapper = new ObjectMapper();

        // 重置WebSocketManager实例以确保测试隔离
        resetWebSocketManager();
    }

    /**
     * 重置WebSocketManager单例实例
     */
    private void resetWebSocketManager() {
        try {
            var field = WebSocketManager.class.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            // 忽略重置失败
        }
    }

    /**
     * 创建模拟的WebSocketSession
     */
    private WebSocketSession createMockSession(String sessionId) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        lenient().when(session.getId()).thenReturn(sessionId);
        lenient().when(session.isOpen()).thenReturn(true);
        return session;
    }

    /**
     * 解析TextMessage为Map
     */
    private Map<String, Object> parseMessage(TextMessage message) throws Exception {
        return objectMapper.readValue(message.getPayload(), new TypeReference<Map<String, Object>>() {});
    }

    // ==================== TC-WS-001: 正常连接 ====================

    @Nested
    @DisplayName("TC-WS-001: 正常连接")
    class ConnectionTests {

        @Test
        @DisplayName("TC-WS-001: WebSocket连接成功，返回连接成功消息")
        void testAfterConnectionEstablished_Success_ReturnsConnectedMessage() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session-001");

            // When
            handler.afterConnectionEstablished(session);

            // Then
            ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
            verify(session).sendMessage(captor.capture());

            Map<String, Object> response = parseMessage(captor.getValue());
            assertEquals("connection", response.get("type"));
            assertEquals("connected", response.get("status"));
            assertEquals("session-001", response.get("sessionId"));
        }

        @Test
        @DisplayName("TC-WS-001: 连接建立后会话被添加到管理器")
        void testAfterConnectionEstablished_SessionAddedToManager() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session-001");

            // When
            handler.afterConnectionEstablished(session);

            // Then
            WebSocketManager manager = WebSocketManager.getInstance();
            assertEquals(1, manager.getSessionCount());
        }

        @Test
        @DisplayName("TC-WS-001: 多个客户端连接，都会收到连接成功消息")
        void testAfterConnectionEstablished_MultipleConnections() throws Exception {
            // Given
            WebSocketSession session1 = createMockSession("session-001");
            WebSocketSession session2 = createMockSession("session-002");

            // When
            handler.afterConnectionEstablished(session1);
            handler.afterConnectionEstablished(session2);

            // Then
            verify(session1).sendMessage(any(TextMessage.class));
            verify(session2).sendMessage(any(TextMessage.class));

            WebSocketManager manager = WebSocketManager.getInstance();
            assertEquals(2, manager.getSessionCount());
        }

        @Test
        @DisplayName("TC-WS-001: 连接关闭后从管理器移除")
        void testAfterConnectionClosed_SessionRemovedFromManager() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session-001");
            handler.afterConnectionEstablished(session);

            // When
            handler.afterConnectionClosed(session, org.springframework.web.socket.CloseStatus.NORMAL);

            // Then
            WebSocketManager manager = WebSocketManager.getInstance();
            assertEquals(0, manager.getSessionCount());
        }
    }

    // ==================== TC-WS-002: 订阅设备 ====================

    @Nested
    @DisplayName("TC-WS-002: 订阅设备")
    class SubscribeTests {

        @Test
        @DisplayName("TC-WS-002: subscribe请求，返回订阅成功")
        void testHandleSubscribe_Success_ReturnsSubscribeSuccess() throws Exception {
            // Given
            String deviceId = "device_001";
            WebSocketSession session = createMockSession("session-001");

            handler.afterConnectionEstablished(session);

            Map<String, Object> request = new HashMap<>();
            request.put("type", "subscribe");
            request.put("deviceId", deviceId);
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(request));

            // When
            handler.handleTextMessage(session, message);

            // Then - 验证收到了订阅响应
            ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
            verify(session, atLeast(1)).sendMessage(captor.capture());

            // 找到订阅响应消息
            Map<String, Object> response = parseMessage(captor.getAllValues().get(captor.getAllValues().size() - 1));
            assertEquals("subscribe", response.get("type"));
            assertEquals("success", response.get("status"));
            assertEquals(deviceId, response.get("deviceId"));
        }

        @Test
        @DisplayName("TC-WS-002: 订阅成功后，会话被添加到设备订阅列表")
        void testHandleSubscribe_SessionAddedToDeviceSubscribers() throws Exception {
            // Given
            String deviceId = "device_001";
            WebSocketSession session = createMockSession("session-001");
            handler.afterConnectionEstablished(session);

            Map<String, Object> request = new HashMap<>();
            request.put("type", "subscribe");
            request.put("deviceId", deviceId);
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(request));

            // When
            handler.handleTextMessage(session, message);

            // Then
            WebSocketManager manager = WebSocketManager.getInstance();
            assertEquals(1, manager.getSubscriberCount(deviceId));
        }

        @Test
        @DisplayName("TC-WS-002: 订阅多个设备，都成功添加")
        void testHandleSubscribe_MultipleDevices() throws Exception {
            // Given
            String deviceId1 = "device_001";
            String deviceId2 = "device_002";
            WebSocketSession session = createMockSession("session-001");
            handler.afterConnectionEstablished(session);

            Map<String, Object> request1 = new HashMap<>();
            request1.put("type", "subscribe");
            request1.put("deviceId", deviceId1);

            Map<String, Object> request2 = new HashMap<>();
            request2.put("type", "subscribe");
            request2.put("deviceId", deviceId2);

            // When
            handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(request1)));
            handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(request2)));

            // Then
            WebSocketManager manager = WebSocketManager.getInstance();
            assertEquals(1, manager.getSubscriberCount(deviceId1));
            assertEquals(1, manager.getSubscriberCount(deviceId2));
        }

        @Test
        @DisplayName("TC-WS-002: 缺少deviceId参数，返回错误消息")
        void testHandleSubscribe_MissingDeviceId_ReturnsError() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session-001");
            handler.afterConnectionEstablished(session);

            Map<String, Object> request = new HashMap<>();
            request.put("type", "subscribe");
            // 不设置deviceId
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(request));

            // When
            handler.handleTextMessage(session, message);

            // Then
            ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
            verify(session, atLeast(1)).sendMessage(captor.capture());

            Map<String, Object> response = parseMessage(captor.getAllValues().get(captor.getAllValues().size() - 1));
            assertEquals("error", response.get("type"));
            assertTrue(response.get("message").toString().contains("Device ID is required"));
        }
    }

    // ==================== TC-WS-003: 接收推送 ====================

    @Nested
    @DisplayName("TC-WS-003: 接收推送")
    class PushNotificationTests {

        @Test
        @DisplayName("TC-WS-003: 设备状态变化，订阅者收到推送消息")
        void testSendToDeviceSubscribers_SubscribersReceiveMessage() throws Exception {
            // Given
            String deviceId = "device_001";
            WebSocketSession session = createMockSession("session-001");
            handler.afterConnectionEstablished(session);

            // 订阅设备
            Map<String, Object> subscribeRequest = new HashMap<>();
            subscribeRequest.put("type", "subscribe");
            subscribeRequest.put("deviceId", deviceId);
            handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(subscribeRequest)));

            // When - 模拟设备状态变化推送
            String pushMessage = "{\"type\":\"device_status\",\"deviceId\":\"device_001\",\"status\":\"online\"}";
            WebSocketManager.getInstance().sendToDeviceSubscribers(deviceId, pushMessage);

            // Then
            ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
            verify(session, atLeast(1)).sendMessage(captor.capture());

            // 找到推送消息
            boolean foundPushMessage = false;
            for (TextMessage msg : captor.getAllValues()) {
                Map<String, Object> response = parseMessage(msg);
                if ("device_status".equals(response.get("type"))) {
                    assertEquals(deviceId, response.get("deviceId"));
                    foundPushMessage = true;
                    break;
                }
            }
            assertTrue(foundPushMessage, "应该收到设备状态推送消息");
        }

        @Test
        @DisplayName("TC-WS-003: 未订阅设备的会话不会收到推送")
        void testSendToDeviceSubscribers_UnsubscribedSessionDoesNotReceive() throws Exception {
            // Given
            String deviceId = "device_001";
            WebSocketSession subscribedSession = createMockSession("session-001");
            WebSocketSession unsubscribedSession = createMockSession("session-002");

            handler.afterConnectionEstablished(subscribedSession);
            handler.afterConnectionEstablished(unsubscribedSession);

            Map<String, Object> subscribeRequest = new HashMap<>();
            subscribeRequest.put("type", "subscribe");
            subscribeRequest.put("deviceId", deviceId);
            handler.handleTextMessage(subscribedSession, new TextMessage(objectMapper.writeValueAsString(subscribeRequest)));

            // When
            String pushMessage = "{\"type\":\"device_status\",\"deviceId\":\"device_001\"}";
            WebSocketManager.getInstance().sendToDeviceSubscribers(deviceId, pushMessage);

            // Then
            verify(subscribedSession, atLeast(1)).sendMessage(any(TextMessage.class));
            // unsubscribedSession 只收到连接消息，不应该收到推送
            verify(unsubscribedSession, times(1)).sendMessage(any(TextMessage.class)); // 只有连接消息
        }

        @Test
        @DisplayName("TC-WS-003: 广播消息所有会话都能收到")
        void testBroadcast_AllSessionsReceive() throws Exception {
            // Given
            WebSocketSession session1 = createMockSession("session-001");
            WebSocketSession session2 = createMockSession("session-002");

            handler.afterConnectionEstablished(session1);
            handler.afterConnectionEstablished(session2);

            // When
            String broadcastMessage = "{\"type\":\"system\",\"message\":\"server restart\"}";
            WebSocketManager.getInstance().broadcast(broadcastMessage);

            // Then
            verify(session1, atLeast(1)).sendMessage(any(TextMessage.class));
            verify(session2, atLeast(1)).sendMessage(any(TextMessage.class));
        }
    }

    // ==================== TC-WS-004: 心跳检测 ====================

    @Nested
    @DisplayName("TC-WS-004: 心跳检测")
    class HeartbeatTests {

        @Test
        @DisplayName("TC-WS-004: ping消息，返回pong消息")
        void testHandlePing_ReturnsPong() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session-001");
            handler.afterConnectionEstablished(session);

            Map<String, Object> request = new HashMap<>();
            request.put("type", "ping");
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(request));

            // When
            handler.handleTextMessage(session, message);

            // Then
            ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
            verify(session, atLeast(1)).sendMessage(captor.capture());

            // 找到pong响应
            boolean foundPong = false;
            for (TextMessage msg : captor.getAllValues()) {
                Map<String, Object> response = parseMessage(msg);
                if ("pong".equals(response.get("type"))) {
                    assertTrue(response.containsKey("timestamp"));
                    foundPong = true;
                    break;
                }
            }
            assertTrue(foundPong, "应该收到pong响应");
        }

        @Test
        @DisplayName("TC-WS-004: pong消息包含时间戳")
        void testHandlePong_ContainsTimestamp() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session-001");
            handler.afterConnectionEstablished(session);

            Map<String, Object> request = new HashMap<>();
            request.put("type", "ping");
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(request));

            long beforeTime = System.currentTimeMillis();

            // When
            handler.handleTextMessage(session, message);

            // Then
            ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
            verify(session, atLeast(1)).sendMessage(captor.capture());

            // 找到pong响应并验证时间戳
            for (TextMessage msg : captor.getAllValues()) {
                Map<String, Object> response = parseMessage(msg);
                if ("pong".equals(response.get("type"))) {
                    Long timestamp = ((Number) response.get("timestamp")).longValue();
                    assertTrue(timestamp >= beforeTime && timestamp <= System.currentTimeMillis());
                    break;
                }
            }
        }

        @Test
        @DisplayName("TC-WS-004: 连续ping都能正常响应")
        void testHandlePing_MultiplePings() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session-001");
            handler.afterConnectionEstablished(session);

            Map<String, Object> request = new HashMap<>();
            request.put("type", "ping");
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(request));

            // When - 连续发送3次ping
            for (int i = 0; i < 3; i++) {
                handler.handleTextMessage(session, message);
            }

            // Then - 验证收到多个消息
            ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
            verify(session, atLeast(4)).sendMessage(captor.capture()); // 连接 + 3个pong

            // 验证所有pong响应
            int pongCount = 0;
            for (TextMessage msg : captor.getAllValues()) {
                Map<String, Object> response = parseMessage(msg);
                if ("pong".equals(response.get("type"))) {
                    pongCount++;
                }
            }
            assertEquals(3, pongCount, "应该收到3个pong响应");
        }
    }

    // ==================== TC-WS-005: 取消订阅 ====================

    @Nested
    @DisplayName("TC-WS-005: 取消订阅")
    class UnsubscribeTests {

        @Test
        @DisplayName("TC-WS-005: unsubscribe请求，取消订阅成功")
        void testHandleUnsubscribe_Success_ReturnsUnsubscribeSuccess() throws Exception {
            // Given
            String deviceId = "device_001";
            WebSocketSession session = createMockSession("session-001");
            handler.afterConnectionEstablished(session);

            // 先订阅
            Map<String, Object> subscribeRequest = new HashMap<>();
            subscribeRequest.put("type", "subscribe");
            subscribeRequest.put("deviceId", deviceId);
            handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(subscribeRequest)));

            // When - 取消订阅
            Map<String, Object> unsubscribeRequest = new HashMap<>();
            unsubscribeRequest.put("type", "unsubscribe");
            unsubscribeRequest.put("deviceId", deviceId);
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(unsubscribeRequest));
            handler.handleTextMessage(session, message);

            // Then
            ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
            verify(session, atLeast(1)).sendMessage(captor.capture());

            // 找到取消订阅响应
            boolean foundUnsubscribeResponse = false;
            for (TextMessage msg : captor.getAllValues()) {
                Map<String, Object> response = parseMessage(msg);
                if ("unsubscribe".equals(response.get("type"))) {
                    assertEquals("success", response.get("status"));
                    assertEquals(deviceId, response.get("deviceId"));
                    foundUnsubscribeResponse = true;
                    break;
                }
            }
            assertTrue(foundUnsubscribeResponse, "应该收到取消订阅响应");
        }

        @Test
        @DisplayName("TC-WS-005: 取消订阅后不再收到该设备的推送")
        void testHandleUnsubscribe_NoLongerReceivesPush() throws Exception {
            // Given
            String deviceId = "device_001";
            WebSocketSession session = createMockSession("session-001");
            handler.afterConnectionEstablished(session);

            // 先订阅
            Map<String, Object> subscribeRequest = new HashMap<>();
            subscribeRequest.put("type", "subscribe");
            subscribeRequest.put("deviceId", deviceId);
            handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(subscribeRequest)));

            // 取消订阅
            Map<String, Object> unsubscribeRequest = new HashMap<>();
            unsubscribeRequest.put("type", "unsubscribe");
            unsubscribeRequest.put("deviceId", deviceId);
            handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(unsubscribeRequest)));

            // 清除之前的调用记录
            clearInvocations(session);

            // When - 发送设备推送
            String pushMessage = "{\"type\":\"device_status\",\"deviceId\":\"device_001\"}";
            WebSocketManager.getInstance().sendToDeviceSubscribers(deviceId, pushMessage);

            // Then - 不应该收到推送
            verify(session, never()).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("TC-WS-005: 取消订阅不指定deviceId，取消所有订阅")
        void testHandleUnsubscribe_NoDeviceId_UnsubscribesAll() throws Exception {
            // Given
            String deviceId1 = "device_001";
            String deviceId2 = "device_002";
            WebSocketSession session = createMockSession("session-001");
            handler.afterConnectionEstablished(session);

            // 订阅两个设备
            Map<String, Object> subscribeRequest1 = new HashMap<>();
            subscribeRequest1.put("type", "subscribe");
            subscribeRequest1.put("deviceId", deviceId1);
            handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(subscribeRequest1)));

            Map<String, Object> subscribeRequest2 = new HashMap<>();
            subscribeRequest2.put("type", "subscribe");
            subscribeRequest2.put("deviceId", deviceId2);
            handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(subscribeRequest2)));

            // When - 取消所有订阅（不指定deviceId）
            Map<String, Object> unsubscribeRequest = new HashMap<>();
            unsubscribeRequest.put("type", "unsubscribe");
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(unsubscribeRequest));
            handler.handleTextMessage(session, message);

            // Then
            WebSocketManager manager = WebSocketManager.getInstance();
            assertEquals(0, manager.getSubscriberCount(deviceId1));
            assertEquals(0, manager.getSubscriberCount(deviceId2));
        }

        @Test
        @DisplayName("TC-WS-005: 取消未订阅的设备，仍返回成功")
        void testHandleUnsubscribe_NotSubscribed_ReturnsSuccess() throws Exception {
            // Given
            String deviceId = "device_not_subscribed";
            WebSocketSession session = createMockSession("session-001");
            handler.afterConnectionEstablished(session);

            Map<String, Object> unsubscribeRequest = new HashMap<>();
            unsubscribeRequest.put("type", "unsubscribe");
            unsubscribeRequest.put("deviceId", deviceId);
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(unsubscribeRequest));

            // When
            handler.handleTextMessage(session, message);

            // Then
            ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
            verify(session, atLeast(1)).sendMessage(captor.capture());

            Map<String, Object> response = parseMessage(captor.getAllValues().get(captor.getAllValues().size() - 1));
            assertEquals("unsubscribe", response.get("type"));
            assertEquals("success", response.get("status"));
        }
    }

    // ==================== 错误处理测试 ====================

    @Nested
    @DisplayName("错误处理测试")
    class ErrorHandlingTests {

        @Test
        @DisplayName("未知消息类型，返回错误消息")
        void testHandleUnknownType_ReturnsError() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session-001");
            handler.afterConnectionEstablished(session);

            Map<String, Object> request = new HashMap<>();
            request.put("type", "unknown_type");
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(request));

            // When
            handler.handleTextMessage(session, message);

            // Then
            ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
            verify(session, atLeast(1)).sendMessage(captor.capture());

            Map<String, Object> response = parseMessage(captor.getAllValues().get(captor.getAllValues().size() - 1));
            assertEquals("error", response.get("type"));
            assertTrue(response.get("message").toString().contains("Unknown message type"));
        }

        @Test
        @DisplayName("无效JSON消息，返回错误消息")
        void testHandleInvalidJson_ReturnsError() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session-001");
            handler.afterConnectionEstablished(session);

            TextMessage invalidMessage = new TextMessage("not a valid json");

            // When
            handler.handleTextMessage(session, invalidMessage);

            // Then
            ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
            verify(session, atLeast(1)).sendMessage(captor.capture());

            Map<String, Object> response = parseMessage(captor.getAllValues().get(captor.getAllValues().size() - 1));
            assertEquals("error", response.get("type"));
            assertTrue(response.get("message").toString().contains("Failed to parse"));
        }

        @Test
        @DisplayName("消息缺少type字段，返回错误消息")
        void testHandleMissingType_ReturnsError() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session-001");
            handler.afterConnectionEstablished(session);

            Map<String, Object> request = new HashMap<>();
            request.put("deviceId", "device_001");
            // 不设置type
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(request));

            // When
            handler.handleTextMessage(session, message);

            // Then
            ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
            verify(session, atLeast(1)).sendMessage(captor.capture());

            Map<String, Object> response = parseMessage(captor.getAllValues().get(captor.getAllValues().size() - 1));
            assertEquals("error", response.get("type"));
        }
    }
}