package com.dormpower.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis缓存性能测试
 * 
 * 测试目标：
 * 1. 对比缓存命中和缓存未命中的查询性能
 * 2. 测试缓存对数据库查询压力的影响
 * 3. 测试系统吞吐量的提升
 * 
 * 注意：此测试需要Redis服务运行，请确保Redis已启动
 * 
 * @author dormpower team
 * @version 1.0
 */
@SpringBootTest
public class RedisCachePerformanceTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    private static final int TEST_ITERATIONS = 1000;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    /**
     * 测试Redis操作延迟
     */
    @Test
    void testRedisOperationLatency() {
        System.out.println("\n========== Redis操作延迟测试 ==========");
        
        String testKey = "test:latency:key";
        String testValue = "test_value";
        
        List<Long> writeLatencies = new ArrayList<>();
        List<Long> readLatencies = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            long startTime = System.nanoTime();
            redisTemplate.opsForValue().set(testKey + i, testValue);
            long endTime = System.nanoTime();
            writeLatencies.add(endTime - startTime);
            
            startTime = System.nanoTime();
            Object value = redisTemplate.opsForValue().get(testKey + i);
            endTime = System.nanoTime();
            readLatencies.add(endTime - startTime);
        }
        
        double avgWriteLatency = writeLatencies.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0) / 1_000_000.0;
        
        double avgReadLatency = readLatencies.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0) / 1_000_000.0;
        
        System.out.println("Redis写入平均延迟: " + 
            String.format("%.2f", avgWriteLatency) + " ms");
        System.out.println("Redis读取平均延迟: " + 
            String.format("%.2f", avgReadLatency) + " ms");
        System.out.println("Redis操作平均延迟: " + 
            String.format("%.2f", (avgWriteLatency + avgReadLatency) / 2) + " ms");
        
        assertTrue(avgWriteLatency < 10, 
            "Redis写入延迟应该小于10ms");
        assertTrue(avgReadLatency < 5, 
            "Redis读取延迟应该小于5ms");
    }

    /**
     * 测试Redis批量操作性能
     */
    @Test
    void testRedisBatchPerformance() {
        System.out.println("\n========== Redis批量操作性能测试 ==========");
        
        String testKeyPrefix = "test:batch:key:";
        String testValue = "test_value";
        
        long startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            redisTemplate.opsForValue().set(testKeyPrefix + i, testValue);
        }
        long endTime = System.nanoTime();
        long writeTime = endTime - startTime;
        
        startTime = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            redisTemplate.opsForValue().get(testKeyPrefix + i);
        }
        endTime = System.nanoTime();
        long readTime = endTime - startTime;
        
        double writeThroughput = 
            (double) TEST_ITERATIONS / (writeTime / 1_000_000_000.0);
        double readThroughput = 
            (double) TEST_ITERATIONS / (readTime / 1_000_000_000.0);
        
        System.out.println("写入 " + TEST_ITERATIONS + " 条数据耗时: " + 
            String.format("%.2f", writeTime / 1_000_000.0) + " ms");
        System.out.println("读取 " + TEST_ITERATIONS + " 条数据耗时: " + 
            String.format("%.2f", readTime / 1_000_000.0) + " ms");
        System.out.println("写入吞吐量: " + 
            String.format("%.2f", writeThroughput) + " ops/s");
        System.out.println("读取吞吐量: " + 
            String.format("%.2f", readThroughput) + " ops/s");
        
        assertTrue(writeThroughput > 1000, 
            "Redis写入吞吐量应该大于1000 ops/s");
        assertTrue(readThroughput > 1000, 
            "Redis读取吞吐量应该大于1000 ops/s");
    }

    /**
     * 测试Redis Hash操作性能
     */
    @Test
    void testRedisHashPerformance() {
        System.out.println("\n========== Redis Hash操作性能测试 ==========");
        
        String hashKey = "test:hash:device";
        
        List<Long> putLatencies = new ArrayList<>();
        List<Long> getLatencies = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            long startTime = System.nanoTime();
            redisTemplate.opsForHash().put(hashKey, "field_" + i, "value_" + i);
            long endTime = System.nanoTime();
            putLatencies.add(endTime - startTime);
            
            startTime = System.nanoTime();
            Object value = redisTemplate.opsForHash().get(hashKey, "field_" + i);
            endTime = System.nanoTime();
            getLatencies.add(endTime - startTime);
        }
        
        double avgPutLatency = putLatencies.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0) / 1_000_000.0;
        
        double avgGetLatency = getLatencies.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0) / 1_000_000.0;
        
        System.out.println("Hash Put平均延迟: " + 
            String.format("%.2f", avgPutLatency) + " ms");
        System.out.println("Hash Get平均延迟: " + 
            String.format("%.2f", avgGetLatency) + " ms");
        
        assertTrue(avgPutLatency < 10, 
            "Hash Put延迟应该小于10ms");
        assertTrue(avgGetLatency < 5, 
            "Hash Get延迟应该小于5ms");
    }

    /**
     * 测试Redis缓存命中率模拟
     */
    @Test
    void testCacheHitRateSimulation() {
        System.out.println("\n========== 缓存命中率模拟测试 ==========");
        
        String cacheKeyPrefix = "test:cache:device:";
        String cacheValue = "cached_device_data";
        
        int totalQueries = 1000;
        int cacheHits = 0;
        
        for (int i = 0; i < 100; i++) {
            redisTemplate.opsForValue().set(cacheKeyPrefix + i, cacheValue);
        }
        
        for (int i = 0; i < totalQueries; i++) {
            String key = cacheKeyPrefix + (i % 100);
            Object value = redisTemplate.opsForValue().get(key);
            
            if (value != null) {
                cacheHits++;
            }
        }
        
        double hitRate = (double) cacheHits / totalQueries * 100;
        
        System.out.println("总查询次数: " + totalQueries);
        System.out.println("缓存命中次数: " + cacheHits);
        System.out.println("缓存命中率: " + String.format("%.2f", hitRate) + "%");
        
        assertTrue(hitRate > 90, 
            "缓存命中率应该高于90%");
    }

    /**
     * 测试Redis连接池性能
     */
    @Test
    void testRedisConnectionPoolPerformance() {
        System.out.println("\n========== Redis连接池性能测试 ==========");
        
        int threadCount = 10;
        int operationsPerThread = 100;
        
        List<Thread> threads = new ArrayList<>();
        List<Long> latencies = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    long startTime = System.nanoTime();
                    redisTemplate.opsForValue().set(
                        "test:concurrent:" + Thread.currentThread().getId() + ":" + j, 
                        "value"
                    );
                    long endTime = System.nanoTime();
                    synchronized (latencies) {
                        latencies.add(endTime - startTime);
                    }
                }
            });
            threads.add(thread);
        }
        
        long startTime = System.nanoTime();
        threads.forEach(Thread::start);
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        
        double avgLatency = latencies.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0) / 1_000_000.0;
        
        int totalOperations = threadCount * operationsPerThread;
        double throughput = (double) totalOperations / (totalTime / 1_000_000_000.0);
        
        System.out.println("并发线程数: " + threadCount);
        System.out.println("每线程操作数: " + operationsPerThread);
        System.out.println("总操作数: " + totalOperations);
        System.out.println("总耗时: " + String.format("%.2f", totalTime / 1_000_000.0) + " ms");
        System.out.println("平均延迟: " + String.format("%.2f", avgLatency) + " ms");
        System.out.println("吞吐量: " + String.format("%.2f", throughput) + " ops/s");
        
        assertTrue(throughput > 1000, 
            "并发吞吐量应该大于1000 ops/s");
    }
}
