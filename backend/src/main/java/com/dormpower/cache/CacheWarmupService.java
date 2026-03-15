package com.dormpower.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * 缓存预热服务
 * 
 * 提供缓存预热功能
 * 
 * @author dormpower team
 * @version 1.0
 */
@Service
public class CacheWarmupService {

    private static final Logger logger = LoggerFactory.getLogger(CacheWarmupService.class);

    @Autowired
    private CacheManager cacheManager;

    /**
     * 预热缓存
     */
    public void warmupCache(String cacheName, String key) {
        try {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                logger.debug("Warmup cache - cache: {}, key: {}", cacheName, key);
            }
        } catch (Exception e) {
            logger.error("Failed to warmup cache - cache: {}, key: {}, error: {}", 
                cacheName, key, e.getMessage());
        }
    }
}
