package com.dormpower.service;

import com.dormpower.model.Device;
import com.dormpower.model.SystemMetrics;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.SystemMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 监控服务类
 */
@Service
public class MonitoringService {

    @Autowired
    private SystemMetricsRepository systemMetricsRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    private final OperatingSystemMXBean osMXBean;
    private final MemoryMXBean memoryMXBean;
    private final RuntimeMXBean runtimeMXBean;

    public MonitoringService() {
        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    }

    /**
     * 收集系统指标
     */
    public void collectSystemMetrics() {
        // CPU使用率
        double cpuUsage = osMXBean.getSystemLoadAverage();
        if (cpuUsage >= 0) {
            saveMetric("SYSTEM", "cpu_usage", cpuUsage, "%");
        }

        // 内存使用
        long usedMemory = memoryMXBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryMXBean.getHeapMemoryUsage().getMax();
        double memoryUsagePercent = maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0;
        saveMetric("SYSTEM", "memory_usage", memoryUsagePercent, "%");
        saveMetric("SYSTEM", "memory_used_mb", usedMemory / (1024.0 * 1024), "MB");

        // 运行时间
        long uptime = runtimeMXBean.getUptime();
        saveMetric("SYSTEM", "uptime_minutes", uptime / 60000.0, "minutes");
    }

    /**
     * 记录API响应时间
     */
    public void recordApiResponseTime(String endpoint, long responseTimeMs) {
        saveMetric("API", "response_time_" + endpoint, (double) responseTimeMs, "ms");
    }

    /**
     * 记录API请求
     */
    public void recordApiRequest(String endpoint) {
        saveMetric("API", "request_count_" + endpoint, 1.0, "count");
    }

    /**
     * 获取系统状态
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();

        // CPU信息
        int cpuCores = osMXBean.getAvailableProcessors();
        double cpuLoad = osMXBean.getSystemLoadAverage();
        double cpuUsage = cpuLoad >= 0 ? (cpuLoad / cpuCores) * 100 : 0;
        
        status.put("cpuCores", cpuCores);
        status.put("cpuLoad", cpuLoad);
        status.put("cpuUsage", cpuUsage);

        // 内存信息
        long usedMemory = memoryMXBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryMXBean.getHeapMemoryUsage().getMax();
        long committedMemory = memoryMXBean.getHeapMemoryUsage().getCommitted();

        Map<String, Object> memoryInfo = new HashMap<>();
        memoryInfo.put("used", usedMemory);
        memoryInfo.put("total", maxMemory);
        memoryInfo.put("max", maxMemory);
        memoryInfo.put("committed", committedMemory);
        memoryInfo.put("usedMB", usedMemory / (1024 * 1024));
        memoryInfo.put("maxMB", maxMemory / (1024 * 1024));
        memoryInfo.put("usagePercent", maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0);
        status.put("memory", memoryInfo);
        
        // 磁盘信息
        java.io.File root = new java.io.File("/");
        long totalDisk = root.getTotalSpace();
        long freeDisk = root.getFreeSpace();
        long usedDisk = totalDisk - freeDisk;
        double diskUsagePercent = totalDisk > 0 ? (double) usedDisk / totalDisk * 100 : 0;
        
        Map<String, Object> diskInfo = new HashMap<>();
        diskInfo.put("total", totalDisk);
        diskInfo.put("used", usedDisk);
        diskInfo.put("free", freeDisk);
        diskInfo.put("usagePercent", diskUsagePercent);
        status.put("disk", diskInfo);

        // 运行时间（转换为秒）
        status.put("uptime", runtimeMXBean.getUptime() / 1000);
        status.put("uptimeFormatted", formatUptime(runtimeMXBean.getUptime()));

        return status;
    }

    /**
     * 获取设备在线状态
     */
    public Map<String, Object> getDeviceStatus() {
        Map<String, Object> status = new HashMap<>();

        List<Device> allDevices = deviceRepository.findAll();
        long totalDevices = allDevices.size();
        long now = System.currentTimeMillis() / 1000;
        long onlineTimeout = 60; // 60秒超时

        long onlineDevices = allDevices.stream()
                .filter(d -> d.isOnline() && (now - d.getLastSeenTs()) < onlineTimeout)
                .count();

        long offlineDevices = totalDevices - onlineDevices;

        status.put("totalDevices", totalDevices);
        status.put("onlineDevices", onlineDevices);
        status.put("offlineDevices", offlineDevices);
        status.put("onlineRate", totalDevices > 0 ? (double) onlineDevices / totalDevices * 100 : 0);

        return status;
    }

    /**
     * 获取API性能统计
     */
    public Map<String, Object> getApiPerformanceStats(int hours) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime start = LocalDateTime.now().minusHours(hours);

        // 获取平均响应时间
        Double avgResponseTime = systemMetricsRepository.calculateAverage("API", start);
        stats.put("averageResponseTime", avgResponseTime != null ? avgResponseTime : 0);

        // 获取最大响应时间
        Double maxResponseTime = systemMetricsRepository.findMaxValue("API", start);
        stats.put("maxResponseTime", maxResponseTime != null ? maxResponseTime : 0);

        return stats;
    }

    /**
     * 获取历史指标
     */
    public List<SystemMetrics> getHistoricalMetrics(String metricType, int hours) {
        LocalDateTime start = LocalDateTime.now().minusHours(hours);
        LocalDateTime end = LocalDateTime.now();
        return systemMetricsRepository.findByMetricTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                metricType, start, end);
    }

    /**
     * 清理过期指标
     */
    public void cleanupOldMetrics(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        systemMetricsRepository.deleteByCreatedAtBefore(cutoffDate);
    }

    /**
     * 保存指标
     */
    private void saveMetric(String type, String name, double value, String unit) {
        SystemMetrics metric = new SystemMetrics();
        metric.setMetricType(type);
        metric.setMetricName(name);
        metric.setMetricValue(value);
        metric.setMetricUnit(unit);
        systemMetricsRepository.save(metric);
    }

    /**
     * 格式化运行时间
     */
    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%d天 %d小时 %d分钟", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%d小时 %d分钟", hours, minutes % 60);
        } else {
            return String.format("%d分钟", minutes);
        }
    }
}
