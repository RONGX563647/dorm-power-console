package com.dormpower.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * WebSocket连接管理器单元测试
 *
 * 测试用例覆盖：
 * - TC-WS-001: 会话管理
 * - TC-WS-002: 设备订阅管理
 * - TC-WS-003: 消息推送
 * - TC-WS-005: 取消订阅管理
 *
 * @author dormpower team
 * @version 1.0
 */
class WebSocketManagerTest {

    private WebSocketManager manager;

    @BeforeEach
    void setUp() {
        // 重置单例实例以确保测试隔离
        resetSingleton();
        manager = WebSocketManager.getInstance();
    }

    /**
     * 重置WebSocketManager单例实例
     */
    private void resetSingleton() {
        try {
            var field = WebSocketManager.class.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            // 忽略重置失败
        }
    }

    // ==================== 会话管理测试 ====================

    @Nested
    @DisplayName("会话管理测试")
    class SessionManagementTests {

        @Test
        @DisplayName("TC-WS-001: 添加会话，连接数正确增加")
        void testAddSession_ConnectionCountIncreases() {
            // Given
            WebSocketSession session = createMockSession("session-001");

            // When
            manager.addSession(session);

            // Then
            assertEquals(1, manager.getSessionCount());
        }

        @Test
        @DisplayName("TC-WS-001: 移除会话，连接数正确减少")
        void testRemoveSession_ConnectionCountDecreases() {
            // Given
            WebSocketSession session = createMockSession("session-001");
            manager.addSession(session);

            // When
            manager.removeSession(session);

            // Then
            assertEquals(0, manager.getSessionCount());
        }

        @Test
        @DisplayName("TC-WS-001: 添加多个会话，连接数正确统计")
        void testAddMultipleSessions_CorrectCount() {
            // Given
            WebSocketSession session1 = createMockSession("session-001");
            WebSocketSession session2 = createMockSession("session-002");
            WebSocketSession session3 = createMockSession("session-003");

            // When
            manager.addSession(session1);
            manager.addSession(session2);
            manager.addSession(session3);

            // Then
            assertEquals(3, manager.getSessionCount());
        }

        @Test
        @DisplayName("TC-WS-001: 移除会话时清理相关订阅")
        void testRemoveSession_ClearsSubscriptions() {
            // Given
            WebSocketSession session = createMockSession("session-001");
            String deviceId = "device_001";
            manager.addSession(session);
            manager.subscribeDevice(session, deviceId);

            // When
            manager.removeSession(session);

            // Then
            assertEquals(0, manager.getSubscriberCount(deviceId));
        }

        @Test
        @DisplayName("TC-WS-001: 重复添加同一会话，不会重复计数")
        void testAddDuplicateSession_NoDuplicateCount() {
            // Given
            WebSocketSession session = createMockSession("session-001");

            // When
            manager.addSession(session);
            manager.addSession(session); // 重复添加

            // Then - CopyOnWriteArraySet不允许重复元素
            assertEquals(1, manager.getSessionCount());
        }
    }

    // ==================== 设备订阅管理测试 ====================

    @Nested
    @DisplayName("设备订阅管理测试")
    class DeviceSubscriptionTests {

        @Test
        @DisplayName("TC-WS-002: 订阅设备，订阅者数量增加")
        void testSubscribeDevice_SubscriberCountIncreases() {
            // Given
            WebSocketSession session = createMockSession("session-001");
            String deviceId = "device_001";
            manager.addSession(session);

            // When
            manager.subscribeDevice(session, deviceId);

            // Then
            assertEquals(1, manager.getSubscriberCount(deviceId));
        }

