package com.dormpower.config;

import com.dormpower.service.MqttSimulatorService;
import com.dormpower.websocket.WebSocketManager;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 线程池监控指标配置
 *
 * 监控的线程池：
 * 1. taskExecutor - 异步任务线程池
 * 2. ws-sender - WebSocket 发送线程池
 * 3. mqtt-simulator - MQTT 模拟器线程池
 */
@Configuration
public class ThreadPoolMetricsConfig {

    @Autowired(required = false)
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired(required = false)
    private WebSocketManager webSocketManager;

    @Autowired(required = false)
    private MqttSimulatorService mqttSimulatorService;

    // taskExecutor 线程池监控
    @Bean
    public Gauge taskExecutorActiveGauge(MeterRegistry registry) {
        return Gauge.builder("executor.active", () -> {
                if (taskExecutor != null) {
                    return taskExecutor.getActiveCount();
                }
                return 0;
            })
            .tag("name", "taskExecutor")
            .description("Active threads in task executor")
            .register(registry);
    }

    @Bean
    public Gauge taskExecutorQueuedGauge(MeterRegistry registry) {
        return Gauge.builder("executor.queued", () -> {
                if (taskExecutor != null) {
                    return taskExecutor.getThreadPoolExecutor().getQueue().size();
                }
                return 0;
            })
            .tag("name", "taskExecutor")
            .description("Queued tasks in task executor")
            .register(registry);
    }

    @Bean
    public Gauge taskExecutorPoolSizeGauge(MeterRegistry registry) {
        return Gauge.builder("executor.pool.size", () -> {
                if (taskExecutor != null) {
                    return taskExecutor.getPoolSize();
                }
                return 0;
            })
            .tag("name", "taskExecutor")
            .description("Current pool size of task executor")
            .register(registry);
    }

    @Bean
    public Gauge taskExecutorCorePoolSizeGauge(MeterRegistry registry) {
        return Gauge.builder("executor.pool.core", () -> {
                if (taskExecutor != null) {
                    return taskExecutor.getCorePoolSize();
                }
                return 0;
            })
            .tag("name", "taskExecutor")
            .description("Core pool size of task executor")
            .register(registry);
    }

    @Bean
    public Gauge taskExecutorMaxPoolSizeGauge(MeterRegistry registry) {
        return Gauge.builder("executor.pool.max", () -> {
                if (taskExecutor != null) {
                    return taskExecutor.getMaxPoolSize();
                }
                return 0;
            })
            .tag("name", "taskExecutor")
            .description("Max pool size of task executor")
            .register(registry);
    }

    // WebSocket 发送线程池监控
    @Bean
    public Gauge wsSenderActiveGauge(MeterRegistry registry) {
        return Gauge.builder("executor.active", () -> {
                if (webSocketManager != null) {
                    return webSocketManager.getSendExecutor().getActiveCount();
                }
                return 0;
            })
            .tag("name", "wsSender")
            .description("Active threads in WebSocket sender executor")
            .register(registry);
    }

    @Bean
    public Gauge wsSenderQueuedGauge(MeterRegistry registry) {
        return Gauge.builder("executor.queued", () -> {
                if (webSocketManager != null) {
                    return webSocketManager.getSendExecutor().getQueue().size();
                }
                return 0;
            })
            .tag("name", "wsSender")
            .description("Queued tasks in WebSocket sender executor")
            .register(registry);
    }

    @Bean
    public Gauge wsSenderPoolSizeGauge(MeterRegistry registry) {
        return Gauge.builder("executor.pool.size", () -> {
                if (webSocketManager != null) {
                    return webSocketManager.getSendExecutor().getPoolSize();
                }
                return 0;
            })
            .tag("name", "wsSender")
            .description("Current pool size of WebSocket sender executor")
            .register(registry);
    }

    @Bean
    public Gauge wsSenderCompletedGauge(MeterRegistry registry) {
        return Gauge.builder("executor.completed", () -> {
                if (webSocketManager != null) {
                    return webSocketManager.getSendExecutor().getCompletedTaskCount();
                }
                return 0L;
            })
            .tag("name", "wsSender")
            .description("Completed tasks in WebSocket sender executor")
            .register(registry);
    }

    // MQTT 模拟器线程池监控
    @Bean
    public Gauge mqttSimulatorActiveGauge(MeterRegistry registry) {
        return Gauge.builder("executor.active", () -> {
                if (mqttSimulatorService != null) {
                    return mqttSimulatorService.getExecutorService().getActiveCount();
                }
                return 0;
            })
            .tag("name", "mqttSimulator")
            .description("Active threads in MQTT simulator executor")
            .register(registry);
    }

    @Bean
    public Gauge mqttSimulatorQueuedGauge(MeterRegistry registry) {
        return Gauge.builder("executor.queued", () -> {
                if (mqttSimulatorService != null) {
                    return mqttSimulatorService.getExecutorService().getQueue().size();
                }
                return 0;
            })
            .tag("name", "mqttSimulator")
            .description("Queued tasks in MQTT simulator executor")
            .register(registry);
    }

    @Bean
    public Gauge mqttSimulatorPoolSizeGauge(MeterRegistry registry) {
        return Gauge.builder("executor.pool.size", () -> {
                if (mqttSimulatorService != null) {
                    return mqttSimulatorService.getExecutorService().getPoolSize();
                }
                return 0;
            })
            .tag("name", "mqttSimulator")
            .description("Current pool size of MQTT simulator executor")
            .register(registry);
    }

    @Bean
    public Gauge mqttSimulatorCompletedGauge(MeterRegistry registry) {
        return Gauge.builder("executor.completed", () -> {
                if (mqttSimulatorService != null) {
                    return mqttSimulatorService.getExecutorService().getCompletedTaskCount();
                }
                return 0L;
            })
            .tag("name", "mqttSimulator")
            .description("Completed tasks in MQTT simulator executor")
            .register(registry);
    }
}