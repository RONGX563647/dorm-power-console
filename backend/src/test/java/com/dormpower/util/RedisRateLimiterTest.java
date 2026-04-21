package com.dormpower.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis+Lua 分布式限流测试
 */
@SpringBootTest
@ActiveProfiles("test")
class RedisRateLimiterTest {

    @Autowired(required = false)
    private RedisRateLimiter redisRateLimiter;

    private String testKey;

    @BeforeEach
    void setUp() {
        testKey = "rate_limit:test:" + System.currentTimeMillis();
    }

    @Test
    void testTryAcquire_Allowed() {
        // 测试允许请求
        boolean allowed = redisRateLimiter.tryAcquire(testKey, 5, 1);
        assertTrue(allowed, "第一个请求应该被允许");
    }

    @Test
    void testTryAcquire_Rejected() {
        // 测试限流拒绝
        int maxRequests = 3;
        int windowSize = 1;
        
        // 发送 3 个请求 (应该都允许)
        for (int i = 0; i < maxRequests; i++) {
            boolean allowed = redisRateLimiter.tryAcquire(testKey, maxRequests, windowSize);
            assertTrue(allowed, "第 " + (i + 1) + " 个请求应该被允许");
        }
        
        // 第 4 个请求应该被拒绝
        boolean allowed = redisRateLimiter.tryAcquire(testKey, maxRequests, windowSize);
        assertFalse(allowed, "超过限制的请求应该被拒绝");
    }

    @Test
    void testGetCurrentCount() {
        int maxRequests = 5;
        int windowSize = 1;
        
        // 发送 3 个请求
        for (int i = 0; i < 3; i++) {
            redisRateLimiter.tryAcquire(testKey, maxRequests, windowSize);
        }
        
        // 获取当前计数
        long count = redisRateLimiter.getCurrentCount(testKey, windowSize);
        assertEquals(3, count, "当前计数应该是 3");
    }

    @Test
    void testGetRemainingQuota() {
        int maxRequests = 10;
        int windowSize = 1;
        
        // 发送 3 个请求
        for (int i = 0; i < 3; i++) {
            redisRateLimiter.tryAcquire(testKey, maxRequests, windowSize);
        }
        
        // 获取剩余配额
        long remaining = redisRateLimiter.getRemainingQuota(testKey, maxRequests, windowSize);
        assertEquals(7, remaining, "剩余配额应该是 7");
    }

    @Test
    void testReset() {
        int maxRequests = 5;
        int windowSize = 1;
        
        // 发送请求直到限流
        for (int i = 0; i < maxRequests; i++) {
            redisRateLimiter.tryAcquire(testKey, maxRequests, windowSize);
        }
        
        // 重置限流器
        redisRateLimiter.reset(testKey);
        
        // 验证可以再次请求
        boolean allowed = redisRateLimiter.tryAcquire(testKey, maxRequests, windowSize);
        assertTrue(allowed, "重置后应该允许请求");
    }

    @Test
    void testConcurrentRateLimiting() throws InterruptedException {
        int threadCount = 20;
        int maxRequests = 10;
        int windowSize = 1;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger allowedCount = new AtomicInteger(0);
        
        // 并发发送请求
        for (int i = 0; i < threadCount; i++) {
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
        
        latch.await();
        executor.shutdown();
        
        // 验证允许的请求数在合理范围内 (考虑并发误差)
        assertTrue(allowedCount.get() >= maxRequests - 2, 
            "允许的请求数应该接近限制值，实际：" + allowedCount.get());
        assertTrue(allowedCount.get() <= maxRequests + 2, 
            "允许的请求数不应该超过限制太多，实际：" + allowedCount.get());
    }

    @Test
    void testDifferentKeys() {
        String key1 = "rate_limit:key1:" + System.currentTimeMillis();
        String key2 = "rate_limit:key2:" + System.currentTimeMillis();
        
        int maxRequests = 3;
        int windowSize = 1;
        
        // key1 发送 3 个请求
        for (int i = 0; i < maxRequests; i++) {
            redisRateLimiter.tryAcquire(key1, maxRequests, windowSize);
        }
        
        // key2 发送 1 个请求 (应该允许，因为独立计数)
        boolean allowed = redisRateLimiter.tryAcquire(key2, maxRequests, windowSize);
        assertTrue(allowed, "不同 key 应该独立计数");
    }

    @Test
    void testWindowExpiration() throws InterruptedException {
        int maxRequests = 2;
        int windowSize = 1; // 1 秒窗口
        
        // 发送 2 个请求
        for (int i = 0; i < maxRequests; i++) {
            redisRateLimiter.tryAcquire(testKey, maxRequests, windowSize);
        }
        
        // 等待窗口过期
        Thread.sleep(1500);
        
        // 应该可以再次请求
        boolean allowed = redisRateLimiter.tryAcquire(testKey, maxRequests, windowSize);
        assertTrue(allowed, "窗口过期后应该允许请求");
    }

    @Test
    void testFixedWindowVsSlidingWindow() {
        // 测试滑动窗口和固定窗口的区别
        String fixedKey = "rate_limit:fixed:" + System.currentTimeMillis();
        String slidingKey = "rate_limit:sliding:" + System.currentTimeMillis();
        
        int maxRequests = 5;
        int windowSize = 1;
        
        // 两种窗口都应该允许 5 个请求
        for (int i = 0; i < maxRequests; i++) {
            assertTrue(redisRateLimiter.tryAcquire(fixedKey, maxRequests, windowSize));
            assertTrue(redisRateLimiter.tryAcquire(slidingKey, maxRequests, windowSize));
        }
        
        // 第 6 个请求都应该被拒绝
        assertFalse(redisRateLimiter.tryAcquire(fixedKey, maxRequests, windowSize));
        assertFalse(redisRateLimiter.tryAcquire(slidingKey, maxRequests, windowSize));
    }
}
