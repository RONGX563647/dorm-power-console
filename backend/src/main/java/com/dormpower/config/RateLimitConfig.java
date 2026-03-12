package com.dormpower.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 限流配置类
 *
 * 使用 Redis 实现分布式限流，支持多实例部署场景。
 *
 * 限流策略：
 * 1. API通用限流：限制每秒10个请求，适用于大多数API接口
 * 2. 登录接口限流：限制每秒2个请求，防止暴力破解
 * 3. 设备级限流：为每个设备单独设置限流器，限制每秒5个请求
 *
 * @author DormPower Team
 * @version 2.0
 */
@Configuration
public class RateLimitConfig {

    // ========== 限流常量配置 ==========

    /**
     * API 通用限流窗口大小（毫秒）
     */
    public static final long API_WINDOW_MS = 1000;

    /**
     * API 通用限流最大请求数
     */
    public static final long API_MAX_REQUESTS = 10;

    /**
     * 登录接口限流窗口大小（毫秒）
     */
    public static final long LOGIN_WINDOW_MS = 1000;

    /**
     * 登录接口限流最大请求数
     */
    public static final long LOGIN_MAX_REQUESTS = 2;

    /**
     * 设备级限流窗口大小（毫秒）
     */
    public static final long DEVICE_WINDOW_MS = 1000;

    /**
     * 设备级限流最大请求数
     */
    public static final long DEVICE_MAX_REQUESTS = 5;

    /**
     * 创建 API 通用限流器配置
     *
     * 使用方式：
     * redisRateLimiter.tryAcquire(RedisRateLimiter.apiKey("general"), API_MAX_REQUESTS, API_WINDOW_MS)
     *
     * @return 限流配置信息
     */
    @Bean
    public RateLimitProperties apiRateLimitProperties() {
        return new RateLimitProperties("api", API_MAX_REQUESTS, API_WINDOW_MS);
    }

    /**
     * 创建登录接口限流器配置
     *
     * 使用方式：
     * redisRateLimiter.tryAcquire(RedisRateLimiter.apiKey("login"), LOGIN_MAX_REQUESTS, LOGIN_WINDOW_MS)
     *
     * @return 限流配置信息
     */
    @Bean
    public RateLimitProperties loginRateLimitProperties() {
        return new RateLimitProperties("login", LOGIN_MAX_REQUESTS, LOGIN_WINDOW_MS);
    }

    /**
     * 创建设备级限流器配置
     *
     * 使用方式：
     * redisRateLimiter.tryAcquire(RedisRateLimiter.deviceKey(deviceId), DEVICE_MAX_REQUESTS, DEVICE_WINDOW_MS)
     *
     * @return 限流配置信息
     */
    @Bean
    public RateLimitProperties deviceRateLimitProperties() {
        return new RateLimitProperties("device", DEVICE_MAX_REQUESTS, DEVICE_WINDOW_MS);
    }

    /**
     * 限流配置属性类
     */
    public static class RateLimitProperties {
        private final String name;
        private final long maxRequests;
        private final long windowMs;

        public RateLimitProperties(String name, long maxRequests, long windowMs) {
            this.name = name;
            this.maxRequests = maxRequests;
            this.windowMs = windowMs;
        }

        public String getName() {
            return name;
        }

        public long getMaxRequests() {
            return maxRequests;
        }

        public long getWindowMs() {
            return windowMs;
        }
    }
}