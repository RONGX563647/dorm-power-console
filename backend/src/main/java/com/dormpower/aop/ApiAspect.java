package com.dormpower.aop;

import com.dormpower.annotation.AuditLog;
import com.dormpower.annotation.RateLimit;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AOP切面（请求日志、限流、审计）
 */
@Aspect
@Component
public class ApiAspect {

    private static final Logger logger = LoggerFactory.getLogger(ApiAspect.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

    @Autowired
    private RateLimiter apiRateLimiter;

    @Autowired
    private RateLimiter loginRateLimiter;

    @Autowired
    private ConcurrentHashMap<String, RateLimiter> deviceRateLimiters;

    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    /**
     * API请求日志和限流
     */
    @Around("@annotation(com.dormpower.annotation.RateLimit) || " +
            "@within(org.springframework.web.bind.annotation.RestController)")
    public Object handleApiRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        HttpServletRequest request = getRequest();
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";
        String ip = request != null ? getClientIp(request) : "UNKNOWN";
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = signature.getMethod();
        
        // 限流检查
        RateLimit rateLimit = targetMethod.getAnnotation(RateLimit.class);
        if (rateLimit != null) {
            RateLimiter limiter = getRateLimiter(rateLimit);
            if (!limiter.tryAcquire()) {
                logger.warn("请求被限流: {} {} from {}", method, uri, ip);
                throw new RuntimeException("请求过于频繁，请稍后再试");
            }
        } else {
            // 默认API限流
            if (!apiRateLimiter.tryAcquire()) {
                logger.warn("API请求被限流: {} {} from {}", method, uri, ip);
                throw new RuntimeException("请求过于频繁，请稍后再试");
            }
        }
        
        // 记录请求日志
        logger.debug("请求开始: {} {} from {}", method, uri, ip);
        
        try {
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("请求完成: {} {} - {}ms - {}", method, uri, duration, ip);
            
            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("请求失败: {} {} - {}ms - {} - {}", method, uri, duration, ip, e.getMessage());
            throw e;
        }
    }

    /**
     * 审计日志
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
            auditLogger.info("[{}] {} - {} - {}ms - SUCCESS", 
                    auditLog.type(), auditLog.value(), user != null ? user : ip, duration);
            
            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            auditLogger.error("[{}] {} - {} - {}ms - FAILED: {}", 
                    auditLog.type(), auditLog.value(), user != null ? user : ip, duration, e.getMessage());
            throw e;
        }
    }

    /**
     * 获取限流器
     */
    private RateLimiter getRateLimiter(RateLimit rateLimit) {
        String key = rateLimit.type();
        return rateLimiters.computeIfAbsent(key, k -> RateLimiter.create(rateLimit.value()));
    }

    /**
     * 获取请求对象
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取客户端IP
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