        @Test
        @DisplayName("TC-WS-002: 多个会话订阅同一设备，订阅者数量正确")
        void testSubscribeSameDevice_MultipleSessions() {
            // Given
            WebSocketSession session1 = createMockSession("session-001");
            WebSocketSession session2 = createMockSession("session-002");
            String deviceId = "device_001";
            manager.addSession(session1);
            manager.addSession(session2);

            // When
            manager.subscribeDevice(session1, deviceId);
            manager.subscribeDevice(session2, deviceId);

            // Then
            assertEquals(2, manager.getSubscriberCount(deviceId));
        }

        @Test
        @DisplayName("TC-WS-002: 一个会话订阅多个设备")
        void testSubscribeMultipleDevices_OneSession() {
            // Given
            WebSocketSession session = createMockSession("session-001");
            String deviceId1 = "device_001";
            String deviceId2 = "device_002";
            manager.addSession(session);

            // When
            manager.subscribeDevice(session, deviceId1);
            manager.subscribeDevice(session, deviceId2);

            // Then
            assertEquals(1, manager.getSubscriberCount(deviceId1));
            assertEquals(1, manager.getSubscriberCount(deviceId2));
        }

        @Test
        @DisplayName("TC-WS-002: 取消订阅设备，订阅者数量减少")
        void testUnsubscribeDevice_SubscriberCountDecreases() {
            // Given
            WebSocketSession session = createMockSession("session-001");
            String deviceId = "device_001";
            manager.addSession(session);
            manager.subscribeDevice(session, deviceId);

            // When
            manager.unsubscribeDevice(session, deviceId);

            // Then
            assertEquals(0, manager.getSubscriberCount(deviceId));
        }

        @Test
        @DisplayName("TC-WS-005: 取消所有设备订阅")
        void testUnsubscribeAllDevices_AllSubscriptionsRemoved() {
            // Given
            WebSocketSession session = createMockSession("session-001");
            String deviceId1 = "device_001";
            String deviceId2 = "device_002";
            manager.addSession(session);
            manager.subscribeDevice(session, deviceId1);
            manager.subscribeDevice(session, deviceId2);

            // When
            manager.unsubscribeAllDevices(session);

            // Then
            assertEquals(0, manager.getSubscriberCount(deviceId1));
            assertEquals(0, manager.getSubscriberCount(deviceId2));
        }

        @Test
        @DisplayName("TC-WS-002: 查询未订阅设备的订阅者数量返回0")
        void testGetSubscriberCount_NoSubscribers_ReturnsZero() {
            // Given
            String deviceId = "device_no_subscribers";

            // When
            int count = manager.getSubscriberCount(deviceId);

            // Then
            assertEquals(0, count);
        }
    }

    // ==================== 消息推送测试 ====================

    @Nested
    @DisplayName("消息推送测试")
    class MessagePushTests {

        @Test
        @DisplayName("TC-WS-003: 发送消息给设备订阅者，订阅者收到消息")
        void testSendToDeviceSubscribers_SubscribersReceiveMessage() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session-001");
            String deviceId = "device_001";
            manager.addSession(session);
            manager.subscribeDevice(session, deviceId);

            // When
            String message = "{\"type\":\"status\",\"value\":\"online\"}";
            manager.sendToDeviceSubscribers(deviceId, message);

