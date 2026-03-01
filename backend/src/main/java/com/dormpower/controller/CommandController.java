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
import java.util.Map;

/**
 * 命令控制器
 */
@RestController
@RequestMapping("/api")
@Tag(name = "设备控制", description = "设备控制命令下发接口")
public class CommandController {

    @Autowired
    private CommandService commandService;

    @Autowired
    private MqttBridge mqttBridge;

    /**
     * 下发控制命令
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
        try {
            Map<String, Object> response = commandService.sendCommand(deviceId, request);
            
            String cmdId = (String) response.get("cmdId");
            Map<String, Object> payload = new HashMap<>(request);
            payload.put("cmdId", cmdId);

            boolean published = mqttBridge.publishCommand(deviceId, payload);
            if (!published) {
                commandService.updateCommandState(cmdId, "failed", "mqtt unavailable");
            }

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("code", "CMD_CONFLICT");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }

}
