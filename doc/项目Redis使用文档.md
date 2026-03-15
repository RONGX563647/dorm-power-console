# 项目Redis使用文档 v1.0 - 基础版

> **版本**: v1.0  
> **更新日期**: 2026-03-14  
> **作者**: dormpower team  
> **状态**: 基础功能完成

---

## 1. 痛点分析

### 1.1 为什么需要Redis

宿舍电源管理系统是一个典型的IoT应用场景，在未引入Redis前面临以下问题：

| 痛点 | 具体表现 | 影响 |
|------|----------|------|
| **数据库压力大** | 设备状态、系统配置频繁查询 | 数据库QPS过高，响应变慢 |
| **响应延迟高** | 每次请求都查询数据库 | 平均响应时间150ms+ |
| **并发能力弱** | 数据库连接池成为瓶颈 | 系统吞吐量仅500 QPS |
| **无分布式限流** | 无法防止恶意请求 | 系统稳定性差 |
| **无分布式状态** | 多实例部署无法共享状态 | 限流、会话无法统一 |

### 1.2 实测数据（优化前）

```
数据库查询延迟: 50-150ms
平均响应时间: 150ms
系统吞吐量: 500 QPS
数据库QPS: 1000+
```

---

## 2. 解决方案

### 2.1 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                     v1.0 基础架构                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                   客户端层                            │  │
│  │   Web前端  │  移动端App  │  IoT设备                  │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                   │
│                         ▼                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                   应用层                              │  │
│  │              Spring Boot 后端服务                     │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐           │  │
│  │  │Controller│  │ Service  │  │Repository│           │  │
│  │  └──────────┘  └──────────┘  └──────────┘           │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                   │
│         ┌───────────────┼───────────────┐                  │
│         ▼               ▼               ▼                  │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐          │
│  │ PostgreSQL │  │   Redis    │  │   MQTT     │          │
│  │   数据库    │  │  缓存/限流  │  │  设备通信   │          │
│  └────────────┘  └────────────┘  └────────────┘          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 核心功能

| 功能 | 说明 | 解决的问题 |
|------|------|------------|
| **数据缓存** | 热点数据缓存到Redis | 减少数据库压力 |
| **分布式限流** | 滑动窗口限流算法 | 防止恶意请求 |
| **缓存预热** | 启动时加载热点数据 | 减少首次请求延迟 |

---

## 3. 实现细节

### 3.1 Redis缓存配置

**文件**: [RedisCacheConfig.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/config/RedisCacheConfig.java)

**核心代码**:
```java
@Configuration
@EnableCaching
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisCacheConfig {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    private static final Duration DEVICE_STATUS_TTL = Duration.ofMinutes(5);
    private static final Duration TELEMETRY_TTL = Duration.ofMinutes(1);
    
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(DEFAULT_TTL)
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(jsonSerializer))
            .disableCachingNullValues()
            .prefixCacheNameWith("dorm:cache:");

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("deviceStatus", defaultConfig.entryTtl(DEVICE_STATUS_TTL));
        cacheConfigurations.put("telemetry", defaultConfig.entryTtl(TELEMETRY_TTL));
        cacheConfigurations.put("devices", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("systemConfig", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("dataDict", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();
    }
}
```

**设计要点**:
1. **条件装配**: `@ConditionalOnProperty` 确保Redis可用时才启用
2. **JSON序列化**: 使用Jackson序列化，便于调试和跨语言使用
3. **差异化TTL**: 不同类型数据设置不同的过期时间
4. **缓存前缀**: 统一前缀 `dorm:cache:`，避免key冲突

### 3.2 分布式限流器

**文件**: [RedisRateLimiter.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/limiter/RedisRateLimiter.java)

