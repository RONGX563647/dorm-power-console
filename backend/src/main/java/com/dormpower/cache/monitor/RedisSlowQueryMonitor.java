package com.dormpower.cache.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 慢查询监控服务
 *
 * 监控 Redis 操作耗时，记录慢查询日志
 *
 * 特点：
 * 1. 慢查询阈值可配置
 * 2. 慢查询计数统计
 * 3. 操作耗时分布
 * 4. 定时汇总报告
 *
 * @author dormpower team
 * @version 1.0
 */
@Service
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisSlowQueryMonitor {

    private static final Logger logger = LoggerFactory.getLogger(RedisSlowQueryMonitor.class);

    /** 慢查询阈值（毫秒） */
    @Value("${redis.monitor.slow-query-threshold-ms:100}")
    private long slowQueryThresholdMs;

    /** 慢查询日志保留数量 */
    @Value("${redis.monitor.slow-query-log-size:100}")
    private int slowQueryLogSize;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /** 慢查询计数器 */
    private Counter slowQueryCounter;

    /** 操作计时器 */
    private Timer operationTimer;

    /** 慢查询统计（按操作类型） */
    private final Map<String, LongAdder> slowQueryStats = new ConcurrentHashMap<>();

    /** 总操作次数 */
    private final LongAdder totalOperations = new LongAdder();

    /** 慢查询次数 */
    private final LongAdder slowQueryCount = new LongAdder();

    /**
     * 初始化监控指标
     */
    @Autowired(required = false)
    public void initMetrics() {
        if (meterRegistry != null) {
            slowQueryCounter = Counter.builder("redis.slow.query.count")
                .description("Redis slow query count")
                .tag("type", "total")
                .register(meterRegistry);

            operationTimer = Timer.builder("redis.operation.duration")
                .description("Redis operation duration")
                .register(meterRegistry);

            logger.info("Redis slow query monitor initialized, threshold: {}ms", slowQueryThresholdMs);
        }
    }

    /**
     * 包装 Redis 操作，记录慢查询
     *
     * @param operation 操作名称
     * @param action 要执行的操作
     * @param <T> 返回类型
     * @return 操作结果
     */
    public <T> T executeWithMonitor(String operation, Supplier<T> action) {
        long start = System.nanoTime();
        totalOperations.increment();

        try {
            T result = action.get();
            recordSuccess(operation, System.nanoTime() - start);
            return result;
        } catch (Exception e) {
            recordError(operation, System.nanoTime() - start, e);
            throw e;
        }
    }

    /**
     * 包装无返回值的 Redis 操作
     */
    public void executeWithMonitor(String operation, Runnable action) {
        executeWithMonitor(operation, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 记录成功操作
     */
    private void recordSuccess(String operation, long durationNanos) {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);

        // 记录到 Micrometer
        if (operationTimer != null) {
            operationTimer.record(durationNanos, TimeUnit.NANOSECONDS);
        }

        // 检查是否为慢查询
        if (durationMs > slowQueryThresholdMs) {
            recordSlowQuery(operation, durationMs);
        }
    }

    /**
     * 记录错误操作
     */
    private void recordError(String operation, long durationNanos, Exception e) {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);
        logger.error("Redis operation failed: {} - error: {}, duration: {}ms",
            operation, e.getMessage(), durationMs);

        if (durationMs > slowQueryThresholdMs) {
            recordSlowQuery(operation, durationMs);
        }
    }

    /**
     * 记录慢查询
     */
    private void recordSlowQuery(String operation, long durationMs) {
        slowQueryCount.increment();
        slowQueryStats.computeIfAbsent(operation, k -> new LongAdder()).increment();

        if (slowQueryCounter != null) {
            slowQueryCounter.increment();
        }

        logger.warn("Redis slow query detected: operation={}, duration={}ms, threshold={}ms",
            operation, durationMs, slowQueryThresholdMs);
    }

    /**
     * 定时汇总报告
     */
    @Scheduled(fixedRateString = "${redis.monitor.report-interval-ms:60000}")
    public void reportStats() {
        if (totalOperations.sum() == 0) {
            return;
        }

        long total = totalOperations.sum();
        long slow = slowQueryCount.sum();
        double slowRate = (double) slow / total * 100;

        if (slow > 0) {
            logger.info("Redis operation stats: total={}, slow={}, slowRate={:.2f}%",
                total, slow, slowRate);

            // 输出各操作的慢查询统计
            slowQueryStats.forEach((op, count) -> {
                logger.info("  - {}: {} slow queries", op, count.sum());
            });
        }

        // 重置计数器
        resetCounters();
    }

    /**
     * 重置计数器
     */
    private void resetCounters() {
        totalOperations.reset();
        slowQueryCount.reset();
        slowQueryStats.clear();
    }

    /**
     * 获取 Redis 慢日志（从 Redis 服务器获取）
     */
    public void logRedisSlowLog() {
        if (stringRedisTemplate == null) {
            return;
        }

        try {
            // 使用 SLOWLOG GET 命令获取慢日志
            stringRedisTemplate.execute((connection) -> {
                Object result = connection.execute("SLOWLOG", "GET".getBytes(), "10".getBytes());
                if (result != null) {
                    logger.info("Redis SLOWLOG: {}", result);
                }
                return null;
            }, true);
        } catch (Exception e) {
            logger.warn("Failed to get Redis slowlog: {}", e.getMessage());
        }
    }

    /**
     * 简单的 LongAdder 包装
     */
    private static class LongAdder {
        private long value = 0;

        public synchronized void increment() {
            value++;
        }

        public synchronized long sum() {
            return value;
        }

        public synchronized void reset() {
            value = 0;
        }
    }
}