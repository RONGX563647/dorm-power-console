package com.dormpower.kafka;

import com.dormpower.model.CacheUpdateMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * 缓存更新生产者
 * 
 * 通过Kafka消息队列异步更新缓存
 * 
 * @author dormpower team
 * @version 1.0
 */
@Service
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class CacheUpdateProducer {

    private static final Logger logger = LoggerFactory.getLogger(CacheUpdateProducer.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${cache.async-update.kafka-topic:cache.update}")
    private String topic;

    /**
     * 发送缓存更新消息
     */
    public void sendCacheUpdate(String cacheName, String key, Object value, String operation) {
        try {
            CacheUpdateMessage message = new CacheUpdateMessage();
            message.setCacheName(cacheName);
            message.setKey(key);
            message.setValue(value);
            message.setOperation(operation);
            message.setTimestamp(System.currentTimeMillis());

            String messageJson = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, key, messageJson);

            logger.debug("Sent cache update message - cache: {}, key: {}, operation: {}", 
                cacheName, key, operation);

        } catch (Exception e) {
            logger.error("Failed to send cache update message - cache: {}, key: {}, error: {}", 
                cacheName, key, e.getMessage());
        }
    }

    /**
     * 发送缓存清除消息
     */
    public void sendCacheEvict(String cacheName, String key) {
        sendCacheUpdate(cacheName, key, null, "EVICT");
    }

    /**
     * 发送缓存清空消息
     */
    public void sendCacheClear(String cacheName) {
        sendCacheUpdate(cacheName, "ALL", null, "CLEAR");
    }
}
