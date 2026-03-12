package com.dormpower.controller;

import com.dormpower.service.PowerControlService;
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
 * 断电控制控制器
 */
@RestController
@RequestMapping("/api/power-control")
@Tag(name = "断电控制", description = "自动断电控制管理接口")
public class PowerControlController {

    @Autowired
    private PowerControlService powerControlService;

    @Operation(summary = "手动断电", description = "手动对指定房间执行断电操作", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "断电成功"),
            @ApiResponse(responseCode = "400", description = "房间不存在或无设备")
    })
    @PostMapping("/cutoff/{roomId}")
    public ResponseEntity<?> manualPowerCutoff(
            @Parameter(description = "房间ID", required = true, example = "room_001")
            @PathVariable String roomId,
            @Parameter(description = "操作人", example = "admin")
            @RequestParam(defaultValue = "system") String operator) {
        try {
            boolean success = powerControlService.manualPowerCutoff(roomId, operator);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Power cutoff executed"));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to execute power cutoff"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "恢复供电", description = "恢复指定房间的供电", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "恢复成功"),
            @ApiResponse(responseCode = "400", description = "房间不存在或无设备")
    })
    @PostMapping("/restore/{roomId}")
    public ResponseEntity<?> restorePower(
            @Parameter(description = "房间ID", required = true, example = "room_001")
            @PathVariable String roomId,
            @Parameter(description = "操作人", example = "admin")
            @RequestParam(defaultValue = "system") String operator) {
        try {
            boolean success = powerControlService.restorePower(roomId, operator);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Power restored"));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to restore power"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "获取断电状态", description = "获取指定房间的断电状态", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/status/{roomId}")
    public ResponseEntity<Map<String, Object>> getPowerCutoffStatus(
            @Parameter(description = "房间ID", required = true, example = "room_001")
            @PathVariable String roomId) {
        return ResponseEntity.ok(powerControlService.getPowerCutoffStatus(roomId));
    }

    @Operation(summary = "获取所有断电房间", description = "获取所有处于断电状态的房间列表", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/cutoff-rooms")
    public ResponseEntity<List<Map<String, Object>>> getAllCutoffRooms() {
        return ResponseEntity.ok(powerControlService.getAllCutoffRooms());
    }

    @Operation(summary = "获取欠费房间", description = "获取所有欠费房间列表", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/overdue-rooms")
    public ResponseEntity<List<Map<String, Object>>> getOverdueRooms() {
        return ResponseEntity.ok(powerControlService.getOverdueRooms());
    }
}
