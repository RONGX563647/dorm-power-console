package com.dormpower.controller;

import com.dormpower.annotation.AuditLog;
import com.dormpower.model.DeviceAlert;
import com.dormpower.model.DeviceAlertConfig;
import com.dormpower.service.AlertService;
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
 * 告警控制器
 * 
 * 提供设备告警管理的接口，包括：
 * - 查询设备告警列表
 * - 查询未解决的告警
 * - 解决告警
 * - 查询和更新告警配置
 * - 模拟告警（用于测试）
 * 
 * 所有接口都需要Bearer Token认证。
 * 告警解决和配置更新操作会被记录到审计日志。
 * 
 * @author dormpower team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/alerts")
@Tag(name = "设备告警", description = "提供设备异常告警管理的RESTful API接口")
public class AlertController {

    // 告警服务，处理告警的业务逻辑
    @Autowired
    private AlertService alertService;

    /**
     * 获取设备告警列表
     */
    @Operation(summary = "获取设备告警列表", description = "获取指定设备的告警列表", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<?> getDeviceAlerts(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @PathVariable String deviceId,
            @Parameter(description = "仅获取未解决的告警", required = false, example = "true")
            @RequestParam(required = false, defaultValue = "false") boolean onlyUnresolved) {
        List<DeviceAlert> alerts = alertService.getDeviceAlerts(deviceId, onlyUnresolved);
        return ResponseEntity.ok(alerts);
    }

    /**
     * 获取所有未解决的告警
     */
    @Operation(summary = "获取未解决告警", description = "获取所有未解决的告警", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/unresolved")
    public ResponseEntity<?> getUnresolvedAlerts() {
        List<DeviceAlert> alerts = alertService.getUnresolvedAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * 解决告警
     */
    @Operation(summary = "解决告警", description = "标记告警为已解决", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "解决成功"),
            @ApiResponse(responseCode = "404", description = "告警不存在")
    })
    @AuditLog(value = "解决告警", type = "ALERT")
    @PutMapping("/{alertId}/resolve")
    public ResponseEntity<?> resolveAlert(
            @Parameter(description = "告警ID", required = true, example = "alert_001")
            @PathVariable String alertId) {
        try {
            alertService.resolveAlert(alertId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Alert resolved successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to resolve alert: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取设备告警配置
     */
    @Operation(summary = "获取设备告警配置", description = "获取指定设备的告警配置", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/config/{deviceId}")
    public ResponseEntity<?> getDeviceAlertConfigs(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @PathVariable String deviceId) {
        List<DeviceAlertConfig> configs = alertService.getDeviceAlertConfigs(deviceId);
        return ResponseEntity.ok(configs);
    }

    /**
     * 更新设备告警配置
     */
    @Operation(summary = "更新告警配置", description = "更新设备的告警阈值配置", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @AuditLog(value = "更新告警配置", type = "ALERT")
    @PutMapping("/config/{deviceId}")
    public ResponseEntity<?> updateAlertConfig(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @PathVariable String deviceId,
            @Parameter(description = "告警配置", required = true)
            @RequestBody AlertConfigRequest request) {
        try {
            DeviceAlertConfig config = alertService.updateAlertConfig(
                    deviceId,
                    request.getType(),
                    request.getThresholdMin(),
                    request.getThresholdMax(),
                    request.isEnabled()
            );
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to update alert config: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 告警配置请求类
     */
    public static class AlertConfigRequest {
        private String type;
        private double thresholdMin;
        private double thresholdMax;
        private boolean enabled;

        // Getters and setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public double getThresholdMin() {
            return thresholdMin;
        }

        public void setThresholdMin(double thresholdMin) {
            this.thresholdMin = thresholdMin;
        }

        public double getThresholdMax() {
            return thresholdMax;
        }

        public void setThresholdMax(double thresholdMax) {
            this.thresholdMax = thresholdMax;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 模拟告警
     */
    @Operation(summary = "模拟告警", description = "模拟设备告警用于测试", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "模拟成功")
    })
    @AuditLog(value = "模拟告警", type = "ALERT")
    @PostMapping("/simulate/{deviceId}")
    public ResponseEntity<?> simulateAlarm(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @PathVariable String deviceId,
            @Parameter(description = "告警类型", example = "power")
            @RequestParam(defaultValue = "power") String type,
            @Parameter(description = "模拟值", example = "1500")
            @RequestParam(defaultValue = "1500") double value) {
        try {
            alertService.checkAndGenerateAlert(deviceId, type, value);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Alert simulated successfully");
            response.put("deviceId", deviceId);
            response.put("type", type);
            response.put("value", value);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to simulate alarm: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

}
