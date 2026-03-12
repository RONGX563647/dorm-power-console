package com.dormpower.service;

import com.dormpower.websocket.WebSocketManager;
import org.springframework.stereotype.Service;

/**
 * WebSocket服务
 */
@Service
public class WebSocketService {

    private final WebSocketManager webSocketManager = WebSocketManager.getInstance();

    /**
     * 广播消息
     * @param message 消息内容
     */
    public void broadcast(String message) {
        webSocketManager.broadcast(message);
    }

}