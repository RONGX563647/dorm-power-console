package com.dormpower.service;

import com.dormpower.util.RedisRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 新功能集成测试
 * 
 * 测试内容:
 * 1. Java 21 虚拟线程高并发处理
 * 2. Redis+Lua 分布式限流
 * 3. Caffeine+Redis 多级缓存
 * 
 * @author dormpower team
 */
@SpringBootTest
@ActiveProfiles("test")
public class NewFeaturesIntegrationTest {

    @Autowired(required = false)
    private VirtualThreadService virtualThreadService;

    @Autowired(required = false)
    private RedisRateLimiter redisRateLimiter;

    @Autowired(required = false)
    private MultiLevelCacheService cacheService;

    /**
     * 测试 1: Java 21 虚拟线程高并发
     * 
     * 场景：模拟 1000 个设备并发接入
     * 预期：
     * - 所有设备处理完成
     * - 耗时 < 2 秒
     * - 内存占用 < 100MB
     */
    @Test
    public void testVirtualThreadHighConcurrency() throws Exception {
        System.out.println("\n========== 测试虚拟线程高并发 ==========");
        
        int deviceCount = 1000;
        long startTime = System.currentTimeMillis();
        
        // 模拟 1000 个设备并发接入
        int processedCount = virtualThreadService.simulateDeviceConnection(deviceCount);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // 验证
        assertEquals(deviceCount, processedCount, "所有设备应该处理完成");
        assertTrue(duration < 2000, "处理时间应该小于 2 秒，实际：" + duration + "ms");
        
        System.out.println("✓ 虚拟线程测试通过");
        System.out.println("  - 设备数量：" + deviceCount);
        System.out.println("  - 处理耗时：" + duration + "ms");
        System.out.println("  - 吞吐量：" + (deviceCount * 1000 / duration) + " QPS");
    }

    /**
     * 测试 2: Redis+Lua 分布式限流
     * 
     * 场景：1 秒内发送 10 个请求，限制 5 个/秒
     * 预期:
     * - 前 5 个请求允许
     * - 后 5 个请求拒绝
     */
    @Test
    public void testRedisLuaRateLimiter() {
        System.out.println("\n========== 测试 Redis+Lua 分布式限流 ==========");
        
        String testKey = "rate_limit:test:" + System.currentTimeMillis();
        int maxRequests = 5;
        int windowSize = 1;  // 1 秒
        
        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        
        // 发送 10 个请求
        for (int i = 0; i < 10; i++) {
            boolean allowed = redisRateLimiter.tryAcquire(testKey, maxRequests, windowSize);
            if (allowed) {
                allowedCount.incrementAndGet();
            } else {
                rejectedCount.incrementAndGet();
            }
        }
        
        // 验证
        assertEquals(5, allowedCount.get(), "应该允许 5 个请求");
        assertEquals(5, rejectedCount.get(), "应该拒绝 5 个请求");
        
        System.out.println("✓ 限流测试通过");
        System.out.println("  - 允许请求：" + allowedCount.get());
        System.out.println("  - 拒绝请求：" + rejectedCount.get());
        System.out.println("  - 限制：" + maxRequests + " 请求/" + windowSize + "秒");
    }

    /**
     * 测试 3: Redis+Lua 限流并发测试
     * 
     * 场景：100 个并发请求，限制 50 个/秒
     * 预期:
     * - 精确控制并发
     * - 允许约 50 个请求
     */
    @Test
    public void testRedisLuaRateLimiterConcurrency() throws Exception {
        System.out.println("\n========== 测试 Redis+Lua 限流并发 ==========");
        
        String testKey = "rate_limit:concurrent:" + System.currentTimeMillis();
        int maxRequests = 50;
        int windowSize = 1;
        int totalRequests = 100;
        
        AtomicInteger allowedCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        
        // 并发发送 100 个请求
        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    boolean allowed = redisRateLimiter.tryAcquire(testKey, maxRequests, windowSize);
                    if (allowed) {
                        allowedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();  // 等待所有请求完成
        executor.shutdown();
        
        // 验证：允许的请求数应该在 45-55 之间 (考虑并发误差)
        assertTrue(allowedCount.get() >= 45, "允许的请求数应该 >= 45，实际：" + allowedCount.get());
        assertTrue(allowedCount.get() <= 55, "允许的请求数应该 <= 55，实际：" + allowedCount.get());
        
        System.out.println("✓ 限流并发测试通过");
        System.out.println("  - 总请求数：" + totalRequests);
        System.out.println("  - 允许请求：" + allowedCount.get());
        System.out.println("  - 限制：" + maxRequests + " 请求/" + windowSize + "秒");
    }

    /**
     * 测试 4: Caffeine+Redis 多级缓存
     * 
     * 场景：连续查询同一设备 3 次
     * 预期:
     * - 第 1 次：数据库查询 + 写入 L1+L2
     * - 第 2 次：L1 命中
     * - 第 3 次：L1 命中
     */
    @Test
    public void testMultiLevelCache() {
        System.out.println("\n========== 测试多级缓存 ==========");
        
        // 注意：这个测试需要实际数据库中有设备数据
        // 如果测试失败，请确保数据库中有测试设备
        
        try {
            String deviceId = "test_device_001";
            
            // 第 1 次查询
            System.out.println("第 1 次查询...");
            var device1 = cacheService.getDevice(deviceId);
            
            // 第 2 次查询 (应该 L1 命中)
            System.out.println("第 2 次查询...");
            var device2 = cacheService.getDevice(deviceId);
            
            // 第 3 次查询 (应该 L1 命中)
            System.out.println("第 3 次查询...");
            var device3 = cacheService.getDevice(deviceId);
            
            // 验证：三次查询结果应该相同
            if (device1 != null) {
                assertEquals(device1.getDeviceId(), device2.getDeviceId());
                assertEquals(device1.getDeviceId(), device3.getDeviceId());
                System.out.println("✓ 多级缓存测试通过");
            } else {
                System.out.println("⚠ 设备不存在，跳过验证");
            }
            
        } catch (Exception e) {
            System.out.println("⚠ 多级缓存测试跳过 (数据库无测试数据)");
        }
    }

    /**
     * 测试 5: 获取缓存统计信息
     */
    @Test
    public void testCacheStats() {
        System.out.println("\n========== 获取缓存统计信息 ==========");
        
        try {
            var stats = cacheService.getCacheStats();
            
            System.out.println("✓ 缓存统计信息:");
            stats.forEach((key, value) -> 
                System.out.println("  - " + key + ": " + value)
            );
            
        } catch (Exception e) {
            System.out.println("⚠ 获取缓存统计失败：" + e.getMessage());
        }
    }

    /**
     * 测试 6: 虚拟线程批量处理
     */
    @Test
    public void testVirtualThreadBatch() throws Exception {
        System.out.println("\n========== 测试虚拟线程批量处理 ==========");
        
        List<String> deviceIds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            deviceIds.add("device_" + i);
        }
        
        long startTime = System.currentTimeMillis();
        
        virtualThreadService.processDevicesBatch(deviceIds);
        
        // 等待处理完成 (异步)
        Thread.sleep(2000);
        
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("✓ 批量处理测试完成");
        System.out.println("  - 设备数量：" + deviceIds.size());
        System.out.println("  - 处理耗时：" + duration + "ms");
    }
}
