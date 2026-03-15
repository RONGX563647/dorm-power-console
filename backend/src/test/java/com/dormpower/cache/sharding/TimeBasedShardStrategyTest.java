package com.dormpower.cache.sharding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TimeBasedShardStrategy单元测试
 */
@DisplayName("时间分片策略测试")
class TimeBasedShardStrategyTest {

    private TimeBasedShardStrategy strategy;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    @BeforeEach
    void setUp() {
        strategy = new TimeBasedShardStrategy(7);
    }

    @Test
    @DisplayName("测试获取分片Key - 使用时间戳")
    void testGetShardKeyWithTimestamp() {
        String cacheName = "telemetry";
        long timestamp = System.currentTimeMillis() / 1000;
        String expectedDate = dateFormat.format(new Date(timestamp * 1000));

        String shardKey = strategy.getShardKey(cacheName, timestamp);

        assertEquals(cacheName + ":" + expectedDate, shardKey);
    }

    @Test
    @DisplayName("测试获取分片Key - 使用字符串Key")
    void testGetShardKeyWithStringKey() {
        String cacheName = "telemetry";
        long timestamp = System.currentTimeMillis() / 1000;
        String key = "device_123_" + timestamp;
        String expectedDate = dateFormat.format(new Date(timestamp * 1000));

        String shardKey = strategy.getShardKey(cacheName, key);

        assertEquals(cacheName + ":" + expectedDate, shardKey);
    }

    @Test
    @DisplayName("测试获取所有分片")
    void testGetAllShards() {
        String cacheName = "telemetry";
        int shardDays = 7;

        List<String> shards = strategy.getAllShards(cacheName);

        assertEquals(shardDays, shards.size());
        
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < shardDays; i++) {
            String expectedDate = dateFormat.format(cal.getTime());
            assertTrue(shards.contains(cacheName + ":" + expectedDate));
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
    }

    @Test
    @DisplayName("测试获取分片数量")
    void testGetShardCount() {
        assertEquals(7, strategy.getShardCount());
    }

    @Test
    @DisplayName("测试不同分片天数")
    void testDifferentShardDays() {
        TimeBasedShardStrategy strategy5Days = new TimeBasedShardStrategy(5);
        assertEquals(5, strategy5Days.getShardCount());
        
        TimeBasedShardStrategy strategy14Days = new TimeBasedShardStrategy(14);
        assertEquals(14, strategy14Days.getShardCount());
    }

    @Test
    @DisplayName("测试分片Key格式")
    void testShardKeyFormat() {
        String cacheName = "testCache";
        long timestamp = System.currentTimeMillis() / 1000;
        
        String shardKey = strategy.getShardKey(cacheName, timestamp);
        
        assertTrue(shardKey.startsWith(cacheName + ":"));
        assertTrue(shardKey.length() > cacheName.length() + 1);
        
        String datePart = shardKey.substring(cacheName.length() + 1);
        assertEquals(8, datePart.length());
        assertTrue(datePart.matches("\\d{8}"));
    }
}
