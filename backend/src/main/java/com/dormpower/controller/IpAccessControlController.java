package com.dormpower.controller;

import com.dormpower.model.IpAccessControl;
import com.dormpower.service.IpAccessControlService;
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
 * IP访问控制控制器
 */
@RestController
@RequestMapping("/api/ip-control")
@Tag(name = "IP访问控制", description = "IP黑白名单管理接口")
public class IpAccessControlController {

    @Autowired
    private IpAccessControlService ipAccessControlService;

    @Operation(summary = "添加IP到白名单", description = "将指定IP添加到白名单", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "添加成功"),
            @ApiResponse(responseCode = "400", description = "IP已存在")
    })
    @PostMapping("/whitelist")
    public ResponseEntity<?> addToWhitelist(
            @Parameter(description = "IP地址", required = true, example = "192.168.1.100")
            @RequestParam String ipAddress,
            @Parameter(description = "描述", example = "管理员IP")
            @RequestParam(required = false) String description,
            @Parameter(description = "操作人", example = "admin")
            @RequestParam(defaultValue = "system") String createdBy) {
        try {
            return ResponseEntity.ok(ipAccessControlService.addToWhitelist(ipAddress, description, createdBy));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "添加IP到黑名单", description = "将指定IP添加到黑名单", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "添加成功"),
            @ApiResponse(responseCode = "400", description = "IP已存在")
    })
    @PostMapping("/blacklist")
    public ResponseEntity<?> addToBlacklist(
            @Parameter(description = "IP地址", required = true, example = "10.0.0.50")
            @RequestParam String ipAddress,
            @Parameter(description = "描述", example = "恶意IP")
            @RequestParam(required = false) String description,
            @Parameter(description = "操作人", example = "admin")
            @RequestParam(defaultValue = "system") String createdBy) {
        try {
            return ResponseEntity.ok(ipAccessControlService.addToBlacklist(ipAddress, description, createdBy));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "移除IP控制", description = "移除指定IP的访问控制", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "移除成功"),
            @ApiResponse(responseCode = "404", description = "IP不存在")
    })
    @DeleteMapping("/{ipAddress}")
    public ResponseEntity<?> removeIpControl(
            @Parameter(description = "IP地址", required = true, example = "192.168.1.100")
            @PathVariable String ipAddress) {
        try {
            ipAccessControlService.removeIpControl(ipAddress);
            return ResponseEntity.ok(Map.of("message", "Removed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "更新IP控制", description = "更新指定IP的控制设置", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "IP不存在")
    })
    @PutMapping("/{ipAddress}")
    public ResponseEntity<?> updateIpControl(
            @Parameter(description = "IP地址", required = true, example = "192.168.1.100")
            @PathVariable String ipAddress,
            @Parameter(description = "是否启用", required = true, example = "true")
            @RequestParam boolean enabled,
            @Parameter(description = "过期时间(Unix时间戳)", example = "1704153600")
            @RequestParam(defaultValue = "0") long expiresAt) {
        try {
            return ResponseEntity.ok(ipAccessControlService.updateIpControl(ipAddress, enabled, expiresAt));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "获取白名单列表", description = "获取所有白名单IP", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/whitelist")
    public ResponseEntity<List<IpAccessControl>> getWhitelist() {
        return ResponseEntity.ok(ipAccessControlService.getWhitelist());
    }

    @Operation(summary = "获取黑名单列表", description = "获取所有黑名单IP", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/blacklist")
    public ResponseEntity<List<IpAccessControl>> getBlacklist() {
        return ResponseEntity.ok(ipAccessControlService.getBlacklist());
    }

    @Operation(summary = "获取所有活跃IP控制", description = "获取所有当前生效的IP控制记录", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/active")
    public ResponseEntity<List<IpAccessControl>> getAllActive() {
        return ResponseEntity.ok(ipAccessControlService.getAllActive());
    }

    @Operation(summary = "检查IP是否允许访问", description = "检查指定IP是否被允许访问系统", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "检查成功")
    })
    @GetMapping("/check/{ipAddress}")
    public ResponseEntity<Map<String, Boolean>> checkIpAllowed(
            @Parameter(description = "IP地址", required = true, example = "192.168.1.100")
            @PathVariable String ipAddress) {
        boolean allowed = ipAccessControlService.isIpAllowed(ipAddress);
        return ResponseEntity.ok(Map.of("allowed", allowed));
    }

    @Operation(summary = "检查IP是否在黑名单", description = "检查指定IP是否在黑名单中", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "检查成功")
    })
    @GetMapping("/blocked/{ipAddress}")
    public ResponseEntity<Map<String, Boolean>> checkIpBlocked(
            @Parameter(description = "IP地址", required = true, example = "10.0.0.50")
            @PathVariable String ipAddress) {
        boolean blocked = ipAccessControlService.isIpBlocked(ipAddress);
        return ResponseEntity.ok(Map.of("blocked", blocked));
    }

    @Operation(summary = "检查IP是否在白名单", description = "检查指定IP是否在白名单中", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "检查成功")
    })
    @GetMapping("/whitelisted/{ipAddress}")
    public ResponseEntity<Map<String, Boolean>> checkIpWhitelisted(
            @Parameter(description = "IP地址", required = true, example = "192.168.1.100")
            @PathVariable String ipAddress) {
        boolean whitelisted = ipAccessControlService.isIpWhitelisted(ipAddress);
        return ResponseEntity.ok(Map.of("whitelisted", whitelisted));
    }

    @Operation(summary = "根据IP获取控制记录", description = "获取指定IP的控制记录详情", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "IP不存在")
    })
    @GetMapping("/{ipAddress}")
    public ResponseEntity<?> getByIpAddress(
            @Parameter(description = "IP地址", required = true, example = "192.168.1.100")
            @PathVariable String ipAddress) {
        return ipAccessControlService.getByIpAddress(ipAddress)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "清理过期记录", description = "清理所有过期的IP控制记录", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "清理成功")
    })
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, String>> cleanupExpired() {
        ipAccessControlService.cleanupExpired();
        return ResponseEntity.ok(Map.of("message", "Expired records cleaned up"));
    }
}
