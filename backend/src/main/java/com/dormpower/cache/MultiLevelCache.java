package com.dormpower.cache;

import org.springframework.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * 多级缓存实现
 * 
 * 实现本地缓存(L1) + 分布式缓存(L2)的多级缓存策略
 * 
 * 读取流程：
 * 1. 先查L1本地缓存
 * 2. L1未命中，查L2分布式缓存
 * 3. L2命中，回填L1缓存
 * 
 * 写入流程：
 * 1. 同时写入L1和L2缓存
 * 2. 保证两级缓存一致性
 * 
 * 多节点一致性：
 * - evictLocal(): 仅清除本地L1缓存
 * - clearLocal(): 仅清空本地L1缓存
 * 
 * @author dormpower team
 * @version 2.0
 */
public class MultiLevelCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCache.class);

    private final String name;
    private final Cache localCache;
    private final Cache remoteCache;

    public MultiLevelCache(String name, Cache localCache, Cache remoteCache) {
        this.name = name;
        this.localCache = localCache;
        this.remoteCache = remoteCache;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(Object key) {
        long startTime = System.currentTimeMillis();
        
        try {
            ValueWrapper value = localCache.get(key);
            if (value != null) {
                logger.debug("Cache HIT (L1) - cache: {}, key: {}, time: {}ms", 
                    name, key, System.currentTimeMillis() - startTime);
                return value;
            }
            
            value = remoteCache.get(key);
            if (value != null) {
                logger.debug("Cache HIT (L2) - cache: {}, key: {}, time: {}ms", 
                    name, key, System.currentTimeMillis() - startTime);
                localCache.put(key, value.get());
                return value;
            }
            
            logger.debug("Cache MISS - cache: {}, key: {}, time: {}ms", 
                name, key, System.currentTimeMillis() - startTime);
            return null;
            
        } catch (Exception e) {
            logger.error("Cache GET error - cache: {}, key: {}, error: {}", 
                name, key, e.getMessage());
            return null;
        }
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            Object value = wrapper.get();
            if (value != null && type != null && !type.isInstance(value)) {
                throw new IllegalStateException(
                    "Cached value is not of required type [" + type.getName() + "]: " + value);
            }
            return (T) value;
        }
        return null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }
        
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        long startTime = System.currentTimeMillis();
        
        try {
            localCache.put(key, value);
            remoteCache.put(key, value);
            
            logger.debug("Cache PUT - cache: {}, key: {}, time: {}ms", 
                name, key, System.currentTimeMillis() - startTime);
                
        } catch (Exception e) {
            logger.error("Cache PUT error - cache: {}, key: {}, error: {}", 
                name, key, e.getMessage());
        }
    }

    @Override
    public void evict(Object key) {
        long startTime = System.currentTimeMillis();
        
        try {
            localCache.evict(key);
            remoteCache.evict(key);
            
            logger.debug("Cache EVICT - cache: {}, key: {}, time: {}ms", 
                name, key, System.currentTimeMillis() - startTime);
                
        } catch (Exception e) {
            logger.error("Cache EVICT error - cache: {}, key: {}, error: {}", 
                name, key, e.getMessage());
        }
    }

    @Override
    public void clear() {
        long startTime = System.currentTimeMillis();
        
        try {
            localCache.clear();
            remoteCache.clear();
            
            logger.debug("Cache CLEAR - cache: {}, time: {}ms", 
                name, System.currentTimeMillis() - startTime);
                
        } catch (Exception e) {
            logger.error("Cache CLEAR error - cache: {}, error: {}", 
                name, e.getMessage());
        }
    }
    
    /**
     * 仅清除本地L1缓存
     * 用于多节点部署下接收其他节点的失效广播
     */
    public void evictLocal(Object key) {
        long startTime = System.currentTimeMillis();
        
        try {
            localCache.evict(key);
            
            logger.debug("Cache EVICT_LOCAL (L1 only) - cache: {}, key: {}, time: {}ms", 
                name, key, System.currentTimeMillis() - startTime);
                
        } catch (Exception e) {
            logger.error("Cache EVICT_LOCAL error - cache: {}, key: {}, error: {}", 
                name, key, e.getMessage());
        }
    }
    
    /**
     * 仅清空本地L1缓存
     */
    public void clearLocal() {
        long startTime = System.currentTimeMillis();
        
        try {
            localCache.clear();
            
            logger.debug("Cache CLEAR_LOCAL (L1 only) - cache: {}, time: {}ms", 
                name, System.currentTimeMillis() - startTime);
                
        } catch (Exception e) {
            logger.error("Cache CLEAR_LOCAL error - cache: {}, error: {}", 
                name, e.getMessage());
        }
    }
    
    /**
     * 获取本地缓存实例
     */
    public Cache getLocalCache() {
        return localCache;
    }
    
    /**
     * 获取远程缓存实例
     */
    public Cache getRemoteCache() {
        return remoteCache;
    }
}
