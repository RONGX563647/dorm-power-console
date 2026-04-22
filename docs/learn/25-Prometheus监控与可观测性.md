# 模块25：Prometheus监控与可观测性

> **学习时长**: 3-5 天  
> **难度**: ⭐⭐⭐⭐  
> **前置知识**: 监控概念、Prometheus基础、Grafana

---

## 一、系统架构

### 1.1 可观测性三大支柱

```yaml
可观测性:
  1. Metrics（指标）:
     - Prometheus + Grafana
     - 系统指标（CPU、内存）
     - 业务指标（QPS、延迟）
  
  2. Logging（日志）:
     - Log4j2 + ELK
     - 应用日志
     - 审计日志
  
  3. Tracing（链路追踪）:
     - Jaeger / Zipkin
     - 请求链路
     - 性能分析
```

### 1.2 监控指标分类

```
指标类型:
  ├── Counter（计数器）
  │   ├── 请求总数
  │   ├── 错误总数
  │   └── 消息处理数
  │
  ├── Gauge（仪表盘）
  │   ├── 连接数
  │   ├── 队列大小
  │   └── 缓存命中率
  │
  ├── Histogram（直方图）
  │   ├── 请求延迟分布
  │   ├── 响应大小分布
  │   └── 批处理大小
  │
  └── Summary（摘要）
      ├── P50/P90/P99延迟
      └── 平均响应时间
```

---

## 二、核心组件

### 2.1 PrometheusMetrics 监控指标

**文件位置**: `backend/src/main/java/com/dormpower/monitoring/PrometheusMetrics.java`

```java
package com.dormpower.monitoring;

import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Prometheus监控指标
 * 
 * 指标列表:
 * 1. 设备在线数
 * 2. 消息处理速率
 * 3. 缓存命中率
 * 4. WebSocket连接数
 * 5. 数据库连接池
 * 
 * @author dormpower team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class PrometheusMetrics {

    private final MeterRegistry meterRegistry;
    
    private Counter telemetryProcessedCounter;
    private Counter telemetryErrorCounter;
    private Gauge onlineDevicesGauge;
    private Gauge wsConnectionsGauge;
    private Timer telemetryProcessTimer;
    
    @PostConstruct
    public void initMetrics() {
        // 遥测处理计数器
        telemetryProcessedCounter = Counter.builder("dormpower.telemetry.processed")
            .description("Total telemetry messages processed")
            .tag("component", "mqtt")
            .register(meterRegistry);
        
        // 遥测错误计数器
        telemetryErrorCounter = Counter.builder("dormpower.telemetry.errors")
            .description("Total telemetry processing errors")
            .tag("component", "mqtt")
            .register(meterRegistry);
        
        // WebSocket连接数
        wsConnectionsGauge = Gauge.builder("dormpower.websocket.connections", 
            () -> getWsConnectionCount())
            .description("Current WebSocket connection count")
            .register(meterRegistry);
        
        // 遥测处理耗时
        telemetryProcessTimer = Timer.builder("dormpower.telemetry.process.time")
            .description("Time to process telemetry message")
            .tag("component", "mqtt")
            .register(meterRegistry);
    }
    
    public void incrementTelemetryProcessed() {
        telemetryProcessedCounter.increment();
    }
    
    public void incrementTelemetryError() {
        telemetryErrorCounter.increment();
    }
    
    public void recordTelemetryProcessTime(long millis) {
        telemetryProcessTimer.record(millis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    private int getWsConnectionCount() {
        // 从WebSocketManager获取
        return 0;
    }
}
```

### 2.2 ThreadPoolHealthIndicator 线程池健康检查

**文件位置**: `backend/src/main/java/com/dormpower/config/ThreadPoolHealthIndicator.java`

```java
package com.dormpower.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 线程池健康检查
 * 
 * 检查项:
 * 1. 活跃线程数
 * 2. 队列等待数
 * 3. 拒绝任务数
 * 
 * @author dormpower team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class ThreadPoolHealthIndicator implements HealthIndicator {

    private final Map<String, ThreadPoolTaskExecutor> threadPools;
    
    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        for (Map.Entry<String, ThreadPoolTaskExecutor> entry : threadPools.entrySet()) {
            String name = entry.getKey();
            ThreadPoolTaskExecutor pool = entry.getValue();
            
            int active = pool.getActiveCount();
            int poolSize = pool.getPoolSize();
            int queueSize = pool.getThreadPoolExecutor().getQueue().size();
            
            // 如果队列堆积超过1000，标记为DOWN
            if (queueSize > 1000) {
                builder.withDetail(name, Map.of(
                    "status", "DOWN",
                    "active", active,
                    "poolSize", poolSize,
                    "queueSize", queueSize
                ));
            } else {
                builder.withDetail(name, Map.of(
                    "status", "UP",
                    "active", active,
                    "poolSize", poolSize,
                    "queueSize", queueSize
                ));
            }
        }
        
        return builder.build();
    }
}
```

---

## 三、Prometheus配置

### 3.1 prometheus.yml

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'dorm-power'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8000']
        labels:
          application: 'dorm-power'
```

### 3.2 Grafana Dashboard

```json
{
  "dashboard": {
    "title": "宿舍用电管理系统监控",
    "panels": [
      {
        "title": "设备在线数",
        "type": "stat",
        "targets": [{"expr": "dormpower.devices.online"}]
      },
      {
        "title": "遥测消息处理速率",
        "type": "graph",
        "targets": [{"expr": "rate(dormpower.telemetry.processed[5m])"}]
      },
      {
        "title": "API响应时间P99",
        "type": "graph",
        "targets": [{"expr": "histogram_quantile(0.99, rate(api_request_duration_seconds_bucket[5m]))"}]
      }
    ]
  }
}
```

---

## 四、监控最佳实践

```yaml
指标命名规范:
  格式: {application}.{component}.{metric}
  示例:
    - dormpower.telemetry.processed
    - dormpower.mqtt.connections
    - dormpower.cache.hit_rate
  
告警规则:
  1. 设备离线率 > 10% 持续5分钟
  2. API错误率 > 5% 持续1分钟
  3. 数据库连接池使用率 > 80%
  4. 内存使用率 > 85%
  5. 磁盘使用率 > 90%
```

---

**最后更新**: 2026-04-22  
**文档版本**: 1.0
