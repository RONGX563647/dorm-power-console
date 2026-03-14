# Prometheus 学习文档

## 一、基础功能

### 1.1 什么是 Prometheus？

Prometheus 是一个开源的系统监控和告警工具包，最初由 SoundCloud 开发，现在是 CNCF（云原生计算基金会）的毕业项目。

#### 核心特性

| 特性 | 说明 |
|------|------|
| **多维数据模型** | 时间序列数据由指标名称和键值对（标签）标识 |
| **PromQL** | 强大的查询语言，支持多维数据查询和聚合 |
| **Pull 模式** | 主动拉取指标数据，无需客户端推送 |
| **服务发现** | 自动发现监控目标，支持多种服务发现机制 |
| **告警管理** | 通过 Alertmanager 实现告警去重、分组、路由 |

#### 架构组件

```
┌─────────────────────────────────────────────────────────────────┐
│                    Prometheus 架构                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐         │
│  │ Exporter    │    │ Pushgateway │    │ 服务发现    │         │
│  │ (指标暴露)  │    │ (推送网关)  │    │ (SD)        │         │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘         │
│         │                  │                  │                 │
│         └──────────────────┼──────────────────┘                 │
│                            │ Pull                               │
│                            ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   Prometheus Server                       │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │   │
│  │  │ 检索模块    │  │ TSDB存储    │  │ HTTP Server │      │   │
│  │  │ (Retrieval) │  │ (时序数据库)│  │ (查询接口)  │      │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘      │   │
│  └─────────────────────────────────────────────────────────┘   │
│                            │                                    │
│         ┌──────────────────┼──────────────────┐                │
│         │                  │                  │                │
│         ▼                  ▼                  ▼                │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐         │
│  │ PromQL      │    │ Grafana     │    │ Alertmanager│         │
│  │ (查询)      │    │ (可视化)    │    │ (告警)      │         │
│  └─────────────┘    └─────────────┘    └─────────────┘         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 数据模型

#### 指标格式

```
<metric_name>{<label_name>=<label_value>, ...} <value> [timestamp]
```

**示例**：

```
http_requests_total{method="GET", status="200"} 1027
http_requests_total{method="POST", status="201"} 4
executor_active{name="taskExecutor"} 3
websocket_connections_active 5
```

#### 四种指标类型

| 类型 | 说明 | 示例 |
|------|------|------|
| **Counter** | 只增不减的计数器 | 请求数、错误数、消息发送数 |
| **Gauge** | 可增可减的瞬时值 | 温度、内存使用、活跃连接数 |
| **Histogram** | 直方图，统计分布 | 请求延迟分布、响应大小分布 |
| **Summary** | 摘要，统计分位数 | 请求延迟的 P50、P95、P99 |

### 1.3 Micrometer 集成

Spring Boot 通过 Micrometer 与 Prometheus 集成：

#### 依赖配置

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

#### application.yml 配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    prometheus:
      enabled: true
```

### 1.4 核心注解与 API

#### MeterRegistry

Micrometer 的核心接口，用于注册和管理指标：

```java
@Autowired
private MeterRegistry registry;
```

#### Counter（计数器）

```java
Counter counter = Counter.builder("http_requests_total")
    .description("Total HTTP requests")
    .tag("method", "GET")
    .tag("status", "200")
    .register(registry);

counter.increment();
counter.increment(5);
```

#### Gauge（瞬时值）

```java
Gauge.builder("jvm_memory_used", () -> memoryUsage)
    .description("JVM memory used")
    .tag("area", "heap")
    .register(registry);
```

#### Timer（计时器）

```java
Timer timer = Timer.builder("http_request_duration")
    .description("HTTP request duration")
    .publishPercentiles(0.5, 0.95, 0.99)
    .register(registry);

timer.record(() -> {
    // 执行业务逻辑
});

// 或手动记录
timer.record(durationMs, TimeUnit.MILLISECONDS);
```

---

## 二、场景使用

### 2.1 场景一：API 请求监控

