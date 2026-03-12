package com.dormpower.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 简单内存缓存服务
 * 
 * 替代Redis，适用于低内存环境
 * 特点：
 * 1. 线程安全
 * 2. 支持过期时间
 * 3. 自动清理过期数据
 * 4. 限制最大容量
 */
@Service
public class SimpleCacheService {

    private static final int MAX_SIZE = 500;
    private static final long DEFAULT_EXPIRE_MS = 30 * 60 * 1000;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    public SimpleCacheService() {
        cleaner.scheduleAtFixedRate(this::cleanExpired, 5, 5, TimeUnit.MINUTES);
    }

    private static class CacheEntry {
        final Object value;
        final long expireTime;

        CacheEntry(Object value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    /**
     * 存入缓存
     */
    public void put(String key, Object value) {
        put(key, value, DEFAULT_EXPIRE_MS);
    }

    /**
     * 存入缓存（带过期时间）
     */
    public void put(String key, Object value, long expireMs) {
        if (cache.size() >= MAX_SIZE) {
            cleanExpired();
            if (cache.size() >= MAX_SIZE) {
                return;
            }
        }
        cache.put(key, new CacheEntry(value, System.currentTimeMillis() + expireMs));
    }

    /**
     * 获取缓存
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return (T) entry.value;
    }

    /**
     * 获取缓存（带默认值）
     */
    public <T> T getOrDefault(String key, T defaultValue) {
        T value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 删除缓存
     */
    public void remove(String key) {
        cache.remove(key);
    }

    /**
     * 清空缓存
     */
    public void clear() {
        cache.clear();
    }

    /**
     * 缓存大小
     */
    public int size() {
        return cache.size();
    }

    /**
     * 是否存在
     */
    public boolean containsKey(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }
        return true;
    }

    /**
     * 清理过期数据
     */
    private void cleanExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 关闭清理线程
     */
    public void shutdown() {
        cleaner.shutdown();
    }
}
