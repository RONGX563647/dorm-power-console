# 项目 Prometheus 使用文档

## 一、项目为什么使用 Prometheus

### 1.1 技术选型背景

本项目是一个宿舍电源管理系统，涉及设备监控、实时通信、消息队列等多个组件，需要一套完善的监控体系来保障系统稳定运行。选择 Prometheus 主要基于以下考虑：

#### 1.1.1 与 Spring Boot 的完美集成

| 需求 | Prometheus 方案 | 其他方案 |
|------|----------------|----------|
| Spring Boot 集成 | Micrometer 原生支持 | 需要额外适配 |
| 自动指标暴露 | Actuator 端点 | 手动实现 |
| JVM 监控 | 内置 JVM 指标 | 需要额外配置 |

#### 1.1.2 云原生生态

```
┌─────────────────────────────────────────────────────────────────┐
│                    项目监控架构                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐         │
│  │ Spring Boot │    │ Prometheus  │    │  Grafana    │         │
│  │   应用      │───>│   采集存储  │───>│   可视化    │         │
│  │ Micrometer  │    │   TSDB      │    │  Dashboard  │         │
│  └─────────────┘    └─────────────┘    └─────────────┘         │
│        │                  │                  │                 │
│        │                  │                  │                 │
│        ▼                  ▼                  ▼                 │
│  /actuator/prometheus    :9090            :3001                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 1.1.3 轻量级部署

项目部署在 **2核2G** 的京东云轻量服务器上，Prometheus 的资源占用非常适合：

| 组件 | 内存限制 | 说明 |
|------|----------|------|
| Prometheus | 128M | 足够应对当前规模 |
| Grafana | 128M | 可视化展示 |

### 1.2 项目中的实际价值

#### 1.2.1 全方位监控覆盖

项目通过 Prometheus 监控了以下方面：

| 监控维度 | 指标示例 | 价值 |
|----------|----------|------|
| **系统指标** | CPU、内存、JVM | 了解系统健康状态 |
| **线程池** | 活跃线程、队列大小 | 发现性能瓶颈 |
| **WebSocket** | 连接数、消息数 | 监控实时通信状态 |
| **消息队列** | Kafka、MQTT 消息数 | 监控消息处理能力 |
| **数据库** | 操作延迟 | 发现数据库瓶颈 |
| **限流** | 被拒绝请求数 | 监控流量保护效果 |

#### 1.2.2 Grafana 可视化

项目预置了 Grafana Dashboard，实现开箱即用的可视化监控：

```json
{
  "title": "Thread Pool Monitor",
  "panels": [
    {"title": "Active Threads", "type": "stat"},
    {"title": "Queued Tasks", "type": "stat"},
    {"title": "Thread Pool Size Over Time", "type": "timeseries"},
    {"title": "Completed Tasks", "type": "timeseries"}
  ]
}
```

#### 1.2.3 问题排查能力

通过 Prometheus 可以快速定位问题：

```
场景：系统响应变慢

1. 查看 JVM 内存 → 发现内存使用率高
2. 查看线程池队列 → 发现任务堆积
3. 查看数据库延迟 → 发现慢查询
4. 定位问题 → 某个 SQL 缺少索引
```

---

## 二、项目中怎么使用 Prometheus

### 2.1 依赖配置

在 [pom.xml](../backend/pom.xml) 中引入依赖：

```xml
<!-- Micrometer Prometheus Registry -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 2.2 application.yml 配置

```yaml
# Actuator配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      enabled: true
    metrics:
      enabled: true
    prometheus:
      enabled: true
  health:
    db:
      enabled: true
    diskspace:
      enabled: true
```

### 2.3 自定义指标配置

#### 2.3.1 MetricsConfig.java

项目配置类位于 [MetricsConfig.java](../backend/src/main/java/com/dormpower/config/MetricsConfig.java)：

```java
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
}
```

#### 2.3.2 ThreadPoolMetricsConfig.java

线程池监控配置位于 [ThreadPoolMetricsConfig.java](../backend/src/main/java/com/dormpower/config/ThreadPoolMetricsConfig.java)：

```java
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
}
```

### 2.4 Prometheus 配置

#### 2.4.1 prometheus.yml

配置文件位于 [prometheus/prometheus.yml](../prometheus/prometheus.yml)：

```yaml
# Dorm Power Prometheus Configuration
# Server: 2-core 2GB (JD Cloud)

global:
  scrape_interval: 15s       # 采集间隔
  evaluation_interval: 15s   # 规则评估间隔

scrape_configs:
  - job_name: 'dorm-power-backend'
    metrics_path: '/actuator/prometheus'  # Spring Boot 指标端点
    static_configs:
      - targets: ['backend:8000']         # 后端服务地址
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: 'dorm-power-backend'
```

### 2.5 Grafana 配置

#### 2.5.1 数据源配置

配置文件位于 [grafana/provisioning/datasources/datasources.yml](../grafana/provisioning/datasources/datasources.yml)：

```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
```

#### 2.5.2 Dashboard 配置

