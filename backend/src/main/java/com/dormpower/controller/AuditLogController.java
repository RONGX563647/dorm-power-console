package com.dormpower.controller;

import com.dormpower.model.AuditLog;
import com.dormpower.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 审计日志控制器
 */
@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "审计日志", description = "操作审计日志管理接口")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @Operation(summary = "获取审计日志列表", description = "分页获取所有审计日志", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditLogService.getAllAuditLogs(pageable));
    }

    @Operation(summary = "获取用户审计日志", description = "获取指定用户的操作审计日志", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/user/{username}")
    public ResponseEntity<Page<AuditLog>> getUserAuditLogs(
            @Parameter(description = "用户名", required = true, example = "admin")
            @PathVariable String username,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditLogService.getUserAuditLogs(username, pageable));
    }

    @Operation(summary = "获取模块审计日志", description = "获取指定模块的操作审计日志", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/module/{module}")
    public ResponseEntity<Page<AuditLog>> getModuleAuditLogs(
            @Parameter(description = "模块名称", required = true, example = "DEVICE")
            @PathVariable String module,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditLogService.getModuleAuditLogs(module, pageable));
    }

    @Operation(summary = "获取时间范围内的审计日志", description = "获取指定时间范围内的审计日志", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/range")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByTimeRange(
            @Parameter(description = "开始时间(Unix时间戳)", required = true, example = "1704067200")
            @RequestParam long startTime,
            @Parameter(description = "结束时间(Unix时间戳)", required = true, example = "1704153600")
            @RequestParam long endTime,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditLogService.getAuditLogsByTimeRange(startTime, endTime, pageable));
    }

    @Operation(summary = "获取用户时间范围内的审计日志", description = "获取指定用户在时间范围内的审计日志", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/user/{username}/range")
    public ResponseEntity<Page<AuditLog>> getUserAuditLogsByTimeRange(
            @Parameter(description = "用户名", required = true, example = "admin")
            @PathVariable String username,
            @Parameter(description = "开始时间(Unix时间戳)", required = true, example = "1704067200")
            @RequestParam long startTime,
            @Parameter(description = "结束时间(Unix时间戳)", required = true, example = "1704153600")
            @RequestParam long endTime,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditLogService.getUserAuditLogsByTimeRange(username, startTime, endTime, pageable));
    }

    @Operation(summary = "获取审计统计", description = "获取审计统计数据", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getAuditStatistics(
            @Parameter(description = "统计起始时间(Unix时间戳)", example = "1704067200")
            @RequestParam(defaultValue = "0") long since) {
        if (since == 0) {
            since = System.currentTimeMillis() / 1000 - 604800;
        }
        return ResponseEntity.ok(auditLogService.getAuditStatistics(since));
    }

    @Operation(summary = "清理过期日志", description = "清理指定天数之前的审计日志", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "清理成功")
    })
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Long>> cleanupOldLogs(
            @Parameter(description = "保留天数", required = true, example = "30")
            @RequestParam int retentionDays) {
        long deleted = auditLogService.cleanupOldLogs(retentionDays);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}
