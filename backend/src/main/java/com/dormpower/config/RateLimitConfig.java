package com.dormpower.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流配置类（轻量级实现）
 */
@Configuration
public class RateLimitConfig {

    /**
     * API限流器（每秒10个请求）
     */
    @Bean
    public RateLimiter apiRateLimiter() {
        return RateLimiter.create(10.0);
    }

    /**
     * 登录限流器（每秒2个请求）
     */
    @Bean
    public RateLimiter loginRateLimiter() {
        return RateLimiter.create(2.0);
    }

    /**
     * 设备限流器映射（每个设备每秒5个请求）
     */
    @Bean
    public ConcurrentHashMap<String, RateLimiter> deviceRateLimiters() {
        return new ConcurrentHashMap<>();
    }

}
