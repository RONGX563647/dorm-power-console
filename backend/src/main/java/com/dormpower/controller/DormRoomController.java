package com.dormpower.controller;

import com.dormpower.annotation.AuditLog;
import com.dormpower.model.Building;
import com.dormpower.model.DormRoom;
import com.dormpower.service.DormRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 宿舍房间管理控制器
 */
@RestController
@RequestMapping("/api/dorm")
@Tag(name = "宿舍管理", description = "楼栋、房间管理接口")
public class DormRoomController {

    @Autowired
    private DormRoomService dormRoomService;

    // ==================== 楼栋管理 ====================

    /**
     * 获取所有楼栋
     */
    @Operation(summary = "获取楼栋列表", description = "获取所有宿舍楼栋", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/buildings")
    public ResponseEntity<?> getAllBuildings() {
        List<Building> buildings = dormRoomService.getAllBuildings();
        return ResponseEntity.ok(buildings);
    }

    /**
     * 创建楼栋
     */
    @Operation(summary = "创建楼栋", description = "创建新的宿舍楼栋", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "创建失败")
    })
    @AuditLog(value = "创建楼栋", type = "DORM")
    @PostMapping("/buildings")
    public ResponseEntity<?> createBuilding(
            @Parameter(description = "楼栋信息", required = true)
            @RequestBody Building building) {
        try {
            Building created = dormRoomService.createBuilding(building);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to create building: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 更新楼栋
     */
    @Operation(summary = "更新楼栋", description = "更新指定的楼栋信息", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "楼栋不存在")
    })
    @AuditLog(value = "更新楼栋", type = "DORM")
    @PutMapping("/buildings/{id}")
    public ResponseEntity<?> updateBuilding(
            @Parameter(description = "楼栋ID", required = true) @PathVariable String id,
            @Parameter(description = "楼栋信息", required = true) @RequestBody Building building) {
        try {
            Building updated = dormRoomService.updateBuilding(id, building);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to update building: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 删除楼栋
     */
    @Operation(summary = "删除楼栋", description = "删除指定的楼栋", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "楼栋不存在")
    })
    @AuditLog(value = "删除楼栋", type = "DORM")
    @DeleteMapping("/buildings/{id}")
    public ResponseEntity<?> deleteBuilding(
            @Parameter(description = "楼栋ID", required = true) @PathVariable String id) {
        try {
            dormRoomService.deleteBuilding(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Building deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to delete building: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // ==================== 房间管理 ====================

    /**
     * 获取所有房间
     */
    @Operation(summary = "获取房间列表", description = "获取所有宿舍房间", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/rooms")
    public ResponseEntity<?> getAllRooms() {
        List<DormRoom> rooms = dormRoomService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }

    /**
     * 根据楼栋获取房间
     */
    @Operation(summary = "获取楼栋房间", description = "获取指定楼栋的所有房间", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/buildings/{buildingId}/rooms")
    public ResponseEntity<?> getRoomsByBuilding(
            @Parameter(description = "楼栋ID", required = true) @PathVariable String buildingId) {
        List<DormRoom> rooms = dormRoomService.getRoomsByBuilding(buildingId);
        return ResponseEntity.ok(rooms);
    }

    /**
     * 根据楼栋和楼层获取房间
     */
    @Operation(summary = "获取楼层房间", description = "获取指定楼栋和楼层的房间", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/buildings/{buildingId}/floors/{floor}/rooms")
    public ResponseEntity<?> getRoomsByBuildingAndFloor(
            @Parameter(description = "楼栋ID", required = true) @PathVariable String buildingId,
            @Parameter(description = "楼层", required = true) @PathVariable int floor) {
        List<DormRoom> rooms = dormRoomService.getRoomsByBuildingAndFloor(buildingId, floor);
        return ResponseEntity.ok(rooms);
    }

    /**
     * 创建房间
     */
    @Operation(summary = "创建房间", description = "创建新的宿舍房间", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "创建失败")
    })
    @AuditLog(value = "创建房间", type = "DORM")
    @PostMapping("/rooms")
    public ResponseEntity<?> createRoom(
            @Parameter(description = "房间信息", required = true)
            @RequestBody DormRoom room) {
        try {
            DormRoom created = dormRoomService.createRoom(room);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to create room: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 更新房间
     */
    @Operation(summary = "更新房间", description = "更新指定的房间信息", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "房间不存在")
    })
    @AuditLog(value = "更新房间", type = "DORM")
    @PutMapping("/rooms/{id}")
    public ResponseEntity<?> updateRoom(
            @Parameter(description = "房间ID", required = true) @PathVariable String id,
            @Parameter(description = "房间信息", required = true) @RequestBody DormRoom room) {
        try {
            DormRoom updated = dormRoomService.updateRoom(id, room);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to update room: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 删除房间
     */
    @Operation(summary = "删除房间", description = "删除指定的房间", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "房间不存在")
    })
    @AuditLog(value = "删除房间", type = "DORM")
    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<?> deleteRoom(
            @Parameter(description = "房间ID", required = true) @PathVariable String id) {
        try {
            dormRoomService.deleteRoom(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Room deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to delete room: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 入住
     */
    @Operation(summary = "房间入住", description = "办理房间入住", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "入住成功"),
            @ApiResponse(responseCode = "400", description = "入住失败")
    })
    @AuditLog(value = "房间入住", type = "DORM")
    @PostMapping("/rooms/{roomId}/check-in")
    public ResponseEntity<?> checkIn(
            @Parameter(description = "房间ID", required = true) @PathVariable String roomId,
            @Parameter(description = "入住人数", required = true) @RequestParam int occupantCount) {
        try {
            DormRoom room = dormRoomService.checkIn(roomId, occupantCount);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to check in: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 退宿
     */
    @Operation(summary = "房间退宿", description = "办理房间退宿", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "退宿成功"),
            @ApiResponse(responseCode = "400", description = "退宿失败")
    })
    @AuditLog(value = "房间退宿", type = "DORM")
    @PostMapping("/rooms/{roomId}/check-out")
    public ResponseEntity<?> checkOut(
            @Parameter(description = "房间ID", required = true) @PathVariable String roomId) {
        try {
            DormRoom room = dormRoomService.checkOut(roomId);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to check out: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取房间统计
     */
    @Operation(summary = "获取房间统计", description = "获取房间入住率等统计数据", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/rooms/statistics")
    public ResponseEntity<?> getRoomStatistics() {
        Map<String, Object> stats = dormRoomService.getRoomStatistics();
        return ResponseEntity.ok(stats);
    }
}
