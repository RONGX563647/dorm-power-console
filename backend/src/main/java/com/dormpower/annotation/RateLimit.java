package com.dormpower.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解 (基于 Redis+Lua 分布式限流)
 * 
 * 使用方式:
 * @RateLimit(value = 10, type = "api", windowSize = 1)
 * - value: 窗口内最大请求数
 * - type: 限流类型 (用于构建 Redis key)
 * - windowSize: 窗口大小 (秒)
 * 
 * 示例:
 * - @RateLimit(value = 5) - 每秒最多 5 个请求
 * - @RateLimit(value = 100, windowSize = 60) - 每分钟最多 100 个请求
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 窗口内最大请求数
     * 默认值：10 个请求/秒
     */
    int value() default 10;

    /**
     * 限流类型：api, login, device, upload 等
     * 用于构建 Redis key: rate_limit:{type}:{ip}
     */
    String type() default "api";

    /**
     * 时间窗口大小 (秒)
     * 默认值：1 秒
     */
    int windowSize() default 1;

}
