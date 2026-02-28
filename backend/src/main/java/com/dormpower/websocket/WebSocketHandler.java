package com.dormpower.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket处理器
 */
public class WebSocketHandler extends TextWebSocketHandler {

    private final WebSocketManager webSocketManager = WebSocketManager.getInstance();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        webSocketManager.addSession(session);
        System.out.println("WebSocket connection established: " + session.getId());
        
        // 发送连接成功消息
        Map<String, Object> message = new HashMap<>();
        message.put("type", "connection");
        message.put("status", "connected");
        message.put("sessionId", session.getId());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        webSocketManager.removeSession(session);
        System.out.println("WebSocket connection closed: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("Received message from " + session.getId() + ": " + payload);
        
        try {
            // 解析客户端消息
            Map<String, Object> request = objectMapper.readValue(payload, Map.class);
            String type = (String) request.get("type");
            
            // 根据消息类型处理
            switch (type) {
                case "subscribe":
                    handleSubscribe(session, request);
                    break;
                case "unsubscribe":
                    handleUnsubscribe(session, request);
                    break;
                case "ping":
                    handlePing(session);
                    break;
                default:
                    sendError(session, "Unknown message type: " + type);
            }
        } catch (Exception e) {
            sendError(session, "Failed to parse message: " + e.getMessage());
        }
    }

    /**
     * 处理订阅请求
     * @param session WebSocketSession
     * @param request 请求数据
     */
    private void handleSubscribe(WebSocketSession session, Map<String, Object> request) throws Exception {
        String deviceId = (String) request.get("deviceId");
        if (deviceId != null) {
            webSocketManager.subscribeDevice(session, deviceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "subscribe");
            response.put("status", "success");
            response.put("deviceId", deviceId);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } else {
            sendError(session, "Device ID is required for subscription");
        }
    }

    /**
     * 处理取消订阅请求
     * @param session WebSocketSession
     * @param request 请求数据
     */
    private void handleUnsubscribe(WebSocketSession session, Map<String, Object> request) throws Exception {
        String deviceId = (String) request.get("deviceId");
        if (deviceId != null) {
            webSocketManager.unsubscribeDevice(session, deviceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "unsubscribe");
            response.put("status", "success");
            response.put("deviceId", deviceId);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } else {
            webSocketManager.unsubscribeAllDevices(session);
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "unsubscribe");
            response.put("status", "success");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
    }

    /**
     * 处理ping请求
     * @param session WebSocketSession
     */
    private void handlePing(WebSocketSession session) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "pong");
        response.put("timestamp", System.currentTimeMillis());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    /**
     * 发送错误消息
     * @param session WebSocketSession
     * @param error 错误信息
     */
    private void sendError(WebSocketSession session, String error) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "error");
        response.put("message", error);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

}