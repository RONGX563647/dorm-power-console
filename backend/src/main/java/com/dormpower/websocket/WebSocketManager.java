package com.dormpower.websocket;

import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket连接管理器
 */
public class WebSocketManager {

    private static WebSocketManager instance;
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final Map<WebSocketSession, Set<String>> sessionDeviceSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<WebSocketSession>> deviceSubscribers = new ConcurrentHashMap<>();

    private WebSocketManager() {
    }

    /**
     * 获取单例实例
     * @return WebSocketManager实例
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
     * 添加WebSocket会话
     * @param session WebSocketSession
     */
    public void addSession(WebSocketSession session) {
        sessions.add(session);
        sessionDeviceSubscriptions.put(session, ConcurrentHashMap.newKeySet());
    }

    /**
     * 移除WebSocket会话
     * @param session WebSocketSession
     */
    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
        
        // 移除所有设备订阅
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
     * 订阅设备
     * @param session WebSocketSession
     * @param deviceId 设备ID
     */
    public void subscribeDevice(WebSocketSession session, String deviceId) {
        sessionDeviceSubscriptions.computeIfAbsent(session, k -> ConcurrentHashMap.newKeySet()).add(deviceId);
        deviceSubscribers.computeIfAbsent(deviceId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    /**
     * 取消订阅设备
     * @param session WebSocketSession
     * @param deviceId 设备ID
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
     * @param session WebSocketSession
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
     * 广播消息给所有连接的客户端
     * @param message 消息内容
     */
    public void broadcast(String message) {
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new org.springframework.web.socket.TextMessage(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 发送消息给指定设备的订阅者
     * @param deviceId 设备ID
     * @param message 消息内容
     */
    public void sendToDeviceSubscribers(String deviceId, String message) {
        Set<WebSocketSession> subscribers = deviceSubscribers.get(deviceId);
        if (subscribers != null) {
            subscribers.forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new org.springframework.web.socket.TextMessage(message));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

}