package com.dormpower.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Pub/Sub 配置类
 *
 * 配置 Redis 消息监听容器，用于：
 * 1. 缓存失效广播 - 多节点缓存一致性
 * 2. 其他 Pub/Sub 场景
 *
 * @author dormpower team
 * @version 1.0
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisPubSubConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisPubSubConfig.class);

    /**
     * Redis 消息监听容器
     *
     * 用于订阅 Redis Pub/Sub 频道，支持缓存失效广播等功能
     *
     * @param connectionFactory Redis 连接工厂
     * @return 消息监听容器
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // 设置任务执行器（使用默认的 SimpleAsyncTaskExecutor）
        // 设置错误处理器
        container.setErrorHandler(e -> {
            logger.error("Redis message listener error: {}", e.getMessage());
        });

        logger.info("RedisMessageListenerContainer initialized");
        return container;
    }
}