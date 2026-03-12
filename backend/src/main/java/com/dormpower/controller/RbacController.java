package com.dormpower.controller;

import com.dormpower.annotation.AuditLog;
import com.dormpower.annotation.RateLimit;
import com.dormpower.model.Permission;
import com.dormpower.model.Resource;
import com.dormpower.model.Role;
import com.dormpower.service.RbacService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RBAC管理控制器
 */
@RestController
@RequestMapping("/api/rbac")
@Tag(name = "RBAC管理", description = "角色、权限、资源管理接口")
public class RbacController {

    @Autowired
    private RbacService rbacService;

    // ==================== 角色管理 ====================

    /**
     * 获取角色列表
     */
    @Operation(
            summary = "获取角色列表",
            description = "分页获取所有角色信息",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/roles")
    public ResponseEntity<?> getRoles(
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Role> roles = rbacService.getAllRoles(PageRequest.of(page, size));
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to get roles: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取启用的角色
     */
    @Operation(
            summary = "获取启用的角色",
            description = "获取所有启用状态的角色列表",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/roles/enabled")
    public ResponseEntity<?> getEnabledRoles() {
        try {
            List<Role> roles = rbacService.getEnabledRoles();
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to get enabled roles: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 创建角色
     */
    @Operation(
            summary = "创建角色",
            description = "创建新的角色",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "创建失败，角色编码已存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 2.0, type = "create-role")
    @AuditLog(value = "创建角色", type = "RBAC")
    @PostMapping("/roles")
    public ResponseEntity<?> createRole(
            @Parameter(description = "角色信息", required = true,
                    content = @Content(schema = @Schema(implementation = Role.class)))
            @Valid @RequestBody Role role) {
        try {
            Role created = rbacService.createRole(role);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to create role: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取角色详情
     */
    @Operation(
            summary = "获取角色详情",
            description = "根据角色ID获取详细信息",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "角色不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/roles/{roleId}")
    public ResponseEntity<?> getRole(
            @Parameter(description = "角色ID", required = true, example = "role_admin")
            @PathVariable String roleId) {
        try {
            Role role = rbacService.getRoleById(roleId);
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Role not found: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * 更新角色
     */
    @Operation(
            summary = "更新角色",
            description = "更新角色信息",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "更新失败"),
            @ApiResponse(responseCode = "404", description = "角色不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 2.0, type = "update-role")
    @AuditLog(value = "更新角色", type = "RBAC")
    @PutMapping("/roles/{roleId}")
    public ResponseEntity<?> updateRole(
            @Parameter(description = "角色ID", required = true, example = "role_admin")
            @PathVariable String roleId,
            @Parameter(description = "角色信息", required = true,
                    content = @Content(schema = @Schema(implementation = Role.class)))
            @Valid @RequestBody Role role) {
        try {
            Role updated = rbacService.updateRole(roleId, role);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to update role: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 删除角色
     */
    @Operation(
            summary = "删除角色",
            description = "删除指定角色，系统角色不可删除",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "删除失败，系统角色不可删除"),
            @ApiResponse(responseCode = "404", description = "角色不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 1.0, type = "delete-role")
    @AuditLog(value = "删除角色", type = "RBAC")
    @DeleteMapping("/roles/{roleId}")
    public ResponseEntity<?> deleteRole(
            @Parameter(description = "角色ID", required = true, example = "role_admin")
            @PathVariable String roleId) {
        try {
            rbacService.deleteRole(roleId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Role deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to delete role: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 为角色分配权限
     */
    @Operation(
            summary = "为角色分配权限",
            description = "为指定角色分配权限列表",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "分配成功"),
            @ApiResponse(responseCode = "400", description = "分配失败"),
            @ApiResponse(responseCode = "404", description = "角色不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 2.0, type = "assign-permissions")
    @AuditLog(value = "分配角色权限", type = "RBAC")
    @PostMapping("/roles/{roleId}/permissions")
    public ResponseEntity<?> assignPermissions(
            @Parameter(description = "角色ID", required = true, example = "role_admin")
            @PathVariable String roleId,
            @Parameter(description = "权限ID列表", required = true)
            @RequestBody List<String> permissionIds) {
        try {
            Role role = rbacService.assignPermissions(roleId, permissionIds);
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to assign permissions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取角色的权限
     */
    @Operation(
            summary = "获取角色的权限",
            description = "获取指定角色的所有权限",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "角色不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/roles/{roleId}/permissions")
    public ResponseEntity<?> getRolePermissions(
            @Parameter(description = "角色ID", required = true, example = "role_admin")
            @PathVariable String roleId) {
        try {
            Set<Permission> permissions = rbacService.getRolePermissions(roleId);
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to get permissions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取角色的用户数
     */
    @Operation(
            summary = "获取角色的用户数",
            description = "统计拥有指定角色的用户数量",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/roles/{roleId}/user-count")
    public ResponseEntity<?> getRoleUserCount(
            @Parameter(description = "角色ID", required = true, example = "role_admin")
            @PathVariable String roleId) {
        try {
            long count = rbacService.getRoleUserCount(roleId);
            Map<String, Object> response = new HashMap<>();
            response.put("roleId", roleId);
            response.put("userCount", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to get user count: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // ==================== 权限管理 ====================

    /**
     * 获取权限列表
     */
    @Operation(
            summary = "获取权限列表",
            description = "分页获取所有权限信息",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/permissions")
    public ResponseEntity<?> getPermissions(
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Permission> permissions = rbacService.getAllPermissions(PageRequest.of(page, size));
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to get permissions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 创建权限
     */
    @Operation(
            summary = "创建权限",
            description = "创建新的权限",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "创建失败"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 2.0, type = "create-permission")
    @AuditLog(value = "创建权限", type = "RBAC")
    @PostMapping("/permissions")
    public ResponseEntity<?> createPermission(
            @Parameter(description = "权限信息", required = true,
                    content = @Content(schema = @Schema(implementation = Permission.class)))
            @Valid @RequestBody Permission permission) {
        try {
            Permission created = rbacService.createPermission(permission);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to create permission: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取权限详情
     */
    @Operation(
            summary = "获取权限详情",
            description = "根据权限ID获取详细信息",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "权限不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/permissions/{permissionId}")
    public ResponseEntity<?> getPermission(
            @Parameter(description = "权限ID", required = true, example = "perm_001")
            @PathVariable String permissionId) {
        try {
            Permission permission = rbacService.getPermissionById(permissionId);
            return ResponseEntity.ok(permission);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Permission not found: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * 更新权限
     */
    @Operation(
            summary = "更新权限",
            description = "更新权限信息",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "更新失败"),
            @ApiResponse(responseCode = "404", description = "权限不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 2.0, type = "update-permission")
    @AuditLog(value = "更新权限", type = "RBAC")
    @PutMapping("/permissions/{permissionId}")
    public ResponseEntity<?> updatePermission(
            @Parameter(description = "权限ID", required = true, example = "perm_001")
            @PathVariable String permissionId,
            @Parameter(description = "权限信息", required = true,
                    content = @Content(schema = @Schema(implementation = Permission.class)))
            @Valid @RequestBody Permission permission) {
        try {
            Permission updated = rbacService.updatePermission(permissionId, permission);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to update permission: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 删除权限
     */
    @Operation(
            summary = "删除权限",
            description = "删除指定权限",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "权限不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 1.0, type = "delete-permission")
    @AuditLog(value = "删除权限", type = "RBAC")
    @DeleteMapping("/permissions/{permissionId}")
    public ResponseEntity<?> deletePermission(
            @Parameter(description = "权限ID", required = true, example = "perm_001")
            @PathVariable String permissionId) {
        try {
            rbacService.deletePermission(permissionId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Permission deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to delete permission: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // ==================== 资源管理 ====================

    /**
     * 获取资源列表
     */
    @Operation(
            summary = "获取资源列表",
            description = "分页获取所有资源信息",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/resources")
    public ResponseEntity<?> getResources(
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Resource> resources = rbacService.getAllResources(PageRequest.of(page, size));
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to get resources: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取资源树
     */
    @Operation(
            summary = "获取资源树",
            description = "获取树形结构的资源列表",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/resources/tree")
    public ResponseEntity<?> getResourceTree() {
        try {
            List<Map<String, Object>> tree = rbacService.getResourceTree();
            return ResponseEntity.ok(tree);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to get resource tree: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 创建资源
     */
    @Operation(
            summary = "创建资源",
            description = "创建新的资源",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "创建失败"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 2.0, type = "create-resource")
    @AuditLog(value = "创建资源", type = "RBAC")
    @PostMapping("/resources")
    public ResponseEntity<?> createResource(
            @Parameter(description = "资源信息", required = true,
                    content = @Content(schema = @Schema(implementation = Resource.class)))
            @Valid @RequestBody Resource resource) {
        try {
            Resource created = rbacService.createResource(resource);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to create resource: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取资源详情
     */
    @Operation(
            summary = "获取资源详情",
            description = "根据资源ID获取详细信息",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/resources/{resourceId}")
    public ResponseEntity<?> getResource(
            @Parameter(description = "资源ID", required = true, example = "res_001")
            @PathVariable String resourceId) {
        try {
            Resource resource = rbacService.getResourceById(resourceId);
            return ResponseEntity.ok(resource);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Resource not found: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * 更新资源
     */
    @Operation(
            summary = "更新资源",
            description = "更新资源信息",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "更新失败"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 2.0, type = "update-resource")
    @AuditLog(value = "更新资源", type = "RBAC")
    @PutMapping("/resources/{resourceId}")
    public ResponseEntity<?> updateResource(
            @Parameter(description = "资源ID", required = true, example = "res_001")
            @PathVariable String resourceId,
            @Parameter(description = "资源信息", required = true,
                    content = @Content(schema = @Schema(implementation = Resource.class)))
            @Valid @RequestBody Resource resource) {
        try {
            Resource updated = rbacService.updateResource(resourceId, resource);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to update resource: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 删除资源
     */
    @Operation(
            summary = "删除资源",
            description = "删除指定资源及其关联权限",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 1.0, type = "delete-resource")
    @AuditLog(value = "删除资源", type = "RBAC")
    @DeleteMapping("/resources/{resourceId}")
    public ResponseEntity<?> deleteResource(
            @Parameter(description = "资源ID", required = true, example = "res_001")
            @PathVariable String resourceId) {
        try {
            rbacService.deleteResource(resourceId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Resource deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to delete resource: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // ==================== 用户角色管理 ====================

    /**
     * 获取用户的角色
     */
    @Operation(
            summary = "获取用户的角色",
            description = "获取指定用户的所有角色",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/users/{username}/roles")
    public ResponseEntity<?> getUserRoles(
            @Parameter(description = "用户名", required = true, example = "admin")
            @PathVariable String username) {
        try {
            List<Role> roles = rbacService.getUserRoles(username);
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to get user roles: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取用户的权限
     */
    @Operation(
            summary = "获取用户的权限",
            description = "获取指定用户的所有权限（通过角色继承）",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/users/{username}/permissions")
    public ResponseEntity<?> getUserPermissions(
            @Parameter(description = "用户名", required = true, example = "admin")
            @PathVariable String username) {
        try {
            Set<Permission> permissions = rbacService.getUserPermissions(username);
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to get user permissions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 为用户分配角色
     */
    @Operation(
            summary = "为用户分配角色",
            description = "为指定用户分配角色列表",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "分配成功"),
            @ApiResponse(responseCode = "400", description = "分配失败"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 2.0, type = "assign-user-roles")
    @AuditLog(value = "分配用户角色", type = "RBAC")
    @PostMapping("/users/{username}/roles")
    public ResponseEntity<?> assignUserRoles(
            @Parameter(description = "用户名", required = true, example = "testuser")
            @PathVariable String username,
            @Parameter(description = "角色ID列表", required = true)
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> roleIds = (List<String>) request.get("roleIds");
            String assignedBy = (String) request.getOrDefault("assignedBy", "admin");
            rbacService.assignRolesToUser(username, roleIds, assignedBy);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Roles assigned successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to assign roles: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 设置用户角色
     */
    @Operation(
            summary = "设置用户角色",
            description = "设置指定用户的角色列表（替换原有角色）",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "设置成功"),
            @ApiResponse(responseCode = "400", description = "设置失败"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 2.0, type = "set-user-roles")
    @AuditLog(value = "设置用户角色", type = "RBAC")
    @PutMapping("/users/{username}/roles")
    public ResponseEntity<?> setUserRoles(
            @Parameter(description = "用户名", required = true, example = "testuser")
            @PathVariable String username,
            @Parameter(description = "角色ID列表", required = true)
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> roleIds = (List<String>) request.get("roleIds");
            String assignedBy = (String) request.getOrDefault("assignedBy", "admin");
            rbacService.setUserRoles(username, roleIds, assignedBy);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User roles set successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to set user roles: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 移除用户角色
     */
    @Operation(
            summary = "移除用户角色",
            description = "移除指定用户的角色",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "移除成功"),
            @ApiResponse(responseCode = "400", description = "移除失败"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 2.0, type = "remove-user-roles")
    @AuditLog(value = "移除用户角色", type = "RBAC")
    @DeleteMapping("/users/{username}/roles")
    public ResponseEntity<?> removeUserRoles(
            @Parameter(description = "用户名", required = true, example = "testuser")
            @PathVariable String username,
            @Parameter(description = "角色ID列表", required = true)
            @RequestBody List<String> roleIds) {
        try {
            rbacService.removeRolesFromUser(username, roleIds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Roles removed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to remove roles: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 检查用户权限
     */
    @Operation(
            summary = "检查用户权限",
            description = "检查用户是否拥有指定权限",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "检查成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/users/{username}/has-permission")
    public ResponseEntity<?> checkUserPermission(
            @Parameter(description = "用户名", required = true, example = "admin")
            @PathVariable String username,
            @Parameter(description = "权限编码", required = true, example = "api:devices:read")
            @RequestParam String permissionCode) {
        try {
            boolean hasPermission = rbacService.hasPermission(username, permissionCode);
            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("permissionCode", permissionCode);
            response.put("hasPermission", hasPermission);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to check permission: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 检查用户角色
     */
    @Operation(
            summary = "检查用户角色",
            description = "检查用户是否拥有指定角色",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "检查成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/users/{username}/has-role")
    public ResponseEntity<?> checkUserRole(
            @Parameter(description = "用户名", required = true, example = "admin")
            @PathVariable String username,
            @Parameter(description = "角色编码", required = true, example = "admin")
            @RequestParam String roleCode) {
        try {
            boolean hasRole = rbacService.hasRole(username, roleCode);
            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("roleCode", roleCode);
            response.put("hasRole", hasRole);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to check role: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 初始化RBAC数据
     */
    @Operation(
            summary = "初始化RBAC数据",
            description = "初始化默认的角色、权限、资源数据",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "初始化成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @AuditLog(value = "初始化RBAC数据", type = "RBAC")
    @PostMapping("/init")
    public ResponseEntity<?> initRbacData() {
        try {
            rbacService.initDefaultRbacData();
            Map<String, Object> response = new HashMap<>();
            response.put("message", "RBAC data initialized successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to initialize RBAC data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
