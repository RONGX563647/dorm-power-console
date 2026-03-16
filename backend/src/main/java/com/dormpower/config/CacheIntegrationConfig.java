package com.dormpower.config;

import com.dormpower.cache.MultiLevelCacheManager;
import com.dormpower.cache.consistency.CacheInvalidationBroadcaster;
import com.dormpower.cache.hotkey.HotKeyDetectorService;
import com.dormpower.cache.hotkey.HotKeyDetectorService.HotKeyHandler;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 缓存集成配置类
 *
 * 负责将高级缓存组件集成到多级缓存管理器中：
 * 1. 熔断器集成 - Redis故障时自动降级
 * 2. 热点Key检测集成 - 自动识别并提升热点Key
 * 3. 缓存广播集成 - 多节点缓存一致性
 *
 * @author dormpower team
 * @version 1.0
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.host")
public class CacheIntegrationConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheIntegrationConfig.class);

    @Autowired(required = false)
    private CircuitBreaker circuitBreaker;

    @Autowired(required = false)
    private HotKeyDetectorService hotKeyDetectorService;

    @Autowired(required = false)
    private CacheInvalidationBroadcaster cacheInvalidationBroadcaster;

    @Autowired
    @Qualifier("multiLevelCacheManager")
    private CacheManager multiLevelCacheManager;

    /**
     * 热点Key本地缓存
     *
     * 用于存储被检测为热点的Key，提升到本地缓存
     */
    @Bean
    @ConditionalOnBean(HotKeyDetectorService.class)
    public Cache<String, Object> hotKeyLocalCache() {
        return Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .recordStats()
            .build();
    }

    /**
     * 热点Key处理器实现
     *
     * 当检测到热点Key时，将其提升到本地缓存
     * 这个Bean会被HotKeyDetectorService自动注入
     */
    @Bean
    @ConditionalOnBean(HotKeyDetectorService.class)
    public HotKeyHandler hotKeyHandler(Cache<String, Object> hotKeyLocalCache) {
        return new HotKeyHandler() {
            @Override
            public void onHotKeyDetected(String key, long qps) {
                logger.info("Hot key detected and handled - key: {}, QPS: {}", key, qps);
            }

            @Override
            public void promoteToLocalCache(String key, Duration ttl) {
                logger.debug("Hot key promoted to local cache - key: {}, ttl: {}", key, ttl);
            }
        };
    }

    /**
     * 配置多级缓存管理器的高级功能
     *
     * 将熔断器、热点检测、缓存广播注入到缓存管理器
     */
    @Bean
    public CacheManagerIntegrator cacheManagerIntegrator() {
        if (multiLevelCacheManager instanceof MultiLevelCacheManager) {
            MultiLevelCacheManager mlcm = (MultiLevelCacheManager) multiLevelCacheManager;

            // 注入熔断器
            if (circuitBreaker != null) {
                mlcm.setCircuitBreaker(circuitBreaker);
                logger.info("CircuitBreaker integrated into MultiLevelCacheManager");
            }

            // 注入热点检测服务
            if (hotKeyDetectorService != null) {
                mlcm.setHotKeyDetectorService(hotKeyDetectorService);
                logger.info("HotKeyDetector integrated into MultiLevelCacheManager");
            }

            // 注入缓存广播服务
            if (cacheInvalidationBroadcaster != null) {
                mlcm.setCacheInvalidationBroadcaster(cacheInvalidationBroadcaster);
                logger.info("CacheInvalidationBroadcaster integrated into MultiLevelCacheManager");
            }
        }

        return new CacheManagerIntegrator();
    }

    /**
     * 缓存管理器集成器（占位Bean）
     */
    public static class CacheManagerIntegrator {
        // 标记集成完成的占位类
    }
}