package com.dormpower.cache.sharding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * 分片缓存实现
 * 
 * 根据分片策略路由到不同的分片缓存
 * 
 * @author dormpower team
 * @version 1.0
 */
public class ShardedCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(ShardedCache.class);

    private final String cacheName;
    private final ShardStrategy shardStrategy;
    private final CacheManager cacheManager;

    public ShardedCache(String cacheName, ShardStrategy shardStrategy, CacheManager cacheManager) {
        this.cacheName = cacheName;
        this.shardStrategy = shardStrategy;
        this.cacheManager = cacheManager;
    }

    @Override
    public String getName() {
        return this.cacheName;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(Object key) {
        String shardKey = shardStrategy.getShardKey(cacheName, key);
        Cache shardCache = getShardCache(shardKey);
        
        if (shardCache != null) {
            return shardCache.get(key);
        }
        
        return null;
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
        String shardKey = shardStrategy.getShardKey(cacheName, key);
        Cache shardCache = getShardCache(shardKey);
        
        if (shardCache != null) {
            shardCache.put(key, value);
            logger.debug("Cache PUT - shard: {}, key: {}", shardKey, key);
        }
    }

    @Override
    public void evict(Object key) {
        String shardKey = shardStrategy.getShardKey(cacheName, key);
        Cache shardCache = getShardCache(shardKey);
        
        if (shardCache != null) {
            shardCache.evict(key);
            logger.debug("Cache EVICT - shard: {}, key: {}", shardKey, key);
        }
    }

    @Override
    public void clear() {
        List<String> allShards = shardStrategy.getAllShards(cacheName);
        
        for (String shard : allShards) {
            Cache shardCache = getShardCache(shard);
            if (shardCache != null) {
                shardCache.clear();
            }
        }
        
        logger.debug("Cache CLEAR - cache: {}, shards: {}", cacheName, allShards.size());
    }

    private Cache getShardCache(String shardKey) {
        return cacheManager.getCache(shardKey);
    }
}
