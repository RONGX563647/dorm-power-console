package com.dormpower.aop;

import com.dormpower.annotation.AuditLog;
import com.dormpower.model.AuditLog as AuditLogEntity;
import com.dormpower.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * AOP 审计日志切面
 * 
 * 自动记录所有标注@AuditLog 的操作:
 * - 操作人信息
 * - 操作类型
 * - 操作资源
 * - 操作参数
 * - 操作结果
 * - 操作耗时
 * 
 * @author dormpower team
 * @version 1.0
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogAspect.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 审计日志切面
     */
    @Around("@annotation(com.dormpower.annotation.AuditLog)")
    public Object aroundAuditLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuditLog auditLogAnnotation = method.getAnnotation(AuditLog.class);
        
        // 获取审计日志信息
        String action = auditLogAnnotation.action();
        String resource = parseResourceExpression(auditLogAnnotation.resource(), joinPoint);
        String description = auditLogAnnotation.description();
        
        // 创建审计日志实体
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setAction(action);
        auditLog.setResource(resource);
        auditLog.setDescription(description);
        
        // 记录请求参数
        if (auditLogAnnotation.logParams()) {
            try {
                Map<String, Object> params = new HashMap<>();
                String[] paramNames = signature.getParameterNames();
                Object[] args = joinPoint.getArgs();
                
                for (int i = 0; i < paramNames.length; i++) {
                    params.put(paramNames[i], args[i]);
                }
                
                auditLog.setParams(objectMapper.writeValueAsString(params));
            } catch (Exception e) {
                logger.warn("记录请求参数失败", e);
            }
        }
        
        // 获取用户信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            auditLog.setUsername(auth.getName());
            // TODO: 从数据库查询用户 ID
        }
        
        // 获取请求信息
        HttpServletRequest request = getRequest();
        if (request != null) {
            auditLog.setIpAddress(getClientIp(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
        }
        
        try {
            // 执行方法
            Object result = joinPoint.proceed();
            
            // 记录成功
            auditLog.setStatus("SUCCESS");
            auditLog.setDurationMs(System.currentTimeMillis() - startTime);
            
            // 记录响应结果
            if (auditLogAnnotation.logResult()) {
                try {
                    auditLog.setResult(objectMapper.writeValueAsString(result));
                } catch (Exception e) {
                    logger.warn("记录响应结果失败", e);
                }
            }
            
            // 保存审计日志 (异步)
            saveAuditLogAsync(auditLog);
            
            return result;
            
        } catch (Throwable e) {
            // 记录失败
            auditLog.setStatus("FAILED");
            auditLog.setDurationMs(System.currentTimeMillis() - startTime);
            auditLog.setErrorMessage(e.getMessage());
            
            // 保存审计日志
            saveAuditLogAsync(auditLog);
            
            throw e;
        }
    }

    /**
     * 解析资源表达式
     * 支持 SpEL 表达式，如：#deviceId, #userId
     */
    private String parseResourceExpression(String expression, ProceedingJoinPoint joinPoint) {
        if (expression == null || expression.isEmpty()) {
            return "";
        }
        
        // 简单实现：替换#xxx 为实际参数值
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        
        String resource = expression;
        for (int i = 0; i < paramNames.length; i++) {
            String paramName = "#" + paramNames[i];
            if (resource.contains(paramName)) {
                resource = resource.replace(paramName, String.valueOf(args[i]));
            }
        }
        
        return resource;
    }

    /**
     * 异步保存审计日志
     */
    private void saveAuditLogAsync(AuditLogEntity auditLog) {
        // TODO: 使用异步线程池保存
        try {
            auditLogRepository.save(auditLog);
            logger.info("审计日志保存成功：{} - {} - {}", 
                auditLog.getUsername(), auditLog.getAction(), auditLog.getResource());
        } catch (Exception e) {
            logger.error("保存审计日志失败", e);
        }
    }

    /**
     * 获取当前 HTTP 请求
     */
    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            logger.warn("获取请求对象失败", e);
            return null;
        }
    }

    /**
     * 获取客户端 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
