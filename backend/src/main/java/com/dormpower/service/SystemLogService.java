package com.dormpower.service;

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
 * 系统日志服务类
 */
@Service
public class SystemLogService {

    @Autowired
    private SystemLogRepository systemLogRepository;

    /**
     * 记录日志
     */
    public SystemLog log(SystemLog log) {
        return systemLogRepository.save(log);
    }

    /**
     * 记录信息日志
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
     * 记录警告日志
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
     * 记录错误日志
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
     * 记录操作日志
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
     * 获取所有日志（分页）
     */
    public Page<SystemLog> getAllLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return systemLogRepository.findAll(pageable);
    }

    /**
     * 根据级别获取日志
     */
    public Page<SystemLog> getLogsByLevel(String level, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return systemLogRepository.findByLevel(level, pageable);
    }

    /**
     * 根据类型获取日志
     */
    public Page<SystemLog> getLogsByType(String type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return systemLogRepository.findByType(type, pageable);
    }

    /**
     * 根据用户名获取日志
     */
    public Page<SystemLog> getLogsByUsername(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return systemLogRepository.findByUsername(username, pageable);
    }

    /**
     * 根据时间范围获取日志
     */
    public Page<SystemLog> getLogsByTimeRange(LocalDateTime start, LocalDateTime end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return systemLogRepository.findByTimeRange(start, end, pageable);
    }

    /**
     * 搜索日志
     */
    public Page<SystemLog> searchLogs(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return systemLogRepository.searchByKeyword(keyword, pageable);
    }

    /**
     * 清理过期日志
     */
    public void cleanupOldLogs(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        systemLogRepository.deleteByCreatedAtBefore(cutoffDate);
    }

    /**
     * 获取日志统计
     */
    public Map<String, Object> getLogStatistics(int days) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime start = LocalDateTime.now().minusDays(days);

        // 按类型统计
        List<Object[]> typeCounts = systemLogRepository.countByTypeSince(start);
        Map<String, Long> typeStats = new HashMap<>();
        for (Object[] row : typeCounts) {
            typeStats.put((String) row[0], (Long) row[1]);
        }
        stats.put("byType", typeStats);

        // 按级别统计
        List<Object[]> levelCounts = systemLogRepository.countByLevelSince(start);
        Map<String, Long> levelStats = new HashMap<>();
        for (Object[] row : levelCounts) {
            levelStats.put((String) row[0], (Long) row[1]);
        }
        stats.put("byLevel", levelStats);

        return stats;
    }
}
