# Grafana学习文档 - 基础功能与场景使用与原理分析

## 目录

1. [Grafana概述](#1-grafana概述)
2. [核心概念与架构](#2-核心概念与架构)
3. [基础功能详解](#3-基础功能详解)
4. [数据可视化原理](#4-数据可视化原理)
5. [典型使用场景](#5-典型使用场景)
6. [高级特性与最佳实践](#6-高级特性与最佳实践)

---

## 1. Grafana概述

### 1.1 什么是Grafana

Grafana是一个开源的数据可视化和监控平台,由Grafana Labs开发维护。它能够将来自多种数据源的时间序列数据转换为美观、直观的图表和仪表板。

**核心价值**:
- **多数据源支持**: 支持Prometheus、InfluxDB、MySQL、PostgreSQL等30+种数据源
- **灵活可视化**: 提供丰富的图表类型和自定义选项
- **告警通知**: 支持多种告警渠道和灵活的告警规则
- **团队协作**: 支持Dashboard共享、权限管理
- **开源免费**: 社区版完全免费,企业版提供更多高级功能

### 1.2 Grafana vs 其他监控工具

| 对比维度 | Grafana | Kibana | Datadog |
|---------|---------|---------|---------|
| **定位** | 通用可视化平台 | 日志分析平台 | 全栈监控平台 |
| **数据源** | 多数据源支持 | 主要Elasticsearch | 自有数据源 |
| **成本** | 开源免费 | 开源免费 | 商业收费 |
| **灵活性** | 非常高 | 中等 | 低 |
| **学习曲线** | 平缓 | 中等 | 平缓 |
| **适用场景** | 通用监控可视化 | 日志分析 | 商业监控方案 |

### 1.3 Grafana发展历程

- **2014年**: Grafana项目启动
- **2015年**: 发布1.0版本,支持Graphite
- **2016年**: 支持Prometheus、InfluxDB
- **2017年**: 发布告警功能
- **2018年**: 支持MySQL、PostgreSQL
- **2019年**: 发布Grafana Cloud
- **2020年**: 发布Grafana 7.0,全新UI
- **2021年**: 发布Grafana 8.0,增强告警功能
- **2022年**: 发布Grafana 9.0,支持机器学习
- **2023年**: 发布Grafana 10.0,增强可观测性

---

## 2. 核心概念与架构

### 2.1 核心概念

#### 2.1.1 Dashboard (仪表板)

Dashboard是Grafana的核心概念,是一组Panel的集合,用于展示完整的监控视图。

**Dashboard结构**:
```
Dashboard
├── Panel 1 (图表)
├── Panel 2 (统计卡片)
├── Panel 3 (表格)
├── Row (行分组)
│   ├── Panel 4
│   └── Panel 5
└── Variables (变量)
```

**Dashboard特性**:
- **可分享**: 支持导出JSON、分享链接
- **可模板化**: 支持变量、模板化Dashboard
- **可嵌套**: 支持Dashboard嵌套
- **可标注**: 支持Annotations标注事件

#### 2.1.2 Panel (面板)

Panel是Dashboard的基本组成单元,每个Panel展示一个或多个指标。

**Panel类型**:
1. **Time Series (时间序列图)**: 展示时间序列数据
2. **Stat (统计卡片)**: 展示单个统计值
3. **Gauge (仪表盘)**: 展示当前值和阈值
4. **Bar Chart (柱状图)**: 展示分类数据
5. **Table (表格)**: 展示表格数据
6. **Heatmap (热力图)**: 展示密度分布
7. **Pie Chart (饼图)**: 展示比例分布

**Panel配置**:
```json
{
  "title": "CPU使用率",
  "type": "timeseries",
  "datasource": "Prometheus",
  "targets": [
    {
      "expr": "rate(cpu_usage_total[5m])",
      "legendFormat": "{{instance}}"
    }
  ],
  "fieldConfig": {
    "defaults": {
      "unit": "percent",
      "min": 0,
      "max": 100
    }
  }
}
```

#### 2.1.3 Data Source (数据源)

Data Source是Grafana获取数据的来源,支持多种数据源类型。

**常用数据源**:
1. **Prometheus**: 时序数据库,最适合监控场景
2. **InfluxDB**: 时序数据库,高性能写入
3. **MySQL/PostgreSQL**: 关系数据库,适合业务数据
4. **Elasticsearch**: 搜索引擎,适合日志数据
5. **Graphite**: 时序数据库,老牌监控方案

**数据源配置示例**:
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

#### 2.1.4 Query (查询)

Query是从数据源获取数据的语句,不同数据源有不同的查询语法。

**Prometheus查询示例**:
```promql
# 简单查询
http_requests_total

# 速率计算
rate(http_requests_total[5m])

# 聚合查询
sum(rate(http_requests_total[5m])) by (job)

# 过滤查询
http_requests_total{job="api-server",status="200"}
```

**InfluxDB查询示例**:
```sql
SELECT mean("value") 
FROM "cpu_usage" 
WHERE time > now() - 1h 
GROUP BY time(5m)
```

#### 2.1.5 Alert (告警)

Alert是Grafana的告警功能,支持灵活的告警规则和通知渠道。

**告警规则配置**:
```yaml
# 告警规则示例
- alert: HighCPUUsage
  expr: cpu_usage > 80
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "CPU使用率过高"
    description: "实例 {{ $labels.instance }} CPU使用率超过80%"
```

**通知渠道**:
- Email
- Slack
- PagerDuty
- Webhook
- 钉钉
- 企业微信

### 2.2 系统架构

#### 2.2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        Grafana架构                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │   Web UI    │  │   API层     │  │  插件系统   │            │
│  │  (React)    │  │  (Go)       │  │             │            │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘            │
│         │                │                │                     │
│         └────────────────┼────────────────┘                     │
│                          │                                       │
│                          ▼                                       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    服务层 (Services)                       │  │
│  │  - DashboardService                                       │  │
│  │  - DatasourceService                                      │  │
│  │  - AlertService                                           │  │
│  │  - UserService                                            │  │
│  └──────────────────────────┬───────────────────────────────┘  │
│                             │                                   │
│                             ▼                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    数据层 (Data Layer)                     │  │
│  │  - SQL Database (SQLite/PostgreSQL/MySQL)                 │  │
│  │  - Cache (Redis/Memory)                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    数据源连接层                            │  │
│  │  - Prometheus                                             │  │
│  │  - InfluxDB                                               │  │
│  │  - MySQL/PostgreSQL                                       │  │
│  │  - Elasticsearch                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 2.2.2 数据流

```
┌─────────────┐
│ 应用程序    │
│ (Metrics)   │
└──────┬──────┘
       │ 暴露指标
       │ /metrics
       ▼
┌─────────────┐
│ Prometheus  │ ← 抓取指标 (scrape)
│ (TSDB)      │
└──────┬──────┘
       │ 查询数据
       │ PromQL
       ▼
┌─────────────┐
│  Grafana    │ ← 可视化展示
│ (Dashboard) │
└──────┬──────┘
       │ 触发告警
       │
       ▼
┌─────────────┐
│ 通知渠道    │
│ (Email/Slack)│
└─────────────┘
```

#### 2.2.3 核心组件

**1. Frontend (前端)**:
- 技术栈: React + TypeScript
- 功能: 用户界面、图表渲染、交互操作
- 特点: 单页应用(SPA)、响应式设计

**2. Backend (后端)**:
- 技术栈: Go
- 功能: API服务、数据处理、权限管理
- 特点: 高性能、并发处理

**3. Database (数据库)**:
- 默认: SQLite (嵌入式)
- 生产: PostgreSQL / MySQL
- 存储: Dashboard配置、用户数据、告警规则

**4. Plugin System (插件系统)**:
- 数据源插件: 支持新数据源
- 面板插件: 支持新图表类型
- 应用插件: 扩展功能

---

## 3. 基础功能详解

### 3.1 Dashboard管理

#### 3.1.1 创建Dashboard

**方式一: 手动创建**:
1. 点击 "+" → "New Dashboard"
2. 添加Panel
3. 配置查询和可视化
4. 保存Dashboard

**方式二: 导入JSON**:
```json
{
  "dashboard": {
    "title": "My Dashboard",
    "panels": [...]
  },
  "overwrite": false
}
```

**方式三: 使用模板**:
- Grafana官方模板库: https://grafana.com/grafana/dashboards
- 导入模板ID或URL

#### 3.1.2 Dashboard配置

**基本配置**:
```json
{
  "title": "系统监控",
  "tags": ["system", "monitoring"],
  "timezone": "browser",
  "refresh": "30s",
  "time": {
    "from": "now-6h",
    "to": "now"
  },
  "templating": {
    "list": [
      {
        "name": "instance",
        "type": "query",
        "datasource": "Prometheus",
        "query": "label_values(instance)"
      }
    ]
  }
}
```

**时间范围配置**:
- **相对时间**: `now-6h`, `now-24h`, `now-7d`
- **绝对时间**: `2024-01-01 00:00:00` to `2024-01-02 00:00:00`
- **刷新间隔**: `5s`, `10s`, `30s`, `1m`, `5m`, `15m`, `30m`, `1h`

#### 3.1.3 Dashboard变量

**变量类型**:
1. **Query变量**: 从数据源动态获取值
2. **Custom变量**: 自定义值列表
3. **Constant变量**: 固定值
4. **Ad hoc变量**: 临时过滤变量
5. **Interval变量**: 时间间隔变量
6. **Data source变量**: 数据源选择变量

**Query变量示例**:
```json
{
  "name": "instance",
  "type": "query",
  "datasource": "Prometheus",
  "refresh": 1,
  "query": "label_values(node_cpu_seconds_total, instance)",
  "sort": 1,
  "multi": true,
  "includeAll": true
}
```

**使用变量**:
```promql
# 在查询中使用变量
node_cpu_seconds_total{instance=~"$instance"}

# 在Panel标题中使用变量
CPU Usage - $instance
```

### 3.2 Panel配置

#### 3.2.1 Panel类型详解

**1. Time Series (时间序列图)**

最适合展示随时间变化的指标数据。

**配置示例**:
```json
{
  "type": "timeseries",
  "title": "CPU使用率趋势",
  "targets": [
    {
      "expr": "rate(cpu_usage_total[5m]) * 100",
      "legendFormat": "{{instance}}"
    }
  ],
  "fieldConfig": {
    "defaults": {
      "unit": "percent",
      "min": 0,
      "max": 100,
      "custom": {
        "lineWidth": 2,
        "fillOpacity": 10,
        "showPoints": "auto"
      }
    }
  },
  "options": {
    "legend": {
      "displayMode": "list",
      "placement": "bottom"
    },
    "tooltip": {
      "mode": "multi"
    }
  }
}
```

**2. Stat (统计卡片)**

展示单个统计值,支持阈值和颜色变化。

**配置示例**:
```json
{
  "type": "stat",
  "title": "当前在线用户",
  "targets": [
    {
      "expr": "active_users_total"
    }
  ],
  "fieldConfig": {
    "defaults": {
      "unit": "short",
      "thresholds": {
        "mode": "absolute",
        "steps": [
          {"color": "green", "value": null},
          {"color": "yellow", "value": 100},
          {"color": "red", "value": 200}
        ]
      }
    }
  },
  "options": {
    "colorMode": "background",
    "graphMode": "area"
  }
}
```

**3. Gauge (仪表盘)**

展示当前值相对于阈值的位置。

**配置示例**:
```json
{
  "type": "gauge",
  "title": "内存使用率",
  "targets": [
    {
      "expr": "(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100"
    }
  ],
  "fieldConfig": {
    "defaults": {
      "unit": "percent",
      "min": 0,
      "max": 100,
      "thresholds": {
        "steps": [
          {"color": "green", "value": null},
          {"color": "yellow", "value": 70},
          {"color": "red", "value": 85}
        ]
      }
    }
  }
}
```

**4. Table (表格)**

展示表格数据,支持排序和过滤。

**配置示例**:
```json
{
  "type": "table",
  "title": "服务状态",
  "targets": [
    {
      "expr": "up",
      "format": "table",
      "instant": true
    }
  ],
  "transformations": [
    {
      "id": "organize",
      "options": {
        "excludeByName": {
          "Time": true,
          "__name__": true
        },
        "indexByName": {
          "instance": 0,
          "job": 1,
          "Value": 2
        },
        "renameByName": {
          "Value": "状态"
        }
      }
    }
  ]
}
```

#### 3.2.2 查询配置

**Prometheus查询**:

```promql
# 基本查询
http_requests_total

# 速率计算
rate(http_requests_total[5m])

# 聚合函数
sum(http_requests_total) by (job)
avg(http_requests_total) by (instance)
min(http_requests_total)
max(http_requests_total)

# 数学运算
http_requests_total / 60 / 60  # 转换为每小时

# 过滤
http_requests_total{job="api-server"}

# 时间窗口
http_requests_total offset 1h  # 1小时前的值
http_requests_total[5m]  # 5分钟内的所有值

# 预测(需要Prometheus 2.0+)
predict_linear(http_requests_total[1h], 3600)  # 预测1小时后的值
```

**查询选项**:
- **Format**: Time series / Table / Heatmap
- **Instant**: 返回瞬时值
- **Interval**: 查询间隔
- **Legend**: 图例格式

#### 3.2.3 可视化配置

**颜色配置**:
```json
{
  "fieldConfig": {
    "defaults": {
      "color": {
        "mode": "palette-classic"  // 经典调色板
        // 或
        "mode": "thresholds",      // 阈值颜色
        // 或
        "mode": "continuous-GrYlRd"  // 连续渐变
      }
    }
  }
}
```

**阈值配置**:
```json
{
  "thresholds": {
    "mode": "absolute",  // 或 "percentage"
    "steps": [
      {"color": "green", "value": null},
      {"color": "yellow", "value": 70},
      {"color": "red", "value": 90}
    ]
  }
}
```

**单位配置**:
- **时间**: seconds, minutes, hours, days
- **数据**: bytes, KB, MB, GB, TB
- **速率**: Bps, KBs, MBs
- **百分比**: percent (0-100), percentunit (0-1)
- **数值**: short, number
- **货币**: currencyUSD, currencyEUR

### 3.3 告警配置

#### 3.3.1 告警规则

**创建告警规则**:
1. 进入 "Alerting" → "Alert rules"
2. 点击 "New alert rule"
3. 配置告警条件
4. 设置通知策略

**告警规则示例**:
```yaml
# 告警规则配置
groups:
  - name: system_alerts
    rules:
      - alert: HighCPUUsage
        expr: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage detected"
          description: "CPU usage is above 80% (current value: {{ $value }}%)"
      
      - alert: HighMemoryUsage
        expr: (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100 > 85
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High memory usage detected"
          description: "Memory usage is above 85% (current value: {{ $value }}%)"
```

#### 3.3.2 通知渠道

**Email配置**:
```yaml
# grafana.ini配置
[smtp]
enabled = true
host = smtp.example.com:587
user = your-email@example.com
password = your-password
from_address = grafana@example.com
from_name = Grafana
```

**Slack配置**:
```yaml
# 通知渠道配置
apiVersion: 1
notifiers:
  - name: Slack
    type: slack
    settings:
      url: https://hooks.slack.com/services/xxx/yyy/zzz
      recipient: "#alerts"
      username: Grafana
```

**Webhook配置**:
```yaml
# Webhook通知
apiVersion: 1
notifiers:
  - name: Webhook
    type: webhook
    settings:
      url: http://your-server/webhook
      httpMethod: POST
      authorization_scheme: bearer
      authorization_credentials: your-token
```

#### 3.3.3 告警状态

**告警状态流转**:
```
┌─────────────┐
│   Normal    │ ← 指标正常
└──────┬──────┘
       │ 触发条件
       ▼
┌─────────────┐
│   Pending   │ ← 等待持续时间
└──────┬──────┘
       │ 持续时间到期
       ▼
┌─────────────┐
│   Firing    │ ← 发送告警通知
└──────┬──────┘
       │ 条件恢复
       ▼
┌─────────────┐
│  Resolved   │ ← 发送恢复通知
└─────────────┘
```

---

## 4. 数据可视化原理

### 4.1 数据采集流程

#### 4.1.1 指标暴露

**应用程序暴露指标**:
```java
// Spring Boot + Micrometer示例
@Configuration
public class MetricsConfig {
    
    @Bean
    public Counter apiRequestsCounter(MeterRegistry registry) {
        return Counter.builder("api.requests.total")
            .description("Total API requests")
            .tag("endpoint", "/api/users")
            .register(registry);
    }
    
    @Bean
    public Timer apiResponseTimer(MeterRegistry registry) {
        return Timer.builder("api.response.duration")
            .description("API response time")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }
}
```

**指标端点**:
```
# Spring Boot Actuator端点
http://localhost:8080/actuator/prometheus

# 返回示例
# HELP api_requests_total Total API requests
# TYPE api_requests_total counter
api_requests_total{endpoint="/api/users"} 1234.0

# HELP api_response_duration_seconds API response time
# TYPE api_response_duration_seconds summary
api_response_duration_seconds{endpoint="/api/users",quantile="0.5"} 0.023
api_response_duration_seconds{endpoint="/api/users",quantile="0.95"} 0.156
api_response_duration_seconds{endpoint="/api/users",quantile="0.99"} 0.234
```

#### 4.1.2 Prometheus抓取

**Prometheus配置**:
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'my-application'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app1:8080', 'app2:8080']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: '$1'
```

**抓取流程**:
```
┌─────────────┐
│ Prometheus  │
└──────┬──────┘
       │ 1. 定时抓取 (scrape_interval)
       │ GET /actuator/prometheus
       ▼
┌─────────────┐
│ 应用程序    │
│ /metrics    │
└──────┬──────┘
       │ 2. 返回指标数据
       │
       ▼
┌─────────────┐
│ Prometheus  │ ← 3. 存储到TSDB
│ TSDB        │
└─────────────┘
```

### 4.2 数据存储原理

#### 4.2.1 时序数据库(TSDB)

**Prometheus存储结构**:
```
数据目录结构:
data/
├── chunks_head/        # 当前写入的数据块
│   └── 000001          # 数据块文件
├── chunks/             # 已压缩的数据块
│   └── 000001
├── index/              # 索引文件
│   └── index
├── meta.json           # 元数据
└── tombstones/         # 删除标记
```

**数据压缩**:
- **Chunk**: 每个Chunk存储约512个数据点
- **压缩算法**: Facebook ZSTD / Google Snappy
- **压缩比**: 约10:1 (原始数据 vs 压缩后数据)

**数据保留**:
```yaml
# prometheus.yml
global:
  retention: 15d        # 数据保留15天
  retention_size: 10GB  # 或最大10GB
```

#### 4.2.2 索引机制

**倒排索引**:
```
指标名称 → 时间序列ID列表
标签组合 → 时间序列ID列表

示例:
http_requests_total{job="api",instance="server1"} → TSID: 001
http_requests_total{job="api",instance="server2"} → TSID: 002
http_requests_total{job="web",instance="server1"} → TSID: 003
```

**查询优化**:
```promql
# 快速查询 (使用索引)
http_requests_total{job="api"}

# 慢速查询 (需要全表扫描)
http_requests_total{job=~"api.*"}  # 正则匹配
```

### 4.3 数据查询原理

#### 4.3.1 PromQL查询执行

**查询执行流程**:
```
┌─────────────────────────────────────────────────────────┐
│ PromQL查询执行流程                                       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1. 解析查询语句 (Parse)                                 │
│     ┌──────────────────────────────────────────────┐   │
│     │ rate(http_requests_total[5m]) by (job)       │   │
│     └──────────────────────────────────────────────┘   │
│                     │                                   │
│                     ▼                                   │
│  2. 查找匹配的时间序列 (Select)                          │
│     ┌──────────────────────────────────────────────┐   │
│     │ http_requests_total{job="api",instance="s1"} │   │
│     │ http_requests_total{job="api",instance="s2"} │   │
│     └──────────────────────────────────────────────┘   │
│                     │                                   │
│                     ▼                                   │
│  3. 加载数据点 (Load)                                    │
│     ┌──────────────────────────────────────────────┐   │
│     │ [时间戳, 值] 数组                              │   │
│     └──────────────────────────────────────────────┘   │
│                     │                                   │
│                     ▼                                   │
│  4. 执行函数计算 (Evaluate)                              │
│     ┌──────────────────────────────────────────────┐   │
│     │ rate(): 计算速率                               │   │
│     │ by (job): 按job分组聚合                         │   │
│     └──────────────────────────────────────────────┘   │
│                     │                                   │
│                     ▼                                   │
│  5. 返回结果 (Return)                                    │
│     ┌──────────────────────────────────────────────┐   │
│     │ {job="api"}: [时间戳, 速率值]                  │   │
│     └──────────────────────────────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**查询优化技巧**:
1. **使用标签过滤**: 减少查询的数据量
2. **避免正则匹配**: 使用精确匹配更快
3. **合理使用聚合**: 减少返回的数据点
4. **设置查询超时**: 防止慢查询影响性能

#### 4.3.2 数据采样

**降采样(Downsampling)**:
```promql
# 原始数据: 每15秒一个点
http_requests_total

# 降采样: 每5分钟一个点
avg_over_time(http_requests_total[5m])
```

**数据插值**:
```
原始数据点:
t0: 10, t1: null, t2: null, t3: 20

线性插值后:
t0: 10, t1: 13.3, t2: 16.6, t3: 20
```

### 4.4 可视化渲染

#### 4.4.1 前端渲染架构

**技术栈**:
- **React**: UI框架
- **D3.js**: 数据可视化库
- **Canvas**: 图表渲染
- **SVG**: 矢量图形

**渲染流程**:
```
┌─────────────────────────────────────────────────────────┐
│ Grafana前端渲染流程                                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1. 获取数据 (Fetch Data)                                │
│     ┌──────────────────────────────────────────────┐   │
│     │ 从数据源API获取查询结果                         │   │
│     └──────────────────────────────────────────────┘   │
│                     │                                   │
│                     ▼                                   │
│  2. 数据转换 (Transform Data)                            │
│     ┌──────────────────────────────────────────────┐   │
│     │ 格式化数据、应用transformations                 │   │
│     └──────────────────────────────────────────────┘   │
│                     │                                   │
│                     ▼                                   │
│  3. 计算布局 (Calculate Layout)                          │
│     ┌──────────────────────────────────────────────┐   │
│     │ 计算坐标轴、图例、网格等位置                     │   │
│     └──────────────────────────────────────────────┘   │
│                     │                                   │
│                     ▼                                   │
│  4. 渲染图表 (Render Chart)                              │
│     ┌──────────────────────────────────────────────┐   │
│     │ 使用Canvas/SVG绘制图表                         │   │
│     └──────────────────────────────────────────────┘   │
│                     │                                   │
│                     ▼                                   │
│  5. 交互处理 (Handle Interactions)                       │
│     ┌──────────────────────────────────────────────┐   │
│     │ 处理鼠标悬停、缩放、点击等事件                   │   │
│     └──────────────────────────────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 4.4.2 性能优化

**数据点限制**:
```javascript
// Grafana配置
[panels]
default_chart_height = 300
min_chart_height = 100
max_data_points = 10000  // 单个Panel最大数据点数
```

**渲染优化**:
1. **虚拟滚动**: 大量Panel时只渲染可见区域
2. **Canvas缓存**: 缓存已渲染的图表
3. **防抖节流**: 限制刷新频率
4. **懒加载**: 延迟加载Panel数据

---

## 5. 典型使用场景

### 5.1 基础设施监控

#### 5.1.1 服务器监控

**监控指标**:
- **CPU**: 使用率、负载、上下文切换
- **内存**: 使用率、缓存、交换空间
- **磁盘**: 使用率、I/O、读写速度
- **网络**: 流量、连接数、错误率

**Dashboard示例**:
```json
{
  "title": "服务器监控",
  "panels": [
    {
      "title": "CPU使用率",
      "type": "timeseries",
      "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0},
      "targets": [
        {
          "expr": "100 - (avg by(instance) (irate(node_cpu_seconds_total{mode=\"idle\"}[5m])) * 100)",
          "legendFormat": "{{instance}}"
        }
      ]
    },
    {
      "title": "内存使用率",
      "type": "gauge",
      "gridPos": {"h": 8, "w": 6, "x": 12, "y": 0},
      "targets": [
        {
          "expr": "(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100"
        }
      ]
    },
    {
      "title": "磁盘I/O",
      "type": "timeseries",
      "gridPos": {"h": 8, "w": 6, "x": 18, "y": 0},
      "targets": [
        {
          "expr": "rate(node_disk_read_bytes_total[5m])",
          "legendFormat": "Read - {{device}}"
        },
        {
          "expr": "rate(node_disk_written_bytes_total[5m])",
          "legendFormat": "Write - {{device}}"
        }
      ]
    }
  ]
}
```

#### 5.1.2 容器监控

**监控指标**:
- **容器状态**: 运行、停止、重启次数
- **资源使用**: CPU、内存、网络
- **健康检查**: 存活探针、就绪探针

**查询示例**:
```promql
# 容器CPU使用率
sum(rate(container_cpu_usage_seconds_total{container_name!="POD"}[5m])) by (pod_name)

# 容器内存使用
sum(container_memory_working_set_bytes{container_name!="POD"}) by (pod_name)

# 容器网络流量
sum(rate(container_network_receive_bytes_total[5m])) by (pod_name)
sum(rate(container_network_transmit_bytes_total[5m])) by (pod_name)
```

### 5.2 应用性能监控(APM)

#### 5.2.1 API性能监控

**监控指标**:
- **请求量**: QPS、请求总数
- **响应时间**: P50、P95、P99延迟
- **错误率**: 4xx、5xx错误比例

**Dashboard示例**:
```json
{
  "title": "API性能监控",
  "panels": [
    {
      "title": "请求QPS",
      "type": "timeseries",
      "targets": [
        {
          "expr": "sum(rate(http_requests_total[5m])) by (endpoint)",
          "legendFormat": "{{endpoint}}"
        }
      ]
    },
    {
      "title": "响应时间P95",
      "type": "stat",
      "targets": [
        {
          "expr": "histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, endpoint))",
          "legendFormat": "{{endpoint}}"
        }
      ]
    },
    {
      "title": "错误率",
      "type": "gauge",
      "targets": [
        {
          "expr": "sum(rate(http_requests_total{status=~\"5..\"}[5m])) / sum(rate(http_requests_total[5m])) * 100"
        }
      ]
    }
  ]
}
```

#### 5.2.2 数据库性能监控

**监控指标**:
- **连接数**: 活跃连接、空闲连接
- **查询性能**: 查询时间、慢查询
- **事务**: 提交、回滚、锁等待

**查询示例**:
```promql
# 数据库连接数
pg_stat_activity_count

# 查询响应时间
pg_query_duration_seconds{quantile="0.95"}

# 慢查询数量
pg_stat_statements_calls{query_time > 1}
```

### 5.3 业务监控

#### 5.3.1 用户行为监控

**监控指标**:
- **在线用户**: 当前在线、峰值用户
- **用户活跃度**: DAU、MAU
- **用户行为**: 页面访问、点击、转化

**Dashboard示例**:
```json
{
  "title": "用户行为监控",
  "panels": [
    {
      "title": "在线用户数",
      "type": "stat",
      "targets": [
        {
          "expr": "active_users_total"
        }
      ]
    },
    {
      "title": "页面访问量",
      "type": "timeseries",
      "targets": [
        {
          "expr": "sum(rate(page_views_total[5m])) by (page)",
          "legendFormat": "{{page}}"
        }
      ]
    },
    {
      "title": "转化漏斗",
      "type": "bargauge",
      "targets": [
        {
          "expr": "sum(page_views_total{page=\"home\"})",
          "legendFormat": "首页"
        },
        {
          "expr": "sum(page_views_total{page=\"product\"})",
          "legendFormat": "产品页"
        },
        {
          "expr": "sum(page_views_total{page=\"checkout\"})",
          "legendFormat": "结算页"
        }
      ]
    }
  ]
}
```

#### 5.3.2 业务指标监控

**监控指标**:
- **订单**: 订单量、订单金额
- **支付**: 支付成功率、支付金额
- **库存**: 库存水平、缺货率

**查询示例**:
```promql
# 订单量
sum(rate(orders_total[1h]))

# 订单金额
sum(order_amount_total)

# 支付成功率
sum(payment_success_total) / sum(payment_attempts_total) * 100
```

---

## 6. 高级特性与最佳实践

### 6.1 Dashboard模板化

#### 6.1.1 使用变量

**场景**: 监控多个服务器,每个服务器需要相同的监控面板。

**实现**:
```json
{
  "templating": {
    "list": [
      {
        "name": "server",
        "type": "query",
        "datasource": "Prometheus",
        "query": "label_values(node_cpu_seconds_total, instance)",
        "refresh": 1,
        "multi": true,
        "includeAll": true
      }
    ]
  },
  "panels": [
    {
      "title": "CPU使用率 - $server",
      "targets": [
        {
          "expr": "100 - (avg by(instance) (irate(node_cpu_seconds_total{instance=~\"$server\",mode=\"idle\"}[5m])) * 100)"
        }
      ]
    }
  ]
}
```

#### 6.1.2 Dashboard链接

**场景**: 从总览Dashboard跳转到详细Dashboard。

**实现**:
```json
{
  "links": [
    {
      "title": "查看服务器详情",
      "type": "link",
      "targetBlank": true,
      "url": "/d/server-detail?var-server=${__value}"
    }
  ]
}
```

### 6.2 告警最佳实践

#### 6.2.1 告警分级

**告警级别**:
- **P0 (Critical)**: 需要立即处理
- **P1 (Warning)**: 需要尽快处理
- **P2 (Info)**: 需要关注

**配置示例**:
```yaml
groups:
  - name: critical_alerts
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "服务宕机"
          description: "服务 {{ $labels.job }} 已宕机超过1分钟"
  
  - name: warning_alerts
    rules:
      - alert: HighCPUUsage
        expr: cpu_usage > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "CPU使用率过高"
          description: "CPU使用率超过80%"
```

#### 6.2.2 告警聚合

**场景**: 避免告警风暴,相同类型的告警聚合发送。

**配置**:
```yaml
# 告警路由配置
route:
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 1h
  receiver: 'team-email'
  
receivers:
  - name: 'team-email'
    email_configs:
      - to: 'team@example.com'
```

### 6.3 性能优化

#### 6.3.1 查询优化

**优化技巧**:
1. **使用标签过滤**: 减少查询数据量
2. **避免正则匹配**: 使用精确匹配
3. **合理设置时间范围**: 不要查询过长时间范围
4. **使用recording rules**: 预计算常用查询

**Recording Rules示例**:
```yaml
groups:
  - name: recording_rules
    rules:
      - record: job:http_requests:rate5m
        expr: sum(rate(http_requests_total[5m])) by (job)
      
      - record: instance:cpu_usage:rate5m
        expr: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
```

#### 6.3.2 Dashboard优化

**优化技巧**:
1. **限制Panel数量**: 单个Dashboard不超过20个Panel
2. **使用变量**: 减少重复查询
3. **设置刷新间隔**: 不要设置过短的刷新间隔
4. **使用transformations**: 在前端处理数据

**transformations示例**:
```json
{
  "transformations": [
    {
      "id": "calculateField",
      "options": {
        "mode": "reduceRow",
        "reduce": {
          "reducer": "sum"
        }
      }
    },
    {
      "id": "organize",
      "options": {
        "excludeByName": {
          "Time": true
        }
      }
    }
  ]
}
```

### 6.4 安全最佳实践

#### 6.4.1 权限管理

**角色权限**:
- **Admin**: 完全访问权限
- **Editor**: 可编辑Dashboard
- **Viewer**: 只读权限

**配置示例**:
```yaml
# grafana.ini
[auth]
disable_login_form = false

[users]
default_role = Viewer

[auth.anonymous]
enabled = false
```

#### 6.4.2 数据源安全

**配置示例**:
```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    basicAuth: true
    basicAuthUser: admin
    secureJsonData:
      basicAuthPassword: ${PROMETHEUS_PASSWORD}
```

---

## 附录

### A. Grafana配置文件详解

**grafana.ini主要配置**:
```ini
[server]
http_addr = 0.0.0.0
http_port = 3000
domain = localhost
root_url = http://localhost:3000

[database]
type = sqlite3
path = data/grafana.db

[session]
provider = memory
cookie_secure = false
cookie_samesite = lax

[security]
admin_user = admin
admin_password = admin
secret_key = SW2YcwTIb9zpOOhoPsMm

[auth]
disable_login_form = false

[dashboards]
default_home_dashboard_path = /var/lib/grafana/dashboards/home.json

[paths]
data = /var/lib/grafana/data
logs = /var/log/grafana
plugins = /var/lib/grafana/plugins
provisioning = /etc/grafana/provisioning
```

### B. 常用PromQL查询

**系统监控**:
```promql
# CPU使用率
100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# 内存使用率
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100

# 磁盘使用率
(1 - (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"})) * 100

# 网络流量
rate(node_network_receive_bytes_total{device="eth0"}[5m])
rate(node_network_transmit_bytes_total{device="eth0"}[5m])
```

**应用监控**:
```promql
# 请求QPS
sum(rate(http_requests_total[5m])) by (endpoint)

# 响应时间P95
histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, endpoint))

# 错误率
sum(rate(http_requests_total{status=~"5.."}[5m])) / sum(rate(http_requests_total[5m])) * 100

# 在线用户数
active_users_total
```

### C. Grafana API使用

**创建Dashboard**:
```bash
curl -X POST http://admin:admin@localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d '{
    "dashboard": {
      "title": "My Dashboard",
      "panels": []
    },
    "overwrite": false
  }'
```

**查询数据源**:
```bash
curl -X POST http://admin:admin@localhost:3000/api/datasources/proxy/1/api/v1/query \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d 'query=up'
```

### D. 参考资料

1. **Grafana官方文档**: https://grafana.com/docs/grafana/latest/
2. **Prometheus文档**: https://prometheus.io/docs/
3. **PromQL教程**: https://prometheus.io/docs/prometheus/latest/querying/basics/
4. **Grafana社区**: https://community.grafana.com/
5. **Grafana插件市场**: https://grafana.com/grafana/plugins/

---

**文档版本**: v1.0  
**编写日期**: 2026年3月14日  
**编写人**: 系统架构组
