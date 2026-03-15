package com.dormpower.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 异步缓存预热服务
 * 
 * 提供异步、并行、分层的缓存预热功能
 * 
 * 特点：
 * 1. 异步预热 - 不阻塞应用启动
 * 2. 并行执行 - 多线程并行预热
 * 3. 分层策略 - 核心数据优先预热
 * 4. 超时控制 - 防止预热卡住
 * 
 * @author dormpower team
 * @version 2.0
 */
@Service
@ConditionalOnProperty(name = "spring.data.redis.host")
public class AsyncCacheWarmupService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCacheWarmupService.class);

    @Autowired
    private CacheManager cacheManager;

    @Value("${cache.warmup.thread-pool-size:4}")
    private int threadPoolSize;

    @Value("${cache.warmup.timeout-seconds:60}")
    private int timeoutSeconds;

    private ExecutorService warmupExecutor;

    public AsyncCacheWarmupService() {
    }

    private ExecutorService getWarmupExecutor() {
        if (warmupExecutor == null || warmupExecutor.isShutdown()) {
            warmupExecutor = Executors.newFixedThreadPool(threadPoolSize);
        }
        return warmupExecutor;
    }

    /**
     * 异步预热缓存
     */
    @Async
    public CompletableFuture<Integer> warmupAsync(String cacheName, String key) {
        try {
            warmupCache(cacheName, key);
            return CompletableFuture.completedFuture(1);
        } catch (Exception e) {
            logger.error("Async warmup failed - cache: {}, key: {}, error: {}", 
                cacheName, key, e.getMessage());
            return CompletableFuture.completedFuture(0);
        }
    }

    /**
     * 并行预热多个缓存
     */
    public int warmupParallel(Map<String, List<String>> cacheKeys) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting parallel warmup for {} cache regions", cacheKeys.size());

        ExecutorService executor = getWarmupExecutor();
        
        List<CompletableFuture<Integer>> futures = cacheKeys.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                .map(key -> CompletableFuture.supplyAsync(() -> {
                    warmupCache(entry.getKey(), key);
                    return 1;
                }, executor)))
            .collect(Collectors.toList());

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));
        
        try {
            allFutures.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Parallel warmup timeout or error: {}", e.getMessage());
        }

        int totalCount = futures.stream()
            .map(f -> {
                try {
                    return f.getNow(0);
                } catch (Exception e) {
                    return 0;
                }
            })
            .mapToInt(Integer::intValue)
            .sum();

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Parallel warmup completed: {} items, {}ms", totalCount, duration);
        
        return totalCount;
    }

    /**
     * 分层预热 - 核心数据优先
     */
    public void warmupTiered(TieredWarmupConfig config) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting tiered warmup...");

        warmupTier(config.getTier1Critical(), "Tier1-Critical");
        warmupTier(config.getTier2Important(), "Tier2-Important");
        warmupTier(config.getTier3Normal(), "Tier3-Normal");

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Tiered warmup completed in {}ms", duration);
    }

    /**
     * 预热单个层级
     */
    private void warmupTier(Map<String, List<String>> tierData, String tierName) {
        if (tierData == null || tierData.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        logger.info("Warming up {} - {} cache regions", tierName, tierData.size());

        int count = warmupParallel(tierData);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("{} warmup completed: {} items, {}ms", tierName, count, duration);
    }

    /**
     * 预热单个缓存
     */
    private void warmupCache(String cacheName, String key) {
        try {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.get(key);
                logger.debug("Warmed up cache - cache: {}, key: {}", cacheName, key);
            }
        } catch (Exception e) {
            logger.error("Failed to warmup cache - cache: {}, key: {}, error: {}", 
                cacheName, key, e.getMessage());
        }
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        if (warmupExecutor != null && !warmupExecutor.isShutdown()) {
            warmupExecutor.shutdown();
            try {
                if (!warmupExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    warmupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                warmupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 分层预热配置
     */
    public static class TieredWarmupConfig {
        private Map<String, List<String>> tier1Critical;
        private Map<String, List<String>> tier2Important;
        private Map<String, List<String>> tier3Normal;

        public Map<String, List<String>> getTier1Critical() {
            return tier1Critical;
        }

        public void setTier1Critical(Map<String, List<String>> tier1Critical) {
            this.tier1Critical = tier1Critical;
        }

        public Map<String, List<String>> getTier2Important() {
            return tier2Important;
        }

        public void setTier2Important(Map<String, List<String>> tier2Important) {
            this.tier2Important = tier2Important;
        }

        public Map<String, List<String>> getTier3Normal() {
            return tier3Normal;
        }

        public void setTier3Normal(Map<String, List<String>> tier3Normal) {
            this.tier3Normal = tier3Normal;
        }
    }
}
