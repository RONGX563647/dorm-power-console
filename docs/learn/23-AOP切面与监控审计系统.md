# 模块23：AOP切面与监控审计系统

> **学习时长**: 3-4 天  
> **难度**: ⭐⭐⭐⭐  
> **前置知识**: Spring AOP、面向切面编程、日志系统

---

## 一、系统架构

### 1.1 为什么需要AOP？

```yaml
AOP应用场景:
  1. 性能监控:
     - 接口响应时间统计
     - 慢请求自动检测
     - 方法调用统计
  
  2. 审计日志:
     - 操作记录
     - 数据变更追踪
     - 合规审计
  
  3. 异常处理:
     - 统一异常拦截
     - 错误日志记录
     - 告警通知
```

---

## 二、核心组件

### 2.1 ApiAspect API性能监控

**文件位置**: `backend/src/main/java/com/dormpower/aop/ApiAspect.java`

```java
package com.dormpower.aop;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * API性能监控切面
 * 
 * 功能:
 * 1. 统计所有Controller方法的执行时间
 * 2. 记录慢请求（> 1秒）
 * 3. 暴露Prometheus指标
 * 
 * @author dormpower team
 * @version 1.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ApiAspect {

    private final MeterRegistry meterRegistry;
    
    // 慢请求阈值（1秒）
    private static final long SLOW_REQUEST_THRESHOLD = 1000;
    
    /**
     * 环绕通知：拦截所有Controller方法
     */
    @Around("execution(* com.dormpower.controller..*(..))")
    public Object monitorApiPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String metricName = className + "." + methodName;
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录指标
            Timer.builder("api.request.duration")
                .tag("class", className)
                .tag("method", methodName)
                .register(meterRegistry)
                .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
            
            // 慢请求告警
            if (duration > SLOW_REQUEST_THRESHOLD) {
                log.warn("Slow API Request: {}.{} took {}ms", 
                    className, methodName, duration);
            }
            
            return result;
            
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录错误指标
            meterRegistry.counter("api.request.error", 
                "class", className, 
                "method", methodName,
                "exception", e.getClass().getSimpleName())
                .increment();
            
            log.error("API Error: {}.{} took {}ms, error: {}", 
                className, methodName, duration, e.getMessage());
            
            throw e;
        }
    }
}
```

### 2.2 AuditLogAspect 审计日志切面

**文件位置**: `backend/src/main/java/com/dormpower/aop/AuditLogAspect.java`

```java
package com.dormpower.aop;

import com.dormpower.annotation.AuditLog;
import com.dormpower.model.AuditLog;
import com.dormpower.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 审计日志切面
 * 
 * 功能:
 * 1. 记录关键操作
 * 2. 记录操作人、操作时间
 * 3. 记录操作前后的数据变化
 * 
 * 使用方式:
 * @AuditLog(module = "设备管理", action = "创建设备")
 * public Device createDevice(Device device) { ... }
 * 
 * @author dormpower team
 * @version 1.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    
    @Around("@annotation(auditLog)")
    public Object recordAuditLog(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        String userId = getCurrentUserId();  // 从SecurityContext获取
        String module = auditLog.module();
        String action = auditLog.action();
        
        // 记录操作前
        AuditLog beforeLog = new AuditLog();
        beforeLog.setId(UUID.randomUUID());
        beforeLog.setUserId(userId);
        beforeLog.setModule(module);
        beforeLog.setAction(action);
        beforeLog.setOperationTime(LocalDateTime.now());
        beforeLog.setParameters(getMethodParameters(joinPoint));
        
        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            
            // 记录操作后
            beforeLog.setResult("SUCCESS");
            beforeLog.setResultData(getResultSummary(result));
            
            auditLogService.save(beforeLog);
            
            log.info("Audit Log: user={}, module={}, action={}, result=SUCCESS", 
                userId, module, action);
            
            return result;
            
        } catch (Exception e) {
            // 记录失败
            beforeLog.setResult("FAILED");
            beforeLog.setErrorMessage(e.getMessage());
            
            auditLogService.save(beforeLog);
            
            log.error("Audit Log: user={}, module={}, action={}, result=FAILED, error={}", 
                userId, module, action, e.getMessage());
            
            throw e;
        }
    }
    
    private String getCurrentUserId() {
        // 从SecurityContext获取当前用户ID
        return "system";
    }
    
    private String getMethodParameters(ProceedingJoinPoint joinPoint) {
        // 获取方法参数（简化版）
        Object[] args = joinPoint.getArgs();
        if (args.length == 0) {
            return "";
        }
        return args[0].toString();
    }
    
    private String getResultSummary(Object result) {
        if (result == null) {
            return "null";
        }
        return result.toString();
    }
}
```

---

## 三、使用示例

### 3.1 自定义注解

```java
package com.dormpower.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    String module() default "";
    String action() default "";
}
```

### 3.2 Controller中使用

```java
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    @AuditLog(module = "设备管理", action = "创建设备")
    @PostMapping
    public Device createDevice(@RequestBody Device device) {
        return deviceService.create(device);
    }
    
    @AuditLog(module = "设备管理", action = "删除设备")
    @DeleteMapping("/{id}")
    public void deleteDevice(@PathVariable String id) {
        deviceService.delete(id);
    }
}
```

---

**最后更新**: 2026-04-22  
**文档版本**: 1.0
