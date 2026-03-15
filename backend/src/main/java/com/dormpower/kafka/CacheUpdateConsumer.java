package com.dormpower.kafka;

import com.dormpower.cache.MultiLevelCacheManager;
import com.dormpower.model.CacheUpdateMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * 缓存更新消费者
 * 
 * 消费Kafka消息队列中的缓存更新消息
 * 支持多节点部署下的缓存一致性
 * 
 * 处理操作：
 * - PUT: 写入缓存（L1 + L2）
 * - EVICT: 清除缓存（L1 + L2）
 * - CLEAR: 清空缓存（L1 + L2）
 * - EVICT_LOCAL: 仅清除本地L1缓存（多节点广播）
 * - CLEAR_LOCAL: 仅清空本地L1缓存（多节点广播）
 * 
 * @author dormpower team
 * @version 2.0
 */
@Service
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class CacheUpdateConsumer {

    private static final Logger logger = LoggerFactory.getLogger(CacheUpdateConsumer.class);

    @Autowired
    private CacheManager cacheManager;
    
    @Autowired(required = false)
    private MultiLevelCacheManager multiLevelCacheManager;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${cache.node.id:}")
    private String currentNodeId;

    @KafkaListener(topics = "${cache.async-update.kafka-topic:cache.update}", groupId = "cache-updater")
    public void handleCacheUpdate(String message) {
        try {
            CacheUpdateMessage msg = objectMapper.readValue(message, CacheUpdateMessage.class);
            
            logger.debug("Received cache update message - cache: {}, key: {}, operation: {}, nodeId: {}", 
                msg.getCacheName(), msg.getKey(), msg.getOperation(), msg.getNodeId());

            if (msg.getNodeId() != null && msg.getNodeId().equals(currentNodeId)) {
                logger.debug("Skipping self-originated message from nodeId: {}", msg.getNodeId());
                return;
            }

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
                case "EVICT_LOCAL":
                    handleEvictLocal(msg);
                    break;
                case "CLEAR_LOCAL":
                    handleClearLocal(msg);
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
            Cache cache = cacheManager.getCache(msg.getCacheName());
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
            Cache cache = cacheManager.getCache(msg.getCacheName());
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
            Cache cache = cacheManager.getCache(msg.getCacheName());
            if (cache != null) {
                cache.clear();
                logger.debug("Cache CLEAR - cache: {}", msg.getCacheName());
            }
        } catch (Exception e) {
            logger.error("Failed to CLEAR cache - cache: {}, error: {}", 
                msg.getCacheName(), e.getMessage());
        }
    }
    
    /**
     * 处理本地缓存失效消息
     * 仅清除本地L1缓存，用于多节点部署下的一致性
     */
    private void handleEvictLocal(CacheUpdateMessage msg) {
        try {
            if (multiLevelCacheManager != null) {
                multiLevelCacheManager.evictLocal(msg.getCacheName(), msg.getKey());
                logger.debug("Local cache EVICT_LOCAL - cache: {}, key: {}, fromNode: {}", 
                    msg.getCacheName(), msg.getKey(), msg.getNodeId());
            } else {
                logger.debug("MultiLevelCacheManager not available, skipping EVICT_LOCAL");
            }
        } catch (Exception e) {
            logger.error("Failed to EVICT_LOCAL cache - cache: {}, key: {}, error: {}", 
                msg.getCacheName(), msg.getKey(), e.getMessage());
        }
    }
    
    /**
     * 处理本地缓存清空消息
     */
    private void handleClearLocal(CacheUpdateMessage msg) {
        try {
            if (multiLevelCacheManager != null) {
                multiLevelCacheManager.clearLocal(msg.getCacheName());
                logger.debug("Local cache CLEAR_LOCAL - cache: {}, fromNode: {}", 
                    msg.getCacheName(), msg.getNodeId());
            } else {
                logger.debug("MultiLevelCacheManager not available, skipping CLEAR_LOCAL");
            }
        } catch (Exception e) {
            logger.error("Failed to CLEAR_LOCAL cache - cache: {}, error: {}", 
                msg.getCacheName(), e.getMessage());
        }
    }
}
