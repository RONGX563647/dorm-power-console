package com.dormpower.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * 缓存配置类（本地缓存）
 *
 * 当 Redis 未配置时，使用 ConcurrentMapCacheManager 作为本地缓存。
 * 当 Redis 配置存在时，由 RedisCacheConfig 提供 RedisCacheManager。
 *
 * @author dormpower team
 * @version 1.0
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.data.redis.host", matchIfMissing = true)
public class CacheConfig {

    /**
     * 配置本地缓存管理器
     *
     * 使用 ConcurrentMap 作为存储后端，适用于单机环境。
     *
     * @return 缓存管理器 Bean
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "devices",      // 设备信息缓存
            "deviceStatus", // 设备状态缓存
            "telemetry"     // 遥测数据缓存
        );
    }
}