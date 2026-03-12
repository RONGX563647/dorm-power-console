package com.dormpower.controller;

import com.dormpower.model.SystemMetrics;
import com.dormpower.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 监控控制器
 */
@RestController
@RequestMapping("/api/admin/monitor")
@Tag(name = "系统监控", description = "系统监控和性能监控接口")
public class MonitoringController {

    @Autowired
    private MonitoringService monitoringService;

    /**
     * 获取系统状态
     */
    @Operation(summary = "获取系统状态", description = "获取系统运行状态和资源使用情况", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/system")
    public ResponseEntity<?> getSystemStatus() {
        Map<String, Object> status = monitoringService.getSystemStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * 获取设备状态
     */
    @Operation(summary = "获取设备状态", description = "获取设备在线状态统计", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/devices")
    public ResponseEntity<?> getDeviceStatus() {
        Map<String, Object> status = monitoringService.getDeviceStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * 获取API性能统计
     */
    @Operation(summary = "获取API性能统计", description = "获取API响应时间等性能指标", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/api-performance")
    public ResponseEntity<?> getApiPerformanceStats(
            @Parameter(description = "统计小时数", example = "24")
            @RequestParam(defaultValue = "24") int hours) {
        Map<String, Object> stats = monitoringService.getApiPerformanceStats(hours);
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取历史指标
     */
    @Operation(summary = "获取历史指标", description = "获取指定类型的历史监控指标", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/metrics")
    public ResponseEntity<?> getHistoricalMetrics(
            @Parameter(description = "指标类型", required = true, example = "SYSTEM")
            @RequestParam String type,
            @Parameter(description = "小时数", example = "24")
            @RequestParam(defaultValue = "24") int hours) {
        List<SystemMetrics> metrics = monitoringService.getHistoricalMetrics(type, hours);
        return ResponseEntity.ok(metrics);
    }

    /**
     * 手动收集指标
     */
    @Operation(summary = "手动收集指标", description = "手动触发系统指标收集", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "收集成功")
    })
    @PostMapping("/collect")
    public ResponseEntity<?> collectMetrics() {
        monitoringService.collectSystemMetrics();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Metrics collected successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * 清理过期指标
     */
    @Operation(summary = "清理过期指标", description = "清理指定天数前的监控指标", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "清理成功")
    })
    @DeleteMapping("/cleanup")
    public ResponseEntity<?> cleanupOldMetrics(
            @Parameter(description = "保留天数", example = "7")
            @RequestParam(defaultValue = "7") int retentionDays) {
        monitoringService.cleanupOldMetrics(retentionDays);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Old metrics cleaned up successfully");
        return ResponseEntity.ok(response);
    }
}
