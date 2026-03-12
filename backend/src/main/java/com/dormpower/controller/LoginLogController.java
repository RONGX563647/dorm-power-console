package com.dormpower.controller;

import com.dormpower.model.LoginLog;
import com.dormpower.service.LoginLogService;
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
 * 登录日志控制器
 */
@RestController
@RequestMapping("/api/login-logs")
@Tag(name = "登录日志", description = "登录日志管理接口")
public class LoginLogController {

    @Autowired
    private LoginLogService loginLogService;

    @Operation(summary = "获取登录日志列表", description = "分页获取所有登录日志", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public ResponseEntity<Page<LoginLog>> getLoginLogs(
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(loginLogService.getAllLoginLogs(pageable));
    }

    @Operation(summary = "获取用户登录历史", description = "获取指定用户的登录历史记录", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/user/{username}")
    public ResponseEntity<Page<LoginLog>> getUserLoginHistory(
            @Parameter(description = "用户名", required = true, example = "admin")
            @PathVariable String username,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(loginLogService.getUserLoginHistory(username, pageable));
    }

    @Operation(summary = "获取时间范围内的登录日志", description = "获取指定时间范围内的登录日志", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/range")
    public ResponseEntity<Page<LoginLog>> getLoginLogsByTimeRange(
            @Parameter(description = "开始时间(Unix时间戳)", required = true, example = "1704067200")
            @RequestParam long startTime,
            @Parameter(description = "结束时间(Unix时间戳)", required = true, example = "1704153600")
            @RequestParam long endTime,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(loginLogService.getLoginLogsByTimeRange(startTime, endTime, pageable));
    }

    @Operation(summary = "获取登录统计", description = "获取登录统计数据", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getLoginStatistics(
            @Parameter(description = "统计起始时间(Unix时间戳)", example = "1704067200")
            @RequestParam(defaultValue = "0") long since) {
        if (since == 0) {
            since = System.currentTimeMillis() / 1000 - 604800;
        }
        return ResponseEntity.ok(loginLogService.getLoginStatistics(since));
    }

    @Operation(summary = "检查账户锁定状态", description = "检查指定账户是否被锁定", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/locked/{username}")
    public ResponseEntity<Map<String, Boolean>> checkAccountLocked(
            @Parameter(description = "用户名", required = true, example = "admin")
            @PathVariable String username) {
        boolean locked = loginLogService.isAccountLocked(username);
        return ResponseEntity.ok(Map.of("locked", locked));
    }

    @Operation(summary = "检查IP封禁状态", description = "检查指定IP是否被封禁", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/blocked-ip/{ipAddress}")
    public ResponseEntity<Map<String, Boolean>> checkIpBlocked(
            @Parameter(description = "IP地址", required = true, example = "192.168.1.1")
            @PathVariable String ipAddress) {
        boolean blocked = loginLogService.isIpBlocked(ipAddress);
        return ResponseEntity.ok(Map.of("blocked", blocked));
    }

    @Operation(summary = "获取最近登录记录", description = "获取用户最近一次成功登录记录", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/last-login/{username}")
    public ResponseEntity<?> getLastLogin(
            @Parameter(description = "用户名", required = true, example = "admin")
            @PathVariable String username) {
        LoginLog lastLogin = loginLogService.getLastLogin(username);
        if (lastLogin != null) {
            return ResponseEntity.ok(lastLogin);
        }
        return ResponseEntity.ok(Map.of("message", "No previous login found"));
    }

    @Operation(summary = "获取在线用户数", description = "获取当前在线用户数量", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/active-sessions")
    public ResponseEntity<Map<String, Long>> getActiveSessionCount() {
        long count = loginLogService.getActiveSessionCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @Operation(summary = "清理过期日志", description = "清理指定天数之前的登录日志", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "清理成功")
    })
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Long>> cleanupOldLogs(
            @Parameter(description = "保留天数", required = true, example = "30")
            @RequestParam int retentionDays) {
        long deleted = loginLogService.cleanupOldLogs(retentionDays);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}
