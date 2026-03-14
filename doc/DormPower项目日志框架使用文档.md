# DormPower项目日志框架使用文档

## 面试导向 - 项目日志实践解析

---

## 目录

1. [项目日志架构选型](#1-项目日志架构选型)
2. [项目日志配置详解](#2-项目日志配置详解)
3. [项目日志使用场景](#3-项目日志使用场景)
4. [面试问答指南](#4-面试问答指南)
5. [项目日志最佳实践](#5-项目日志最佳实践)

---

## 1. 项目日志架构选型

### 1.1 项目使用的日志技术栈

```
┌─────────────────────────────────────────────────────────┐
│                    DormPower日志架构                      │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  应用代码层                                              │
│  └── 使用SLF4J API                                      │
│      Logger logger = LoggerFactory.getLogger(Xxx.class) │
│                                                          │
│  门面层                                                  │
│  └── SLF4J API (slf4j-api)                              │
│      - 统一日志接口                                      │
│      - 占位符支持                                        │
│                                                          │
│  绑定层                                                  │
│  └── log4j-slf4j2-impl (Log4j2 SLF4J绑定)               │
│      - 将SLF4J调用桥接到Log4j2                           │
│                                                          │
│  实现层                                                  │
│  ├── log4j-core (Log4j2核心)                            │
│  │   - 高性能异步日志                                    │
│  │   - 插件机制                                          │
│  └── logback-classic (Logback实现)                      │
│      - Spring Boot默认                                  │
│      - 配置文件: logback-spring.xml                      │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### 1.2 Maven依赖配置

```xml
<!-- pom.xml中的日志依赖 -->

<!-- Log4j2核心 -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.20.0</version>
</dependency>

<!-- Log4j2 SLF4J绑定 -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-slf4j2-impl</artifactId>
    <version>2.20.0</version>
</dependency>
```

**面试要点：为什么这样配置？**

```
面试回答模板：

"我们项目使用SLF4J作为日志门面，Log4j2作为日志实现。

选择SLF4J的原因：
1. 门面模式解耦：应用程序不依赖具体日志实现
2. 占位符特性：避免不必要的字符串拼接，性能更好
3. 统一API：团队开发规范统一

选择Log4j2的原因：
1. 高性能：Disruptor异步日志，吞吐量是Logback的10倍+
2. 插件机制：可扩展性强
3. 功能丰富：支持JSON输出、Lambda表达式等

为什么同时配置Logback？
Spring Boot默认使用Logback，我们的配置文件是logback-spring.xml。
Log4j2依赖主要用于兼容性和特定场景的高性能日志需求。"
```

### 1.3 为什么选择这个日志架构？

| 考量因素 | 我们的选型 | 理由 |
|----------|------------|------|
| 性能要求 | Log4j2 | 物联网设备数据量大，需要高性能日志 |
| 开发效率 | SLF4J门面 | 统一API，团队协作方便 |
| 运维需求 | 结构化日志 | 便于日志分析和问题排查 |
| Spring集成 | Logback | Spring Boot默认，配置简单 |

---

## 2. 项目日志配置详解

### 2.1 logback-spring.xml配置文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    
    <!-- ==================== 控制台输出 ==================== -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <!-- 日志格式：时间 线程 级别 类名 消息 -->
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- ==================== 应用日志文件 ==================== -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/dorm-power.log</file>
        
        <!-- 滚动策略：按天滚动 -->
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/dorm-power.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>           <!-- 保留7天 -->
            <totalSizeCap>100MB</totalSizeCap>   <!-- 总大小限制 -->
        </rollingPolicy>
        
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- ==================== 审计日志文件（独立） ==================== -->
    <appender name="AUDIT_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/audit.log</file>
        
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/audit.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>          <!-- 审计日志保留30天 -->
            <totalSizeCap>200MB</totalSizeCap>
        </rollingPolicy>
        
        <encoder>
            <!-- 审计日志格式更简洁 -->
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- ==================== 审计日志Logger ==================== -->
    <logger name="AUDIT" level="INFO" additivity="false">
        <appender-ref ref="AUDIT_FILE"/>
    </logger>

    <!-- ==================== 包级别日志配置 ==================== -->
    <logger name="com.dormpower" level="INFO"/>
    
    <!-- 减少框架日志噪音 -->
    <logger name="org.springframework" level="WARN"/>
    <logger name="org.hibernate" level="WARN"/>

    <!-- ==================== 根日志配置 ==================== -->
    <root level="WARN">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>

</configuration>
```

### 2.2 配置要点解析

**面试要点：为什么这样配置？**

```
面试回答模板：

"我们的日志配置分为三个层次：

1. 日志输出目标（Appender）：
   - CONSOLE：开发调试用，输出到控制台
   - FILE：应用日志，按天滚动，保留7天
   - AUDIT_FILE：审计日志，独立文件，保留30天

2. 日志级别控制：
   - 业务代码(com.dormpower)：INFO级别
   - 框架代码(Spring/Hibernate)：WARN级别，减少噪音
   - 根级别：WARN，避免过多日志

3. 审计日志独立：
   - 使用独立的Logger名称'AUDIT'
   - additivity=false，不向上传播
   - 单独的文件和保留策略

这样配置的好处：
- 业务日志和审计日志分离，便于问题排查
- 滚动策略避免日志文件过大
- 级别控制减少不必要的日志输出"
```

### 2.3 日志格式详解

```
日志格式Pattern：
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n

各部分含义：
%d{yyyy-MM-dd HH:mm:ss.SSS}  - 时间戳，精确到毫秒
[%thread]                     - 线程名，便于追踪并发问题
%-5level                      - 日志级别，左对齐占5字符
%logger{36}                   - Logger名称，最长36字符
%msg                          - 日志消息
%n                            - 换行符

实际输出示例：
2024-03-14 10:30:45.123 [http-nio-8080-exec-1] INFO  c.d.service.DeviceService - 设备注册成功: device001
```

---

## 3. 项目日志使用场景

### 3.1 场景一：AOP切面日志

**代码位置**：[ApiAspect.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/aop/ApiAspect.java)

```java
@Aspect
@Component
public class ApiAspect {

    // 普通业务日志
    private static final Logger logger = LoggerFactory.getLogger(ApiAspect.class);
    
    // 审计日志（独立Logger）
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

    /**
     * API请求日志和限流处理
     */
    @Around("@annotation(com.dormpower.annotation.RateLimit) || " +
            "@within(org.springframework.web.bind.annotation.RestController)")
    public Object handleApiRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        HttpServletRequest request = getRequest();
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";
        String ip = request != null ? getClientIp(request) : "UNKNOWN";

        logger.debug("请求开始: {} {} from {}", method, uri, ip);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录请求完成日志
            logger.info("请求完成: {} {} - {}ms - {}", method, uri, duration, ip);
            return result;
            
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录请求失败日志
            logger.error("请求失败: {} {} - {}ms - {} - {}", method, uri, duration, ip, e.getMessage());
            throw e;
        }
    }

    /**
     * 审计日志处理
     */
    @Around("@annotation(com.dormpower.annotation.AuditLog)")
    public Object handleAuditLog(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuditLog auditLog = method.getAnnotation(AuditLog.class);

        HttpServletRequest request = getRequest();
        String ip = request != null ? getClientIp(request) : "UNKNOWN";
        String user = request != null ? request.getHeader("X-User") : "anonymous";

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            // 审计日志：成功
            auditLogger.info("[{}] {} - {} - {}ms - SUCCESS",
                    auditLog.type(), auditLog.value(), user != null ? user : ip, duration);
            return result;
            
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            
            // 审计日志：失败
            auditLogger.error("[{}] {} - {} - {}ms - FAILED: {}",
                    auditLog.type(), auditLog.value(), user != null ? user : ip, duration, e.getMessage());
            throw e;
        }
    }
}
```

**面试要点：AOP日志的优势**

```
面试回答模板：

"我们使用AOP切面统一处理日志，有以下优势：

1. 代码解耦：
   - 日志逻辑与业务逻辑分离
   - 不需要在每个Controller方法中写日志代码

2. 统一规范：
   - 所有API请求的日志格式一致
   - 便于日志分析和监控

3. 功能增强：
   - 自动记录请求耗时
   - 自动获取客户端IP
   - 与限流功能结合

4. 审计日志独立：
   - 使用独立的Logger（AUDIT）
   - 输出到独立文件（audit.log）
   - 保留时间更长（30天）

实现原理：
- @Around环绕通知：可以在方法执行前后都添加逻辑
- ProceedingJoinPoint：获取方法参数、返回值等信息
- 反射获取注解信息：动态获取审计日志配置"
```

### 3.2 场景二：全局异常处理日志

**代码位置**：[GlobalExceptionHandler.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/exception/GlobalExceptionHandler.java)

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        logger.info("参数验证异常: {}", ex.getMessage());
        // ... 处理逻辑
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.info("资源未找到异常: {}", ex.getMessage());
        // ... 处理逻辑
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        logger.info("业务异常: {}", ex.getMessage());
        // ... 处理逻辑
    }

    /**
     * 处理运行时异常（包括限流错误）
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        logger.warn("运行时异常: {}", ex.getMessage(), ex);
        // ... 处理逻辑
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        logger.error("全局异常: {}", ex.getMessage(), ex);
        // ... 处理逻辑
    }
}
```

**面试要点：异常日志的级别选择**

```
面试回答模板：

"我们在全局异常处理器中，根据异常类型选择不同的日志级别：

1. INFO级别：
   - 参数验证异常：用户输入问题，不是系统错误
   - 资源未找到异常：正常的业务场景
   - 业务异常：预期的业务规则校验失败

2. WARN级别：
   - 运行时异常：需要关注但不影响系统运行
   - 限流触发：系统保护机制生效

3. ERROR级别：
   - 全局异常：未预期的系统错误
   - 需要打印完整堆栈，便于排查问题

这样设计的好处：
- 避免日志噪音：正常业务异常不会触发告警
- 快速定位问题：ERROR级别日志优先处理
- 便于监控：可以基于日志级别设置告警阈值"
```

### 3.3 场景三：业务服务日志

**代码位置**：[DeviceService.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/service/DeviceService.java)

```java
@Service
public class DeviceService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);

    /**
     * 获取设备列表
     */
    @Cacheable(value = "devices", unless = "#result == null || #result.isEmpty()")
    public List<Map<String, Object>> getDevices() {
        logger.debug("获取设备列表");
        List<Device> devices = deviceRepository.findAll();
        // ... 处理逻辑
        logger.info("获取设备列表成功，共{}个设备", result.size());
        return result;
    }

    /**
     * 更新设备状态
     */
    @CacheEvict(value = {"devices", "deviceStatus"}, allEntries = true)
    public void updateDeviceStatus(String deviceId, boolean online) {
        logger.debug("更新设备状态: {} -> {}", deviceId, online);
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device != null) {
            device.setOnline(online);
            device.setLastSeenTs(System.currentTimeMillis());
            deviceRepository.save(device);
            logger.info("设备状态已更新: {} -> {}", deviceId, online);
        } else {
            logger.warn("设备不存在，无法更新状态: {}", deviceId);
        }
    }

    /**
     * 处理设备心跳
     */
    @CacheEvict(value = {"devices", "deviceStatus"}, allEntries = true)
    public Device processHeartbeat(String deviceId) {
        logger.debug("处理设备心跳: {}", deviceId);
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            logger.warn("设备不存在，无法处理心跳: {}", deviceId);
            return null;
        }
        // ... 处理逻辑
        logger.info("设备心跳处理成功: {} -> 在线", deviceId);
        return savedDevice;
    }
}
```

**面试要点：业务日志的规范**

```
面试回答模板：

"我们在业务代码中遵循以下日志规范：

1. 日志级别使用：
   - DEBUG：方法进入/退出、中间变量值
   - INFO：关键业务操作成功
   - WARN：业务异常情况（如设备不存在）
   - ERROR：系统异常

2. 日志内容规范：
   - 使用占位符：logger.info("设备注册成功: {}", deviceId)
   - 包含关键信息：设备ID、操作结果
   - 使用中文：便于国内团队理解

3. 日志时机：
   - 方法入口：DEBUG级别记录参数
   - 关键操作：INFO级别记录结果
   - 异常情况：WARN级别记录原因

4. 性能考虑：
   - DEBUG日志在生产环境关闭
   - 使用占位符避免不必要的字符串拼接
   - 避免在循环中打印日志"
```

### 3.4 场景四：Kafka消费者日志

**代码位置**：[TelemetryConsumer.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/kafka/TelemetryConsumer.java)

```java
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class TelemetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryConsumer.class);

    @KafkaListener(topics = "dorm.telemetry", groupId = "dorm-power-telemetry")
    public void consumeTelemetryBatch(List<ConsumerRecord<String, String>> records, 
                                       Acknowledgment acknowledgment) {
        if (records.isEmpty()) {
            return;
        }

        logger.debug("Received {} telemetry records", records.size());

        try {
            // 处理逻辑...
            
            logger.info("Batch inserted {} telemetry records", telemetryList.size());
            
            // 手动提交偏移量
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process telemetry batch: {}", e.getMessage(), e);
            // 不提交偏移量，让Kafka重新投递
        }
    }
}
```

**面试要点：消息消费日志**

```
面试回答模板：

