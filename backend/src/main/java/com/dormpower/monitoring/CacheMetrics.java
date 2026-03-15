package com.dormpower.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 缓存监控指标
 * 
 * 提供缓存性能监控指标
 * 
 * 指标类型：
 * 1. 缓存命中/未命中计数
 * 2. 缓存操作延迟
 * 3. 缓存大小
 * 4. 缓存操作次数
 * 
 * @author dormpower team
 * @version 1.0
 */
@Component
public class CacheMetrics {

    private final MeterRegistry meterRegistry;
    
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cachePutCounter;
    private final Counter cacheEvictCounter;
    private final Counter cacheClearCounter;
    
    private final Timer cacheGetTimer;
    private final Timer cachePutTimer;

    public CacheMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.cacheHitCounter = Counter.builder("cache.hits")
            .description("Cache hit count")
            .tag("cache", "multi-level")
            .register(meterRegistry);
        
        this.cacheMissCounter = Counter.builder("cache.misses")
            .description("Cache miss count")
            .tag("cache", "multi-level")
            .register(meterRegistry);
        
        this.cachePutCounter = Counter.builder("cache.puts")
            .description("Cache put count")
            .tag("cache", "multi-level")
            .register(meterRegistry);
        
        this.cacheEvictCounter = Counter.builder("cache.evicts")
            .description("Cache evict count")
            .tag("cache", "multi-level")
            .register(meterRegistry);
        
        this.cacheClearCounter = Counter.builder("cache.clears")
            .description("Cache clear count")
            .tag("cache", "multi-level")
            .register(meterRegistry);
        
        this.cacheGetTimer = Timer.builder("cache.get.time")
            .description("Cache get operation time")
            .tag("cache", "multi-level")
            .register(meterRegistry);
        
        this.cachePutTimer = Timer.builder("cache.put.time")
            .description("Cache put operation time")
            .tag("cache", "multi-level")
            .register(meterRegistry);
    }

    public void recordHit() {
        cacheHitCounter.increment();
    }

    public void recordMiss() {
        cacheMissCounter.increment();
    }

    public void recordPut() {
        cachePutCounter.increment();
    }

    public void recordEvict() {
        cacheEvictCounter.increment();
    }

    public void recordClear() {
        cacheClearCounter.increment();
    }

    public void recordGetTime(long duration, TimeUnit unit) {
        cacheGetTimer.record(duration, unit);
    }

    public void recordPutTime(long duration, TimeUnit unit) {
        cachePutTimer.record(duration, unit);
    }

    public void recordCacheSize(String cacheName, int size) {
        Gauge.builder("cache.size", () -> size)
            .description("Cache size")
            .tag("cache", cacheName)
            .register(meterRegistry);
    }

    public double getHitRate() {
        double hits = cacheHitCounter.count();
        double misses = cacheMissCounter.count();
        double total = hits + misses;
        
        return total > 0 ? hits / total : 0.0;
    }
}