配置文件位于 [grafana/provisioning/dashboards/dashboards.yml](../grafana/provisioning/dashboards/dashboards.yml)：

```yaml
apiVersion: 1
providers:
  - name: 'default'
    folder: ''
    type: file
    options:
      path: /etc/grafana/provisioning/dashboards/json
```

#### 2.5.3 线程池 Dashboard

Dashboard 定义位于 [grafana/provisioning/dashboards/json/threadpool-dashboard.json](../grafana/provisioning/dashboards/json/threadpool-dashboard.json)：

```json
{
  "title": "Thread Pool Monitor",
  "panels": [
    {
      "title": "Active Threads",
      "type": "stat",
      "targets": [
        {
          "expr": "executor_active",
          "legendFormat": "{{name}}"
        }
      ]
    },
    {
      "title": "Queued Tasks",
      "type": "stat",
      "targets": [
        {
          "expr": "executor_queued",
          "legendFormat": "{{name}}"
        }
      ]
    },
    {
      "title": "Thread Pool Size Over Time",
      "type": "timeseries",
      "targets": [
        {
          "expr": "executor_pool_size",
          "legendFormat": "{{name}}"
        }
      ]
    }
  ]
}
```

### 2.6 Docker Compose 配置

在 [docker-compose.production.yml](../docker-compose.production.yml) 中配置：

```yaml
services:
  # Prometheus 监控
  prometheus:
    image: prom/prometheus:latest
    container_name: dorm-power-prometheus
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    ports:
      - "127.0.0.1:9090:9090"
    mem_limit: 128M
    memswap_limit: 128M
    restart: unless-stopped
    networks:
      - dorm-power-network

  # Grafana 可视化
  grafana:
    image: grafana/grafana:latest
    container_name: dorm-power-grafana
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD:-admin}
      - GF_USERS_ALLOW_SIGN_UP=false
      - GF_SERVER_ROOT_URL=%(protocol)s://%(domain)s:%(http_port)s/grafana/
      - GF_SERVER_SERVE_FROM_SUB_PATH=true
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning:ro
    ports:
      - "127.0.0.1:3001:3000"
    mem_limit: 128M
    memswap_limit: 128M
    restart: unless-stopped
    networks:
      - dorm-power-network
```

### 2.7 访问方式

| 服务 | 地址 | 说明 |
|------|------|------|
| Prometheus | `http://localhost:9090` | Prometheus UI |
| Grafana | `http://localhost:3001` | Grafana Dashboard |
| Actuator | `http://localhost:8000/actuator/prometheus` | 指标端点 |

---

## 三、面试要点

### 3.1 基础问题

#### Q1: 什么是 Prometheus？它的核心特性有哪些？

**回答要点**：

1. **定义**：Prometheus 是一个开源的系统监控和告警工具包，CNCF 毕业项目
2. **核心特性**：
   - 多维数据模型（指标名 + 标签）
   - PromQL 查询语言
   - Pull 模式采集
   - 服务发现
   - Alertmanager 告警管理

#### Q2: Prometheus 的四种指标类型是什么？各有什么用途？

**回答要点**：

| 类型 | 说明 | 适用场景 |
|------|------|----------|
| **Counter** | 只增不减的计数器 | 请求数、错误数、消息数 |
| **Gauge** | 可增可减的瞬时值 | 温度、内存、连接数 |
| **Histogram** | 直方图，统计分布 | 请求延迟分布 |
| **Summary** | 摘要，统计分位数 | P50、P95、P99 |

#### Q3: Prometheus 为什么采用 Pull 模式而不是 Push 模式？

**回答要点**：

**Pull 模式优势**：
1. 服务端控制采集频率
2. 易于发现目标是否存活
3. 无需客户端维护推送逻辑
4. 便于调试（直接访问指标端点）

**Push 模式适用场景**：
- 短生命周期任务（批处理作业）
- 防火墙后的服务

### 3.2 进阶问题

#### Q4: 项目中如何集成 Prometheus？

**回答要点**：

1. **依赖引入**：
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

2. **配置暴露端点**：
```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus
```

3. **自定义指标**：
```java
@Bean
public Counter webSocketMessagesSent(MeterRegistry registry) {
    return Counter.builder("websocket_messages_sent_total")
        .description("Total WebSocket messages sent")
        .register(registry);
}
```

4. **Prometheus 采集配置**：
```yaml
scrape_configs:
  - job_name: 'dorm-power-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8000']
```

#### Q5: 什么是高基数问题？如何避免？

**回答要点**：

**高基数问题**：标签值无限增长，导致时间序列数量爆炸

**示例**：
```java
// 错误示例：userId 有无限可能值
registry.counter("requests", "user_id", userId);

// 正确示例：使用有限标签
registry.counter("requests", "endpoint", "/api/users", "status", "200");
```

**解决方案**：
1. 避免使用高基数标签（userId、requestId）
2. 使用聚合减少维度
3. 配置指标过期
4. 使用 Histogram 替代 Summary

