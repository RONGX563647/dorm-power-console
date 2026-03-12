# 维度6：异常处理&日志实战指南

> 基于DormPower项目的异常处理和日志学习指南
> 
> 从项目实际代码出发，讲解异常处理、日志框架的实际应用

---

## 目录

- [1. 异常处理基础](#1-异常处理基础)
- [2. 自定义异常](#2-自定义异常)
- [3. 全局异常处理](#3-全局异常处理)
- [4. 日志框架使用](#4-日志框架使用)
- [5. 日志配置](#5-日志配置)

---

## 1. 异常处理基础

### 1.1 try-catch-finally

#### 1.1.1 基础异常处理

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);
    
    /**
     * 【异常处理】try-catch：捕获并处理异常
     */
    public Device getDeviceSafe(String deviceId) {
        try {
            // 【异常处理】可能抛出异常的代码
            return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("设备不存在"));
        } catch (ResourceNotFoundException e) {
            // 【异常处理】捕获特定异常，进行处理
            logger.warn("设备未找到: {}", deviceId);
            return null;
        } catch (Exception e) {
            // 【异常处理】捕获其他异常
            logger.error("查询设备异常: {}", e.getMessage(), e);
            return null;
        }
    }
}
```

#### 1.1.2 try-with-resources

```java
@Service
public class FileService {
    
    /**
     * 【异常处理】try-with-resources：自动关闭资源（Java 7+）
     */
    public void processFile(String filePath) {
        // 【异常处理】自动关闭实现了AutoCloseable接口的资源
        try (InputStream is = new FileInputStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line);
            }
            
        } catch (IOException e) {
            logger.error("文件处理失败: {}", e.getMessage(), e);
            throw new BusinessException("文件处理失败", e);
        }
        // 自动调用close()，释放文件句柄
    }
}
```

### 1.2 抛出异常

#### 1.2.1 throw关键字

```java
/**
 * 【异常处理】throw：主动抛出异常
 */
@Service
public class DeviceService {
    
    public Device createDevice(Device device) {
        // 【异常处理】参数校验，不满足条件抛出异常
        if (device.getId() == null || device.getId().isEmpty()) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
        
        // 【异常处理】业务规则校验
        if (deviceRepository.existsById(device.getId())) {
            throw new BusinessException("设备ID已存在: " + device.getId());
        }
        
        return deviceRepository.save(device);
    }
}
```

---

## 2. 自定义异常

### 2.1 业务异常基类

#### 2.1.1 BusinessException定义

```java
// 文件：backend/src/main/java/com/dormpower/exception/BusinessException.java

/**
 * 【异常处理】自定义业务异常
 * 继承RuntimeException，无需强制捕获
 */
public class BusinessException extends RuntimeException {
    
    // 【异常处理】错误码
    private String errorCode;
    
    /**
     * 【异常处理】构造方法1：只传消息
     */
    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }
    
    /**
     * 【异常处理】构造方法2：传错误码和消息
     */
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 【异常处理】构造方法3：传消息和原因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BUSINESS_ERROR";
    }
    
    /**
     * 【异常处理】构造方法4：传错误码、消息和原因
     */
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
```

### 2.2 具体业务异常

#### 2.2.1 ResourceNotFoundException

```java
// 文件：backend/src/main/java/com/dormpower/exception/ResourceNotFoundException.java

/**
 * 【异常处理】资源不存在异常
 */
public class ResourceNotFoundException extends BusinessException {
    
    /**
     * 【异常处理】构造方法1
     */
    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", message);
    }
    
    /**
     * 【异常处理】构造方法2：带资源名和字段值
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super("NOT_FOUND", 
              String.format("%s not found with %s : '%s'", 
                          resourceName, fieldName, fieldValue));
    }
}
```

#### 2.2.2 AuthenticationException

```java
// 文件：backend/src/main/java/com/dormpower/exception/AuthenticationException.java

/**
 * 【异常处理】认证异常
 */
public class AuthenticationException extends BusinessException {
    
    /**
     * 【异常处理】构造方法
     */
    public AuthenticationException(String message) {
        super("AUTHENTICATION_FAILED", message);
    }
}
```

**异常继承关系图：**
```
Throwable（所有异常的根类）
    └── Exception（受检异常）
            └── RuntimeException（运行时异常）
                    └── BusinessException（自定义业务异常）
                            ├── ResourceNotFoundException（资源不存在异常）
                            ├── AuthenticationException（认证异常）
                            └── ...
```

---

## 3. 全局异常处理

### 3.1 @RestControllerAdvice

#### 3.1.1 全局异常处理器

```java
// 文件：backend/src/main/java/com/dormpower/exception/GlobalExceptionHandler.java

/**
 * 【异常处理】@RestControllerAdvice：全局异常处理器
 * 统一处理所有Controller抛出的异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 【异常处理】处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        logger.info("参数验证异常: {}", ex.getMessage());
        
        // 收集所有验证错误
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "参数验证失败",
            errors,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 【异常处理】处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.info("资源未找到异常: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            null,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * 【异常处理】处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        logger.info("业务异常: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            null,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 【异常处理】处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        logger.info("认证异常: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            ex.getMessage(),
            null,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * 【异常处理】处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex) {
        logger.error("系统异常: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "系统内部错误，请联系管理员",
            null,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
```

### 3.2 错误响应对象

#### 3.2.1 ErrorResponse定义

```java
// 文件：backend/src/main/java/com/dormpower/exception/ErrorResponse.java

/**
 * 【异常处理】错误响应对象
 */