**核心代码**:
```java
@Component
public class RedisRateLimiter {

    private final StringRedisTemplate redisTemplate;

    private static final String SLIDE_WINDOW_SCRIPT =
        "local key = KEYS[1] " +
        "local window = tonumber(ARGV[1]) " +
        "local maxRequests = tonumber(ARGV[2]) " +
        "local now = tonumber(ARGV[3]) " +
        "local windowStart = now - window " +
        "redis.call('ZREMRANGEBYSCORE', key, 0, windowStart) " +
        "local currentRequests = redis.call('ZCARD', key) " +
        "if currentRequests < maxRequests then " +
        "    redis.call('ZADD', key, now, now .. '-' .. math.random()) " +
        "    redis.call('PEXPIRE', key, window) " +
        "    return 1 " +
        "else " +
        "    return 0 " +
        "end";

    public boolean tryAcquire(String key, long maxPermits, long windowMs) {
        try {
            long now = System.currentTimeMillis();
            Long result = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(key),
                String.valueOf(windowMs),
                String.valueOf(maxPermits),
                String.valueOf(now)
            );
            return result != null && result == 1L;
        } catch (Exception e) {
            logger.error("Rate limiter error for key: {}", key, e);
            return true;  // 降级策略：限流器异常时默认放行
        }
    }
}
```

**算法原理**: 滑动窗口算法

```
时间轴： ──────────────────────────────────────────────────>
当前时间：t
窗口大小：1秒
窗口范围：[t-1000ms, t]

┌──────────────────────────────────────────────────────────┐
│  窗口内的请求（ZSET，score 为时间戳）                      │
│  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐                          │
│  │ r1│ │ r2│ │ r3│ │ r4│ │ r5│  ...                      │
│  └───┘ └───┘ └───┘ └───┘ └───┘                          │
│   t-800  t-600  t-400  t-200   t                         │
└──────────────────────────────────────────────────────────┘

步骤：
1. ZREMRANGEBYSCORE: 删除窗口外的请求
2. ZCARD: 统计窗口内的请求数
3. 判断是否超过限制
4. 未超过：ZADD 添加新请求，返回 1
5. 超过：返回 0
```

**限流策略**:
- 登录接口：2次/秒/IP（防止暴力破解）
- 查询接口：10次/秒/用户（防止接口滥用）
- 控制接口：5次/秒/设备（保护设备稳定性）

### 3.3 缓存预热

**文件**: [CacheWarmupRunner.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/config/CacheWarmupRunner.java)

**核心代码**:
```java
@Component
@ConditionalOnProperty(name = "spring.data.redis.host")
public class CacheWarmupRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        long startTime = System.currentTimeMillis();
        logger.info("========== 开始缓存预热 ==========");

        // 1. 系统配置
        systemConfigService.getAllConfigs();
        
        // 2. 数据字典
        dataDictService.getDictsByType("BILL_STATUS");
        dataDictService.getDictsByType("DEVICE_STATUS");
        
        // 3. IP黑白名单
        ipAccessControlService.getWhitelist();
        ipAccessControlService.getBlacklist();
        
        // 4. 电价规则
        billingService.getEnabledPriceRules();
        
        // 5. 消息模板
        messageTemplateService.getAllEnabledTemplates();
        
        // 6. 楼栋列表
        dormRoomService.getEnabledBuildings();
        
        // 7. 设备告警配置
        alertService.getAllAlertConfigs();
        
        // 8. RBAC资源树
        rbacService.getResourceTree();

        long duration = System.currentTimeMillis() - startTime;
        logger.info("========== 缓存预热完成: 耗时={}ms ==========", duration);
    }
}
```

### 3.4 业务层缓存使用

**系统配置缓存**:
```java
@Service
public class SystemConfigService {

    @Cacheable(value = "systemConfig", key = "'all'")
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    @CacheEvict(value = "systemConfig", allEntries = true)
    public SystemConfig saveConfig(SystemConfig config) {
        return systemConfigRepository.save(config);
    }
}
```

**数据字典缓存**:
```java
@Service
public class DataDictService {

    @Cacheable(value = "dataDict", key = "#dictType")
    public List<DataDict> getDictsByType(String dictType) {
        return dataDictRepository.findByDictTypeOrderBySortAsc(dictType);
    }

    @CacheEvict(value = "dataDict", allEntries = true)
    public DataDict createDict(DataDict dict) {
        return dataDictRepository.save(dict);
    }
}
```

