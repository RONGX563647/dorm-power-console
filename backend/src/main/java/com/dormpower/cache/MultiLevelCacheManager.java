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
 * 
 * @author dormpower team
 * @version 1.0
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
}