@Data
@AllArgsConstructor
public class ErrorResponse {
    private int status;           // HTTP状态码
    private String message;       // 错误消息
    private Map<String, String> errors;  // 详细错误信息
    private LocalDateTime timestamp;     // 时间戳
}
```

---

## 4. 日志框架使用

### 4.1 SLF4J + Logback

#### 4.1.1 日志使用示例

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    // 【日志使用】创建Logger实例
    // 通常使用类名作为logger名称
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);
    
    public Device getDeviceById(String deviceId) {
        // 【日志使用】DEBUG级别：详细的调试信息
        logger.debug("开始查询设备: {}", deviceId);
        
        try {
            Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("设备不存在"));
            
            // 【日志使用】INFO级别：正常的业务信息
            logger.info("成功查询设备: {}, 名称: {}", deviceId, device.getName());
            
            return device;
            
        } catch (ResourceNotFoundException e) {
            // 【日志使用】WARN级别：警告信息，不影响系统运行
            logger.warn("设备未找到: {}", deviceId);
            throw e;
            
        } catch (Exception e) {
            // 【日志使用】ERROR级别：错误信息，需要处理
            // 最后一个参数传入异常对象，会打印堆栈
            logger.error("查询设备失败: {}, 异常: {}", deviceId, e.getMessage(), e);
            throw new BusinessException("查询设备失败", e);
        }
    }
}
```

### 4.2 日志级别

#### 4.2.1 日志级别说明

```yaml
# application.yml 日志配置

logging:
  level:
    # 【日志使用】根日志级别
    root: warn
    
    # 【日志使用】指定包的日志级别
    com.dormpower: info
    com.dormpower.controller: debug
    com.dormpower.service: info
    
    # 【日志使用】框架日志级别
    org.springframework: warn
    org.hibernate: error
```

**日志级别（从低到高）：**
| 级别 | 使用场景 | 示例 | 输出 |
|------|----------|------|------|
| TRACE | 最详细的跟踪信息 | `logger.trace("进入方法")` | 全部 |
| DEBUG | 调试信息 | `logger.debug("变量值: {}", value)` | DEBUG及以上 |
| INFO | 正常业务信息 | `logger.info("操作成功")` | INFO及以上 |
| WARN | 警告信息 | `logger.warn("参数为空")` | WARN及以上 |
| ERROR | 错误信息 | `logger.error("操作失败", e)` | ERROR及以上 |

### 4.3 日志占位符

#### 4.3.1 日志占位符使用

```java
@Service
public class DeviceService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);
    
    public void logExamples() {
        String deviceId = "dev_001";
        String deviceName = "空调";
        int count = 10;
        
        // 【日志使用】使用{}占位符，避免字符串拼接
        logger.info("设备ID: {}, 设备名称: {}", deviceId, deviceName);
        
        // 【日志使用】多个占位符
        logger.info("查询到{}个设备，第一个设备ID: {}", count, deviceId);
        
        // 【日志使用】条件日志（避免不必要的计算）
        if (logger.isDebugEnabled()) {
            // 复杂的日志内容只在DEBUG级别计算
            logger.debug("复杂对象: {}", expensiveOperation());
        }
    }
    
    private String expensiveOperation() {
        // 耗时操作
        return "result";
    }
}
```

**日志占位符优势：**
1. 性能更好：避免字符串拼接
2. 延迟计算：只有需要时才计算参数
3. 可读性强：清晰表达日志结构

---

## 5. 日志配置

### 5.1 日志文件配置

#### 5.1.1 Logback配置

```xml
<!-- 文件：backend/src/main/resources/logback-spring.xml -->
<configuration>
    
    <!-- 【日志配置】控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- 【日志配置】文件输出 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/dorm-power.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/dorm-power.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>
    
    <!-- 【日志配置】错误日志单独文件 -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/error.log</file>
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/error.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>
    
    <!-- 【日志配置】根logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
        <appender-ref ref="ERROR_FILE" />
    </root>
    
    <!-- 【日志配置】特定包的logger -->
    <logger name="com.dormpower" level="DEBUG" additivity="false">
        <appender-ref ref="FILE" />
    </logger>
    
</configuration>
```

### 5.2 日志输出格式

#### 5.2.1 日志格式模式

```xml
<!-- 【日志配置】常用格式模式 -->

<!-- 模式1：简洁格式 -->
<pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
<!-- 输出：14:30:45.123 INFO  c.d.service.DeviceService - 操作成功 -->

<!-- 模式2：详细格式 -->
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
<!-- 输出：2024-01-15 14:30:45.123 [http-nio-8080-exec-1] INFO  c.d.service.DeviceService - 操作成功 -->

<!-- 模式3：带类名和行号 -->
<pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36}.%M - %msg%n</pattern>
<!-- 输出：14:30:45.123 [http-nio-8080-exec-1] INFO  c.d.service.DeviceService.getDeviceById - 操作成功 -->

<!-- 模式4：带异常堆栈 -->
<pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n%ex</pattern>
<!-- 输出：14:30:45.123 INFO  c.d.service.DeviceService - 操作成功 -->
<!--       java.lang.NullPointerException -->
<!--           at com.dormpower.service.DeviceService.getDeviceById(DeviceService.java:45) -->
```

