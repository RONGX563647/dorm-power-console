package com.dormpower.monitoring;

import com.dormpower.cache.AccessPatternAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓存监控端点
 * 
 * 提供缓存监控数据的REST API
 * 
 * @author dormpower team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/monitoring/cache")
@ConditionalOnProperty(name = "spring.data.redis.host")
public class CacheMonitoringController {

    @Autowired
    private CacheMetrics cacheMetrics;

    @Autowired(required = false)
    private AccessPatternAnalyzer accessPatternAnalyzer;

    @GetMapping("/stats")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("hitRate", cacheMetrics.getHitRate());
        stats.put("timestamp", System.currentTimeMillis());
        
        return stats;
    }

    @GetMapping("/hot-keys")
    public Map<String, List<String>> getHotKeys() {
        if (accessPatternAnalyzer != null) {
            return accessPatternAnalyzer.getAllHotKeys(10);
        }
        return new HashMap<>();
    }

    @GetMapping("/access-patterns")
    public Map<String, Object> getAccessPatterns() {
        if (accessPatternAnalyzer != null) {
            return accessPatternAnalyzer.getStatistics();
        }
        return new HashMap<>();
    }
}
