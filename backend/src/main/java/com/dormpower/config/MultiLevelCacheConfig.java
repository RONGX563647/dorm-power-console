package com.dormpower.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存配置
 * 
 * 架构设计:
 * L1: Caffeine 本地缓存 (JVM 内存)
 *   - 访问延迟：<1μs
 *   - 容量：1000 个条目
 *   - 过期时间：1 分钟
 *   - 适用场景：热点数据 (设备状态、用户信息)
 * 
 * L2: Redis 分布式缓存 (远程 Redis)
 *   - 访问延迟：~1ms (网络)
 *   - 容量：无限制
 *   - 过期时间：5-30 分钟
 *   - 适用场景：全量数据、共享数据
 * 
 * 缓存策略:
 * 1. 先查 L1，命中直接返回
 * 2. L1 未命中查 L2，写入 L1
 * 3. 数据更新时同时清除 L1 和 L2
 * 
 * @author dormpower team
 * @version 1.0
 */
@Configuration
public class MultiLevelCacheConfig {

    /**
     * L1 缓存管理器：Caffeine
     * 
     * 配置说明:
     * - maximumSize: 最大缓存 1000 个条目
     * - expireAfterWrite: 写入后 1 分钟过期
     * - recordStats: 记录命中率统计
     */
    @Bean("caffeineCacheManager")
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .recordStats());
        
        return cacheManager;
    }

    /**
     * L2 缓存管理器：Redis
     * 
     * 配置说明:
     * - defaultTtl: 默认 5 分钟过期
     * - keySerializer: String 序列化
     * - valueSerializer: JSON 序列化
     * - usePrefix: 使用缓存前缀
     */
    @Bean("redisCacheManager")
    public CacheManager redisCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))  // 默认 5 分钟
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .prefixCacheNameWith("cache:")  // 缓存 key 前缀
            .disableCachingNullValues();  // 不缓存 null 值
        
        // 为不同缓存配置不同的 TTL
        RedisCacheManager cacheManager = RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .withCacheConfiguration("devices", 
                config.entryTtl(Duration.ofMinutes(1)))  // 设备缓存 1 分钟
            .withCacheConfiguration("users", 
                config.entryTtl(Duration.ofMinutes(30)))  // 用户缓存 30 分钟
            .withCacheConfiguration("telemetry", 
                config.entryTtl(Duration.ofMinutes(10)))  // 遥测缓存 10 分钟
            .withCacheConfiguration("aiReport", 
                config.entryTtl(Duration.ofMinutes(15)))  // AI 报告缓存 15 分钟
            .transactionAware()
            .build();
        
        return cacheManager;
    }
}