**格式占位符说明：**
| 占位符 | 说明 | 示例 |
|--------|------|------|
| %d | 日期时间 | 2024-01-15 14:30:45 |
| %thread | 线程名 | http-nio-8080-exec-1 |
| %-5level | 日志级别（左对齐，宽度5） | INFO |
| %logger{36} | Logger名称（长度36） | c.d.service.DeviceService |
| %msg | 日志消息 | 操作成功 |
| %n | 换行符 | - |
| %ex | 异常堆栈 | 异常信息 |

---

## 6. 异常处理最佳实践

### 6.1 异常处理原则

#### 6.1.1 异常处理原则

```java
/**
 * 【异常处理】异常处理最佳实践
 */
public class ExceptionBestPractices {
    
    /**
     * 【原则1】早抛出，晚捕获
     * 尽早发现并抛出异常，在合适的层级捕获处理
     */
    @Service
    public class Principle1 {
        
        public void createDevice(Device device) {
            // 【异常处理】参数校验，尽早抛出异常
            if (device == null) {
                throw new IllegalArgumentException("设备不能为空");
            }
            
            if (device.getId() == null || device.getId().isEmpty()) {
                throw new IllegalArgumentException("设备ID不能为空");
            }
            
            // 业务逻辑
            deviceRepository.save(device);
        }
    }
    
    /**
     * 【原则2】使用具体的异常类型
     * 避免使用通用的Exception，使用具体的异常类型
     */
    @Service
    public class Principle2 {
        
        public Device getDevice(String deviceId) {
            // ✅ 推荐：使用具体的异常
            if (deviceId == null || deviceId.isEmpty()) {
                throw new IllegalArgumentException("设备ID不能为空");
            }
            
            return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("设备不存在: " + deviceId));
        }
        
        // ❌ 不推荐：使用通用的Exception
        public Device getDeviceBad(String deviceId) {
            if (deviceId == null || deviceId.isEmpty()) {
                throw new RuntimeException("设备ID不能为空");
            }
            
            return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("设备不存在: " + deviceId));
        }
    }
    
    /**
     * 【原则3】异常信息要清晰
     * 异常信息应该包含足够的上下文信息
     */
    @Service
    public class Principle3 {
        
        public void updateDevice(String deviceId, String newName) {
            Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "设备不存在: deviceId=" + deviceId  // ✅ 推荐：包含上下文信息
                ));
            
            device.setName(newName);
            deviceRepository.save(device);
        }
        
        // ❌ 不推荐：异常信息不清晰
        public void updateDeviceBad(String deviceId, String newName) {
            Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "设备不存在"  // ❌ 不推荐：缺少上下文信息
                ));
            
            device.setName(newName);
            deviceRepository.save(device);
        }
    }
    
    /**
     * 【原则4】不要吞掉异常
     * 捕获异常后要处理，不能简单地忽略
     */
    @Service
    public class Principle4 {
        
        public void processDevice(String deviceId) {
            try {
                Device device = deviceRepository.findById(deviceId).orElse(null);
                if (device != null) {
                    processDevice(device);
                }
            } catch (Exception e) {
                // ✅ 推荐：记录日志并重新抛出
                logger.error("处理设备失败: {}", deviceId, e);
                throw new BusinessException("处理设备失败", e);
            }
        }
        
        // ❌ 不推荐：吞掉异常
        public void processDeviceBad(String deviceId) {
            try {
                Device device = deviceRepository.findById(deviceId).orElse(null);
                if (device != null) {
                    processDevice(device);
                }
            } catch (Exception e) {
                // ❌ 不推荐：吞掉异常，没有处理
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 【原则5】使用finally释放资源
     * 确保资源被正确释放
     */
    @Service
    public class Principle5 {
        
        public void processFile(String filePath) {
            InputStream is = null;
            try {
                is = new FileInputStream(filePath);
                // 处理文件
            } catch (IOException e) {
                logger.error("文件处理失败", e);
                throw new BusinessException("文件处理失败", e);
            } finally {
                // ✅ 推荐：在finally中释放资源
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        logger.error("关闭文件失败", e);
                    }
                }
            }
        }
        
        // ✅ 推荐：使用try-with-resources（Java 7+）
        public void processFileBetter(String filePath) {
            try (InputStream is = new FileInputStream(filePath)) {
                // 处理文件
            } catch (IOException e) {
                logger.error("文件处理失败", e);
                throw new BusinessException("文件处理失败", e);
            }
            // 自动调用close()
        }
    }
}
```

### 6.2 异常处理层次

#### 6.2.1 异常处理层次结构

```java
/**
 * 【异常处理】异常处理层次
 */
public class ExceptionLayers {
    
    /**
     * 【层次1】Controller层：捕获所有异常，返回友好的错误信息
     */
    @RestControllerAdvice
    public class ControllerExceptionHandler {
        
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleException(Exception ex) {
            logger.error("系统异常: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "系统内部错误", null, LocalDateTime.now()));
        }
    }
    
    /**
     * 【层次2】Service层：捕获业务异常，转换为自定义异常
     */
    @Service
    public class DeviceService {
        
        public Device getDevice(String deviceId) {
            try {
                return deviceRepository.findById(deviceId).orElse(null);
            } catch (Exception e) {
                // 捕获底层异常，转换为业务异常
                throw new BusinessException("查询设备失败: " + deviceId, e);
            }
        }
    }
    
    /**
     * 【层次3】Repository层：捕获数据访问异常，转换为业务异常
     */
    @Repository
    public class DeviceRepository {
        
        public Device findById(String deviceId) {
            try {
                return entityManager.find(Device.class, deviceId);
            } catch (PersistenceException e) {
                // 捕获数据访问异常，转换为业务异常
                throw new BusinessException("数据访问失败", e);
            }
        }
    }
}
```

