package com.dormpower.controller;

import com.dormpower.model.Device;
import com.dormpower.model.StripStatus;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.StripStatusRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备控制器
 */
@RestController
@RequestMapping("/api")
@Tag(name = "设备管理", description = "设备查询和状态管理接口")
public class DeviceController {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private StripStatusRepository stripStatusRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final long ONLINE_TIMEOUT_SECONDS = 60;

    /**
     * 获取设备列表
     */
    @Operation(summary = "获取设备列表", description = "获取所有设备的列表信息", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/devices")
    public List<Map<String, Object>> getDevices() {
        List<Device> devices = deviceRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        
        long now = System.currentTimeMillis() / 1000;
        
        for (Device d : devices) {
            boolean isOnline = d.isOnline() && (now - d.getLastSeenTs()) < ONLINE_TIMEOUT_SECONDS;
            
            Map<String, Object> deviceMap = new HashMap<>();
            deviceMap.put("id", d.getId());
            deviceMap.put("name", d.getName());
            deviceMap.put("room", d.getRoom());
            deviceMap.put("online", isOnline);
            deviceMap.put("lastSeen", Instant.ofEpochSecond(d.getLastSeenTs()).toString());
            result.add(deviceMap);
        }
        
        return result;
    }

    /**
     * 获取设备状态
     */
    @Operation(summary = "获取设备状态", description = "获取指定设备的详细状态信息", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "设备不存在")
    })
    @GetMapping("/devices/{deviceId}/status")
    public ResponseEntity<?> getDeviceStatus(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @PathVariable String deviceId) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        StripStatus status = stripStatusRepository.findByDeviceId(deviceId);
        
        if (device == null || status == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("code", "NOT_FOUND");
            error.put("message", "device not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        long now = System.currentTimeMillis() / 1000;
        boolean isOnline = device.isOnline() && status.isOnline() && (now - device.getLastSeenTs()) < ONLINE_TIMEOUT_SECONDS;

        List<Map<String, Object>> sockets = new ArrayList<>();
        try {
            List<?> socketList = objectMapper.readValue(status.getSocketsJson(), List.class);
            for (Object s : socketList) {
                if (s instanceof Map) {
                    Map<String, Object> socket = new HashMap<>();
                    Map<?, ?> sMap = (Map<?, ?>) s;
                    socket.put("id", sMap.get("id"));
                    socket.put("on", sMap.get("on"));
                    socket.put("power_w", sMap.get("power_w"));
                    sockets.add(socket);
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }

        Map<String, Object> response = new HashMap<>();
        response.put("ts", status.getTs());
        response.put("online", isOnline);
        response.put("total_power_w", status.getTotalPowerW());
        response.put("voltage_v", status.getVoltageV());
        response.put("current_a", status.getCurrentA());
        response.put("sockets", sockets);
        
        return ResponseEntity.ok(response);
    }

}