```java
@RestController
public class ApiController {

    @Autowired
    private MeterRegistry registry;

    @GetMapping("/api/users")
    public List<User> getUsers() {
        Timer.Sample sample = Timer.start(registry);
        
        try {
            List<User> users = userService.findAll();
            
            // 记录成功请求
            registry.counter("api_requests_total",
                "endpoint", "/api/users",
                "method", "GET",
                "status", "200"
            ).increment();
            
            return users;
        } catch (Exception e) {
            // 记录失败请求
            registry.counter("api_requests_total",
                "endpoint", "/api/users",
                "method", "GET",
                "status", "500"
            ).increment();
            throw e;
        } finally {
            sample.stop(registry.timer("api_request_duration",
                "endpoint", "/api/users"));
        }
    }
}
```

### 2.2 场景二：线程池监控

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
            .description("Active threads in executor")
            .register(registry);
    }

    @Bean
    public Gauge queuedTasksGauge(MeterRegistry registry) {
        return Gauge.builder("executor.queued",
                () -> taskExecutor.getThreadPoolExecutor().getQueue().size())
            .tag("name", "taskExecutor")
            .description("Queued tasks in executor")
            .register(registry);
    }

    @Bean
    public Gauge poolSizeGauge(MeterRegistry registry) {
        return Gauge.builder("executor.pool.size",
                () -> taskExecutor.getPoolSize())
            .tag("name", "taskExecutor")
            .description("Current pool size")
            .register(registry);
    }
}
```

### 2.3 场景三：消息队列监控

```java
@Configuration
public class MetricsConfig {

    @Bean
    public Counter kafkaMessagesSent(MeterRegistry registry) {
        return Counter.builder("kafka_messages_sent_total")
            .description("Total Kafka messages sent")
            .tag("topic", "telemetry")
            .register(registry);
    }

    @Bean
    public Counter kafkaMessagesConsumed(MeterRegistry registry) {
        return Counter.builder("kafka_messages_consumed_total")
            .description("Total Kafka messages consumed")
            .tag("topic", "telemetry")
            .register(registry);
    }

    @Bean
    public Counter mqttMessagesReceived(MeterRegistry registry) {
        return Counter.builder("mqtt_messages_received_total")
            .description("Total MQTT messages received")
            .register(registry);
    }
}
```

### 2.4 场景四：WebSocket 连接监控

```java
@Configuration
public class MetricsConfig {

    @Autowired(required = false)
    private WebSocketManager webSocketManager;

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

    @Bean
    public Counter webSocketMessagesSent(MeterRegistry registry) {
        return Counter.builder("websocket_messages_sent_total")
            .description("Total WebSocket messages sent")
            .register(registry);
    }

    @Bean
    public Counter webSocketMessagesFailed(MeterRegistry registry) {
        return Counter.builder("websocket_messages_failed_total")
            .description("Total WebSocket messages failed to send")
            .register(registry);
    }
}
```

### 2.5 场景五：数据库操作监控

```java
@Configuration
public class MetricsConfig {

    @Bean
    public Timer telemetryWriteTimer(MeterRegistry registry) {
        return Timer.builder("telemetry_write_duration")
            .description("Time taken to write telemetry data")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }

    @Bean
    public Timer redisCommandTimer(MeterRegistry registry) {
        return Timer.builder("redis_command_duration")
            .description("Time taken for Redis commands")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }
}

// 使用示例
@Service
public class TelemetryService {

    @Autowired
    private Timer telemetryWriteTimer;

    public void saveTelemetry(Telemetry data) {
        telemetryWriteTimer.record(() -> {
            telemetryRepository.save(data);
        });
    }
}
```

### 2.6 场景六：限流监控

```java
@Configuration
public class MetricsConfig {

    @Bean
    public Counter rateLimitExceededCounter(MeterRegistry registry) {
        return Counter.builder("rate_limit_exceeded_total")
            .description("Total requests rejected by rate limiter")
            .register(registry);
    }
}

// 使用示例
@Aspect
@Component
public class RateLimitAspect {