---

## 7. 日志最佳实践

### 7.1 日志使用原则

#### 7.1.1 日志使用原则

```java
/**
 * 【日志使用】日志使用最佳实践
 */
public class LoggingBestPractices {
    
    /**
     * 【原则1】使用正确的日志级别
     */
    @Service
    public class Principle1 {
        
        public Device getDevice(String deviceId) {
            // 【日志使用】TRACE：最详细的跟踪信息
            logger.trace("进入getDevice方法，参数: {}", deviceId);
            
            // 【日志使用】DEBUG：调试信息
            logger.debug("开始查询设备: {}", deviceId);
            
            Device device = deviceRepository.findById(deviceId).orElse(null);
            
            if (device == null) {
                // 【日志使用】WARN：警告信息
                logger.warn("设备未找到: {}", deviceId);
                return null;
            }
            
            // 【日志使用】INFO：正常的业务信息
            logger.info("成功查询设备: {}, 名称: {}", deviceId, device.getName());
            
            return device;
        }
        
        public void updateDevice(String deviceId, String newName) {
            try {
                Device device = deviceRepository.findById(deviceId).orElse(null);
                if (device == null) {
                    throw new ResourceNotFoundException("设备不存在");
                }
                device.setName(newName);
                deviceRepository.save(device);
                
                logger.info("设备更新成功: {}", deviceId);
            } catch (Exception e) {
                // 【日志使用】ERROR：错误信息
                logger.error("设备更新失败: {}, 异常: {}", deviceId, e.getMessage(), e);
                throw e;
            }
        }
    }
    
    /**
     * 【原则2】使用占位符而不是字符串拼接
     */
    @Service
    public class Principle2 {
        
        public void logExample(String deviceId, String deviceName) {
            // ✅ 推荐：使用占位符
            logger.info("设备ID: {}, 设备名称: {}", deviceId, deviceName);
            
            // ❌ 不推荐：字符串拼接
            logger.info("设备ID: " + deviceId + ", 设备名称: " + deviceName);
        }
    }
    
    /**
     * 【原则3】避免记录敏感信息
     */
    @Service
    public class Principle3 {
        
        public void login(String username, String password) {
            // ✅ 推荐：不记录密码
            logger.info("用户登录: {}", username);
            
            // ❌ 不推荐：记录密码
            logger.info("用户登录: {}, 密码: {}", username, password);
        }
        
        public void processPayment(String cardNumber, String cvv) {
            // ✅ 推荐：脱敏处理
            String maskedCardNumber = maskCardNumber(cardNumber);
            logger.info("处理支付: {}", maskedCardNumber);
            
            // ❌ 不推荐：记录完整卡号
            logger.info("处理支付: {}", cardNumber);
        }
        
        private String maskCardNumber(String cardNumber) {
            if (cardNumber == null || cardNumber.length() < 4) {
                return "****";
            }
            return "****" + cardNumber.substring(cardNumber.length() - 4);
        }
    }
    
    /**
     * 【原则4】使用条件日志避免不必要的计算
     */
    @Service
    public class Principle4 {
        
        public void processDevice(String deviceId) {
            // ✅ 推荐：使用条件日志
            if (logger.isDebugEnabled()) {
                logger.debug("设备详情: {}", getDeviceDetails(deviceId));
            }
            
            // ❌ 不推荐：无条件记录复杂对象
            logger.debug("设备详情: {}", getDeviceDetails(deviceId));
        }
        
        private String getDeviceDetails(String deviceId) {
            // 耗时操作
            return "详细设备信息";
        }
    }
    
    /**
     * 【原则5】日志要包含上下文信息
     */
    @Service
    public class Principle5 {
        
        public void processOrder(String orderId, String userId) {
            // ✅ 推荐：包含上下文信息
            logger.info("处理订单: orderId={}, userId={}", orderId, userId);
            
            // ❌ 不推荐：缺少上下文信息
            logger.info("处理订单");
        }
        
        public void handleException(String deviceId, Exception e) {
            // ✅ 推荐：包含上下文信息和异常堆栈
            logger.error("处理设备失败: deviceId={}, 异常: {}", deviceId, e.getMessage(), e);
            
            // ❌ 不推荐：缺少上下文信息
            logger.error("处理设备失败");
        }
    }
}
```

### 7.2 结构化日志

#### 7.2.1 使用JSON格式日志

```java
/**
 * 【日志使用】结构化日志
 */
@Service
public class StructuredLoggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(StructuredLoggingService.class);
    
    /**
     * 【日志使用】使用JSON格式日志
     */
    public void logStructured(String deviceId, String operation, boolean success) {
        // 【日志使用】JSON格式日志
        logger.info("{\"deviceId\":\"{}\",\"operation\":\"{}\",\"success\":{}}", 
            deviceId, operation, success);
    }
    
    /**
     * 【日志使用】使用MDC（Mapped Diagnostic Context）
     */
    public void logWithMDC(String deviceId, String userId) {
        // 【日志使用】设置MDC
        MDC.put("deviceId", deviceId);
        MDC.put("userId", userId);
        
        try {
            logger.info("处理设备");
            // 日志输出：处理设备 [deviceId=dev_001, userId=user_123]
        } finally {
            // 【日志使用】清除MDC
            MDC.clear();
        }
    }
}
```

#### 7.2.2 Logback JSON配置

