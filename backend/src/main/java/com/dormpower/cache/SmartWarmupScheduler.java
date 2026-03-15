package com.dormpower.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 智能预热调度器
 * 
 * 根据访问模式智能预热缓存
 * 
 * 功能：
 * 1. 定时分析访问模式
 * 2. 识别热点数据
 * 3. 智能预热缓存
 * 4. 监控预热效果
 * 
 * @author dormpower team
 * @version 1.0
 */
@Service
@ConditionalOnProperty(name = "cache.smart-warmup.enabled", havingValue = "true", matchIfMissing = false)
public class SmartWarmupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SmartWarmupScheduler.class);

    @Autowired
    private AccessPatternAnalyzer patternAnalyzer;

    @Autowired
    private CacheWarmupService cacheWarmupService;

    @Value("${cache.smart-warmup.hot-key-threshold:100}")
    private int hotKeyThreshold;

    @Scheduled(cron = "${cache.smart-warmup.cron:0 0 * * * ?}")
    public void smartWarmup() {
        long startTime = System.currentTimeMillis();
        logger.info("========== 开始智能预热 ==========");

        try {
            Map<String, List<String>> hotKeys = patternAnalyzer.getAllHotKeys(10);
            
            int warmupCount = 0;
            for (Map.Entry<String, List<String>> entry : hotKeys.entrySet()) {
                String cacheName = entry.getKey();
                List<String> keys = entry.getValue();
                
                for (String key : keys) {
                    try {
                        cacheWarmupService.warmupCache(cacheName, key);
                        warmupCount++;
                    } catch (Exception e) {
                        logger.error("Failed to warmup cache - cache: {}, key: {}, error: {}", 
                            cacheName, key, e.getMessage());
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("========== 智能预热完成: 预热{}个缓存, 耗时{}ms ==========", 
                warmupCount, duration);

        } catch (Exception e) {
            logger.error("Smart warmup failed: {}", e.getMessage(), e);
        }
    }
}
