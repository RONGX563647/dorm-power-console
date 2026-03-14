# Redis 学习文档

## 目录
- [1. Redis 基础概念](#1-redis-基础概念)
- [2. Redis 数据结构](#2-redis-数据结构)
- [3. Redis 核心特性](#3-redis-核心特性)
- [4. Redis 应用场景](#4-redis-应用场景)
- [5. Redis 原理分析](#5-redis-原理分析)
- [6. Redis 性能优化](#6-redis-性能优化)
- [7. Redis 面试要点](#7-redis-面试要点)

---

## 1. Redis 基础概念

### 1.1 什么是 Redis

Redis（Remote Dictionary Server）是一个开源的、基于内存的数据结构存储系统，可以用作：
- **数据库**：持久化存储数据
- **缓存**：加速数据访问
- **消息队列**：支持发布订阅模式

### 1.2 Redis 特点

| 特性 | 说明 |
|------|------|
| **高性能** | 基于内存操作，读写速度极快（10万+ QPS） |
| **数据结构丰富** | 支持 String、Hash、List、Set、ZSet、Stream 等 |
| **持久化** | 支持 RDB 和 AOF 两种持久化方式 |
| **原子性** | 所有操作都是原子性的 |
| **支持过期** | 可设置 key 的过期时间 |
| **发布订阅** | 支持消息发布订阅模式 |
| **事务** | 支持简单的事务操作 |
| **高可用** | 支持主从复制、哨兵模式、集群模式 |

### 1.3 Redis 与其他数据库对比

| 对比维度 | Redis | MySQL | MongoDB | Memcached |
|---------|-------|-------|---------|-----------|
| **存储方式** | 内存+持久化 | 磁盘 | 磁盘 | 内存 |
| **数据结构** | 丰富 | 关系表 | 文档 | 简单KV |
| **性能** | 极高 | 中等 | 高 | 高 |
| **持久化** | 支持 | 原生支持 | 支持 | 不支持 |
| **事务** | 简单事务 | ACID | 4.0+支持 | 不支持 |
| **适用场景** | 缓存/会话/队列 | 业务数据 | 文档存储 | 简单缓存 |

---

## 2. Redis 数据结构

### 2.1 String（字符串）

**底层实现**：SDS（Simple Dynamic String）

```redis
# 设置值
SET key value
SET user:1 "张三"
SETEX session:token 3600 "abc123"  # 设置并指定过期时间

# 获取值
GET key
GET user:1

# 计数器
INCR page:views    # 自增1
INCRBY stock:1001 10  # 自增指定值
DECR stock:1001    # 自减1

# 批量操作
MSET key1 value1 key2 value2
MGET key1 key2

# 追加
APPEND key "suffix"

# 获取长度
STRLEN key
```

**应用场景**：
- 缓存对象（JSON 序列化）
- 计数器（点赞数、访问量）
- 分布式锁
- Session 共享

### 2.2 Hash（哈希）

**底层实现**：ziplist（压缩列表）或 hashtable（哈希表）

```redis
# 设置字段
HSET user:1 name "张三" age 20 email "zhangsan@example.com"

# 获取字段
HGET user:1 name
HGETALL user:1

# 批量设置
HMSET user:2 name "李四" age 22

# 判断字段是否存在
HEXISTS user:1 name

# 删除字段
HDEL user:1 email

# 自增
HINCRBY user:1 age 1

# 获取所有字段名/值
HKEYS user:1
HVALS user:1
```

**应用场景**：
- 存储对象（比 String 更节省内存）
- 购物车
- 用户信息缓存

### 2.3 List（列表）

**底层实现**：quicklist（快速列表，ziplist + linkedlist）

```redis
# 左侧插入
LPUSH queue:task task1 task2 task3

# 右侧插入
RPUSH queue:task task4

# 弹出
LPOP queue:task    # 左侧弹出
RPOP queue:task    # 右侧弹出

# 阻塞弹出
BLPOP queue:task 5  # 阻塞5秒

# 获取列表元素
LRANGE queue:task 0 -1  # 获取所有
LINDEX queue:task 0     # 获取指定索引

# 获取长度
LLEN queue:task

# 删除元素
LREM queue:task 2 "task1"  # 删除2个task1
```

**应用场景**：
- 消息队列
- 最新列表（最新文章、最新评论）
- 分页查询

### 2.4 Set（集合）

**底层实现**：intset（整数集合）或 hashtable

```redis
# 添加元素
SADD tags:article:1 redis java spring

# 获取所有元素
SMEMBERS tags:article:1

# 判断元素是否存在
SISMEMBER tags:article:1 redis

# 删除元素
SREM tags:article:1 java

# 集合运算
SADD set1 a b c
SADD set2 b c d

SINTER set1 set2    # 交集 {b, c}
SUNION set1 set2    # 并集 {a, b, c, d}
SDIFF set1 set2     # 差集 {a}

# 随机获取
SRANDMEMBER tags:article:1 2  # 随机获取2个

# 计数
SCARD tags:article:1
```

**应用场景**：
- 标签系统
- 共同好友
- 抽奖系统
- 点赞/收藏（去重）

### 2.5 ZSet（有序集合）

**底层实现**：ziplist 或 skiplist（跳表）+ dict

```redis
# 添加元素
ZADD leaderboard 100 user1 200 user2 150 user3

# 获取排名（从小到大）
ZRANK leaderboard user1    # 排名
ZRANGE leaderboard 0 9 WITHSCORES  # 前10名

# 获取排名（从大到小）
ZREVRANK leaderboard user1
ZREVRANGE leaderboard 0 9 WITHSCORES

# 获取分数范围
ZRANGEBYSCORE leaderboard 100 200 WITHSCORES

# 增加分数
ZINCRBY leaderboard 50 user1

# 删除元素
ZREM leaderboard user1

# 计数
ZCARD leaderboard
ZCOUNT leaderboard 100 200  # 分数范围内的数量
```

**应用场景**：
- 排行榜
- 延时队列（时间戳作为分数）
- 范围查询

### 2.6 其他数据结构

#### Bitmap（位图）

```redis
# 设置位
SETBIT user:sign:2024:01 0 1  # 第1天签到

# 获取位
GETBIT user:sign:2024:01 0

# 统计
BITCOUNT user:sign:2024:01  # 统计签到天数

# 位运算
BITOP AND result key1 key2
```

**应用场景**：签到统计、在线状态、布隆过滤器

#### HyperLogLog（基数统计）

```redis
PFADD uv:page:1 user1 user2 user3
PFCOUNT uv:page:1  # 统计独立访客数
PFMERGE uv:all uv:page:1 uv:page:2  # 合并
```

**应用场景**：UV 统计（误差约 0.81%）

#### Geo（地理位置）

```redis
GEOADD locations 116.404 39.915 "北京" 121.474 31.230 "上海"
GEODIST locations "北京" "上海" km  # 计算距离
GEORADIUS locations 116.404 39.915 100 km  # 查询附近
```

**应用场景**：附近的人、位置推荐

#### Stream（流）

```redis
XADD mystream * field1 value1
XRANGE mystream - +
XREAD COUNT 2 STREAMS mystream 0
```

**应用场景**：消息队列、事件流

---

## 3. Redis 核心特性

### 3.1 持久化

#### RDB（Redis Database）

**原理**：在指定时间间隔内将内存中的数据集快照写入磁盘

```
# redis.conf 配置
save 900 1      # 900秒内至少1次修改
save 300 10     # 300秒内至少10次修改
save 60 10000   # 60秒内至少10000次修改
dbfilename dump.rdb
dir ./
```

**优点**：
- 文件紧凑，适合备份
- 恢复速度快
- 对性能影响小

**缺点**：
- 可能丢失最后一次快照后的数据
- 数据量大时 fork 耗时

#### AOF（Append Only File）

**原理**：记录所有写操作命令，追加到文件末尾

```
# redis.conf 配置
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec  # 同步策略
no-appendfsync-on-rewrite no
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
```

**同步策略**：
| 策略 | 说明 | 性能 | 安全性 |
|------|------|------|--------|
| always | 每次写入都同步 | 低 | 高 |
| everysec | 每秒同步一次 | 中 | 中 |
| no | 由操作系统决定 | 高 | 低 |

**优点**：
- 数据更安全，最多丢失1秒数据
- 可读性好，易于分析

**缺点**：
- 文件体积大
- 恢复速度慢
- 对性能影响较大

#### 混合持久化（Redis 4.0+）

```
aof-use-rdb-preamble yes
```

结合 RDB 和 AOF 的优点：
- RDB 快速恢复
- AOF 保证数据完整性

### 3.2 过期策略

#### 定期删除

每隔一段时间随机检查部分 key，删除过期的 key

```
# redis.conf
hz 10  # 每秒执行10次过期检查
```

#### 惰性删除

访问 key 时检查是否过期，过期则删除

#### 内存淘汰策略

当内存不足时，如何处理新写入请求：

| 策略 | 说明 |
|------|------|
| noeviction | 不淘汰，写入报错（默认） |
| allkeys-lru | 从所有 key 中淘汰最近最少使用的 |
| volatile-lru | 从设置了过期时间的 key 中淘汰 LRU |
| allkeys-lfu | 从所有 key 中淘汰最不常用的 |
| volatile-lfu | 从设置了过期时间的 key 中淘汰 LFU |
| allkeys-random | 从所有 key 中随机淘汰 |
| volatile-random | 从设置了过期时间的 key 中随机淘汰 |
| volatile-ttl | 淘汰即将过期的 key |

```
# redis.conf
maxmemory 2gb
maxmemory-policy allkeys-lru
```

### 3.3 事务

```redis
MULTI        # 开启事务
SET key1 value1
SET key2 value2
EXEC         # 执行事务
DISCARD      # 取消事务
```

**注意**：
- Redis 事务不支持回滚
- 事务中的命令按顺序执行
- 不会中断其他客户端的命令

### 3.4 发布订阅

```redis
# 订阅
SUBSCRIBE channel1 channel2

# 发布
PUBLISH channel1 "message"

# 模式订阅
PSUBSCRIBE news:*
```

**缺点**：
- 消息不持久化
- 客户端离线时消息丢失

### 3.5 主从复制

```
# 从节点配置
replicaof <masterip> <masterport>
replica-serve-stale-data yes
replica-read-only yes
```

**复制流程**：
1. 从节点连接主节点
2. 主节点执行 BGSAVE 生成 RDB
3. 主节点将 RDB 发送给从节点
4. 从节点加载 RDB
5. 主节点持续发送写命令

**特点**：
- 异步复制
- 一主多从
- 读写分离

### 3.6 哨兵模式

```
# sentinel.conf
sentinel monitor mymaster 192.168.1.1 6379 2
sentinel down-after-milliseconds mymaster 30000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 180000
```

**功能**：
- 监控：检查主从节点是否正常
- 通知：通知客户端主节点变化
- 自动故障转移：主节点故障时选举新主节点

### 3.7 集群模式

```
# redis.conf
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
cluster-require-full-coverage yes
```

**特点**：
- 数据分片（16384 个槽位）
- 分布式存储
- 高可用

---

## 4. Redis 应用场景

### 4.1 缓存

**场景**：加速数据访问，减轻数据库压力

```java
// Spring Cache 示例
@Cacheable(value = "user", key = "#id")
public User getUserById(Long id) {
    return userRepository.findById(id);
}

@CacheEvict(value = "user", key = "#id")
public void deleteUser(Long id) {
    userRepository.deleteById(id);
}
```

**缓存策略**：
- **Cache Aside**：先查缓存，未命中则查数据库并写入缓存
- **Read Through**：缓存未命中时由缓存组件查询数据库
- **Write Through**：写入时同时更新缓存和数据库
- **Write Behind**：写入缓存，异步写入数据库

**缓存问题**：

| 问题 | 解决方案 |
|------|----------|
| 缓存穿透 | 布隆过滤器、缓存空值 |
| 缓存击穿 | 加锁、热点数据永不过期 |
| 缓存雪崩 | 过期时间随机、多级缓存 |
| 缓存一致性 | 延时双删、订阅 binlog |

### 4.2 分布式锁

```java
// Redisson 分布式锁示例
RLock lock = redisson.getLock("lock:resource");
try {
    if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
        // 执行业务逻辑
    }
} finally {
    lock.unlock();
}
```

**实现原理**：
```redis
# 加锁
SET lock:resource value NX PX 30000

# 解锁（Lua脚本保证原子性）
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```

### 4.3 限流

```java
// 滑动窗口限流
public boolean tryAcquire(String key, long maxRequests, long windowMs) {
    long now = System.currentTimeMillis();
    long windowStart = now - windowMs;
    
    // Lua脚本保证原子性
    String script = 
        "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1]) " +
        "local count = redis.call('ZCARD', KEYS[1]) " +
        "if count < tonumber(ARGV[2]) then " +
        "    redis.call('ZADD', KEYS[1], ARGV[3], ARGV[3]) " +
        "    redis.call('PEXPIRE', KEYS[1], ARGV[4]) " +
        "    return 1 " +
        "else " +
        "    return 0 " +
        "end";
    
    Long result = redisTemplate.execute(
        new DefaultRedisScript<>(script, Long.class),
        Collections.singletonList(key),
        String.valueOf(windowStart),
        String.valueOf(maxRequests),
        String.valueOf(now),
        String.valueOf(windowMs)
    );
    
    return result != null && result == 1;
}
```

### 4.4 消息队列

#### List 实现队列

```redis
# 生产者
LPUSH queue:task task1

# 消费者
RPOP queue:task
BRPOP queue:task 5  # 阻塞获取
```

#### Stream 实现消息队列

```redis
# 生产者
XADD mystream * field1 value1

# 消费者
XREAD GROUP mygroup consumer1 COUNT 1 STREAMS mystream >
```

### 4.5 排行榜

```redis
# 添加分数
ZADD leaderboard 100 user1 200 user2

# 获取排名
ZREVRANGE leaderboard 0 9 WITHSCORES

# 更新分数
ZINCRBY leaderboard 50 user1
```

### 4.6 计数器

```redis
# 文章阅读量
INCR article:views:1

# 点赞数
INCR like:article:1

# 库存扣减
DECR stock:product:1
```

### 4.7 会话缓存

```redis
# 存储会话
SETEX session:token123 3600 '{"userId":1,"username":"admin"}'

# 获取会话
GET session:token123
```

### 4.8 社交网络

```redis
# 关注关系
SADD user:1:following user2 user3
SADD user:2:followers user1

# 共同关注
SINTER user:1:following user:2:following

# 可能认识的人
SDIFF user:2:following user:1:following
```

---

## 5. Redis 原理分析

### 5.1 为什么 Redis 快

#### 1. 基于内存操作

- 内存访问速度约 100ns
- 磁盘访问速度约 10ms
- 差距约 10 万倍

#### 2. 单线程模型

**为什么单线程还能快？**
- 避免线程切换开销
- 避免锁竞争
- CPU 不是瓶颈，内存和网络才是

**单线程如何处理并发？**
- IO 多路复用（epoll）
- 事件循环机制

#### 3. IO 多路复用

```
┌─────────────────────────────────────────────────────────┐
│                    Redis 事件循环                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────┐                                       │
│  │  IO 多路复用  │ ← epoll/select/kqueue                │
│  │   (epoll)    │                                       │
│  └──────┬───────┘                                       │
│         │                                               │
│         ▼                                               │
│  ┌──────────────────────────────────────────┐          │
│  │            文件事件处理器                  │          │
│  │  ┌──────────┐  ┌──────────┐  ┌─────────┐│          │
│  │  │ 连接应答 │  │ 命令请求 │  │ 命令回复 ││          │
│  │  │ 处理器   │  │ 处理器   │  │ 处理器   ││          │
│  │  └──────────┘  └──────────┘  └─────────┘│          │
│  └──────────────────────────────────────────┘          │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 4. 高效的数据结构

- SDS：O(1) 获取长度
- 哈希表：O(1) 查找
- 跳表：O(logN) 查找
- 压缩列表：节省内存

### 5.2 数据结构底层实现

#### SDS（Simple Dynamic String）

```c
struct sdshdr {
    int len;      // 已使用长度
    int free;     // 剩余空间
    char buf[];   // 字节数组
};
```

**优点**：
- O(1) 获取长度
- 避免缓冲区溢出
- 减少内存重分配次数
- 二进制安全

#### 跳表（Skip List）

```
Level 4:    1 ───────────────────────> 21
Level 3:    1 ─────────> 11 ────────> 21
Level 2:    1 ───> 6 ──> 11 ───> 17 ─> 21
Level 1:    1 ─> 4 ─> 6 ─> 11 ─> 15 ─> 17 ─> 21
Level 0:    1 ─> 3 ─> 4 ─> 6 ─> 9 ─> 11 ─> 13 ─> 15 ─> 17 ─> 19 ─> 21
```

**特点**：
- 多层索引
- 查找复杂度 O(logN)
- 实现简单，支持范围查询

#### 压缩列表（ziplist）

连续内存块，节省内存：

```
┌────────┬────────┬────────┬────────┬────────┬────────┐
│ zlbytes│ zltail │  zllen │ entry1 │ entry2 │ zlend  │
│  4字节 │  4字节 │  2字节 │  变长  │  变长  │  1字节 │
└────────┴────────┴────────┴────────┴────────┴────────┘
```

### 5.3 内存回收

#### 引用计数

```c
typedef struct redisObject {
    unsigned type:4;
    unsigned encoding:4;
    unsigned lru:LRU_BITS;
    int refcount;    // 引用计数
    void *ptr;
} robj;
```

#### 内存分配器

- jemalloc（默认）
- tcmalloc
- libc malloc

### 5.4 过期键删除原理

#### 过期字典

```c
typedef struct redisDb {
    dict *dict;       // 键空间
    dict *expires;    // 过期字典
} redisDb;
```

#### 删除策略

1. **定时删除**：创建定时器，到期立即删除
   - 优点：及时释放内存
   - 缺点：CPU 开销大

2. **惰性删除**：访问时检查是否过期
   - 优点：CPU 开销小
   - 缺点：可能内存泄漏

3. **定期删除**：定期随机检查并删除
   - 平衡 CPU 和内存

**Redis 采用：惰性删除 + 定期删除**

---

## 6. Redis 性能优化

### 6.1 内存优化

#### 1. 选择合适的数据结构

| 场景 | 推荐结构 | 原因 |
|------|----------|------|
| 少量字段对象 | Hash | ziplist 编码节省内存 |
| 多字段对象 | String | 避免 Hash 编码转换 |
| 小整数集合 | Set | intset 编码 |
| 有序数据 | ZSet | 跳表 + ziplist |

#### 2. 使用压缩编码

```
# redis.conf
hash-max-ziplist-entries 512
hash-max-ziplist-value 64
list-max-ziplist-size -2
set-max-intset-entries 512
zset-max-ziplist-entries 128
zset-max-ziplist-value 64
```

#### 3. 共享对象

Redis 内置共享对象池：
- 0-9999 的整数
- 复用时直接引用，不创建新对象

### 6.2 网络优化

#### 1. Pipeline

```java
// 批量执行命令
Pipeline pipeline = jedis.pipelined();
for (int i = 0; i < 1000; i++) {
    pipeline.set("key" + i, "value" + i);
}
pipeline.sync();
```

#### 2. Lua 脚本

```java
// 原子性执行多个命令
String script = 
    "local current = redis.call('GET', KEYS[1]) " +
    "if current == ARGV[1] then " +
    "    redis.call('SET', KEYS[1], ARGV[2]) " +
    "    return 1 " +
    "else " +
    "    return 0 " +
    "end";
jedis.eval(script, 1, "key", "old", "new");
```

### 6.3 持久化优化

#### RDB 优化

```
save 900 1
save 300 10
save 60 10000

# 减少fork频率
stop-writes-on-bgsave-error yes
rdbcompression yes
rdbchecksum yes
```

#### AOF 优化

```
appendonly yes
appendfsync everysec

# AOF重写优化
no-appendfsync-on-rewrite no
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
```

### 6.4 集群优化

#### 分片策略

- 根据 key 的业务类型分片
- 避免热点 key
- 使用 hash tag 保证相关 key 在同一节点

```
# hash tag 示例
user:{1001}:profile
user:{1001}:settings
```

#### 连接池配置

```java
JedisPoolConfig config = new JedisPoolConfig();
config.setMaxTotal(200);
config.setMaxIdle(50);
config.setMinIdle(10);
config.setMaxWaitMillis(3000);
config.setTestOnBorrow(true);
```

---

## 7. Redis 面试要点

### 7.1 基础问题

#### Q1: Redis 为什么快？

**答案**：
1. 基于内存操作，访问速度快
2. 单线程模型，避免线程切换和锁竞争
3. IO 多路复用，高效处理并发连接
4. 高效的数据结构（SDS、跳表、压缩列表等）

#### Q2: Redis 单线程为什么还能处理高并发？

**答案**：
- Redis 是单线程处理命令，但 IO 多路复用可以同时监听多个连接
- CPU 不是瓶颈，内存和网络才是
- 单线程避免了多线程的锁竞争和上下文切换开销

#### Q3: Redis 有哪些数据结构？底层实现是什么？

**答案**：

| 数据结构 | 底层实现 |
|----------|----------|
| String | SDS |
| Hash | ziplist / hashtable |
| List | quicklist |
| Set | intset / hashtable |
| ZSet | ziplist / skiplist + dict |

### 7.2 持久化问题

#### Q4: RDB 和 AOF 的区别？

**答案**：

| 对比维度 | RDB | AOF |
|----------|-----|-----|
| 存储内容 | 数据快照 | 写命令 |
| 文件大小 | 小 | 大 |
| 恢复速度 | 快 | 慢 |
| 数据安全 | 可能丢失分钟级数据 | 最多丢失1秒 |
| 性能影响 | fork 时有影响 | 持续影响 |

#### Q5: 如何选择持久化策略？

**答案**：
- 只用于缓存：可以不持久化
- 允许丢失少量数据：RDB
- 不允许丢失数据：AOF
- 综合考虑：RDB + AOF 混合持久化

### 7.3 集群问题

#### Q6: Redis 主从复制的原理？

**答案**：
1. 从节点连接主节点，发送 SYNC 命令
2. 主节点执行 BGSAVE 生成 RDB 快照
3. 主节点将 RDB 发送给从节点
4. 从节点加载 RDB 到内存
5. 主节点持续发送写命令给从节点

#### Q7: 哨兵模式的作用？

**答案**：
- **监控**：检查主从节点是否正常
- **通知**：通知客户端主节点变化
- **自动故障转移**：主节点故障时选举新主节点

#### Q8: Redis 集群的数据分片原理？

**答案**：
- 使用 16384 个槽位（slot）
- 每个节点负责一部分槽位
- key 通过 CRC16(key) % 16384 计算所属槽位

### 7.4 应用问题

#### Q9: 如何解决缓存穿透？

**答案**：
1. **缓存空值**：查询为空时也缓存，设置较短过期时间
2. **布隆过滤器**：提前过滤不存在的 key

#### Q10: 如何解决缓存击穿？

**答案**：
1. **加锁**：查询数据库时加分布式锁
2. **热点数据永不过期**：逻辑过期，后台异步更新

#### Q11: 如何解决缓存雪崩？

**答案**：
1. **过期时间随机**：避免同时过期
2. **多级缓存**：本地缓存 + Redis
3. **熔断降级**：缓存失效时降级处理

#### Q12: 如何保证缓存和数据库一致性？

**答案**：
1. **延时双删**：先删缓存，再更新数据库，延时后再删缓存
2. **订阅 binlog**：通过 Canal 等工具订阅 MySQL binlog 更新缓存
3. **设置过期时间**：最终一致性保证

### 7.5 分布式锁问题

#### Q13: 如何实现分布式锁？

**答案**：

```redis
# 加锁
SET lock:resource value NX PX 30000

# 解锁（Lua脚本）
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```

#### Q14: 分布式锁可能存在什么问题？

**答案**：
1. **锁超时**：业务执行时间超过锁过期时间
   - 解决：看门狗机制自动续期
2. **主从切换**：主节点加锁后宕机，从节点未同步
   - 解决：Redlock 算法
3. **误删锁**：删除了其他线程的锁
   - 解决：Lua 脚本保证原子性

### 7.6 高级问题

#### Q15: Redis 的内存淘汰策略有哪些？

**答案**：
- noeviction：不淘汰，写入报错
- allkeys-lru：淘汰最近最少使用的 key
- volatile-lru：淘汰设置了过期时间的 LRU key
- allkeys-lfu：淘汰最不常用的 key
- volatile-lfu：淘汰设置了过期时间的 LFU key
- allkeys-random：随机淘汰
- volatile-random：随机淘汰有过期时间的 key
- volatile-ttl：淘汰即将过期的 key

#### Q16: 如何优化 Redis 内存使用？

**答案**：
1. 选择合适的数据结构
2. 使用压缩编码（ziplist、intset）
3. 设置合理的过期时间
4. 避免大 key（拆分）
5. 使用共享对象池

#### Q17: Redis 如何实现限流？

**答案**：
1. **计数器算法**：INCR + EXPIRE
2. **滑动窗口算法**：ZSET + Lua 脚本
3. **令牌桶算法**：Lua 脚本实现

#### Q18: Redis 如何实现消息队列？

**答案**：
1. **List**：LPUSH + BRPOP
2. **Pub/Sub**：不支持持久化
3. **Stream**：支持消费组、持久化

---

## 总结

Redis 是一个功能强大、性能优异的内存数据库，掌握其核心原理和应用场景对于构建高性能系统至关重要。本文档涵盖了：

1. **基础概念**：Redis 的特点和优势
2. **数据结构**：5 种基础结构 + 4 种高级结构
3. **核心特性**：持久化、过期策略、事务、集群
4. **应用场景**：缓存、锁、限流、队列等
5. **原理分析**：为什么快、底层实现
6. **性能优化**：内存、网络、持久化优化
7. **面试要点**：常见问题和答案

建议结合实际项目实践，深入理解 Redis 的使用和原理。

---

**文档版本**：v1.0  
**编写日期**：2026年3月14日  
**作者**：DormPower Team