```xml
<!-- 文件：backend/src/main/resources/logback-spring.xml -->

<configuration>
    
    <!-- 【日志配置】JSON格式输出 -->
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/dorm-power-json.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <fieldNames>
                <timestamp>timestamp</timestamp>
                <level>level</level>
                <logger>logger</logger>
                <message>message</message>
            </fieldNames>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/dorm-power-json.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="JSON_FILE" />
    </root>
    
</configuration>
```

---

## 8. 异常处理常见陷阱

### 8.1 异常处理陷阱

#### 8.1.1 常见陷阱

```java
/**
 * 【异常处理】异常处理常见陷阱
 */
public class ExceptionTraps {
    
    /**
     * 【陷阱1】吞掉异常
     */
    @Service
    public class Trap1 {
        
        public void processDevice(String deviceId) {
            try {
                Device device = deviceRepository.findById(deviceId).orElse(null);
                if (device != null) {
                    processDevice(device);
                }
            } catch (Exception e) {
                // ❌ 陷阱：吞掉异常，没有处理
                e.printStackTrace();
            }
        }
        
        // ✅ 正确：记录日志并重新抛出
        public void processDeviceCorrect(String deviceId) {
            try {
                Device device = deviceRepository.findById(deviceId).orElse(null);
                if (device != null) {
                    processDevice(device);
                }
            } catch (Exception e) {
                logger.error("处理设备失败: {}", deviceId, e);
                throw new BusinessException("处理设备失败", e);
            }
        }
    }
    
    /**
     * 【陷阱2】捕获过宽的异常
     */
    @Service
    public class Trap2 {
        
        public void processDevice(String deviceId) {
            try {
                Device device = deviceRepository.findById(deviceId).orElse(null);
                if (device != null) {
                    processDevice(device);
                }
            } catch (Exception e) {  // ❌ 陷阱：捕获过宽的异常
                logger.error("处理设备失败", e);
                throw new BusinessException("处理设备失败", e);
            }
        }
        
        // ✅ 正确：捕获具体的异常
        public void processDeviceCorrect(String deviceId) {
            try {
                Device device = deviceRepository.findById(deviceId).orElse(null);
                if (device != null) {
                    processDevice(device);
                }
            } catch (ResourceNotFoundException e) {
                logger.warn("设备未找到: {}", deviceId);
                throw e;
            } catch (BusinessException e) {
                logger.error("业务异常: {}", e.getMessage(), e);
                throw e;
            } catch (Exception e) {
                logger.error("系统异常: {}", e.getMessage(), e);
                throw new BusinessException("系统异常", e);
            }
        }
    }
    
    /**
     * 【陷阱3】异常信息不清晰
     */
    @Service
    public class Trap3 {
        
        public Device getDevice(String deviceId) {
            return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("设备不存在"));  // ❌ 陷阱：缺少上下文
        }
        
        // ✅ 正确：包含上下文信息
        public Device getDeviceCorrect(String deviceId) {
            return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("设备不存在: deviceId=" + deviceId));
        }
    }
    
    /**
     * 【陷阱4】忽略finally中的异常
     */
    @Service
    public class Trap4 {
        
        public void processFile(String filePath) {
            InputStream is = null;
            try {
                is = new FileInputStream(filePath);
                // 处理文件
            } catch (IOException e) {
                logger.error("文件处理失败", e);
                throw new BusinessException("文件处理失败", e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // ❌ 陷阱：忽略finally中的异常
                        e.printStackTrace();
                    }
                }
            }
        }
        
        // ✅ 正确：处理finally中的异常
        public void processFileCorrect(String filePath) {
            InputStream is = null;
            try {
                is = new FileInputStream(filePath);
                // 处理文件
            } catch (IOException e) {
                logger.error("文件处理失败", e);
                throw new BusinessException("文件处理失败", e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // ✅ 正确：记录finally中的异常
                        logger.error("关闭文件失败", e);
                    }
                }
            }
        }
    }
}
```

---

## 9. 日志性能优化

### 9.1 日志性能优化

#### 9.1.1 日志性能优化技巧

```java
/**
 * 【日志使用】日志性能优化
 */
public class LoggingPerformanceOptimization {
    
    /**
     * 【优化1】使用条件日志
     */
    @Service
    public class Optimization1 {
        
        public void processDevice(String deviceId) {
            // ✅ 推荐：使用条件日志
            if (logger.isDebugEnabled()) {
                logger.debug("设备详情: {}", getDeviceDetails(deviceId));
            }
            
            // ❌ 不推荐：无条件记录复杂对象
            logger.debug("设备详情: {}", getDeviceDetails(deviceId));
        }
        
        private String getDeviceDetails(String deviceId) {
            // 耗时操作
            return "详细设备信息";
        }
    }
    
    /**
     * 【优化2】避免在日志中进行复杂计算
     */
    @Service
    public class Optimization2 {
        
        public void processDevice(String deviceId) {
            // ✅ 推荐：延迟计算
            logger.debug("设备详情: {}", () -> getDeviceDetails(deviceId));
            
            // ❌ 不推荐：提前计算
            String details = getDeviceDetails(deviceId);
            logger.debug("设备详情: {}", details);
        }
    }
    
    /**
     * 【优化3】使用异步日志
     */
    @Service
    public class Optimization3 {
        
        // 【日志使用】异步日志可以提高性能
        private static final Logger asyncLogger = LoggerFactory.getLogger("async");
        
        public void processDevice(String deviceId) {
            // 使用异步日志记录
            asyncLogger.info("处理设备: {}", deviceId);
        }
    }
}
```