#### Q6: 如何监控线程池？

**回答要点**：

```java
@Configuration
public class ThreadPoolMetricsConfig {

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Bean
    public Gauge activeThreadsGauge(MeterRegistry registry) {
        return Gauge.builder("executor.active", 
                () -> taskExecutor.getActiveCount())
            .tag("name", "taskExecutor")
            .register(registry);
    }

    @Bean
    public Gauge queuedTasksGauge(MeterRegistry registry) {
        return Gauge.builder("executor.queued",
                () -> taskExecutor.getThreadPoolExecutor().getQueue().size())
            .tag("name", "taskExecutor")
            .register(registry);
    }
}
```

**监控指标**：
- 活跃线程数
- 队列任务数
- 池大小
- 完成任务数

### 3.3 场景问题

#### Q7: 项目中监控了哪些指标？

**回答要点**：

| 监控维度 | 指标 | 类型 |
|----------|------|------|
| **WebSocket** | websocket_connections_active | Gauge |
| **WebSocket** | websocket_messages_sent_total | Counter |
| **Kafka** | kafka_messages_sent_total | Counter |
| **MQTT** | mqtt_messages_received_total | Counter |
| **线程池** | executor_active | Gauge |
| **线程池** | executor_queued | Gauge |
| **数据库** | telemetry_write_duration | Timer |
| **Redis** | redis_command_duration | Timer |
| **限流** | rate_limit_exceeded_total | Counter |

#### Q8: 如何使用 PromQL 查询监控数据？

**回答要点**：

```promql
# 查询 WebSocket 连接数
websocket_connections_active

# 查询每秒消息发送速率
rate(websocket_messages_sent_total[5m])

# 查询线程池活跃线程数
executor_active{name="taskExecutor"}

# 查询 P95 延迟
histogram_quantile(0.95, telemetry_write_duration_seconds_bucket)

# 查询限流拒绝率
rate(rate_limit_exceeded_total[5m])
```

#### Q9: 如何配置 Grafana Dashboard？

**回答要点**：

1. **数据源配置**：
```yaml
datasources:
  - name: Prometheus
    type: prometheus
    url: http://prometheus:9090
```

2. **Dashboard 配置**：
```json
{
  "panels": [
    {
      "title": "Active Threads",
      "type": "stat",
      "targets": [
        {"expr": "executor_active"}
      ]
    }
  ]
}
```

3. **自动配置**：通过 provisioning 实现开箱即用

### 3.4 架构设计问题

#### Q10: Prometheus 与 Spring Boot Actuator 是什么关系？

**回答要点**：

```
Spring Boot Actuator
        │
        ▼
    Micrometer（门面）
        │
        ▼
PrometheusMeterRegistry（适配器）
        │
        ▼
/actuator/prometheus（端点）
        │
        ▼
   Prometheus（采集）
```

**关系说明**：
- Actuator 提供监控端点
- Micrometer 提供指标门面
- PrometheusMeterRegistry 适配 Prometheus 格式
- Prometheus 采集 `/actuator/prometheus` 端点

#### Q11: Prometheus 的数据存储原理是什么？

**回答要点**：

1. **TSDB（时序数据库）**：
   - 按时间分块存储
   - 内存中 Head Block + 磁盘持久化 Block
   - 支持压缩和保留策略

2. **存储格式**：
```
<metric_name>{<labels>} <value> [timestamp]
```

3. **保留策略**：
   - 默认保留 15 天
   - 可配置保留时间

#### Q12: 如何实现告警功能？

**回答要点**：

1. **配置告警规则**：
```yaml
groups:
  - name: dorm_power_alerts
    rules:
      - alert: ThreadPoolExhausted
        expr: executor_active / executor_pool_max > 0.9
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Thread pool nearly exhausted"
```

2. **配置 Alertmanager**：
```yaml
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']
```

3. **告警流程**：
```
Prometheus 评估规则 → 触发告警 → 发送到 Alertmanager → 
去重/分组/路由 → 发送通知（邮件/Slack/钉钉）
```

---

## 四、总结

### 4.1 项目使用总结

| 方面 | 实现方式 |
|------|----------|
| **依赖** | `micrometer-registry-prometheus` |
| **配置** | `MetricsConfig.java` + `ThreadPoolMetricsConfig.java` |
| **端点** | `/actuator/prometheus` |
| **采集** | Prometheus 每 15 秒 Pull |
| **可视化** | Grafana Dashboard |

### 4.2 核心价值

1. **全方位监控**：系统、线程池、WebSocket、消息队列全覆盖
2. **问题定位**：快速发现性能瓶颈和异常
3. **可视化展示**：Grafana Dashboard 直观展示
4. **轻量级部署**：128M 内存限制，适合小规模服务器

### 4.3 最佳实践

1. 合理设计指标名称和标签
2. 避免高基数标签
3. 配置合适的分位数
4. 定期清理过期数据
5. 结合 Grafana 实现可视化

---

**文档版本**：v1.0  
**编写日期**：2026年3月14日
