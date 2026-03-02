package com.dormpower.controller;

import com.dormpower.annotation.AuditLog;
import com.dormpower.model.ScheduledTask;
import com.dormpower.service.ScheduledTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务控制器
 */
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "定时任务", description = "设备定时开关任务管理")
public class ScheduledTaskController {

    @Autowired
    private ScheduledTaskService scheduledTaskService;

    /**
     * 创建定时任务
     */
    @Operation(summary = "创建定时任务", description = "创建设备的定时开关任务", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "创建失败")
    })
    @AuditLog(value = "创建定时任务", type = "TASK")
    @PostMapping
    public ResponseEntity<?> createTask(
            @Parameter(description = "定时任务信息", required = true)
            @RequestBody TaskRequest request) {
        try {
            ScheduledTask task = scheduledTaskService.createTask(
                    request.getDeviceId(),
                    request.getType(),
                    request.getSocketId(),
                    request.getScheduledTime(),
                    request.getCronExpression(),
                    request.isRecurring()
            );
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to create task: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取设备的定时任务列表
     */
    @Operation(summary = "获取设备任务列表", description = "获取指定设备的定时任务列表", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<?> getDeviceTasks(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @PathVariable String deviceId) {
        List<ScheduledTask> tasks = scheduledTaskService.getDeviceTasks(deviceId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取所有启用的定时任务
     */
    @Operation(summary = "获取所有任务", description = "获取所有启用的定时任务", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public ResponseEntity<?> getAllTasks() {
        List<ScheduledTask> tasks = scheduledTaskService.getAllEnabledTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * 更新定时任务
     */
    @Operation(summary = "更新定时任务", description = "更新指定的定时任务", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "任务不存在")
    })
    @AuditLog(value = "更新定时任务", type = "TASK")
    @PutMapping("/{taskId}")
    public ResponseEntity<?> updateTask(
            @Parameter(description = "任务ID", required = true, example = "task_001")
            @PathVariable String taskId,
            @Parameter(description = "任务更新信息", required = true)
            @RequestBody Map<String, Object> updates) {
        try {
            ScheduledTask task = scheduledTaskService.updateTask(taskId, updates);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to update task: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 删除定时任务
     */
    @Operation(summary = "删除定时任务", description = "删除指定的定时任务", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "任务不存在")
    })
    @AuditLog(value = "删除定时任务", type = "TASK")
    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(
            @Parameter(description = "任务ID", required = true, example = "task_001")
            @PathVariable String taskId) {
        try {
            scheduledTaskService.deleteTask(taskId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Task deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to delete task: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 启用/禁用定时任务
     */
    @Operation(summary = "切换任务状态", description = "启用或禁用指定的定时任务", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "404", description = "任务不存在")
    })
    @AuditLog(value = "切换任务状态", type = "TASK")
    @PutMapping("/{taskId}/toggle")
    public ResponseEntity<?> toggleTask(
            @Parameter(description = "任务ID", required = true, example = "task_001")
            @PathVariable String taskId,
            @Parameter(description = "是否启用", required = true, example = "true")
            @RequestParam boolean enabled) {
        try {
            ScheduledTask task = scheduledTaskService.toggleTask(taskId, enabled);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to toggle task: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 任务请求类
     */
    public static class TaskRequest {
        private String deviceId;
        private String type;
        private int socketId;
        private long scheduledTime;
        private String cronExpression;
        private boolean recurring;

        // Getters and setters
        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getSocketId() {
            return socketId;
        }

        public void setSocketId(int socketId) {
            this.socketId = socketId;
        }

        public long getScheduledTime() {
            return scheduledTime;
        }

        public void setScheduledTime(long scheduledTime) {
            this.scheduledTime = scheduledTime;
        }

        public String getCronExpression() {
            return cronExpression;
        }

        public void setCronExpression(String cronExpression) {
            this.cronExpression = cronExpression;
        }

        public boolean isRecurring() {
            return recurring;
        }

        public void setRecurring(boolean recurring) {
            this.recurring = recurring;
        }
    }

}