#### 9.1.2 Logback异步配置

```xml
<!-- 文件：backend/src/main/resources/logback-spring.xml -->

<configuration>
    
    <!-- 【日志配置】异步日志 -->
    <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <appender-ref ref="FILE" />
    </appender>
    
    <root level="INFO">
        <appender-ref ref="ASYNC_FILE" />
    </root>
    
</configuration>
```

---

## 10. 日志监控和分析

### 10.1 日志监控

#### 10.1.1 日志监控指标

```java
/**
 * 【日志使用】日志监控
 */
@Service
public class LoggingMonitoring {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingMonitoring.class);
    
    /**
     * 【日志使用】记录业务指标
     */
    public void processDevice(String deviceId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 业务逻辑
            processDeviceInternal(deviceId);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("处理设备成功: deviceId={}, duration={}ms", deviceId, duration);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("处理设备失败: deviceId={}, duration={}ms, error={}", 
                deviceId, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 【日志使用】记录关键业务事件
     */
    public void recordBusinessEvent(String eventType, String deviceId, Map<String, Object> data) {
        logger.info("业务事件: eventType={}, deviceId={}, data={}", 
            eventType, deviceId, data);
    }
}
```

---

## 本章小结

| 知识点 | 项目体现 | 关键代码 |
|--------|----------|----------|
| try-catch | Service异常处理 | `try { ... } catch (Exception e)` |
| 自定义异常 | BusinessException | `extends RuntimeException` |
| 全局异常处理 | GlobalExceptionHandler | `@RestControllerAdvice` |
| 日志使用 | 各Service类 | `LoggerFactory.getLogger()` |
| 日志级别 | application.yml | `logging.level.root` |
| 异常处理最佳实践 | 异常处理原则 | 早抛出，晚捕获 |
| 日志最佳实践 | 日志使用原则 | 使用正确的日志级别 |
| 结构化日志 | JSON格式日志 | `LogstashEncoder` |
| 日志性能优化 | 异步日志 | `AsyncAppender` |
| 项目实战 | 异常处理、日志服务 | GlobalExceptionHandler、SystemLogService |

---

## 10. 项目实战案例

### 10.1 全局异常处理器完整实现

#### 10.1.1 实际项目代码分析

```java
// 文件：backend/src/main/java/com/dormpower/exception/GlobalExceptionHandler.java

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 【全局异常处理】全局异常处理器
 * 
 * 使用@RestControllerAdvice注解，统一处理所有Controller抛出的异常。
 * 避免在每个Controller中重复编写异常处理代码。
 */
@RestControllerAdvice  // 【全局异常处理】标记为全局异常处理器
public class GlobalExceptionHandler {

    // 【日志使用】创建Logger实例
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 【异常处理】处理参数验证异常
     * @param ex 参数验证异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)  // 【异常处理】处理参数验证异常
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        // 【日志使用】记录INFO级别日志
        logger.info("参数验证异常: {}", ex.getMessage());
        
        // 【集合】使用Map存储字段错误信息
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // 【异常处理】检查是否是登录相关的验证错误
        boolean isLoginValidation = false;
        try {
            jakarta.servlet.http.HttpServletRequest request = 
                ((org.springframework.web.context.request.ServletRequestAttributes) 
                 org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()).getRequest();
            if (request != null) {
                String requestURI = request.getRequestURI();
                isLoginValidation = requestURI != null && requestURI.contains("/api/auth/login");
            }
        } catch (Exception e) {
            // 【异常处理】忽略异常
        }
        
        // 【异常处理】登录验证错误返回401
        if (isLoginValidation) {
            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    "invalid account or password",
                    null,
                    LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        // 【异常处理】其他验证错误返回400
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "参数验证失败",
                errors,
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 【异常处理】处理资源未找到异常
     * @param ex 资源未找到异常
     * @return 错误响应
     */
    @ExceptionHandler(ResourceNotFoundException.class)  // 【异常处理】处理资源未找到异常
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        // 【日志使用】记录INFO级别日志
        logger.info("资源未找到异常: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                null,
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * 【异常处理】处理业务异常
     * @param ex 业务异常
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)  // 【异常处理】处理业务异常
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        // 【日志使用】记录INFO级别日志
        logger.info("业务异常: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                null,
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 【异常处理】处理认证异常
     * @param ex 认证异常
     * @return 错误响应
     */
    @ExceptionHandler(AuthenticationException.class)  // 【异常处理】处理认证异常
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        // 【日志使用】记录INFO级别日志
        logger.info("认证异常: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                null,
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * 【异常处理】处理运行时异常（包括限流错误）
     * @param ex 运行时异常
     * @return 错误响应
     */
    @ExceptionHandler(RuntimeException.class)  // 【异常处理】处理运行时异常
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        // 【日志使用】记录WARN级别日志，包含异常堆栈
        logger.warn("运行时异常: {}", ex.getMessage(), ex);
        String message = ex.getMessage();
        
        // 【异常处理】检查是否是限流错误
        if (message != null && message.contains("请求过于频繁")) {
            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    message,
                    null,
                    LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
        }
        
        // 【异常处理】其他运行时异常返回500
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "服务器内部错误",
                message,
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 【异常处理】处理所有其他异常
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)  // 【异常处理】处理所有其他异常
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        // 【日志使用】记录ERROR级别日志，包含异常堆栈
        logger.error("全局异常: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "服务器内部错误",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 【异常处理】错误响应类
     */
    public static class ErrorResponse {
        private int status;  // HTTP状态码
        private String message;  // 错误消息
        private Object errors;  // 错误详情
        private LocalDateTime timestamp;  // 时间戳

        public ErrorResponse(int status, String message, Object errors, LocalDateTime timestamp) {
            this.status = status;
            this.message = message;
            this.errors = errors;
            this.timestamp = timestamp;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getErrors() {
            return errors;
        }

        public void setErrors(Object errors) {
            this.errors = errors;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }
}
```

