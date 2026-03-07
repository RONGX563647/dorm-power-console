package com.dormpower.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流配置类
 * 
 * 该类负责配置和管理系统中的限流器，使用Guava的RateLimiter实现令牌桶算法的限流功能。
 * 主要用于防止系统过载，保护后端服务免受恶意攻击或突发流量影响。
 * 
 * 限流策略：
 * 1. API通用限流：限制每秒10个请求，适用于大多数API接口
 * 2. 登录接口限流：限制每秒2个请求，防止暴力破解
 * 3. 设备级限流：为每个设备单独设置限流器，限制每秒5个请求
 * 
 * @author DormPower Team
 * @version 1.0
 * @since 2023-01-01
 */
@Configuration
public class RateLimitConfig {

    /**
     * 创建API通用限流器
     * 
     * 该限流器用于限制API接口的访问频率，采用令牌桶算法实现。
     * 每秒生成10个令牌，当请求到来时，如果有足够的令牌则放行，否则阻塞或拒绝请求。
     * 
     * 限流配置：
     * - 速率：每秒10个请求（10.0 permits per second）
     * - 应用范围：适用于大多数API接口
     * - 实现方式：Guava RateLimiter（平滑突发限流）
     * 
     * @return 配置好的RateLimiter实例，每秒允许10个请求通过
     */
    @Bean
    public RateLimiter apiRateLimiter() {
        return RateLimiter.create(10.0);
    }

    /**
     * 创建登录接口限流器
     * 
     * 该限流器专门用于限制登录接口的访问频率，防止暴力破解攻击。
     * 登录接口需要更严格的限流策略，因此设置为每秒2个请求。
     * 
     * 限流配置：
     * - 速率：每秒2个请求（2.0 permits per second）
     * - 应用范围：仅用于登录接口
     * - 安全目的：防止暴力破解和恶意登录尝试
     * - 实现方式：Guava RateLimiter（平滑突发限流）
     * 
     * @return 配置好的RateLimiter实例，每秒允许2个登录请求通过
     */
    @Bean
    public RateLimiter loginRateLimiter() {
        return RateLimiter.create(2.0);
    }

    /**
     * 创建设备级限流器映射
     * 
     * 该方法创建一个ConcurrentHashMap，用于存储和管理各个设备的限流器。
     * 每个设备ID作为键，对应的RateLimiter实例作为值。
     * 当有新设备接入时，系统会为该设备创建独立的限流器。
     * 
     * 限流配置：
     * - 速率：每个设备每秒5个请求（5.0 permits per second）
     * - 应用范围：所有设备接口
     * - 管理方式：动态创建和管理，每个设备独立限流
     * - 实现方式：Guava RateLimiter + ConcurrentHashMap（线程安全）
     * 
     * 使用示例：
     * <pre>
     * // 获取或创建设备限流器
     * RateLimiter deviceLimiter = deviceRateLimiters().computeIfAbsent(deviceId, 
     *     id -> RateLimiter.create(5.0));
     * 
     * // 尝试获取令牌
     * if (deviceLimiter.tryAcquire()) {
     *     // 处理设备请求
     * } else {
     *     // 限流，拒绝请求
     * }
     * </pre>
     * 
     * @return ConcurrentHashMap实例，用于存储设备ID到RateLimiter的映射关系
     */
    @Bean
    public ConcurrentHashMap<String, RateLimiter> deviceRateLimiters() {
        return new ConcurrentHashMap<>();
    }

}
