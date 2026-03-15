package com.dormpower.cache.metrics;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 缓存指标监控服务
 *
 * 集成 Micrometer + Prometheus，提供缓存性能监控
 *
 * 监控指标：
 * 1. 命中率（Hit Rate）
 * 2. 未命中数（Miss Count）
 * 3. 驱逐数（Eviction Count）
 * 4. 加载时间（Load Time）
 * 5. 缓存大小（Cache Size）
 *
 * @author dormpower team
 * @version 1.0
 */
@Service
public class CacheMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(CacheMetricsService.class);

    /** 指标名称前缀 */
    private static final String METRIC_PREFIX = "cache.";

    @Autowired(required = false)
    private CaffeineCacheManager caffeineCacheManager;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    /** 缓存命中计数器 */
    private Counter hitCounter;

    /** 缓存未命中计数器 */
    private Counter missCounter;

    /** 缓存驱逐计数器 */
    private Counter evictionCounter;

    /** 缓存加载计时器 */
    private Timer loadTimer;

    /**
     * 初始化监控指标
     */
    @Autowired(required = false)
    public void initMetrics() {
        if (meterRegistry == null) {
            logger.warn("MeterRegistry not available, metrics collection disabled");
            return;
        }

        // 初始化计数器
        hitCounter = Counter.builder(METRIC_PREFIX + "hits")
            .description("Cache hit count")
            .register(meterRegistry);

        missCounter = Counter.builder(METRIC_PREFIX + "misses")
            .description("Cache miss count")
            .register(meterRegistry);

        evictionCounter = Counter.builder(METRIC_PREFIX + "evictions")
            .description("Cache eviction count")
            .register(meterRegistry);

        loadTimer = Timer.builder(METRIC_PREFIX + "load.time")
            .description("Cache load time")
            .register(meterRegistry);

        // 注册 Caffeine 缓存指标
        registerCaffeineMetrics();

        logger.info("Cache metrics service initialized");
    }

    /**
     * 注册 Caffeine 缓存指标到 Micrometer
     */
    private void registerCaffeineMetrics() {
        if (caffeineCacheManager == null || meterRegistry == null) {
            return;
        }

        for (String cacheName : caffeineCacheManager.getCacheNames()) {
            CaffeineCache cache = (CaffeineCache) caffeineCacheManager.getCache(cacheName);
            if (cache == null) {
                continue;
            }

            Cache<Object, Object> nativeCache = cache.getNativeCache();
            String finalCacheName = cacheName;

            // 命中率
            Gauge.builder(METRIC_PREFIX + "hit.rate", nativeCache, c -> c.stats().hitRate())
                .tag("cache", finalCacheName)
                .description("Cache hit rate")
                .register(meterRegistry);

            // 缓存大小
            Gauge.builder(METRIC_PREFIX + "size", nativeCache, c -> c.estimatedSize())
                .tag("cache", finalCacheName)
                .description("Cache size")
                .register(meterRegistry);

            // 未命中数
            Gauge.builder(METRIC_PREFIX + "miss.count", nativeCache, c -> c.stats().missCount())
                .tag("cache", finalCacheName)
                .description("Cache miss count")
                .register(meterRegistry);

            // 驱逐数
            Gauge.builder(METRIC_PREFIX + "eviction.count", nativeCache, c -> c.stats().evictionCount())
                .tag("cache", finalCacheName)
                .description("Cache eviction count")
                .register(meterRegistry);

            // 加载失败数
            Gauge.builder(METRIC_PREFIX + "load.failure.count", nativeCache, c -> c.stats().loadFailureCount())
                .tag("cache", finalCacheName)
                .description("Cache load failure count")
                .register(meterRegistry);

            logger.debug("Registered metrics for cache: {}", cacheName);
        }
    }

    /**
     * 定时采集缓存统计
     */
    @Scheduled(fixedRate = 60000)
    public void collectMetrics() {
        if (caffeineCacheManager == null) {
            return;
        }

        for (String cacheName : caffeineCacheManager.getCacheNames()) {
            CaffeineCache cache = (CaffeineCache) caffeineCacheManager.getCache(cacheName);
            if (cache == null) {
                continue;
            }

            CacheStats stats = cache.getNativeCache().stats();

            // 更新计数器
            if (hitCounter != null) {
                hitCounter.increment(stats.hitCount());
            }
            if (missCounter != null) {
                missCounter.increment(stats.missCount());
            }
            if (evictionCounter != null) {
                evictionCounter.increment(stats.evictionCount());
            }

            // 记录日志
            if (logger.isDebugEnabled()) {
                logger.debug("Cache stats - name: {}, hits: {}, misses: {}, hitRate: {:.2f}%, evictions: {}, size: {}",
                    cacheName,
                    stats.hitCount(),
                    stats.missCount(),
                    stats.hitRate() * 100,
                    stats.evictionCount(),
                    cache.getNativeCache().estimatedSize());
            }
        }
    }

    /**
     * 记录缓存加载时间
     *
     * @param duration 加载耗时（纳秒）
     */
    public void recordLoadTime(long duration) {
        if (loadTimer != null) {
            loadTimer.record(duration, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * 获取缓存统计摘要
     *
     * @param cacheName 缓存名称
     * @return 统计摘要字符串
     */
    public String getCacheStatsSummary(String cacheName) {
        if (caffeineCacheManager == null) {
            return "Cache manager not available";
        }

        CaffeineCache cache = (CaffeineCache) caffeineCacheManager.getCache(cacheName);
        if (cache == null) {
            return "Cache not found: " + cacheName;
        }

        CacheStats stats = cache.getNativeCache().stats();
        return String.format(
            "Cache[%s] - HitRate: %.2f%%, Hits: %d, Misses: %d, Evictions: %d, Size: %d",
            cacheName,
            stats.hitRate() * 100,
            stats.hitCount(),
            stats.missCount(),
            stats.evictionCount(),
            cache.getNativeCache().estimatedSize()
        );
    }

    /**
     * 检查缓存健康状态
     *
     * @param cacheName 缓存名称
     * @param minHitRate 最低命中率阈值
     * @return 是否健康
     */
    public boolean isCacheHealthy(String cacheName, double minHitRate) {
        if (caffeineCacheManager == null) {
            return false;
        }

        CaffeineCache cache = (CaffeineCache) caffeineCacheManager.getCache(cacheName);
        if (cache == null) {
            return false;
        }

        CacheStats stats = cache.getNativeCache().stats();
        return stats.hitRate() >= minHitRate;
    }
}