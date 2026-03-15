package com.dormpower.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket 连接管理器（优化版）
 *
 * 优化内容：
 * 1. 异步消息发送，不阻塞业务线程
 * 2. 发送失败会话自动清理
 * 3. 连接数统计和监控
 * 4. 优雅关闭机制
 *
 * @author dormpower team
 * @version 2.0
 */
public class WebSocketManager {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketManager.class);

    private static volatile WebSocketManager instance;

    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final Map<WebSocketSession, Set<String>> sessionDeviceSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<WebSocketSession>> deviceSubscribers = new ConcurrentHashMap<>();

    // 异步发送线程池
    private final ThreadPoolExecutor sendExecutor;

    // 统计指标
    private final AtomicInteger totalSent = new AtomicInteger(0);
    private final AtomicInteger totalFailed = new AtomicInteger(0);

    private WebSocketManager() {
        // 创建可监控的线程池
        int poolSize = Runtime.getRuntime().availableProcessors();
        this.sendExecutor = new ThreadPoolExecutor(
            poolSize,
            poolSize,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            r -> {
                Thread t = new Thread(r, "ws-sender");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 获取单例实例（双重检查锁）
     */
    public static WebSocketManager getInstance() {
        if (instance == null) {
            synchronized (WebSocketManager.class) {
                if (instance == null) {
                    instance = new WebSocketManager();
                }
            }
        }
        return instance;
    }

    /**
     * 添加 WebSocket 会话
     */
    public void addSession(WebSocketSession session) {
        sessions.add(session);
        sessionDeviceSubscriptions.put(session, ConcurrentHashMap.newKeySet());
        logger.info("WebSocket session added: {}, total sessions: {}", session.getId(), sessions.size());
    }

    /**
     * 移除 WebSocket 会话
     */
    public void removeSession(WebSocketSession session) {
        sessions.remove(session);

        Set<String> devices = sessionDeviceSubscriptions.remove(session);
        if (devices != null) {
            devices.forEach(deviceId -> {
                Set<WebSocketSession> subscribers = deviceSubscribers.get(deviceId);
                if (subscribers != null) {
                    subscribers.remove(session);
                }
            });
        }
        logger.info("WebSocket session removed: {}, total sessions: {}", session.getId(), sessions.size());
    }

    /**
     * 订阅设备
     */
    public void subscribeDevice(WebSocketSession session, String deviceId) {
        sessionDeviceSubscriptions.computeIfAbsent(session, k -> ConcurrentHashMap.newKeySet()).add(deviceId);
        deviceSubscribers.computeIfAbsent(deviceId, k -> ConcurrentHashMap.newKeySet()).add(session);
        logger.debug("Session {} subscribed to device {}", session.getId(), deviceId);
    }

    /**
     * 取消订阅设备
     */
    public void unsubscribeDevice(WebSocketSession session, String deviceId) {
        Set<String> devices = sessionDeviceSubscriptions.get(session);
        if (devices != null) {
            devices.remove(deviceId);
        }

        Set<WebSocketSession> subscribers = deviceSubscribers.get(deviceId);
        if (subscribers != null) {
            subscribers.remove(session);
        }
    }

    /**
     * 取消订阅所有设备
     */
    public void unsubscribeAllDevices(WebSocketSession session) {
        Set<String> devices = sessionDeviceSubscriptions.remove(session);
        if (devices != null) {
            devices.forEach(deviceId -> {
                Set<WebSocketSession> subscribers = deviceSubscribers.get(deviceId);
                if (subscribers != null) {
                    subscribers.remove(session);
                }
            });
        }
    }

    /**
     * 广播消息给所有连接的客户端（同步）
     */
    public void broadcast(String message) {
        sessions.forEach(session -> sendSync(session, message));
    }

    /**
     * 广播消息给所有连接的客户端（异步）
     */
    public void broadcastAsync(String message) {
        sendExecutor.submit(() -> broadcast(message));
    }

    /**
     * 发送消息给指定设备的订阅者（同步）
     */
    public void sendToDeviceSubscribers(String deviceId, String message) {
        Set<WebSocketSession> subscribers = deviceSubscribers.get(deviceId);
        if (subscribers != null) {
            subscribers.forEach(session -> sendSync(session, message));
        }
    }

    /**
     * 发送消息给指定设备的订阅者（异步）
     */
    public void sendToDeviceSubscribersAsync(String deviceId, String message) {
        sendExecutor.submit(() -> sendToDeviceSubscribers(deviceId, message));
    }

    /**
     * 批量发送消息给多个设备的订阅者（异步）
     */
    public void sendToDevicesAsync(List<String> deviceIds, String message) {
        sendExecutor.submit(() -> {
            for (String deviceId : deviceIds) {
                sendToDeviceSubscribers(deviceId, message);
            }
        });
    }

    /**
     * 同步发送消息给单个会话
     */
    private void sendSync(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
                totalSent.incrementAndGet();
            }
        } catch (IOException e) {
            totalFailed.incrementAndGet();
            logger.warn("Failed to send message to session {}: {}", session.getId(), e.getMessage());
            // 移除失效会话
            removeSession(session);
        }
    }

    /**
     * 异步发送消息给单个会话
     */
    public void sendAsync(WebSocketSession session, String message) {
        sendExecutor.submit(() -> sendSync(session, message));
    }

    /**
     * 获取当前连接数
     */
    public int getSessionCount() {
        return sessions.size();
    }

    /**
     * 获取设备订阅者数量
     */
    public int getSubscriberCount(String deviceId) {
        Set<WebSocketSession> subscribers = deviceSubscribers.get(deviceId);
        return subscribers != null ? subscribers.size() : 0;
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStats() {
        return Map.of(
            "totalSessions", sessions.size(),
            "totalSent", totalSent.get(),
            "totalFailed", totalFailed.get(),
            "deviceSubscriptions", deviceSubscribers.size()
        );
    }

    /**
     * 获取发送线程池（用于监控）
     */
    public ThreadPoolExecutor getSendExecutor() {
        return sendExecutor;
    }

    /**
     * 优雅关闭
     */
    public void shutdown() {
        logger.info("Shutting down WebSocket manager...");

        // 关闭所有会话
        List<WebSocketSession> sessionsToClose = new ArrayList<>(sessions);
        for (WebSocketSession session : sessionsToClose) {
            try {
                if (session.isOpen()) {
                    session.close();
                }
            } catch (IOException e) {
                logger.warn("Failed to close session: {}", e.getMessage());
            }
        }

        // 清理所有会话和订阅
        sessions.clear();
        sessionDeviceSubscriptions.clear();
        deviceSubscribers.clear();

        // 关闭线程池
        sendExecutor.shutdown();
        try {
            if (!sendExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                sendExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            sendExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("WebSocket manager shutdown complete. Stats: {}", getStats());
    }
}