"Kafka消费者日志的关键点：

1. 批量处理日志：
   - DEBUG：记录每批消息数量
   - INFO：记录处理成功数量
   - ERROR：记录处理失败原因

2. 错误处理：
   - 捕获异常但不提交偏移量
   - 让Kafka重新投递消息
   - 记录完整堆栈便于排查

3. 性能考虑：
   - 不对每条消息单独打日志
   - 使用批量统计日志
   - 避免影响消息处理性能

4. 监控告警：
   - 可以基于ERROR日志设置告警
   - 监控消息处理延迟"
```

### 3.5 场景五：限流器日志

**代码位置**：[RedisRateLimiter.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/limiter/RedisRateLimiter.java)

```java
@Component
public class RedisRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(RedisRateLimiter.class);

    public boolean tryAcquire(String key, long permits, long maxPermits, long windowMs) {
        try {
            // ... Lua脚本执行
            
            boolean allowed = result != null && result == 1L;

            if (!allowed) {
                logger.debug("Rate limit exceeded for key: {}", key);
            }

            return allowed;
            
        } catch (Exception e) {
            logger.error("Rate limiter error for key: {}, error: {}", key, e.getMessage());
            // 限流器异常时，默认放行（降级策略）
            return true;
        }
    }
}
```

**面试要点：限流日志与降级**

```
面试回答模板：

