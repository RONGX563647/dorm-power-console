package com.dormpower.cache.protection;

import com.dormpower.cache.bloom.BloomFilterService;
import com.dormpower.cache.lock.DistributedLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;
import java.util.function.Supplier;

/**
 * 缓存防护服务
 *
 * 提供缓存穿透、击穿、雪崩的综合防护
 *
 * 防护策略：
 * 1. 缓存穿透：布隆过滤器 + 空值缓存
 * 2. 缓存击穿：分布式锁 + 互斥更新
 * 3. 缓存雪崩：随机 TTL 偏移
 *
 * @author dormpower team
 * @version 1.0
 */
@Service
public class CacheProtectionService {

    private static final Logger logger = LoggerFactory.getLogger(CacheProtectionService.class);

    /** 空值缓存标识 */
    private static final String NULL_VALUE = "NULL_PLACEHOLDER";

    /** TTL 随机偏移范围（秒） */
    private static final int TTL_RANDOM_OFFSET_SECONDS = 60;

    /** 获取锁超时时间（毫秒） */
    private static final long LOCK_WAIT_TIMEOUT_MS = 3000;

    private final Random random = new Random();

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private BloomFilterService bloomFilterService;

    @Autowired
    private DistributedLockService lockService;

    /**
     * 带防护的缓存查询
     *
     * 综合防护：穿透（布隆过滤器 + 空值缓存）+ 击穿（分布式锁）
     *
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param bloomFilterType 布隆过滤器类型（可选）
     * @param loader 数据加载器
     * @param <T> 返回类型
     * @return 缓存数据
     */
    public <T> T getWithProtection(String cacheName, String key, String bloomFilterType, Supplier<T> loader) {
        return getWithProtection(cacheName, key, bloomFilterType, loader, Duration.ZERO);
    }

    /**
     * 带防护的缓存查询（带 TTL）
     *
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param bloomFilterType 布隆过滤器类型（可选，传 null 跳过布隆过滤）
     * @param loader 数据加载器
     * @param ttl 缓存过期时间（0 表示使用默认 TTL）
     * @param <T> 返回类型
     * @return 缓存数据
     */
    public <T> T getWithProtection(String cacheName, String key, String bloomFilterType,
                                   Supplier<T> loader, Duration ttl) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            logger.warn("Cache not found: {}", cacheName);
            return loader.get();
        }

        // 1. 检查缓存
        Cache.ValueWrapper wrapper = cache.get(key);
        if (wrapper != null) {
            Object value = wrapper.get();
            // 检查是否为空值占位符
            if (NULL_VALUE.equals(value)) {
                logger.debug("Cache hit but null placeholder - cache: {}, key: {}", cacheName, key);
                return null;
            }
            @SuppressWarnings("unchecked")
            T result = (T) value;
            logger.debug("Cache hit - cache: {}, key: {}", cacheName, key);
            return result;
        }

        // 2. 布隆过滤器检查（防止穿透）
        if (bloomFilterType != null && !checkBloomFilter(bloomFilterType, key)) {
            logger.debug("Bloom filter miss, key definitely not exists - filter: {}, key: {}", bloomFilterType, key);
            return null;
        }

        // 3. 使用分布式锁防止击穿
        String lockKey = cacheName + ":" + key;
        try {
            return lockService.executeWithLock(lockKey, 30, LOCK_WAIT_TIMEOUT_MS, () -> {
                // 双重检查：获取锁后再次检查缓存
                Cache.ValueWrapper doubleCheck = cache.get(key);
                if (doubleCheck != null) {
                    Object value = doubleCheck.get();
                    if (NULL_VALUE.equals(value)) {
                        return null;
                    }
                    @SuppressWarnings("unchecked")
                    T result = (T) value;
                    return result;
                }

                // 加载数据
                T data = loader.get();

                // 写入缓存
                if (data == null) {
                    // 空值缓存（防止穿透）
                    cache.put(key, NULL_VALUE);
                    logger.debug("Cached null placeholder - cache: {}, key: {}", cacheName, key);
                } else {
                    // 正常缓存
                    cache.put(key, data);
                    logger.debug("Cached data - cache: {}, key: {}", cacheName, key);
                }

                return data;
            });
        } catch (DistributedLockService.LockAcquireException e) {
            logger.warn("Failed to acquire lock, fallback to direct load - key: {}", lockKey);
            // 锁获取失败，直接加载（降级策略）
            return loader.get();
        } catch (Exception e) {
            logger.error("Cache protection error - cache: {}, key: {}", cacheName, key, e);
            return loader.get();
        }
    }

    /**
     * 布隆过滤器检查
     *
     * @param filterType 过滤器类型
     * @param key 键
     * @return 是否可能存在
     */
    private boolean checkBloomFilter(String filterType, String key) {
        switch (filterType) {
            case BloomFilterService.DEVICE_FILTER:
                return bloomFilterService.mightContainDevice(key);
            case BloomFilterService.USER_FILTER:
                return bloomFilterService.mightContainUser(key);
            case BloomFilterService.ROOM_FILTER:
                return bloomFilterService.mightContainRoom(key);
            case BloomFilterService.BUILDING_FILTER:
                return bloomFilterService.mightContainBuilding(key);
            default:
                logger.warn("Unknown bloom filter type: {}", filterType);
                return true; // 未知类型，允许通过
        }
    }

    /**
     * 计算带随机偏移的 TTL
     *
     * 用于防止缓存雪崩
     *
     * @param baseTtl 基础 TTL（秒）
     * @return 带随机偏移的 TTL（秒）
     */
    public long calculateTtlWithOffset(long baseTtl) {
        int offset = random.nextInt(TTL_RANDOM_OFFSET_SECONDS);
        // 随机加减，避免所有缓存同时过期
        if (random.nextBoolean()) {
            return baseTtl + offset;
        } else {
            return Math.max(baseTtl - offset, baseTtl / 2);
        }
    }

    /**
     * 计算带随机偏移的 Duration
     *
     * @param baseDuration 基础 Duration
     * @return 带随机偏移的 Duration
     */
    public Duration calculateDurationWithOffset(Duration baseDuration) {
        long baseSeconds = baseDuration.getSeconds();
        long adjustedSeconds = calculateTtlWithOffset(baseSeconds);
        return Duration.ofSeconds(adjustedSeconds);
    }

    /**
     * 缓存失效（带布隆过滤器更新）
     *
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param bloomFilterType 布隆过滤器类型（可选）
     */
    public void evict(String cacheName, String key, String bloomFilterType) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            logger.debug("Cache evicted - cache: {}, key: {}", cacheName, key);
        }

        // 更新布隆过滤器
        if (bloomFilterType != null) {
            addToBloomFilter(bloomFilterType, key);
        }
    }

    /**
     * 添加到布隆过滤器
     */
    private void addToBloomFilter(String filterType, String key) {
        switch (filterType) {
            case BloomFilterService.DEVICE_FILTER:
                bloomFilterService.addDevice(key);
                break;
            case BloomFilterService.USER_FILTER:
                bloomFilterService.addUser(key);
                break;
            case BloomFilterService.ROOM_FILTER:
                bloomFilterService.addRoom(key);
                break;
            case BloomFilterService.BUILDING_FILTER:
                bloomFilterService.addBuilding(key);
                break;
            default:
                logger.warn("Unknown bloom filter type: {}", filterType);
        }
    }
}