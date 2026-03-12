package com.dormpower.controller;

import com.dormpower.dto.MqttSimulatorRequest;
import com.dormpower.dto.MqttSimulatorResponse;
import com.dormpower.dto.MqttSimulatorStatus;
import com.dormpower.service.MqttSimulatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MQTT模拟器控制器
 */
@RestController
@RequestMapping("/api/simulator")
@Tag(name = "MQTT模拟器", description = "MQTT设备模拟器管理API")
public class MqttSimulatorController {

    private final MqttSimulatorService mqttSimulatorService;

    public MqttSimulatorController(MqttSimulatorService mqttSimulatorService) {
        this.mqttSimulatorService = mqttSimulatorService;
    }

    /**
     * 启动MQTT模拟器
     */
    @Operation(summary = "启动MQTT模拟器", description = "启动MQTT设备模拟器，模拟多个设备发送消息", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/start")
    public ResponseEntity<MqttSimulatorResponse> startSimulator(@RequestBody MqttSimulatorRequest request) {
        MqttSimulatorResponse response = mqttSimulatorService.startSimulator(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 停止MQTT模拟器
     */
    @Operation(summary = "停止MQTT模拟器", description = "停止指定的MQTT设备模拟器任务", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/stop/{taskId}")
    public ResponseEntity<MqttSimulatorResponse> stopSimulator(@PathVariable String taskId) {
        MqttSimulatorResponse response = mqttSimulatorService.stopSimulator(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取模拟器状态
     */
    @Operation(summary = "获取模拟器状态", description = "获取指定MQTT设备模拟器任务的状态", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/status/{taskId}")
    public ResponseEntity<MqttSimulatorStatus> getSimulatorStatus(@PathVariable String taskId) {
        MqttSimulatorStatus status = mqttSimulatorService.getSimulatorStatus(taskId);
        return ResponseEntity.ok(status);
    }

    /**
     * 获取所有模拟器任务
     */
    @Operation(summary = "获取所有模拟器任务", description = "获取所有正在运行的MQTT设备模拟器任务", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/tasks")
    public ResponseEntity<List<MqttSimulatorStatus>> getAllSimulatorTasks() {
        List<MqttSimulatorStatus> tasks = mqttSimulatorService.getAllSimulatorTasks();
        return ResponseEntity.ok(tasks);
    }
}
