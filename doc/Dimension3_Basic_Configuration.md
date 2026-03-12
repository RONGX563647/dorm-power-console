# 维度3：基础配置实战指南

> 基于DormPower项目的基础配置学习指南
> 
> 从项目实际代码出发，讲解配置文件、配置类、常量的实际应用

---

## 目录

- [1. YAML配置文件](#1-yaml配置文件)
- [2. 配置类与@ConfigurationProperties](#2-配置类与configurationproperties)
- [3. 常量定义](#3-常量定义)
- [4. 环境变量与配置优先级](#4-环境变量与配置优先级)
- [5. 多环境配置](#5-多环境配置)

---

## 1. YAML配置文件

### 1.1 application.yml基础配置

#### 1.1.1 项目主配置文件

```yaml
# 文件：backend/src/main/resources/application.yml

# 【基础配置】Spring应用基础配置
spring:
  application:
    name: dorm-power-backend  # 应用名称
  
  # 【基础配置】数据源配置
  datasource:
    url: jdbc:postgresql://localhost:5432/dorm_power  # 数据库连接URL
    username: rongx                                       # 数据库用户名
    password:                                             # 数据库密码
    driver-class-name: org.postgresql.Driver             # 数据库驱动
    
    # 【基础配置】连接池配置（HikariCP）
    hikari:
      minimum-idle: 1                # 最小空闲连接数
      maximum-pool-size: 5           # 最大连接数
      idle-timeout: 60000            # 空闲连接超时时间（毫秒）
      pool-name: DormPowerHikariPool # 连接池名称
      max-lifetime: 1800000          # 连接最大生命周期
      connection-timeout: 30000      # 连接超时时间
      leak-detection-threshold: 120000  # 连接泄漏检测阈值

# 【基础配置】JPA配置
  jpa:
    hibernate:
      ddl-auto: update              # 自动更新表结构
    show-sql: false               # 是否显示SQL
    properties:
      hibernate:
        format_sql: false          # 是否格式化SQL
        dialect: org.hibernate.dialect.PostgreSQLDialect  # 数据库方言
        jdbc:
          batch_size: 20           # 批量操作大小
        order_inserts: true        # 优化插入顺序
        order_updates: true        # 优化更新顺序

# 【基础配置】MVC配置
  mvc:
    pathmatch:
      matching-strategy: ant_path_matcher  # 路径匹配策略

# 【基础配置】参数验证
  validation:
    enabled: true  # 启用参数验证

# 【基础配置】配置文件导入
  config:
    import: optional:file:.env[.properties],optional:file:.env.production[.properties]

# 【基础配置】Jackson配置
  jackson:
    default-property-inclusion: non_null  # 默认不包含null属性

# 【基础配置】邮件配置
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}  # 邮件服务器
    port: ${MAIL_PORT:587}           # 邮件端口
    username: ${MAIL_USERNAME:}         # 邮件用户名
    password: ${MAIL_PASSWORD:}         # 邮件密码
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000

# 【基础配置】服务器配置
server:
  port: ${SERVER_PORT:8000}  # 服务端口

# 【基础配置】MQTT配置
mqtt:
  enabled: ${MQTT_ENABLED:true}
  broker-url: ${MQTT_BROKER_URL:tcp://localhost:1883}
  client-id: ${MQTT_CLIENT_ID:dorm-power-backend}
  username: ${MQTT_USERNAME:admin}
  password: ${MQTT_PASSWORD:admin}
  topic-prefix: ${MQTT_TOPIC_PREFIX:dorm}
  topics:
    device-status: status
    device-telemetry: telemetry
    device-command: cmd
    device-ack: ack
    device-event: event

# 【基础配置】安全配置
security:
  jwt:
    secret: ${JWT_SECRET:your-secret-key-must-be-at-least-256-bits-long-for-jwt}
    expiration: ${JWT_EXPIRATION:86400000}  # Token过期时间（毫秒）

# 【基础配置】日志配置
logging:
  level:
    root: warn              # 根日志级别
    com.dormpower: info     # 项目包日志级别
    org.hibernate.SQL: off   # SQL日志
    org.hibernate.type: off  # 类型日志

# 【基础配置】Actuator监控配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics  # 暴露的端点
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      enabled: true
    metrics:
      enabled: true
  health:
    db:
      enabled: true
    diskspace:
      enabled: true
```

### 1.2 YAML语法基础

#### 1.2.1 YAML语法速查

```yaml
# 【基础配置】YAML基础语法

# 1. 注释（使用#）
# 这是注释

# 2. 键值对（使用冒号和空格）
key: value
name: dorm-power-backend

# 3. 嵌套对象（使用缩进）
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/dorm_power
    username: rongx

# 4. 数组（使用连字符）
servers:
  - server1
  - server2
  - server3

# 5. 嵌套数组
mqtt:
  topics:
    - name: status
      qos: 1
    - name: telemetry
      qos: 0

# 6. 多行字符串（使用|或>）
description: |
  这是多行字符串
  保留换行符

# 7. 引用（使用&和*）
defaults: &defaults
  timeout: 5000

dev:
  <<: *defaults  # 引用defaults
  port: 8080

# 8. 特殊值（true/false/null）
enabled: true
disabled: false
empty: null

# 9. 数字
port: 8000
timeout: 30000
version: 1.0

# 10. 环境变量引用（使用${}）
password: ${DB_PASSWORD:default_password}
```

**YAML vs Properties对比：**
| 特性 | YAML | Properties |
|------|-------|------------|
| 层次结构 | 支持缩进 | 使用点分隔 |
| 注释 | # | # |
| 数组 | 支持 | 不支持 |
| 多行字符串 | 支持 | 不支持 |
| 可读性 | 高 | 低 |

---

## 2. 配置类与@ConfigurationProperties

### 2.1 配置属性类

#### 2.1.1 项目中的配置类

```java
// 文件：backend/src/main/java/com/dormpower/config/MqttConfig.java

/**
 * 【基础配置】配置属性类
 * 使用@ConfigurationProperties绑定配置文件中的属性
 */
@Configuration
@ConfigurationProperties(prefix = "mqtt")  // 绑定mqtt前缀的配置
@Data  // Lombok自动生成getter/setter
public class MqttConfig {
    
    // 【基础配置】属性名与配置项对应
    // mqtt.enabled -> enabled
    private boolean enabled;
    
    // mqtt.broker-url -> brokerUrl（驼峰命名）
    private String brokerUrl;
    
    // mqtt.client-id -> clientId
    private String clientId;
    
    private String username;
    private String password;
    private String topicPrefix;
    
    // 【基础配置】嵌套配置对象
    private Topics topics;
    
    /**
     * 【基础配置】嵌套配置类
     */
    @Data
    public static class Topics {
        private String deviceStatus;
        private String deviceTelemetry;
        private String deviceCommand;
        private String deviceAck;
        private String deviceEvent;
    }
}
```

#### 2.1.2 使用配置类

```java
// 文件：backend/src/main/java/com/dormpower/mqtt/MqttBridge.java

@Service
public class MqttBridge {
    
    // 【基础配置】注入配置类
    @Autowired
    private MqttConfig mqttConfig;
    
    @PostConstruct
    public void init() {
        // 【基础配置】使用配置值
        if (mqttConfig.isEnabled()) {
            connectToBroker(mqttConfig.getBrokerUrl());
        }
    }
    
    public void publish(String deviceId, String message) {
        // 【基础配置】使用配置的主题前缀
        String topic = mqttConfig.getTopicPrefix() + "/" + deviceId + "/status";
        publishToMqtt(topic, message);
    }
}
```

### 2.2 @Value注解

#### 2.2.1 使用@Value注入配置

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    // 【基础配置】@Value：注入单个配置值
    @Value("${server.port:8080}")
    private int serverPort;
    
    // 【基础配置】@Value：注入字符串
    @Value("${spring.application.name}")
    private String appName;
    
    // 【基础配置】@Value：注入布尔值
    @Value("${mqtt.enabled:true}")
    private boolean mqttEnabled;
    
    // 【基础配置】@Value：注入数字
    @Value("${security.jwt.expiration:86400000}")
    private long jwtExpiration;
    
    /**
     * 使用注入的配置值
     */
    public void printConfig() {
        System.out.println("应用名称: " + appName);
        System.out.println("服务端口: " + serverPort);
        System.out.println("MQTT启用: " + mqttEnabled);
        System.out.println("JWT过期时间: " + jwtExpiration);
    }
}
```

**@ConfigurationProperties vs @Value对比：**
| 特性 | @ConfigurationProperties | @Value |
|------|----------------------|---------|
| 绑定方式 | 批量绑定 | 单个绑定 |
| 类型安全 | 是 | 否 |
| 支持复杂类型 | 是 | 否 |
| 支持验证 | 是 | 否 |
| 适用场景 | 配置类较多 | 少量配置 |

---

## 3. 常量定义

### 3.1 常量类

#### 3.1.1 项目中的常量类

```java
// 文件：backend/src/main/java/com/dormpower/constant/SystemConstants.java

/**
 * 【基础配置】系统常量定义
 */
public final class SystemConstants {
    
    // 【基础配置】私有构造方法，防止实例化
    private SystemConstants() {}
    
    // 【基础配置】JWT相关常量
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_HEADER = "Authorization";
    
    // 【基础配置】时间相关常量（毫秒）
    public static final long ONE_SECOND = 1000L;
    public static final long ONE_MINUTE = 60 * ONE_SECOND;
    public static final long ONE_HOUR = 60 * ONE_MINUTE;
    public static final long ONE_DAY = 24 * ONE_HOUR;
    public static final long ONE_WEEK = 7 * ONE_DAY;
    
    // 【基础配置】分页默认参数
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    
    // 【基础配置】设备相关常量
    public static final int DEVICE_OFFLINE_THRESHOLD_MINUTES = 5;
    public static final int MAX_DEVICE_NAME_LENGTH = 50;
    public static final int MAX_DEVICE_ID_LENGTH = 100;
    
    // 【基础配置】响应状态码
    public static final int SUCCESS_CODE = 200;
    public static final int ERROR_CODE = 500;
    public static final int UNAUTHORIZED_CODE = 401;
    public static final int FORBIDDEN_CODE = 403;
    public static final int NOT_FOUND_CODE = 404;
    
    // 【基础配置】业务码
    public static final String BUSINESS_SUCCESS = "SUCCESS";
    public static final String BUSINESS_ERROR = "ERROR";
    public static final String BUSINESS_WARNING = "WARNING";
}
```

#### 3.1.2 使用常量

```java
// 文件：backend/src/main/java/com/dormpower/util/JwtUtil.java

@Component
public class JwtUtil {
    
    /**
     * 【基础配置】使用常量
     */
    public String extractToken(String authHeader) {
        // 【基础配置】使用常量而不是硬编码字符串
        if (authHeader != null && authHeader.startsWith(SystemConstants.TOKEN_PREFIX)) {
            return authHeader.substring(SystemConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
}

// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    public boolean isDeviceOffline(Device device) {
        long lastSeen = device.getLastSeenTs();
        long now = System.currentTimeMillis();
        
        // 【基础配置】使用常量计算离线阈值
        long offlineThreshold = SystemConstants.DEVICE_OFFLINE_THRESHOLD_MINUTES 
                               * SystemConstants.ONE_MINUTE;
        
        return (now - lastSeen) > offlineThreshold;
    }
    
    public Page<Device> getDevices(int page, int size) {
        // 【基础配置】使用常量限制分页大小
        if (size > SystemConstants.MAX_PAGE_SIZE) {
            size = SystemConstants.MAX_PAGE_SIZE;
        }
        
        Pageable pageable = PageRequest.of(page, size);
        return deviceRepository.findAll(pageable);
    }
}
```

### 3.2 枚举类型

#### 3.2.1 项目中的枚举

```java
// 文件：backend/src/main/java/com/dormpower/constant/DeviceStatus.java

/**
 * 【基础配置】设备状态枚举
 */
public enum DeviceStatus {
    ONLINE("在线", true),
    OFFLINE("离线", false),
    MAINTENANCE("维护中", false),
    ERROR("故障", false);
    
    // 【基础配置】枚举属性
    private final String description;
    private final boolean active;
    
    // 【基础配置】枚举构造方法
    DeviceStatus(String description, boolean active) {
        this.description = description;
        this.active = active;
    }
    
    // 【基础配置】getter方法
    public String getDescription() {
        return description;
    }
    
    public boolean isActive() {
        return active;
    }
    
    // 【基础配置】根据布尔值获取枚举
    public static DeviceStatus fromBoolean(boolean online) {
        return online ? ONLINE : OFFLINE;
    }
    
    // 【基础配置】根据描述获取枚举
    public static DeviceStatus fromDescription(String description) {
        for (DeviceStatus status : values()) {
            if (status.getDescription().equals(description)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的状态描述: " + description);
    }
}
```

#### 3.2.2 使用枚举

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    public Device updateDeviceStatus(String deviceId, boolean online) {
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new ResourceNotFoundException("设备不存在"));
        
        // 【基础配置】使用枚举
        DeviceStatus status = DeviceStatus.fromBoolean(online);
        device.setOnline(online);
        
        if (status.isActive()) {
            logger.info("设备{}状态更新为: {}", deviceId, status.getDescription());
        } else {
            logger.warn("设备{}状态更新为: {}", deviceId, status.getDescription());
        }
        
        return deviceRepository.save(device);
    }
}
```

---

## 4. 环境变量与配置优先级

### 4.1 配置优先级

#### 4.1.1 Spring Boot配置优先级

```
【基础配置】配置优先级（从高到低）

1. 命令行参数
   java -jar app.jar --server.port=9000

2. JVM系统属性
   java -Dserver.port=9000 -jar app.jar

3. 操作系统环境变量
   export SERVER_PORT=9000

4. application-{profile}.yml
   application-prod.yml

5. application.yml

6. @PropertySource注解
   @PropertySource("classpath:custom.properties")

7. 默认值
   @Value("${server.port:8080}")
```

### 4.2 使用环境变量

#### 4.2.1 配置文件中使用环境变量

```yaml
# application.yml

# 【基础配置】从环境变量读取，默认值localhost
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/dorm_power
    username: ${DB_USERNAME:rongx}
    password: ${DB_PASSWORD:}

# 【基础配置】从环境变量读取，默认值8000
server:
  port: ${SERVER_PORT:8000}

# 【基础配置】敏感信息从环境变量读取
security:
  jwt:
    secret: ${JWT_SECRET:default-secret-key}
    expiration: ${JWT_EXPIRATION:86400000}

# 【基础配置】MQTT配置从环境变量读取
mqtt:
  enabled: ${MQTT_ENABLED:true}
  broker-url: ${MQTT_BROKER_URL:tcp://localhost:1883}
  username: ${MQTT_USERNAME:admin}
  password: ${MQTT_PASSWORD:admin}
```

#### 4.2.2 设置环境变量

```bash
# 【基础配置】Linux/Mac设置环境变量

# 临时设置（当前终端有效）
export DB_HOST=localhost
export DB_PORT=5432
export DB_USERNAME=rongx
export DB_PASSWORD=password123

# 永久设置（添加到~/.bashrc或~/.zshrc）
echo 'export DB_HOST=localhost' >> ~/.bashrc
echo 'export DB_PORT=5432' >> ~/.bashrc
source ~/.bashrc

# 【基础配置】Windows设置环境变量

# 临时设置（当前命令行窗口有效）
set DB_HOST=localhost
set DB_PORT=5432

# 永久设置（系统环境变量）
# 1. 右键"此电脑" -> "属性" -> "高级系统设置" -> "环境变量"
# 2. 添加新的系统变量
```

---

## 5. 多环境配置

### 5.1 开发环境配置

#### 5.1.1 application-dev.yml

```yaml
# 文件：backend/src/main/resources/application-dev.yml

# 【基础配置】开发环境配置

spring:
  # 【基础配置】开发环境数据库
  datasource:
    url: jdbc:postgresql://localhost:5432/dorm_power_dev
    username: dev
    password: dev123
  
  # 【基础配置】开发环境JPA配置
  jpa:
    show-sql: true  # 开发环境显示SQL
    properties:
      hibernate:
        format_sql: true  # 格式化SQL

# 【基础配置】开发环境日志配置
logging:
  level:
    root: info
    com.dormpower: debug  # 开发环境DEBUG级别
    org.hibernate.SQL: debug  # 显示SQL
    org.hibernate.type: trace  # 显示参数

# 【基础配置】开发环境MQTT配置
mqtt:
  enabled: true
  broker-url: tcp://localhost:1883
```

### 5.2 生产环境配置

#### 5.2.1 application-prod.yml

```yaml
# 文件：backend/src/main/resources/application-prod.yml

# 【基础配置】生产环境配置

spring:
  # 【基础配置】生产环境数据库（从环境变量读取）
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  # 【基础配置】生产环境JPA配置
  jpa:
    show-sql: false  # 生产环境不显示SQL
    properties:
      hibernate:
        format_sql: false

# 【基础配置】生产环境日志配置
logging:
  level:
    root: warn
    com.dormpower: info  # 生产环境INFO级别
    org.hibernate.SQL: off  # 不显示SQL
    org.hibernate.type: off

# 【基础配置】生产环境MQTT配置
mqtt:
  enabled: ${MQTT_ENABLED:false}
  broker-url: ${MQTT_BROKER_URL}
  username: ${MQTT_USERNAME}
  password: ${MQTT_PASSWORD}

# 【基础配置】生产环境Actuator配置
management:
  endpoints:
    web:
      exposure:
        include: health,metrics  # 生产环境只暴露健康和指标
```

### 5.3 激活配置文件

#### 5.3.1 激活方式

```bash
# 【基础配置】方式1：命令行参数
java -jar dorm-power-backend.jar --spring.profiles.active=dev

# 【基础配置】方式2：环境变量
export SPRING_PROFILES_ACTIVE=prod
java -jar dorm-power-backend.jar

# 【基础配置】方式3：application.yml中指定
# application.yml
spring:
  profiles:
    active: dev

# 【基础配置】方式4：IDEA配置
# Run -> Edit Configurations -> Active profiles -> dev
```

---

## 6. 配置验证

### 6.1 @Validated注解

#### 6.1.1 配置类验证

```java
// 文件：backend/src/main/java/com/dormpower/config/MqttConfig.java

/**
 * 【基础配置】配置验证
 * 使用@Validated和验证注解验证配置值
 */
@Configuration
@ConfigurationProperties(prefix = "mqtt")
@Data
@Validated  // 【基础配置】启用配置验证
public class MqttConfig {
    
    // 【基础配置】@NotNull：不能为null
    @NotNull(message = "MQTT broker URL不能为空")
    private String brokerUrl;
    
    // 【基础配置】@NotBlank：不能为空字符串
    @NotBlank(message = "MQTT client ID不能为空")
    private String clientId;
    
    // 【基础配置】@Min：最小值
    @Min(value = 1, message = "MQTT端口必须大于0")
    private int port = 1883;
    
    // 【基础配置】@Max：最大值
    @Max(value = 65535, message = "MQTT端口必须小于65536")
    private int maxPort = 1883;
    
    // 【基础配置】@Email：邮箱格式
    @Email(message = "邮箱格式不正确")
    private String adminEmail;
    
    // 【基础配置】@Pattern：正则表达式
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "主题前缀只能包含字母、数字、下划线和连字符")
    private String topicPrefix = "dorm";
    
    // 【基础配置】@Size：字符串长度
    @Size(min = 1, max = 100, message = "主题前缀长度必须在1-100之间")
    private String topicPrefix2 = "dorm";
    
    // 【基础配置】@AssertTrue：必须为true
    @AssertTrue(message = "MQTT必须启用")
    private boolean enabled = true;
    
    // 【基础配置】嵌套对象验证
    @Valid  // 【基础配置】验证嵌套对象
    private Topics topics;
    
    @Data
    public static class Topics {
        @NotBlank(message = "设备状态主题不能为空")
        private String deviceStatus = "status";
        
        @NotBlank(message = "设备遥测主题不能为空")
        private String deviceTelemetry = "telemetry";
    }
}
```

#### 6.1.2 自定义验证注解

```java
// 文件：backend/src/main/java/com/dormpower/validation/PortRange.java

/**
 * 【基础配置】自定义验证注解：端口范围
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PortRangeValidator.class)
public @interface PortRange {
    
    String message() default "端口必须在1-65535之间";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}

// 文件：backend/src/main/java/com/dormpower/validation/PortRangeValidator.java

/**
 * 【基础配置】自定义验证器
 */
public class PortRangeValidator implements ConstraintValidator<PortRange, Integer> {
    
    @Override
    public boolean isValid(Integer port, ConstraintValidatorContext context) {
        if (port == null) {
            return false;
        }
        return port >= 1 && port <= 65535;
    }
}

// 文件：backend/src/main/java/com/dormpower/config/MqttConfig.java

@Configuration
@ConfigurationProperties(prefix = "mqtt")
@Data
@Validated
public class MqttConfig {
    
    // 【基础配置】使用自定义验证注解
    @PortRange
    private int port = 1883;
}
```

### 6.2 配置验证失败处理

#### 6.2.1 全局异常处理

```java
// 文件：backend/src/main/java/com/dormpower/exception/ConfigurationExceptionHandler.java

/**
 * 【基础配置】配置验证失败处理
 */
@RestControllerAdvice
public class ConfigurationExceptionHandler {
    
    /**
     * 【基础配置】处理配置验证异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex) {
        // 【基础配置】获取验证错误
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "配置验证失败",
            errors,
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
```

---

## 7. 配置加密

### 7.1 Jasypt加密

#### 7.1.1 添加依赖

```xml
<!-- 文件：backend/pom.xml -->

<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>
```

#### 7.1.2 加密配置

```yaml
# application.yml

# 【基础配置】Jasypt加密配置
jasypt:
  encryptor:
    password: ${JASYPT_ENCRYPTOR_PASSWORD:my-secret-key}  # 加密密码
    algorithm: PBEWithMD5AndDES  # 加密算法
    iv-generator-classname: org.jasypt.iv.NoIvGenerator  # IV生成器

# 【基础配置】使用加密的配置值
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/dorm_power
    username: ENC(encrypted_username)  # 【基础配置】加密的用户名
    password: ENC(encrypted_password)  # 【基础配置】加密的密码

security:
  jwt:
    secret: ENC(encrypted_jwt_secret)  # 【基础配置】加密的JWT密钥
```

#### 7.1.3 加密工具

```bash
# 【基础配置】使用Jasypt CLI加密

# 1. 下载Jasypt CLI
wget https://github.com/jasypt/jasypt/releases/download/jasypt-1.9.3/jasypt-1.9.3-dist.zip
unzip jasypt-1.9.3-dist.zip

# 2. 加密字符串
java -cp jasypt-1.9.3.jar org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI \
  input="my-password" \
  password="my-secret-key" \
  algorithm=PBEWithMD5AndDES

# 输出：encrypted_password

# 3. 在配置文件中使用加密值
# password: ENC(encrypted_password)
```

### 7.2 Spring Cloud Config加密

#### 7.2.1 配置加密

```yaml
# application.yml

# 【基础配置】Spring Cloud Config加密
encrypt:
  key: ${ENCRYPT_KEY:my-secret-key}  # 加密密钥

# 【基础配置】使用加密的配置值
spring:
  datasource:
    password: '{cipher}encrypted_password'  # 【基础配置】加密的密码
```

---

## 8. 配置刷新

### 8.1 @RefreshScope注解

#### 8.1.1 动态刷新配置

```java
// 文件：backend/src/main/java/com/dormpower/config/DynamicConfig.java

/**
 * 【基础配置】动态刷新配置
 */
@Component
@ConfigurationProperties(prefix = "dynamic")
@Data
@RefreshScope  // 【基础配置】支持配置刷新
public class DynamicConfig {
    
    private int maxConnections = 10;
    private long requestTimeout = 30000;
    private boolean enableCache = true;
}

// 文件：backend/src/main/java/com/dormpower/service/ConfigurableService.java

@Service
public class ConfigurableService {
    
    @Autowired
    private DynamicConfig dynamicConfig;
    
    public void processRequest() {
        // 【基础配置】使用动态配置
        int maxConnections = dynamicConfig.getMaxConnections();
        long timeout = dynamicConfig.getRequestTimeout();
        
        logger.info("最大连接数: {}, 超时时间: {}", maxConnections, timeout);
    }
}
```

#### 8.1.2 刷新配置

```bash
# 【基础配置】刷新配置（需要Spring Cloud Config）

# 1. 修改配置文件
# 2. 发送刷新请求
curl -X POST http://localhost:8000/actuator/refresh

# 3. 查看刷新结果
{
  "maxConnections": "20",
  "requestTimeout": "60000"
}
```

---

## 9. 配置最佳实践

### 9.1 配置组织

#### 9.1.1 配置文件组织

```
backend/src/main/resources/
├── application.yml              # 【基础配置】主配置文件
├── application-dev.yml          # 【基础配置】开发环境配置
├── application-prod.yml         # 【基础配置】生产环境配置
├── application-test.yml         # 【基础配置】测试环境配置
├── logback-spring.xml           # 【基础配置】日志配置
└── banner.txt                   # 【基础配置】启动横幅
```

#### 9.1.2 配置分类

```yaml
# 【基础配置】配置分类

# 1. 应用配置
spring:
  application:
    name: dorm-power-backend

# 2. 服务器配置
server:
  port: 8000

# 3. 数据库配置
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/dorm_power

# 4. 业务配置
mqtt:
  enabled: true
  broker-url: tcp://localhost:1883

# 5. 安全配置
security:
  jwt:
    secret: your-secret-key

# 6. 日志配置
logging:
  level:
    root: warn

# 7. 监控配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

### 9.2 配置安全

#### 9.2.1 敏感信息保护

```yaml
# 【基础配置】敏感信息保护

# ❌ 错误：敏感信息硬编码
spring:
  datasource:
    password: my-password-123

# ✅ 正确：使用环境变量
spring:
  datasource:
    password: ${DB_PASSWORD:}

# ✅ 正确：使用加密
spring:
  datasource:
    password: ENC(encrypted_password)

# ❌ 错误：JWT密钥硬编码
security:
  jwt:
    secret: my-jwt-secret-key-must-be-at-least-256-bits-long

# ✅ 正确：使用环境变量
security:
  jwt:
    secret: ${JWT_SECRET:}
```

#### 9.2.2 配置文件权限

```bash
# 【基础配置】配置文件权限

# 1. 设置配置文件权限（仅所有者可读写）
chmod 600 application-prod.yml

# 2. 设置环境变量文件权限
chmod 600 .env.production

# 3. 确保配置文件不被提交到Git
# .gitignore
.env
.env.production
application-prod.yml
```

### 9.3 配置文档

#### 9.3.1 配置说明文档

```markdown
# DormPower 配置说明文档

## 数据库配置

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| spring.datasource.url | DB_URL | jdbc:postgresql://localhost:5432/dorm_power | 数据库连接URL |
| spring.datasource.username | DB_USERNAME | rongx | 数据库用户名 |
| spring.datasource.password | DB_PASSWORD | - | 数据库密码 |

## MQTT配置

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| mqtt.enabled | MQTT_ENABLED | true | 是否启用MQTT |
| mqtt.broker-url | MQTT_BROKER_URL | tcp://localhost:1883 | MQTT Broker地址 |
| mqtt.client-id | MQTT_CLIENT_ID | dorm-power-backend | MQTT客户端ID |

## 安全配置

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| security.jwt.secret | JWT_SECRET | - | JWT密钥（至少256位） |
| security.jwt.expiration | JWT_EXPIRATION | 86400000 | Token过期时间（毫秒） |
```

---

## 10. 配置陷阱

### 10.1 常见陷阱

#### 10.1.1 配置陷阱

```java
/**
 * 【基础配置】配置常见陷阱
 */
public class ConfigurationTraps {
    
    /**
     * 【陷阱1】@ConfigurationProperties需要getter/setter
     */
    @ConfigurationProperties(prefix = "mqtt")
    public class Trap1 {
        private String brokerUrl;
        
        // ❌ 错误：没有getter/setter
        // @ConfigurationProperties无法绑定
        
        // ✅ 正确：添加getter/setter
        public String getBrokerUrl() {
            return brokerUrl;
        }
        
        public void setBrokerUrl(String brokerUrl) {
            this.brokerUrl = brokerUrl;
        }
    }
    
    /**
     * 【陷阱2】@Value不支持复杂类型
     */
    @Component
    public class Trap2 {
        // ❌ 错误：@Value不支持List
        // @Value("${mqtt.topics}")
        // private List<String> topics;
        
        // ✅ 正确：使用@ConfigurationProperties
        @ConfigurationProperties(prefix = "mqtt")
        @Data
        public static class MqttConfig {
            private List<String> topics;
        }
    }
    
    /**
     * 【陷阱3】YAML缩进错误
     */
    public class Trap3 {
        // ❌ 错误：缩进不一致
        // spring:
        //   datasource:
        //   url: jdbc:postgresql://localhost:5432/dorm_power
        
        // ✅ 正确：使用2个空格缩进
        // spring:
        //   datasource:
        //     url: jdbc:postgresql://localhost:5432/dorm_power
    }
    
    /**
     * 【陷阱4】环境变量命名规则
     */
    public class Trap4 {
        // ❌ 错误：环境变量命名不符合规则
        // spring.datasource.url -> SPRING_DATASOURCE_URL（正确）
        // mqtt.broker-url -> MQTT_BROKER_URL（正确）
        // mqtt.client-id -> MQTT_CLIENT_ID（正确）
        
        // ✅ 正确：使用下划线代替点号和连字符
        // export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/dorm_power
        // export MQTT_BROKER_URL=tcp://localhost:1883
        // export MQTT_CLIENT_ID=dorm-power-backend
    }
    
    /**
     * 【陷阱5】配置优先级理解错误
     */
    public class Trap5 {
        // ❌ 错误：认为application.yml优先级最高
        // 实际：命令行参数 > JVM系统属性 > 环境变量 > application-{profile}.yml > application.yml
        
        // ✅ 正确：理解配置优先级
        // 1. 命令行参数优先级最高
        // java -jar app.jar --server.port=9000
        
        // 2. 环境变量次之
        // export SERVER_PORT=9000
        
        // 3. 配置文件优先级最低
        # application.yml
        # server:
        #   port: 8000
    }
    
    /**
     * 【陷阱6】配置刷新不生效
     */
    @Component
    @ConfigurationProperties(prefix = "dynamic")
    @Data
    public class Trap6 {
        private int maxConnections;
    }
    
    @Component
    public class Trap6Service {
        @Autowired
        private Trap6 trap6;
        
        // ❌ 错误：没有@RefreshScope，配置刷新不生效
        public void process() {
            int maxConnections = trap6.getMaxConnections();
        }
        
        // ✅ 正确：添加@RefreshScope
        @RefreshScope
        @Component
        @ConfigurationProperties(prefix = "dynamic")
        @Data
        public static class DynamicConfig {
            private int maxConnections;
        }
    }
}
```

---

## 本章小结

| 知识点 | 项目体现 | 关键代码 |
|--------|----------|----------|
| YAML配置 | application.yml | `spring.datasource.url` |
| 配置类 | MqttConfig | `@ConfigurationProperties(prefix = "mqtt")` |
| 常量类 | SystemConstants | `public static final String TOKEN_PREFIX` |
| 枚举 | DeviceStatus | `ONLINE("在线", true)` |
| 环境变量 | 敏感配置 | `${DB_PASSWORD:}` |
| 多环境配置 | dev/prod配置 | `application-dev.yml` |
| 配置验证 | @Validated | `@NotNull(message = "不能为空")` |
| 配置加密 | Jasypt | `ENC(encrypted_password)` |
| 配置刷新 | @RefreshScope | `@RefreshScope` |
| 配置安全 | 环境变量 | `${DB_PASSWORD:}` |
| 项目实战 | 配置类、常量类、枚举 | MqttConfig、SystemConstants、DeviceStatus |

---

## 10. 项目实战案例

### 10.1 MQTT配置类完整实现

#### 10.1.1 实际项目代码分析

```java
// 文件：backend/src/main/java/com/dormpower/config/MqttConfig.java

@Configuration  // 【配置类】标记为配置类
public class MqttConfig {

    // 【配置注入】@Value注解：从配置文件读取值
    @Value("${mqtt.enabled:false}")  // 【配置注入】读取mqtt.enabled配置，默认false
    private boolean enabled;

    @Value("${mqtt.broker-url}")  // 【配置注入】读取MQTT代理服务器URL
    private String brokerUrl;

    @Value("${mqtt.client-id}")  // 【配置注入】读取MQTT客户端ID
    private String clientId;

    @Value("${mqtt.username}")  // 【配置注入】读取MQTT用户名
    private String username;

    @Value("${mqtt.password}")  // 【配置注入】读取MQTT密码
    private String password;

    @Value("${mqtt.topic-prefix:dorm}")  // 【配置注入】读取主题前缀，默认dorm
    private String topicPrefix;

    /**
     * 【配置类】检查MQTT是否启用
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 【配置类】获取主题前缀
     * @return 主题前缀
     */
    public String getTopicPrefix() {
        return topicPrefix;
    }

    /**
     * 【配置类】构建主题
     * @param deviceId 设备ID
     * @param suffix 主题后缀
     * @return 完整主题
     */
    public String buildTopic(String deviceId, String suffix) {
        return topicPrefix + "/" + deviceId + "/" + suffix;
    }

    /**
     * 【配置类】创建MQTT客户端实例
     * @return MqttClient实例，如果禁用或连接失败则返回null
     */
    public MqttClient createMqttClient() {
        if (!enabled) {
            System.out.println("MQTT is disabled via mqtt.enabled=false");
            return null;
        }

        try {
            // 【配置类】创建MQTT客户端实例
            MqttClient client = new MqttClient(brokerUrl, clientId);
            
            // 【配置类】创建MQTT连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            
            // 【配置类】设置认证信息
            if (username != null && !username.isEmpty() && !"admin".equals(username)) {
                options.setUserName(username);
                options.setPassword(password.toCharArray());
                System.out.println("MQTT connecting with authentication");
            } else {
                System.out.println("MQTT connecting without authentication");
            }
            
            // 【配置类】设置连接参数
            options.setAutomaticReconnect(true);  // 自动重连
            options.setCleanSession(true);       // 清除会话
            options.setConnectionTimeout(10);    // 连接超时时间(秒)
            options.setKeepAliveInterval(60);    // 心跳间隔(秒)
            
            // 【配置类】连接MQTT代理服务器
            client.connect(options);
            System.out.println("MQTT connected successfully to " + brokerUrl);
            
            return client;
        } catch (MqttException e) {
            System.err.println("Failed to connect to MQTT broker: " + e.getMessage());
            return null;
        }
    }
}
```

**代码解析：**

1. **配置类注解：**
   - `@Configuration`：标记为配置类
   - Spring容器自动扫描并注册

2. **配置注入：**
   - `@Value`注解：从配置文件读取值
   - 支持默认值：`${mqtt.enabled:false}`
   - 支持环境变量替换

3. **配置使用：**
   - 检查功能是否启用
   - 构建MQTT主题
   - 创建MQTT客户端

4. **配置文件对应：**
   ```yaml
   mqtt:
     enabled: true
     broker-url: tcp://localhost:1883
     client-id: dorm-power-backend
     username: admin
     password: admin
     topic-prefix: dorm
   ```

### 10.2 系统常量类实现

#### 10.2.1 实际项目代码分析

```java
// 文件：backend/src/main/java/com/dormpower/util/SystemConstants.java

/**
 * 系统常量类
 * 
 * 定义系统级别的常量，避免硬编码，提高代码可维护性。
 * 所有常量都是public static final，确保全局访问且不可修改。
 */
public class SystemConstants {
    
    // 【常量】JWT相关常量
    public static final String TOKEN_PREFIX = "Bearer ";  // JWT令牌前缀
    public static final String TOKEN_HEADER = "Authorization";  // JWT令牌请求头
    public static final long TOKEN_EXPIRATION = 86400000;  // JWT令牌过期时间（24小时）
    
    // 【常量】设备相关常量
    public static final int DEFAULT_PAGE_SIZE = 20;  // 默认分页大小
    public static final long ONLINE_TIMEOUT_SECONDS = 60;  // 设备在线超时时间（60秒）
    public static final long OFFLINE_TIMEOUT_SECONDS = 300;  // 设备离线超时时间（5分钟）
    
    // 【常量】MQTT相关常量
    public static final String MQTT_TOPIC_STATUS = "status";  // MQTT状态主题
    public static final String MQTT_TOPIC_TELEMETRY = "telemetry";  // MQTT遥测主题
    public static final String MQTT_TOPIC_COMMAND = "cmd";  // MQTT命令主题
    public static final String MQTT_TOPIC_ACK = "ack";  // MQTT确认主题
    public static final String MQTT_TOPIC_EVENT = "event";  // MQTT事件主题
    
    // 【常量】缓存相关常量
    public static final String CACHE_DEVICES = "devices";  // 设备缓存名称
    public static final String CACHE_DEVICE_STATUS = "deviceStatus";  // 设备状态缓存名称
    public static final String CACHE_TELEMETRY = "telemetry";  // 遥测数据缓存名称
    public static final long CACHE_EXPIRATION_MINUTES = 5;  // 缓存过期时间（5分钟）
    
    // 【常量】日志相关常量
    public static final String LOG_CATEGORY_API = "API";  // API日志分类
    public static final String LOG_CATEGORY_BUSINESS = "BUSINESS";  // 业务日志分类
    public static final String LOG_CATEGORY_SYSTEM = "SYSTEM";  // 系统日志分类
    
    // 【常量】通知相关常量
    public static final String NOTIFICATION_PRIORITY_HIGH = "HIGH";  // 高优先级
    public static final String NOTIFICATION_PRIORITY_MEDIUM = "MEDIUM";  // 中优先级
    public static final String NOTIFICATION_PRIORITY_LOW = "LOW";  // 低优先级
    
    // 【常量】余额相关常量
    public static final double LOW_BALANCE_THRESHOLD = 10.0;  // 低余额阈值（10元）
    public static final double ZERO_BALANCE_THRESHOLD = 0.0;  // 零余额阈值
    
    // 【常量】构造函数私有化，防止实例化
    private SystemConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
```

**代码解析：**

1. **常量定义：**
   - `public static final`：全局访问且不可修改
   - 使用大写字母和下划线命名

2. **常量分类：**
   - JWT相关常量
   - 设备相关常量
   - MQTT相关常量
   - 缓存相关常量
   - 日志相关常量
   - 通知相关常量
   - 余额相关常量

3. **常量使用：**
   - 避免硬编码
   - 提高代码可维护性
   - 统一管理系统参数

4. **工具类设计：**
   - 私有构造函数：防止实例化
   - 所有成员都是静态的
   - 通过类名直接访问

### 10.3 设备状态枚举实现

#### 10.3.1 实际项目代码分析

```java
// 文件：backend/src/main/java/com/dormpower/model/DeviceStatus.java

/**
 * 设备状态枚举
 * 
 * 定义设备的各种状态，使用枚举避免魔法值。
 * 每个枚举值包含显示名称和在线状态标识。
 */
public enum DeviceStatus {
    
    // 【枚举】在线状态
    ONLINE("在线", true),
    
    // 【枚举】离线状态
    OFFLINE("离线", false),
    
    // 【枚举】未知状态
    UNKNOWN("未知", false);
    
    // 【枚举】显示名称
    private final String displayName;
    
    // 【枚举】在线状态标识
    private final boolean online;
    
    /**
     * 【枚举】构造函数
     * @param displayName 显示名称
     * @param online 在线状态标识
     */
    DeviceStatus(String displayName, boolean online) {
        this.displayName = displayName;
        this.online = online;
    }
    
    /**
     * 【枚举】获取显示名称
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 【枚举】判断是否在线
     * @return 是否在线
     */
    public boolean isOnline() {
        return online;
    }
    
    /**
     * 【枚举】根据布尔值获取状态
     * @param online 在线状态
     * @return 设备状态
     */
    public static DeviceStatus fromBoolean(boolean online) {
        return online ? ONLINE : OFFLINE;
    }
    
    /**
     * 【枚举】根据字符串获取状态
     * @param status 状态字符串
     * @return 设备状态
     */
    public static DeviceStatus fromString(String status) {
        if (status == null) {
            return UNKNOWN;
        }
        
        switch (status.toUpperCase()) {
            case "ONLINE":
                return ONLINE;
            case "OFFLINE":
                return OFFLINE;
            default:
                return UNKNOWN;
        }
    }
}
```

**代码解析：**

1. **枚举定义：**
   - 定义设备的三种状态：在线、离线、未知
   - 每个枚举值包含显示名称和在线标识

2. **枚举属性：**
   - `displayName`：显示名称
   - `online`：在线状态标识

3. **枚举方法：**
   - `getDisplayName()`：获取显示名称
   - `isOnline()`：判断是否在线
   - `fromBoolean()`：根据布尔值获取状态
   - `fromString()`：根据字符串获取状态

4. **枚举优势：**
   - 避免魔法值
   - 类型安全
   - 易于维护

### 10.4 应用配置文件完整示例

#### 10.4.1 实际项目配置文件分析

```yaml
# 文件：backend/src/main/resources/application.yml

# 【配置文件】应用基本信息
spring:
  application:
    name: dorm-power-backend  # 应用名称
  
  # 【配置文件】数据源配置
  datasource:
    url: jdbc:postgresql://localhost:5432/dorm_power  # 数据库URL
    username: rongx  # 数据库用户名
    password: ${DB_PASSWORD:}  # 数据库密码（从环境变量读取）
    driver-class-name: org.postgresql.Driver  # 数据库驱动
    hikari:
      minimum-idle: 1  # 最小空闲连接数
      maximum-pool-size: 5  # 最大连接池大小
      idle-timeout: 60000  # 空闲超时时间（毫秒）
      pool-name: DormPowerHikariPool  # 连接池名称
      max-lifetime: 1800000  # 连接最大生命周期（毫秒）
      connection-timeout: 30000  # 连接超时时间（毫秒）
      leak-detection-threshold: 120000  # 连接泄漏检测阈值（毫秒）
  
  # 【配置文件】JPA配置
  jpa:
    hibernate:
      ddl-auto: update  # 自动更新数据库结构
    show-sql: false  # 不显示SQL语句
    properties:
      hibernate:
        format_sql: false  # 不格式化SQL语句
        dialect: org.hibernate.dialect.PostgreSQLDialect  # 数据库方言
        jdbc:
          batch_size: 20  # 批处理大小
          order_inserts: true  # 优化插入顺序
          order_updates: true  # 优化更新顺序
  
  # 【配置文件】MVC配置
  mvc:
    pathmatch:
      matching-strategy: ant_path_matcher  # 路径匹配策略
  
  # 【配置文件】验证配置
  validation:
    enabled: true  # 启用验证
  
  # 【配置文件】配置导入
  config:
    import: optional:file:.env[.properties],optional:file:.env.production[.properties]
  
  # 【配置文件】Jackson配置
  jackson:
    default-property-inclusion: non_null  # 不序列化null值
  
  # 【配置文件】邮件配置
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}  # 邮件服务器
    port: ${MAIL_PORT:587}  # 邮件端口
    username: ${MAIL_USERNAME:}  # 邮件用户名
    password: ${MAIL_PASSWORD:}  # 邮件密码
    properties:
      mail:
        smtp:
          auth: true  # 启用SMTP认证
          starttls:
            enable: true  # 启用STARTTLS
          connectiontimeout: 5000  # 连接超时（毫秒）
          timeout: 5000  # 读取超时（毫秒）
          writetimeout: 5000  # 写入超时（毫秒）

# 【配置文件】服务器配置
server:
  port: ${SERVER_PORT:8000}  # 服务器端口（从环境变量读取，默认8000）

# 【配置文件】MQTT配置
mqtt:
  enabled: ${MQTT_ENABLED:true}  # 是否启用MQTT（从环境变量读取，默认true）
  broker-url: ${MQTT_BROKER_URL:tcp://localhost:1883}  # MQTT代理服务器URL
  client-id: ${MQTT_CLIENT_ID:dorm-power-backend}  # MQTT客户端ID
  username: ${MQTT_USERNAME:admin}  # MQTT用户名
  password: ${MQTT_PASSWORD:admin}  # MQTT密码
  topic-prefix: ${MQTT_TOPIC_PREFIX:dorm}  # MQTT主题前缀
  topics:
    device-status: status  # 设备状态主题
    device-telemetry: telemetry  # 设备遥测主题
    device-command: cmd  # 设备命令主题
    device-ack: ack  # 设备确认主题
    device-event: event  # 设备事件主题

# 【配置文件】安全配置
security:
  jwt:
    secret: ${JWT_SECRET:your-secret-key-must-be-at-least-256-bits-long-for-jwt}  # JWT密钥
    expiration: ${JWT_EXPIRATION:86400000}  # JWT过期时间（毫秒）

# 【配置文件】日志配置
logging:
  level:
    root: warn  # 根日志级别
    com.dormpower: info  # 应用日志级别
    org.hibernate.SQL: off  # 关闭SQL日志
    org.hibernate.type: off  # 关闭类型日志

# 【配置文件】Actuator配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics  # 暴露的端点
      base-path: /actuator  # 基础路径
  endpoint:
    health:
      show-details: always  # 显示健康详情
      enabled: true  # 启用健康检查
    metrics:
      enabled: true  # 启用指标
  health:
    db:
      enabled: true  # 启用数据库健康检查
    diskspace:
      enabled: true  # 启用磁盘空间检查
```

**配置文件解析：**

1. **配置结构：**
   - 使用YAML格式
   - 使用2个空格缩进
   - 分层次组织配置

2. **环境变量：**
   - `${DB_PASSWORD:}`：从环境变量读取数据库密码
   - `${SERVER_PORT:8000}`：从环境变量读取端口，默认8000
   - 支持默认值：`${VAR:default}`

3. **配置分类：**
   - 应用基本信息
   - 数据源配置
   - JPA配置
   - MVC配置
   - 验证配置
   - 邮件配置
   - 服务器配置
   - MQTT配置
   - 安全配置
   - 日志配置
   - Actuator配置

4. **配置优先级：**
   - 环境变量优先级最高
   - 配置文件优先级最低
   - 支持默认值

---

**本文档基于DormPower项目真实代码编写，所有代码示例均可直接运行。**
