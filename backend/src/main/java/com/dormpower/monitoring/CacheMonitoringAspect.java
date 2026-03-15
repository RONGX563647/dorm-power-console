package com.dormpower.monitoring;

import com.dormpower.cache.MultiLevelCache;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 缓存监控切面
 * 
 * 自动监控缓存操作的性能指标
 * 
 * @author dormpower team
 * @version 1.0
 */
@Aspect
@Component
@ConditionalOnProperty(name = "spring.data.redis.host")
public class CacheMonitoringAspect {

    private static final Logger logger = LoggerFactory.getLogger(CacheMonitoringAspect.class);

    @Autowired
    private CacheMetrics cacheMetrics;

    @Around("execution(* com.dormpower.cache.MultiLevelCache.get(..))")
    public Object monitorCacheGet(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.nanoTime();
        
        try {
            Object result = joinPoint.proceed();
            
            long duration = System.nanoTime() - startTime;
            cacheMetrics.recordGetTime(duration, TimeUnit.NANOSECONDS);
            
            if (result != null) {
                cacheMetrics.recordHit();
            } else {
                cacheMetrics.recordMiss();
            }
            
            return result;
        } catch (Throwable e) {
            logger.error("Cache get operation failed: {}", e.getMessage());
            throw e;
        }
    }

    @Around("execution(* com.dormpower.cache.MultiLevelCache.put(..))")
    public Object monitorCachePut(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.nanoTime();
        
        try {
            Object result = joinPoint.proceed();
            
            long duration = System.nanoTime() - startTime;
            cacheMetrics.recordPutTime(duration, TimeUnit.NANOSECONDS);
            cacheMetrics.recordPut();
            
            return result;
        } catch (Throwable e) {
            logger.error("Cache put operation failed: {}", e.getMessage());
            throw e;
        }
    }

    @Around("execution(* com.dormpower.cache.MultiLevelCache.evict(..))")
    public Object monitorCacheEvict(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Object result = joinPoint.proceed();
            cacheMetrics.recordEvict();
            return result;
        } catch (Throwable e) {
            logger.error("Cache evict operation failed: {}", e.getMessage());
            throw e;
        }
    }

    @Around("execution(* com.dormpower.cache.MultiLevelCache.clear(..))")
    public Object monitorCacheClear(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Object result = joinPoint.proceed();
            cacheMetrics.recordClear();
            return result;
        } catch (Throwable e) {
            logger.error("Cache clear operation failed: {}", e.getMessage());
            throw e;
        }
    }
}
