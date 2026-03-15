package com.dormpower.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 多级缓存管理器
 * 
 * 实现本地缓存(Caffeine) + 分布式缓存(Redis)的多级缓存架构
 * 
 * 特点：
 * 1. L1缓存：本地缓存(Caffeine) - 快速访问，容量有限
 * 2. L2缓存：分布式缓存(Redis) - 数据共享，容量大
 * 3. 自动回填：L2命中时自动回填L1
 * 4. 一致性保证：同时更新两级缓存
 * 5. 多节点支持：支持本地缓存失效广播
 * 
 * @author dormpower team
 * @version 2.0
 */
public class MultiLevelCacheManager implements CacheManager {

    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCacheManager.class);

    private final CacheManager localCacheManager;
    private final CacheManager remoteCacheManager;
    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

    public MultiLevelCacheManager(CacheManager localCacheManager, CacheManager remoteCacheManager) {
        this.localCacheManager = localCacheManager;
        this.remoteCacheManager = remoteCacheManager;
        logger.info("MultiLevelCacheManager initialized with local: {} and remote: {}", 
            localCacheManager.getClass().getSimpleName(), 
            remoteCacheManager.getClass().getSimpleName());
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, key -> {
            Cache localCache = localCacheManager.getCache(key);
            Cache remoteCache = remoteCacheManager.getCache(key);
            
            if (localCache == null || remoteCache == null) {
                logger.warn("Failed to create multi-level cache for name: {}, local: {}, remote: {}", 
                    key, localCache != null, remoteCache != null);
                return null;
            }
            
            logger.debug("Created multi-level cache for name: {}", key);
            return new MultiLevelCache(key, localCache, remoteCache);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(cacheMap.keySet());
    }
    
    /**
     * 获取本地缓存（L1）
     * 用于多节点缓存失效时只清除本地缓存
     */
    public Cache getLocalCache(String name) {
        return localCacheManager.getCache(name);
    }
    
    /**
     * 获取远程缓存（L2）
     */
    public Cache getRemoteCache(String name) {
        return remoteCacheManager.getCache(name);
    }
    
    /**
     * 获取本地缓存管理器
     */
    public CacheManager getLocalCacheManager() {
        return localCacheManager;
    }
    
    /**
     * 获取远程缓存管理器
     */
    public CacheManager getRemoteCacheManager() {
        return remoteCacheManager;
    }
    
    /**
     * 清除指定缓存的本地L1缓存
     * 用于多节点部署下接收其他节点的失效广播
     */
    public void evictLocal(String cacheName, Object key) {
        Cache localCache = localCacheManager.getCache(cacheName);
        if (localCache != null) {
            localCache.evict(key);
            logger.debug("Evicted local cache - cache: {}, key: {}", cacheName, key);
        }
    }
    
    /**
     * 清空指定缓存的本地L1缓存
     */
    public void clearLocal(String cacheName) {
        Cache localCache = localCacheManager.getCache(cacheName);
        if (localCache != null) {
            localCache.clear();
            logger.debug("Cleared local cache - cache: {}", cacheName);
        }
    }
}