    @Autowired
    private Counter rateLimitExceededCounter;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        if (!rateLimiter.tryAcquire()) {
            rateLimitExceededCounter.increment();
            throw new RateLimitExceededException("请求过于频繁");
        }
        return joinPoint.proceed();
    }
}
```

---

## 三、原理分析

### 3.1 Prometheus 数据采集原理

#### Pull 模式

```
┌─────────────────────────────────────────────────────────────────┐
│                     Prometheus Pull 模式                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Prometheus Server              应用服务                        │
│  ┌─────────────┐               ┌─────────────┐                 │
│  │             │   HTTP GET    │             │                 │
│  │  Scraper    │──────────────>│  /metrics   │                 │
│  │  (采集器)   │   每15秒      │  (指标端点) │                 │
│  │             │<──────────────│             │                 │
│  └─────────────┘   指标数据    └─────────────┘                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**优势**：
- 服务端控制采集频率
- 易于发现目标是否存活
- 无需客户端维护推送逻辑

### 3.2 Spring Boot Actuator 集成原理

```
┌─────────────────────────────────────────────────────────────────┐
│                Spring Boot Actuator 架构                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Spring Boot 应用                                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Micrometer                            │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │   │
│  │  │ Counter     │  │ Gauge       │  │ Timer       │      │   │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘      │   │
│  │         │                │                │              │   │
│  │         └────────────────┼────────────────┘              │   │
│  │                          │                               │   │
│  │                          ▼                               │   │
│  │                 ┌─────────────┐                          │   │
│  │                 │MeterRegistry│                          │   │
│  │                 └──────┬──────┘                          │   │
│  └────────────────────────┼────────────────────────────────┘   │
│                           │                                    │
│                           ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              PrometheusMeterRegistry                     │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │          Prometheus 端点                         │    │   │
│  │  │          /actuator/prometheus                    │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.3 指标类型实现原理

#### Counter 实现

```java
public class PrometheusCounter implements Counter {
    private final DoubleAdder value = new DoubleAdder();
    
    @Override
    public void increment(double amount) {
        value.add(amount);
    }
    
    @Override
    public double count() {
        return value.sum();
    }
}
```

**特点**：
- 使用 `DoubleAdder` 保证线程安全
- 只增不减，适合累计值
- 重启后归零

#### Gauge 实现

```java
public class PrometheusGauge implements Gauge {
    private final Supplier<Number> valueSupplier;
    
    @Override
    public double value() {
        Number value = valueSupplier.get();
        return value != null ? value.doubleValue() : Double.NaN;
    }
}
```

**特点**：
- 通过 Supplier 获取实时值
- 可增可减
- 适合瞬时状态

#### Timer 实现

```java
public class PrometheusTimer implements Timer {
    private final Histogram histogram;
    
    @Override
    public void record(long amount, TimeUnit unit) {
        histogram.observe(unit.toSeconds(amount));
    }
    
    @Override
    public HistogramSnapshot takeSnapshot() {
        return histogram.getSnapshot();
    }
}
```

**特点**：
- 内部使用 Histogram 统计分布
- 支持分位数计算（P50、P95、P99）
- 适合延迟、耗时统计

### 3.4 Prometheus 配置详解

```yaml
# prometheus.yml
global:
  scrape_interval: 15s       # 全局采集间隔
  evaluation_interval: 15s   # 规则评估间隔

scrape_configs:
  - job_name: 'dorm-power-backend'
    metrics_path: '/actuator/prometheus'  # 指标端点路径
    static_configs:
      - targets: ['backend:8000']         # 监控目标
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: 'dorm-power-backend'
```

### 3.5 数据存储原理

```
┌─────────────────────────────────────────────────────────────────┐
│                    Prometheus TSDB 存储                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  内存中                                                         │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Head Block                            │   │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐                 │   │
│  │  │ Chunk 1 │  │ Chunk 2 │  │ Chunk 3 │  ...            │   │
│  │  │ (2h)    │  │ (2h)    │  │ (2h)    │                 │   │
│  │  └─────────┘  └─────────┘  └─────────┘                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           │                                    │
│                           │ 持久化                             │
│                           ▼                                    │
│  磁盘中                                                         │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                  Persistent Blocks                       │   │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐                 │   │
│  │  │ Block 1 │  │ Block 2 │  │ Block 3 │  ...            │   │
│  │  │ (2h)    │  │ (2h)    │  │ (2h)    │                 │   │
│  │  └─────────┘  └─────────┘  └─────────┘                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**存储特点**：
- 时间序列数据库（TSDB）
- 按时间分块存储
- 支持压缩和保留策略
- 高效的范围查询