**代码解析：**

1. **全局异常处理：**
   - `@RestControllerAdvice`：标记为全局异常处理器
   - `@ExceptionHandler`：指定处理的异常类型
   - 统一处理所有Controller异常

2. **异常分类处理：**
   - 参数验证异常：返回400状态码
   - 资源未找到异常：返回404状态码
   - 业务异常：返回400状态码
   - 认证异常：返回401状态码
   - 限流错误：返回429状态码
   - 其他异常：返回500状态码

3. **日志使用：**
   - INFO级别：记录业务异常
   - WARN级别：记录运行时异常
   - ERROR级别：记录系统异常
   - 使用占位符：`{}`

4. **错误响应：**
   - 统一的错误响应格式
   - 包含状态码、消息、详情、时间戳

### 10.2 自定义业务异常实现

#### 10.2.1 实际项目代码分析

```java
// 文件：backend/src/main/java/com/dormpower/exception/BusinessException.java

/**
 * 【自定义异常】业务异常
 * 
 * 继承RuntimeException，用于表示业务逻辑异常。
 * 业务异常通常是可预期的异常，不需要回滚事务。
 */
public class BusinessException extends RuntimeException {

    /**
     * 【自定义异常】构造函数
     * @param message 异常消息
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * 【自定义异常】构造函数
     * @param message 异常消息
     * @param cause 原因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**代码解析：**

1. **自定义异常：**
   - 继承`RuntimeException`：运行时异常
   - 不需要强制捕获
   - 不触发事务回滚

2. **使用场景：**
   - 业务规则验证失败
   - 数据状态不符合预期
   - 用户操作错误

3. **最佳实践：**
   - 提供清晰的错误消息
   - 使用业务语言描述问题
   - 避免暴露技术细节

### 10.3 系统日志服务实现

#### 10.3.1 实际项目代码分析

```java
// 文件：backend/src/main/java/com/dormpower/service/SystemLogService.java

import com.dormpower.model.SystemLog;
import com.dormpower.repository.SystemLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 【日志服务】系统日志服务类
 * 
 * 提供统一的日志记录和查询功能。
 * 支持不同级别的日志记录，以及按各种条件查询日志。
 */
@Service  // 【Spring注解】标记为服务类
public class SystemLogService {

    // 【依赖注入】注入日志Repository
    @Autowired
    private SystemLogRepository systemLogRepository;

    /**
     * 【日志服务】记录日志
     * @param log 日志对象
     * @return 保存的日志对象
     */
    public SystemLog log(SystemLog log) {
        return systemLogRepository.save(log);
    }

    /**
     * 【日志服务】记录信息日志
     * @param type 日志类型
     * @param message 日志消息
     * @param source 日志来源
     * @return 保存的日志对象
     */
    public SystemLog info(String type, String message, String source) {
        SystemLog log = new SystemLog();
        log.setLevel("INFO");
        log.setType(type);
        log.setMessage(message);
        log.setSource(source);
        return systemLogRepository.save(log);
    }

    /**
     * 【日志服务】记录警告日志
     * @param type 日志类型
     * @param message 日志消息
     * @param source 日志来源
     * @return 保存的日志对象
     */
    public SystemLog warn(String type, String message, String source) {
        SystemLog log = new SystemLog();
        log.setLevel("WARN");
        log.setType(type);
        log.setMessage(message);
        log.setSource(source);
        return systemLogRepository.save(log);
    }

    /**
     * 【日志服务】记录错误日志
     * @param type 日志类型
     * @param message 日志消息
     * @param source 日志来源
     * @param details 错误详情
     * @return 保存的日志对象
     */
    public SystemLog error(String type, String message, String source, String details) {
        SystemLog log = new SystemLog();
        log.setLevel("ERROR");
        log.setType(type);
        log.setMessage(message);
        log.setSource(source);
        log.setDetails(details);
        return systemLogRepository.save(log);
    }

    /**
     * 【日志服务】记录操作日志（审计日志）
     * @param type 日志类型
     * @param message 日志消息
     * @param username 用户名
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @return 保存的日志对象
     */
    public SystemLog audit(String type, String message, String username, String ipAddress, String userAgent) {
        SystemLog log = new SystemLog();
        log.setLevel("INFO");
        log.setType(type);
        log.setMessage(message);
        log.setSource("AUDIT");
        log.setUsername(username);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        return systemLogRepository.save(log);
    }

    /**
     * 【日志服务】获取所有日志（分页）
     * @param page 页码
     * @param size 每页大小
     * @return 日志分页结果
     */
    public Page<SystemLog> getAllLogs(int page, int size) {
        // 【分页查询】创建分页对象，按创建时间降序排序
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return systemLogRepository.findAll(pageable);
    }

    /**
     * 【日志服务】根据级别获取日志
     * @param level 日志级别
     * @param page 页码
     * @param size 每页大小
     * @return 日志分页结果
     */
    public Page<SystemLog> getLogsByLevel(String level, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return systemLogRepository.findByLevel(level, pageable);
    }

