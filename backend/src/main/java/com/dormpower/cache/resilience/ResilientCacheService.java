package com.dormpower.cache.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 弹性缓存服务
 *
 * 提供带熔断保护的 Redis 操作
 *
 * 特点：
 * 1. 熔断保护 - Redis 故障时自动降级
 * 2. 超时控制 - 防止长时间阻塞
 * 3. 降级策略 - 提供 fallback 机制
 *
 * @author dormpower team
 * @version 1.0
 */
@Service
@ConditionalOnProperty(name = "spring.data.redis.host")
public class ResilientCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ResilientCacheService.class);

    @Autowired
    private CircuitBreaker circuitBreaker;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 带熔断保护的缓存读取
     *
     * @param key 缓存键
     * @param fallback 降级数据提供者
     * @return 缓存值或降级值
     */
    public String get(String key, Supplier<String> fallback) {
        Supplier<String> supplier = CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
            if (stringRedisTemplate == null) {
                return fallback.get();
            }
            return stringRedisTemplate.opsForValue().get(key);
        });

        try {
            return supplier.get();
        } catch (CallNotPermittedException e) {
            logger.warn("CircuitBreaker is OPEN, using fallback for key: {}", key);
            return fallback.get();
        } catch (Exception e) {
            logger.error("Redis operation failed for key: {}, error: {}", key, e.getMessage());
            return fallback.get();
        }
    }

    /**
     * 带熔断保护的缓存写入
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间
     * @return 是否成功
     */
    public boolean set(String key, String value, Duration ttl) {
        Supplier<Boolean> supplier = CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
            if (stringRedisTemplate == null) {
                return false;
            }
            stringRedisTemplate.opsForValue().set(key, value, ttl);
            return true;
        });

        try {
            return supplier.get();
        } catch (CallNotPermittedException e) {
            logger.warn("CircuitBreaker is OPEN, skip set for key: {}", key);
            return false;
        } catch (Exception e) {
            logger.error("Redis set failed for key: {}, error: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 带熔断保护的缓存删除
     *
     * @param key 缓存键
     * @return 是否成功
     */
    public boolean delete(String key) {
        Supplier<Boolean> supplier = CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
            if (stringRedisTemplate == null) {
                return false;
            }
            return Boolean.TRUE.equals(stringRedisTemplate.delete(key));
        });

        try {
            return supplier.get();
        } catch (CallNotPermittedException e) {
            logger.warn("CircuitBreaker is OPEN, skip delete for key: {}", key);
            return false;
        } catch (Exception e) {
            logger.error("Redis delete failed for key: {}, error: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 带熔断保护的操作执行
     *
     * @param operation 操作名称
     * @param action 要执行的操作
     * @param fallback 降级操作
     * @param <T> 返回类型
     * @return 操作结果
     */
    public <T> T execute(String operation, Supplier<T> action, Supplier<T> fallback) {
        Supplier<T> supplier = CircuitBreaker.decorateSupplier(circuitBreaker, action);

        try {
            return supplier.get();
        } catch (CallNotPermittedException e) {
            logger.warn("CircuitBreaker is OPEN, using fallback for operation: {}", operation);
            return fallback != null ? fallback.get() : null;
        } catch (Exception e) {
            logger.error("Redis operation '{}' failed: {}", operation, e.getMessage());
            return fallback != null ? fallback.get() : null;
        }
    }

    /**
     * 获取熔断器状态
     *
     * @return 状态字符串
     */
    public String getCircuitBreakerStatus() {
        return circuitBreaker.getState().name();
    }

    /**
     * 获取熔断器指标
     *
     * @return 指标信息
     */
    public CircuitBreakerMetrics getMetrics() {
        io.github.resilience4j.circuitbreaker.CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        float successRate = metrics.getNumberOfBufferedCalls() > 0
            ? (float) metrics.getNumberOfSuccessfulCalls() / metrics.getNumberOfBufferedCalls() * 100
            : 0;
        return new CircuitBreakerMetrics(
            metrics.getFailureRate(),
            successRate,
            metrics.getNumberOfBufferedCalls(),
            metrics.getNumberOfFailedCalls(),
            metrics.getNumberOfSuccessfulCalls()
        );
    }

    /**
     * 熔断器指标数据
     */
    public static class CircuitBreakerMetrics {
        private final float failureRate;
        private final float successRate;
        private final int bufferedCalls;
        private final int failedCalls;
        private final int successfulCalls;

        public CircuitBreakerMetrics(float failureRate, float successRate,
                                      int bufferedCalls, int failedCalls, int successfulCalls) {
            this.failureRate = failureRate;
            this.successRate = successRate;
            this.bufferedCalls = bufferedCalls;
            this.failedCalls = failedCalls;
            this.successfulCalls = successfulCalls;
        }

        public float getFailureRate() { return failureRate; }
        public float getSuccessRate() { return successRate; }
        public int getBufferedCalls() { return bufferedCalls; }
        public int getFailedCalls() { return failedCalls; }
        public int getSuccessfulCalls() { return successfulCalls; }

        @Override
        public String toString() {
            return String.format("CircuitBreakerMetrics{failureRate=%.1f%%, successRate=%.1f%%, " +
                "buffered=%d, failed=%d, success=%d}",
                failureRate, successRate, bufferedCalls, failedCalls, successfulCalls);
        }
    }
}