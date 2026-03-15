package com.dormpower.cache.sharding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 时间分片策略
 * 
 * 按时间对缓存进行分片，适用于时间序列数据
 * 
 * 特点：
 * 1. 按天分片
 * 2. 支持查询最近N天的数据
 * 3. 自动过期旧分片
 * 
 * @author dormpower team
 * @version 1.0
 */
public class TimeBasedShardStrategy implements ShardStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TimeBasedShardStrategy.class);

    private final int shardDays;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    public TimeBasedShardStrategy(int shardDays) {
        this.shardDays = shardDays;
    }

    @Override
    public String getShardKey(String cacheName, Object key) {
        try {
            long timestamp = extractTimestamp(key);
            String date = dateFormat.format(new Date(timestamp * 1000));
            return cacheName + ":" + date;
        } catch (Exception e) {
            logger.error("Failed to get shard key - cache: {}, key: {}, error: {}", 
                cacheName, key, e.getMessage());
            return cacheName + ":default";
        }
    }

    @Override
    public List<String> getAllShards(String cacheName) {
        List<String> shards = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < shardDays; i++) {
            shards.add(cacheName + ":" + dateFormat.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }

        return shards;
    }

    @Override
    public int getShardCount() {
        return shardDays;
    }

    /**
     * 从Key中提取时间戳
     */
    private long extractTimestamp(Object key) {
        if (key instanceof Long) {
            return (Long) key;
        } else if (key instanceof String) {
            String keyStr = (String) key;
            if (keyStr.contains("_")) {
                String[] parts = keyStr.split("_");
                if (parts.length > 1) {
                    try {
                        return Long.parseLong(parts[parts.length - 1]);
                    } catch (NumberFormatException e) {
                        logger.debug("Failed to parse timestamp from key: {}", keyStr);
                    }
                }
            }
        }
        
        return System.currentTimeMillis() / 1000;
    }
}
