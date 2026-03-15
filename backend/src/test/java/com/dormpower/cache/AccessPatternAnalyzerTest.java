package com.dormpower.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AccessPatternAnalyzer单元测试
 */
@DisplayName("访问模式分析器测试")
class AccessPatternAnalyzerTest {

    private AccessPatternAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new AccessPatternAnalyzer();
    }

    @Test
    @DisplayName("测试记录访问")
    void testRecordAccess() {
        analyzer.recordAccess("testCache", "key1");
        analyzer.recordAccess("testCache", "key1");
        analyzer.recordAccess("testCache", "key2");

        Map<String, Object> stats = analyzer.getStatistics();
        assertEquals(2, stats.get("totalKeys"));
        assertEquals(3L, stats.get("totalAccess"));
    }

    @Test
    @DisplayName("测试获取热点Key")
    void testGetHotKeys() {
        analyzer.recordAccess("testCache", "key1");
        analyzer.recordAccess("testCache", "key1");
        analyzer.recordAccess("testCache", "key1");
        analyzer.recordAccess("testCache", "key2");
        analyzer.recordAccess("testCache", "key2");
        analyzer.recordAccess("testCache", "key3");

        List<String> hotKeys = analyzer.getHotKeys("testCache", 2);

        assertEquals(2, hotKeys.size());
        assertEquals("key1", hotKeys.get(0));
        assertEquals("key2", hotKeys.get(1));
    }

    @Test
    @DisplayName("测试获取所有热点Key")
    void testGetAllHotKeys() {
        analyzer.recordAccess("cache1", "key1");
        analyzer.recordAccess("cache1", "key1");
        analyzer.recordAccess("cache2", "key2");
        analyzer.recordAccess("cache2", "key2");
        analyzer.recordAccess("cache2", "key2");

        Map<String, List<String>> allHotKeys = analyzer.getAllHotKeys(1);

        assertEquals(2, allHotKeys.size());
        assertTrue(allHotKeys.containsKey("cache1"));
        assertTrue(allHotKeys.containsKey("cache2"));
        assertEquals("key1", allHotKeys.get("cache1").get(0));
        assertEquals("key2", allHotKeys.get("cache2").get(0));
    }

    @Test
    @DisplayName("测试清除统计数据")
    void testClear() {
        analyzer.recordAccess("testCache", "key1");
        analyzer.recordAccess("testCache", "key2");

        Map<String, Object> statsBefore = analyzer.getStatistics();
        assertEquals(2, statsBefore.get("totalKeys"));

        analyzer.clear();

        Map<String, Object> statsAfter = analyzer.getStatistics();
        assertEquals(0, statsAfter.get("totalKeys"));
        assertEquals(0L, statsAfter.get("totalAccess"));
    }

    @Test
    @DisplayName("测试获取统计信息")
    void testGetStatistics() {
        analyzer.recordAccess("cache1", "key1");
        analyzer.recordAccess("cache1", "key2");
        analyzer.recordAccess("cache2", "key3");

        Map<String, Object> stats = analyzer.getStatistics();

        assertEquals(3, stats.get("totalKeys"));
        assertEquals(3L, stats.get("totalAccess"));
        assertTrue(((List<?>) stats.get("cacheNames")).contains("cache1"));
        assertTrue(((List<?>) stats.get("cacheNames")).contains("cache2"));
    }
}
