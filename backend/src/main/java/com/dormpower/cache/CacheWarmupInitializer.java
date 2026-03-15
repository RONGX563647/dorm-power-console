package com.dormpower.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓存预热初始化器
 * 
 * 应用启动后异步预热缓存
 * 不阻塞应用就绪
 * 
 * 预热策略：
 * 1. Tier1（核心数据）：系统配置、数据字典 - 同步预热
 * 2. Tier2（重要数据）：用户权限、楼栋列表 - 异步预热
 * 3. Tier3（普通数据）：设备列表、电价规则 - 懒加载
 * 
 * @author dormpower team
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "cache.warmup.on-startup.enabled", havingValue = "true", matchIfMissing = true)
public class CacheWarmupInitializer {

    private static final Logger logger = LoggerFactory.getLogger(CacheWarmupInitializer.class);

    @Autowired
    private AsyncCacheWarmupService asyncCacheWarmupService;

    @Value("${cache.warmup.on-startup.tier1-enabled:true}")
    private boolean tier1Enabled;

    @Value("${cache.warmup.on-startup.tier2-enabled:true}")
    private boolean tier2Enabled;

    @Value("${cache.warmup.on-startup.tier3-enabled:false}")
    private boolean tier3Enabled;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void onApplicationReady() {
        logger.info("========== 开始异步缓存预热 ==========");
        long startTime = System.currentTimeMillis();

        try {
            AsyncCacheWarmupService.TieredWarmupConfig config = buildWarmupConfig();
            asyncCacheWarmupService.warmupTiered(config);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("========== 异步缓存预热完成，耗时{}ms ==========", duration);

        } catch (Exception e) {
            logger.error("Async cache warmup failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 构建分层预热配置
     */
    private AsyncCacheWarmupService.TieredWarmupConfig buildWarmupConfig() {
        AsyncCacheWarmupService.TieredWarmupConfig config = new AsyncCacheWarmupService.TieredWarmupConfig();

        if (tier1Enabled) {
            config.setTier1Critical(buildTier1Config());
        }

        if (tier2Enabled) {
            config.setTier2Important(buildTier2Config());
        }

        if (tier3Enabled) {
            config.setTier3Normal(buildTier3Config());
        }

        return config;
    }

    /**
     * Tier1: 核心数据 - 变化极少，高频访问
     */
    private Map<String, List<String>> buildTier1Config() {
        Map<String, List<String>> tier1 = new HashMap<>();
        
        tier1.put("systemConfig", Arrays.asList("all"));
        tier1.put("dataDict", Arrays.asList("all"));
        tier1.put("messageTemplates", Arrays.asList("all"));
        
        logger.debug("Tier1 config: {} cache regions", tier1.size());
        return tier1;
    }

    /**
     * Tier2: 重要数据 - 变化较少，频繁访问
     */
    private Map<String, List<String>> buildTier2Config() {
        Map<String, List<String>> tier2 = new HashMap<>();
        
        tier2.put("buildings", Arrays.asList("all"));
        tier2.put("priceRules", Arrays.asList("all"));
        tier2.put("alertConfigs", Arrays.asList("all"));
        tier2.put("resourceTree", Arrays.asList("all"));
        
        logger.debug("Tier2 config: {} cache regions", tier2.size());
        return tier2;
    }

    /**
     * Tier3: 普通数据 - 按需加载
     */
    private Map<String, List<String>> buildTier3Config() {
        Map<String, List<String>> tier3 = new HashMap<>();
        
        tier3.put("devices", Arrays.asList("all"));
        tier3.put("userPermissions", Arrays.asList("all"));
        tier3.put("userRoles", Arrays.asList("all"));
        
        logger.debug("Tier3 config: {} cache regions", tier3.size());
        return tier3;
    }
}
