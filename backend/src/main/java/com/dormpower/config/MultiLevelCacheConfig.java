package com.dormpower.config;

import com.dormpower.cache.MultiLevelCacheManager;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
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
 * @author dormpower team
 * @version 1.0
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.data.redis.host")
public class MultiLevelCacheConfig {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * Caffeine本地缓存管理器
     * 
     * 配置：
     * - 最大容量：10000条
     * - 写入后过期：5分钟
     * - 访问后过期：10分钟
     * - 统计功能：开启
     */
    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .recordStats();
        
        cacheManager.setCaffeine(caffeine);
        
        return cacheManager;
    }

    /**
     * Redis分布式缓存管理器
     * 
     * 使用已有的RedisCacheConfig配置
     */
    @Bean
    public CacheManager redisCacheManager() {
        RedisCacheManager.RedisCacheManagerBuilder builder = 
            RedisCacheManager.builder(redisConnectionFactory);
        
        return builder.build();
    }

    /**
     * 多级缓存管理器（主缓存管理器）
     * 
     * 整合本地缓存和分布式缓存
     */
    @Bean
    @Primary
    public CacheManager multiLevelCacheManager() {
        return new MultiLevelCacheManager(
            caffeineCacheManager(),
            redisCacheManager()
        );
    }
}