"限流器日志的设计考量：

1. 日志级别：
   - DEBUG：限流触发（正常业务场景）
   - ERROR：限流器异常（需要关注）

2. 降级策略：
   - 限流器异常时默认放行
   - 记录ERROR日志便于排查
   - 避免限流器故障影响业务

3. 为什么限流触发用DEBUG？
   - 限流是正常的系统保护机制
   - 不应该触发告警
   - 但需要记录便于分析

4. 监控指标：
   - 可以基于日志统计限流次数
   - 结合Prometheus监控限流率"
```

---

## 4. 面试问答指南

### 4.1 项目日志架构相关

**Q1: 你们项目用的是什么日志框架？为什么选择它？**

```
标准答案：

"我们项目使用SLF4J作为日志门面，Logback作为主要实现，同时引入了Log4j2依赖。

选择SLF4J的原因：
1. 门面模式解耦：应用程序不依赖具体日志实现
2. 占位符特性：logger.info("用户{}登录", userId)，避免不必要的字符串拼接
3. 统一API：团队开发规范统一

选择Logback的原因：
1. Spring Boot默认集成，配置简单
2. 功能完善，稳定可靠
3. 性能满足我们的业务需求

同时引入Log4j2的原因：
1. 为未来可能的高性能需求做准备
2. Log4j2的Disruptor异步日志性能更好
3. 提供了更多的扩展可能性"
```

**Q2: 你们项目的日志配置是怎样的？**

```
标准答案：

