package com.dormpower.cache.hotkey;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 热点 Key 检测服务
 *
 * 自动检测访问频率异常高的 Key，实现：
 * 1. 热点告警
 * 2. 自动提升到本地缓存
 * 3. 防止热点 Key 攻击
 *
 * 检测算法：滑动窗口计数
 *
 * @author dormpower team
 * @version 1.0
 */
@Service
@ConditionalOnProperty(name = "spring.data.redis.host")
public class HotKeyDetectorService {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyDetectorService.class);

    /** 热点阈值：每秒访问次数 */
    @Value("${redis.hotkey.threshold-qps:100}")
    private int hotKeyThresholdQps;

    /** 滑动窗口大小（毫秒） */
    @Value("${redis.hotkey.window-size-ms:1000}")
    private long windowSizeMs;

    /** 热点 Key 本地缓存时间（秒） */
    @Value("${redis.hotkey.local-cache-seconds:60}")
    private int localCacheSeconds;

    /** 热点 Key 告警阈值（连续 N 次检测到才告警） */
    @Value("${redis.hotkey.alert-threshold:3}")
    private int alertThreshold;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    private HotKeyHandler hotKeyHandler;

    /** 滑动窗口计数器 */
    private final ConcurrentHashMap<String, SlidingWindowCounter> counters = new ConcurrentHashMap<>();

    /** 热点 Key 连续检测次数 */
    private final ConcurrentHashMap<String, AtomicLong> hotKeyDetectCount = new ConcurrentHashMap<>();

    /** 已提升的热点 Key */
    private final ConcurrentHashMap<String, Long> promotedKeys = new ConcurrentHashMap<>();

    /** 热点 Key 计数器 */
    private Counter hotKeyCounter;

    /** 当前热点 Key 数量 */
    private final AtomicLong currentHotKeyCount = new AtomicLong(0);

    /**
     * 初始化监控指标
     */
    @Autowired(required = false)
    public void initMetrics() {
        if (meterRegistry != null) {
            hotKeyCounter = Counter.builder("redis.hotkey.count")
                .description("Hot key detected count")
                .register(meterRegistry);

            Gauge.builder("redis.hotkey.current", currentHotKeyCount, AtomicLong::get)
                .description("Current hot key count")
                .register(meterRegistry);

            logger.info("Hot key detector initialized, threshold: {} QPS, window: {}ms",
                hotKeyThresholdQps, windowSizeMs);
        }
    }

    /**
     * 记录 Key 访问
     *
     * @param key 缓存键
     */
    public void recordAccess(String key) {
        SlidingWindowCounter counter = counters.computeIfAbsent(key,
            k -> new SlidingWindowCounter(windowSizeMs));
        counter.increment();
    }

    /**
     * 记录带缓存名的 Key 访问
     */
    public void recordAccess(String cacheName, String key) {
        recordAccess(cacheName + ":" + key);
    }

    /**
     * 检测热点 Key
     */
    @Scheduled(fixedRateString = "${redis.hotkey.detect-interval-ms:1000}")
    public void detectHotKeys() {
        long now = System.currentTimeMillis();
        int hotKeyFound = 0;

        for (Map.Entry<String, SlidingWindowCounter> entry : counters.entrySet()) {
            String key = entry.getKey();
            SlidingWindowCounter counter = entry.getValue();

            long qps = counter.getCountAndReset(now);
            if (qps >= hotKeyThresholdQps) {
                hotKeyFound++;
                handleHotKey(key, qps);
            }
        }

        currentHotKeyCount.set(hotKeyFound);

        // 清理过期的计数器
        cleanupExpiredCounters(now);
    }

    /**
     * 处理热点 Key
     */
    private void handleHotKey(String key, long qps) {
        // 增加连续检测次数
        AtomicLong detectCount = hotKeyDetectCount.computeIfAbsent(key, k -> new AtomicLong(0));
        long count = detectCount.incrementAndGet();

        // 达到告警阈值
        if (count >= alertThreshold) {
            logger.warn("Hot key detected: key={}, QPS={}, consecutiveDetects={}",
                key, qps, count);

            if (hotKeyCounter != null) {
                hotKeyCounter.increment();
            }

            // 提升到本地缓存
            promoteToLocalCache(key);

            // 调用处理器
            if (hotKeyHandler != null) {
                hotKeyHandler.onHotKeyDetected(key, qps);
            }
        } else {
            logger.info("Potential hot key: key={}, QPS={}, detectCount={}/{}",
                key, qps, count, alertThreshold);
        }
    }

    /**
     * 提升热点 Key 到本地缓存
     */
    private void promoteToLocalCache(String key) {
        if (promotedKeys.containsKey(key)) {
            return; // 已经提升过了
        }

        promotedKeys.put(key, System.currentTimeMillis());
        logger.info("Promoting hot key to local cache: key={}, localCacheSeconds={}",
            key, localCacheSeconds);

        if (hotKeyHandler != null) {
            hotKeyHandler.promoteToLocalCache(key, Duration.ofSeconds(localCacheSeconds));
        }
    }

    /**
     * 检查 Key 是否为热点 Key
     */
    public boolean isHotKey(String key) {
        return promotedKeys.containsKey(key);
    }

    /**
     * 检查 Key 是否为热点 Key
     */
    public boolean isHotKey(String cacheName, String key) {
        return isHotKey(cacheName + ":" + key);
    }

    /**
     * 移除热点 Key 标记（数据更新时调用）
     */
    public void removeHotKey(String key) {
        promotedKeys.remove(key);
        hotKeyDetectCount.remove(key);
        counters.remove(key);
    }

    /**
     * 清理过期的计数器
     */
    private void cleanupExpiredCounters(long now) {
        // 清理超过 10 秒未访问的计数器
        long expireThreshold = now - 10000;

        counters.entrySet().removeIf(entry -> {
            SlidingWindowCounter counter = entry.getValue();
            return counter.getLastAccessTime() < expireThreshold;
        });

        // 清理过期的热点 Key（超过本地缓存时间）
        long promoteExpire = now - localCacheSeconds * 1000L;
        promotedKeys.entrySet().removeIf(entry -> entry.getValue() < promoteExpire);
    }

    /**
     * 获取当前热点 Key 统计
     */
    public HotKeyStats getStats() {
        return new HotKeyStats(
            counters.size(),
            promotedKeys.size(),
            hotKeyDetectCount.size()
        );
    }

    /**
     * 热点 Key 统计
     */
    public static class HotKeyStats {
        private final int trackedKeys;
        private final int promotedKeys;
        private final int suspectedKeys;

        public HotKeyStats(int trackedKeys, int promotedKeys, int suspectedKeys) {
            this.trackedKeys = trackedKeys;
            this.promotedKeys = promotedKeys;
            this.suspectedKeys = suspectedKeys;
        }

        public int getTrackedKeys() { return trackedKeys; }
        public int getPromotedKeys() { return promotedKeys; }
        public int getSuspectedKeys() { return suspectedKeys; }

        @Override
        public String toString() {
            return String.format("HotKeyStats{tracked=%d, promoted=%d, suspected=%d}",
                trackedKeys, promotedKeys, suspectedKeys);
        }
    }

    /**
     * 滑动窗口计数器
     */
    private static class SlidingWindowCounter {
        private final long windowSizeMs;
        private final ConcurrentHashMap<Long, AtomicLong> windows = new ConcurrentHashMap<>();
        private volatile long lastAccessTime;

        public SlidingWindowCounter(long windowSizeMs) {
            this.windowSizeMs = windowSizeMs;
            this.lastAccessTime = System.currentTimeMillis();
        }

        public void increment() {
            long windowStart = System.currentTimeMillis() / windowSizeMs;
            windows.computeIfAbsent(windowStart, k -> new AtomicLong(0)).incrementAndGet();
            lastAccessTime = System.currentTimeMillis();
        }

        public long getCountAndReset(long now) {
            long currentWindow = now / windowSizeMs;
            long previousWindow = currentWindow - 1;

            long count = 0;

            // 获取当前窗口计数
            AtomicLong current = windows.get(currentWindow);
            if (current != null) {
                count += current.get();
            }

            // 获取上一窗口计数（部分）
            AtomicLong previous = windows.get(previousWindow);
            if (previous != null) {
                count += previous.get();
                windows.remove(previousWindow); // 清理旧窗口
            }

            // 清理更旧的窗口
            windows.keySet().removeIf(k -> k < previousWindow);

            return count;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }
    }

    /**
     * 热点 Key 处理器接口
     */
    public interface HotKeyHandler {
        /**
         * 热点 Key 被检测到
         */
        void onHotKeyDetected(String key, long qps);

        /**
         * 提升到本地缓存
         */
        void promoteToLocalCache(String key, Duration ttl);
    }
}