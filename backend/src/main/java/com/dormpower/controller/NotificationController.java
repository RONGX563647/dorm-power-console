package com.dormpower.controller;

import com.dormpower.model.Notification;
import com.dormpower.model.NotificationPreference;
import com.dormpower.service.EmailService;
import com.dormpower.service.NotificationService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "通知中心", description = "系统通知管理接口")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    /**
     * 获取用户通知列表
     */
    @Operation(summary = "获取用户通知", description = "获取当前用户的通知列表", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public ResponseEntity<?> getUserNotifications(
            @Parameter(description = "用户名", required = true, example = "admin")
            @RequestParam String username,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Page<Notification> notifications = notificationService.getUserNotifications(username, page, size);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 获取用户未读通知
     */
    @Operation(summary = "获取未读通知", description = "获取当前用户的未读通知", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/unread")
    public ResponseEntity<?> getUserUnreadNotifications(
            @Parameter(description = "用户名", required = true, example = "admin")
            @RequestParam String username) {
        List<Notification> notifications = notificationService.getUserUnreadNotifications(username);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 获取未读通知数量
     */
    @Operation(summary = "获取未读数量", description = "获取当前用户的未读通知数量", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/unread/count")
    public ResponseEntity<?> getUserUnreadCount(
            @Parameter(description = "用户名", required = true, example = "admin")
            @RequestParam String username) {
        long count = notificationService.getUserUnreadCount(username);
        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * 标记通知为已读
     */
    @Operation(summary = "标记已读", description = "将指定通知标记为已读", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "标记成功")
    })
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @Parameter(description = "通知ID", required = true, example = "1")
            @PathVariable Long id) {
        notificationService.markAsRead(id);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Notification marked as read");
        return ResponseEntity.ok(response);
    }

    /**
     * 标记所有通知为已读
     */
    @Operation(summary = "标记全部已读", description = "将所有通知标记为已读", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "标记成功")
    })
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(
            @Parameter(description = "用户名", required = true, example = "admin")
            @RequestParam String username) {
        notificationService.markAllAsRead(username);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "All notifications marked as read");
        return ResponseEntity.ok(response);
    }

    /**
     * 删除通知
     */
    @Operation(summary = "删除通知", description = "删除指定的通知", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(
            @Parameter(description = "通知ID", required = true, example = "1")
            @PathVariable Long id) {
        notificationService.deleteNotification(id);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Notification deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取通知统计
     */
    @Operation(summary = "获取通知统计", description = "获取通知统计信息", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/statistics")
    public ResponseEntity<?> getNotificationStatistics(
            @Parameter(description = "用户名", required = true, example = "admin")
            @RequestParam String username) {
        Map<String, Object> stats = notificationService.getNotificationStatistics(username);
        return ResponseEntity.ok(stats);
    }

    /**
     * 发送测试邮件
     */
    @Operation(summary = "发送测试邮件", description = "发送测试邮件", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "发送成功"),
            @ApiResponse(responseCode = "400", description = "发送失败")
    })
    @PostMapping("/email/test")
    public ResponseEntity<?> sendTestEmail(
            @Parameter(description = "收件人邮箱", required = true, example = "test@example.com")
            @RequestParam String to) {
        try {
            emailService.sendSimpleEmail(to, "测试邮件", "这是一封测试邮件，来自宿舍电源管理系统。");
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Test email sent successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to send email: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // ==================== 通知偏好设置 ====================
    
    /**
     * 获取用户通知偏好设置
     */
    @Operation(summary = "获取通知偏好设置", description = "获取当前用户的通知偏好设置", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/preferences")
    public ResponseEntity<?> getUserPreference(
            @Parameter(description = "用户名", required = true, example = "admin")
            @RequestParam String username) {
        NotificationPreference preference = notificationService.getUserPreference(username);
        return ResponseEntity.ok(preference);
    }
    
    /**
     * 更新用户通知偏好设置
     */
    @Operation(summary = "更新通知偏好设置", description = "更新当前用户的通知偏好设置", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PutMapping("/preferences")
    public ResponseEntity<?> updateUserPreference(
            @Parameter(description = "用户名", required = true, example = "admin")
            @RequestParam String username,
            @RequestBody NotificationPreference preference) {
        NotificationPreference updated = notificationService.updateUserPreference(username, preference);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * 重置用户通知偏好设置
     */
    @Operation(summary = "重置通知偏好设置", description = "重置当前用户的通知偏好设置为默认值", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "重置成功")
    })
    @PostMapping("/preferences/reset")
    public ResponseEntity<?> resetUserPreference(
            @Parameter(description = "用户名", required = true, example = "admin")
            @RequestParam String username) {
        NotificationPreference preference = notificationService.resetUserPreference(username);
        return ResponseEntity.ok(preference);
    }
    
    /**
     * 检查通知是否启用
     */
    @Operation(summary = "检查通知是否启用", description = "检查用户是否启用了特定类型的通知", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "检查成功")
    })
    @GetMapping("/preferences/enabled")
    public ResponseEntity<?> isNotificationEnabled(
            @Parameter(description = "用户名", required = true, example = "admin")
            @RequestParam String username,
            @Parameter(description = "通知类型", required = true, example = "EMAIL")
            @RequestParam String type) {
        boolean enabled = notificationService.isNotificationEnabled(username, type);
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", enabled);
        response.put("type", type);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 检查是否在免打扰时段
     */
    @Operation(summary = "检查免打扰时段", description = "检查当前是否在用户的免打扰时段内", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "检查成功")
    })
    @GetMapping("/preferences/quiet-hours")
    public ResponseEntity<?> isInQuietHours(
            @Parameter(description = "用户名", required = true, example = "admin")
            @RequestParam String username) {
        boolean inQuietHours = notificationService.isInQuietHours(username);
        Map<String, Object> response = new HashMap<>();
        response.put("inQuietHours", inQuietHours);
        return ResponseEntity.ok(response);
    }
}
