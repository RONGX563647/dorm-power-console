package com.dormpower.controller;

import com.dormpower.annotation.AuditLog;
import com.dormpower.annotation.RateLimit;
import com.dormpower.model.DeviceGroup;
import com.dormpower.model.DeviceGroupMapping;
import com.dormpower.repository.DeviceGroupMappingRepository;
import com.dormpower.repository.DeviceGroupRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 设备分组控制器
 */
@RestController
@RequestMapping("/api/groups")
@Tag(name = "设备分组管理", description = "设备分组的CRUD操作和关联管理")
public class DeviceGroupController {

    @Autowired
    private DeviceGroupRepository deviceGroupRepository;

    @Autowired
    private DeviceGroupMappingRepository deviceGroupMappingRepository;

    /**
     * 获取分组列表
     */
    @Operation(summary = "获取分组列表", description = "获取所有设备分组的列表", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public ResponseEntity<?> getGroups(
            @Parameter(description = "分组类型", required = false, example = "room")
            @RequestParam(required = false) String type) {
        List<DeviceGroup> groups;
        if (type != null) {
            groups = deviceGroupRepository.findByType(type);
        } else {
            groups = deviceGroupRepository.findAll();
        }
        return ResponseEntity.ok(groups);
    }

    /**
     * 获取分组详情
     */
    @Operation(summary = "获取分组详情", description = "获取指定分组的详细信息", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "分组不存在")
    })
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroup(
            @Parameter(description = "分组ID", required = true, example = "group_001")
            @PathVariable String groupId) {
        DeviceGroup group = deviceGroupRepository.findById(groupId).orElse(null);
        if (group == null) {
            throw new com.dormpower.exception.ResourceNotFoundException("group not found");
        }
        return ResponseEntity.ok(group);
    }

    /**
     * 创建分组
     */
    @Operation(summary = "创建分组", description = "创建新的设备分组", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "创建失败")
    })
    @RateLimit(value = 2.0, type = "create-group")
    @AuditLog(value = "创建设备分组", type = "DEVICE")
    @PostMapping
    public ResponseEntity<?> createGroup(
            @Parameter(description = "分组信息", required = true)
            @Valid @RequestBody DeviceGroup group) {
        try {
            // 生成ID和设置创建时间
            if (group.getId() == null) {
                group.setId("group_" + UUID.randomUUID().toString().substring(0, 8));
            }
            group.setCreatedAt(System.currentTimeMillis() / 1000);
            
            DeviceGroup savedGroup = deviceGroupRepository.save(group);
            return ResponseEntity.ok(savedGroup);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to create group: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 更新分组
     */
    @Operation(summary = "更新分组", description = "更新指定分组的信息", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "分组不存在"),
            @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @AuditLog(value = "更新设备分组", type = "DEVICE")
    @PutMapping("/{groupId}")
    public ResponseEntity<?> updateGroup(
            @Parameter(description = "分组ID", required = true, example = "group_001")
            @PathVariable String groupId,
            @Parameter(description = "分组信息", required = true)
            @Valid @RequestBody DeviceGroup group) {
        try {
            DeviceGroup existingGroup = deviceGroupRepository.findById(groupId).orElse(null);
            if (existingGroup == null) {
                throw new com.dormpower.exception.ResourceNotFoundException("group not found");
            }
            
            // 更新分组信息
            existingGroup.setName(group.getName());
            existingGroup.setType(group.getType());
            existingGroup.setParentId(group.getParentId());
            
            DeviceGroup updatedGroup = deviceGroupRepository.save(existingGroup);
            return ResponseEntity.ok(updatedGroup);
        } catch (com.dormpower.exception.ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to update group: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 删除分组
     */
    @Operation(summary = "删除分组", description = "删除指定的设备分组", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "分组不存在")
    })
    @AuditLog(value = "删除设备分组", type = "DEVICE")
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(
            @Parameter(description = "分组ID", required = true, example = "group_001")
            @PathVariable String groupId) {
        try {
            DeviceGroup group = deviceGroupRepository.findById(groupId).orElse(null);
            if (group == null) {
                throw new com.dormpower.exception.ResourceNotFoundException("group not found");
            }
            
            // 删除分组关联
            deviceGroupMappingRepository.deleteByGroupId(groupId);
            // 删除分组
            deviceGroupRepository.delete(group);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Group deleted successfully");
            return ResponseEntity.ok(response);
        } catch (com.dormpower.exception.ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to delete group: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取分组下的设备列表
     */
    @Operation(summary = "获取分组设备列表", description = "获取指定分组下的设备列表", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/{groupId}/devices")
    public ResponseEntity<?> getGroupDevices(
            @Parameter(description = "分组ID", required = true, example = "group_001")
            @PathVariable String groupId) {
        List<DeviceGroupMapping> mappings = deviceGroupMappingRepository.findByGroupId(groupId);
        List<String> deviceIds = new ArrayList<>();
        for (DeviceGroupMapping mapping : mappings) {
            deviceIds.add(mapping.getDeviceId());
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("deviceIds", deviceIds);
        response.put("count", deviceIds.size());
        return ResponseEntity.ok(response);
    }

    /**
     * 向分组添加设备
     */
    @Operation(summary = "向分组添加设备", description = "向指定分组添加设备", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "添加成功"),
            @ApiResponse(responseCode = "400", description = "添加失败")
    })
    @AuditLog(value = "向分组添加设备", type = "DEVICE")
    @PostMapping("/{groupId}/devices")
    public ResponseEntity<?> addDeviceToGroup(
            @Parameter(description = "分组ID", required = true, example = "group_001")
            @PathVariable String groupId,
            @Parameter(description = "设备ID列表", required = true)
            @RequestBody List<String> deviceIds) {
        try {
            List<DeviceGroupMapping> mappings = new ArrayList<>();
            long now = System.currentTimeMillis() / 1000;
            
            for (String deviceId : deviceIds) {
                // 检查是否已存在
                List<DeviceGroupMapping> existing = deviceGroupMappingRepository.findByDeviceId(deviceId);
                boolean alreadyExists = existing.stream().anyMatch(m -> m.getGroupId().equals(groupId));
                if (!alreadyExists) {
                    DeviceGroupMapping mapping = new DeviceGroupMapping();
                    mapping.setId("mapping_" + UUID.randomUUID().toString().substring(0, 8));
                    mapping.setDeviceId(deviceId);
                    mapping.setGroupId(groupId);
                    mapping.setCreatedAt(now);
                    mappings.add(mapping);
                }
            }
            
            if (!mappings.isEmpty()) {
                deviceGroupMappingRepository.saveAll(mappings);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Devices added to group successfully");
            response.put("count", mappings.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to add devices to group: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 从分组移除设备
     */
    @Operation(summary = "从分组移除设备", description = "从指定分组移除设备", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "移除成功"),
            @ApiResponse(responseCode = "400", description = "移除失败")
    })
    @AuditLog(value = "从分组移除设备", type = "DEVICE")
    @DeleteMapping("/{groupId}/devices/{deviceId}")
    public ResponseEntity<?> removeDeviceFromGroup(
            @Parameter(description = "分组ID", required = true, example = "group_001")
            @PathVariable String groupId,
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @PathVariable String deviceId) {
        try {
            List<DeviceGroupMapping> mappings = deviceGroupMappingRepository.findByDeviceId(deviceId);
            for (DeviceGroupMapping mapping : mappings) {
                if (mapping.getGroupId().equals(groupId)) {
                    deviceGroupMappingRepository.delete(mapping);
                    break;
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Device removed from group successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to remove device from group: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取设备所属的分组列表
     */
    @Operation(summary = "获取设备分组列表", description = "获取指定设备所属的分组列表", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<?> getDeviceGroups(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @PathVariable String deviceId) {
        List<DeviceGroupMapping> mappings = deviceGroupMappingRepository.findByDeviceId(deviceId);
        List<DeviceGroup> groups = new ArrayList<>();
        
        for (DeviceGroupMapping mapping : mappings) {
            DeviceGroup group = deviceGroupRepository.findById(mapping.getGroupId()).orElse(null);
            if (group != null) {
                groups.add(group);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("groups", groups);
        response.put("count", groups.size());
        return ResponseEntity.ok(response);
    }

}
