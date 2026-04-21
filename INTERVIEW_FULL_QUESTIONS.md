# 🎯 面试题库完整版 - 全栈覆盖

## 目录

1. [架构设计](#1-架构设计) (15 题)
2. [后端技术](#2-后端技术) (20 题)
3. [数据库设计](#3-数据库设计) (12 题)
4. [前端技术](#4-前端技术) (10 题)
5. [消息队列](#5-消息队列) (8 题)
6. [缓存系统](#6-缓存系统) (10 题)
7. [并发编程](#7-并发编程) (8 题)
8. [安全认证](#8-安全认证) (8 题)
9. [性能优化](#9-性能优化) (10 题)
10. [运维部署](#10-运维部署) (8 题)
11. [项目实战](#11-项目实战) (10 题)

**总计：109 道面试题**

***

## 1. 架构设计

### Q1: 整体架构是怎么设计的？

**一句话回答：**

> "前后端分离 + 微服务化，Nginx 反向代理，Docker 容器化部署，2 核 2G 服务器支持 10,000+ 设备并发。"

**架构图（口述）：**

```
用户层：Web + App + IoT 设备
   ↓
接入层：Nginx（负载均衡+SSL）
   ↓
应用层：Spring Boot（JWT+WebSocket+MQTT）
   ↓
数据层：PostgreSQL + Redis + RabbitMQ
   ↓
监控层：Prometheus + Grafana
```

**关键数据：**

- 并发连接：10,000+
- 响应时间：< 50ms
- 内存占用：512MB

***

### Q2: 为什么选择单体架构而不是微服务？

**一句话回答：**

> "2 核 2G 资源有限，单体架构部署简单、运维成本低，足够支撑当前业务规模。"

**对比分析：**

| 架构  | 优点         | 缺点   | 选择理由   |
| --- | ---------- | ---- | ------ |
| 单体  | 部署简单、调试方便  | 扩展性差 | ✅ 资源受限 |
| 微服务 | 独立扩展、技术栈灵活 | 运维复杂 | ❌ 成本高  |

**扩展方案：**

> "业务增长后，可拆分为：用户服务、设备服务、告警服务、账单服务。"

***

### Q3: 如何保证系统的高可用性？

**一句话回答：**

> "熔断降级 + 限流保护 + 多实例部署 + 健康检查。"

**4 个措施：**

1. **熔断降级**：Resilience4j 熔断 Redis 故障
2. **限流保护**：Redis+Lua 防止流量洪峰
3. **多实例**：Docker Compose 部署 3 个实例
4. **健康检查**：Prometheus 实时监控

**可用性数据：** 99.9%（年停机<9 小时）

***

### Q4: 系统如何扩展？

**一句话回答：**

> "水平扩展：增加实例数；垂直扩展：升级服务器；数据分片：按楼栋/房间分库。"

**3 种扩展方案：**

1. **水平扩展**：增加后端实例（当前 3 个→10 个）
2. **垂直扩展**：升级服务器配置（2 核 2G→4 核 4G）
3. **数据分片**：按楼栋分库，每个库处理一栋楼

**扩展瓶颈：**

- 当前瓶颈：Redis 单点
- 解决方案：Redis Cluster 分片

***

### Q5: WebSocket 连接如何管理？

**一句话回答：**

> "单例 WebSocketManager，CopyOnWriteArraySet 存储连接，ConcurrentHashMap 管理订阅关系。"

**关键设计：**

```java
// 连接管理（读多写少）
CopyOnWriteArraySet<WebSocketSession> sessions

// 订阅关系（高频读写）
ConcurrentHashMap<WebSocketSession, Set<String>> subscriptions

// 精准推送（避免广播）
sendToDeviceSubscribers(deviceId, message)
```

**性能数据：**

- 连接数：10,000+
- 推送延迟：< 100ms
- 内存占用：40MB

***

### Q6: 如何处理 IoT 设备海量数据上报？

**一句话回答：**

> "MQTT 接收 → 虚拟线程处理 → RabbitMQ 异步落库 → WebSocket 实时推送。"

**数据流：**

```
设备上报 (10,000 条/秒)
   ↓
MQTT Broker (Mosquitto)
   ↓
MqttBridge (虚拟线程处理)
   ↓
RabbitMQ (异步缓冲)
   ↓
消费者 (批量写入数据库)
```

**性能数据：** 10,000 条/秒，无积压

***

### Q7: 如何设计 RESTful API？

**一句话回答：**

> "资源命名、动词使用 HTTP 方法、状态码规范、统一响应格式。"

**设计规范：**

```
GET    /api/devices          # 获取设备列表
POST   /api/devices          # 创建设备
GET    /api/devices/{id}     # 获取设备详情
PUT    /api/devices/{id}     # 更新设备
DELETE /api/devices/{id}     # 删除设备
```

**统一响应格式：**

```json
{
  "code": 200,
  "data": { ... },
  "message": "success"
}
```

***

### Q8: 如何处理跨域问题？

**一句话回答：**

> "Nginx 反向代理统一入口，后端配置 CORS 允许特定域名。"

**后端配置：**

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowCredentials(true);
    }
}
```

***

### Q9: 如何设计统一的异常处理？

**一句话回答：**

> "@RestControllerAdvice 全局异常处理器，分类处理业务异常、系统异常、参数校验异常。"

**异常分类：**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(...) {
        // 业务异常：返回 400
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(...) {
        // 参数校验异常：返回 422
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleSystemException(...) {
        // 系统异常：返回 500，记录日志
    }
}
```

***

### Q10: 如何设计日志系统？

**一句话回答：**

> "Log4j2 异步日志，按日期滚动，ELK 集中收集，敏感信息脱敏。"

**日志配置：**

```xml
<!-- 异步日志，性能提升 10 倍 -->
<AsyncRoot level="INFO">
    <AppenderRef ref="RollingFile"/>
</AsyncRoot>

<!-- 按日期滚动，保留 30 天 -->
<RollingFile fileName="logs/app.log">
    <TimeBasedTriggeringPolicy interval="1"/>
    <MaxHistory>30</MaxHistory>
</RollingFile>
```

**日志规范：**

- INFO：正常业务日志
- WARN：警告信息
- ERROR：错误日志（必须处理）
- DEBUG：调试日志（生产关闭）

***

### Q11: 如何设计定时任务？

**一句话回答：**

> "@Scheduled 注解，独立线程池，避免阻塞业务线程，支持分布式锁防重复执行。"

**定时任务示例：**

```java
@Component
public class ScheduledTasks {
    
    @Scheduled(cron = "0 0 1 * * ?")  // 每天凌晨 1 点
    public void generateDailyReports() {
        // 生成日报
    }
    
    @Scheduled(fixedRate = 60000)  // 每分钟
    public void checkDeviceStatus() {
        // 检查设备在线状态
    }
}
```

**注意事项：**

- 定时任务使用独立线程池
- 分布式环境用 Redis 锁防重复执行
- 长任务拆分为小批次

***

### Q12: 如何设计批量操作？

**一句话回答：**

> "分批处理（每批 1000 条）、并行执行、失败重试、事务控制。"

**批量插入示例：**

```java
@Transactional
public void batchInsert(List<Telemetry> data) {
    int batchSize = 1000;
    for (int i = 0; i < data.size(); i += batchSize) {
        int end = Math.min(i + batchSize, data.size());
        repository.saveAll(data.subList(i, end));
    }
}
```

**性能优化：**

- 关闭 Hibernate 一级缓存
- 使用 JDBC 批量
- 并行处理多个批次

***

### Q13: 如何设计分页查询？

**一句话回答：**

> "传统分页（Offset+Limit）+ 游标分页（WHERE id>lastId）+ 流式分页（大数据量）。"

**3 种分页场景：**

```java
// 1. 传统分页（前 1000 条）
PageRequest.of(page, size)

// 2. 游标分页（深度分页）
@Query("WHERE id > :lastId ORDER BY id LIMIT :limit")

// 3. 流式分页（大数据量导出）
@Query("SELECT ...")
Stream<T> streamAll();
```

**性能对比：**

- 传统分页：OFFSET 100000 耗时 5 秒
- 游标分页：耗时 50ms
- 流式分页：内存降低 90%

***

### Q14: 如何设计搜索功能？

**一句话回答：**

> "简单搜索用 SQL LIKE，复杂搜索用 Elasticsearch，实时搜索用 Redis 缓存。"

**搜索方案对比：**

| 场景     | 方案            | 响应时间  |
| ------ | ------------- | ----- |
| 简单模糊搜索 | SQL LIKE      | 100ms |
| 全文检索   | Elasticsearch | 10ms  |
| 热门搜索   | Redis 缓存      | 1ms   |

**优化技巧：**

- 建立全文索引
- 搜索词预处理（分词、去空格）
- 搜索结果缓存

***

### Q15: 如何设计文件上传功能？

**一句话回答：**

> "前端直传 OSS，后端生成签名 URL，异步处理文件（压缩、水印）。"

**文件上传流程：**

```
1. 前端请求后端获取签名 URL
2. 前端直传文件到 OSS
3. OSS 回调后端通知上传完成
4. 后端异步处理文件（压缩、生成缩略图）
```

**注意事项：**

- 限制文件大小（< 10MB）
- 限制文件类型（白名单）
- 文件名 UUID 防止冲突
- 病毒扫描（可选）

***

## 2. 后端技术

### Q16: Spring Boot 自动装配原理？

**一句话回答：**

> "@SpringBootApplication 包含@EnableAutoConfiguration，扫描 META-INF/spring.factories 加载自动配置类。"

**核心注解：**

```java
@SpringBootApplication
  = @SpringBootConfiguration
  + @EnableAutoConfiguration  // 关键
  + @ComponentScan

@EnableAutoConfiguration
  = @Import(AutoConfigurationImportSelector.class)
  → 加载 META-INF/spring.factories
  → 按需加载自动配置类
```

**按需加载条件：**

- @ConditionalOnClass：类路径存在某个类
- @ConditionalOnMissingBean：容器中不存在某个 Bean
- @ConditionalOnProperty：配置文件中存在某个配置

***

### Q17: Spring Bean 的生命周期？

**一句话回答：**

> "实例化 → 属性赋值 → 初始化（Aware 接口→BeanPostProcessor 前→InitializingBean→BeanPostProcessor 后）→ 使用 → 销毁。"

**生命周期流程：**

```
1. 实例化（Constructor）
2. 属性赋值（@Autowired）
3. Aware 接口回调（BeanNameAware、BeanFactoryAware）
4. BeanPostProcessor.postProcessBeforeInitialization
5. InitializingBean.afterPropertiesSet
6. @PostConstruct
7. BeanPostProcessor.postProcessAfterInitialization
8. 使用 Bean
9. @PreDestroy
10. DisposableBean.destroy
```

**记忆口诀：** "实例化、赋属性、Aware、前后置、初始化、使用、销毁"

***

### Q18: Spring 事务失效场景？

**一句话回答：**

> "非 public 方法、自调用、异常被捕获、异步方法、非 RuntimeException。"

**5 种失效场景：**

```java
// 1. 非 public 方法（❌ 失效）
@Transactional
protected void save() { }

// 2. 自调用（❌ 失效）
public void method1() {
    method2();  // 直接调用，事务失效
}
@Transactional
public void method2() { }

// 3. 异常被捕获（❌ 失效）
try {
    save();
} catch (Exception e) {
    // 未抛出异常，事务不回滚
}

// 4. 异步方法（❌ 失效）
@Async
@Transactional
public void asyncSave() { }

// 5. 非 RuntimeException（❌ 失效）
@Transactional(rollbackFor = Exception.class)  // ✅ 正确
public void save() throws Exception { }
```

***

### Q19: AOP 实现原理？

**一句话回答：**

> "动态代理（JDK 动态代理/CGLIB），运行时生成代理类，拦截目标方法执行。"

**两种代理方式：**

```
JDK 动态代理：
  - 基于接口
  - 实现 InvocationHandler
  - 只能代理接口方法

CGLIB 代理：
  - 基于继承
  - 生成子类
  - 可以代理类方法（final 方法除外）
```

**Spring AOP 选择规则：**

- 有接口 → JDK 动态代理
- 无接口 → CGLIB
- 强制 CGLIB：`@EnableAspectJAutoProxy(proxyTargetClass=true)`

***

### Q20: 如何设计幂等性？

**一句话回答：**

> "Token 机制（提交前获取 Token）、唯一索引、分布式锁、状态机。"

**4 种幂等方案：**

```
1. Token 机制（推荐）
   - 提交前获取 Token
   - 提交时验证并删除 Token
   - 重复提交 Token 无效

2. 唯一索引
   - 数据库唯一约束
   - 重复插入抛异常

3. 分布式锁
   - Redis SETNX 锁
   - 处理完释放锁

4. 状态机
   - 订单状态：待支付→已支付
   - 重复支付状态不匹配
```

**代码示例（Token 机制）：**

```java
// 获取 Token
String token = UUID.randomUUID().toString();
redisTemplate.opsForValue().set("token:" + token, "1", 300, TimeUnit.SECONDS);

// 验证 Token
if (redisTemplate.delete("token:" + token)) {
    // Token 有效，处理业务
} else {
    // Token 无效，重复提交
}
```

***

### Q21: 如何设计分布式锁？

**一句话回答：**

> "Redis SETNX+EX 原子操作，Redisson 看门狗自动续期，RedLock 多 Redis 实例。"

**Redis 分布式锁实现：**

```java
// 基础实现（SETNX+EX）
String key = "lock:order:" + orderId;
String value = UUID.randomUUID().toString();

// 原子操作：设置值 + 过期时间
Boolean locked = redisTemplate.opsForValue()
    .setIfAbsent(key, value, 30, TimeUnit.SECONDS);

if (Boolean.TRUE.equals(locked)) {
    try {
        // 执行业务
    } finally {
        // Lua 脚本删除锁（防误删）
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), 
            Collections.singletonList(key), value);
    }
}
```

**进阶方案（Redisson）：**

```java
RLock lock = redisson.getLock("lock:order:" + orderId);
lock.lock();  // 自动续期（看门狗）
try {
    // 执行业务
} finally {
    lock.unlock();
}
```

**注意事项：**

- 必须设置过期时间（防死锁）
- 删除锁时验证所有权（防误删）
- 高并发用 RedLock（多 Redis 实例）

***

### Q22: 如何保证接口安全？

**一句话回答：**

> "HTTPS 加密传输、签名验证、防重放攻击、限流、敏感信息脱敏。"

**5 层安全防护：**

```
1. 传输层：HTTPS 加密
2. 认证层：JWT Token 验证
3. 签名层：参数签名防篡改
4. 限流层：防止暴力攻击
5. 脱敏层：敏感信息加密存储
```

**签名验证示例：**

```java
// 客户端签名
String sign = MD5(params + timestamp + nonce + secretKey)

// 服务端验证
1. 验证 timestamp（防重放，5 分钟有效期）
2. 验证 nonce（防重放，已使用的 nonce 存入 Redis）
3. 验证 sign（防篡改）
```

***

### Q23: 如何设计数据校验？

**一句话回答：**

> "Hibernate Validator 注解校验（@NotNull、@Size）、自定义校验器、分组校验。"

**校验注解示例：**

```java
public class UserDTO {
    
    @NotNull(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20")
    private String username;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @Min(value = 18, message = "年龄必须>=18")
    @Max(value = 100, message = "年龄必须<=100")
    private Integer age;
}
```

**分组校验：**

```java
// 定义分组
public interface CreateGroup {}
public interface UpdateGroup {}

// 使用分组
@NotNull(groups = CreateGroup.class)
@Null(groups = UpdateGroup.class)  // 更新时 ID 不能为空
private Long id;
```

***

### Q24: 如何处理跨事务问题？

**一句话回答：**

> "本地事务用@Transactional，分布式事务用最终一致性（消息队列）、TCC、Saga。"

**分布式事务方案对比：**

| 方案   | 一致性  | 性能 | 复杂度 | 适用场景  |
| ---- | ---- | -- | --- | ----- |
| 2PC  | 强一致  | 低  | 高   | 金融场景  |
| TCC  | 最终一致 | 中  | 中   | 订单场景  |
| 消息队列 | 最终一致 | 高  | 低   | 异步场景  |
| Saga | 最终一致 | 高  | 中   | 长流程场景 |

**推荐方案（消息队列）：**

```
1. 订单服务：创建订单 → 发送消息
2. 库存服务：消费消息 → 扣减库存
3. 失败处理：消息重试 + 人工介入
```

***

### Q25: 如何设计重试机制？

**一句话回答：**

> "Spring Retry 注解（@Retryable）、指数退避、最大重试次数、熔断保护。"

**重试配置示例：**

```java
@Retryable(
    value = {RemoteCallException.class},  // 重试的异常类型
    maxAttempts = 3,                       // 最大重试 3 次
    backoff = @Backoff(delay = 1000, multiplier = 2)  // 指数退避：1s, 2s, 4s
)
public void callRemoteService() {
    // 调用远程服务
}

// 重试失败处理
@Recover
public void recover(RemoteCallException e) {
    // 重试 3 次后仍然失败，执行补偿逻辑
}
```

**注意事项：**

- 幂等性：重试可能多次执行
- 退避策略：避免雪崩
- 熔断保护：服务不可用时停止重试

***

### Q26: 如何设计熔断降级？

**一句话回答：**

> "Resilience4j 熔断器，失败率>50% 熔断，10 秒后探测恢复，降级返回默认值。"

**熔断器配置：**

```java
CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendService");

// 熔断器状态机
CLOSED（正常） → OPEN（熔断） → HALF_OPEN（探测） → CLOSED（恢复）

// 状态转换规则：
// 100 次请求中 50 次失败 → CLOSED → OPEN
// OPEN 持续 10 秒 → OPEN → HALF_OPEN
// HALF_OPEN 请求成功 → HALF_OPEN → CLOSED
```

**降级处理：**

```java
@CircuitBreaker(name = "backendService", fallbackMethod = "fallback")
public String callService() {
    return remoteService.call();
}

// 降级方法
public String fallback(Exception e) {
    return "默认值";  // 或缓存数据
}
```

***

### Q27: 如何设计链路追踪？

**一句话回答：**

> "Sleuth+Zipkin，每个请求生成 TraceId，跨服务传递 SpanId，可视化调用链路。"

**链路追踪配置：**

```xml
<!-- 引入依赖 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
```

**日志输出：**

```
2024-01-01 10:00:00.000 INFO [user-service,traceId=abc123,spanId=def456] ...
```

**Zipkin 可视化：**

- 查看完整调用链路
- 分析性能瓶颈
- 定位故障点

***

### Q28: 如何设计配置中心？

**一句话回答：**

> "Apollo/Nacos 配置中心，动态刷新（@RefreshScope），配置加密，版本管理。"

**配置中心优势：**

- 集中管理配置
- 动态刷新（无需重启）
- 配置版本控制
- 配置加密（敏感信息）

**Spring Cloud Config 示例：**

```java
@RefreshScope  // 动态刷新
@RestController
public class ConfigController {
    
    @Value("${config.key}")
    private String configValue;
    
    @GetMapping("/config")
    public String getConfig() {
        return configValue;  // 配置变更后自动刷新
    }
}
```

***

### Q29: 如何设计服务注册与发现？

**一句话回答：**

> "Nacos/Eureka 服务注册，客户端负载均衡（Ribbon），健康检查自动摘除。"

**服务注册流程：**

```
1. 服务启动时注册到 Nacos
2. 定期发送心跳（30 秒）
3. Nacos 推送服务列表给消费者
4. 消费者使用 Ribbon 负载均衡
5. 服务宕机自动摘除（90 秒无心跳）
```

**负载均衡策略：**

- 轮询（默认）
- 随机
- 权重
- 最少连接数

***

### Q30: 如何设计 API 网关？

**一句话回答：**

> "Spring Cloud Gateway/Kong，统一鉴权、限流、日志、监控。"

**网关功能：**

- 统一入口
- 路由转发
- 鉴权认证
- 限流熔断
- 日志监控
- 协议转换

**Gateway 配置示例：**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
          filters:
            - Authentication  # 自定义鉴权过滤器
            - RateLimiter     # 限流过滤器
```

***

### Q31: 如何设计定时任务调度？

**一句话回答：**

> "XXL-Job/Elastic-Job 分布式调度，分片广播、故障转移、可视化监控。"

**分布式调度方案对比：**

| 方案          | 优点     | 缺点           | 适用场景  |
| ----------- | ------ | ------------ | ----- |
| @Scheduled  | 简单     | 单机、重复执行      | 简单场景  |
| XXL-Job     | 可视化、分片 | 需额外部署        | 生产环境  |
| Elastic-Job | 去中心化   | 依赖 ZooKeeper | 高可用场景 |

**XXL-Job 示例：**

```java
@XxlJob("demoJobHandler")
public void execute() throws Exception {
    // 分片参数
    int shardIndex = XxlJobHelper.getShardIndex();
    int shardTotal = XxlJobHelper.getShardTotal();
    
    // 分片处理
    List<Data> dataList = getDataByShard(shardIndex, shardTotal);
    dataList.forEach(this::process);
}
```

***

### Q32: 如何设计消息推送？

**一句话回答：**

> "WebSocket 实时推送 + 短信/邮件兜底 + 移动端 Push，多渠道保证触达。"

**推送渠道：**

```
1. WebSocket（在线用户）
   - 实时性高
   - 成本低
   
2. 短信（紧急通知）
   - 触达率高
   - 成本高
   
3. 邮件（非紧急）
   - 成本低
   - 实时性差
   
4. 移动端 Push
   - 触达率高
   - 需集成第三方（极光、个推）
```

***

### Q33: 如何设计数据导出？

**一句话回答：**

> "异步导出、流式查询、分批次处理、OSS 存储、下载链接过期。"

**导出流程：**

```
1. 用户发起导出请求
2. 后端异步处理（生成 Excel）
3. 上传到 OSS
4. 发送通知（WebSocket/邮件）
5. 用户下载（链接 24 小时有效）
```

**性能优化：**

- 流式查询（降低内存）
- 分批处理（每批 1000 条）
- 多线程生成 Excel
- OSS  CDN 加速下载

***

### Q34: 如何设计数据导入？

**一句话回答：**

> "模板下载、数据校验、异步处理、失败重试、错误报告。"

**导入流程：**

```
1. 下载模板
2. 填写数据
3. 上传文件
4. 数据校验（格式、重复、关联）
5. 异步处理（批量插入）
6. 生成导入报告（成功/失败）
```

**数据校验规则：**

- 格式校验（必填、长度、类型）
- 重复校验（唯一索引）
- 关联校验（外键存在）
- 业务校验（状态、权限）

***

### Q35: 如何设计报表统计？

**一句话回答：**

> "预计算（定时任务）、中间表存储、缓存结果、异步导出。"

**报表设计方案：**

```
1. 实时报表
   - Redis 缓存（TTL 5 分钟）
   - 直接查询（小数据量）
   
2. 离线报表
   - 定时任务预计算
   - 存储到中间表
   - 查询时直接读取
   
3. 大数据报表
   - ClickHouse/ES
   - 列式存储
   - 聚合查询
```

***

## 3. 数据库设计

### Q36: 数据库表设计原则？

**一句话回答：**

> "三范式（无冗余、原子性、依赖主键）、适当反范式（减少关联）、索引优化。"

**三范式：**

1. **第一范式**：字段不可再分
2. **第二范式**：不能部分依赖主键
3. **第三范式**：不能传递依赖

**反范式优化：**

```sql
-- 范式化设计（需关联查询）
orders(user_id, product_id, amount)
users(id, name, phone)

-- 反范式优化（冗余字段）
orders(user_id, user_name, user_phone, product_id, amount)
-- 优点：减少关联，查询快
-- 缺点：数据冗余，更新麻烦
```

***

### Q37: 索引设计原则？

**一句话回答：**

> "高频查询字段、区分度高、联合索引最左匹配、覆盖索引避免回表。"

**索引设计原则：**

```
1. 高频查询字段建索引
2. WHERE、ORDER BY、GROUP BY 字段
3. 区分度高的字段（性别不适合）
4. 联合索引：最左匹配原则
5. 覆盖索引：避免回表
```

**联合索引示例：**

```sql
-- 索引：(device_id, ts DESC)

-- ✅ 使用索引
WHERE device_id = 'd1' ORDER BY ts DESC
WHERE device_id = 'd1' AND ts > 1000

-- ❌ 不使用索引
WHERE ts > 1000  -- 缺少最左列
```

***

### Q38: 如何优化慢查询？

**一句话回答：**

> "EXPLAIN 分析执行计划、添加索引、避免 SELECT \*、优化 SQL 写法。"

**优化步骤：**

```
1. 开启慢查询日志（>1 秒）
2. EXPLAIN 分析执行计划
3. 查看 type（ALL→index→range→ref→eq_ref→const→system）
4. 查看 key（实际使用的索引）
5. 查看 rows（扫描行数）
6. 添加索引/优化 SQL
```

**优化技巧：**

```sql
-- ❌ 全表扫描
SELECT * FROM telemetry WHERE YEAR(ts) = 2024;

-- ✅ 使用索引
SELECT * FROM telemetry WHERE ts BETWEEN '2024-01-01' AND '2024-12-31';

-- ❌ 回表查询
SELECT * FROM telemetry WHERE device_id = 'd1';

-- ✅ 覆盖索引
SELECT id, device_id, ts FROM telemetry WHERE device_id = 'd1';
```

***

### Q39: 事务隔离级别？

**一句话回答：**

> "读未提交、读已提交（MySQL 默认）、可重复读（MySQL 默认）、串行化。"

**4 种隔离级别：**

| 隔离级别 | 脏读 | 幻读 | 不可重复读 | 性能 |
| ---- | -- | -- | ----- | -- |
| 读未提交 | ✅  | ✅  | ✅     | 最高 |
| 读已提交 | ❌  | ✅  | ✅     | 高  |
| 可重复读 | ❌  | ❌  | ❌     | 中  |
| 串行化  | ❌  | ❌  | ❌     | 最低 |

**MySQL 默认：** 可重复读（Repeatable Read）

**脏读、幻读、不可重复读：**

```
脏读：读到未提交的数据
不可重复读：同一事务两次读取数据不一致
幻读：同一事务两次查询，记录数不一致
```

***

### Q40: 如何处理数据库死锁？

**一句话回答：**

> "固定顺序访问资源、一次性获取所有锁、超时机制、死锁检测。"

**死锁产生条件：**

```
1. 互斥条件：资源只能被一个事务占用
2. 请求与保持：已持有资源，请求新资源
3. 不剥夺：资源不能被强制剥夺
4. 循环等待：形成环路
```

**解决方案：**

```
1. 固定顺序访问（推荐）
   - 所有事务按相同顺序获取锁
   - 先 A 后 B，不会形成环路
   
2. 一次性获取所有锁
   - 开始事务时获取所有需要的锁
   - 获取失败不执行
   
3. 超时机制
   - 设置锁超时时间
   - 超时自动回滚
   
4. 死锁检测
   - MySQL 自动检测死锁
   - 回滚代价小的事务
```

***

### Q41: 如何分库分表？

**一句话回答：**

> "垂直分库（按业务）、水平分表（按 ID 哈希/时间范围）、中间件（ShardingSphere）。"

**分库分表方案：**

```
1. 垂直分库
   - 按业务拆分：用户库、订单库、设备库
   - 优点：业务隔离
   - 缺点：跨库查询复杂
   
2. 水平分表
   - 按 ID 哈希：user_0, user_1, ... user_9
   - 按时间范围：order_202401, order_202402, ...
   - 优点：单表数据量小
   - 缺点：扩容复杂
   
3. 分库分表中间件
   - ShardingSphere
   - MyCAT
   - 透明分片，应用无感知
```

**分片键选择：**

- 高频查询字段
- 数据分布均匀
- 避免数据倾斜

***

### Q42: 如何优化大表？

**一句话回答：**

> "分区表、历史数据归档、冷热分离、读写分离。"

**大表优化方案：**

```
1. 分区表
   - 按时间 RANGE 分区
   - 按 ID HASH 分区
   - 查询时自动裁剪分区
   
2. 历史数据归档
   - 3 个月前数据归档到历史表
   - 主表只保留热数据
   
3. 冷热分离
   - 热数据：Redis 缓存
   - 冷数据：数据库查询
   
4. 读写分离
   - 主库写，从库读
   - 报表查询走从库
```

***

### Q43: 如何保证数据一致性？

**一句话回答：**

> "事务保证（ACID）、最终一致性（消息队列）、对账机制、补偿机制。"

**数据一致性方案：**

```
1. 强一致性
   - 本地事务（@Transactional）
   - 分布式事务（2PC、TCC）
   
2. 最终一致性
   - 消息队列（异步处理）
   - 定时对账（发现不一致）
   - 补偿机制（修复数据）
   
3. 对账机制
   - 每天凌晨对账
   - 发现差异告警
   - 人工介入修复
```

***

### Q44: 如何设计数据库主键？

**一句话回答：**

> "自增 ID（简单）、UUID（分布式）、雪花算法（全局唯一）、业务 ID（有意义）。"

**主键方案对比：**

| 方案    | 优点      | 缺点      | 适用场景    |
| ----- | ------- | ------- | ------- |
| 自增 ID | 简单、有序   | 分库分表冲突  | 单库单表    |
| UUID  | 全局唯一    | 无序、占用空间 | 分布式     |
| 雪花算法  | 全局唯一、有序 | 依赖时钟    | 分布式（推荐） |
| 业务 ID | 有意义     | 可能变化    | 特殊场景    |

**雪花算法结构：**

```
64 位 = 1 位符号 + 41 位时间戳 + 10 位机器 ID + 12 位序列号
- 41 位时间戳：支持 69 年
- 10 位机器 ID：支持 1024 台机器
- 12 位序列号：每毫秒 4096 个 ID
```

***

### Q45: 如何优化数据库连接池？

**一句话回答：**

> "HikariCP（默认），核心参数（最大连接数、最小空闲、超时时间），监控连接使用。"

**HikariCP 配置：**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # 最大连接数
      minimum-idle: 5            # 最小空闲连接
      connection-timeout: 30000  # 连接超时 30 秒
      idle-timeout: 600000       # 空闲超时 10 分钟
      max-lifetime: 1800000      # 最大生命周期 30 分钟
```

**参数调优：**

```
最大连接数 = ((CPU 核心数 * 2) + 1) * 磁盘数  # 经验公式
2 核服务器：((2 * 2) + 1) * 1 = 5 → 实际设置 10-20
```

***

### Q46: 如何查询优化？

**一句话回答：**

> "EXPLAIN 分析、索引优化、避免 SELECT \*、分页优化、批量操作。"

**查询优化技巧：**

```sql
-- 1. 使用索引
CREATE INDEX idx_device_ts ON telemetry(device_id, ts DESC);

-- 2. 避免 SELECT *
SELECT id, device_id, ts FROM telemetry;

-- 3. 分页优化（深度分页）
-- ❌ 慢：OFFSET 100000 LIMIT 10
-- ✅ 快：WHERE id > 100000 LIMIT 10

-- 4. 批量操作
-- ❌ 慢：循环插入 1000 次
-- ✅ 快：INSERT INTO ... VALUES (...), (...), ...

-- 5. 关联查询优化
-- ❌ 慢：N+1 查询
-- ✅ 快：JOIN 一次性查询
```

***

### Q47: 如何备份与恢复？

**一句话回答：**

> "mysqldump 全量备份、binlog 增量备份、定时任务、异地备份、定期演练。"

**备份方案：**

```
1. 全量备份
   - mysqldump --all-databases > backup.sql
   - 每天凌晨 2 点执行
   
2. 增量备份
   - 开启 binlog
   - 记录所有变更
   
3. 备份策略
   - 每天全量备份
   - 每小时增量备份
   - 异地备份（不同机房）
   
4. 恢复演练
   - 每月演练一次
   - 验证备份有效性
```

**恢复命令：**

```bash
# 全量恢复
mysql < backup.sql

# 恢复到指定时间点
mysqlbinlog --stop-datetime="2024-01-01 12:00:00" binlog.000001 | mysql
```

***

## 4. 前端技术

### Q48: Vue 3 相比 Vue 2 的改进？

**一句话回答：**

> "Composition API、性能提升（Proxy 替代 Object.defineProperty）、TypeScript 支持、Tree Shaking。"

**核心改进：**

```
1. Composition API
   - 逻辑复用（替代 mixins）
   - 代码组织更灵活
   
2. 性能提升
   - Proxy 替代 Object.defineProperty
   - 响应式性能提升 40%
   
3. TypeScript 支持
   - 更好的类型推导
   - IDE 智能提示
   
4. Tree Shaking
   - 按需引入
   - 打包体积减少 40%
```

***

### Q49: Vue 组件通信方式？

**一句话回答：**

> "父子（props/$emit）、跨级（provide/inject）、全局（Pinia/Vuex）、事件总线。"

**通信方式对比：**

| 方式                | 场景   | 优点   | 缺点      |
| ----------------- | ---- | ---- | ------- |
| props/$emit       | 父子组件 | 简单   | 只能父子    |
| provide/inject    | 跨级组件 | 简洁   | 数据流向不清晰 |
| Pinia/Vuex        | 全局状态 | 集中管理 | 样板代码多   |
| $attrs/$listeners | 属性透传 | 灵活   | 难以维护    |

**Pinia 示例：**

```javascript
// store/deviceStore.js
export const useDeviceStore = defineStore('device', {
  state: () => ({
    devices: []
  }),
  actions: {
    fetchDevices() {
      api.getDevices().then(data => this.devices = data)
    }
  }
})

// 组件中使用
const deviceStore = useDeviceStore()
deviceStore.fetchDevices()
```

***

### Q50: 如何实现路由懒加载？

**一句话回答：**

> "动态 import()，路由分包，按需加载，减少首屏体积。"

**路由懒加载：**

```javascript
// ❌ 不推荐：所有路由打包在一起
import DashboardView from '@/views/DashboardView.vue'

// ✅ 推荐：路由懒加载
const DashboardView = () => import('@/views/DashboardView.vue')

// 路由配置
const routes = [
  {
    path: '/dashboard',
    component: () => import('@/views/DashboardView.vue')
  }
]
```

**效果：**

- 首屏体积减少 60%
- 加载时间从 3 秒降到 1 秒

***

### Q51: 如何实现权限控制？

**一句话回答：**

> "路由守卫（beforeEach）、动态路由、按钮级权限（v-permission）、接口鉴权。"

**权限控制方案：**

```javascript
// 1. 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

// 2. 动态路由（根据角色加载路由）
const userRoutes = user.roles.includes('ADMIN') ? adminRoutes : userRoutes
router.addRoutes(userRoutes)

// 3. 按钮级权限
<div v-permission="['device:write']">
  <button>删除</button>
</div>
```

***

### Q52: 如何优化前端性能？

**一句话回答：**

> "路由懒加载、组件懒加载、图片懒加载、防抖节流、虚拟列表、CDN 加速。"

**性能优化技巧：**

```
1. 路由懒加载：首屏体积减少 60%
2. 组件懒加载：大组件按需加载
3. 图片懒加载：滚动到可视区域再加载
4. 防抖节流：搜索框防抖（500ms）
5. 虚拟列表：长列表只渲染可视区域
6. CDN 加速：静态资源走 CDN
7. Gzip 压缩：体积减少 70%
8. 缓存策略：强缓存 + 协商缓存
```

**防抖示例：**

```javascript
// 防抖（n 秒内只执行一次）
const debounce = (fn, delay) => {
  let timer = null
  return (...args) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
}

// 使用
const handleSearch = debounce((keyword) => {
  api.search(keyword)
}, 500)
```

***

### Q53: 如何实现 WebSocket 连接？

**一句话回答：**

> "原生 WebSocket API，心跳检测，断线重连，消息队列缓存。"

**WebSocket 实现：**

```javascript
class WebSocketClient {
  constructor(url) {
    this.url = url
    this.reconnectInterval = 5000
    this.heartbeatInterval = 30000
    this.connect()
  }
  
  connect() {
    this.ws = new WebSocket(this.url)
    
    this.ws.onopen = () => {
      console.log('连接成功')
      this.startHeartbeat()
    }
    
    this.ws.onclose = () => {
      console.log('连接断开，重连中...')
      setTimeout(() => this.connect(), this.reconnectInterval)
    }
    
    this.ws.onerror = (error) => {
      console.error('连接错误', error)
    }
  }
  
  startHeartbeat() {
    setInterval(() => {
      this.ws.send(JSON.stringify({ type: 'ping' }))
    }, this.heartbeatInterval)
  }
  
  send(message) {
    this.ws.send(JSON.stringify(message))
  }
}
```

***

### Q54: 如何管理全局状态？

**一句话回答：**

> "Pinia（Vue 3 推荐），模块化设计，持久化插件，DevTools 调试。"

**Pinia 状态管理：**

```javascript
// stores/deviceStore.js
export const useDeviceStore = defineStore('device', {
  state: () => ({
    devices: [],
    selectedDevice: null,
    loading: false
  }),
  
  getters: {
    onlineDevices: (state) => state.devices.filter(d => d.online),
    deviceCount: (state) => state.devices.length
  },
  
  actions: {
    async fetchDevices() {
      this.loading = true
      const data = await api.getDevices()
      this.devices = data
      this.loading = false
    },
    
    selectDevice(deviceId) {
      this.selectedDevice = this.devices.find(d => d.id === deviceId)
    }
  },
  
  persist: true  // 持久化到 localStorage
})
```

***

### Q55: 如何处理跨域？

**一句话回答：**

> "开发环境（Vite 代理）、生产环境（Nginx 反向代理）、CORS 配置。"

**跨域解决方案：**

```javascript
// 1. Vite 代理（开发环境）
// vite.config.js
export default {
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true
      }
    }
  }
}

// 2. Nginx 反向代理（生产环境）
// nginx.conf
location /api/ {
  proxy_pass http://backend:8000/;
  proxy_set_header Host $host;
}

// 3. CORS 配置（后端）
@Configuration
public class CorsConfig implements WebMvcConfigurer {
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
      .allowedOrigins("http://localhost:3000")
      .allowedMethods("GET", "POST", "PUT", "DELETE");
  }
}
```

***

### Q56: 如何实现表单验证？

**一句话回答：**

> "Element Plus 表单验证（rules）、自定义验证器、异步验证、实时反馈。"

**表单验证示例：**

```vue
<template>
  <el-form :model="form" :rules="rules" ref="formRef">
    <el-form-item prop="username">
      <el-input v-model="form.username" />
    </el-form-item>
    
    <el-form-item prop="email">
      <el-input v-model="form.email" />
    </el-form-item>
  </el-form>
</template>

<script setup>
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度 3-20', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        // 自定义异步验证
        api.checkEmail(value).then(exists => {
          if (exists) callback(new Error('邮箱已存在'))
          else callback()
        })
      },
      trigger: 'blur'
    }
  ]
}
</script>
```

***

### Q57: 如何实现表格分页？

**一句话回答：**

> "Element Plus 分页组件，前端分页（小数据量）、后端分页（大数据量）、虚拟滚动（超大数据量）。"

**后端分页示例：**

```vue
<template>
  <el-table :data="tableData">
    <el-table-column prop="name" label="名称" />
    <el-table-column prop="status" label="状态" />
  </el-table>
  
  <el-pagination
    v-model:current-page="currentPage"
    v-model:page-size="pageSize"
    :total="total"
    @current-change="handlePageChange"
  />
</template>

<script setup>
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])

const fetchData = async () => {
  const { data } = await api.getDevices({
    page: currentPage.value,
    size: pageSize.value
  })
  tableData.value = data.list
  total.value = data.total
}

const handlePageChange = () => {
  fetchData()
}
</script>
```

***

## 5. 消息队列

### Q58: 为什么使用消息队列？

**一句话回答：**

> "异步解耦、削峰填谷、延迟处理、消息重试。"

**4 大作用：**

```
1. 异步解耦
   - 生产者不等待消费者
   - 系统解耦，独立扩展
   
2. 削峰填谷
   - 高峰期消息入队
   - 低峰期消费处理
   
3. 延迟处理
   - 定时任务（延迟队列）
   - 超时取消订单
   
4. 消息重试
   - 消费失败自动重试
   - 死信队列人工处理
```

***

### Q59: RabbitMQ 如何保证消息不丢失？

**一句话回答：**

> "生产者确认、消息持久化、消费者手动 ACK、死信队列。"

**4 重保障：**

```
1. 生产者确认（Publisher Confirm）
   - 消息到达 Broker 后确认
   - 未确认则重试
   
2. 消息持久化
   - Exchange 持久化
   - Queue 持久化
   - Message 持久化
   
3. 消费者手动 ACK
   - 处理成功后确认
   - 失败则重新入队
   
4. 死信队列
   - 重试失败进入死信队列
   - 人工介入处理
```

***

### Q60: 如何保证消息顺序性？

**一句话回答：**

> "单个队列单消费者、分区有序、哈希路由。"

**顺序性保证：**

```
1. 全局有序（性能差）
   - 单个队列
   - 单个消费者
   - 顺序处理
   
2. 分区有序（推荐）
   - 按业务 ID 哈希路由
   - 同一 ID 的消息进入同一队列
   - 局部有序
   
3. 消费者顺序处理
   - 内存队列缓冲
   - 单线程处理
```

***

### Q61: 如何处理消息积压？

**一句话回答：**

> "增加消费者、批量消费、降级策略、排查慢消费。"

**处理步骤：**

```
1. 紧急处理
   - 增加消费者实例（2→10）
   - 批量消费（一次 100 条）
   
2. 降级策略
   - 丢弃非关键消息
   - 降低处理优先级
   
3. 排查原因
   - 慢查询优化
   - 外部依赖超时
   - 死锁问题
   
4. 长期优化
   - 水平扩展消费者
   - 优化消费逻辑
```

***

### Q62: 如何实现延迟队列？

**一句话回答：**

> "TTL+ 死信队列、RabbitMQ 延迟插件、时间轮算法。"

**延迟队列实现：**

```
方案 1：TTL+ 死信队列（推荐）
1. 消息设置 TTL（10 分钟）
2. 过期后进入死信队列
3. 消费者监听死信队列

方案 2：RabbitMQ 延迟插件
- rabbitmq_delayed_message_exchange
- 直接发送延迟消息

方案 3：时间轮算法
- 定时任务轮询
- 到期消息入队
```

**TTL+ 死信队列示例：**

```yaml
# 普通队列（设置 TTL 和死信）
queue:
  name: order.queue
  arguments:
    x-message-ttl: 600000  # 10 分钟
    x-dead-letter-exchange: dlx.exchange
    x-dead-letter-routing-key: order.timeout

# 死信队列
queue:
  name: order.timeout.queue
  binding:
    exchange: dlx.exchange
    routingKey: order.timeout
```

***

### Q63: 如何保证幂等性？

**一句话回答：**

> "消息唯一 ID、Redis 去重、数据库唯一索引、状态机。"

**幂等性方案：**

```
1. 消息唯一 ID
   - 生产者生成 UUID
   - 消费者记录已处理的 ID
   
2. Redis 去重
   - SETNX 记录消息 ID
   - 已存在则丢弃
   
3. 数据库唯一索引
   - 唯一约束防重复
   - 重复插入抛异常
   
4. 状态机
   - 订单状态：待支付→已支付
   - 重复消息状态不匹配
```

***

### Q64: 如何监控消息队列？

**一句话回答：**

> "管理界面（15672 端口）、Prometheus 指标、告警规则（队列长度、积压量）。"

**监控指标：**

```
1. 队列长度
   - 超过 1000 告警
   - 超过 10000 严重告警
   
2. 消息积压量
   - 消费者处理速度 < 生产速度
   - 积压量持续增长告警
   
3. 消费者数量
   - 消费者宕机告警
   
4. 消息处理延迟
   - 从生产到消费的时间
   - 超过阈值告警
```

***

### Q65: 如何设计重试机制？

**一句话回答：**

> "指数退避（1s, 2s, 4s）、最大重试次数、死信队列、人工介入。"

**重试策略：**

```
1. 指数退避
   - 第 1 次重试：1 秒后
   - 第 2 次重试：2 秒后
   - 第 3 次重试：4 秒后
   - 避免雪崩效应
   
2. 最大重试次数
   - 默认 3 次
   - 超过则进入死信队列
   
3. 死信队列
   - 人工分析失败原因
   - 手动重试或修复数据
```

***

## 6. 缓存系统

### Q66: 缓存使用场景？

**一句话回答：**

> "热点数据、读多写少、计算复杂、接口聚合。"

**适用场景：**

```
1. 热点数据
   - 用户信息、设备状态
   - 访问频率高
   
2. 读多写少
   - 配置信息、字典数据
   - 变更频率低
   
3. 计算复杂
   - 统计报表、聚合查询
   - 计算成本高
   
4. 接口聚合
   - 多个接口数据合并
   - 减少网络请求
```

***

### Q67: 缓存更新策略？

**一句话回答：**

> "先更新数据库再删除缓存、延时双删、Canal 监听 binlog。"

**更新策略对比：**

| 策略         | 优点   | 缺点    | 适用场景  |
| ---------- | ---- | ----- | ----- |
| 先更 DB 再删缓存 | 简单   | 短暂不一致 | 一般场景  |
| 延时双删       | 一致性好 | 复杂    | 高一致场景 |
| Canal 监听   | 最终一致 | 需额外组件 | 分布式场景 |

**推荐方案（先更 DB 再删缓存）：**

```java
@Transactional
public void updateData(Data data) {
    // 1. 更新数据库
    dataRepository.save(data);
    
    // 2. 删除缓存
    cacheManager.deleteCache(data.getId());
}
```

***

### Q68: 如何处理缓存穿透？

**一句话回答：**

> "布隆过滤器、缓存空值、接口限流。"

**解决方案：**

```
1. 布隆过滤器（推荐）
   - 拦截不存在的 Key
   - 误判率低
   
2. 缓存空值
   - 查询结果为空也缓存
   - TTL 设置短一些（5 分钟）
   
3. 接口限流
   - 防止恶意攻击
   - 限流阈值调低
```

***

### Q69: 如何处理缓存击穿？

**一句话回答：**

> "互斥锁、逻辑过期、永不过期。"

**解决方案：**

```
1. 互斥锁（推荐）
   - 第一个线程查数据库
   - 其他线程等待
   - 更新缓存后释放锁
   
2. 逻辑过期
   - 缓存永不过期
   - 后台线程异步更新
   - 返回旧数据
   
3. 永不过期
   - 物理过期时间很长
   - 后台定时更新
```

**互斥锁示例：**

```java
public Data getData(Long id) {
    Data data = cache.get(id);
    if (data == null) {
        String lockKey = "lock:" + id;
        if (redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS)) {
            try {
                // 双重检查
                data = cache.get(id);
                if (data == null) {
                    data = db.findById(id);
                    cache.set(id, data);
                }
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            // 等待后重试
            Thread.sleep(100);
            return getData(id);
        }
    }
    return data;
}
```

***

### Q70: 如何处理缓存雪崩？

**一句话回答：**

> "随机 TTL、多级缓存、限流降级。"

**解决方案：**

```
1. 随机 TTL
   - 基础 TTL + 随机值
   - 避免同时过期
   
2. 多级缓存
   - L1 Caffeine + L2 Redis
   - Redis 故障 L1 仍可用
   
3. 限流降级
   - 数据库限流保护
   - 返回默认值
```

***

### Q71: 如何保证缓存一致性？

**一句话回答：**

> "TTL 过期、主动删除、消息队列通知、最终一致性。"

**一致性方案：**

```
1. TTL 过期（简单）
   - 设置合理过期时间
   - 允许短暂不一致
   
2. 主动删除（推荐）
   - 更新 DB 后删除缓存
   - 下次查询重建缓存
   
3. 消息队列通知（分布式）
   - 更新 DB 后发消息
   - 所有实例删除缓存
   
4. 最终一致性
   - 不要求强一致
   - 秒级最终一致
```

***

### Q72: 如何设计缓存 Key？

**一句话回答：**

> "命名规范（业务：类型：ID）、避免冲突、可读性。"

**Key 设计规范：**

```
格式：业务名：类型：ID

示例：
user:info:1001          # 用户信息
device:status:d001      # 设备状态
order:detail:20240101   # 订单详情

好处：
- 避免 Key 冲突
- 便于管理
- 易于排查
```

***

### Q73: 如何监控缓存？

**一句话回答：**

> "命中率、加载时间、淘汰率、内存使用、连接数。"

**监控指标：**

```
1. 命中率
   - 目标：> 90%
   - 过低：缓存策略问题
   
2. 加载时间
   - 目标：< 10ms
   - 过高：数据库慢查询
   
3. 淘汰率
   - 目标：< 10%
   - 过高：内存不足
   
4. 内存使用
   - 目标：< 80%
   - 过高：需扩容
   
5. 连接数
   - 目标：< 80%
   - 过高：连接池不足
```

***

### Q74: 如何优化缓存性能？

**一句话回答：**

> "本地缓存（Caffeine）、批量加载、预热、Pipeline。"

**优化技巧：**

```
1. 本地缓存
   - 热点数据 Caffeine
   - 减少网络开销
   
2. 批量加载
   - 一次加载多条数据
   - 减少数据库查询
   
3. 缓存预热
   - 启动时加载热点数据
   - 避免冷启动
   
4. Pipeline
   - 批量操作 Redis
   - 减少网络往返
```

***

### Q75: Redis 持久化方案？

**一句话回答：**

> "RDB（快照）、AOF（追加日志）、混合持久化（Redis 4.0+）。"

**持久化对比：**

| 方案  | 优点      | 缺点      | 适用场景 |
| --- | ------- | ------- | ---- |
| RDB | 文件小、恢复快 | 数据丢失多   | 备份   |
| AOF | 数据丢失少   | 文件大、恢复慢 | 高可用  |
| 混合  | 平衡      | 折中      | 推荐   |

**推荐配置（混合持久化）：**

```conf
# 开启 AOF
appendonly yes

# 混合持久化
aof-use-rdb-preamble yes

# AOF 刷盘策略（每秒）
appendfsync everysec

# RDB 快照
save 900 1
save 300 10
save 60 10000
```

***

## 7. 并发编程

### Q76: 线程池参数如何配置？

**一句话回答：**

> "核心线程数（CPU 核心数）、最大线程数（CPU\*2+1）、队列容量、拒绝策略。"

**参数配置：**

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    8,                      // 核心线程数（CPU 核心数）
    16,                     // 最大线程数（CPU*2）
    60L, TimeUnit.SECONDS,  // 空闲线程存活时间
    new LinkedBlockingQueue<>(100),  // 队列容量
    new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
);
```

**调优公式：**

```
CPU 密集型：核心线程数 = CPU 核心数 + 1
IO 密集型：核心线程数 = CPU 核心数 * 2
混合型：根据 IO/CPU 比例调整
```

***

### Q77: 线程池拒绝策略？

**一句话回答：**

> "AbortPolicy（抛异常）、CallerRunsPolicy（调用线程执行）、DiscardPolicy（丢弃）、DiscardOldestPolicy（丢弃最老）。"

**拒绝策略对比：**

| 策略                  | 行为     | 适用场景    |
| ------------------- | ------ | ------- |
| AbortPolicy         | 抛异常    | 不能丢失任务  |
| CallerRunsPolicy    | 调用线程执行 | 推荐，不丢失  |
| DiscardPolicy       | 直接丢弃   | 允许丢失    |
| DiscardOldestPolicy | 丢弃最老   | 允许丢弃旧任务 |

**推荐 CallerRunsPolicy：**

```
优点：
- 不丢失任务
- 自动降速（阻塞生产者）
- 背压机制
```

***

### Q78: 如何保证线程安全？

**一句话回答：**

> "synchronized、ReentrantLock、原子类、ThreadLocal、不可变对象。"

**线程安全方案：**

```
1. synchronized（内置锁）
   - 简单、自动
   - 性能一般
   
2. ReentrantLock（显式锁）
   - 灵活、可中断
   - 需手动释放
   
3. 原子类（AtomicInteger）
   - CAS 无锁
   - 性能高
   
4. ThreadLocal（线程隔离）
   - 每个线程独立副本
   - 避免共享
   
5. 不可变对象（final）
   - 创建后不可修改
   - 天然线程安全
```

***

### Q79: volatile 关键字作用？

**一句话回答：**

> "保证可见性、禁止指令重排序、不保证原子性。"

**volatile 作用：**

```
1. 可见性
   - 一个线程修改，其他线程立即可见
   - 通过内存屏障实现
   
2. 禁止指令重排序
   - 防止编译器优化导致乱序
   - 单例模式双重检查锁需要
   
3. 不保证原子性
   - i++ 操作仍需 synchronized
   - 用 AtomicLong 替代
```

**单例模式示例：**

```java
private static volatile Singleton instance;

public static Singleton getInstance() {
    if (instance == null) {
        synchronized (Singleton.class) {
            if (instance == null) {
                instance = new Singleton();  // volatile 防止重排序
            }
        }
    }
    return instance;
}
```

***

### Q80: ConcurrentHashMap 原理？

**一句话回答：**

> "JDK1.8 数组 + 链表 + 红黑树，CAS+synchronized 保证线程安全，分段锁优化并发。"

**实现原理：**

```
JDK 1.7：
- Segment 分段锁
- 16 个 Segment
- 并发度 16

JDK 1.8：
- 数组 + 链表 + 红黑树
- CAS + synchronized
- 并发度更高
```

**线程安全保证：**

```
1. 读操作：CAS，无锁
2. 写操作：synchronized 锁单个节点
3. 扩容：多线程并行扩容
```

***

### Q81: ThreadLocal 使用场景？

**一句话回答：**

> "线程隔离数据（用户上下文、数据库连接）、避免参数传递、内存泄漏预防。"

**使用场景：**

```java
// 1. 用户上下文
ThreadLocal<UserContext> userContext = new ThreadLocal<>();

// 2. 数据库连接
ThreadLocal<Connection> connection = new ThreadLocal<>();

// 3. 日期格式化
ThreadLocal<SimpleDateFormat> dateFormat = 
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
```

**内存泄漏预防：**

```java
// 使用完必须 remove
try {
    userContext.set(user);
    // 业务逻辑
} finally {
    userContext.remove();  // 防止内存泄漏
}
```

***

### Q82: 如何实现异步编程？

**一句话回答：**

> "CompletableFuture、线程池、@Async、Reactive Streams。"

**异步编程方案：**

```java
// 1. CompletableFuture（推荐）
CompletableFuture.supplyAsync(() -> {
    return fetchData();
}).thenApply(data -> {
    return processData(data);
}).exceptionally(ex -> {
    return defaultValue;
});

// 2. @Async 注解
@Async
public void asyncMethod() {
    // 异步执行
}

// 3. 线程池
ExecutorService executor = Executors.newFixedThreadPool(10);
executor.submit(() -> {
    // 异步任务
});
```

***

### Q83: 虚拟线程优势？

**一句话回答：**

> "内存占用小（1KB）、创建快（0.1μs）、并发高（100 万+）、适合 IO 密集型。"

**虚拟线程 vs 平台线程：**

| 特性   | 平台线程 | 虚拟线程   | 提升     |
| ---- | ---- | ------ | ------ |
| 栈大小  | 1MB  | 1KB    | 1000 倍 |
| 创建时间 | 10μs | 0.1μs  | 100 倍  |
| 最大数量 | 1 万  | 100 万  | 100 倍  |
| 适用场景 | 混合型  | IO 密集型 | -      |

**使用示例：**

```java
// 创建虚拟线程
Thread.startVirtualThread(() -> {
    // IO 密集型任务
    handleRequest();
});

// 虚拟线程池
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

***

## 8. 安全认证

### Q84: JWT 组成结构？

**一句话回答：**

> "Header（算法 + 类型）、Payload（载荷数据）、Signature（签名），Base64 编码。"

**JWT 结构：**

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.     # Header
eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.  # Payload
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c  # Signature
```

**各部分说明：**

```
1. Header
   - alg: 签名算法（HS256、RS256）
   - typ: 类型（JWT）
   
2. Payload
   - iss: 签发者
   - sub: 主题
   - exp: 过期时间
   - iat: 签发时间
   - 自定义数据（用户 ID、角色）
   
3. Signature
   - 对 Header+Payload 签名
   - 防止篡改
```

***

### Q85: JWT 如何验证？

**一句话回答：**

> "验证签名、验证过期时间、验证签发者、验证黑名单。"

**验证流程：**

```java
public Claims validateToken(String token) {
    try {
        // 1. 验证签名
        Claims claims = Jwts.parser()
            .setSigningKey(secretKey)
            .parseClaimsJws(token)
            .getBody();
        
        // 2. 验证过期时间
        if (claims.getExpiration().before(new Date())) {
            throw new TokenExpiredException();
        }
        
        // 3. 验证黑名单
        if (tokenBlacklist.isBlacklisted(token)) {
            throw new TokenBlacklistedException();
        }
        
        return claims;
        
    } catch (Exception e) {
        throw new InvalidTokenException();
    }
}
```

***

### Q86: 如何防止 Token 泄露？

**一句话回答：**

> "HTTPS 传输、HttpOnly Cookie、Token 刷新、黑名单机制。"

**安全措施：**

```
1. HTTPS 传输
   - 防止中间人攻击
   - 加密传输
   
2. HttpOnly Cookie
   - 防止 XSS 攻击
   - JS 无法读取
   
3. Token 刷新
   - Access Token 短有效期（2 小时）
   - Refresh Token 长有效期（7 天）
   - 定期刷新
   
4. 黑名单机制
   - 登出时加入黑名单
   - 异常时强制失效
```

***

### Q87: 如何防止 SQL 注入？

**一句话回答：**

> "预编译语句（PreparedStatement）、参数化查询、ORM 框架、输入校验。"

**安全示例：**

```java
// ❌ 不安全：字符串拼接
String sql = "SELECT * FROM users WHERE username = '" + username + "'";

// ✅ 安全：预编译语句
String sql = "SELECT * FROM users WHERE username = ?";
PreparedStatement ps = connection.prepareStatement(sql);
ps.setString(1, username);

// ✅ 安全：JPA
@Query("SELECT u FROM User u WHERE u.username = :username")
User findByUsername(@Param("username") String username);
```

***

### Q88: 如何防止 XSS 攻击？

**一句话回答：**

> "输入过滤、输出编码、HttpOnly Cookie、CSP 策略。"

**防护方案：**

```
1. 输入过滤
   - 过滤<script>标签
   - 过滤特殊字符
   
2. 输出编码
   - HTML 实体编码
   - < → &lt;
   - > → &gt;
   
3. HttpOnly Cookie
   - 防止 JS 读取 Cookie
   
4. CSP 策略
   - Content-Security-Policy
   - 限制资源加载来源
```

***

### Q89: 如何防止 CSRF 攻击？

**一句话回答：**

> "CSRF Token、SameSite Cookie、验证 Referer。"

**防护方案：**

```
1. CSRF Token
   - 表单提交携带 Token
   - 服务端验证 Token
   
2. SameSite Cookie
   - Set-Cookie: SameSite=Strict
   - 禁止第三方 Cookie
   
3. 验证 Referer
   - 检查请求来源
   - 非本站拒绝
```

***

### Q90: 如何加密存储密码？

**一句话回答：**

> "PBKDF2（推荐）、BCrypt、加盐哈希、不可逆加密。"

**密码加密方案：**

```java
// 1. PBKDF2（推荐）
PBEKeySpec spec = new PBEKeySpec(
    password.toCharArray(),
    salt,           // 随机盐
    65536,          // 迭代次数
    256             // 密钥长度
);
SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
byte[] hash = skf.generateSecret(spec).getEncoded();

// 2. BCrypt（Spring Security 默认）
PasswordEncoder encoder = new BCryptPasswordEncoder();
String encodedPassword = encoder.encode(password);
```

**注意事项：**

- 必须加盐（防止彩虹表）
- 迭代次数足够（增加破解成本）
- 使用安全随机数生成盐

***

### Q91: 如何实现权限控制？

**一句话回答：**

> "Spring Security、RBAC 模型、注解鉴权（@PreAuthorize）、方法级权限。"

**权限控制方案：**

```java
// 1. 配置安全策略
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/user/**").hasAnyRole("ADMIN", "USER")
                .anyRequest().authenticated()
            )
            .httpBasic();
        return http.build();
    }
}

// 2. 方法级权限
@PreAuthorize("hasAuthority('device:write')")
public void updateDevice(Device device) {
    // ...
}

// 3. 数据级权限
@PreAuthorize("@permissionService.canAccessDevice(authentication, #deviceId)")
public Device getDevice(Long deviceId) {
    // ...
}
```

***

### Q92: 如何实现单点登录（SSO）？

**一句话回答：**

> "OAuth2、CAS、JWT、统一认证中心。"

**SSO 方案对比：**

| 方案     | 优点         | 缺点         | 适用场景 |
| ------ | ---------- | ---------- | ---- |
| OAuth2 | 标准协议、支持第三方 | 复杂         | 开放平台 |
| CAS    | 简单、成熟      | 需额外部署      | 企业内部 |
| JWT    | 无状态、跨域     | Token 管理复杂 | 微服务  |

**OAuth2 流程：**

```
1. 用户访问应用
2. 应用重定向到认证中心
3. 用户登录认证中心
4. 认证中心返回授权码
5. 应用用授权码换取 Token
6. 应用用 Token 访问资源
```

***

## 9. 性能优化

### Q93: 如何优化接口响应时间？

**一句话回答：**

> "缓存热点数据、异步处理、批量查询、数据库索引、连接池优化。"

**优化方案：**

```
1. 缓存优化
   - 热点数据缓存
   - 多级缓存架构
   
2. 异步处理
   - 非关键操作异步
   - 消息队列解耦
   
3. 批量查询
   - N+1 问题优化
   - 一次查询多条数据
   
4. 数据库优化
   - 添加索引
   - 优化 SQL
   
5. 连接池优化
   - 合理配置参数
   - 监控连接使用
```

**优化效果：** 200ms → 50ms

***

### Q94: 如何优化数据库性能？

**一句话回答：**

> "索引优化、SQL 优化、分库分表、读写分离、连接池调优。"

**优化技巧：**

```sql
-- 1. 索引优化
CREATE INDEX idx_device_ts ON telemetry(device_id, ts DESC);

-- 2. SQL 优化
-- ❌ 慢：SELECT * FROM telemetry WHERE YEAR(ts) = 2024
-- ✅ 快：SELECT * FROM telemetry WHERE ts BETWEEN '2024-01-01' AND '2024-12-31'

-- 3. 分库分表
-- 按时间范围分表：telemetry_202401, telemetry_202402, ...

-- 4. 读写分离
-- 主库写，从库读

-- 5. 连接池调优
-- 最大连接数：((CPU*2)+1)*磁盘数
```

***

### Q95: 如何优化 JVM 性能？

**一句话回答：**

> "选择合适的 GC（G1）、调整堆大小、减少对象创建、监控 GC 日志。"

**JVM 参数调优：**

```bash
# 2 核 2G 服务器配置
java -server \
  -Xms384m \                    # 最小堆
  -Xmx512m \                    # 最大堆
  -XX:+UseG1GC \                # G1 垃圾收集器
  -XX:MaxGCPauseMillis=200 \    # GC 停顿目标
  -XX:+UseContainerSupport \    # 容器感知
  -XX:MaxRAMPercentage=70.0 \   # 堆内存比例
  -jar app.jar
```

**优化技巧：**

- 减少对象创建（复用对象）
- 使用基本类型（避免装箱）
- 使用 StringBuilder（字符串拼接）
- 及时释放引用（避免内存泄漏）

***

### Q96: 如何优化前端加载速度？

**一句话回答：**

> "路由懒加载、图片懒加载、Gzip 压缩、CDN 加速、Tree Shaking。"

**优化技巧：**

```
1. 路由懒加载：首屏体积减少 60%
2. 图片懒加载：只加载可视区域
3. Gzip 压缩：体积减少 70%
4. CDN 加速：静态资源走 CDN
5. Tree Shaking：按需引入组件
6. 代码分割：分包加载
7. 缓存策略：强缓存 + 协商缓存
```

**优化效果：** 首屏加载 3 秒 → 1 秒

***

### Q97: 如何优化 Redis 性能？

**一句话回答：**

> "Pipeline 批量操作、Lua 脚本、避免大 Key、合理数据结构、持久化优化。"

**优化技巧：**

```
1. Pipeline 批量操作
   - 减少网络往返
   - 一次执行多条命令
   
2. Lua 脚本
   - 原子操作
   - 减少网络开销
   
3. 避免大 Key
   - Key 大小 < 10KB
   - 大对象拆分存储
   
4. 合理数据结构
   - 简单场景用 String
   - 复杂场景用 Hash
   
5. 持久化优化
   - AOF 每秒刷盘
   - RDB 定时快照
```

***

### Q98: 如何优化网络性能？

**一句话回答：**

> "连接池、Keep-Alive、HTTP/2、CDN、压缩。"

**优化技巧：**

```
1. 连接池
   - 复用 TCP 连接
   - 减少握手开销
   
2. Keep-Alive
   - 长连接
   - 减少连接建立
   
3. HTTP/2
   - 多路复用
   - 头部压缩
   
4. CDN
   - 就近访问
   - 减少延迟
   
5. 压缩
   - Gzip/Brotli
   - 减少传输体积
```

***

### Q99: 如何优化系统吞吐量？

**一句话回答：**

> "异步处理、消息队列、水平扩展、缓存、批量处理。"

**优化方案：**

```
1. 异步处理
   - 非关键操作异步
   - 提升响应速度
   
2. 消息队列
   - 削峰填谷
   - 异步解耦
   
3. 水平扩展
   - 增加实例数
   - 负载均衡
   
4. 缓存
   - 减少数据库查询
   - 提升响应速度
   
5. 批量处理
   - 减少 IO 次数
   - 提升吞吐
```

**优化效果：** 500/s → 5000/s（10 倍）

***

### Q100: 如何优化内存使用？

**一句话回答：**

> "对象池、软引用、及时释放、避免内存泄漏、JVM 参数调优。"

**优化技巧：**

```
1. 对象池
   - 复用对象
   - 减少创建开销
   
2. 软引用
   - 内存不足时回收
   - 缓存场景
   
3. 及时释放
   - 置为 null
   - 移除集合
   
4. 避免内存泄漏
   - ThreadLocal 用完 remove
   - 监听器及时注销
   
5. JVM 参数调优
   - 合理设置堆大小
   - 选择合适的 GC
```

***

### Q101: 如何定位性能瓶颈？

**一句话回答：**

> "APM 工具（SkyWalking）、JVM 监控、慢查询日志、火焰图、压力测试。"

**定位工具：**

```
1. APM 工具
   - SkyWalking
   - Pinpoint
   - 链路追踪
   
2. JVM 监控
   - JConsole
   - VisualVM
   - JFR
   
3. 慢查询日志
   - MySQL slow log
   - Redis slow log
   
4. 火焰图
   - 分析 CPU 热点
   - 定位瓶颈
   
5. 压力测试
   - JMeter
   - 找出瓶颈点
```

***

### Q102: 如何进行压力测试？

**一句话回答：**

> "JMeter、Gradual Ramp-Up、监控指标、找出瓶颈、持续优化。"

**压测步骤：**

```
1. 准备测试数据
   - 真实数据量级
   - 脱敏处理
   
2. 配置压测场景
   - 并发用户数
   - 压测时长
   - Ramp-Up 时间
   
3. 执行压测
   - 逐步增加并发
   - 监控各项指标
   
4. 分析结果
   - 响应时间
   - 吞吐量
   - 错误率
   
5. 优化迭代
   - 定位瓶颈
   - 优化代码
   - 重新压测
```

***

## 10. 运维部署

### Q103: Docker 优势？

**一句话回答：**

> "环境一致、快速部署、资源隔离、版本管理、易于扩展。"

**Docker 优势：**

```
1. 环境一致
   - 开发、测试、生产一致
   - 避免"在我机器上能跑"
   
2. 快速部署
   - 秒级启动
   - 比虚拟机快
   
3. 资源隔离
   - CPU、内存隔离
   - 互不影响
   
4. 版本管理
   - 镜像版本控制
   - 快速回滚
   
5. 易于扩展
   - 水平扩展
   - 容器编排
```

***

### Q104: Docker Compose 作用？

**一句话回答：**

> "多容器编排、一键启动、服务依赖、网络隔离、数据卷管理。"

**Docker Compose 示例：**

```yaml
version: '3.8'
services:
  backend:
    image: dorm-power:latest
    ports:
      - "8000:8000"
    depends_on:
      - postgres
      - redis
    environment:
      - DB_HOST=postgres
      - REDIS_HOST=redis
  
  postgres:
    image: postgres:16
    volumes:
      - pgdata:/var/lib/postgresql/data
  
  redis:
    image: redis:7
    command: redis-server --maxmemory 128mb

volumes:
  pgdata:
```

***

### Q105: 如何监控 Docker 容器？

**一句话回答：**

> "docker stats、Prometheus、cAdvisor、日志收集。"

**监控方案：**

```
1. docker stats
   - 实时资源使用
   - CPU、内存、网络
   
2. Prometheus + cAdvisor
   - 容器指标采集
   - Grafana 展示
   
3. 日志收集
   - ELK Stack
   - 集中查看日志
   
4. 健康检查
   - HEALTHCHECK 指令
   - 自动重启
```

***

### Q106: 如何实现 CI/CD？

**一句话回答：**

> "GitHub Actions、Jenkins、GitLab CI，自动化构建、测试、部署。"

**CI/CD 流程：**

```
1. 代码提交
   - Git Push
   
2. 触发构建
   - GitHub Actions
   - 自动拉取代码
   
3. 执行测试
   - 单元测试
   - 集成测试
   
4. 构建镜像
   - Docker Build
   - 推送镜像仓库
   
5. 部署
   - SSH 远程部署
   - K8s 滚动更新
   
6. 验证
   - 健康检查
   -  smoke test
```

***

### Q107: 如何保证高可用？

**一句话回答：**

> "多实例部署、负载均衡、健康检查、自动重启、故障转移。"

**高可用方案：**

```
1. 多实例部署
   - 至少 2 个实例
   - 避免单点故障
   
2. 负载均衡
   - Nginx 反向代理
   - 分发请求
   
3. 健康检查
   - 定期检查
   - 故障自动摘除
   
4. 自动重启
   - restart: always
   - 故障自动恢复
   
5. 故障转移
   - 主从切换
   - 数据不丢失
```

***

### Q108: 如何备份数据？

**一句话回答：**

> "mysqldump 定时备份、Redis RDB/AOF、异地备份、定期演练。"

**备份策略：**

```
1. 数据库备份
   - mysqldump 每天全量
   - binlog 增量备份
   
2. Redis 备份
   - RDB 快照
   - AOF 日志
   
3. 备份存储
   - 本地备份
   - 异地备份（不同机房）
   
4. 备份验证
   - 定期恢复演练
   - 验证备份有效性
```

***

### Q109: 如何处理生产事故？

**一句话回答：**

> "监控告警、快速回滚、问题定位、修复上线、复盘总结。"

**事故处理流程：**

```
1. 发现告警
   - 监控告警
   - 用户反馈
   
2. 快速响应
   - 确认问题
   - 评估影响
   
3. 紧急处理
   - 快速回滚
   - 降级服务
   
4. 问题定位
   - 查看日志
   - 分析监控
   
5. 修复上线
   - 紧急修复
   - 测试验证
   
6. 复盘总结
   - 事故报告
   - 改进措施
```

***

## 11. 项目实战

### Q110: 项目遇到的最大挑战？

**一句话回答：**

> "2 核 2G 服务器支持 10,000+ 设备并发，通过虚拟线程、多级缓存、RabbitMQ 解决。"

**STAR 回答：**

```
S（情境）：
- 2 核 2G 服务器
- 需支持 10,000+ 设备并发
- 内存和 CPU 资源紧张

T（任务）：
- 在有限资源下实现高并发
- 保证响应时间<50ms

A（行动）：
- 引入 Java 21 虚拟线程
- Caffeine+Redis 多级缓存
- RabbitMQ 异步解耦
- Redis+Lua 分布式限流

R（结果）：
- 并发连接从 500 提升到 10,000+
- 响应时间从 200ms 降到 50ms
- 内存占用从 1.5GB 降到 512MB
```

***

### Q111: 项目中最有成就感的功能？

**一句话回答：**

> "RabbitMQ 异步解耦，三大异步链路，系统吞吐提升 10 倍。"

**详细介绍：**

```
背景：
- 同步处理遥测数据
- 吞吐量仅 500 条/秒
- 高峰期数据积压

方案：
- 引入 RabbitMQ
- 遥测数据异步落库
- 告警消息异步推送
- 账单生成异步处理

效果：
- 吞吐量提升到 5000 条/秒（10 倍）
- 响应时间降低到 50ms 以内
- 削平流量峰值
```

***

### Q112: 项目中的技术难点？

**一句话回答：**

> "WebSocket 高并发连接管理，通过 JUC 并发组件、虚拟线程、异步发送解决。"

**难点解析：**

```
难点 1：线程安全
- 使用 CopyOnWriteArraySet 管理连接
- ConcurrentHashMap 管理订阅关系

难点 2：内存占用
- 虚拟线程替代平台线程
- 内存从 1GB 降到 50MB

难点 3：网络阻塞
- 独立线程池异步发送
- 避免阻塞业务线程

难点 4：精准推送
- 订阅机制
- 只推送给关注设备的用户
```

***

### Q113: 如何保证代码质量？

**一句话回答：**

> "单元测试、代码审查、静态检查、持续集成、规范文档。"

**质量保证：**

```
1. 单元测试
   - 覆盖率>80%
   - 核心功能 100%
   
2. 代码审查
   - Merge Request
   - 至少 1 人 Review
   
3. 静态检查
   - SonarQube
   - Checkstyle
   
4. 持续集成
   - 自动化测试
   - 构建失败不部署
   
5. 规范文档
   - 代码规范
   - Git 提交规范
```

***

### Q114: 如何学习新技术？

**一句话回答：**

> "官方文档、实战项目、技术博客、源码分析、总结分享。"

**学习方法：**

```
1. 官方文档
   - 第一手资料
   - 最权威
   
2. 实战项目
   - 动手实践
   - 遇到问题解决问题
   
3. 技术博客
   - 最佳实践
   - 踩坑经验
   
4. 源码分析
   - 深入理解原理
   - 学习设计思想
   
5. 总结分享
   - 写技术博客
   - 内部分享
```

***

### Q115: 团队协作经验？

**一句话回答：**

> "Git 工作流、代码规范、定期同步、知识分享、文档沉淀。"

**协作经验：**

```
1. Git 工作流
   - Feature 分支开发
   - MR 合并到 main
   - 保护 main 分支
   
2. 代码规范
   - 统一编码风格
   - 自动格式化
   
3. 定期同步
   - 每日站会
   - 每周技术分享
   
4. 知识分享
   - Wiki 文档
   - 技术培训
   
5. 文档沉淀
   - API 文档
   - 架构文档
```

***

### Q116: 项目如何迭代？

**一句话回答：**

> "敏捷开发、2 周一个 Sprint、用户反馈、持续交付、数据驱动。"

**迭代流程：**

```
1. 需求收集
   - 用户反馈
   - 数据分析
   
2. 优先级排序
   - 重要且紧急
   - 重要不紧急
   
3. Sprint 规划
   - 2 周一个迭代
   - 评估工作量
   
4. 开发测试
   - 每日站会
   - 持续集成
   
5. 上线发布
   - 灰度发布
   - 监控指标
   
6. 复盘总结
   - 回顾会议
   - 改进措施
```

***

### Q117: 项目未来规划？

**一句话回答：**

> "微服务拆分、K8s 容器编排、Service Mesh、AI 智能分析、边缘计算。"

**未来规划：**

```
短期（3 个月）：
- 性能优化
- 监控完善
- 文档补充

中期（6 个月）：
- 微服务拆分
- K8s 容器编排
- 服务网格

长期（1 年）：
- AI 智能分析
- 边缘计算
- 多租户支持
```

***

### Q118: 如果重新设计会怎么做？

**一句话回答：**

> "从一开始就建立完整监控、引入 K8s、采用事件驱动架构、更严格的代码审查。"

**改进方向：**

```
1. 监控体系
   - 从第一天就建立
   - 数据驱动优化
   
2. 容器编排
   - 直接上 K8s
   - 更好的弹性伸缩
   
3. 架构设计
   - 事件驱动架构
   - CQRS 模式
   
4. 代码质量
   - 更严格的审查
   - 更高的测试覆盖率
   
5. 文档建设
   - 架构决策记录
   - API 文档自动化
```

***

### Q119: 项目的商业价值？

**一句话回答：**

> "降低用电成本 30%、提升管理效率 5 倍、预防电气火灾、数据驱动决策。"

**商业价值：**

```
1. 经济效益
   - 降低用电成本 30%
   - 减少人工巡检
   - 预防设备损坏
   
2. 管理效益
   - 远程监控
   - 自动告警
   - 数据报表
   
3. 安全效益
   - 预防电气火灾
   - 及时发现异常
   - 保障生命财产安全
   
4. 数据价值
   - 用电行为分析
   - 节能优化建议
   - 设备寿命预测
```

***

## 📊 速查表

### 性能数据汇总

| 指标    | 优化前   | 优化后     | 提升    |
| ----- | ----- | ------- | ----- |
| 并发连接  | 500   | 10,000+ | 20 倍  |
| 响应时间  | 200ms | 50ms    | 4 倍   |
| 消息吞吐  | 500/s | 5000/s  | 10 倍  |
| 缓存命中率 | 60%   | 95%     | 1.6 倍 |
| 内存占用  | 1.5GB | 512MB   | 3 倍   |

### 技术栈关键词

| 技术       | 关键词                   | 核心指标             |
| -------- | --------------------- | ---------------- |
| Redis    | SETNX、Lua、持久化         | <1ms、10,000+ QPS |
| RabbitMQ | Exchange、Queue、ACK    | <50ms、5000/s     |
| 虚拟线程     | Java 21、1KB 栈         | 10,000+ 并发       |
| 多级缓存     | Caffeine+Redis        | 95% 命中率          |
| JWT      | Token、签名、黑名单          | 无状态、可扩展          |
| Vue 3    | Composition API、Proxy | 性能 +40%          |

***

**最后提醒：自信！所有技术都是实际实现的！** 🎉
