package com.dormpower.controller;

import com.dormpower.model.DeviceFirmware;
import com.dormpower.service.DeviceFirmwareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 设备固件控制器
 */
@RestController
@RequestMapping("/api/firmware")
@Tag(name = "设备固件", description = "设备固件OTA升级管理接口")
public class DeviceFirmwareController {

    @Autowired
    private DeviceFirmwareService firmwareService;

    @Operation(summary = "发起固件升级", description = "为指定设备发起固件升级任务", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "设备已有待处理升级")
    })
    @PostMapping("/upgrade")
    public ResponseEntity<?> initiateUpgrade(
            @Parameter(description = "设备ID", required = true, example = "strip01")
            @RequestParam String deviceId,
            @Parameter(description = "目标版本", required = true, example = "1.2.0")
            @RequestParam String version,
            @Parameter(description = "固件文件路径", required = true, example = "/firmware/strip_v1.2.0.bin")
            @RequestParam String filePath,
            @Parameter(description = "文件校验和", example = "a1b2c3d4e5f6")
            @RequestParam(required = false) String checksum,
            @Parameter(description = "文件大小(字节)", example = "102400")
            @RequestParam(defaultValue = "0") long fileSize,
            @Parameter(description = "发起人", example = "admin")
            @RequestParam(defaultValue = "system") String initiatedBy) {
        try {
            return ResponseEntity.ok(firmwareService.initiateUpgrade(
                    deviceId, version, filePath, checksum, fileSize, initiatedBy));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "发送升级命令", description = "向设备发送固件升级命令", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "发送成功"),
            @ApiResponse(responseCode = "404", description = "固件记录不存在")
    })
    @PostMapping("/{firmwareId}/send")
    public ResponseEntity<?> sendUpgradeCommand(
            @Parameter(description = "固件记录ID", required = true)
            @PathVariable Long firmwareId) {
        boolean success = firmwareService.sendUpgradeCommand(firmwareId);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Upgrade command sent"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Failed to send upgrade command"));
    }

    @Operation(summary = "更新升级进度", description = "更新固件升级进度", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "固件记录不存在")
    })
    @PutMapping("/{firmwareId}/progress")
    public ResponseEntity<?> updateProgress(
            @Parameter(description = "固件记录ID", required = true)
            @PathVariable Long firmwareId,
            @Parameter(description = "进度(0-100)", required = true, example = "50")
            @RequestParam int progress) {
        try {
            return ResponseEntity.ok(firmwareService.updateProgress(firmwareId, progress));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "完成升级", description = "标记固件升级完成", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "固件记录不存在")
    })
    @PostMapping("/{firmwareId}/complete")
    public ResponseEntity<?> completeUpgrade(
            @Parameter(description = "固件记录ID", required = true)
            @PathVariable Long firmwareId,
            @Parameter(description = "是否成功", required = true, example = "true")
            @RequestParam boolean success,
            @Parameter(description = "错误信息", example = "Download failed")
            @RequestParam(required = false) String errorMessage) {
        try {
            return ResponseEntity.ok(firmwareService.completeUpgrade(firmwareId, success, errorMessage));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "取消升级", description = "取消进行中的固件升级", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "取消成功"),
            @ApiResponse(responseCode = "400", description = "无法取消已完成的升级")
    })
    @PostMapping("/{firmwareId}/cancel")
    public ResponseEntity<?> cancelUpgrade(
            @Parameter(description = "固件记录ID", required = true)
            @PathVariable Long firmwareId,
            @Parameter(description = "取消原因", example = "User requested")
            @RequestParam(defaultValue = "User requested") String reason) {
        try {
            return ResponseEntity.ok(firmwareService.cancelUpgrade(firmwareId, reason));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "获取设备固件历史", description = "获取指定设备的固件升级历史", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<DeviceFirmware>> getDeviceFirmwareHistory(
            @Parameter(description = "设备ID", required = true, example = "strip01")
            @PathVariable String deviceId) {
        return ResponseEntity.ok(firmwareService.getDeviceFirmwareHistory(deviceId));
    }

    @Operation(summary = "获取设备当前固件版本", description = "获取设备当前成功安装的固件版本", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "无固件记录")
    })
    @GetMapping("/device/{deviceId}/current")
    public ResponseEntity<?> getCurrentFirmware(
            @Parameter(description = "设备ID", required = true, example = "strip01")
            @PathVariable String deviceId) {
        return firmwareService.getCurrentFirmware(deviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "获取待处理升级", description = "获取所有待处理的固件升级任务", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/pending")
    public ResponseEntity<List<DeviceFirmware>> getPendingUpgrades() {
        return ResponseEntity.ok(firmwareService.getPendingUpgrades());
    }

    @Operation(summary = "获取进行中升级", description = "获取所有进行中的固件升级任务", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/active")
    public ResponseEntity<List<DeviceFirmware>> getActiveUpgrades() {
        return ResponseEntity.ok(firmwareService.getActiveUpgrades());
    }

    @Operation(summary = "获取固件详情", description = "根据ID获取固件升级记录详情", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "记录不存在")
    })
    @GetMapping("/{firmwareId}")
    public ResponseEntity<?> getFirmwareById(
            @Parameter(description = "固件记录ID", required = true)
            @PathVariable Long firmwareId) {
        return firmwareService.getFirmwareById(firmwareId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