"我们的日志配置分为三个层次：

1. Appender配置：
   - CONSOLE：控制台输出，开发调试用
   - FILE：应用日志文件，按天滚动，保留7天
   - AUDIT_FILE：审计日志文件，保留30天

2. Logger配置：
   - 业务代码(com.dormpower)：INFO级别
   - 框架代码(Spring/Hibernate)：WARN级别
   - 审计日志(AUDIT)：独立Logger，additivity=false

3. 滚动策略：
   - 按天滚动，自动清理过期日志
   - 总大小限制，避免磁盘占满

这样配置的好处：
- 业务日志和审计日志分离
- 日志文件大小可控
- 便于问题排查和审计追溯"
```

### 4.2 日志使用场景相关

**Q3: 你们项目中日志用在哪些场景？**

```
标准答案：

"我们项目中日志主要用在以下场景：

1. AOP切面日志：
   - 统一记录API请求日志
   - 自动记录请求耗时
   - 审计日志独立输出

2. 全局异常处理：
   - 统一捕获和处理异常
   - 根据异常类型选择日志级别
   - 记录完整堆栈便于排查

3. 业务操作日志：
   - 关键业务操作记录
   - 设备状态变更记录
   - 用户操作审计

4. 消息处理日志：
   - Kafka消息消费日志
   - 批量处理统计
   - 错误重试记录

