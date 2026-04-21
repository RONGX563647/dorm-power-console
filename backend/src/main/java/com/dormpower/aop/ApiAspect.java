package com.dormpower.aop;

import com.dormpower.annotation.AuditLog;
import com.dormpower.annotation.RateLimit;
import com.dormpower.util.RedisRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * AOP切面类：统一处理API请求日志、限流和审计日志。
 * 通过注解实现横切关注点，提升代码可维护性和安全性。
 *
 * 使用 Redis+Lua 分布式限流，支持多实例部署:
 * - 滑动窗口算法，精确到毫秒
 * - Lua 脚本原子操作，避免并发问题
 * - 支持 10000+ QPS
 */
@Aspect
@Component
public class ApiAspect {

    private static final Logger logger = LoggerFactory.getLogger(ApiAspect.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

    // Redis 分布式限流器 (Lua 脚本实现)
    @Autowired(required = false)
    private RedisRateLimiter redisRateLimiter;

    // Spring Environment 用于检查 active profiles
    @Autowired
    private Environment environment;

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

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = signature.getMethod();

        // 测试环境跳过限流（使用 Environment 检查，支持 @ActiveProfiles 注解）
        boolean isTestProfile = Arrays.asList(environment.getActiveProfiles()).contains("test");

        // 检查是否为管理员用户，如果是则跳过限流
        if (!isTestProfile && !isAdminUser()) {
            // 限流检查 - 使用 Redis+Lua 分布式限流
            RateLimit rateLimit = targetMethod.getAnnotation(RateLimit.class);
            if (rateLimit != null && redisRateLimiter != null) {
                // 构建限流 key: rate_limit:{type}:{ip}
                String limitKey = buildRateLimitKey(rateLimit.type(), ip);
                
                // 滑动窗口限流：windowSize 秒内最多 value 个请求
                if (!redisRateLimiter.tryAcquire(limitKey, rateLimit.value(), rateLimit.windowSize())) {
                    logger.warn("请求被限流 (Redis+Lua): {} {} from {} - 限制：{}/{}s", 
                        method, uri, ip, rateLimit.value(), rateLimit.windowSize());
                    throw new RuntimeException("请求过于频繁，请稍后再试");
                }
            }
        }

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

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

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

    /**
     * 构建限流 key
     * 
     * @param type 限流类型
     * @param ip 客户端 IP
     * @return 限流 key
     */
    private String buildRateLimitKey(String type, String ip) {
        return String.format("rate_limit:%s:%s", type, ip);
    }

    private boolean isAdminUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        }
        return false;
    }
}