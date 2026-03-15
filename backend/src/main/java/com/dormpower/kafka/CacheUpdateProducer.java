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

import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.UUID;

/**
 * 缓存更新生产者
 * 
 * 通过Kafka消息队列异步更新缓存
 * 支持多节点部署下的缓存一致性广播
 * 
 * @author dormpower team
 * @version 2.0
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
    
    @Value("${spring.application.name:dorm-power}")
    private String applicationName;
    
    private String nodeId;

    @PostConstruct
    public void init() {
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            String shortUuid = UUID.randomUUID().toString().substring(0, 8);
            this.nodeId = applicationName + "-" + hostAddress + "-" + shortUuid;
            logger.info("CacheUpdateProducer initialized with nodeId: {}", nodeId);
        } catch (Exception e) {
            this.nodeId = applicationName + "-" + UUID.randomUUID().toString().substring(0, 8);
            logger.warn("Failed to get host address, using fallback nodeId: {}", nodeId);
        }
    }

    public String getNodeId() {
        return nodeId;
    }

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
            message.setNodeId(nodeId);
            message.setBroadcast(true);

            String messageJson = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, key, messageJson);

            logger.debug("Sent cache update message - cache: {}, key: {}, operation: {}, nodeId: {}", 
                cacheName, key, operation, nodeId);

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
    
    /**
     * 发送本地缓存失效消息
     * 用于通知其他节点清除本地L1缓存
     */
    public void sendLocalCacheEvict(String cacheName, String key) {
        try {
            CacheUpdateMessage message = new CacheUpdateMessage();
            message.setCacheName(cacheName);
            message.setKey(key);
            message.setOperation("EVICT_LOCAL");
            message.setTimestamp(System.currentTimeMillis());
            message.setNodeId(nodeId);
            message.setBroadcast(true);

            String messageJson = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, key, messageJson);

            logger.debug("Sent local cache evict message - cache: {}, key: {}, nodeId: {}", 
                cacheName, key, nodeId);

        } catch (Exception e) {
            logger.error("Failed to send local cache evict message - cache: {}, key: {}, error: {}", 
                cacheName, key, e.getMessage());
        }
    }
    
    /**
     * 发送本地缓存清空消息
     */
    public void sendLocalCacheClear(String cacheName) {
        try {
            CacheUpdateMessage message = new CacheUpdateMessage();
            message.setCacheName(cacheName);
            message.setKey("ALL");
            message.setOperation("CLEAR_LOCAL");
            message.setTimestamp(System.currentTimeMillis());
            message.setNodeId(nodeId);
            message.setBroadcast(true);

            String messageJson = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, cacheName, messageJson);

            logger.debug("Sent local cache clear message - cache: {}, nodeId: {}", 
                cacheName, nodeId);

        } catch (Exception e) {
            logger.error("Failed to send local cache clear message - cache: {}, error: {}", 
                cacheName, e.getMessage());
        }
    }
}
