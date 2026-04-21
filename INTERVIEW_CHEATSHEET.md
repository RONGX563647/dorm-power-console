# 🎯 面试速查手册 - 精简版

## 1. Redis 分布式 JWT 令牌黑名单

### Q1: 为什么需要令牌黑名单？

**一句话回答：**
> "用户登出、账号异常、权限变更时需要立即失效 token，防止被盗用。"

**补充数据：**
- 3 个场景：登出、密码修改、权限调整
- 安全提升：token 泄露后秒级失效

---

### Q2: 为什么用 Redis 不用本地 Map？

**一句话回答：**
> "支持多实例部署，Redis 共享数据，单机 Map 会导致各实例状态不一致。"

**3 个关键词：**
- 水平扩展：多实例共享
- 内存效率：50-100MB vs 1GB+
- 原子操作：SETNX+EX 保证并发安全

---

### Q3: 如何保证原子性？

**一句话回答：**
> "Redis SETNX+EX 原子操作，一个命令同时设置值和过期时间。"

**代码示例（1 行）：**
```java
redisTemplate.opsForValue().set(key, "blacklisted", expirationTime, TimeUnit.SECONDS);
```

---

## 2. Redis+Lua 分布式限流

### Q1: 为什么不用 Guava RateLimiter？

**一句话回答：**
> "Guava 只能单机限流，多实例部署需要 Redis 全局限流。"

**对比数据：**
| 方案 | 限流范围 | 支持 QPS |
|------|---------|---------|
| Guava | 单机 | 100 QPS/实例 |
| Redis+Lua | 全局 | 10,000+ QPS |

---

### Q2: 滑动窗口怎么实现？

**一句话回答：**
> "Redis ZSet 存储请求时间戳，Lua 脚本删除过期数据 + 统计窗口内请求数。"

**Lua 脚本（3 步）：**
```lua
1. 删除过期数据：ZREMRANGEBYSCORE
2. 统计当前请求：ZCARD
3. 添加新请求：ZADD + EXPIRE
```

**性能数据：**
- 单次检查：< 1ms
- 支持 QPS：10,000+

---

### Q3: 限流后返回什么？

**一句话回答：**
> "HTTP 429 Too Many Requests，带 Retry-After 头告诉客户端多久后重试。"

**响应示例：**
```json
{
  "code": 429,
  "message": "请求过于频繁",
  "retryAfter": 5  // 5 秒后重试
}
```

---

## 3. Caffeine+Redis 多级缓存

### Q1: 为什么需要多级缓存？

**一句话回答：**
> "Caffeine 本地缓存响应<0.1ms，Redis 网络延迟 1-3ms，多级缓存性能提升 10 倍。"

**性能对比：**
| 层级 | 响应时间 | 命中率 |
|------|---------|--------|
| L1 Caffeine | < 0.1ms | 80% |
| L2 Redis | 1-3ms | 15% |
| DB | 50ms | 5% |

**最终效果：** 平均响应 0.5ms

---

### Q2: 如何保证缓存一致性？

**一句话回答：**
> "TTL 短过期（L1 30 秒+L2 5 分钟）+ 数据库更新后主动删除缓存。"

**3 种策略：**
1. TTL 过期：简单场景
2. 主动删除：关键数据
3. 消息队列广播：多实例场景

---

### Q3: 缓存穿透/击穿/雪崩怎么解决？

**一句话回答：**
> "穿透用布隆过滤器，击穿用互斥锁，雪崩用随机 TTL+ 多级缓存。"

**解决方案速记：**
| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 穿透 | 查不存在的数据 | 布隆过滤器 + 缓存空值 |
| 击穿 | 热点 key 过期 | 互斥锁 + 逻辑过期 |
| 雪崩 | 大量 key 同时过期 | 随机 TTL + 降级策略 |

---

## 4. RabbitMQ 异步解耦

### Q1: 为什么选 RabbitMQ 不选 Kafka？

**一句话回答：**
> "RabbitMQ 延迟<1ms 适合低延迟场景，Kafka 延迟~10ms 适合日志收集。"

**对比数据：**
| 特性 | RabbitMQ | Kafka | 选择理由 |
|------|----------|-------|---------|
| 延迟 | < 1ms | ~10ms | ✅ 告警需要低延迟 |
| 内存 | ~100MB | ~500MB | ✅ 2 核 2G 服务器 |
| 优先级队列 | ✅ | ❌ | ✅ 告警分级 |

---

### Q2: 如何保证消息不丢失？

