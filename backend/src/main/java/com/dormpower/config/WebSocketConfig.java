package com.dormpower.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 */
/**
 * WebSocket配置类
 * 用于配置WebSocket服务器端点，允许客户端通过WebSocket连接到服务器
 * 使用@Configuration注解标记为配置类，使用@EnableWebSocket注解启用WebSocket支持
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    /**
     * 注册WebSocket处理器
     * @param registry WebSocket处理器注册器，用于注册WebSocket处理器和端点
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册WebSocket处理器，指定端点为"/ws"
        // setAllowedOrigins("*")允许所有来源的跨域请求
        //todo: 跨域后面需要限制域名前缀 保证安全
        registry.addHandler(new com.dormpower.websocket.WebSocketHandler(), "/ws")
                .setAllowedOrigins("*");
    }

}