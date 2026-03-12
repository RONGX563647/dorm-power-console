package com.dormpower.controller;

import com.dormpower.annotation.AuditLog;
import com.dormpower.model.DataBackup;
import com.dormpower.service.DataBackupService;
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
 * 数据备份控制器
 */
@RestController
@RequestMapping("/api/admin/backup")
@Tag(name = "数据备份管理", description = "数据备份和恢复管理接口")
public class DataBackupController {

    @Autowired
    private DataBackupService dataBackupService;

    /**
     * 获取所有备份
     */
    @Operation(summary = "获取所有备份", description = "获取所有数据备份列表", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public ResponseEntity<?> getAllBackups() {
        List<DataBackup> backups = dataBackupService.getAllBackups();
        return ResponseEntity.ok(backups);
    }

    /**
     * 获取最近的备份
     */
    @Operation(summary = "获取最近的备份", description = "获取最近10个数据备份", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentBackups() {
        List<DataBackup> backups = dataBackupService.getRecentBackups();
        return ResponseEntity.ok(backups);
    }

    /**
     * 创建数据库备份
     */
    @Operation(summary = "创建数据库备份", description = "创建数据库备份", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "创建失败")
    })
    @AuditLog(value = "创建数据库备份", type = "BACKUP")
    @PostMapping("/database")
    public ResponseEntity<?> createDatabaseBackup(
            @Parameter(description = "备份描述", example = "每日自动备份")
            @RequestParam(required = false) String description,
            @Parameter(description = "创建者", example = "admin")
            @RequestParam String createdBy) {
        try {
            DataBackup backup = dataBackupService.createDatabaseBackup(description, createdBy);
            return ResponseEntity.ok(backup);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to create backup: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 创建数据导出备份
     */
    @Operation(summary = "创建数据导出备份", description = "创建数据导出备份", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "创建失败")
    })
    @AuditLog(value = "创建数据导出备份", type = "BACKUP")
    @PostMapping("/export")
    public ResponseEntity<?> createDataExportBackup(
            @Parameter(description = "备份描述", example = "数据导出")
            @RequestParam(required = false) String description,
            @Parameter(description = "创建者", example = "admin")
            @RequestParam String createdBy) {
        try {
            DataBackup backup = dataBackupService.createDataExportBackup(description, createdBy);
            return ResponseEntity.ok(backup);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to create backup: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 删除备份
     */
    @Operation(summary = "删除备份", description = "删除指定的数据备份", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "备份不存在")
    })
    @AuditLog(value = "删除数据备份", type = "BACKUP")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBackup(
            @Parameter(description = "备份ID", required = true, example = "1")
            @PathVariable Long id) {
        try {
            dataBackupService.deleteBackup(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Backup deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to delete backup: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 清理过期备份
     */
    @Operation(summary = "清理过期备份", description = "清理指定天数前的备份", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "清理成功")
    })
    @AuditLog(value = "清理过期备份", type = "BACKUP")
    @DeleteMapping("/cleanup")
    public ResponseEntity<?> cleanupOldBackups(
            @Parameter(description = "保留天数", example = "30")
            @RequestParam(defaultValue = "30") int retentionDays) {
        dataBackupService.cleanupOldBackups(retentionDays);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Old backups cleaned up successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取备份统计
     */
    @Operation(summary = "获取备份统计", description = "获取数据备份统计信息", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/statistics")
    public ResponseEntity<?> getBackupStatistics() {
        Map<String, Object> stats = dataBackupService.getBackupStatistics();
        return ResponseEntity.ok(stats);
    }
}
