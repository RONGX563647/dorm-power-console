package com.dormpower.config;

import com.dormpower.cache.MultiLevelCacheManager;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * 多级缓存配置类
 *
 * 配置本地缓存(Caffeine) + 分布式缓存(Redis)的多级缓存架构
 *
 * 特点：
 * 1. L1缓存：Caffeine本地缓存 - 高性能、自动过期、容量限制
 * 2. L2缓存：Redis分布式缓存 - 数据共享、持久化
 * 3. 多级缓存管理器：协调两级缓存
 *
 * TTL策略：
 * - L1缓存TTL较短（30秒），支持多节点最终一致性
 * - L2缓存TTL较长（5分钟），减少数据库压力
 *
 * 注意：Redis 缓存管理器由 RedisCacheConfig 提供
 *
 * @author dormpower team
 * @version 2.1
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.data.redis.host")
public class MultiLevelCacheConfig {

    @Autowired
    @Qualifier("redisCacheManager")
    private CacheManager redisCacheManager;

    @Value("${cache.l1.ttl-seconds:30}")
    private int l1TtlSeconds;

    @Value("${cache.l1.max-size:10000}")
    private int l1MaxSize;

    /**
     * Caffeine本地缓存管理器
     *
     * 配置：
     * - 最大容量：10000条（可配置）
     * - 写入后过期：30秒（可配置，短TTL支持多节点一致性）
     * - 访问后过期：60秒
     * - 统计功能：开启
     */
    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
            .maximumSize(l1MaxSize)
            .expireAfterWrite(l1TtlSeconds, TimeUnit.SECONDS)
            .expireAfterAccess(l1TtlSeconds * 2, TimeUnit.SECONDS)
            .recordStats();

        cacheManager.setCaffeine(caffeine);

        return cacheManager;
    }

    /**
     * 多级缓存管理器（主缓存管理器）
     *
     * 整合本地缓存和分布式缓存
     * Redis 缓存管理器由 RedisCacheConfig 提供
     */
    @Bean
    @Primary
    public CacheManager multiLevelCacheManager() {
        return new MultiLevelCacheManager(
            caffeineCacheManager(),
            redisCacheManager
        );
    }
}