5. 限流日志：
   - 限流触发记录
   - 限流器异常告警"
```

**Q4: 你们如何处理敏感信息的日志记录？**

```
标准答案：

"我们在日志记录敏感信息时，遵循以下原则：

1. 不直接输出敏感信息：
   - 密码：不记录明文
   - 手机号：脱敏处理 138****1234
   - 身份证：脱敏处理

2. 代码层面脱敏：
   private String maskPhone(String phone) {
       return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
   }

3. 日志格式规范：
   - 使用占位符，不拼接敏感信息
   - 记录操作结果，不记录敏感数据

4. 运维层面：
   - 日志文件权限控制
   - 日志传输加密
   - 定期审计日志内容"
```

### 4.3 日志原理相关

**Q5: SLF4J是如何绑定到具体实现的？**

```
标准答案：

"SLF4J使用SPI（Service Provider Interface）机制绑定具体实现：

1. 绑定原理：
   - SLF4J在类路径查找org/slf4j/impl/StaticLoggerBinder.class
   - 每个日志实现都提供这个类
   - Logback原生实现，Log4j2通过log4j-slf4j2-impl提供

2. 绑定过程：
   LoggerFactory.getLogger() 
   → 查找StaticLoggerBinder 
   → 获取ILoggerFactory实现 
   → 创建Logger实例

3. 为什么这样设计：
   - 解耦：应用程序只依赖SLF4J API
   - 灵活：运行时决定使用哪个实现
   - 可替换：无需修改代码即可切换实现"
```

**Q6: 异步日志是如何实现的？有什么优势？**

```
标准答案：

"异步日志的核心原理是使用队列解耦：

1. Logback AsyncAppender：
   - 使用BlockingQueue阻塞队列
   - 应用线程将日志事件放入队列
   - 后台线程从队列取出并写入目标

2. Log4j2 AsyncLogger：
   - 使用LMAX Disruptor无锁队列
   - RingBuffer环形数组，预分配内存
   - CAS操作代替锁，避免线程阻塞

优势：
1. 性能提升：日志IO不阻塞业务线程
2. 吞吐量高：批量写入减少IO次数
3. 延迟低：微秒级延迟

