package com.dormpower.cache.capacity;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Redis 容量治理服务
 *
 * 监控 Redis 内存使用，提供预警和容量管理
 *
 * 功能：
 * 1. 内存使用监控
 * 2. 内存预警（80%、90%）
 * 3. 键空间统计
 * 4. 慢日志分析
 *
 * @author dormpower team
 * @version 1.0
 */
@Service
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisCapacityMonitor {

    private static final Logger logger = LoggerFactory.getLogger(RedisCapacityMonitor.class);

    /** 内存预警阈值（百分比） */
    @Value("${redis.capacity.memory-warning-percent:80}")
    private int memoryWarningPercent;

    /** 内存告警阈值（百分比） */
    @Value("${redis.capacity.memory-critical-percent:90}")
    private int memoryCriticalPercent;

    /** 检查间隔（毫秒） */
    @Value("${redis.capacity.check-interval-ms:60000}")
    private long checkIntervalMs;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    /** 内存使用量 Gauge */
    private volatile long usedMemory = 0;
    private volatile long maxMemory = 0;
    private volatile double memoryUsagePercent = 0;
    private volatile long keysCount = 0;

    /** 上次告警时间 */
    private volatile long lastWarningTime = 0;
    private volatile long lastCriticalTime = 0;

    /** 告警冷却时间（毫秒） */
    private static final long ALERT_COOLDOWN_MS = 300000; // 5 分钟

    /**
     * 初始化监控指标
     */
    @Autowired(required = false)
    public void initMetrics() {
        if (meterRegistry != null) {
            Gauge.builder("redis.memory.used", () -> usedMemory)
                .description("Redis used memory in bytes")
                .register(meterRegistry);

            Gauge.builder("redis.memory.max", () -> maxMemory)
                .description("Redis max memory in bytes")
                .register(meterRegistry);

            Gauge.builder("redis.memory.usage.percent", () -> memoryUsagePercent)
                .description("Redis memory usage percent")
                .register(meterRegistry);

            Gauge.builder("redis.keys.count", () -> keysCount)
                .description("Redis total keys count")
                .register(meterRegistry);

            logger.info("Redis capacity monitor initialized, warning threshold: {}%, critical: {}%",
                memoryWarningPercent, memoryCriticalPercent);
        }
    }

    /**
     * 定时检查内存使用
     */
    @Scheduled(fixedRateString = "${redis.capacity.check-interval-ms:60000}")
    public void checkMemory() {
        if (stringRedisTemplate == null) {
            return;
        }

        try {
            // 获取 Redis INFO memory
            Properties memoryInfo = stringRedisTemplate.execute((connection) -> {
                Properties props = connection.info("memory");
                return props != null ? props : new Properties();
            }, true);

            if (memoryInfo == null) {
                return;
            }

            // 解析内存信息
            parseMemoryInfo(memoryInfo);

            // 获取键数量
            keysCount = getKeyCount();

            // 检查预警
            checkAndAlert();

        } catch (Exception e) {
            logger.error("Failed to check Redis memory: {}", e.getMessage());
        }
    }

    /**
     * 解析内存信息
     */
    private void parseMemoryInfo(Properties info) {
        String usedMemoryStr = info.getProperty("used_memory");
        String maxMemoryStr = info.getProperty("maxmemory");

        if (usedMemoryStr != null) {
            usedMemory = Long.parseLong(usedMemoryStr);
        }

        if (maxMemoryStr != null) {
            maxMemory = Long.parseLong(maxMemoryStr);
        }

        // 计算使用率
        if (maxMemory > 0) {
            memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        } else {
            // 如果没有设置 maxmemory，尝试使用系统内存
            String totalSystemMemory = info.getProperty("total_system_memory");
            if (totalSystemMemory != null) {
                maxMemory = Long.parseLong(totalSystemMemory);
                memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            } else {
                memoryUsagePercent = 0;
            }
        }
    }

    /**
     * 获取键数量
     */
    private long getKeyCount() {
        try {
            Long size = stringRedisTemplate.execute((connection) -> {
                Long dbSize = connection.dbSize();
                return dbSize != null ? dbSize : 0L;
            }, true);
            return size != null ? size : 0;
        } catch (Exception e) {
            logger.warn("Failed to get key count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 检查并告警
     */
    private void checkAndAlert() {
        long now = System.currentTimeMillis();

        // 告警级别
        if (memoryUsagePercent >= memoryCriticalPercent) {
            if (now - lastCriticalTime > ALERT_COOLDOWN_MS) {
                lastCriticalTime = now;
                logger.error("!!! CRITICAL: Redis memory usage is {:.1f}% (used: {}MB, max: {}MB) !!!",
                    memoryUsagePercent, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024);
                // 可以集成告警通知系统
            }
        } else if (memoryUsagePercent >= memoryWarningPercent) {
            if (now - lastWarningTime > ALERT_COOLDOWN_MS) {
                lastWarningTime = now;
                logger.warn("WARNING: Redis memory usage is {:.1f}% (used: {}MB, max: {}MB)",
                    memoryUsagePercent, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024);
            }
        } else {
            logger.debug("Redis memory usage: {:.1f}% (used: {}MB, max: {}MB, keys: {})",
                memoryUsagePercent, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024, keysCount);
        }
    }

    /**
     * 获取容量统计
     */
    public CapacityStats getStats() {
        return new CapacityStats(
            usedMemory,
            maxMemory,
            memoryUsagePercent,
            keysCount,
            memoryUsagePercent >= memoryCriticalPercent ? AlertLevel.CRITICAL :
                memoryUsagePercent >= memoryWarningPercent ? AlertLevel.WARNING : AlertLevel.NORMAL
        );
    }

    /**
     * 获取详细的内存报告
     */
    public MemoryReport getMemoryReport() {
        if (stringRedisTemplate == null) {
            return null;
        }

        try {
            Properties info = stringRedisTemplate.execute((connection) -> {
                Properties props = connection.info("memory");
                return props != null ? props : new Properties();
            }, true);

            return new MemoryReport(
                parseLong(info.getProperty("used_memory")),
                parseLong(info.getProperty("used_memory_rss")),
                parseLong(info.getProperty("used_memory_peak")),
                parseLong(info.getProperty("maxmemory")),
                info.getProperty("mem_fragmentation_ratio"),
                info.getProperty("eviction_policy", "noeviction")
            );
        } catch (Exception e) {
            logger.error("Failed to get memory report: {}", e.getMessage());
            return null;
        }
    }

    private long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 容量统计
     */
    public static class CapacityStats {
        private final long usedMemory;
        private final long maxMemory;
        private final double usagePercent;
        private final long keysCount;
        private final AlertLevel alertLevel;

        public CapacityStats(long usedMemory, long maxMemory, double usagePercent,
                              long keysCount, AlertLevel alertLevel) {
            this.usedMemory = usedMemory;
            this.maxMemory = maxMemory;
            this.usagePercent = usagePercent;
            this.keysCount = keysCount;
            this.alertLevel = alertLevel;
        }

        public long getUsedMemory() { return usedMemory; }
        public long getMaxMemory() { return maxMemory; }
        public double getUsagePercent() { return usagePercent; }
        public long getKeysCount() { return keysCount; }
        public AlertLevel getAlertLevel() { return alertLevel; }

        @Override
        public String toString() {
            return String.format("CapacityStats{used=%dMB, max=%dMB, usage=%.1f%%, keys=%d, level=%s}",
                usedMemory / 1024 / 1024, maxMemory / 1024 / 1024, usagePercent, keysCount, alertLevel);
        }
    }

    /**
     * 内存报告
     */
    public static class MemoryReport {
        private final long usedMemory;
        private final long usedMemoryRss;
        private final long usedMemoryPeak;
        private final long maxMemory;
        private final String fragmentationRatio;
        private final String evictionPolicy;

        public MemoryReport(long usedMemory, long usedMemoryRss, long usedMemoryPeak,
                            long maxMemory, String fragmentationRatio, String evictionPolicy) {
            this.usedMemory = usedMemory;
            this.usedMemoryRss = usedMemoryRss;
            this.usedMemoryPeak = usedMemoryPeak;
            this.maxMemory = maxMemory;
            this.fragmentationRatio = fragmentationRatio;
            this.evictionPolicy = evictionPolicy;
        }

        public long getUsedMemory() { return usedMemory; }
        public long getUsedMemoryRss() { return usedMemoryRss; }
        public long getUsedMemoryPeak() { return usedMemoryPeak; }
        public long getMaxMemory() { return maxMemory; }
        public String getFragmentationRatio() { return fragmentationRatio; }
        public String getEvictionPolicy() { return evictionPolicy; }
    }

    /**
     * 告警级别
     */
    public enum AlertLevel {
        NORMAL,
        WARNING,
        CRITICAL
    }
}