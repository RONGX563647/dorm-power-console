package com.dormpower.controller;

import com.dormpower.mqtt.MqttBridge;
import com.dormpower.service.CommandService;
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
public class CommandController {

    @Autowired
    private CommandService commandService;

    @Autowired
    private MqttBridge mqttBridge;

    /**
     * 下发控制命令 - 与Python后端保持一致的路由
     * @param deviceId 设备ID
     * @param request 命令请求
     * @return 命令响应
     */
    @PostMapping("/strips/{deviceId}/cmd")
    public ResponseEntity<Map<String, Object>> sendCommand(
            @PathVariable String deviceId,
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
