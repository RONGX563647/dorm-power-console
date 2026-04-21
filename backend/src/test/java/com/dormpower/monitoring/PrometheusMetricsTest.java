package com.dormpower.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prometheus 监控指标测试
 */
class PrometheusMetricsTest {

    private PrometheusMetrics prometheusMetrics;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        prometheusMetrics = new PrometheusMetrics(meterRegistry);
    }

    @Test
    void testRecordApiDuration() {
        // 记录 API 请求耗时
        prometheusMetrics.recordApiDuration("GET", "/api/devices", 200, 100);
        
        // 验证指标已注册
        assertTrue(meterRegistry.getMeters().size() > 0, "应该有注册的指标");
    }

    @Test
    void testIncrementApiRequest() {
        // 记录 API 请求
        prometheusMetrics.incrementApiRequest("GET", "/api/devices", 200);
        
        // 验证计数器已增加
        var counters = meterRegistry.get("dormpower_api_request_total").counters();
        assertTrue(counters.size() > 0, "应该有 API 请求计数器");
    }

    @Test
    void testUpdateDeviceOnlineCount() {
        // 更新设备在线数
        prometheusMetrics.updateDeviceOnlineCount(50);
        
        // 验证仪表盘值
        var gauges = meterRegistry.get("dormpower_device_online_total").gauges();
        assertEquals(1, gauges.size(), "应该有一个仪表盘");
        assertEquals(50.0, gauges.get(0).value(), 0.01, "仪表盘值应该是 50");
    }

    @Test
    void testUpdateWebSocketConnections() {
        // 更新 WebSocket 连接数
        prometheusMetrics.updateWebSocketConnections(100);
        
        var gauges = meterRegistry.get("dormpower_websocket_connections_total").gauges();
        assertEquals(100.0, gauges.get(0).value(), 0.01, "WebSocket 连接数应该是 100");
    }

    @Test
    void testUpdateCacheHitRate() {
        // 更新缓存命中率 (0.95 = 95%)
        prometheusMetrics.updateCacheHitRate(0.95);
        
        var gauges = meterRegistry.get("dormpower_cache_hit_rate").gauges();
        assertEquals(95.0, gauges.get(0).value(), 0.01, "缓存命中率应该是 95");
    }

    @Test
    void testIncrementCacheHit() {
        // 记录缓存命中
        prometheusMetrics.incrementCacheHit("devices");
        prometheusMetrics.incrementCacheHit("devices");
        prometheusMetrics.incrementCacheHit("users");
        
        var counters = meterRegistry.get("dormpower_cache_hit_total").counters();
        assertEquals(2, counters.size(), "应该有两个缓存命中计数器");
    }

    @Test
    void testIncrementCacheMiss() {
        // 记录缓存未命中
        prometheusMetrics.incrementCacheMiss("devices");
        
        var counters = meterRegistry.get("dormpower_cache_miss_total").counters();
        assertEquals(1, counters.size(), "应该有一个缓存未命中计数器");
    }

    @Test
    void testIncrementRateLimitRejected() {
        // 记录限流拒绝
        prometheusMetrics.incrementRateLimitRejected("api", "too_many_requests");
        prometheusMetrics.incrementRateLimitRejected("api", "too_many_requests");
        
        var counters = meterRegistry.get("dormpower_rate_limit_rejected_total").counters();
        assertEquals(1, counters.size(), "应该有一个限流拒绝计数器");
    }

    @Test
    void testIncrementRateLimitAllowed() {
        // 记录限流允许
        prometheusMetrics.incrementRateLimitAllowed("api");
        
        var counters = meterRegistry.get("dormpower_rate_limit_allowed_total").counters();
        assertEquals(1, counters.size(), "应该有一个限流允许计数器");
    }

    @Test
    void testUpdateJvmMemoryUsage() {
        // 更新 JVM 内存使用率 (0.75 = 75%)
        prometheusMetrics.updateJvmMemoryUsage(0.75);
        
        var gauges = meterRegistry.get("dormpower_jvm_memory_usage_ratio").gauges();
        assertEquals(75.0, gauges.get(0).value(), 0.01, "内存使用率应该是 75");
    }

    @Test
    void testUpdateActiveThreads() {
        // 更新活跃线程数
        prometheusMetrics.updateActiveThreads(20);
        
        var gauges = meterRegistry.get("dormpower_threads_active_total").gauges();
        assertEquals(20.0, gauges.get(0).value(), 0.01, "活跃线程数应该是 20");
    }

    @Test
    void testClear() {
        // 添加一些指标
        prometheusMetrics.recordApiDuration("GET", "/api/test", 200, 50);
        prometheusMetrics.incrementApiRequest("GET", "/api/test", 200);
        prometheusMetrics.updateDeviceOnlineCount(10);
        
        // 清除所有指标
        prometheusMetrics.clear();
        
        // 验证指标已清除
        assertEquals(0, meterRegistry.getMeters().size(), "所有指标应该已清除");
    }

    @Test
    void testMultipleApiRequests() {
        // 模拟多个 API 请求
        for (int i = 0; i < 100; i++) {
            prometheusMetrics.recordApiDuration("GET", "/api/devices", 200, 50 + i);
            prometheusMetrics.incrementApiRequest("GET", "/api/devices", 200);
        }
        
        // 验证指标数量
        var counters = meterRegistry.get("dormpower_api_request_total").counters();
        assertEquals(1, counters.size(), "应该只有一个 API 请求计数器");
        
        // 验证计数
        double count = counters.iterator().next().count();
        assertEquals(100.0, count, 0.01, "API 请求计数应该是 100");
    }

    @Test
    void testDifferentHttpMethods() {
        // 测试不同 HTTP 方法
        prometheusMetrics.incrementApiRequest("GET", "/api/devices", 200);
        prometheusMetrics.incrementApiRequest("POST", "/api/devices", 201);
        prometheusMetrics.incrementApiRequest("PUT", "/api/devices/1", 200);
        prometheusMetrics.incrementApiRequest("DELETE", "/api/devices/1", 204);
        
        var counters = meterRegistry.get("dormpower_api_request_total").counters();
        assertEquals(4, counters.size(), "应该有 4 个不同方法的计数器");
    }

    @Test
    void testDifferentStatusCodes() {
        // 测试不同状态码
        prometheusMetrics.incrementApiRequest("GET", "/api/devices", 200);
        prometheusMetrics.incrementApiRequest("GET", "/api/devices", 404);
        prometheusMetrics.incrementApiRequest("GET", "/api/devices", 500);
        
        var counters = meterRegistry.get("dormpower_api_request_total").counters();
        assertEquals(3, counters.size(), "应该有 3 个不同状态码的计数器");
    }
}