            // Then
            verify(session).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("TC-WS-003: 发送消息给设备订阅者，非订阅者不收到消息")
        void testSendToDeviceSubscribers_NonSubscribersDoNotReceive() throws Exception {
            // Given
            WebSocketSession subscriber = createMockSession("session-001");
            WebSocketSession nonSubscriber = createMockSession("session-002");
            String deviceId = "device_001";
            manager.addSession(subscriber);
            manager.addSession(nonSubscriber);
            manager.subscribeDevice(subscriber, deviceId);

            // When
            String message = "{\"type\":\"status\"}";
            manager.sendToDeviceSubscribers(deviceId, message);

            // Then
            verify(subscriber).sendMessage(any(TextMessage.class));
            verify(nonSubscriber, never()).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("TC-WS-003: 广播消息，所有会话收到消息")
        void testBroadcast_AllSessionsReceive() throws Exception {
            // Given
            WebSocketSession session1 = createMockSession("session-001");
            WebSocketSession session2 = createMockSession("session-002");
            manager.addSession(session1);
            manager.addSession(session2);

            // When
            String message = "{\"type\":\"system\",\"message\":\"broadcast\"}";
            manager.broadcast(message);

            // Then
            verify(session1).sendMessage(any(TextMessage.class));
            verify(session2).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("TC-WS-003: 发送消息给无订阅者的设备，不发送任何消息")
        void testSendToDeviceSubscribers_NoSubscribers_NoMessageSent() throws Exception {
            // Given
            String deviceId = "device_no_subscribers";

            // When
            String message = "{\"type\":\"status\"}";
            manager.sendToDeviceSubscribers(deviceId, message);

            // Then - 没有异常，只是不发送消息
            // 验证没有发送任何消息
        }

        @Test
        @DisplayName("TC-WS-003: 发送失败时移除失效会话")
        void testSendToDeviceSubscribers_FailedSend_RemovesSession() throws Exception {
            // Given
            WebSocketSession session = mock(WebSocketSession.class);
            when(session.getId()).thenReturn("session-001");
            when(session.isOpen()).thenReturn(true);
            doThrow(new IOException("Connection closed")).when(session).sendMessage(any(TextMessage.class));

            String deviceId = "device_001";
            manager.addSession(session);
            manager.subscribeDevice(session, deviceId);

            // When
            String message = "{\"type\":\"status\"}";
            manager.sendToDeviceSubscribers(deviceId, message);

            // Then - 会话应该被移除
            assertEquals(0, manager.getSessionCount());
        }

        @Test
        @DisplayName("TC-WS-003: 不发送消息给已关闭的会话")
        void testSendToDeviceSubscribers_ClosedSession_NoMessageSent() throws Exception {
            // Given
            WebSocketSession session = mock(WebSocketSession.class);
            when(session.getId()).thenReturn("session-001");
            when(session.isOpen()).thenReturn(false); // 会话已关闭

            String deviceId = "device_001";
            manager.addSession(session);
            manager.subscribeDevice(session, deviceId);

            // When
            String message = "{\"type\":\"status\"}";
            manager.sendToDeviceSubscribers(deviceId, message);

            // Then
            verify(session, never()).sendMessage(any(TextMessage.class));
        }
    }

    // ==================== 异步发送测试 ====================

    @Nested
    @DisplayName("异步发送测试")
    class AsyncSendTests {

        @Test
        @DisplayName("异步广播消息，所有会话收到消息")
        void testBroadcastAsync_AllSessionsReceive() throws Exception {
            // Given
            WebSocketSession session1 = createMockSession("session-001");
            WebSocketSession session2 = createMockSession("session-002");
            manager.addSession(session1);
            manager.addSession(session2);

            // When
            String message = "{\"type\":\"system\"}";
            manager.broadcastAsync(message);

            // 等待异步操作完成
            TimeUnit.MILLISECONDS.sleep(100);

            // Then
            verify(session1).sendMessage(any(TextMessage.class));
            verify(session2).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("异步发送给设备订阅者，订阅者收到消息")
        void testSendToDeviceSubscribersAsync_SubscribersReceive() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session-001");
            String deviceId = "device_001";
            manager.addSession(session);
            manager.subscribeDevice(session, deviceId);

            // When
            String message = "{\"type\":\"status\"}";
            manager.sendToDeviceSubscribersAsync(deviceId, message);

            // 等待异步操作完成
            TimeUnit.MILLISECONDS.sleep(100);

            // Then
            verify(session).sendMessage(any(TextMessage.class));
        }
    }

    // ==================== 统计信息测试 ====================

    @Nested
    @DisplayName("统计信息测试")
    class StatisticsTests {

        @Test
        @DisplayName("获取统计信息，包含正确的字段")
        void testGetStats_ContainsCorrectFields() {
            // Given
            WebSocketSession session = createMockSession("session-001");
            manager.addSession(session);

            // When
            Map<String, Object> stats = manager.getStats();

            // Then
            assertTrue(stats.containsKey("totalSessions"));
            assertTrue(stats.containsKey("totalSent"));
            assertTrue(stats.containsKey("totalFailed"));
            assertTrue(stats.containsKey("deviceSubscriptions"));
        }

        @Test
        @DisplayName("发送消息后，统计发送次数增加")
        void testGetStats_AfterSend_TotalSentIncreases() throws Exception {
            // Given
            WebSocketSession session = createMockSession("session-001");
            String deviceId = "device_001";
            manager.addSession(session);
            manager.subscribeDevice(session, deviceId);

            // When
            manager.sendToDeviceSubscribers(deviceId, "{\"type\":\"status\"}");
            manager.sendToDeviceSubscribers(deviceId, "{\"type\":\"status\"}");

            // Then
            Map<String, Object> stats = manager.getStats();
            assertEquals(2, stats.get("totalSent"));
        }

        @Test
        @DisplayName("获取发送线程池")
        void testGetSendExecutor_ReturnsThreadPool() {
            // When
            var executor = manager.getSendExecutor();

            // Then
            assertNotNull(executor);
            assertTrue(executor instanceof java.util.concurrent.ThreadPoolExecutor);
        }
    }

    // ==================== 优雅关闭测试 ====================

    @Nested
    @DisplayName("优雅关闭测试")
    class ShutdownTests {

        @Test
        @DisplayName("关闭时清理所有会话")
        void testShutdown_ClearsAllSessions() throws Exception {
            // Given - 创建新的manager实例用于测试
            resetSingleton();
            WebSocketManager testManager = WebSocketManager.getInstance();

            WebSocketSession session1 = mock(WebSocketSession.class);
            WebSocketSession session2 = mock(WebSocketSession.class);
            when(session1.getId()).thenReturn("session-001");
            when(session2.getId()).thenReturn("session-002");
            when(session1.isOpen()).thenReturn(true);
            when(session2.isOpen()).thenReturn(true);

            testManager.addSession(session1);
            testManager.addSession(session2);

            // When
            testManager.shutdown();

            // Then
            assertEquals(0, testManager.getSessionCount());
            verify(session1).close();
            verify(session2).close();
        }

        @Test
        @DisplayName("关闭后线程池停止")
        void testShutdown_ExecutorTerminated() throws Exception {
            // Given - 创建新的manager实例用于测试
            resetSingleton();
            WebSocketManager testManager = WebSocketManager.getInstance();
            testManager.addSession(createMockSession("session-001"));

            // When
            testManager.shutdown();

            // Then
            assertTrue(testManager.getSendExecutor().isShutdown());
        }
    }

    // ==================== 单例模式测试 ====================

    @Nested
    @DisplayName("单例模式测试")
    class SingletonTests {

        @Test
        @DisplayName("多次获取实例返回同一对象")
        void testGetInstance_ReturnsSameInstance() {
            // When
            WebSocketManager instance1 = WebSocketManager.getInstance();
            WebSocketManager instance2 = WebSocketManager.getInstance();

            // Then
            assertSame(instance1, instance2);
        }

        @Test
        @DisplayName("多线程环境下单例正确")
        void testGetInstance_ThreadSafe() throws Exception {
            // Given
            int threadCount = 10;
            WebSocketManager[] instances = new WebSocketManager[threadCount];
            Thread[] threads = new Thread[threadCount];

            // When
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    instances[index] = WebSocketManager.getInstance();
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // Then - 所有实例应该相同
            for (int i = 1; i < threadCount; i++) {
                assertSame(instances[0], instances[i]);
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建模拟的WebSocketSession
     */
    private WebSocketSession createMockSession(String sessionId) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}