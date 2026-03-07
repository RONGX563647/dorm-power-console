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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AOP切面类：统一处理API请求日志、限流和审计日志。
 * 通过注解实现横切关注点，提升代码可维护性和安全性。
 */
@Aspect
@Component
public class ApiAspect {

    // 普通日志记录器
    private static final Logger logger = LoggerFactory.getLogger(ApiAspect.class);
    // 专用审计日志记录器
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

    // API全局限流器（如每秒最大请求数）
    @Autowired
    private RateLimiter apiRateLimiter;

    // 登录接口专用限流器
    @Autowired
    private RateLimiter loginRateLimiter;

    // 设备级别限流器（如针对不同设备ID限流）
    @Autowired
    private ConcurrentHashMap<String, RateLimiter> deviceRateLimiters;

    // 动态存储各类限流器（按类型区分）
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    /**
     * API请求日志和限流处理
     * 拦截所有带 @RateLimit 注解的方法，或属于 RestController 的方法。
     * 实现：
     * 1. 获取请求信息（方法、URI、IP）。
     * 2. 判断是否为管理员，管理员跳过限流。
     * 3. 按注解参数或默认限流器进行限流。
     * 4. 记录请求日志，统计耗时。
     * 5. 异常时记录错误日志。
     */
    @Around("@annotation(com.dormpower.annotation.RateLimit) || " +
            "@within(org.springframework.web.bind.annotation.RestController)")
    public Object handleApiRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis(); // 记录开始时间

        HttpServletRequest request = getRequest(); // 获取请求对象
        String method = request != null ? request.getMethod() : "UNKNOWN"; // 请求方法
        String uri = request != null ? request.getRequestURI() : "UNKNOWN"; // 请求路径
        String ip = request != null ? getClientIp(request) : "UNKNOWN"; // 客户端IP

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = signature.getMethod();

        // 检查是否为管理员用户，如果是则跳过限流
        if (!isAdminUser()) {
            // 限流检查：优先使用方法上的 @RateLimit 注解
            RateLimit rateLimit = targetMethod.getAnnotation(RateLimit.class);
            if (rateLimit != null) {
                RateLimiter limiter = getRateLimiter(rateLimit); // 获取限流器
                if (!limiter.tryAcquire()) { // 限流未通过
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
        }

        // 记录请求开始日志
        logger.debug("请求开始: {} {} from {}", method, uri, ip);

        try {
            Object result = joinPoint.proceed(); // 执行目标方法

            long duration = System.currentTimeMillis() - startTime; // 计算耗时
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
     * 拦截所有带 @AuditLog 注解的方法。
     * 实现：
     * 1. 获取注解参数（type、value）。
     * 2. 获取请求用户和IP。
     * 3. 方法执行前后分别记录审计日志，包含操作类型、描述、用户/IP、耗时和结果。
     */
    @Around("@annotation(com.dormpower.annotation.AuditLog)")
    public Object handleAuditLog(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuditLog auditLog = method.getAnnotation(AuditLog.class); // 获取注解实例

        HttpServletRequest request = getRequest();
        String ip = request != null ? getClientIp(request) : "UNKNOWN"; // 客户端IP
        String user = request != null ? request.getHeader("X-User") : "anonymous"; // 用户标识

        long startTime = System.currentTimeMillis(); // 记录开始时间

        try {
            Object result = joinPoint.proceed(); // 执行目标方法

            long duration = System.currentTimeMillis() - startTime; // 计算耗时
            // 记录成功审计日志
            auditLogger.info("[{}] {} - {} - {}ms - SUCCESS", 
                    auditLog.type(), auditLog.value(), user != null ? user : ip, duration);

            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            // 记录失败审计日志
            auditLogger.error("[{}] {} - {} - {}ms - FAILED: {}", 
                    auditLog.type(), auditLog.value(), user != null ? user : ip, duration, e.getMessage());
            throw e;
        }
    }

    /**
     * 获取限流器
     * 根据 @RateLimit 注解的 type 和 value 动态创建或获取限流器。
     * type 用于区分不同限流场景，value 通常为限流速率。
     */
    private RateLimiter getRateLimiter(RateLimit rateLimit) {
        String key = rateLimit.type(); // 限流类型作为key
        // 若不存在则创建新的限流器
        return rateLimiters.computeIfAbsent(key, k -> RateLimiter.create(rateLimit.value()));
    }

    /**
     * 获取当前请求对象
     * 用于获取请求信息、头部、IP等。
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取客户端真实IP地址
     * 优先从 X-Forwarded-For、X-Real-IP 获取，最后取 remoteAddr。
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

    /**
     * 检查当前用户是否为管理员
     * 通过 Spring Security 获取认证信息，判断是否拥有 ROLE_ADMIN 权限。
     * 管理员用户可跳过限流。
     */
    private boolean isAdminUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            // 检查用户是否具有ROLE_ADMIN权限
            return authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        }
        return false;
    }

}
