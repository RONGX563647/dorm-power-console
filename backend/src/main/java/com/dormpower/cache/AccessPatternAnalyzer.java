package com.dormpower.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 访问模式分析器
 * 
 * 分析缓存访问模式，识别热点数据
 * 
 * 功能：
 * 1. 记录缓存访问频率
 * 2. 分析访问时间分布
 * 3. 识别热点数据
 * 4. 提供预热建议
 * 
 * @author dormpower team
 * @version 1.0
 */
@Service
public class AccessPatternAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(AccessPatternAnalyzer.class);

    private final Map<String, AccessStats> accessStats = new ConcurrentHashMap<>();

    /**
     * 记录缓存访问
     */
    public void recordAccess(String cacheName, String key) {
        String fullKey = cacheName + ":" + key;
        accessStats.compute(fullKey, (k, v) -> {
            if (v == null) {
                v = new AccessStats(cacheName, key);
            }
            v.incrementCount();
            v.recordAccessTime(System.currentTimeMillis());
            return v;
        });
    }

    /**
     * 获取热点Key列表
     */
    public List<String> getHotKeys(String cacheName, int topN) {
        return accessStats.entrySet().stream()
            .filter(e -> e.getKey().startsWith(cacheName + ":"))
            .sorted((e1, e2) -> Long.compare(e2.getValue().getCount(), e1.getValue().getCount()))
            .limit(topN)
            .map(e -> e.getValue().getKey())
            .collect(Collectors.toList());
    }

    /**
     * 获取所有缓存的热点Key
     */
    public Map<String, List<String>> getAllHotKeys(int topN) {
        Map<String, List<String>> result = new HashMap<>();
        
        accessStats.values().stream()
            .collect(Collectors.groupingBy(AccessStats::getCacheName))
            .forEach((cacheName, stats) -> {
                List<String> hotKeys = stats.stream()
                    .sorted((s1, s2) -> Long.compare(s2.getCount(), s1.getCount()))
                    .limit(topN)
                    .map(AccessStats::getKey)
                    .collect(Collectors.toList());
                result.put(cacheName, hotKeys);
            });
        
        return result;
    }

    /**
     * 获取访问统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalKeys", accessStats.size());
        stats.put("totalAccess", accessStats.values().stream()
            .mapToLong(AccessStats::getCount)
            .sum());
        stats.put("cacheNames", accessStats.values().stream()
            .map(AccessStats::getCacheName)
            .distinct()
            .collect(Collectors.toList()));
        
        return stats;
    }

    /**
     * 清除统计数据
     */
    public void clear() {
        accessStats.clear();
        logger.info("Access pattern statistics cleared");
    }

    /**
     * 访问统计类
     */
    private static class AccessStats {
        private final String cacheName;
        private final String key;
        private long count;
        private long lastAccessTime;
        private final List<Long> accessTimes = new ArrayList<>();

        public AccessStats(String cacheName, String key) {
            this.cacheName = cacheName;
            this.key = key;
            this.count = 0;
        }

        public void incrementCount() {
            this.count++;
        }

        public void recordAccessTime(long time) {
            this.lastAccessTime = time;
            if (accessTimes.size() < 100) {
                accessTimes.add(time);
            }
        }

        public String getCacheName() {
            return cacheName;
        }

        public String getKey() {
            return key;
        }

        public long getCount() {
            return count;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }
    }
}
