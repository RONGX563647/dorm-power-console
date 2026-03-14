package com.dormpower.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 测试环境缓存配置
 *
 * 使用简单的内存缓存替代 Redis
 *
 * @author dormpower team
 * @version 1.0
 */
@Configuration
@EnableCaching
public class TestCacheConfig {

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "deviceStatus",
            "telemetry",
            "devices",
            "systemConfig",
            "dataDict",
            "ipWhitelist",
            "ipBlacklist",
            "priceRules",
            "messageTemplates",
            "buildings",
            "alertConfigs",
            "resourceTree"
        );
    }
}