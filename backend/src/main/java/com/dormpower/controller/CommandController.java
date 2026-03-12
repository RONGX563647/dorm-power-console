package com.dormpower.controller;

import com.dormpower.annotation.AuditLog;
import com.dormpower.annotation.RateLimit;
import com.dormpower.mqtt.MqttBridge;
import com.dormpower.service.CommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 命令控制器
 * 
 * 提供设备控制命令的下发和查询功能，包括：
 * - 单设备命令下发
 * - 批量设备命令下发
 * - 命令历史查询
 * - 命令状态查询
 * 
 * 所有接口都需要Bearer Token认证
 * 
 * @author dormpower team
 * @version 1.0
 */
@RestController
@RequestMapping("/api")
@Tag(name = "设备控制", description = "提供设备控制命令下发的RESTful API接口")
public class CommandController {

    // 命令服务，处理命令的业务逻辑
    @Autowired
    private CommandService commandService;

    // MQTT桥接，用于向设备下发命令
    @Autowired
    private MqttBridge mqttBridge;

    /**
     * 下发控制命令
     * 
     * 向指定设备下发控制命令，支持开关操作和定时功能。
     * 命令下发流程：
     * 1. 创建命令记录，生成唯一命令ID
     * 2. 检查是否存在冲突的待处理命令
     * 3. 通过MQTT向设备下发命令
     * 4. 如果MQTT不可用，将命令状态标记为失败
     * 
     * @param deviceId 设备ID，唯一标识一个设备
     * @param request 命令请求体，包含action、socket等字段
     * @return 命令响应，包含cmdId、stripId和acceptedAt字段
     * @throws RuntimeException 当命令冲突时抛出
     */
    @Operation(summary = "下发控制命令", 
               description = "向指定设备下发控制命令，支持开关操作和定时功能", 
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "命令下发成功"),
            @ApiResponse(responseCode = "409", description = "命令冲突，设备有待处理命令")
    })
    @RateLimit(value = 5.0, type = "device")
    @AuditLog(value = "下发设备控制命令", type = "CONTROL")
    @PostMapping("/strips/{deviceId}/cmd")
    public ResponseEntity<Map<String, Object>> sendCommand(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @PathVariable String deviceId,
            @Parameter(description = "命令请求体", required = true)
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = commandService.sendCommand(deviceId, request);
        
        String cmdId = (String) response.get("cmdId");
        Map<String, Object> payload = new HashMap<>(request);
        payload.put("cmdId", cmdId);

        boolean published = mqttBridge.publishCommand(deviceId, payload);
        if (!published) {
            commandService.updateCommandState(cmdId, "failed", "mqtt unavailable");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 批量下发命令
     */
    @Operation(summary = "批量下发命令", 
               description = "向多个设备同时下发相同的控制命令", 
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "命令下发成功")
    })
    @RateLimit(value = 2.0, type = "batch-command")
    @AuditLog(value = "批量下发设备控制命令", type = "CONTROL")
    @PostMapping("/commands/batch")
    public ResponseEntity<Map<String, Object>> sendBatchCommand(
            @Parameter(description = "批量命令请求", required = true)
            @RequestBody BatchCommandRequest request) {
        Map<String, Object> response = commandService.sendBatchCommand(request.deviceIds(), request.command());

        // 向每个设备下发命令
        Map<String, String> commandIds = (Map<String, String>) response.get("commandIds");
        if (commandIds != null) {
            for (Map.Entry<String, String> entry : commandIds.entrySet()) {
                String deviceId = entry.getKey();
                String cmdId = entry.getValue();
                Map<String, Object> payload = new HashMap<>(request.command());
                payload.put("cmdId", cmdId);
                mqttBridge.publishCommand(deviceId, payload);
            }
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 获取设备命令历史
     */
    @Operation(summary = "获取设备命令历史", 
               description = "获取指定设备的命令执行历史记录", 
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/commands/device/{deviceId}")
    public ResponseEntity<?> getDeviceCommandHistory(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @PathVariable String deviceId) {
        List<com.dormpower.model.CommandRecord> commands = commandService.getCommandsByDeviceId(deviceId);
        return ResponseEntity.ok(commands);
    }

    /**
     * 获取命令详情
     */
    @Operation(summary = "获取命令详情", 
               description = "获取指定命令的详细信息", 
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "命令不存在")
    })
    @GetMapping("/commands/{cmdId}")
    public ResponseEntity<?> getCommandDetail(
            @Parameter(description = "命令ID", required = true, example = "cmd-1234567890")
            @PathVariable String cmdId) {
        Map<String, Object> status = commandService.getCommandStatus(cmdId);
        if (status == null) {
            throw new com.dormpower.exception.ResourceNotFoundException("Command not found");
        }
        return ResponseEntity.ok(status);
    }

    /**
     * 批量命令请求类
     */
    public record BatchCommandRequest(List<String> deviceIds, Map<String, Object> command) {}

}
