package com.dormpower.controller;

import com.dormpower.model.Permission;
import com.dormpower.model.Role;
import com.dormpower.service.RbacService;
import com.dormpower.util.JwtUtil;
import com.dormpower.util.TokenBlacklist;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RBAC控制器单元测试
 *
 * 测试用例覆盖：
 * - TC-RBAC-001: 创建角色
 * - TC-RBAC-002: 删除系统角色
 * - TC-RBAC-003: 分配权限
 * - TC-RBAC-004: 用户角色分配
 * - TC-RBAC-005: 权限检查
 *
 * @author dormpower team
 * @version 1.0
 */
@WebMvcTest(RbacController.class)
@AutoConfigureMockMvc
class RbacControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RbacService rbacService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private TokenBlacklist tokenBlacklist;

    // ==================== TC-RBAC-001: 创建角色 ====================

    @Nested
    @DisplayName("TC-RBAC-001: 创建角色")
    class CreateRoleTests {

        @Test
        @DisplayName("TC-RBAC-001: 创建角色 - 有效角色数据，角色创建成功")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testCreateRole_Success() throws Exception {
            // Given
            Role role = createRole("role_test", "test_role", "测试角色", "测试角色描述", false);
            when(rbacService.createRole(any(Role.class))).thenReturn(role);

            // When & Then
            mockMvc.perform(post("/api/rbac/roles")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(role)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("role_test"))
                    .andExpect(jsonPath("$.code").value("test_role"))
                    .andExpect(jsonPath("$.name").value("测试角色"));
        }

        @Test
        @DisplayName("TC-RBAC-001: 创建角色 - 角色编码已存在，返回400")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testCreateRole_CodeAlreadyExists() throws Exception {
            // Given
            Role role = createRole(null, "admin", "管理员副本", "管理员角色副本", false);
            when(rbacService.createRole(any(Role.class)))
                    .thenThrow(new RuntimeException("Role code already exists: admin"));

            // When & Then
            mockMvc.perform(post("/api/rbac/roles")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(role)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("TC-RBAC-001: 创建角色 - 无认证，返回403")
        void testCreateRole_NoAuth() throws Exception {
            // Given
            Role role = createRole(null, "test_role", "测试角色", "测试角色描述", false);

            // When & Then - Spring Security returns 403 for unauthenticated requests in MockMvc
            mockMvc.perform(post("/api/rbac/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(role)))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== TC-RBAC-002: 删除系统角色 ====================

    @Nested
    @DisplayName("TC-RBAC-002: 删除系统角色")
    class DeleteRoleTests {

        @Test
        @DisplayName("TC-RBAC-002: 删除系统角色 - 返回400错误")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testDeleteRole_SystemRole_ThrowsException() throws Exception {
            // Given
            String systemRoleId = "role_admin";
            doThrow(new RuntimeException("Cannot delete system role: admin"))
                    .when(rbacService).deleteRole(systemRoleId);

            // When & Then
            mockMvc.perform(delete("/api/rbac/roles/{roleId}", systemRoleId)
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot delete system role")));
        }

        @Test
        @DisplayName("TC-RBAC-002: 删除非系统角色 - 删除成功")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testDeleteRole_NonSystemRole_Success() throws Exception {
            // Given
            String roleId = "role_test";
            doNothing().when(rbacService).deleteRole(roleId);

            // When & Then
            mockMvc.perform(delete("/api/rbac/roles/{roleId}", roleId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Role deleted successfully"));
        }

        @Test
        @DisplayName("TC-RBAC-002: 删除角色 - 角色不存在")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testDeleteRole_NotFound() throws Exception {
            // Given
            String nonexistentRoleId = "role_nonexistent";
            doThrow(new RuntimeException("Role not found: " + nonexistentRoleId))
                    .when(rbacService).deleteRole(nonexistentRoleId);

            // When & Then
            mockMvc.perform(delete("/api/rbac/roles/{roleId}", nonexistentRoleId)
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    // ==================== TC-RBAC-003: 分配权限 ====================

    @Nested
    @DisplayName("TC-RBAC-003: 分配权限")
    class AssignPermissionsTests {

        @Test
        @DisplayName("TC-RBAC-003: 分配权限 - 有效权限ID，权限分配成功")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testAssignPermissions_Success() throws Exception {
            // Given
            String roleId = "role_test";
            Permission perm1 = createPermission("perm_001", "api:devices:read", "设备读取", "read");
            Role role = createRole(roleId, "test_role", "测试角色", "测试角色", false);
            role.setPermissions(new HashSet<>(Arrays.asList(perm1)));

            List<String> permissionIds = Arrays.asList("perm_001");
            when(rbacService.assignPermissions(roleId, permissionIds)).thenReturn(role);

            // When & Then
            mockMvc.perform(post("/api/rbac/roles/{roleId}/permissions", roleId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(permissionIds)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(roleId));
        }

        @Test
        @DisplayName("TC-RBAC-003: 分配权限 - 角色不存在")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testAssignPermissions_RoleNotFound() throws Exception {
            // Given
            String nonexistentRoleId = "role_nonexistent";
            List<String> permissionIds = Arrays.asList("perm_001");
            when(rbacService.assignPermissions(nonexistentRoleId, permissionIds))
                    .thenThrow(new RuntimeException("Role not found: " + nonexistentRoleId));

            // When & Then
            mockMvc.perform(post("/api/rbac/roles/{roleId}/permissions", nonexistentRoleId)
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(permissionIds)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-RBAC-003: 获取角色权限 - 成功")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testGetRolePermissions_Success() throws Exception {
            // Given
            String roleId = "role_test";
            Permission perm1 = createPermission("perm_001", "api:devices:read", "设备读取", "read");
            when(rbacService.getRolePermissions(roleId)).thenReturn(new HashSet<>(Arrays.asList(perm1)));

            // When & Then
            mockMvc.perform(get("/api/rbac/roles/{roleId}/permissions", roleId))
                    .andExpect(status().isOk());
        }
    }

    // ==================== TC-RBAC-004: 用户角色分配 ====================

    @Nested
    @DisplayName("TC-RBAC-004: 用户角色分配")
    class AssignUserRolesTests {

        @Test
        @DisplayName("TC-RBAC-004: 用户角色分配 - 有效用户和角色，分配成功")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testAssignUserRoles_Success() throws Exception {
            // Given
            String username = "testuser";
            Map<String, Object> request = new HashMap<>();
            request.put("roleIds", Arrays.asList("role_user"));
            request.put("assignedBy", "admin");

            doNothing().when(rbacService).assignRolesToUser(anyString(), anyList(), anyString());

            // When & Then
            mockMvc.perform(post("/api/rbac/users/{username}/roles", username)
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Roles assigned successfully"));
        }

        @Test
        @DisplayName("TC-RBAC-004: 获取用户角色 - 成功")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testGetUserRoles_Success() throws Exception {
            // Given
            String username = "admin";
            Role adminRole = createRole("role_admin", "admin", "管理员", "管理员", true);
            when(rbacService.getUserRoles(username)).thenReturn(Arrays.asList(adminRole));

            // When & Then
            mockMvc.perform(get("/api/rbac/users/{username}/roles", username))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-RBAC-004: 设置用户角色 - 替换原有角色")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testSetUserRoles_Success() throws Exception {
            // Given
            String username = "testuser";
            Map<String, Object> request = new HashMap<>();
            request.put("roleIds", Arrays.asList("role_user"));
            request.put("assignedBy", "admin");

            doNothing().when(rbacService).setUserRoles(anyString(), anyList(), anyString());

            // When & Then
            mockMvc.perform(put("/api/rbac/users/{username}/roles", username)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User roles set successfully"));
        }

        @Test
        @DisplayName("TC-RBAC-004: 移除用户角色 - 成功")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testRemoveUserRoles_Success() throws Exception {
            // Given
            String username = "testuser";
            List<String> roleIds = Arrays.asList("role_user");

            doNothing().when(rbacService).removeRolesFromUser(anyString(), anyList());

            // When & Then
            mockMvc.perform(delete("/api/rbac/users/{username}/roles", username)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(roleIds)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Roles removed successfully"));
        }
    }

    // ==================== TC-RBAC-005: 权限检查 ====================

    @Nested
    @DisplayName("TC-RBAC-005: 权限检查")
    class PermissionCheckTests {

        @Test
        @DisplayName("TC-RBAC-005: 权限检查 - 有权限的用户，返回true")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testHasPermission_UserHasPermission_ReturnsTrue() throws Exception {
            // Given
            String username = "admin";
            String permissionCode = "api:devices:read";
            when(rbacService.hasPermission(username, permissionCode)).thenReturn(true);

            // When & Then
            mockMvc.perform(get("/api/rbac/users/{username}/has-permission", username)
                            .param("permissionCode", permissionCode))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(username))
                    .andExpect(jsonPath("$.permissionCode").value(permissionCode))
                    .andExpect(jsonPath("$.hasPermission").value(true));
        }

        @Test
        @DisplayName("TC-RBAC-005: 权限检查 - 无权限的用户，返回false")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testHasPermission_UserHasNoPermission_ReturnsFalse() throws Exception {
            // Given
            String username = "normal_user";
            String permissionCode = "api:admin:delete";
            when(rbacService.hasPermission(username, permissionCode)).thenReturn(false);

            // When & Then
            mockMvc.perform(get("/api/rbac/users/{username}/has-permission", username)
                            .param("permissionCode", permissionCode))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasPermission").value(false));
        }

        @Test
        @DisplayName("TC-RBAC-005: 角色检查 - 有角色返回true")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testHasRole_UserHasRole_ReturnsTrue() throws Exception {
            // Given
            String username = "admin";
            String roleCode = "admin";
            when(rbacService.hasRole(username, roleCode)).thenReturn(true);

            // When & Then
            mockMvc.perform(get("/api/rbac/users/{username}/has-role", username)
                            .param("roleCode", roleCode))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(username))
                    .andExpect(jsonPath("$.roleCode").value(roleCode))
                    .andExpect(jsonPath("$.hasRole").value(true));
        }

        @Test
        @DisplayName("TC-RBAC-005: 角色检查 - 无角色返回false")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testHasRole_UserHasNoRole_ReturnsFalse() throws Exception {
            // Given
            String username = "normal_user";
            String roleCode = "admin";
            when(rbacService.hasRole(username, roleCode)).thenReturn(false);

            // When & Then
            mockMvc.perform(get("/api/rbac/users/{username}/has-role", username)
                            .param("roleCode", roleCode))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasRole").value(false));
        }

        @Test
        @DisplayName("TC-RBAC-005: 获取用户权限 - 成功")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testGetUserPermissions_Success() throws Exception {
            // Given
            String username = "admin";
            Permission perm1 = createPermission("perm_001", "api:devices:read", "设备读取", "read");
            when(rbacService.getUserPermissions(username)).thenReturn(new HashSet<>(Arrays.asList(perm1)));

            // When & Then
            mockMvc.perform(get("/api/rbac/users/{username}/permissions", username))
                    .andExpect(status().isOk());
        }
    }

    // ==================== 其他功能测试 ====================

    @Nested
    @DisplayName("其他功能测试")
    class OtherFunctionTests {

        @Test
        @DisplayName("获取角色列表 - 分页")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testGetRoles_Pagination() throws Exception {
            // Given
            Role role1 = createRole("role_1", "role1", "角色1", "角色1", false);
            Page<Role> rolePage = new PageImpl<>(Arrays.asList(role1));
            when(rbacService.getAllRoles(any())).thenReturn(rolePage);

            // When & Then
            mockMvc.perform(get("/api/rbac/roles")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("获取启用的角色")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testGetEnabledRoles() throws Exception {
            // Given
            Role role1 = createRole("role_1", "role1", "角色1", "角色1", false);
            when(rbacService.getEnabledRoles()).thenReturn(Arrays.asList(role1));

            // When & Then
            mockMvc.perform(get("/api/rbac/roles/enabled"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("获取权限列表 - 分页")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testGetPermissions_Pagination() throws Exception {
            // Given
            Permission perm1 = createPermission("perm_001", "api:devices:read", "设备读取", "read");
            Page<Permission> permPage = new PageImpl<>(Arrays.asList(perm1));
            when(rbacService.getAllPermissions(any())).thenReturn(permPage);

            // When & Then
            mockMvc.perform(get("/api/rbac/permissions")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("获取角色用户数")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testGetRoleUserCount() throws Exception {
            // Given
            String roleId = "role_admin";
            when(rbacService.getRoleUserCount(roleId)).thenReturn(5L);

            // When & Then
            mockMvc.perform(get("/api/rbac/roles/{roleId}/user-count", roleId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roleId").value(roleId))
                    .andExpect(jsonPath("$.userCount").value(5));
        }

        @Test
        @DisplayName("获取角色详情 - 成功")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testGetRoleById_Success() throws Exception {
            // Given
            String roleId = "role_admin";
            Role role = createRole(roleId, "admin", "管理员", "管理员角色", true);
            when(rbacService.getRoleById(roleId)).thenReturn(role);

            // When & Then
            mockMvc.perform(get("/api/rbac/roles/{roleId}", roleId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(roleId))
                    .andExpect(jsonPath("$.code").value("admin"));
        }

        @Test
        @DisplayName("获取角色详情 - 不存在")
        @WithMockUser(username = "admin", roles = {"admin"})
        void testGetRoleById_NotFound() throws Exception {
            // Given
            String roleId = "nonexistent";
            when(rbacService.getRoleById(roleId))
                    .thenThrow(new RuntimeException("Role not found: " + roleId));

            // When & Then
            mockMvc.perform(get("/api/rbac/roles/{roleId}", roleId))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== 辅助方法 ====================

    private Role createRole(String id, String code, String name, String description, boolean isSystem) {
        Role role = new Role();
        role.setId(id);
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setSystem(isSystem);
        role.setEnabled(true);
        role.setCreatedAt(System.currentTimeMillis() / 1000);
        role.setUpdatedAt(System.currentTimeMillis() / 1000);
        return role;
    }

    private Permission createPermission(String id, String code, String name, String action) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setCode(code);
        permission.setName(name);
        permission.setAction(action);
        permission.setEnabled(true);
        permission.setCreatedAt(System.currentTimeMillis() / 1000);
        permission.setUpdatedAt(System.currentTimeMillis() / 1000);
        return permission;
    }
}