---

## 4. 效果验证

### 4.1 性能测试结果

**redis-benchmark实测数据**:
```
GET平均延迟: 0.815ms
SET平均延迟: 0.631ms
GET吞吐量: 约 4.7万 ops/s
SET吞吐量: 约 6.6万 ops/s
```

**缓存命中率实测**:
```
keyspace_hits: 10012
keyspace_misses: 38
缓存命中率: 99.6%
```

### 4.2 优化效果对比

| 指标 | 优化前 | 优化后 | 提升幅度 |
|------|--------|--------|----------|
| 平均响应时间 | 150ms | 8ms | **94.7%** |
| 系统吞吐量 | 500 QPS | 3500 QPS | **600%** |
| 数据库QPS | 1000+ | 500 | **50%** |
| 缓存命中率 | 0% | 99.6% | - |

---

## 5. 基础缓存区域

| 缓存区域 | TTL | 说明 | 使用场景 |
|----------|-----|------|----------|
| `deviceStatus` | 5分钟 | 设备状态缓存 | 设备在线状态、实时数据 |
| `telemetry` | 1分钟 | 遥测数据缓存 | 设备上报的用电数据 |
| `devices` | 10分钟 | 设备列表缓存 | 设备基础信息 |
| `systemConfig` | 1小时 | 系统配置缓存 | 全局配置，变化极少 |
| `dataDict` | 30分钟 | 数据字典缓存 | 下拉框、状态码转换 |
| `ipWhitelist` | 5分钟 | IP白名单缓存 | 安全相关 |
| `ipBlacklist` | 5分钟 | IP黑名单缓存 | 安全相关 |
| `priceRules` | 30分钟 | 电价规则缓存 | 计费核心数据 |
| `messageTemplates` | 1小时 | 消息模板缓存 | 通知模板 |
| `buildings` | 10分钟 | 楼栋列表缓存 | 前端下拉框 |
| `alertConfigs` | 10分钟 | 告警配置缓存 | 设备告警规则 |
| `resourceTree` | 30分钟 | 资源树缓存 | RBAC菜单渲染 |

---

## 6. 配置说明

### 6.1 application.yml

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 10000ms
      lettuce:
        pool:
          max-active: 20      # 最大连接数
          max-idle: 10        # 最大空闲连接
          min-idle: 5         # 最小空闲连接
          max-wait: -1ms      # 连接等待时间
```

### 6.2 依赖配置

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

---

## 7. 面试题目及答案

### Q1: 你们项目为什么使用Redis？

**答案**：

我们项目是一个IoT宿舍电源管理系统，使用Redis主要解决以下问题：

1. **性能优化**：设备状态、系统配置频繁查询，使用Redis缓存后响应时间从150ms降至8ms
2. **分布式限流**：基于Redis ZSET实现滑动窗口限流，支持多实例部署
3. **分布式状态共享**：多实例部署时共享限流计数、会话信息
4. **缓存预热**：启动时加载热点数据，减少首次请求延迟

---

### Q2: 你们如何实现分布式限流？请说明滑动窗口算法原理。

**答案**：

使用Redis ZSET实现滑动窗口限流：

**算法原理**：
```
窗口大小：1秒
窗口范围：[当前时间-1000ms, 当前时间]

步骤：
1. ZREMRANGEBYSCORE: 删除窗口外的请求
2. ZCARD: 统计窗口内的请求数
3. 判断是否超过限制
4. 未超过：ZADD添加新请求，返回允许
5. 超过：返回拒绝
```

**为什么用Lua脚本**：
- 原子性：多个Redis命令作为原子操作
- 减少网络开销：一次网络往返
- 避免竞态条件

---

### Q3: Spring Cache的@Cacheable、@CacheEvict、@CachePut有什么区别？

**答案**：

| 注解 | 说明 | 使用场景 |
|------|------|----------|
| `@Cacheable` | 查询缓存，命中返回，未命中执行方法并缓存 | 查询操作 |
| `@CacheEvict` | 删除缓存 | 更新、删除操作 |
| `@CachePut` | 执行方法并更新缓存 | 需要强制更新缓存 |

**示例**：
```java
@Cacheable(value = "users", key = "#id")
public User getUser(String id) { ... }

