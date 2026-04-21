package com.dormpower.aop;

import com.dormpower.monitoring.PrometheusMetrics;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Prometheus 监控 AOP 切面
 * 
 * 自动采集:
 * - API 请求耗时 (P50, P90, P99)
 * - API 请求总数 (按方法、路径、状态码分组)
 * - 异常统计
 * 
 * @author dormpower team
 * @version 1.0
 */
@Aspect
@Component
public class MonitoringAspect {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringAspect.class);

    @Autowired
    private PrometheusMetrics prometheusMetrics;

    /**
     * 切点：所有 Controller 方法
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerPointcut() {
    }

    /**
     * 监控 Controller 方法执行
     * 
     * @param joinPoint 切点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("controllerPointcut()")
    public Object monitorController(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        HttpServletRequest request = getRequest();
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";
        
        int status = 200;  // 默认成功
        
        try {
            Object result = joinPoint.proceed();
            return result;
            
        } catch (Exception e) {
            status = 500;  // 服务器错误
            logger.error("监控到异常：{}.{} - {}", 
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                e.getMessage());
            throw e;
            
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录指标
            prometheusMetrics.recordApiDuration(method, uri, status, duration);
            prometheusMetrics.incrementApiRequest(method, uri, status);
            
            logger.debug("API 监控：{} {} - {}ms - HTTP{}", method, uri, duration, status);
        }
    }

    /**
     * 获取当前 HTTP 请求
     * 
     * @return HttpServletRequest
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
}