### 3.6 PromQL 查询语言

#### 基础查询

```promql
# 查询指标
http_requests_total

# 按标签过滤
http_requests_total{method="GET"}
http_requests_total{status=~"2.."}  # 正则匹配

# 范围查询
http_requests_total[5m]
```

#### 聚合操作

```promql
# 求和
sum(http_requests_total)

# 按标签分组求和
sum by (method) (http_requests_total)

# 平均值
avg(http_request_duration_seconds)

# 最大/最小值
max(http_request_duration_seconds)
min(http_request_duration_seconds)
```

#### 函数操作

```promql
# 速率（每秒增量）
rate(http_requests_total[5m])

# 增量
increase(http_requests_total[1h])

# 分位数
histogram_quantile(0.95, http_request_duration_seconds_bucket)
```

---

## 四、最佳实践

### 4.1 指标命名规范

```
<namespace>_<name>_<unit>_<type>
```

**示例**：
- `http_requests_total` - HTTP 请求总数
- `http_request_duration_seconds` - HTTP 请求延迟（秒）
- `jvm_memory_used_bytes` - JVM 内存使用（字节）
- `executor_active_threads` - 活跃线程数

### 4.2 标签设计原则

1. **避免高基数标签**：不要使用 userId、requestId 等无限增长的值
2. **标签要有意义**：便于聚合和过滤
3. **保持一致性**：相同指标使用相同的标签名

```java
// 好的设计
registry.counter("http_requests_total",
    "method", "GET",
    "status", "200",
    "endpoint", "/api/users"
);

// 不好的设计（高基数）
registry.counter("http_requests_total",
    "user_id", userId,  // 避免使用
    "request_id", uuid  // 避免使用
);
```

### 4.3 分位数配置

```java
Timer.builder("http_request_duration")
    .publishPercentiles(0.5, 0.95, 0.99)  // P50, P95, P99
    .publishPercentileHistogram()          // 发布直方图桶
    .minimumExpectedValue(Duration.ofMillis(1))
    .maximumExpectedValue(Duration.ofSeconds(10))
    .register(registry);
```

### 4.4 监控告警规则

```yaml
# alert.rules.yml
groups:
  - name: dorm_power_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} per second"

      - alert: ThreadPoolExhausted
        expr: executor_active / executor_pool_max > 0.9
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Thread pool nearly exhausted"
```

---

## 五、常见问题

### Q1: Prometheus 与其他监控系统的区别？

| 特性 | Prometheus | InfluxDB | Zabbix |
|------|------------|----------|--------|
| 数据模型 | 多维标签 | 标签+字段 | 键值对 |
| 采集方式 | Pull | Push/Pull | Agent |
| 查询语言 | PromQL | InfluxQL/Flux | 自定义 |
| 适用场景 | 云原生、微服务 | IoT、时序数据 | 传统监控 |

### Q2: 如何处理高基数问题？

1. **避免使用高基数标签**
2. **使用聚合减少维度**
3. **配置指标过期**
4. **使用 Histogram 替代 Summary**

### Q3: Counter 重启后归零怎么办？

使用 `rate()` 或 `increase()` 函数计算增量：

```promql
# 每秒请求速率
rate(http_requests_total[5m])

# 每小时增量
increase(http_requests_total[1h])
```

### Q4: 如何监控 Prometheus 自身？

Prometheus 暴露自身指标：

```yaml
scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

关键指标：
- `prometheus_tsdb_head_series` - 内存中的时间序列数
- `prometheus_scrape_duration_seconds` - 采集延迟
- `prometheus_target_sync_length_seconds` - 目标同步时间

---

## 六、总结

Prometheus 是云原生监控的事实标准：

| 优势 | 说明 |
|------|------|
| **多维数据模型** | 灵活的标签系统，支持复杂查询 |
| **Pull 模式** | 服务端控制采集，易于管理 |
| **PromQL** | 强大的查询语言，支持聚合和函数 |
| **生态完善** | 与 Grafana、Alertmanager 无缝集成 |
| **云原生** | CNCF 毕业项目，Kubernetes 默认监控方案 |

---

**文档版本**：v1.0  
**编写日期**：2026年3月14日
