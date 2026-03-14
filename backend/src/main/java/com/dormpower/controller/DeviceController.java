package com.dormpower.controller;

import com.dormpower.annotation.AuditLog;
import com.dormpower.annotation.RateLimit;
import com.dormpower.dto.DeviceRegistrationRequest;
import com.dormpower.model.Device;
import com.dormpower.model.StripStatus;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.StripStatusRepository;
import com.dormpower.service.DeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    private DeviceService deviceService;

    private static final long ONLINE_TIMEOUT_SECONDS = 60;

    /**
     * 获取设备列表
     * 
     * 查询所有注册的设备信息，包括设备ID、名称、房间号和在线状态。
     * 设备的在线状态基于最后心跳时间判断，如果超过ONLINE_TIMEOUT_SECONDS秒未收到心跳，
     * 即使数据库中标记为在线，也会被判定为离线。
     * 
     * @return 设备列表，每个设备包含id、name、room、online和lastSeen字段
     */
    @Operation(
        summary = "获取设备列表",
        description = "查询所有注册的设备信息，包括设备ID、名称、房间号和在线状态",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "获取成功",
            content = @Content(schema = @Schema(implementation = Device.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "未授权，需要提供有效的Bearer Token"
        )
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
     * 
     * 查询指定设备的详细状态信息，包括总功率、电压、电流和各插座状态。
     * 设备的在线状态需要同时满足三个条件：
     * 1. Device表中标记为在线
     * 2. StripStatus表中标记为在线
     * 3. 最后心跳时间在超时范围内
     * 
     * @param deviceId 设备ID，唯一标识一个设备
     * @return 设备状态信息，包含ts、online、total_power_w、voltage_v、current_a和sockets字段
     * @throws ResourceNotFoundException 当设备或设备状态不存在时抛出
     */
    @Operation(
        summary = "获取设备状态",
        description = "获取指定设备的详细状态信息，包括总功率、电压、电流和各插座状态",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "获取成功",
            content = @Content(schema = @Schema(implementation = StripStatus.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "设备不存在或设备状态不存在"
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "未授权，需要提供有效的Bearer Token"
        )
    })
    @GetMapping("/devices/{deviceId}/status")
    public ResponseEntity<?> getDeviceStatus(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @PathVariable String deviceId) {
        // 查询设备基本信息
        Device device = deviceRepository.findById(deviceId).orElse(null);
        // 查询设备状态信息
        StripStatus status = stripStatusRepository.findByDeviceId(deviceId);
        
        // 检查设备和状态是否存在
        if (device == null || status == null) {
            throw new com.dormpower.exception.ResourceNotFoundException("device not found");
        }

        // 获取当前时间戳（秒）
        long now = System.currentTimeMillis() / 1000;
        // 判断设备是否在线：需要同时满足三个条件
        boolean isOnline = device.isOnline() && status.isOnline() && (now - device.getLastSeenTs()) < ONLINE_TIMEOUT_SECONDS;

        // 解析插座状态JSON
        List<Map<String, Object>> sockets = new ArrayList<>();
        try {
            List<?> socketList = objectMapper.readValue(status.getSocketsJson(), List.class);
            // 使用 instanceof 模式匹配简化类型检查和转换
            for (Object s : socketList) {
                if (s instanceof Map<?, ?> sMap) {
                    Map<String, Object> socket = new HashMap<>();
                    // 提取插座ID、开关状态和功率
                    socket.put("id", sMap.get("id"));
                    socket.put("on", sMap.get("on"));
                    socket.put("power_w", sMap.get("power_w"));
                    sockets.add(socket);
                }
            }
        } catch (Exception e) {
            // JSON解析失败时忽略错误，返回空插座列表
            // 这种情况可能是设备上报的数据格式异常
        }

        // 构建响应数据
        Map<String, Object> response = new HashMap<>();
        response.put("ts", status.getTs());
        response.put("online", isOnline);
        response.put("total_power_w", status.getTotalPowerW());
        response.put("voltage_v", status.getVoltageV());
        response.put("current_a", status.getCurrentA());
        response.put("sockets", sockets);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 创建设备
     *
     * 创建新的设备记录，包括设备ID、名称和房间号等信息。
     * 新创建的设备初始状态为离线，创建时间和最后心跳时间设置为当前时间。
     *
     * 该接口有速率限制，每秒最多允许2次请求。
     * 所有创建操作都会被记录到审计日志。
     *
     * @param request 设备注册信息，包含id、name和room字段
     * @return 创建成功的设备信息
     * @throws BusinessException 当设备ID已存在时抛出
     */
    @Operation(
        summary = "创建设备",
        description = "创建新的设备记录，包括设备ID、名称和房间号等信息",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "创建成功",
            content = @Content(schema = @Schema(implementation = Device.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "创建失败，设备ID已存在或参数错误"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "未授权，需要提供有效的Bearer Token"
        ),
        @ApiResponse(
            responseCode = "429",
            description = "请求过于频繁，超过速率限制"
        )
    })
    @RateLimit(value = 2.0, type = "create-device")
    @AuditLog(value = "创建设备", type = "DEVICE")
    @PostMapping("/devices")
    public ResponseEntity<?> createDevice(
            @Parameter(description = "设备注册信息", required = true)
            @Valid @RequestBody DeviceRegistrationRequest request) {
        Device savedDevice = deviceService.registerDevice(
                request.getId(),
                request.getName(),
                request.getRoom()
        );
        return ResponseEntity.ok(savedDevice);
    }

    /**
     * 更新设备
     * 
     * 更新指定设备的信息，包括设备名称、房间号和在线状态。
     * 只更新提供的字段，未提供的字段保持不变。
     * 
     * 所有更新操作都会被记录到审计日志。
     * 
     * @param deviceId 设备ID，唯一标识一个设备
     * @param device 设备信息，包含需要更新的字段
     * @return 更新后的设备信息
     * @throws ResourceNotFoundException 当设备不存在时抛出
     */
    @Operation(
        summary = "更新设备",
        description = "更新指定设备的信息，包括设备名称、房间号和在线状态",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "更新成功",
            content = @Content(schema = @Schema(implementation = Device.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "设备不存在"
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "更新失败，参数错误"
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "未授权，需要提供有效的Bearer Token"
        )
    })
    @AuditLog(value = "更新设备", type = "DEVICE")
    @PutMapping("/devices/{deviceId}")
    public ResponseEntity<?> updateDevice(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @PathVariable String deviceId,
            @Parameter(description = "设备信息", required = true)
            @Valid @RequestBody Device device) {
        try {
            Device existingDevice = deviceRepository.findById(deviceId).orElse(null);
            if (existingDevice == null) {
                throw new com.dormpower.exception.ResourceNotFoundException("device not found");
            }
            
            // 更新设备信息
            existingDevice.setName(device.getName());
            existingDevice.setRoom(device.getRoom());
            existingDevice.setOnline(device.isOnline());
            
            Device updatedDevice = deviceRepository.save(existingDevice);
            return ResponseEntity.ok(updatedDevice);
        } catch (com.dormpower.exception.ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to update device: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 删除设备
     */
    @Operation(summary = "删除设备", description = "删除指定的设备", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "设备不存在")
    })
    @AuditLog(value = "删除设备", type = "DEVICE")
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<?> deleteDevice(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @PathVariable String deviceId) {
        try {
            Device device = deviceRepository.findById(deviceId).orElse(null);
            if (device == null) {
                throw new com.dormpower.exception.ResourceNotFoundException("device not found");
            }
            
            deviceRepository.delete(device);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Device deleted successfully");
            return ResponseEntity.ok(response);
        } catch (com.dormpower.exception.ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to delete device: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
   * 批量删除设备
   */
  @Operation(summary = "批量删除设备", description = "批量删除指定的多个设备", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "批量删除成功"),
          @ApiResponse(responseCode = "400", description = "删除失败")
  })
  @AuditLog(value = "批量删除设备", type = "DEVICE")
  @DeleteMapping("/devices/batch")
  public ResponseEntity<?> batchDeleteDevices(
          @Parameter(description = "设备ID列表", required = true, example = "[\"device_001\", \"device_002\"]")
          @RequestBody List<String> deviceIds) {
        try {
            if (deviceIds == null || deviceIds.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Device ID list cannot be empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // 批量删除设备，只删除存在的设备
            List<Device> existingDevices = deviceRepository.findAllById(deviceIds);
            List<String> existingDeviceIds = existingDevices.stream()
                    .map(Device::getId)
                    .toList();
            
            if (!existingDeviceIds.isEmpty()) {
                deviceRepository.deleteAllById(existingDeviceIds);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Devices deleted successfully");
            response.put("count", existingDeviceIds.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to delete devices: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取指定房间的设备列表
     */
    @Operation(summary = "获取房间设备列表", description = "获取指定房间的设备列表", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/devices/room/{room}")
    public ResponseEntity<?> getDevicesByRoom(
            @Parameter(description = "房间ID", required = true, example = "A-302")
            @PathVariable String room) {
        List<Device> devices = deviceRepository.findByRoom(room);
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
        
        return ResponseEntity.ok(result);
    }

}
