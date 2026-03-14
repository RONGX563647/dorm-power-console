# PostgreSQL 学习文档

## 目录
- [1. PostgreSQL 基础概念](#1-postgresql-基础概念)
- [2. PostgreSQL 核心特性](#2-postgresql-核心特性)
- [3. PostgreSQL 数据类型](#3-postgresql-数据类型)
- [4. PostgreSQL 高级功能](#4-postgresql-高级功能)
- [5. PostgreSQL 性能优化](#5-postgresql-性能优化)
- [6. PostgreSQL 原理分析](#6-postgresql-原理分析)
- [7. PostgreSQL 面试要点](#7-postgresql-面试要点)

---

## 1. PostgreSQL 基础概念

### 1.1 什么是 PostgreSQL

PostgreSQL 是一个功能强大的开源对象关系型数据库管理系统（ORDBMS），以其可靠性、功能强大和性能卓越而闻名。

### 1.2 PostgreSQL 与其他数据库对比

| 对比维度 | PostgreSQL | MySQL | Oracle | SQL Server |
|----------|------------|-------|--------|------------|
| **开源** | ✅ 完全开源 | ✅ 开源 | ❌ 商业 | ❌ 商业 |
| **ACID** | ✅ 完全支持 | ✅ 支持 | ✅ 支持 | ✅ 支持 |
| **JSON支持** | ✅ 强大 | ✅ 支持 | ✅ 支持 | ✅ 支持 |
| **扩展性** | ✅ 极强 | 中等 | 强 | 强 |
| **并发控制** | MVCC | MVCC | MVCC | MVCC |
| **全文搜索** | ✅ 内置 | ✅ 支持 | ✅ 支持 | ✅ 支持 |
| **地理信息** | ✅ PostGIS | ❌ 需插件 | ✅ 支持 | ✅ 支持 |
| **适用场景** | 企业级、复杂查询 | Web应用 | 企业级 | 企业级 |

### 1.3 PostgreSQL 架构

```
┌─────────────────────────────────────────────────────────────┐
│                    PostgreSQL 架构                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   客户端连接层                           ││
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐            ││
│  │  │ JDBC     │  │ ODBC     │  │ libpq    │            ││
│  │  └──────────┘  └──────────┘  └──────────┘            ││
│  └──────────────────────┬──────────────────────────────────┘│
│                         │                                    │
│                         ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   PostgreSQL 进程模型                    ││
│  │  ┌──────────────────────────────────────────────────┐  ││
│  │  │  Postmaster (主进程)                              │  ││
│  │  │  - 监听连接请求                                   │  ││
│  │  │  - 创建后端进程                                   │  ││
│  │  └──────────────────────┬───────────────────────────┘  ││
│  │                         │                               ││
│  │         ┌───────────────┼───────────────┐              ││
│  │         ▼               ▼               ▼              ││
│  │  ┌──────────┐    ┌──────────┐    ┌──────────┐        ││
│  │  │ Backend  │    │ Backend  │    │ Backend  │        ││
│  │  │ Process  │    │ Process  │    │ Process  │        ││
│  │  │ (连接1)  │    │ (连接2)  │    │ (连接3)  │        ││
│  │  └──────────┘    └──────────┘    └──────────┘        ││
│  └─────────────────────────────────────────────────────────┘│
│                         │                                    │
│                         ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   共享内存区域                           ││
│  │  ┌──────────────────────────────────────────────────┐  ││
│  │  │  Shared Buffers (共享缓冲区)                      │  ││
│  │  │  WAL Buffers (WAL缓冲区)                         │  ││
│  │  │  Lock Tables (锁表)                              │  ││
│  │  └──────────────────────────────────────────────────┘  ││
│  └──────────────────────┬──────────────────────────────────┘│
│                         │                                    │
│                         ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   存储层                                ││
│  │  ┌──────────────────────────────────────────────────┐  ││
│  │  │  Data Files (数据文件)                            │  ││
│  │  │  WAL Files (WAL日志)                             │  ││
│  │  │  Index Files (索引文件)                          │  ││
│  │  └──────────────────────────────────────────────────┘  ││
│  └─────────────────────────────────────────────────────────┘│
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. PostgreSQL 核心特性

### 2.1 MVCC（多版本并发控制）

```
┌─────────────────────────────────────────────────────────────┐
│                      MVCC 原理                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  传统锁机制：                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  事务A ──读──> 数据 ──加锁──> 事务B等待              │   │
│  │  问题：读写冲突，并发性能差                          │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  MVCC 机制：                                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  数据行：                                            │   │
│  │  ┌────────────────────────────────────────────┐    │   │
│  │  │ id=1 | name='设备A' | xmin=100 | xmax=null │    │   │
│  │  └────────────────────────────────────────────┘    │   │
│  │                                                      │   │
│  │  更新操作：                                          │   │
│  │  ┌────────────────────────────────────────────┐    │   │
│  │  │ id=1 | name='设备A' | xmin=100 | xmax=200  │旧  │   │
│  │  ├────────────────────────────────────────────┤    │   │
│  │  │ id=1 | name='设备B' | xmin=200 | xmax=null │新  │   │
│  │  └────────────────────────────────────────────┘    │   │
│  │                                                      │   │
│  │  事务A (xmin=100)：看到旧版本 '设备A'               │   │
│  │  事务B (xmin=200)：看到新版本 '设备B'               │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  优势：                                                    │
│  - 读不阻塞写                                             │
│  - 写不阻塞读                                             │
│  - 高并发性能                                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 WAL（预写式日志）

```
┌─────────────────────────────────────────────────────────────┐
│                      WAL 原理                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  写入流程：                                                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  1. 事务开始                                         │   │
│  │  2. 修改数据                                         │   │
│  │  3. 写入 WAL 缓冲区                                  │   │
│  │  4. WAL 缓冲区刷盘                                   │   │
│  │  5. 提交事务                                         │   │
│  │  6. 异步刷写数据文件                                 │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  WAL 结构：                                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  WAL 文件：pg_wal/000000010000000000000001          │   │
│  │  ┌────────────────────────────────────────────┐    │   │
│  │  │  LSN 1: INSERT INTO devices VALUES (...)   │    │   │
│  │  │  LSN 2: UPDATE devices SET online=true     │    │   │
│  │  │  LSN 3: COMMIT                              │    │   │
│  │  └────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  优势：                                                    │
│  - 崩溃恢复                                               │
│  - 时间点恢复（PITR）                                     │
│  - 流复制                                                 │
│  - 逻辑复制                                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 事务隔离级别

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 说明 |
|----------|------|------------|------|------|
| READ UNCOMMITTED | ❌ | ❌ | ❌ | PostgreSQL 不支持 |
| READ COMMITTED | ✅ | ❌ | ❌ | 默认级别 |
| REPEATABLE READ | ✅ | ✅ | ❌ | 快照隔离 |
| SERIALIZABLE | ✅ | ✅ | ✅ | 最高隔离级别 |

```sql
-- 设置事务隔离级别
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

-- 查看当前隔离级别
SHOW default_transaction_isolation;
```

---

## 3. PostgreSQL 数据类型

### 3.1 基本数据类型

| 类型分类 | 数据类型 | 说明 |
|----------|----------|------|
| **整数** | smallint, integer, bigint | 2/4/8 字节 |
| **自增** | smallserial, serial, bigserial | 自增整数 |
| **浮点** | real, double precision | 4/8 字节浮点 |
| **定点** | numeric(p, s), decimal(p, s) | 精确数值 |
| **字符串** | char(n), varchar(n), text | 定长/变长/无限 |
| **布尔** | boolean | true/false |
| **日期时间** | date, time, timestamp | 日期时间 |
| **UUID** | uuid | 通用唯一标识符 |
| **JSON** | json, jsonb | JSON 数据 |
| **数组** | integer[], text[] | 数组类型 |
| **网络地址** | inet, cidr, macaddr | IP/MAC 地址 |

### 3.2 JSON/JSONB 类型

```sql
-- 创建表
CREATE TABLE devices (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    config JSONB
);

-- 插入 JSON 数据
INSERT INTO devices (name, config) VALUES (
    '设备A',
    '{"power": 1500, "voltage": 220, "sockets": [{"id": 1, "on": true}]}'
);

-- 查询 JSON 字段
SELECT 
    name,
    config->>'power' AS power,
    config->'sockets'->0->>'id' AS socket_id
FROM devices;

-- JSONB 操作符
SELECT * FROM devices WHERE config @> '{"power": 1500}';
SELECT * FROM devices WHERE config ? 'power';
SELECT * FROM devices WHERE config->'sockets' @> '[{"id": 1}]';

-- 更新 JSON 字段
UPDATE devices 
SET config = jsonb_set(config, '{power}', '2000')
WHERE id = 1;

-- 添加 JSON 字段
UPDATE devices 
SET config = config || '{"current": 10}'
WHERE id = 1;
```

### 3.3 数组类型

```sql
-- 创建数组字段
CREATE TABLE rooms (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    device_ids TEXT[]
);

-- 插入数组数据
INSERT INTO rooms (name, device_ids) VALUES ('101', ARRAY['dev1', 'dev2', 'dev3']);
INSERT INTO rooms (name, device_ids) VALUES ('102', '{"dev4", "dev5"}');

-- 查询数组
SELECT * FROM rooms WHERE 'dev1' = ANY(device_ids);
SELECT * FROM rooms WHERE device_ids @> ARRAY['dev1'];
SELECT * FROM rooms WHERE device_ids && ARRAY['dev1', 'dev4'];

-- 数组操作
SELECT array_length(device_ids, 1) FROM rooms;
SELECT device_ids[1] FROM rooms;
UPDATE rooms SET device_ids = array_append(device_ids, 'dev6') WHERE id = 1;
```

---

## 4. PostgreSQL 高级功能

### 4.1 索引类型

| 索引类型 | 说明 | 适用场景 |
|----------|------|----------|
| **B-Tree** | 默认索引 | 等值、范围、排序查询 |
| **Hash** | 哈希索引 | 等值查询 |
| **GiST** | 通用搜索树 | 几何、全文搜索 |
| **GIN** | 倒排索引 | 数组、JSONB、全文搜索 |
| **SP-GiST** | 空间分区 | 四叉树、基数树 |
| **BRIN** | 块范围索引 | 大表、有序数据 |

```sql
-- B-Tree 索引（默认）
CREATE INDEX idx_device_room ON devices(room);
CREATE INDEX idx_device_room_online ON devices(room, online);

-- GIN 索引（JSONB、数组）
CREATE INDEX idx_device_config ON devices USING GIN(config);
CREATE INDEX idx_room_devices ON rooms USING GIN(device_ids);

-- 部分索引
CREATE INDEX idx_device_online ON devices(room) WHERE online = true;

-- 表达式索引
CREATE INDEX idx_device_name_lower ON devices(LOWER(name));

-- 唯一索引
CREATE UNIQUE INDEX idx_device_name_unique ON devices(name);

-- 并发创建索引（不阻塞写入）
CREATE INDEX CONCURRENTLY idx_device_created ON devices(created_at);
```

### 4.2 全文搜索

```sql
-- 创建全文搜索索引
CREATE INDEX idx_device_name_search ON devices USING GIN(to_tsvector('english', name));

-- 全文搜索查询
SELECT * FROM devices 
WHERE to_tsvector('english', name) @@ to_tsquery('english', 'power & device');

-- 使用 plainto_tsquery（自动处理查询词）
SELECT * FROM devices 
WHERE to_tsvector('english', name) @@ plainto_tsquery('english', 'power device');

-- 高亮显示
SELECT 
    name,
    ts_headline('english', name, plainto_tsquery('english', 'power'))
FROM devices
WHERE to_tsvector('english', name) @@ plainto_tsquery('english', 'power');
```

### 4.3 窗口函数

```sql
-- 排名函数
SELECT 
    device_id,
    power_w,
    RANK() OVER (ORDER BY power_w DESC) AS rank,
    DENSE_RANK() OVER (ORDER BY power_w DESC) AS dense_rank,
    ROW_NUMBER() OVER (ORDER BY power_w DESC) AS row_num
FROM telemetry;

-- 分组聚合
SELECT 
    device_id,
    power_w,
    AVG(power_w) OVER (PARTITION BY device_id) AS avg_power,
    SUM(power_w) OVER (PARTITION BY device_id) AS total_power
FROM telemetry;

-- 移动平均
SELECT 
    device_id,
    ts,
    power_w,
    AVG(power_w) OVER (
        PARTITION BY device_id 
        ORDER BY ts 
        ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
    ) AS moving_avg
FROM telemetry;

-- 累计求和
SELECT 
    device_id,
    ts,
    power_w,
    SUM(power_w) OVER (PARTITION BY device_id ORDER BY ts) AS cumulative_power
FROM telemetry;
```

### 4.4 CTE（公共表表达式）

```sql
-- 简单 CTE
WITH active_devices AS (
    SELECT * FROM devices WHERE online = true
)
SELECT d.name, t.power_w
FROM active_devices d
JOIN telemetry t ON d.id = t.device_id;

-- 递归 CTE（查询树形结构）
WITH RECURSIVE building_tree AS (
    -- 基础查询
    SELECT id, name, parent_id, 1 AS level
    FROM buildings
    WHERE parent_id IS NULL
    
    UNION ALL
    
    -- 递归查询
    SELECT b.id, b.name, b.parent_id, bt.level + 1
    FROM buildings b
    JOIN building_tree bt ON b.parent_id = bt.id
)
SELECT * FROM building_tree ORDER BY level;

-- 多个 CTE
WITH 
    device_stats AS (
        SELECT device_id, AVG(power_w) AS avg_power
        FROM telemetry
        GROUP BY device_id
    ),
    high_power_devices AS (
        SELECT * FROM device_stats WHERE avg_power > 1000
    )
SELECT d.name, h.avg_power
FROM devices d
JOIN high_power_devices h ON d.id = h.device_id;
```

---

## 5. PostgreSQL 性能优化

### 5.1 查询优化

```sql
-- 使用 EXPLAIN ANALYZE 分析查询
EXPLAIN ANALYZE SELECT * FROM devices WHERE room = '101';

-- 查看执行计划
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) 
SELECT * FROM devices WHERE room = '101';

-- 常见优化技巧

-- 1. 避免 SELECT *
SELECT id, name, room FROM devices;

-- 2. 使用 LIMIT
SELECT * FROM telemetry ORDER BY ts DESC LIMIT 100;

-- 3. 使用覆盖索引
CREATE INDEX idx_device_room_covering ON devices(room) INCLUDE (name, online);

-- 4. 批量插入
INSERT INTO telemetry (device_id, ts, power_w) VALUES
    ('dev1', 1640000000, 100),
    ('dev2', 1640000001, 200),
    ('dev3', 1640000002, 300);

-- 5. 使用 COPY 批量导入
COPY telemetry FROM '/path/to/data.csv' CSV;

-- 6. 分区表
CREATE TABLE telemetry (
    id BIGSERIAL,
    device_id VARCHAR(50),
    ts BIGINT,
    power_w DOUBLE PRECISION
) PARTITION BY RANGE (ts);

CREATE TABLE telemetry_2024_01 PARTITION OF telemetry
    FOR VALUES FROM (1704067200) TO (1706745600);
```

### 5.2 索引优化

```sql
-- 查看索引使用情况
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- 查找未使用的索引
SELECT 
    schemaname || '.' || relname AS table,
    indexrelname AS index,
    pg_size_pretty(pg_relation_size(i.indexrelid)) AS index_size,
    idx_scan AS index_scans
FROM pg_stat_user_indexes ui
JOIN pg_index i ON ui.indexrelid = i.indexrelid
WHERE NOT indisunique 
    AND idx_scan < 50 
    AND pg_relation_size(relid) > 5 * 8192
ORDER BY pg_relation_size(i.indexrelid) DESC;

-- 重建索引
REINDEX INDEX idx_device_room;
REINDEX TABLE devices;

-- 并发重建索引（不阻塞）
REINDEX INDEX CONCURRENTLY idx_device_room;
```

### 5.3 连接池配置

```sql
-- 查看当前连接
SELECT 
    pid,
    usename,
    application_name,
    client_addr,
    state,
    query,
    query_start
FROM pg_stat_activity;

-- 终止连接
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE pid = 12345;

-- 配置最大连接数
-- postgresql.conf
max_connections = 100
superuser_reserved_connections = 3
```

---

## 6. PostgreSQL 原理分析

### 6.1 查询处理流程

```
┌─────────────────────────────────────────────────────────────┐
│                    查询处理流程                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 解析器(Parser)                                         │
│     ┌─────────────────────────────────────────────────────┐│
│     │  SQL: SELECT * FROM devices WHERE room = '101'      ││
│     │  ↓                                                   ││
│     │  解析树(Parse Tree)                                  ││
│     └─────────────────────────────────────────────────────┘│
│                                                             │
│  2. 分析器(Analyzer)                                       │
│     ┌─────────────────────────────────────────────────────┐│
│     │  语义分析、类型检查、权限检查                        ││
│     │  ↓                                                   ││
│     │  查询树(Query Tree)                                  ││
│     └─────────────────────────────────────────────────────┘│
│                                                             │
│  3. 重写器(Rewriter)                                       │
│     ┌─────────────────────────────────────────────────────┐│
│     │  规则重写、视图展开                                  ││
│     │  ↓                                                   ││
│     │  重写后的查询树                                      ││
│     └─────────────────────────────────────────────────────┘│
│                                                             │
│  4. 规划器(Planner)                                        │
│     ┌─────────────────────────────────────────────────────┐│
│     │  生成执行计划、成本估算                              ││
│     │  ↓                                                   ││
│     │  执行计划(Plan Tree)                                 ││
│     │  ┌────────────────────────────────────────┐        ││
│     │  │  Seq Scan on devices (cost=0.00..1.01) │        ││
│     │  │  Filter: (room = '101'::text)          │        ││
│     │  └────────────────────────────────────────┘        ││
│     └─────────────────────────────────────────────────────┘│
│                                                             │
│  5. 执行器(Executor)                                       │
│     ┌─────────────────────────────────────────────────────┐│
│     │  执行计划、返回结果                                  ││
│     └─────────────────────────────────────────────────────┘│
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 存储结构

```
┌─────────────────────────────────────────────────────────────┐
│                      存储结构                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  表空间(Tablespace)                                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  pg_default (默认表空间)                             │   │
│  │  pg_global (系统表空间)                              │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  数据库(Database)                                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  dorm_power                                          │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  表(Table)                                                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  devices                                             │   │
│  │  ┌──────────────────────────────────────────────┐  │   │
│  │  │  数据文件: base/16384/16385                   │  │   │
│  │  │  页面大小: 8KB                                │  │   │
│  │  │  页面数量: N                                  │  │   │
│  │  └──────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  页面(Page)                                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  ┌──────────────────────────────────────────────┐  │   │
│  │  │  Page Header (24 bytes)                      │  │   │
│  │  ├──────────────────────────────────────────────┤  │   │
│  │  │  Item Pointers (4 bytes each)                │  │   │
│  │  ├──────────────────────────────────────────────┤  │   │
│  │  │  Free Space                                   │  │   │
│  │  ├──────────────────────────────────────────────┤  │   │
│  │  │  Tuple Data                                   │  │   │
│  │  │  ┌──────────────────────────────────────┐   │  │   │
│  │  │  │ id | name | room | online | ...      │   │  │   │
│  │  │  └──────────────────────────────────────┘   │  │   │
│  │  └──────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 7. PostgreSQL 面试要点

### 7.1 基础问题

#### Q1: PostgreSQL 的 MVCC 是什么？

**答案**：
- MVCC（Multi-Version Concurrency Control）多版本并发控制
- 每行数据包含 xmin 和 xmax 两个隐藏字段
- xmin：创建该行版本的事务ID
- xmax：删除/更新该行版本的事务ID
- 读操作不阻塞写操作，写操作不阻塞读操作
- 提高并发性能

#### Q2: PostgreSQL 的 WAL 是什么？

**答案**：
- WAL（Write-Ahead Logging）预写式日志
- 数据修改前先写入WAL日志
- 保证数据的持久性和一致性
- 支持崩溃恢复和时间点恢复（PITR）

#### Q3: PostgreSQL 的索引类型有哪些？

**答案**：
1. **B-Tree**：默认索引，支持等值、范围、排序查询
2. **Hash**：哈希索引，仅支持等值查询
3. **GiST**：通用搜索树，支持几何、全文搜索
4. **GIN**：倒排索引，支持数组、JSONB、全文搜索
5. **SP-GiST**：空间分区索引
6. **BRIN**：块范围索引，适合大表

### 7.2 进阶问题

#### Q4: PostgreSQL 和 MySQL 的区别？

**答案**：

| 对比维度 | PostgreSQL | MySQL |
|----------|------------|-------|
| **架构** | 进程模型 | 线程模型 |
| **MVCC** | 无需回滚段 | 需要回滚段 |
| **JSON** | jsonb（二进制） | json（文本） |
| **扩展性** | 极强（扩展模块） | 中等 |
| **全文搜索** | 内置 | 需要插件 |
| **地理信息** | PostGIS | 需要插件 |
| **窗口函数** | 完整支持 | 8.0+支持 |
| **适用场景** | 复杂查询、企业级 | Web应用、简单查询 |

#### Q5: 如何优化 PostgreSQL 性能？

**答案**：

1. **查询优化**：
   - 使用 EXPLAIN ANALYZE 分析查询
   - 避免 SELECT *
   - 使用覆盖索引

2. **索引优化**：
   - 选择合适的索引类型
   - 创建复合索引
   - 删除未使用的索引

3. **配置优化**：
   - shared_buffers：系统内存的25%
   - work_mem：排序、哈希内存
   - maintenance_work_mem：维护操作内存
   - effective_cache_size：系统可用缓存

4. **连接池**：
   - 使用 PgBouncer 或应用层连接池
   - 减少连接创建开销

#### Q6: PostgreSQL 的 VACUUM 是什么？

**答案**：

- MVCC 产生的死元组需要清理
- VACUUM 清理死元组，释放空间
- VACUUM FULL 重建表，释放磁盘空间
- AUTOVACUUM 自动清理
- 防止事务ID回卷

```sql
-- 手动 VACUUM
VACUUM devices;

-- 完整 VACUUM（阻塞）
VACUUM FULL devices;

-- 分析统计信息
ANALYZE devices;

-- VACUUM + ANALYZE
VACUUM ANALYZE devices;
```

---

## 总结

PostgreSQL 是一个功能强大、可靠性高的开源关系型数据库，掌握其核心特性和最佳实践对于构建企业级应用至关重要。本文档涵盖了 PostgreSQL 的基础概念、核心特性、数据类型、高级功能、性能优化和原理分析，为实际开发提供参考。

---

**文档版本**：v1.0  
**编写日期**：2026年3月14日  
**作者**：DormPower Team
