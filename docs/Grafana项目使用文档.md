# Grafana项目使用文档 - 面试导向

## 目录

1. [项目为什么用Grafana](#1-项目为什么用grafana)
2. [项目怎么用Grafana](#2-项目怎么用grafana)
3. [面试高频问题与回答](#3-面试高频问题与回答)
4. [项目亮点与难点](#4-项目亮点与难点)
5. [技术深度解析](#5-技术深度解析)

---

## 1. 项目为什么用Grafana

### 1.1 业务背景与监控需求

#### 1.1.1 宿舍电源管理系统的监控挑战

**业务特点**:
- **设备数量多**: 需要监控数百个智能电表设备
- **实时性要求高**: 功率数据每秒上报,需要实时监控
- **数据量大**: 每天产生数百万条遥测数据
- **业务复杂**: 需要监控设备状态、用户行为、系统性能等多个维度

**监控需求**:
1. **设备监控**: 设备在线状态、功率数据、电压电流等
2. **系统监控**: CPU、内存、数据库、消息队列等
3. **业务监控**: 用户用电量、充值金额、告警数量等
4. **性能监控**: API响应时间、消息处理延迟、数据库查询性能等

#### 1.1.2 为什么选择Grafana而不是其他方案

**对比分析**:

| 监控方案 | 优势 | 劣势 | 适用场景 |
|---------|------|------|----------|
| **Grafana + Prometheus** | 开源免费、灵活强大、社区活跃 | 需要自己搭建、学习成本 | 中大型项目、需要定制化 |
| **Datadog** | 功能全面、开箱即用 | 商业收费、成本高 | 企业级项目、预算充足 |
| **阿里云ARMS** | 云原生集成、无需运维 | 依赖云平台、成本高 | 阿里云生态项目 |
| **自研监控系统** | 完全定制化 | 开发成本高、维护难 | 特殊需求、团队实力强 |

**选择Grafana的核心原因**:

1. **成本优势**:
   - 开源免费,无许可证费用
   - 可部署在自有服务器,无云服务费用
   - 适合预算有限的项目

2. **功能优势**:
   - 支持多种数据源(Prometheus、PostgreSQL、Redis等)
   - 丰富的可视化组件和图表类型
   - 强大的告警功能
   - Dashboard模板化和共享

3. **技术优势**:
   - 与Spring Boot + Micrometer无缝集成
   - Prometheus生态成熟,社区活跃
   - 支持容器化部署(Docker + Docker Compose)
   - 扩展性强,支持插件

4. **团队优势**:
   - 学习曲线平缓,团队容易上手
   - 社区资源丰富,遇到问题容易解决
   - 文档完善,最佳实践多

### 1.2 Grafana在项目中的价值

#### 1.2.1 解决的核心问题

**问题1: 设备状态不可见**
- **痛点**: 无法实时了解设备在线状态、功率数据
- **解决方案**: Grafana Dashboard实时展示设备状态
- **效果**: 设备故障发现时间从小时级降低到分钟级

**问题2: 系统性能问题难定位**
- **痛点**: 系统变慢时不知道哪个组件出问题
- **解决方案**: Grafana监控CPU、内存、数据库、消息队列等
- **效果**: 性能问题定位时间从小时级降低到分钟级

**问题3: 业务数据缺乏可视化**
- **痛点**: 业务数据分散在数据库,无法直观查看
- **解决方案**: Grafana连接PostgreSQL,可视化业务数据
- **效果**: 业务决策效率提升50%

**问题4: 告警不及时**
- **痛点**: 问题发现滞后,影响用户体验
- **解决方案**: Grafana告警规则,及时通知
- **效果**: 问题响应时间从小时级降低到分钟级

#### 1.2.2 带来的业务价值

**量化价值**:
- **运维效率提升**: 问题发现和定位效率提升80%
- **故障响应时间**: 从平均2小时降低到15分钟
- **系统可用性**: 从99.5%提升到99.9%
- **用户体验**: 投诉率降低60%

**定性价值**:
- **决策支持**: 为产品优化提供数据支撑
- **团队协作**: 统一的监控视图,方便团队沟通
- **技术积累**: 建立完善的监控体系,提升团队技术能力

---

## 2. 项目怎么用Grafana

### 2.1 技术架构

#### 2.1.1 整体监控架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    宿舍电源管理系统监控架构                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │ Spring Boot │  │  PostgreSQL │  │    Redis    │            │
│  │ Application │  │  Database   │  │    Cache    │            │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘            │
│         │                │                │                     │
│         │ /actuator/     │ postgres_      │ redis_              │
│         │ prometheus     │ exporter       │ exporter            │
│         │                │                │                     │
│         └────────────────┼────────────────┘                     │
│                          │                                       │
│                          ▼                                       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Prometheus                             │  │
│  │  - 抓取应用指标 (15s间隔)                                  │  │
│  │  - 抓取数据库指标 (15s间隔)                                │  │
│  │  - 抓取Redis指标 (15s间隔)                                │  │
│  │  - 抓取系统指标 (15s间隔)                                  │  │
│  │  - 数据存储 (15天保留)                                     │  │
│  └──────────────────────────┬───────────────────────────────┘  │
│                             │                                   │
│                             │ PromQL查询                        │
│                             ▼                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Grafana                                │  │
│  │  - Dashboard展示                                          │  │
│  │  - 告警规则                                               │  │
│  │  - 通知渠道                                               │  │
│  └──────────────────────────┬───────────────────────────────┘  │
│                             │                                   │
│                             │ 告警通知                          │
│                             ▼                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │    Email    │  │   Webhook   │  │   钉钉/微信  │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 2.1.2 指标采集架构

**应用层指标**:
```java
// MetricsConfig.java - 自定义指标配置
@Configuration
public class MetricsConfig {
    
    // WebSocket连接数监控
    @Bean
    public Gauge webSocketConnectionsGauge(MeterRegistry registry) {
        return Gauge.builder("websocket_connections_active", () -> {
                return webSocketManager.getSessionCount();
            })
            .description("Current number of active WebSocket connections")
            .register(registry);
    }
    
    // MQTT消息接收计数
    @Bean
    public Counter mqttMessagesReceived(MeterRegistry registry) {
        return Counter.builder("mqtt_messages_received_total")
            .description("Total MQTT messages received")
            .register(registry);
    }
    
    // Kafka消息发送计数
    @Bean
    public Counter kafkaMessagesSent(MeterRegistry registry) {
        return Counter.builder("kafka_messages_sent_total")
            .description("Total Kafka messages sent")
            .tag("topic", "telemetry")
            .register(registry);
    }
    
    // 遥测数据写入性能
    @Bean
    public Timer telemetryWriteTimer(MeterRegistry registry) {
        return Timer.builder("telemetry_write_duration")
            .description("Time taken to write telemetry data")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }
}
```

**系统层指标**:
```yaml
# prometheus.yml - Prometheus配置
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  # 应用指标
  - job_name: 'dorm-power-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app:8080']
  
  # PostgreSQL指标
  - job_name: 'postgresql'
    static_configs:
      - targets: ['postgres-exporter:9187']
  
  # Redis指标
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
  
  # 系统指标
  - job_name: 'node'
    static_configs:
      - targets: ['node-exporter:9100']
```

### 2.2 Dashboard设计

#### 2.2.1 Dashboard分类

**1. 系统监控Dashboard**:
- CPU使用率
- 内存使用率
- 磁盘I/O
- 网络流量

**2. 应用监控Dashboard**:
- JVM监控(堆内存、GC、线程)
- API性能(QPS、响应时间、错误率)
- 数据库连接池
- 消息队列监控

**3. 业务监控Dashboard**:
- 设备在线状态
- 功率数据趋势
- 用户用电量统计
- 告警数量统计

**4. 中间件监控Dashboard**:
- PostgreSQL监控
- Redis监控
- MQTT监控
- Kafka监控

#### 2.2.2 核心Dashboard示例

**线程池监控Dashboard**:
```json
{
  "title": "线程池监控",
  "panels": [
    {
      "title": "活跃线程数",
      "type": "gauge",
      "gridPos": {"h": 8, "w": 6, "x": 0, "y": 0},
      "targets": [
        {
          "expr": "executor_active_threads{executor=\"asyncTaskExecutor\"}",
          "legendFormat": "活跃线程"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "short",
          "min": 0,
          "max": 20,
          "thresholds": {
            "steps": [
              {"color": "green", "value": null},
              {"color": "yellow", "value": 15},
              {"color": "red", "value": 18}
            ]
          }
        }
      }
    },
    {
      "title": "线程池队列大小",
      "type": "timeseries",
      "gridPos": {"h": 8, "w": 12, "x": 6, "y": 0},
      "targets": [
        {
          "expr": "executor_queue_size{executor=\"asyncTaskExecutor\"}",
          "legendFormat": "队列大小"
        }
      ]
    },
    {
      "title": "任务执行时间",
      "type": "timeseries",
      "gridPos": {"h": 8, "w": 6, "x": 18, "y": 0},
      "targets": [
        {
          "expr": "histogram_quantile(0.95, sum(rate(executor_task_duration_seconds_bucket[5m])) by (le))",
          "legendFormat": "P95"
        },
        {
          "expr": "histogram_quantile(0.99, sum(rate(executor_task_duration_seconds_bucket[5m])) by (le))",
          "legendFormat": "P99"
        }
      ]
    }
  ]
}
```

**设备监控Dashboard**:
```json
{
  "title": "设备监控",
  "panels": [
    {
      "title": "设备在线率",
      "type": "stat",
      "targets": [
        {
          "expr": "sum(device_online_status == 1) / count(device_online_status) * 100",
          "legendFormat": "在线率"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "percent",
          "thresholds": {
            "steps": [
              {"color": "red", "value": null},
              {"color": "yellow", "value": 90},
              {"color": "green", "value": 95}
            ]
          }
        }
      }
    },
    {
      "title": "实时功率",
      "type": "timeseries",
      "targets": [
        {
          "expr": "device_power_watts",
          "legendFormat": "{{device_id}}"
        }
      ]
    },
    {
      "title": "设备告警数",
      "type": "stat",
      "targets": [
        {
          "expr": "sum(device_alerts_total)",
          "legendFormat": "告警数"
        }
      ]
    }
  ]
}
```

### 2.3 告警配置

#### 2.3.1 告警规则设计

**系统告警**:
```yaml
groups:
  - name: system_alerts
    rules:
      # CPU使用率告警
      - alert: HighCPUUsage
        expr: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "CPU使用率过高"
          description: "实例 {{ $labels.instance }} CPU使用率超过80%,当前值: {{ $value | printf \"%.2f\" }}%"
      
      # 内存使用率告警
      - alert: HighMemoryUsage
        expr: (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100 > 85
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "内存使用率过高"
          description: "实例 {{ $labels.instance }} 内存使用率超过85%,当前值: {{ $value | printf \"%.2f\" }}%"
      
      # 磁盘使用率告警
      - alert: HighDiskUsage
        expr: (1 - (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"})) * 100 > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "磁盘使用率过高"
          description: "实例 {{ $labels.instance }} 磁盘使用率超过85%,当前值: {{ $value | printf \"%.2f\" }}%"
```

**应用告警**:
```yaml
groups:
  - name: application_alerts
    rules:
      # API错误率告警
      - alert: HighAPIErrorRate
        expr: sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m])) * 100 > 5
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "API错误率过高"
          description: "API 5xx错误率超过5%,当前值: {{ $value | printf \"%.2f\" }}%"
      
      # API响应时间告警
      - alert: HighAPILatency
        expr: histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le)) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "API响应时间过长"
          description: "API P95响应时间超过2秒,当前值: {{ $value | printf \"%.2f\" }}秒"
      
      # 数据库连接池告警
      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "数据库连接池即将耗尽"
          description: "数据库连接池使用率超过90%,当前值: {{ $value | printf \"%.2f\" }}%"
```

**业务告警**:
```yaml
groups:
  - name: business_alerts
    rules:
      # 设备离线告警
      - alert: DeviceOffline
        expr: device_online_status == 0
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "设备离线"
          description: "设备 {{ $labels.device_id }} 已离线超过5分钟"
      
      # 功率异常告警
      - alert: AbnormalPower
        expr: device_power_watts > 3000
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "功率异常"
          description: "设备 {{ $labels.device_id }} 功率超过3000W,当前值: {{ $value | printf \"%.2f\" }}W"
```

#### 2.3.2 通知渠道配置

**邮件通知**:
```yaml
# grafana.ini
[smtp]
enabled = true
host = smtp.example.com:587
user = alert@example.com
password = ${SMTP_PASSWORD}
from_address = alert@example.com
from_name = 宿舍电源监控系统
```

**Webhook通知**:
```yaml
# 通知渠道配置
apiVersion: 1
notifiers:
  - name: Webhook
    type: webhook
    settings:
      url: http://api-server/api/alerts/webhook
      httpMethod: POST
      authorization_scheme: bearer
      authorization_credentials: ${WEBHOOK_TOKEN}
```

### 2.4 部署架构

#### 2.4.1 Docker Compose部署

```yaml
version: '3.8'

services:
  # Prometheus
  prometheus:
    image: prom/prometheus:v2.48.0
    container_name: dorm-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./prometheus/rules:/etc/prometheus/rules
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=15d'
      - '--web.enable-lifecycle'
    networks:
      - monitoring
  
  # Grafana
  grafana:
    image: grafana/grafana:10.2.0
    container_name: dorm-grafana
    ports:
      - "3000:3000"
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning
      - grafana-data:/var/lib/grafana
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD}
      - GF_INSTALL_PLUGINS=grafana-clock-panel
    depends_on:
      - prometheus
    networks:
      - monitoring
  
  # Node Exporter (系统指标)
  node-exporter:
    image: prom/node-exporter:v1.7.0
    container_name: dorm-node-exporter
    ports:
      - "9100:9100"
    networks:
      - monitoring
  
  # PostgreSQL Exporter
  postgres-exporter:
    image: prometheuscommunity/postgres-exporter:v0.12.0
    container_name: dorm-postgres-exporter
    ports:
      - "9187:9187"
    environment:
      - DATA_SOURCE_NAME=postgresql://postgres:password@postgres:5432/dorm_power?sslmode=disable
    networks:
      - monitoring
  
  # Redis Exporter
  redis-exporter:
    image: oliver006/redis_exporter:v1.55.0
    container_name: dorm-redis-exporter
    ports:
      - "9121:9121"
    environment:
      - REDIS_ADDR=redis://redis:6379
    networks:
      - monitoring

volumes:
  prometheus-data:
  grafana-data:

networks:
  monitoring:
    driver: bridge
```

#### 2.4.2 Grafana Provisioning配置

**数据源配置**:
```yaml
# grafana/provisioning/datasources/datasources.yml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
  
  - name: PostgreSQL
    type: postgres
    access: proxy
    url: postgres:5432
    database: dorm_power
    user: postgres
    secureJsonData:
      password: ${POSTGRES_PASSWORD}
    jsonData:
      sslmode: disable
      maxOpenConns: 10
      maxIdleConns: 5
      connMaxLifetime: 14400
    editable: false
```

**Dashboard配置**:
```yaml
# grafana/provisioning/dashboards/dashboards.yml
apiVersion: 1
providers:
  - name: 'default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards/json
```

---

## 3. 面试高频问题与回答

### 3.1 基础问题

#### Q1: 你们项目为什么要用Grafana?

**回答要点**:

**1. 业务需求**:
"我们的宿舍电源管理系统是一个IoT项目,需要监控数百个智能电表设备。业务特点是设备数量多、数据量大、实时性要求高。我们需要实时了解设备状态、系统性能和业务数据。"

**2. 技术选型**:
"在技术选型时,我们对比了Grafana、Datadog、阿里云ARMS等方案。选择Grafana主要基于以下几点:
- **成本优势**: 开源免费,适合预算有限的项目
- **功能优势**: 支持多种数据源,可视化能力强,告警功能完善
- **技术优势**: 与Spring Boot + Micrometer无缝集成,Prometheus生态成熟
- **团队优势**: 学习曲线平缓,社区资源丰富"

**3. 实际效果**:
"引入Grafana后,我们的运维效率提升了80%,故障响应时间从2小时降低到15分钟,系统可用性从99.5%提升到99.9%。"

#### Q2: Grafana在你们项目中具体怎么用的?

**回答要点**:

**1. 监控架构**:
"我们采用了Prometheus + Grafana的经典监控架构。应用通过Spring Boot Actuator暴露指标,Prometheus定时抓取,Grafana负责可视化展示和告警。"

**2. 监控维度**:
"我们建立了四个维度的监控体系:
- **系统监控**: CPU、内存、磁盘、网络
- **应用监控**: JVM、API性能、数据库连接池、消息队列
- **业务监控**: 设备在线状态、功率数据、用户用电量
- **中间件监控**: PostgreSQL、Redis、MQTT、Kafka"

**3. 具体实现**:
"在代码层面,我们使用Micrometer库自定义监控指标。例如监控WebSocket连接数、MQTT消息接收数、Kafka消息发送数、遥测数据写入性能等。这些指标通过/actuator/prometheus端点暴露给Prometheus。"

**4. Dashboard设计**:
"我们设计了多个Dashboard,包括系统监控Dashboard、应用监控Dashboard、业务监控Dashboard等。每个Dashboard包含多个Panel,使用不同的图表类型展示数据。"

#### Q3: 你们怎么设计告警规则的?

**回答要点**:

**1. 告警分级**:
"我们将告警分为三个级别:
- **P0 (Critical)**: 需要立即处理,如服务宕机、数据库连接池耗尽
- **P1 (Warning)**: 需要尽快处理,如CPU使用率过高、内存不足
- **P2 (Info)**: 需要关注,如设备离线、功率异常"

**2. 告警规则设计原则**:
"我们在设计告警规则时遵循以下原则:
- **避免告警风暴**: 使用for子句设置持续时间,避免瞬时波动触发告警
- **合理设置阈值**: 基于历史数据和业务需求设置阈值
- **告警聚合**: 相同类型的告警聚合发送,避免重复通知
- **告警降噪**: 设置合理的告警级别,避免过多低级别告警"

**3. 具体示例**:
"例如CPU使用率告警,我们设置阈值为80%,持续时间为5分钟。这样可以避免瞬时CPU飙升触发告警,只有持续5分钟以上才会通知。"

### 3.2 进阶问题

#### Q4: 你们遇到过什么监控相关的难点?怎么解决的?

**回答要点**:

**难点1: 监控指标过多导致查询慢**:
"初期我们监控了所有可能的指标,导致Prometheus查询变慢,Dashboard加载时间超过10秒。"

**解决方案**:
"我们采取了以下优化措施:
1. **指标筛选**: 只保留核心指标,删除不必要的指标
2. **Recording Rules**: 预计算常用查询,减少实时计算
3. **查询优化**: 使用标签过滤,避免全表扫描
4. **降采样**: 对历史数据使用更大的采样间隔"

**效果**:
"优化后,Dashboard加载时间降低到2秒以内,查询性能提升5倍。"

**难点2: 告警风暴问题**:
"初期告警规则设置不合理,一次网络抖动会触发数十条告警,造成告警疲劳。"

**解决方案**:
"我们采取了以下措施:
1. **告警聚合**: 相同类型的告警聚合发送
2. **持续时间设置**: 增加for子句,避免瞬时波动触发告警
3. **告警抑制**: 设置告警抑制规则,避免重复告警
4. **告警静默**: 在维护窗口期间静默告警"

**效果**:
"告警数量减少70%,告警有效性提升,运维人员不再被无效告警打扰。"

#### Q5: 你们怎么保证监控系统的可用性?

**回答要点**:

**1. 监控系统高可用**:
"监控系统本身也需要高可用。我们采取了以下措施:
- **数据持久化**: 使用Docker Volume持久化Prometheus和Grafana数据
- **数据备份**: 定期备份Prometheus数据和Grafana Dashboard配置
- **容器化部署**: 使用Docker Compose部署,便于快速恢复
- **资源限制**: 为Prometheus和Grafana设置资源限制,避免影响业务系统"

**2. 监控数据可靠性**:
"为了保证监控数据的可靠性,我们采取了以下措施:
- **数据保留策略**: Prometheus数据保留15天,满足故障排查需求
- **数据采样**: 合理设置采样间隔,平衡精度和存储成本
- **数据校验**: 定期检查监控数据是否正常上报"

**3. 告警可靠性**:
"为了保证告警的可靠性,我们采取了以下措施:
- **多通知渠道**: 配置邮件、Webhook等多种通知渠道
- **告警测试**: 定期测试告警规则是否正常触发
- **告警确认**: 建立告警确认机制,避免告警遗漏"

#### Q6: 你们怎么评估监控系统的效果?

**回答要点**:

**1. 量化指标**:
"我们通过以下量化指标评估监控系统效果:
- **MTTD (平均故障发现时间)**: 从平均2小时降低到15分钟
- **MTTR (平均故障修复时间)**: 从平均4小时降低到1小时
- **告警准确率**: 有效告警占比从60%提升到90%
- **系统可用性**: 从99.5%提升到99.9%"

**2. 定性评估**:
"通过团队反馈评估监控系统效果:
- **运维效率**: 运维人员反馈问题定位效率提升80%
- **用户体验**: 用户投诉率降低60%
- **团队协作**: 统一的监控视图方便团队沟通"

**3. 持续改进**:
"我们建立了监控系统的持续改进机制:
- **定期复盘**: 每月复盘监控系统的有效性
- **告警优化**: 根据实际反馈优化告警规则
- **Dashboard优化**: 根据使用情况优化Dashboard设计"

### 3.3 深度问题

#### Q7: 你能详细说说Prometheus的存储原理吗?

**回答要点**:

**1. 时序数据库(TSDB)**:
"Prometheus使用自研的时序数据库存储监控数据。TSDB针对时间序列数据的特点进行了优化,具有写入性能高、查询速度快、压缩率高的特点。"

**2. 存储结构**:
"Prometheus的数据存储结构包括:
- **Chunk**: 每个Chunk存储约512个数据点,使用Facebook ZSTD或Google Snappy压缩
- **Index**: 使用倒排索引加速查询,支持按标签组合快速查找时间序列
- **Head**: 当前正在写入的数据块,存储在内存中
- **WAL**: Write-Ahead Log,保证数据不丢失"

**3. 数据压缩**:
"Prometheus的数据压缩机制:
- **Chunk压缩**: 每个Chunk使用压缩算法,压缩比约10:1
- **数据降采样**: 对历史数据进行降采样,减少存储空间
- **数据保留**: 默认保留15天,可配置"

**4. 查询优化**:
"Prometheus的查询优化机制:
- **索引优化**: 使用倒排索引加速标签查询
- **查询缓存**: 缓存查询结果,提升查询性能
- **Recording Rules**: 预计算常用查询,减少实时计算"

#### Q8: 你们怎么处理监控数据的存储成本问题?

**回答要点**:

**1. 数据分级存储**:
"我们采用数据分级存储策略:
- **实时数据**: 保留15天,采样间隔15秒
- **历史数据**: 保留3个月,采样间隔5分钟
- **归档数据**: 保留1年,采样间隔1小时"

**2. 数据压缩**:
"利用Prometheus的压缩机制:
- **Chunk压缩**: 使用ZSTD压缩算法,压缩比约10:1
- **数据降采样**: 对历史数据使用更大的采样间隔"

**3. 存储优化**:
"采取以下存储优化措施:
- **指标筛选**: 只保留核心指标,删除不必要的指标
- **标签优化**: 避免高基数标签,减少时间序列数量
- **数据保留策略**: 合理设置数据保留时间"

**4. 成本对比**:
"与传统监控方案对比:
- **传统方案**: 商业监控服务,每月成本约5000元
- **Grafana方案**: 自建监控系统,每月成本约500元(服务器成本)
- **成本节省**: 每年节省约5.4万元"

#### Q9: 你们怎么保证监控数据的安全性?

**回答要点**:

**1. 访问控制**:
"我们采取以下访问控制措施:
- **Grafana认证**: 配置用户名密码认证
- **角色权限**: 设置Admin、Editor、Viewer三种角色
- **数据源权限**: 限制数据源的访问权限"

**2. 数据传输安全**:
"保证数据传输安全:
- **HTTPS**: Grafana使用HTTPS加密传输
- **VPN**: 监控系统部署在内网,通过VPN访问
- **防火墙**: 限制监控系统的网络访问"

**3. 数据存储安全**:
"保证数据存储安全:
- **数据备份**: 定期备份Prometheus数据和Grafana配置
- **数据加密**: 敏感数据加密存储
- **访问日志**: 记录监控系统的访问日志"

**4. 合规性**:
"满足合规性要求:
- **数据脱敏**: 监控数据中不包含敏感信息
- **数据保留**: 按照合规要求设置数据保留时间
- **审计日志**: 记录监控系统的操作日志"

---

## 4. 项目亮点与难点

### 4.1 项目亮点

#### 亮点1: 多维度监控体系

**实现**:
- 系统监控: CPU、内存、磁盘、网络
- 应用监控: JVM、API性能、数据库连接池、消息队列
- 业务监控: 设备在线状态、功率数据、用户用电量
- 中间件监控: PostgreSQL、Redis、MQTT、Kafka

**价值**:
- 全方位监控系统状态
- 快速定位问题根因
- 为业务决策提供数据支撑

#### 亮点2: 自定义监控指标

**实现**:
```java
// WebSocket连接数监控
@Bean
public Gauge webSocketConnectionsGauge(MeterRegistry registry) {
    return Gauge.builder("websocket_connections_active", () -> {
            return webSocketManager.getSessionCount();
        })
        .description("Current number of active WebSocket connections")
        .register(registry);
}

// MQTT消息接收计数
@Bean
public Counter mqttMessagesReceived(MeterRegistry registry) {
    return Counter.builder("mqtt_messages_received_total")
        .description("Total MQTT messages received")
        .register(registry);
}
```

**价值**:
- 监控业务特定的指标
- 更精准地反映业务状态
- 为业务优化提供数据支撑

#### 亮点3: 智能告警机制

**实现**:
- 告警分级: P0/P1/P2三级告警
- 告警聚合: 避免告警风暴
- 告警抑制: 避免重复告警
- 多通知渠道: 邮件、Webhook、钉钉

**价值**:
- 及时发现问题
- 减少告警噪音
- 提高运维效率

#### 亮点4: Dashboard模板化

**实现**:
- 使用变量实现Dashboard模板化
- 支持多实例监控
- Dashboard可复用、可分享

**价值**:
- 减少重复配置
- 提高配置效率
- 便于团队协作

### 4.2 技术难点

#### 难点1: 高基数标签问题

**问题**:
"初期我们使用了设备ID作为标签,导致时间序列数量爆炸,查询性能急剧下降。"

**解决方案**:
"我们采取了以下措施:
1. **标签优化**: 避免使用高基数标签(如设备ID)
2. **标签聚合**: 使用聚合函数减少时间序列数量
3. **Recording Rules**: 预计算常用查询"

**效果**:
"时间序列数量从数十万降低到数千,查询性能提升10倍。"

#### 难点2: 监控数据存储成本

**问题**:
"随着监控指标增加,Prometheus存储空间快速增长,存储成本成为问题。"

**解决方案**:
"我们采取了以下措施:
1. **数据分级存储**: 实时数据保留15天,历史数据降采样
2. **指标筛选**: 只保留核心指标
3. **数据压缩**: 利用Prometheus的压缩机制"

**效果**:
"存储成本降低70%,同时满足监控需求。"

#### 难点3: 告警风暴问题

**问题**:
"初期告警规则设置不合理,一次网络抖动会触发数十条告警。"

**解决方案**:
"我们采取了以下措施:
1. **告警聚合**: 相同类型的告警聚合发送
2. **持续时间设置**: 增加for子句
3. **告警抑制**: 设置告警抑制规则"

**效果**:
"告警数量减少70%,告警有效性大幅提升。"

---

## 5. 技术深度解析

### 5.1 Micrometer指标体系

#### 5.1.1 指标类型

**Counter (计数器)**:
- 用途: 只增不减的累计值,如请求总数、错误总数
- 特点: 只能增加,不能减少,重启后归零
- 示例: `http_requests_total`, `errors_total`

**Gauge (仪表)**:
- 用途: 可增可减的瞬时值,如当前温度、当前连接数
- 特点: 可增可减,反映当前状态
- 示例: `jvm_memory_used_bytes`, `active_connections`

**Timer (计时器)**:
- 用途: 测量时间间隔,如请求响应时间、方法执行时间
- 特点: 同时记录计数和总时间,可计算平均值、百分位数
- 示例: `http_request_duration_seconds`, `method_execution_time`

**DistributionSummary (分布摘要)**:
- 用途: 测量分布情况,如请求大小、响应大小
- 特点: 记录分布情况,可计算百分位数
- 示例: `request_size_bytes`, `response_size_bytes`

#### 5.1.2 指标命名规范

**命名规范**:
```
<namespace>_<name>_<unit>

示例:
- http_requests_total (请求总数)
- http_request_duration_seconds (请求持续时间,单位秒)
- jvm_memory_used_bytes (JVM内存使用,单位字节)
- system_cpu_usage_percent (CPU使用率,单位百分比)
```

**最佳实践**:
1. 使用小写字母和下划线
2. 包含单位信息
3. 使用有意义的命名空间
4. 避免使用缩写

### 5.2 PromQL查询优化

#### 5.2.1 查询优化技巧

**1. 使用标签过滤**:
```promql
# 慢查询
sum(http_requests_total)

# 快查询
sum(http_requests_total{job="api-server"})
```

**2. 避免正则匹配**:
```promql
# 慢查询
http_requests_total{job=~"api.*"}

# 快查询
http_requests_total{job="api-server"}
```

**3. 使用Recording Rules**:
```yaml
# 预计算
groups:
  - name: recording_rules
    rules:
      - record: job:http_requests:rate5m
        expr: sum(rate(http_requests_total[5m])) by (job)

# 查询时直接使用
job:http_requests:rate5m
```

**4. 合理设置时间范围**:
```promql
# 查询最近5分钟
rate(http_requests_total[5m])

# 查询最近1小时
rate(http_requests_total[1h])
```

#### 5.2.2 常用查询模式

**速率计算**:
```promql
# 每秒请求数
rate(http_requests_total[5m])

# 每分钟请求数
rate(http_requests_total[5m]) * 60
```

**聚合计算**:
```promql
# 总和
sum(http_requests_total)

# 平均值
avg(http_requests_total)

# 最大值
max(http_requests_total)

# 按标签分组聚合
sum(http_requests_total) by (job)
```

**百分位数计算**:
```promql
# P95响应时间
histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le))

# P99响应时间
histogram_quantile(0.99, sum(rate(http_request_duration_seconds_bucket[5m])) by (le))
```

### 5.3 Grafana性能优化

#### 5.3.1 Dashboard优化

**1. 限制Panel数量**:
- 单个Dashboard不超过20个Panel
- 使用Row分组,折叠不常用的Panel

**2. 使用变量**:
- 减少重复查询
- 支持动态切换监控对象

**3. 设置合理的刷新间隔**:
- 实时监控: 10s-30s
- 历史分析: 1m-5m
- 避免过短的刷新间隔(如1s)

**4. 使用transformations**:
- 在前端处理数据
- 减少后端查询压力

#### 5.3.2 查询优化

**1. 限制数据点数量**:
```javascript
// Grafana配置
max_data_points = 1000
```

**2. 使用降采样**:
```promql
# 原始数据
http_requests_total

# 降采样数据
avg_over_time(http_requests_total[5m])
```

**3. 使用缓存**:
- 启用Grafana查询缓存
- 设置合理的缓存时间

---

## 附录

### A. 项目监控指标清单

**系统指标**:
- `node_cpu_seconds_total`: CPU使用时间
- `node_memory_MemTotal_bytes`: 总内存
- `node_memory_MemAvailable_bytes`: 可用内存
- `node_filesystem_size_bytes`: 文件系统大小
- `node_filesystem_avail_bytes`: 文件系统可用空间
- `node_network_receive_bytes_total`: 网络接收字节数
- `node_network_transmit_bytes_total`: 网络发送字节数

**应用指标**:
- `jvm_memory_used_bytes`: JVM内存使用
- `jvm_gc_pause_seconds`: GC暂停时间
- `jvm_threads_live_threads`: 活跃线程数
- `http_server_requests_seconds`: HTTP请求响应时间
- `hikaricp_connections_active`: 活跃数据库连接数
- `hikaricp_connections_idle`: 空闲数据库连接数

**业务指标**:
- `websocket_connections_active`: 活跃WebSocket连接数
- `mqtt_messages_received_total`: MQTT消息接收总数
- `kafka_messages_sent_total`: Kafka消息发送总数
- `telemetry_write_duration_seconds`: 遥测数据写入时间
- `device_online_status`: 设备在线状态
- `device_power_watts`: 设备功率

### B. 告警规则清单

**系统告警**:
- HighCPUUsage: CPU使用率 > 80%
- HighMemoryUsage: 内存使用率 > 85%
- HighDiskUsage: 磁盘使用率 > 85%

**应用告警**:
- HighAPIErrorRate: API错误率 > 5%
- HighAPILatency: API响应时间P95 > 2s
- DatabaseConnectionPoolExhausted: 数据库连接池使用率 > 90%

**业务告警**:
- DeviceOffline: 设备离线 > 5分钟
- AbnormalPower: 设备功率 > 3000W

### C. Dashboard清单

**系统监控Dashboard**:
- CPU使用率趋势
- 内存使用率趋势
- 磁盘I/O
- 网络流量

**应用监控Dashboard**:
- JVM监控
- API性能监控
- 数据库连接池监控
- 线程池监控

**业务监控Dashboard**:
- 设备在线状态
- 实时功率数据
- 用户用电量统计
- 告警数量统计

**中间件监控Dashboard**:
- PostgreSQL监控
- Redis监控
- MQTT监控
- Kafka监控

---

**文档版本**: v1.0  
**编写日期**: 2026年3月14日  
**编写人**: 系统架构组
