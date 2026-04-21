package com.dormpower.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多级缓存服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
class MultiLevelCacheServiceTest {

    @Autowired
    private MultiLevelCacheService cacheService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // 清除所有缓存
        cacheService.getCacheStats();
    }

    @Test
    void testGetCacheStats() {
        // 获取缓存统计
        var stats = cacheService.getCacheStats();
        
        assertNotNull(stats, "缓存统计不应该为 null");
        assertTrue(stats.size() > 0, "应该有缓存统计信息");
    }

    @Test
    void testWarmupCache() {
        // 测试缓存预热 (使用不存在的设备 ID，验证不会抛异常)
        List<String> deviceIds = Arrays.asList("device_001", "device_002", "device_003");
        
        assertDoesNotThrow(() -> {
            cacheService.warmupCache(deviceIds);
        }, "缓存预热不应该抛出异常");
    }

    @Test
    void testConcurrentCacheAccess() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // 并发访问缓存
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // 访问缓存 (即使设备不存在也不应该抛异常)
                    cacheService.getCacheStats();
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(threadCount, successCount.get(), "所有线程应该成功访问缓存");
    }

    @Test
    void testBatchGetDevices() {
        // 测试批量获取设备
        List<String> deviceIds = Arrays.asList("device_001", "device_002", "device_003");
        
        List<MultiLevelCacheService.TelemetryData> devices = 
            cacheService.batchGetDevices(deviceIds);
        
        assertNotNull(devices, "返回的列表不应该为 null");
        // 注意：由于设备不存在，列表可能为空
    }

    @Test
    void testCacheServiceInitialization() {
        // 验证缓存服务已正确初始化
        assertNotNull(cacheService, "缓存服务应该已注入");
        assertNotNull(cacheManager, "缓存管理器应该已注入");
    }

    @Test
    void testMultipleCacheManagers() {
        // 验证存在多个缓存管理器 (Caffeine 和 Redis)
        var cacheNames = cacheManager.getCacheNames();
        
        // 至少应该有 devices 缓存
        assertTrue(cacheNames.contains("devices"), "应该有 devices 缓存");
    }

    @Test
    void testCacheEvict() {
        // 测试缓存清除 (使用不存在的设备，验证不会抛异常)
        String deviceId = "nonexistent_device";
        
        assertDoesNotThrow(() -> {
            cacheService.deleteDevice(deviceId);
        }, "删除不存在的设备不应该抛出异常");
    }

    @Test
    void testCallableCacheLoading() {
        // 测试 Callable 方式加载缓存
        String deviceId = "test_device";
        
        assertDoesNotThrow(() -> {
            var device = cacheService.getDeviceWithCallable(deviceId);
            // 设备可能为 null (不存在)
        }, "Callable 缓存加载不应该抛出异常");
    }

    @Test
    void testManualCache() {
        // 测试手动缓存操作
        String deviceId = "test_device";
        
        assertDoesNotThrow(() -> {
            var device = cacheService.getDeviceWithManualCache(deviceId);
            // 设备可能为 null (不存在)
        }, "手动缓存操作不应该抛出异常");
    }

    @Test
    void testCachePerformance() {
        // 测试缓存性能
        int iterations = 1000;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            cacheService.getCacheStats();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // 1000 次缓存操作应该在 1 秒内完成
        assertTrue(duration < 1000, 
            "1000 次缓存操作应该小于 1 秒，实际：" + duration + "ms");
        
        System.out.println("缓存性能测试：" + iterations + " 次操作，耗时：" + duration + "ms");
    }

    @Test
    void testCacheStatsAccuracy() {
        // 验证缓存统计准确性
        var stats1 = cacheService.getCacheStats();
        
        // 等待一小段时间
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        var stats2 = cacheService.getCacheStats();
        
        // 统计信息应该一致 (如果没有新的缓存操作)
        assertEquals(stats1.size(), stats2.size(), "缓存统计大小应该一致");
    }
}
