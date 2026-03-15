package com.dormpower.cache.sharding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 分片缓存管理器
 * 
 * 管理多个分片缓存
 * 
 * @author dormpower team
 * @version 1.0
 */
public class ShardedCacheManager implements CacheManager {

    private static final Logger logger = LoggerFactory.getLogger(ShardedCacheManager.class);

    private final ShardStrategy shardStrategy;
    private final CacheManager delegateCacheManager;
    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

    public ShardedCacheManager(ShardStrategy shardStrategy, CacheManager delegateCacheManager) {
        this.shardStrategy = shardStrategy;
        this.delegateCacheManager = delegateCacheManager;
        logger.info("ShardedCacheManager initialized with strategy: {}", 
            shardStrategy.getClass().getSimpleName());
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, key -> {
            logger.debug("Created sharded cache for name: {}", key);
            return new ShardedCache(key, shardStrategy, delegateCacheManager);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(cacheMap.keySet());
    }
}