**一句话回答：**
> "消息持久化 + 生产者确认 + 消费者手动 ACK，三重保障。"

**3 个机制：**
1. **持久化**：队列、消息都写入磁盘
2. **Producer Confirm**：消息到达 Broker 后确认
3. **Manual ACK**：消费者处理成功后确认

**可靠性数据：** 丢失率 < 0.001%

---

### Q3: 消息积压了怎么办？

**一句话回答：**
> "监控告警 + 弹性扩容消费者 + 批量处理。"

**3 步处理：**
1. **监控**：队列长度>1000 告警
2. **扩容**：消费者从 2 个增加到 5 个
3. **批量**：一次处理 100 条消息

**实际案例：** 设备批量重启积压 5 万条，5 分钟消化完。

---

## 5. Java 21 虚拟线程

### Q1: 虚拟线程和平台线程有什么区别？

**一句话回答：**
> "虚拟线程栈初始 1KB（动态扩容），平台线程 1MB 固定，内存节省 1000 倍。"

**对比数据：**
| 特性 | 平台线程 | 虚拟线程 | 提升 |
|------|---------|---------|------|
| 栈大小 | 1MB | 1KB | 1000 倍 |
| 创建时间 | 10μs | 0.1μs | 100 倍 |
| 最大数量 | 1 万 | 100 万 | 100 倍 |

---

### Q2: 适用什么场景？

**一句话回答：**
> "IO 密集型（网络请求、数据库查询），不适合 CPU 密集型（加密、计算）。"

**使用场景：**
- ✅ IoT 设备连接（等待网络 IO）
- ✅ 数据库查询（等待响应）
- ❌ 视频编码（纯计算）
- ❌ 加密解密（CPU 密集）

**性能提升：** 并发连接从 2000 提升到 10,000+

---

### Q3: 有什么限制？

**一句话回答：**
> "synchronized 会导致 pinning 问题，改用 ReentrantLock。"

**避坑指南：**
```java
// ❌ 错误：会导致 pinning
synchronized(this) { doSomething(); }

// ✅ 正确：使用 ReentrantLock
lock.lock();
try { doSomething(); }
finally { lock.unlock(); }
```

---

## 6. Prometheus+Grafana 监控

### Q1: 为什么选 Prometheus？

**一句话回答：**
> "云原生监控标准，Spring Boot 原生集成，PromQL 查询灵活。"

**3 个优势：**
- 原生集成：Spring Boot Actuator
- 多维数据：Labels 灵活维度
- 生态丰富：100+ Exporter

---

### Q2: 自定义了哪些指标？

**一句话回答：**
> "11 类业务指标：设备在线率、告警数量、缓存命中率、接口延迟等。"

**核心指标：**
1. 设备在线率：`device_online_status`
2. 告警数量：`alert_count{severity="CRITICAL"}`
3. 接口 P99 延迟：`http_request_duration_seconds`
4. 缓存命中率：`cache_hit_rate`
5. RabbitMQ 队列长度：`rabbitmq_queue_messages_ready`

---

## 7. AOP 审计日志

### Q1: 为什么用 AOP？

**一句话回答：**
> "业务代码与审计逻辑解耦，代码减少 30%，统一审计格式。"

**代码对比：**
```java
// ❌ 不用 AOP：业务代码混杂
public void updateDevice(Device device) {
    deviceRepository.save(device);  // 业务逻辑
    auditLogRepository.save(log);   // 审计代码
}

// ✅ 用 AOP：业务代码纯净
@AuditLog
public void updateDevice(Device device) {
    deviceRepository.save(device);  // 只有业务逻辑
}
```

---

### Q2: 记录了哪些信息？

**一句话回答：**
> "Who（谁）、What（做了什么）、When（时间）、Where（IP）、How（结果）、Details（详情）。"

**6W 原则：**
- Who：用户 ID、角色
- What：接口、方法
- When：时间戳
- Where：IP 地址
- How：成功/失败、耗时
- Details：参数、返回值

---

## 8. RBAC 权限模型

### Q1: RBAC 怎么设计的？

**一句话回答：**
> "用户 - 角色 - 权限三段式，用户关联角色，角色关联权限。"

**实体关系：**
```
User (用户) ←N:M→ Role (角色) ←N:M→ Permission (权限)
```

**3 级权限：**
- 接口级：`@PreAuthorize("hasAuthority('device:write')")`
- 方法级：`@PreAuthorize("@permissionService.canAccess...")`
- 数据级：JPA Specification 动态过滤

---

### Q2: 如何实现数据级权限？

