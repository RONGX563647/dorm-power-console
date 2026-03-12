package com.dormpower.limiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式限流器
 *
 * 使用滑动窗口算法实现，基于 Redis Lua 脚本保证原子性。
 * 支持分布式环境下的限流控制。
 *
 * 特性：
 * 1. 滑动窗口算法，限流更精确
 * 2. Lua 脚本原子执行，无并发问题
 * 3. 支持多种限流场景（API级、用户级、设备级）
 *
 * @author dormpower team
 * @version 1.0
 */
@Component
public class RedisRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * 滑动窗口限流 Lua 脚本
     *
     * KEYS[1]: 限流 key
     * ARGV[1]: 窗口大小（毫秒）
     * ARGV[2]: 窗口内最大请求数
     * ARGV[3]: 当前时间戳（毫秒）
     *
     * 返回值：1 表示允许，0 表示拒绝
     */
    private static final String SLIDE_WINDOW_SCRIPT =
        "local key = KEYS[1] " +
        "local window = tonumber(ARGV[1]) " +
        "local maxRequests = tonumber(ARGV[2]) " +
        "local now = tonumber(ARGV[3]) " +
        "local windowStart = now - window " +
        // 删除过期的请求记录
        "redis.call('ZREMRANGEBYSCORE', key, 0, windowStart) " +
        // 获取当前窗口内的请求数
        "local currentRequests = redis.call('ZCARD', key) " +
        // 判断是否超过限制
        "if currentRequests < maxRequests then " +
        "    redis.call('ZADD', key, now, now .. '-' .. math.random()) " +
        "    redis.call('PEXPIRE', key, window) " +
        "    return 1 " +
        "else " +
        "    return 0 " +
        "end";

    private final DefaultRedisScript<Long> rateLimitScript;

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = new DefaultRedisScript<>();
        this.rateLimitScript.setScriptText(SLIDE_WINDOW_SCRIPT);
        this.rateLimitScript.setResultType(Long.class);
    }

    /**
     * 尝试获取令牌
     *
     * @param key 限流 key（如 "rate_limit:api:login"）
     * @param maxPermits 窗口内最大请求数
     * @param windowMs 窗口大小（毫秒）
     * @return true 表示允许通过，false 表示被限流
     */
    public boolean tryAcquire(String key, long maxPermits, long windowMs) {
        return tryAcquire(key, 1, maxPermits, windowMs);
    }

    /**
     * 尝试获取多个令牌
     *
     * @param key 限流 key
     * @param permits 请求令牌数
     * @param maxPermits 窗口内最大请求数
     * @param windowMs 窗口大小（毫秒）
     * @return true 表示允许通过，false 表示被限流
     */
    public boolean tryAcquire(String key, long permits, long maxPermits, long windowMs) {
        try {
            long now = System.currentTimeMillis();
            Long result = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(key),
                String.valueOf(windowMs),
                String.valueOf(maxPermits),
                String.valueOf(now)
            );

            boolean allowed = result != null && result == 1L;

            if (!allowed) {
                logger.debug("Rate limit exceeded for key: {}", key);
            }

            return allowed;
        } catch (Exception e) {
            logger.error("Rate limiter error for key: {}, error: {}", key, e.getMessage());
            // 限流器异常时，默认放行（降级策略）
            return true;
        }
    }

    /**
     * 获取当前窗口内的请求数
     *
     * @param key 限流 key
     * @param windowMs 窗口大小（毫秒）
     * @return 当前请求数
     */
    public long getCurrentCount(String key, long windowMs) {
        try {
            long now = System.currentTimeMillis();
            long windowStart = now - windowMs;
            Long count = redisTemplate.opsForZSet().count(key, windowStart, now);
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Failed to get current count for key: {}", key, e);
            return 0;
        }
    }

    /**
     * 重置限流计数
     *
     * @param key 限流 key
     */
    public void reset(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            logger.error("Failed to reset rate limit for key: {}", key, e);
        }
    }

    /**
     * 生成 API 限流 key
     *
     * @param apiName API 名称
     * @return 限流 key
     */
    public static String apiKey(String apiName) {
        return "rate_limit:api:" + apiName;
    }

    /**
     * 生成用户级限流 key
     *
     * @param userId 用户 ID
     * @param apiName API 名称
     * @return 限流 key
     */
    public static String userKey(String userId, String apiName) {
        return "rate_limit:user:" + userId + ":" + apiName;
    }

    /**
     * 生成设备级限流 key
     *
     * @param deviceId 设备 ID
     * @return 限流 key
     */
    public static String deviceKey(String deviceId) {
        return "rate_limit:device:" + deviceId;
    }
}