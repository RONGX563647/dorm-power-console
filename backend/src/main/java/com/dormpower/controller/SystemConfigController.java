package com.dormpower.controller;

import com.dormpower.annotation.AuditLog;
import com.dormpower.model.SystemConfig;
import com.dormpower.service.SystemConfigService;
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
 * 系统配置控制器
 */
@RestController
@RequestMapping("/api/admin/config")
@Tag(name = "系统配置管理", description = "系统配置参数管理接口")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 获取所有配置
     */
    @Operation(summary = "获取所有配置", description = "获取系统所有配置参数", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public ResponseEntity<?> getAllConfigs() {
        List<SystemConfig> configs = systemConfigService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * 根据分类获取配置
     */
    @Operation(summary = "获取分类配置", description = "根据分类获取配置参数", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getConfigsByCategory(
            @Parameter(description = "配置分类", required = true, example = "email")
            @PathVariable String category) {
        List<SystemConfig> configs = systemConfigService.getConfigsByCategory(category);
        return ResponseEntity.ok(configs);
    }

    /**
     * 获取配置值
     */
    @Operation(summary = "获取配置值", description = "根据key获取配置值", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "配置不存在")
    })
    @GetMapping("/{key}")
    public ResponseEntity<?> getConfigValue(
            @Parameter(description = "配置key", required = true, example = "system.name")
            @PathVariable String key) {
        SystemConfig config = systemConfigService.getConfigByKey(key).orElse(null);
        if (config == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Config not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        return ResponseEntity.ok(config);
    }

    /**
     * 更新配置值
     */
    @Operation(summary = "更新配置值", description = "更新指定配置的值", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "更新失败"),
            @ApiResponse(responseCode = "404", description = "配置不存在")
    })
    @AuditLog(value = "更新系统配置", type = "CONFIG")
    @PutMapping("/{key}")
    public ResponseEntity<?> updateConfigValue(
            @Parameter(description = "配置key", required = true, example = "system.name")
            @PathVariable String key,
            @Parameter(description = "配置值", required = true)
            @RequestBody Map<String, String> request) {
        try {
            String value = request.get("value");
            if (value == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Value is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            SystemConfig config = systemConfigService.updateConfigValue(key, value);
            return ResponseEntity.ok(config);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 批量更新配置
     */
    @Operation(summary = "批量更新配置", description = "批量更新多个配置值", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @AuditLog(value = "批量更新系统配置", type = "CONFIG")
    @PutMapping("/batch")
    public ResponseEntity<?> batchUpdateConfig(
            @Parameter(description = "配置更新请求", required = true)
            @RequestBody Map<String, String> configs) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            try {
                systemConfigService.updateConfigValue(entry.getKey(), entry.getValue());
                successCount++;
            } catch (RuntimeException e) {
                failCount++;
            }
        }
        
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("message", "Batch update completed");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 初始化默认配置
     */
    @Operation(summary = "初始化默认配置", description = "初始化系统默认配置", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "初始化成功")
    })
    @AuditLog(value = "初始化系统配置", type = "CONFIG")
    @PostMapping("/init")
    public ResponseEntity<?> initDefaultConfigs() {
        systemConfigService.initDefaultConfigs();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Default configs initialized successfully");
        return ResponseEntity.ok(response);
    }
}