注意事项：
1. 队列满时的处理策略
2. 应用关闭时刷新队列
3. 极端情况可能丢失日志"
```

### 4.4 日志最佳实践相关

**Q7: 你们项目有哪些日志最佳实践？**

```
标准答案：

"我们项目遵循以下日志最佳实践：

1. 日志级别使用规范：
   - TRACE：最详细的调试信息
   - DEBUG：调试信息
   - INFO：关键业务流程
   - WARN：潜在问题
   - ERROR：需要处理的错误

2. 日志内容规范：
   - 使用占位符：logger.info("用户{}登录成功", userId)
   - 包含关键信息：便于问题定位
   - 避免敏感信息：脱敏处理

3. 日志格式规范：
   - 统一格式：时间、线程、级别、类名、消息
   - 结构化日志：JSON格式便于分析

4. 异常日志规范：
   - 记录完整堆栈：logger.error("操作失败", e)
   - 区分业务异常和系统异常

5. 性能考虑：
   - 生产环境关闭DEBUG日志
   - 使用异步日志
   - 避免在循环中打印日志"
```

**Q8: 如何设计一个分布式日志系统？**

```
标准答案：

"分布式日志系统的设计思路：

1. 日志采集层：
   - Filebeat/Fluentd：轻量级采集
   - 从应用服务器采集日志文件

2. 日志传输层：
   - Kafka：高吞吐消息队列
   - 缓冲和解耦

3. 日志处理层：
   - Logstash：日志解析和转换
   - Flink：实时日志分析

4. 日志存储层：
   - Elasticsearch：全文检索
   - HDFS：历史归档

5. 日志展示层：
   - Kibana：可视化分析
   - Grafana：监控告警

关键设计：
1. 统一日志格式：JSON结构化
2. 链路追踪：TraceId全链路传递
3. 日志聚合：按TraceId聚合展示
4. 告警机制：错误日志实时告警

我们项目中：
- 使用MDC存储TraceId
- 日志格式支持JSON输出
- 可以对接ELK进行日志分析"
```

---

## 5. 项目日志最佳实践

### 5.1 日志规范清单

| 规范项 | 要求 | 示例 |
|--------|------|------|
| 日志级别 | 根据场景选择正确级别 | INFO用于关键业务，ERROR用于系统异常 |
| 占位符 | 使用{}占位符，避免字符串拼接 | `logger.info("用户{}登录", userId)` |
| 异常日志 | 传入异常对象，打印完整堆栈 | `logger.error("操作失败", e)` |
| 敏感信息 | 脱敏处理或避免记录 | 手机号：138****1234 |
| 日志格式 | 统一格式，包含关键信息 | 时间、线程、级别、类名、消息 |
| 审计日志 | 独立文件，保留更长时间 | audit.log保留30天 |
| 性能考虑 | 生产环境关闭DEBUG，使用异步日志 | 避免影响业务性能 |

### 5.2 日志监控告警

```yaml
# Prometheus告警规则示例
groups:
  - name: log_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(log_errors_total[5m]) > 10
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "错误日志数量过高"
          description: "5分钟内错误日志数量超过10条/秒"
```

### 5.3 日志分析工具

```
推荐工具：
1. ELK Stack：Elasticsearch + Logstash + Kibana
2. Loki + Grafana：轻量级日志方案
3. 阿里云SLS：云原生日志服务

日志分析场景：
1. 错误日志追踪
2. 性能瓶颈分析
3. 用户行为分析
4. 安全审计分析
```

---

## 总结

本文档详细介绍了DormPower项目中日志框架的使用：

1. **架构选型**：SLF4J门面 + Logback/Log4j2实现
2. **配置详解**：logback-spring.xml配置文件解析
3. **使用场景**：AOP日志、异常日志、业务日志、消息日志、限流日志
4. **面试要点**：架构选择、配置设计、原理分析、最佳实践

掌握这些内容，能够在面试中清晰表达项目日志的设计思路和实现细节。