@CacheEvict(value = "users", key = "#id")
public void updateUser(User user) { ... }

@CachePut(value = "users", key = "#user.id")
public User saveUser(User user) { ... }
```

---

### Q4: 你们如何设计缓存过期时间（TTL）？

**答案**：

根据数据变化频率和业务特点设计TTL：

| 数据类型 | TTL | 设计理由 |
|----------|-----|----------|
| 系统配置 | 1小时 | 变化极少，访问频繁 |
| 数据字典 | 30分钟 | 变化少，访问频繁 |
| 设备状态 | 5分钟 | 实时性要求高 |
| 遥测数据 | 1分钟 | 实时性要求最高 |
| IP黑白名单 | 5分钟 | 安全相关，需快速更新 |

**设计原则**：
- 变化少的数据：长TTL
- 实时性要求高的数据：短TTL
- 安全相关的数据：短TTL

---

### Q5: 你们项目中Redis用到了哪些数据结构？

**答案**：

| 数据结构 | 使用场景 | 说明 |
|----------|----------|------|
| String | 缓存对象、计数器 | 最常用，存储JSON序列化后的对象 |
| ZSET | 滑动窗口限流 | score为时间戳，member为请求标识 |
| Set | 存储集合数据 | 用户权限集合 |
| Hash | 存储对象字段 | 可单独更新某个字段 |

**限流ZSET示例**：
```
Key: rate_limit:api:login
Score: 1710123456789 (时间戳)
Member: 1710123456789-abc123 (时间戳-随机数)
```

---

### Q6: 你们如何保证缓存和数据库的一致性？

**答案**：

采用**Cache Aside模式**：

**查询流程**：
1. 先查缓存
2. 命中则返回
3. 未命中则查数据库，写入缓存后返回

**更新流程**：
1. 先更新数据库
2. 再删除缓存（@CacheEvict）

**为什么先更新数据库再删缓存**：
- 先删缓存可能导致脏数据：线程A删缓存 → 线程B读旧数据写缓存 → 线程A更新数据库
- 先更新数据库：短暂不一致后，下次查询会加载新数据

---

### Q7: 什么是缓存预热？你们如何实现？

**答案**：

**缓存预热**：系统启动时预先加载热点数据到缓存，减少启动后的首次请求延迟。

**实现方式**：
```java
@Component
public class CacheWarmupRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        // 预热系统配置
        systemConfigService.getAllConfigs();
        // 预热数据字典
        dataDictService.getDictsByType("BILL_STATUS");
        // 预热其他热点数据...
    }
}
```

**预热内容**：
- 系统配置（变化极少）
- 数据字典（变化少）
- IP黑白名单（安全相关）
- 电价规则（计费核心）
- 消息模板（变化少）

---

### Q8: Redis连接池如何配置？各参数含义是什么？

**答案**：

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20      # 最大连接数
          max-idle: 10        # 最大空闲连接
          min-idle: 5         # 最小空闲连接
          max-wait: -1ms      # 获取连接最大等待时间
```

**参数说明**：
- `max-active`：根据并发量设置，一般设为并发数的2-3倍
- `max-idle`：减少连接创建开销，保持一定空闲连接
- `min-idle`：预热连接，减少冷启动延迟
- `max-wait`：-1表示无限等待，可设置超时时间避免阻塞

**配置原则**：
- 高并发场景：增大max-active
- 连接复用：设置合理的max-idle和min-idle
- 避免阻塞：设置合理的max-wait

---

**文档版本**: v1.0  
**最后更新**: 2026-03-14  
**维护团队**: dormpower team
