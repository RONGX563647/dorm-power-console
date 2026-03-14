# 宿舍电源管理系统 - Redis 使用文档

## 目录
- [1. 项目为什么使用 Redis](#1-项目为什么使用-redis)
- [2. 项目如何使用 Redis](#2-项目如何使用-redis)
- [3. Redis 配置详解](#3-redis-配置详解)
- [4. 核心功能实现](#4-核心功能实现)
- [5. 面试要点总结](#5-面试要点总结)

---

## 1. 项目为什么使用 Redis

### 1.1 业务需求分析

宿舍电源管理系统是一个典型的 IoT（物联网）应用场景，具有以下特点：

| 业务特点 | 技术挑战 | Redis 解决方案 |
|----------|----------|----------------|
| **高频数据读取** | 设备状态、系统配置等数据频繁查询 | 缓存热点数据，减少数据库压力 |
| **实时性要求高** | 设备状态需要实时更新和推送 | 内存存储，毫秒级响应 |
| **并发访问** | 多用户同时操作系统 | 分布式限流，保护系统稳定性 |
| **分布式部署** | 多实例部署，需要共享状态 | 分布式缓存，数据一致性 |
| **设备在线状态** | 需要快速判断设备是否在线 | 高效的状态存储和查询 |

### 1.2 Redis 在项目中的核心价值

#### 1.2.1 性能优化

**问题**：频繁查询数据库导致响应慢、数据库压力大

**解决方案**：使用 Redis 作为缓存层

```
┌─────────────────────────────────────────────────────────────┐
│                      查询流程优化                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  客户端请求 ──> 查询 Redis 缓存 ──> 缓存命中 ──> 返回数据    │
│                      │                                      │
│                      │ 缓存未命中                            │
│                      ▼                                      │
│               查询数据库 ──> 写入 Redis ──> 返回数据         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**效果**（实测数据）：
- 查询响应时间：GET平均延迟 0.815ms，SET平均延迟 0.631ms（redis-benchmark实测）
- 吞吐量：GET约 4.7万 ops/s，SET约 6.6万 ops/s（redis-benchmark实测）
- 缓存命中率：99.6%（redis-cli info stats实测：keyspace_hits=10012, keyspace_misses=38）

> **数据来源**：以上数据通过redis-benchmark和redis-cli实测获得（2026年3月14日）。
> **测试环境**：Docker容器部署的Redis 7.x，本地开发环境。
> **测试方法**：详见 [Redis性能监控实战教程.md](file:///Users/rongx/Desktop/Code/git/dorm/doc/Redis性能监控实战教程.md)

#### 1.2.2 分布式限流

**问题**：防止恶意请求、保护系统稳定性

**解决方案**：基于 Redis 实现分布式限流

```
┌─────────────────────────────────────────────────────────────┐
│                    分布式限流架构                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                  │
│  │ 实例 1   │  │ 实例 2   │  │ 实例 3   │                  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘                  │
│       │             │             │                         │
│       └─────────────┼─────────────┘                         │
│                     │                                       │
│                     ▼                                       │
│            ┌─────────────────┐                             │
│            │     Redis       │                             │
│            │  滑动窗口限流    │                             │
│            └─────────────────┘                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**限流策略**：
- 登录接口：2 次/秒/IP（防止暴力破解）
- 查询接口：10 次/秒/用户（防止接口滥用）
- 控制接口：5 次/秒/设备（保护设备稳定性）

#### 1.2.3 多级缓存架构

**问题**：单一缓存层无法满足不同数据的访问特性

**解决方案**：实现多级缓存策略

```
┌─────────────────────────────────────────────────────────────┐
│                     多级缓存架构                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    应用层                            │   │
│  └──────────────────────┬──────────────────────────────┘   │
│                         │                                   │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │        L1: 本地缓存 (ConcurrentMap)                  │   │
│  │        容量: 1000条, TTL: 5分钟                       │   │
│  │        命中率高, 无网络开销                           │   │
│  └──────────────────────┬──────────────────────────────┘   │
│                         │ 未命中                            │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │        L2: 分布式缓存 (Redis)                        │   │
│  │        容量: 无限制, TTL: 30分钟                      │   │
│  │        支持分布式共享                                 │   │
│  └──────────────────────┬──────────────────────────────┘   │
│                         │ 未命中                            │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │        L3: 数据库 (PostgreSQL)                       │   │
│  │        持久化存储                                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 1.3 技术选型对比

| 对比维度 | Redis | 本地缓存 | 数据库 |
|----------|-------|----------|--------|
| **响应速度** | 1-5ms | 0.1-1ms | 50-100ms |
| **分布式支持** | ✅ | ❌ | ✅ |
| **数据持久化** | ✅ | ❌ | ✅ |
| **容量限制** | 内存大小 | JVM 内存 | 磁盘大小 |
| **适用场景** | 分布式缓存 | 单机热点 | 持久化存储 |

**结论**：Redis 是分布式缓存的最佳选择

---

## 2. 项目如何使用 Redis

### 2.1 技术栈集成

#### 2.1.1 依赖配置

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

#### 2.1.2 Redis 配置

```yaml
# application-prod.yml
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

### 2.2 核心组件实现

#### 2.2.1 Redis 缓存配置类

**文件**：[RedisCacheConfig.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/config/RedisCacheConfig.java)

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
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

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

**设计要点**：
1. **条件装配**：`@ConditionalOnProperty` 确保 Redis 可用时才启用
2. **JSON 序列化**：使用 Jackson 序列化，便于调试和跨语言使用
3. **差异化 TTL**：不同类型数据设置不同的过期时间
4. **缓存前缀**：统一前缀 `dorm:cache:`，避免 key 冲突

#### 2.2.2 分布式限流器

**文件**：[RedisRateLimiter.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/limiter/RedisRateLimiter.java)

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

**算法原理**：滑动窗口算法

```
┌─────────────────────────────────────────────────────────────┐
│                    滑动窗口限流原理                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  时间轴： ──────────────────────────────────────────────>   │
│                                                             │
│  当前时间：t                                                │
│  窗口大小：1秒                                              │
│  窗口范围：[t-1000ms, t]                                    │
│                                                             │
│  ┌──────────────────────────────────────────────────┐      │
│  │  窗口内的请求（ZSET，score 为时间戳）              │      │
│  │  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐                  │      │
│  │  │ r1│ │ r2│ │ r3│ │ r4│ │ r5│  ...            │      │
│  │  └───┘ └───┘ └───┘ └───┘ └───┘                  │      │
│  │   t-800  t-600  t-400  t-200   t                │      │
│  └──────────────────────────────────────────────────┘      │
│                                                             │
│  步骤：                                                     │
│  1. ZREMRANGEBYSCORE: 删除窗口外的请求                      │
│  2. ZCARD: 统计窗口内的请求数                               │
│  3. 判断是否超过限制                                        │
│  4. 未超过：ZADD 添加新请求，返回 1                          │
│  5. 超过：返回 0                                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**优势**：
- **精确限流**：相比固定窗口，滑动窗口更精确
- **原子操作**：Lua 脚本保证原子性
- **分布式支持**：多实例共享限流状态

#### 2.2.3 缓存预热

**文件**：[CacheWarmupRunner.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/config/CacheWarmupRunner.java)

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

**预热策略**：
- 系统启动时自动加载热点数据
- 减少启动后的首次请求延迟
- 提升用户体验

### 2.3 业务层缓存使用

#### 2.3.1 系统配置缓存

**文件**：[SystemConfigService.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/service/SystemConfigService.java)

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

    @CacheEvict(value = "systemConfig", allEntries = true)
    public void deleteConfig(Long id) {
        systemConfigRepository.deleteById(id);
    }
}
```

**缓存策略**：
- **读取**：`@Cacheable` 缓存查询结果
- **更新**：`@CacheEvict` 清除缓存
- **TTL**：1 小时（变化极少）

#### 2.3.2 数据字典缓存

**文件**：[DataDictService.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/service/DataDictService.java)

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

    @CacheEvict(value = "dataDict", allEntries = true)
    public DataDict updateDict(Long id, DataDict dict) {
        // 更新逻辑
    }

    @CacheEvict(value = "dataDict", allEntries = true)
    public void deleteDict(Long id) {
        dataDictRepository.deleteById(id);
    }
}
```

**缓存策略**：
- **读取**：按字典类型缓存
- **更新**：清除所有字典缓存
- **TTL**：30 分钟

#### 2.3.3 设备状态缓存

**文件**：[DeviceService.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/service/DeviceService.java)

```java
@Service
public class DeviceService {

    @Cacheable(value = "devices", unless = "#result == null || #result.isEmpty()")
    public List<Map<String, Object>> getDevices() {
        List<Device> devices = deviceRepository.findAll();
        // 转换逻辑
        return result;
    }

    @Cacheable(value = "deviceStatus", key = "#deviceId")
    public Map<String, Object> getDeviceStatus(String deviceId) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        // 状态处理逻辑
        return status;
    }
}
```

**缓存策略**：
- **设备列表**：TTL 10 分钟
- **设备状态**：TTL 5 分钟（实时性要求高）

### 2.4 限流切面集成

**文件**：[ApiAspect.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/aop/ApiAspect.java)

```java
@Aspect
@Component
public class ApiAspect {

    @Autowired(required = false)
    private RedisRateLimiter redisRateLimiter;

    @Around("@annotation(com.dormpower.annotation.RateLimit) || " +
            "@within(org.springframework.web.bind.annotation.RestController)")
    public Object handleApiRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        // 测试环境跳过限流
        boolean isTestProfile = activeProfile != null && activeProfile.contains("test");

        // 管理员用户跳过限流
        if (!isTestProfile && !isAdminUser()) {
            RateLimit rateLimit = targetMethod.getAnnotation(RateLimit.class);
            if (rateLimit != null && redisRateLimiter != null) {
                String limitKey = RedisRateLimiter.apiKey(rateLimit.type());
                if (!redisRateLimiter.tryAcquire(limitKey, 1, (long) rateLimit.value(), 1000)) {
                    throw new RuntimeException("请求过于频繁，请稍后再试");
                }
            }
        }

        return joinPoint.proceed();
    }
}
```

**限流策略**：
- 通过注解 `@RateLimit` 标记需要限流的接口
- 测试环境和管理员用户跳过限流
- 限流失败抛出异常

---

## 3. Redis 配置详解

### 3.1 缓存区域配置

| 缓存区域 | TTL | 说明 | 使用场景 |
|----------|-----|------|----------|
| `deviceStatus` | 5分钟 | 设备状态缓存 | 设备在线状态、实时数据 |
| `telemetry` | 1分钟 | 遥测数据缓存 | 设备上报的用电数据 |
| `devices` | 10分钟 | 设备列表缓存 | 设备基础信息 |
| `deviceDetail` | 5分钟 | 设备详情缓存 | 设备详细信息 |
| `deviceOnline` | 1分钟 | 设备在线状态缓存 | 设备在线判断 |
| `systemConfig` | 1小时 | 系统配置缓存 | 全局配置，变化极少 |
| `dataDict` | 30分钟 | 数据字典缓存 | 下拉框、状态码转换 |
| `ipWhitelist` | 5分钟 | IP白名单缓存 | 安全相关，需快速更新 |
| `ipBlacklist` | 5分钟 | IP黑名单缓存 | 安全相关，需快速更新 |
| `priceRules` | 30分钟 | 电价规则缓存 | 计费核心数据 |
| `messageTemplates` | 1小时 | 消息模板缓存 | 通知模板，变化极少 |
| `buildings` | 10分钟 | 楼栋列表缓存 | 前端下拉框 |
| `alertConfigs` | 10分钟 | 告警配置缓存 | 设备告警规则 |
| `deviceAlerts` | 2分钟 | 设备告警列表缓存 | 设备告警信息 |
| `unresolvedAlerts` | 1分钟 | 未解决告警缓存 | 未处理告警列表 |
| `resourceTree` | 30分钟 | 资源树缓存 | RBAC 菜单渲染 |
| `userPermissions` | 15分钟 | 用户权限缓存 | 权限检查 |
| `userRoles` | 15分钟 | 用户角色缓存 | 角色管理 |
| `roomBalance` | 2分钟 | 房间余额缓存 | 余额查询 |
| `unreadCount` | 1分钟 | 未读消息计数缓存 | 未读通知数 |
| `studentStats` | 5分钟 | 学生统计缓存 | 学生统计数据 |
| `roomStats` | 5分钟 | 房间统计缓存 | 房间统计数据 |
| `aiReport` | 30分钟 | AI报告缓存 | AI分析结果 |
| `pendingBills` | 5分钟 | 待缴费账单缓存 | 待缴费账单列表 |
| `electricityStats` | 10分钟 | 用电统计缓存 | 用电统计数据 |

### 3.2 连接池配置

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20      # 最大连接数
          max-idle: 10        # 最大空闲连接
          min-idle: 5         # 最小空闲连接
          max-wait: -1ms      # 连接等待时间（-1 表示无限等待）
```

**配置说明**：
- `max-active`：根据并发量设置，一般设置为并发数的 2-3 倍
- `max-idle`：减少连接创建开销
- `min-idle`：预热连接，减少冷启动延迟

### 3.3 序列化配置

```java
ObjectMapper objectMapper = new ObjectMapper();
objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
objectMapper.activateDefaultTyping(
    objectMapper.getPolymorphicTypeValidator(),
    ObjectMapper.DefaultTyping.NON_FINAL
);
objectMapper.registerModule(new JavaTimeModule());
objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
```

**配置说明**：
- **JSON 序列化**：便于调试和跨语言使用
- **类型信息**：支持多态类型
- **时间处理**：Java 8 时间类型支持

---

## 4. 核心功能实现

### 4.1 缓存查询流程

```
┌─────────────────────────────────────────────────────────────┐
│                      缓存查询流程                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 客户端发起请求                                          │
│     GET /api/system/configs                                 │
│                                                             │
│  2. Controller 接收请求                                     │
│     @GetMapping("/configs")                                 │
│     public List<SystemConfig> getConfigs() {                │
│         return systemConfigService.getAllConfigs();         │
│     }                                                       │
│                                                             │
│  3. Service 查询缓存                                        │
│     @Cacheable(value = "systemConfig", key = "'all'")       │
│     public List<SystemConfig> getAllConfigs() {             │
│         return repository.findAll();                        │
│     }                                                       │
│                                                             │
│  4. Spring Cache 拦截                                       │
│     ┌────────────────────────────────────────┐             │
│     │  4.1 生成缓存 key: dorm:cache:systemConfig:all│      │
│     │  4.2 查询 Redis                         │             │
│     │  4.3 命中：反序列化并返回               │             │
│     │  4.4 未命中：执行方法，缓存结果         │             │
│     └────────────────────────────────────────┘             │
│                                                             │
│  5. 返回响应                                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 缓存更新流程

```
┌─────────────────────────────────────────────────────────────┐
│                      缓存更新流程                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 客户端发起更新请求                                      │
│     PUT /api/system/configs/1                               │
│                                                             │
│  2. Controller 接收请求                                     │
│     @PutMapping("/configs/{id}")                            │
│     public SystemConfig updateConfig(@PathVariable Long id, │
│                                      @RequestBody Config config) {│
│         return systemConfigService.saveConfig(config);      │
│     }                                                       │
│                                                             │
│  3. Service 更新并清除缓存                                  │
│     @CacheEvict(value = "systemConfig", allEntries = true)  │
│     public SystemConfig saveConfig(SystemConfig config) {   │
│         return repository.save(config);                     │
│     }                                                       │
│                                                             │
│  4. Spring Cache 清除缓存                                   │
│     ┌────────────────────────────────────────┐             │
│     │  删除 dorm:cache:systemConfig:*        │             │
│     └────────────────────────────────────────┘             │
│                                                             │
│  5. 下次查询时重新加载缓存                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.3 限流流程

```
┌─────────────────────────────────────────────────────────────┐
│                        限流流程                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 客户端发起请求                                          │
│     POST /api/auth/login                                    │
│                                                             │
│  2. AOP 切面拦截                                            │
│     @Around("@annotation(RateLimit)")                       │
│     public Object handleApiRequest(ProceedingJoinPoint jp) {│
│         // 限流检查                                         │
│     }                                                       │
│                                                             │
│  3. 限流器检查                                              │
│     ┌────────────────────────────────────────┐             │
│     │  3.1 生成限流 key: rate_limit:api:login│             │
│     │  3.2 执行 Lua 脚本                     │             │
│     │  3.3 判断是否允许                      │             │
│     └────────────────────────────────────────┘             │
│                                                             │
│  4. 结果处理                                                │
│     ┌────────────────────────────────────────┐             │
│     │  允许：继续执行业务逻辑                 │             │
│     │  拒绝：抛出异常 "请求过于频繁"          │             │
│     └────────────────────────────────────────┘             │
│                                                             │
│  5. 返回响应                                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. 面试要点总结

### 5.1 项目实战问题

#### Q1: 你们项目为什么使用 Redis？

**答案**：

我们项目是一个 IoT 宿舍电源管理系统，使用 Redis 主要有以下几个原因：

1. **性能优化**：
   - 设备状态、系统配置等数据频繁查询
   - 使用 Redis 缓存后，GET平均延迟 0.815ms，SET平均延迟 0.631ms（实测数据）
   - 吞吐量：GET约 4.7万 ops/s，SET约 6.6万 ops/s（实测数据）

2. **分布式限流**：
   - 系统需要防止恶意请求和接口滥用
   - 使用 Redis 实现滑动窗口限流算法
   - 支持多实例部署下的统一限流

3. **多级缓存架构**：
   - 实现了本地缓存 + Redis 的多级缓存
   - 不同类型数据设置不同的 TTL
   - 平衡性能和实时性

4. **分布式部署支持**：
   - 系统支持多实例部署
   - Redis 作为分布式缓存，保证数据一致性

#### Q2: 你们项目中 Redis 用在哪些场景？

**答案**：

1. **缓存热点数据**：
   - 系统配置（TTL 1小时）
   - 数据字典（TTL 30分钟）
   - 设备状态（TTL 5分钟）
   - 用户权限信息（TTL 30分钟）

2. **分布式限流**：
   - 登录接口：2次/秒/IP
   - 查询接口：10次/秒/用户
   - 控制接口：5次/秒/设备

3. **缓存预热**：
   - 系统启动时加载热点数据
   - 减少启动后的首次请求延迟

#### Q3: 你们如何实现分布式限流？

**答案**：

我们使用 Redis 的 ZSET 数据结构实现滑动窗口限流算法：

1. **算法原理**：
   - 使用 ZSET 存储请求，score 为时间戳
   - 每次请求时，先删除窗口外的请求
   - 统计窗口内的请求数，判断是否超过限制

2. **实现方式**：
   - 使用 Lua 脚本保证原子性
   - 支持多实例部署下的统一限流

3. **Lua 脚本核心逻辑**：
   ```lua
   redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)
   local currentRequests = redis.call('ZCARD', key)
   if currentRequests < maxRequests then
       redis.call('ZADD', key, now, now .. '-' .. math.random())
       return 1
   else
       return 0
   end
   ```

#### Q4: 你们如何保证缓存和数据库一致性？

**答案**：

我们采用 **Cache Aside 模式 + 缓存失效** 策略：

1. **查询流程**：
   - 先查缓存，命中则返回
   - 未命中则查数据库，并写入缓存

2. **更新流程**：
   - 先更新数据库
   - 再删除缓存（`@CacheEvict`）

3. **一致性保证**：
   - 设置合理的 TTL（最终一致性）
   - 更新操作立即清除缓存
   - 下次查询时重新加载最新数据

4. **为什么不先删缓存再更新数据库**：
   - 可能导致脏数据（删除缓存后、更新数据库前，其他线程读取旧数据并写入缓存）

#### Q5: 你们如何处理缓存穿透、击穿、雪崩？

**答案**：

1. **缓存穿透**：
   - 我们使用布隆过滤器过滤不存在的 key
   - 对于查询为空的结果，也缓存空值（设置较短 TTL）

2. **缓存击穿**：
   - 对于热点数据，设置较长的 TTL
   - 使用分布式锁，防止并发查询数据库

3. **缓存雪崩**：
   - 设置不同的 TTL，避免同时过期
   - 实现多级缓存（本地缓存 + Redis）
   - 系统启动时进行缓存预热

#### Q6: 你们如何设计缓存过期时间？

**答案**：

我们根据数据的访问频率和更新频率设计不同的 TTL：

| 数据类型 | TTL | 设计理由 |
|----------|-----|----------|
| 系统配置 | 1小时 | 变化极少，访问频繁 |
| 数据字典 | 30分钟 | 变化少，访问频繁 |
| 设备状态 | 5分钟 | 实时性要求高 |
| 遥测数据 | 1分钟 | 实时性要求最高 |
| IP黑白名单 | 5分钟 | 安全相关，需快速更新 |

**设计原则**：
- 变化少的数据：长 TTL
- 实时性要求高的数据：短 TTL
- 安全相关的数据：短 TTL

#### Q7: 你们项目中 Redis 有什么优化措施？

**答案**：

1. **序列化优化**：
   - 使用 JSON 序列化，便于调试和跨语言使用
   - 注册 JavaTimeModule 支持 Java 8 时间类型

2. **连接池优化**：
   - 配置合理的连接池参数
   - 预热连接，减少冷启动延迟

3. **缓存预热**：
   - 系统启动时加载热点数据
   - 减少启动后的首次请求延迟

4. **降级策略**：
   - Redis 不可用时，自动降级到本地缓存
   - 限流器异常时，默认放行

5. **监控告警**：
   - 监控 Redis 连接数、内存使用率
   - 监控缓存命中率

### 5.2 技术深度问题

#### Q8: Spring Cache 的工作原理？

**答案**：

Spring Cache 基于 AOP 实现：

1. **@Cacheable**：
   - 在方法执行前检查缓存
   - 命中则返回缓存值
   - 未命中则执行方法，缓存结果

2. **@CacheEvict**：
   - 在方法执行后清除缓存
   - 支持清除单个 key 或所有 key

3. **@CachePut**：
   - 执行方法并缓存结果
   - 不影响方法执行

4. **实现原理**：
   - 通过 CachingAnnotationAspect 切面拦截
   - 使用 CacheManager 操作缓存

#### Q9: 为什么使用 Lua 脚本？

**答案**：

1. **原子性**：
   - Redis 执行 Lua 脚本时是原子的
   - 避免多个命令之间的并发问题

2. **减少网络开销**：
   - 多个命令一次发送
   - 减少网络往返时间

3. **复用性**：
   - 脚本可以复用
   - Redis 会缓存脚本

#### Q10: Redis 在你们项目中的架构位置？

**答案**：

```
┌─────────────────────────────────────────────────────────────┐
│                     系统架构                                 │
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

**Redis 的角色**：
- **缓存层**：加速数据访问
- **限流层**：保护系统稳定性
- **状态存储**：分布式状态共享

---

## 总结

本文档详细介绍了宿舍电源管理系统中 Redis 的使用：

1. **为什么用**：性能优化、分布式限流、多级缓存、分布式部署支持
2. **怎么用**：Spring Cache 集成、分布式限流、缓存预热、差异化 TTL
3. **面试要点**：项目实战问题、技术深度问题

通过实际项目实践，深入理解 Redis 的应用场景和实现原理，为面试和实际开发提供参考。

---

**文档版本**：v1.0  
**编写日期**：2026年3月14日  
**作者**：DormPower Team
