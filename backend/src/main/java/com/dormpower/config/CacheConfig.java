package com.dormpower.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 缓存配置类（本地缓存兜底）
 *
 * 当 Redis 未配置时，使用 ConcurrentMapCacheManager 作为本地缓存。
 * 当 Redis 配置存在时，由 MultiLevelCacheConfig 提供多级缓存管理器。
 *
 * 条件说明：
 * - @ConditionalOnMissingBean: 当 RedisConnectionFactory 不存在时生效（即 Redis 未配置）
 * - 移除 @ConditionalOnProperty，仅依赖 Bean 存在性判断，逻辑更清晰
 *
 * @author dormpower team
 * @version 2.0
 */
@Configuration
@EnableCaching
@ConditionalOnMissingBean(RedisConnectionFactory.class)
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