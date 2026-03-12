package com.dormpower.config;

import com.dormpower.service.MqttSimulatorService;
import com.dormpower.websocket.WebSocketManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池健康指示器
 *
 * 健康状态判定：
 * - UP: 所有线程池队列使用率 < 50%
 * - WARNING: 任一线程池队列使用率 >= 50% 且 < 80%
 * - DOWN: 任一线程池队列使用率 >= 80%
 */
@Component
public class ThreadPoolHealthIndicator implements HealthIndicator {

    private static final double WARNING_THRESHOLD = 0.5;
    private static final double DOWN_THRESHOLD = 0.8;

    @Autowired(required = false)
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired(required = false)
    private WebSocketManager webSocketManager;

    @Autowired(required = false)
    private MqttSimulatorService mqttSimulatorService;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        Status overallStatus = Status.UP;

        // 检查 taskExecutor
        if (taskExecutor != null) {
            HealthCheckResult taskExecutorResult = checkThreadPool(
                "taskExecutor",
                taskExecutor.getThreadPoolExecutor()
            );
            details.put("taskExecutor", taskExecutorResult.details);
            overallStatus = worstStatus(overallStatus, taskExecutorResult.status);
        }

        // 检查 WebSocket 发送线程池
        if (webSocketManager != null) {
            HealthCheckResult wsSenderResult = checkThreadPool(
                "wsSender",
                webSocketManager.getSendExecutor()
            );
            details.put("wsSender", wsSenderResult.details);
            overallStatus = worstStatus(overallStatus, wsSenderResult.status);
        }

        // 检查 MQTT 模拟器线程池
        if (mqttSimulatorService != null) {
            HealthCheckResult mqttSimulatorResult = checkThreadPool(
                "mqttSimulator",
                mqttSimulatorService.getExecutorService()
            );
            details.put("mqttSimulator", mqttSimulatorResult.details);
            overallStatus = worstStatus(overallStatus, mqttSimulatorResult.status);
        }

        Health.Builder builder = Health.status(overallStatus);
        details.forEach(builder::withDetail);
        return builder.build();
    }

    private HealthCheckResult checkThreadPool(String name, ThreadPoolExecutor executor) {
        int activeCount = executor.getActiveCount();
        int poolSize = executor.getPoolSize();
        int corePoolSize = executor.getCorePoolSize();
        int maxPoolSize = executor.getMaximumPoolSize();
        int queueSize = executor.getQueue().size();
        int queueCapacity = executor.getQueue().remainingCapacity() + queueSize;
        long completedTasks = executor.getCompletedTaskCount();

        double queueUsage = queueCapacity > 0 ? (double) queueSize / queueCapacity : 0;

        Status status;
        if (queueUsage >= DOWN_THRESHOLD) {
            status = Status.DOWN;
        } else if (queueUsage >= WARNING_THRESHOLD) {
            status = new Status("WARNING", "ThreadPool queue usage is high");
        } else {
            status = Status.UP;
        }

        Map<String, Object> details = new HashMap<>();
        details.put("activeThreads", activeCount);
        details.put("poolSize", poolSize);
        details.put("corePoolSize", corePoolSize);
        details.put("maxPoolSize", maxPoolSize);
        details.put("queueSize", queueSize);
        details.put("queueCapacity", queueCapacity);
        details.put("queueUsage", String.format("%.2f%%", queueUsage * 100));
        details.put("completedTasks", completedTasks);
        details.put("status", status.getCode());

        return new HealthCheckResult(status, details);
    }

    private Status worstStatus(Status current, Status other) {
        if (other.getCode().equals("DOWN")) {
            return Status.DOWN;
        }
        if (other.getCode().equals("WARNING") && current.equals(Status.UP)) {
            return other;
        }
        return current;
    }

    private record HealthCheckResult(Status status, Map<String, Object> details) {}
}