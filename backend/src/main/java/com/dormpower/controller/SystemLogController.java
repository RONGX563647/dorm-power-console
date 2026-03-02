package com.dormpower.controller;

import com.dormpower.model.SystemLog;
import com.dormpower.service.SystemLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统日志控制器
 */
@RestController
@RequestMapping("/api/admin/logs")
@Tag(name = "系统日志管理", description = "系统日志查询和管理接口")
public class SystemLogController {

    @Autowired
    private SystemLogService systemLogService;

    /**
     * 获取所有日志
     */
    @Operation(summary = "获取所有日志", description = "获取系统所有日志（分页）", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public ResponseEntity<?> getAllLogs(
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Page<SystemLog> logs = systemLogService.getAllLogs(page, size);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据级别获取日志
     */
    @Operation(summary = "根据级别获取日志", description = "根据日志级别获取日志", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/level/{level}")
    public ResponseEntity<?> getLogsByLevel(
            @Parameter(description = "日志级别", required = true, example = "ERROR")
            @PathVariable String level,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Page<SystemLog> logs = systemLogService.getLogsByLevel(level, page, size);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据类型获取日志
     */
    @Operation(summary = "根据类型获取日志", description = "根据日志类型获取日志", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/type/{type}")
    public ResponseEntity<?> getLogsByType(
            @Parameter(description = "日志类型", required = true, example = "AUTH")
            @PathVariable String type,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Page<SystemLog> logs = systemLogService.getLogsByType(type, page, size);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据用户名获取日志
     */
    @Operation(summary = "根据用户名获取日志", description = "根据用户名获取操作日志", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/user/{username}")
    public ResponseEntity<?> getLogsByUsername(
            @Parameter(description = "用户名", required = true, example = "admin")
            @PathVariable String username,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Page<SystemLog> logs = systemLogService.getLogsByUsername(username, page, size);
        return ResponseEntity.ok(logs);
    }

    /**
     * 搜索日志
     */
    @Operation(summary = "搜索日志", description = "根据关键词搜索日志", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/search")
    public ResponseEntity<?> searchLogs(
            @Parameter(description = "搜索关键词", required = true, example = "error")
            @RequestParam String keyword,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Page<SystemLog> logs = systemLogService.searchLogs(keyword, page, size);
        return ResponseEntity.ok(logs);
    }

    /**
     * 获取日志统计
     */
    @Operation(summary = "获取日志统计", description = "获取日志统计信息", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/statistics")
    public ResponseEntity<?> getLogStatistics(
            @Parameter(description = "统计天数", example = "7")
            @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> stats = systemLogService.getLogStatistics(days);
        return ResponseEntity.ok(stats);
    }

    /**
     * 清理过期日志
     */
    @Operation(summary = "清理过期日志", description = "清理指定天数前的日志", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "清理成功")
    })
    @DeleteMapping("/cleanup")
    public ResponseEntity<?> cleanupOldLogs(
            @Parameter(description = "保留天数", example = "30")
            @RequestParam(defaultValue = "30") int retentionDays) {
        systemLogService.cleanupOldLogs(retentionDays);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Old logs cleaned up successfully");
        return ResponseEntity.ok(response);
    }
}
