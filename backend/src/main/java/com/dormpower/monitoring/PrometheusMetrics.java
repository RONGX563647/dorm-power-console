package com.dormpower.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Prometheus 性能监控指标
 * 
 * 提供系统关键性能指标 (KPI) 的采集和暴露:
 * - 接口响应时间 (P50, P90, P99)
 * - 请求 QPS 统计
 * - 设备在线数
 * - WebSocket 连接数
 * - 缓存命中率
 * - 限流统计
 * 
 * 指标命名规范:
 * - 前缀：dormpower_
 * - 类型：_total (Counter), _seconds (Timer), _ratio (Gauge)
 * - 标签：method, uri, status, device_type 等
 * 
 * @author dormpower team
 * @version 1.0
 */
@Component
public class PrometheusMetrics {

    private static final Logger logger = LoggerFactory.getLogger(PrometheusMetrics.class);

    private final MeterRegistry meterRegistry;

    // 计数器
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    
    // 定时器
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    
    // 仪表盘
    private final Map<String, AtomicInteger> gauges = new ConcurrentHashMap<>();
    
    // 长整型仪表盘
    private final Map<String, AtomicLong> longGauges = new ConcurrentHashMap<>();

    public PrometheusMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        logger.info("Prometheus 监控指标初始化完成");
    }

    // ==================== API 性能指标 ====================

    /**
     * 记录 API 请求耗时
     * 
     * @param method HTTP 方法 (GET/POST/PUT/DELETE)
     * @param uri 请求路径
     * @param status HTTP 状态码
     * @param durationMs 耗时 (毫秒)
     */
    public void recordApiDuration(String method, String uri, int status, long durationMs) {
        String name = "dormpower_api_request_seconds";
        
        Timer timer = timers.computeIfAbsent(name, k -> 
            Timer.builder(name)
                .description("API 请求耗时")
                .tag("method", method)
                .tag("uri", uri)
                .tag("status", String.valueOf(status))
                .publishPercentileHistogram()  // 直方图用于计算分位数
                .register(meterRegistry)
        );
        
        timer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 记录 API 请求总数
     * 
     * @param method HTTP 方法
     * @param uri 请求路径
     * @param status HTTP 状态码
     */
    public void incrementApiRequest(String method, String uri, int status) {
        String name = "dormpower_api_request_total";
        
        String key = String.format("%s:%s:%s:%s", name, method, uri, status);
        Counter counter = counters.computeIfAbsent(key, k ->
            Counter.builder(name)
                .description("API 请求总数")
                .tag("method", method)
                .tag("uri", uri)
                .tag("status", String.valueOf(status))
                .register(meterRegistry)
        );
        
        counter.increment();
    }

    // ==================== 设备指标 ====================

    /**
     * 更新设备在线数量
     * 
     * @param count 在线设备数
     */
    public void updateDeviceOnlineCount(int count) {
        updateGauge("dormpower_device_online_total", count, "在线设备总数");
    }

    /**
     * 记录设备状态上报
     * 
     * @param deviceId 设备 ID
     * @param deviceType 设备类型
     */
    public void incrementDeviceStatusReport(String deviceId, String deviceType) {
        String name = "dormpower_device_status_report_total";
        
        String key = String.format("%s:%s", name, deviceType);
        Counter counter = counters.computeIfAbsent(key, k ->
            Counter.builder(name)
                .description("设备状态上报总数")
                .tag("device_type", deviceType)
                .register(meterRegistry)
        );
        
        counter.increment();
    }

    // ==================== WebSocket 指标 ====================

    /**
     * 更新 WebSocket 连接数
     * 
     * @param count 连接数
     */
    public void updateWebSocketConnections(int count) {
        updateGauge("dormpower_websocket_connections_total", count, "WebSocket 连接总数");
    }

    /**
     * 记录 WebSocket 消息发送
     * 
     * @param messageType 消息类型
     */
    public void incrementWebSocketMessageSent(String messageType) {
        String name = "dormpower_websocket_message_sent_total";
        
        String key = String.format("%s:%s", name, messageType);
        Counter counter = counters.computeIfAbsent(key, k ->
            Counter.builder(name)
                .description("WebSocket 发送消息总数")
                .tag("message_type", messageType)
                .register(meterRegistry)
        );
        
        counter.increment();
    }

    // ==================== 缓存指标 ====================

    /**
     * 更新缓存命中率
     * 
     * @param hitRate 命中率 (0.0-1.0)
     */
    public void updateCacheHitRate(double hitRate) {
        updateLongGauge("dormpower_cache_hit_rate", (long) (hitRate * 100), "缓存命中率 (百分比)");
    }

    /**
     * 记录缓存命中次数
     * 
     * @param cacheName 缓存名称
     */
    public void incrementCacheHit(String cacheName) {
        String name = "dormpower_cache_hit_total";
        
        String key = String.format("%s:%s", name, cacheName);
        Counter counter = counters.computeIfAbsent(key, k ->
            Counter.builder(name)
                .description("缓存命中总数")
                .tag("cache_name", cacheName)
                .register(meterRegistry)
        );
        
        counter.increment();
    }

    /**
     * 记录缓存未命中次数
     * 
     * @param cacheName 缓存名称
     */
    public void incrementCacheMiss(String cacheName) {
        String name = "dormpower_cache_miss_total";
        
        String key = String.format("%s:%s", name, cacheName);
        Counter counter = counters.computeIfAbsent(key, k ->
            Counter.builder(name)
                .description("缓存未命中总数")
                .tag("cache_name", cacheName)
                .register(meterRegistry)
        );
        
        counter.increment();
    }

    // ==================== 限流指标 ====================

    /**
     * 记录限流拒绝次数
     * 
     * @param limitType 限流类型 (api/login/device)
     * @param reason 拒绝原因
     */
    public void incrementRateLimitRejected(String limitType, String reason) {
        String name = "dormpower_rate_limit_rejected_total";
        
        String key = String.format("%s:%s:%s", name, limitType, reason);
        Counter counter = counters.computeIfAbsent(key, k ->
            Counter.builder(name)
                .description("限流拒绝总数")
                .tag("limit_type", limitType)
                .tag("reason", reason)
                .register(meterRegistry)
        );
        
        counter.increment();
    }

    /**
     * 记录限流允许次数
     * 
     * @param limitType 限流类型
     */
    public void incrementRateLimitAllowed(String limitType) {
        String name = "dormpower_rate_limit_allowed_total";
        
        String key = String.format("%s:%s", name, limitType);
        Counter counter = counters.computeIfAbsent(key, k ->
            Counter.builder(name)
                .description("限流允许总数")
                .tag("limit_type", limitType)
                .register(meterRegistry)
        );
        
        counter.increment();
    }

    // ==================== 系统指标 ====================

    /**
     * 更新 JVM 内存使用率
     * 
     * @param usage 使用率 (0.0-1.0)
     */
    public void updateJvmMemoryUsage(double usage) {
        updateGauge("dormpower_jvm_memory_usage_ratio", (int) (usage * 100), "JVM 内存使用率 (百分比)");
    }

    /**
     * 更新活跃线程数
     * 
     * @param count 线程数
     */
    public void updateActiveThreads(int count) {
        updateGauge("dormpower_threads_active_total", count, "活跃线程总数");
    }

    // ==================== 辅助方法 ====================

    private void updateGauge(String name, int value, String description) {
        AtomicInteger gauge = gauges.computeIfAbsent(name, k -> {
            AtomicInteger atomic = new AtomicInteger(0);
            Gauge.builder(name, atomic, AtomicInteger::get)
                .description(description)
                .register(meterRegistry);
            return atomic;
        });
        gauge.set(value);
    }

    private void updateLongGauge(String name, long value, String description) {
        AtomicLong gauge = longGauges.computeIfAbsent(name, k -> {
            AtomicLong atomic = new AtomicLong(0);
            Gauge.builder(name, atomic, AtomicLong::get)
                .description(description)
                .register(meterRegistry);
            return atomic;
        });
        gauge.set(value);
    }

    /**
     * 清除所有指标 (用于测试)
     */
    public void clear() {
        counters.clear();
        timers.clear();
        gauges.clear();
        longGauges.clear();
    }
}