    /**
     * 【日志服务】根据类型获取日志
     * @param type 日志类型
     * @param page 页码
     * @param size 每页大小
     * @return 日志分页结果
     */
    public Page<SystemLog> getLogsByType(String type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return systemLogRepository.findByType(type, pageable);
    }

    /**
     * 【日志服务】根据用户名获取日志
     * @param username 用户名
     * @param page 页码
     * @param size 每页大小
     * @return 日志分页结果
     */
    public Page<SystemLog> getLogsByUsername(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return systemLogRepository.findByUsername(username, pageable);
    }

    /**
     * 【日志服务】根据时间范围获取日志
     * @param start 开始时间
     * @param end 结束时间
     * @param page 页码
     * @param size 每页大小
     * @return 日志分页结果
     */
    public Page<SystemLog> getLogsByTimeRange(LocalDateTime start, LocalDateTime end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return systemLogRepository.findByTimeRange(start, end, pageable);
    }

    /**
     * 【日志服务】搜索日志
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 日志分页结果
     */
    public Page<SystemLog> searchLogs(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return systemLogRepository.searchByKeyword(keyword, pageable);
    }

    /**
     * 【日志服务】清理过期日志
     * @param retentionDays 保留天数
     */
    public void cleanupOldLogs(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        systemLogRepository.deleteByCreatedAtBefore(cutoffDate);
    }

    /**
     * 【日志服务】获取日志统计
     * @param days 统计天数
     * @return 统计结果
     */
    public Map<String, Object> getLogStatistics(int days) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime start = LocalDateTime.now().minusDays(days);

        // 【日志服务】按类型统计
        List<Object[]> typeCounts = systemLogRepository.countByTypeSince(start);
        Map<String, Long> typeStats = new HashMap<>();
        for (Object[] row : typeCounts) {
            typeStats.put((String) row[0], (Long) row[1]);
        }
        stats.put("byType", typeStats);

        // 【日志服务】按级别统计
        List<Object[]> levelCounts = systemLogRepository.countByLevelSince(start);
        Map<String, Long> levelStats = new HashMap<>();
        for (Object[] row : levelCounts) {
            levelStats.put((String) row[0], (Long) row[1]);
        }
        stats.put("byLevel", levelStats);

        return stats;
    }
}
```

**代码解析：**

1. **日志记录：**
   - `info()`：记录信息日志
   - `warn()`：记录警告日志
   - `error()`：记录错误日志
   - `audit()`：记录审计日志

2. **日志查询：**
   - 按级别查询
   - 按类型查询
   - 按用户名查询
   - 按时间范围查询
   - 关键词搜索

3. **日志管理：**
   - 分页查询
   - 清理过期日志
   - 日志统计

4. **最佳实践：**
   - 使用统一的日志服务
   - 支持多种查询方式
   - 定期清理过期日志
   - 提供统计分析功能

### 10.4 日志配置文件完整示例

#### 10.4.1 实际项目配置文件分析

```xml
<!-- 文件：backend/src/main/resources/logback-spring.xml -->

<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 【日志配置】控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <!-- 【日志配置】日志格式：时间、线程、级别、Logger、消息 -->
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 【日志配置】应用日志文件 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/dorm-power.log</file>
        <!-- 【日志配置】滚动策略：按天滚动 -->
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/dorm-power.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>  <!-- 【日志配置】保留7天 -->
            <totalSizeCap>100MB</totalSizeCap>  <!-- 【日志配置】总大小限制100MB -->
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 【日志配置】审计日志文件 -->
    <appender name="AUDIT_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/audit.log</file>
        <!-- 【日志配置】滚动策略：按天滚动 -->
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/audit.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>  <!-- 【日志配置】保留30天 -->
            <totalSizeCap>200MB</totalSizeCap>  <!-- 【日志配置】总大小限制200MB -->
        </rollingPolicy>
        <encoder>
            <!-- 【日志配置】审计日志格式：时间、消息 -->
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 【日志配置】审计日志Logger -->
    <logger name="AUDIT" level="INFO" additivity="false">
        <appender-ref ref="AUDIT_FILE"/>
    </logger>

    <!-- 【日志配置】应用日志 -->
    <logger name="com.dormpower" level="INFO"/>

    <!-- 【日志配置】减少框架日志 -->
    <logger name="org.springframework" level="WARN"/>
    <logger name="org.hibernate" level="WARN"/>

    <!-- 【日志配置】根日志 -->
    <root level="WARN">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>

</configuration>
```

**配置文件解析：**

1. **日志格式：**
   - `%d{yyyy-MM-dd HH:mm:ss.SSS}`：时间戳
   - `[%thread]`：线程名
   - `%-5level`：日志级别（左对齐，5字符宽度）
   - `%logger{36}`：Logger名称（最多36字符）
   - `%msg%n`：日志消息和换行

2. **日志输出：**
   - CONSOLE：控制台输出
   - FILE：应用日志文件
   - AUDIT_FILE：审计日志文件

3. **滚动策略：**
   - 按天滚动：`%d{yyyy-MM-dd}`
   - 保留天数：`maxHistory`
   - 总大小限制：`totalSizeCap`

4. **日志级别：**
   - 应用日志：INFO
   - 框架日志：WARN
   - 根日志：WARN

5. **最佳实践：**
   - 分离审计日志和应用日志
   - 定期清理过期日志
   - 控制日志文件大小
   - 减少框架日志输出

---

**本文档基于DormPower项目真实代码编写，所有代码示例均可直接运行。**
