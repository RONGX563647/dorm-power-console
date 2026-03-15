package com.dormpower.cache.consistency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.UUID;

/**
 * 缓存失效广播服务
 *
 * 通过 Redis Pub/Sub 实现多节点 L1 缓存一致性
 *
 * 工作原理：
 * 1. 写操作失效 L2 缓存后，发布失效消息
 * 2. 其他节点订阅消息，失效本地 L1 缓存
 * 3. 最终一致性：L1 短 TTL + 广播失效
 *
 * @author dormpower team
 * @version 1.0
 */
@Service
@ConditionalOnProperty(name = "spring.data.redis.host")
public class CacheInvalidationBroadcaster {

    private static final Logger logger = LoggerFactory.getLogger(CacheInvalidationBroadcaster.class);

    /** 失效消息通道 */
    public static final String INVALIDATION_CHANNEL = "dorm:cache:invalidation";

    /** 本节点标识 */
    private final String nodeId = UUID.randomUUID().toString().substring(0, 8);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @Autowired
    private ObjectMapper objectMapper;

    /** 消息监听器 */
    private MessageListener messageListener;

    /** 缓存失效处理器 */
    private CacheInvalidationHandler invalidationHandler;

    /**
     * 设置缓存失效处理器
     */
    public void setInvalidationHandler(CacheInvalidationHandler handler) {
        this.invalidationHandler = handler;
    }

    /**
     * 订阅失效消息
     */
    @PostConstruct
    public void subscribe() {
        if (redisMessageListenerContainer == null) {
            logger.warn("RedisMessageListenerContainer not available, skip subscription");
            return;
        }

        messageListener = new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                handleInvalidationMessage(message);
            }
        };

        redisMessageListenerContainer.addMessageListener(
            messageListener,
            new PatternTopic(INVALIDATION_CHANNEL)
        );

        logger.info("Subscribed to cache invalidation channel: {}, nodeId: {}",
            INVALIDATION_CHANNEL, nodeId);
    }

    /**
     * 取消订阅
     */
    @PreDestroy
    public void unsubscribe() {
        if (redisMessageListenerContainer != null && messageListener != null) {
            redisMessageListenerContainer.removeMessageListener(messageListener);
            logger.info("Unsubscribed from cache invalidation channel");
        }
    }

    /**
     * 发布失效消息
     *
     * @param cacheName 缓存名称
     * @param key 缓存键
     */
    public void publishInvalidation(String cacheName, String key) {
        publishInvalidation(cacheName, key, InvalidationType.EVICT);
    }

    /**
     * 发布失效消息
     *
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param type 失效类型
     */
    public void publishInvalidation(String cacheName, String key, InvalidationType type) {
        CacheInvalidationEvent event = new CacheInvalidationEvent(
            nodeId, cacheName, key, type, System.currentTimeMillis()
        );

        try {
            String message = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(INVALIDATION_CHANNEL, message);
            logger.debug("Published cache invalidation: cache={}, key={}, type={}",
                cacheName, key, type);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize invalidation event: {}", e.getMessage());
        }
    }

    /**
     * 发布清除整个缓存的消息
     */
    public void publishClear(String cacheName) {
        publishInvalidation(cacheName, "*", InvalidationType.CLEAR);
    }

    /**
     * 处理失效消息
     */
    private void handleInvalidationMessage(Message message) {
        try {
            String body = new String(message.getBody());
            CacheInvalidationEvent event = objectMapper.readValue(body, CacheInvalidationEvent.class);

            // 忽略自己发布的消息
            if (nodeId.equals(event.getNodeId())) {
                return;
            }

            logger.debug("Received cache invalidation: cache={}, key={}, from node={}",
                event.getCacheName(), event.getKey(), event.getNodeId());

            // 调用处理器失效本地缓存
            if (invalidationHandler != null) {
                switch (event.getType()) {
                    case EVICT:
                        invalidationHandler.evictLocal(event.getCacheName(), event.getKey());
                        break;
                    case CLEAR:
                        invalidationHandler.clearLocal(event.getCacheName());
                        break;
                }
            }

        } catch (Exception e) {
            logger.error("Failed to handle invalidation message: {}", e.getMessage());
        }
    }

    /**
     * 缓存失效事件
     */
    public static class CacheInvalidationEvent {
        private String nodeId;
        private String cacheName;
        private String key;
        private InvalidationType type;
        private long timestamp;

        public CacheInvalidationEvent() {}

        public CacheInvalidationEvent(String nodeId, String cacheName, String key,
                                       InvalidationType type, long timestamp) {
            this.nodeId = nodeId;
            this.cacheName = cacheName;
            this.key = key;
            this.type = type;
            this.timestamp = timestamp;
        }

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        public String getCacheName() { return cacheName; }
        public void setCacheName(String cacheName) { this.cacheName = cacheName; }
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public InvalidationType getType() { return type; }
        public void setType(InvalidationType type) { this.type = type; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    /**
     * 失效类型
     */
    public enum InvalidationType {
        EVICT,  // 失效单个键
        CLEAR   // 清除整个缓存
    }

    /**
     * 缓存失效处理器接口
     */
    @FunctionalInterface
    public interface CacheInvalidationHandler {
        /**
         * 失效本地缓存
         *
         * @param cacheName 缓存名称
         * @param key 缓存键
         */
        void evictLocal(String cacheName, String key);

        /**
         * 清除本地缓存
         */
        default void clearLocal(String cacheName) {
            evictLocal(cacheName, "*");
        }
    }
}