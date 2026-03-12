package com.dormpower.config;

import com.dormpower.websocket.WebSocketManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 监控指标配置类
 *
 * 功能：
 * 1. 注册自定义 Prometheus 指标
 * 2. 监控 WebSocket 连接数
 * 3. 监控消息处理性能
 * 4. 监控数据库操作性能
 *
 * @author dormpower team
 * @version 1.0
 */
@Configuration
public class MetricsConfig {

    @Autowired(required = false)
    private WebSocketManager webSocketManager;

    /**
     * WebSocket 消息发送计数器
     */
    @Bean
    public Counter webSocketMessagesSent(MeterRegistry registry) {
        return Counter.builder("websocket_messages_sent_total")
            .description("Total WebSocket messages sent")
            .register(registry);
    }

    /**
     * WebSocket 消息发送失败计数器
     */
    @Bean
    public Counter webSocketMessagesFailed(MeterRegistry registry) {
        return Counter.builder("websocket_messages_failed_total")
            .description("Total WebSocket messages failed to send")
            .register(registry);
    }

    /**
     * Kafka 消息发送计数器
     */
    @Bean
    public Counter kafkaMessagesSent(MeterRegistry registry) {
        return Counter.builder("kafka_messages_sent_total")
            .description("Total Kafka messages sent")
            .tag("topic", "telemetry")
            .register(registry);
    }

    /**
     * Kafka 消息消费计数器
     */
    @Bean
    public Counter kafkaMessagesConsumed(MeterRegistry registry) {
        return Counter.builder("kafka_messages_consumed_total")
            .description("Total Kafka messages consumed")
            .tag("topic", "telemetry")
            .register(registry);
    }

    /**
     * MQTT 消息接收计数器
     */
    @Bean
    public Counter mqttMessagesReceived(MeterRegistry registry) {
        return Counter.builder("mqtt_messages_received_total")
            .description("Total MQTT messages received")
            .register(registry);
    }

    /**
     * 遥测数据写入计时器
     */
    @Bean
    public Timer telemetryWriteTimer(MeterRegistry registry) {
        return Timer.builder("telemetry_write_duration")
            .description("Time taken to write telemetry data")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }

    /**
     * 注册 WebSocket 连接数 Gauge
     */
    @Bean
    public Gauge webSocketConnectionsGauge(MeterRegistry registry) {
        return Gauge.builder("websocket_connections_active", () -> {
                if (webSocketManager != null) {
                    return webSocketManager.getSessionCount();
                }
                return 0;
            })
            .description("Current number of active WebSocket connections")
            .register(registry);
    }

    /**
     * Redis 命令计时器
     */
    @Bean
    public Timer redisCommandTimer(MeterRegistry registry) {
        return Timer.builder("redis_command_duration")
            .description("Time taken for Redis commands")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }

    /**
     * API 限流计数器
     */
    @Bean
    public Counter rateLimitExceededCounter(MeterRegistry registry) {
        return Counter.builder("rate_limit_exceeded_total")
            .description("Total requests rejected by rate limiter")
            .register(registry);
    }

    /**
     * 记录消息发送指标
     */
    public static void recordMessageSent(MeterRegistry registry, String type) {
        Counter.builder("messages_sent_total")
            .tag("type", type)
            .register(registry)
            .increment();
    }

    /**
     * 记录消息处理时间
     */
    public static void recordProcessingTime(MeterRegistry registry, String operation, long durationMs) {
        Timer.builder("operation_duration")
            .tag("operation", operation)
            .register(registry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
}