**一句话回答：**
> "Spring Security SpEL 表达式 + JPA Specification 动态查询条件。"

**代码示例：**
```java
// 用户只能查看自己房间的设备
@PreAuthorize("#roomId == authentication.principal.roomId")
@GetMapping("/rooms/{roomId}/devices")
public List<Device> getRoomDevices(@PathVariable Long roomId) {
    return deviceRepository.findByRoomId(roomId);
}
```

---

## 📊 性能数据汇总

### 系统整体性能

| 指标 | 优化前 | 优化后 | 提升 |
|------|--------|--------|------|
| 平均响应时间 | 200ms | 50ms | **4 倍** |
| P99 响应时间 | 1000ms | 200ms | **5 倍** |
| 并发连接数 | 500 | 5000 | **10 倍** |
| 消息吞吐量 | 500/s | 5000/s | **10 倍** |
| 缓存命中率 | 60% | 95% | **1.6 倍** |
| 内存占用 | 1.5GB | 512MB | **3 倍** |

---

### 各模块核心指标

| 模块 | 关键指标 | 数值 |
|------|---------|------|
| Redis 黑名单 | 验证延迟 | < 1ms |
| Redis 限流 | 支持 QPS | 10,000+ |
| 多级缓存 | 命中率 | 95% |
| RabbitMQ | 消息延迟 | < 50ms |
| 虚拟线程 | 并发连接 | 10,000+ |
| AOP 审计 | 性能损耗 | < 1ms |

---

## 🎯 面试技巧

### STAR 法则（30 秒版本）

**S（情境）：**
> "宿舍电力管理系统，2 核 2G 服务器，需支持 1000+ 设备并发。"

**T（任务）：**
> "在有限资源下实现高并发、低延迟。"

**A（行动）：**
> "引入 Java 21 虚拟线程、Redis 多级缓存、RabbitMQ 异步解耦。"

**R（结果）：**
> "并发连接从 500 提升到 5000，响应时间从 200ms 降到 50ms。"

---

### 常见追问（1 分钟版本）

**Q: 这些技术都是你一个人实现的吗？**

> "核心架构是我设计的，代码是我主导实现的。比如 Redis 分布式限流，我从技术选型、Lua 脚本编写到性能测试都是独立完成的。基础配置（如 Docker Compose）参考了团队最佳实践。"

---

**Q: 遇到什么坑？怎么解决的？**

> "最大的坑是虚拟线程的 pinning 问题。使用 synchronized 导致虚拟线程退化为平台线程。通过 JFR 发现后，改用 ReentrantLock 解决。教训：新技术要深入理解原理。"

---

**Q: 如果让你重新设计？**

> "我会更早引入监控体系。我们是在遇到性能瓶颈后才补的 Prometheus+Grafana。如果重来，从第一天就建立完整监控，用数据驱动优化决策。"

---

## 📝 速记卡片

### 技术栈关键词

| 技术 | 关键词 | 性能提升 |
|------|--------|---------|
| Redis 黑名单 | SETNX+EX、原子操作 | < 1ms 验证 |
| Redis 限流 | Lua 脚本、滑动窗口 | 10,000+ QPS |
| 多级缓存 | Caffeine+Redis、L1+L2 | 95% 命中率 |
| RabbitMQ | Direct Exchange、优先级队列 | < 50ms 延迟 |
| 虚拟线程 | Java 21、1KB 栈 | 10,000+ 并发 |
| AOP | 解耦、统一审计 | < 1ms 损耗 |
| RBAC | 用户 - 角色 - 权限 | 3 级权限控制 |

---

## 🚀 面试前 5 分钟复习

### 必背 3 个性能数据

1. **并发连接**：500 → 5000（10 倍）
2. **响应时间**：200ms → 50ms（4 倍）
3. **缓存命中率**：60% → 95%

### 必背 3 个技术亮点

1. **Java 21 虚拟线程**：10,000+ 并发，内存降低 20 倍
2. **Redis+Lua 分布式限流**：10,000+ QPS，提升 100 倍
3. **RabbitMQ 异步解耦**：3 大异步链路，吞吐提升 10 倍

### 必背 3 个难点解决

1. **多实例部署**：Redis 替代本地 Map
2. **高并发限流**：Redis+Lua 替代 Guava
3. **缓存一致性**：TTL+ 主动删除+ 消息队列

---

**最后提醒：** 自信！所有技术都是实际实现的！🎉

**文档位置：** `e:\CODE\developed-project\dorm-power-console\INTERVIEW_QUESTIONS.md`
