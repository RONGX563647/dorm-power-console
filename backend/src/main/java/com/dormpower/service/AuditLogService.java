package com.dormpower.service;

import com.dormpower.model.AuditLog;
import com.dormpower.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 审计日志服务
 */
@Service
@Aspect
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 记录操作日志
     */
    public AuditLog log(String username, String module, String action, String status) {
        AuditLog log = new AuditLog(username, module, action, status);
        log.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                log.setIpAddress(getClientIp(request));
                log.setUserAgent(request.getHeader("User-Agent"));
                log.setRequestMethod(request.getMethod());
                log.setRequestUrl(request.getRequestURI());
            }
        } catch (Exception ignored) {
        }
        
        return auditLogRepository.save(log);
    }

    /**
     * 记录详细操作日志
     */
    public AuditLog logDetail(String username, String module, String action, 
                               String targetType, String targetId, String status,
                               String requestParams, String requestBody) {
        AuditLog log = new AuditLog(username, module, action, status);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setRequestParams(requestParams);
        log.setRequestBody(requestBody);
        log.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                log.setIpAddress(getClientIp(request));
                log.setUserAgent(request.getHeader("User-Agent"));
                log.setRequestMethod(request.getMethod());
                log.setRequestUrl(request.getRequestURI());
            }
        } catch (Exception ignored) {
        }
        
        return auditLogRepository.save(log);
    }

    /**
     * 记录操作日志（带响应数据）
     */
    public AuditLog logWithResponse(String username, String module, String action,
                                     String status, String responseData, long duration) {
        AuditLog log = new AuditLog(username, module, action, status);
        log.setResponseData(responseData);
        log.setDuration(duration);
        log.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                log.setIpAddress(getClientIp(request));
                log.setUserAgent(request.getHeader("User-Agent"));
                log.setRequestMethod(request.getMethod());
                log.setRequestUrl(request.getRequestURI());
            }
        } catch (Exception ignored) {
        }
        
        return auditLogRepository.save(log);
    }

    /**
     * 获取用户操作历史
     */
    public Page<AuditLog> getUserAuditLogs(String username, Pageable pageable) {
        return auditLogRepository.findByUsernameOrderByTsDesc(username, pageable);
    }

    /**
     * 获取模块操作日志
     */
    public Page<AuditLog> getModuleAuditLogs(String module, Pageable pageable) {
        return auditLogRepository.findByModuleOrderByTsDesc(module, pageable);
    }

    /**
     * 获取所有审计日志
     */
    public Page<AuditLog> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTsDesc(pageable);
    }

    /**
     * 获取时间范围内的审计日志
     */
    public Page<AuditLog> getAuditLogsByTimeRange(long startTime, long endTime, Pageable pageable) {
        return auditLogRepository.findByTimeRange(startTime, endTime, pageable);
    }

    /**
     * 获取用户在时间范围内的审计日志
     */
    public Page<AuditLog> getUserAuditLogsByTimeRange(String username, long startTime, long endTime, Pageable pageable) {
        return auditLogRepository.findByUsernameAndTimeRange(username, startTime, endTime, pageable);
    }

    /**
     * 获取审计统计
     */
    public Map<String, Object> getAuditStatistics(long since) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalOperations", auditLogRepository.count());

        List<Object[]> moduleStats = auditLogRepository.countByModuleSince(since);
        stats.put("operationsByModule", moduleStats);

        List<Object[]> topUsers = auditLogRepository.findTopActiveUsers(since, PageRequest.of(0, 10));
        stats.put("topActiveUsers", topUsers);

        return stats;
    }

    /**
     * 清理过期日志
     */
    public long cleanupOldLogs(int retentionDays) {
        long cutoff = System.currentTimeMillis() / 1000 - (retentionDays * 86400L);
        return auditLogRepository.deleteByTsBefore(cutoff);
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
