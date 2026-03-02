package com.dormpower.controller;

import com.dormpower.model.DeviceStatusHistory;
import com.dormpower.repository.DeviceStatusHistoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 设备状态历史控制器
 */
@RestController
@RequestMapping("/api/devices")
@Tag(name = "设备状态历史", description = "设备状态变更历史记录查询")
public class DeviceStatusHistoryController {

    @Autowired
    private DeviceStatusHistoryRepository deviceStatusHistoryRepository;

    /**
     * 获取设备状态历史
     */
    @Operation(summary = "获取设备状态历史", description = "获取指定设备的状态变更历史记录", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/{deviceId}/history")
    public ResponseEntity<?> getDeviceStatusHistory(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @PathVariable String deviceId,
            @Parameter(description = "开始时间戳", required = false, example = "1700000000")
            @RequestParam(required = false) Long start,
            @Parameter(description = "结束时间戳", required = false, example = "1700003600")
            @RequestParam(required = false) Long end,
            @Parameter(description = "限制条数", required = false, example = "100")
            @RequestParam(required = false, defaultValue = "100") int limit) {
        List<DeviceStatusHistory> history;
        
        if (start != null && end != null) {
            history = deviceStatusHistoryRepository.findByDeviceIdAndTsBetweenOrderByTsDesc(deviceId, start, end);
        } else {
            history = deviceStatusHistoryRepository.findByDeviceIdOrderByTsDesc(deviceId);
        }
        
        // 限制返回条数
        if (history.size() > limit) {
            history = history.subList(0, limit);
        }
        
        return ResponseEntity.ok(history);
    }

    /**
     * 获取设备状态历史详情
     */
    @Operation(summary = "获取状态历史详情", description = "获取指定状态历史记录的详细信息", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "记录不存在")
    })
    @GetMapping("/history/{historyId}")
    public ResponseEntity<?> getStatusHistoryDetail(
            @Parameter(description = "历史记录ID", required = true, example = "history_001")
            @PathVariable String historyId) {
        DeviceStatusHistory history = deviceStatusHistoryRepository.findById(historyId).orElse(null);
        if (history == null) {
            throw new com.dormpower.exception.ResourceNotFoundException("history not found");
        }
        return ResponseEntity.ok(history);
    }

}
