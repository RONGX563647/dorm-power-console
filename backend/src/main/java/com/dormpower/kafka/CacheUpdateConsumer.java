package com.dormpower.kafka;

import com.dormpower.model.CacheUpdateMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * 缓存更新消费者
 * 
 * 消费Kafka消息队列中的缓存更新消息
 * 
 * @author dormpower team
 * @version 1.0
 */
@Service
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class CacheUpdateConsumer {

    private static final Logger logger = LoggerFactory.getLogger(CacheUpdateConsumer.class);

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "${cache.async-update.kafka-topic:cache.update}", groupId = "cache-updater")
    public void handleCacheUpdate(String message) {
        try {
            CacheUpdateMessage msg = objectMapper.readValue(message, CacheUpdateMessage.class);
            
            logger.debug("Received cache update message - cache: {}, key: {}, operation: {}", 
                msg.getCacheName(), msg.getKey(), msg.getOperation());

            switch (msg.getOperation()) {
                case "PUT":
                    handlePut(msg);
                    break;
                case "EVICT":
                    handleEvict(msg);
                    break;
                case "CLEAR":
                    handleClear(msg);
                    break;
                default:
                    logger.warn("Unknown cache operation: {}", msg.getOperation());
            }

        } catch (Exception e) {
            logger.error("Failed to handle cache update message: {}, error: {}", 
                message, e.getMessage());
        }
    }

    private void handlePut(CacheUpdateMessage msg) {
        try {
            org.springframework.cache.Cache cache = cacheManager.getCache(msg.getCacheName());
            if (cache != null) {
                cache.put(msg.getKey(), msg.getValue());
                logger.debug("Cache PUT - cache: {}, key: {}", msg.getCacheName(), msg.getKey());
            }
        } catch (Exception e) {
            logger.error("Failed to PUT cache - cache: {}, key: {}, error: {}", 
                msg.getCacheName(), msg.getKey(), e.getMessage());
        }
    }

    private void handleEvict(CacheUpdateMessage msg) {
        try {
            org.springframework.cache.Cache cache = cacheManager.getCache(msg.getCacheName());
            if (cache != null) {
                cache.evict(msg.getKey());
                logger.debug("Cache EVICT - cache: {}, key: {}", msg.getCacheName(), msg.getKey());
            }
        } catch (Exception e) {
            logger.error("Failed to EVICT cache - cache: {}, key: {}, error: {}", 
                msg.getCacheName(), msg.getKey(), e.getMessage());
        }
    }

    private void handleClear(CacheUpdateMessage msg) {
        try {
            org.springframework.cache.Cache cache = cacheManager.getCache(msg.getCacheName());
            if (cache != null) {
                cache.clear();
                logger.debug("Cache CLEAR - cache: {}", msg.getCacheName());
            }
        } catch (Exception e) {
            logger.error("Failed to CLEAR cache - cache: {}, error: {}", 
                msg.getCacheName(), e.getMessage());
        }
    }
}
