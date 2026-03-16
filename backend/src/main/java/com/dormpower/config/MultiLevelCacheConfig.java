package com.dormpower.config;

import com.dormpower.cache.MultiLevelCacheManager;
import com.dormpower.cache.bloom.RedisBloomFilter;
import com.dormpower.cache.consistency.CacheInvalidationBroadcaster;
import com.dormpower.cache.hotkey.HotKeyDetectorService;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
 * 高级功能集成：
 * - 熔断保护：Redis故障时自动降级
 * - 热点检测：自动识别热点Key
 * - 缓存广播：多节点缓存一致性
 * - 布隆过滤器：防止缓存穿透
 *
 * TTL策略：
 * - L1缓存TTL较短（30秒），支持多节点最终一致性
 * - L2缓存TTL较长（5分钟），减少数据库压力
 *
 * @author dormpower team
 * @version 3.0
 */
@Configuration
@EnableCaching
@ConditionalOnClass(name = "org.springframework.data.redis.connection.RedisConnectionFactory")
@ConditionalOnProperty(name = "spring.data.redis.host", matchIfMissing = true)
public class MultiLevelCacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCacheConfig.class);

    @Autowired
    @Qualifier("redisCacheManager")
    private CacheManager redisCacheManager;

    // 可选的高级功能组件
    @Autowired(required = false)
    private CircuitBreaker circuitBreaker;

    @Autowired(required = false)
    private HotKeyDetectorService hotKeyDetectorService;

    @Autowired(required = false)
    private CacheInvalidationBroadcaster cacheInvalidationBroadcaster;

    @Autowired(required = false)
    private RedisBloomFilter redisBloomFilter;

    @Value("${cache.l1.ttl-seconds:30}")
    private int l1TtlSeconds;

    @Value("${cache.l1.max-size:10000}")
    private int l1MaxSize;

    /**
     * Caffeine本地缓存管理器
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
     * 创建时注入所有高级功能组件
     */
    @Bean
    @Primary
    public CacheManager multiLevelCacheManager() {
        MultiLevelCacheManager manager = new MultiLevelCacheManager(
            caffeineCacheManager(),
            redisCacheManager
        );

        // 注入熔断器
        if (circuitBreaker != null) {
            manager.setCircuitBreaker(circuitBreaker);
            logger.info("CircuitBreaker integrated into MultiLevelCacheManager");
        }

        // 注入热点检测服务
        if (hotKeyDetectorService != null) {
            manager.setHotKeyDetectorService(hotKeyDetectorService);
            logger.info("HotKeyDetector integrated into MultiLevelCacheManager");
        }

        // 注入缓存广播服务
        if (cacheInvalidationBroadcaster != null) {
            manager.setCacheInvalidationBroadcaster(cacheInvalidationBroadcaster);
            logger.info("CacheInvalidationBroadcaster integrated into MultiLevelCacheManager");
        }

        // 注入布隆过滤器
        if (redisBloomFilter != null) {
            manager.setRedisBloomFilter(redisBloomFilter);
            logger.info("RedisBloomFilter integrated into MultiLevelCacheManager");
        }

        return manager;
    }
}
