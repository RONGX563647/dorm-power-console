package com.dormpower.service;

import com.dormpower.model.Permission;
import com.dormpower.model.Role;
import com.dormpower.model.UserRole;
import com.dormpower.repository.PermissionRepository;
import com.dormpower.repository.ResourceRepository;
import com.dormpower.repository.RoleRepository;
import com.dormpower.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * RBAC服务单元测试
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
@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private RbacService rbacService;

    // ==================== TC-RBAC-001: 创建角色 ====================

    @Nested
    @DisplayName("TC-RBAC-001: 创建角色")
    class CreateRoleTests {

        @Test
        @DisplayName("TC-RBAC-001: 创建角色 - 有效角色数据，角色创建成功")
        void testCreateRole_Success() {
            // Given
            Role role = createRole(null, "test_role", "测试角色", "测试角色描述", false);

            when(roleRepository.existsByCode("test_role")).thenReturn(false);
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> {
                Role savedRole = inv.getArgument(0);
                savedRole.setId("role_test123");
                return savedRole;
            });

            // When
            Role result = rbacService.createRole(role);

            // Then
            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals("test_role", result.getCode());
            assertEquals("测试角色", result.getName());
            assertEquals("测试角色描述", result.getDescription());
            assertTrue(result.isEnabled());
            assertFalse(result.isSystem());
            verify(roleRepository).existsByCode("test_role");
            verify(roleRepository).save(any(Role.class));
        }

        @Test
        @DisplayName("TC-RBAC-001: 创建角色 - 角色编码已存在，抛出异常")
        void testCreateRole_CodeAlreadyExists() {
            // Given
            Role role = createRole(null, "admin", "管理员", "管理员角色", false);

            when(roleRepository.existsByCode("admin")).thenReturn(true);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> rbacService.createRole(role));
            assertTrue(exception.getMessage().contains("Role code already exists"));
            verify(roleRepository).existsByCode("admin");
            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-RBAC-001: 创建角色 - 验证默认值设置")
        void testCreateRole_DefaultValues() {
            // Given
            Role role = new Role();
            role.setCode("new_role");
            role.setName("新角色");

            when(roleRepository.existsByCode("new_role")).thenReturn(false);
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Role result = rbacService.createRole(role);

            // Then
            assertTrue(result.isEnabled());
            assertFalse(result.isSystem());
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());
        }
    }

    // ==================== TC-RBAC-002: 删除系统角色 ====================

    @Nested
    @DisplayName("TC-RBAC-002: 删除系统角色")
    class DeleteRoleTests {

        @Test
        @DisplayName("TC-RBAC-002: 删除系统角色 - 返回400错误")
        void testDeleteRole_SystemRole_ThrowsException() {
            // Given
            String roleId = "role_admin";
            Role systemRole = createRole(roleId, "admin", "管理员", "管理员角色", true);
            systemRole.setSystem(true);

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(systemRole));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> rbacService.deleteRole(roleId));
            assertTrue(exception.getMessage().contains("Cannot delete system role"));
            verify(roleRepository).findById(roleId);
            verify(roleRepository, never()).delete(any());
            verify(userRoleRepository, never()).deleteByRoleId(any());
        }

        @Test
        @DisplayName("TC-RBAC-002: 删除非系统角色 - 删除成功")
        void testDeleteRole_NonSystemRole_Success() {
            // Given
            String roleId = "role_test";
            Role normalRole = createRole(roleId, "test_role", "测试角色", "测试角色", false);

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(normalRole));
            doNothing().when(userRoleRepository).deleteByRoleId(roleId);
            doNothing().when(roleRepository).delete(normalRole);

            // When
            rbacService.deleteRole(roleId);

            // Then
            verify(roleRepository).findById(roleId);
            verify(userRoleRepository).deleteByRoleId(roleId);
            verify(roleRepository).delete(normalRole);
        }

        @Test
        @DisplayName("TC-RBAC-002: 删除角色 - 角色不存在")
        void testDeleteRole_NotFound() {
            // Given
            String roleId = "nonexistent";
            when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(RuntimeException.class, () -> rbacService.deleteRole(roleId));
            verify(roleRepository).findById(roleId);
            verify(roleRepository, never()).delete(any());
        }
    }

    // ==================== TC-RBAC-003: 分配权限 ====================

    @Nested
    @DisplayName("TC-RBAC-003: 分配权限")
    class AssignPermissionsTests {

        @Test
        @DisplayName("TC-RBAC-003: 分配权限 - 有效权限ID，权限分配成功")
        void testAssignPermissions_Success() {
            // Given
            String roleId = "role_test";
            Role role = createRole(roleId, "test_role", "测试角色", "测试角色", false);

            Permission perm1 = createPermission("perm_001", "api:devices:read", "设备读取", "read");
            Permission perm2 = createPermission("perm_002", "api:devices:create", "设备创建", "create");
            List<String> permissionIds = Arrays.asList("perm_001", "perm_002");

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(permissionRepository.findAllById(permissionIds)).thenReturn(Arrays.asList(perm1, perm2));
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Role result = rbacService.assignPermissions(roleId, permissionIds);

            // Then
            assertNotNull(result);
            assertNotNull(result.getPermissions());
            assertEquals(2, result.getPermissions().size());
            verify(roleRepository).findById(roleId);
            verify(permissionRepository).findAllById(permissionIds);
            verify(roleRepository).save(any(Role.class));
        }

        @Test
        @DisplayName("TC-RBAC-003: 分配权限 - 角色不存在")
        void testAssignPermissions_RoleNotFound() {
            // Given
            String roleId = "nonexistent";
            List<String> permissionIds = Arrays.asList("perm_001");

            when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(RuntimeException.class, () -> rbacService.assignPermissions(roleId, permissionIds));
            verify(permissionRepository, never()).findAllById(anyList());
        }

        @Test
        @DisplayName("TC-RBAC-003: 分配权限 - 清空权限")
        void testAssignPermissions_ClearPermissions() {
            // Given
            String roleId = "role_test";
            Role role = createRole(roleId, "test_role", "测试角色", "测试角色", false);
            role.setPermissions(new HashSet<>(Arrays.asList(
                    createPermission("perm_001", "api:devices:read", "设备读取", "read")
            )));

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(permissionRepository.findAllById(Collections.emptyList())).thenReturn(Collections.emptyList());
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Role result = rbacService.assignPermissions(roleId, Collections.emptyList());

            // Then
            assertNotNull(result.getPermissions());
            assertTrue(result.getPermissions().isEmpty());
        }

        @Test
        @DisplayName("TC-RBAC-003: 移除角色权限 - 成功")
        void testRemovePermissions_Success() {
            // Given
            String roleId = "role_test";
            Permission perm1 = createPermission("perm_001", "api:devices:read", "设备读取", "read");
            Permission perm2 = createPermission("perm_002", "api:devices:create", "设备创建", "create");

            Role role = createRole(roleId, "test_role", "测试角色", "测试角色", false);
            role.setPermissions(new HashSet<>(Arrays.asList(perm1, perm2)));

            List<String> permissionIdsToRemove = Arrays.asList("perm_001");

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Role result = rbacService.removePermissions(roleId, permissionIdsToRemove);

            // Then
            assertEquals(1, result.getPermissions().size());
            assertFalse(result.getPermissions().stream().anyMatch(p -> p.getId().equals("perm_001")));
        }
    }

    // ==================== TC-RBAC-004: 用户角色分配 ====================

    @Nested
    @DisplayName("TC-RBAC-004: 用户角色分配")
    class AssignRolesToUserTests {

        @Test
        @DisplayName("TC-RBAC-004: 用户角色分配 - 有效用户和角色，分配成功")
        void testAssignRolesToUser_Success() {
            // Given
            String username = "testuser";
            List<String> roleIds = Arrays.asList("role_admin", "role_user");
            String assignedBy = "admin";

            when(userRoleRepository.existsByUsernameAndRoleId(anyString(), anyString())).thenReturn(false);
            when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            rbacService.assignRolesToUser(username, roleIds, assignedBy);

            // Then
            verify(userRoleRepository, times(2)).existsByUsernameAndRoleId(anyString(), anyString());
            verify(userRoleRepository, times(2)).save(any(UserRole.class));
        }

        @Test
        @DisplayName("TC-RBAC-004: 用户角色分配 - 避免重复分配")
        void testAssignRolesToUser_AvoidDuplicate() {
            // Given
            String username = "testuser";
            List<String> roleIds = Arrays.asList("role_admin", "role_user");
            String assignedBy = "admin";

            when(userRoleRepository.existsByUsernameAndRoleId(username, "role_admin")).thenReturn(true);
            when(userRoleRepository.existsByUsernameAndRoleId(username, "role_user")).thenReturn(false);
            when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            rbacService.assignRolesToUser(username, roleIds, assignedBy);

            // Then
            verify(userRoleRepository, times(2)).existsByUsernameAndRoleId(anyString(), anyString());
            verify(userRoleRepository, times(1)).save(any(UserRole.class)); // 只保存一个
        }

        @Test
        @DisplayName("TC-RBAC-004: 设置用户角色 - 替换原有角色")
        void testSetUserRoles_ReplaceRoles() {
            // Given
            String username = "testuser";
            List<String> newRoleIds = Arrays.asList("role_admin");
            String assignedBy = "admin";

            doNothing().when(userRoleRepository).deleteByUsername(username);
            when(userRoleRepository.existsByUsernameAndRoleId(anyString(), anyString())).thenReturn(false);
            when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            rbacService.setUserRoles(username, newRoleIds, assignedBy);

            // Then
            verify(userRoleRepository).deleteByUsername(username);
            verify(userRoleRepository).save(any(UserRole.class));
        }

        @Test
        @DisplayName("TC-RBAC-004: 移除用户角色 - 成功")
        void testRemoveRolesFromUser_Success() {
            // Given
            String username = "testuser";
            List<String> roleIds = Arrays.asList("role_admin");

            doNothing().when(userRoleRepository).deleteByUsernameAndRoleId(username, "role_admin");

            // When
            rbacService.removeRolesFromUser(username, roleIds);

            // Then
            verify(userRoleRepository).deleteByUsernameAndRoleId(username, "role_admin");
        }

        @Test
        @DisplayName("TC-RBAC-004: 获取用户角色 - 成功")
        void testGetUserRoles_Success() {
            // Given
            String username = "testuser";
            List<String> roleIds = Arrays.asList("role_admin", "role_user");
            Role adminRole = createRole("role_admin", "admin", "管理员", "管理员", true);
            Role userRole = createRole("role_user", "user", "普通用户", "普通用户", true);

            when(userRoleRepository.findRoleIdsByUsername(username)).thenReturn(roleIds);
            when(roleRepository.findAllById(roleIds)).thenReturn(Arrays.asList(adminRole, userRole));

            // When
            List<Role> result = rbacService.getUserRoles(username);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(userRoleRepository).findRoleIdsByUsername(username);
            verify(roleRepository).findAllById(roleIds);
        }

        @Test
        @DisplayName("TC-RBAC-004: 获取用户角色 - 用户无角色")
        void testGetUserRoles_NoRoles() {
            // Given
            String username = "newuser";
            when(userRoleRepository.findRoleIdsByUsername(username)).thenReturn(Collections.emptyList());

            // When
            List<Role> result = rbacService.getUserRoles(username);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== TC-RBAC-005: 权限检查 ====================

    @Nested
    @DisplayName("TC-RBAC-005: 权限检查")
    class PermissionCheckTests {

        @Test
        @DisplayName("TC-RBAC-005: 权限检查 - 有权限的用户，返回true")
        void testHasPermission_UserHasPermission_ReturnsTrue() {
            // Given
            String username = "admin";
            String permissionCode = "api:devices:read";

            Role adminRole = createRole("role_admin", "admin", "管理员", "管理员", true);
            Permission permission = createPermission("perm_001", permissionCode, "设备读取", "read");
            permission.setEnabled(true);
            adminRole.setPermissions(new HashSet<>(Arrays.asList(permission)));

            when(userRoleRepository.findRoleIdsByUsername(username)).thenReturn(Arrays.asList("role_admin"));
            when(roleRepository.findAllById(anyList())).thenReturn(Arrays.asList(adminRole));

            // When
            boolean result = rbacService.hasPermission(username, permissionCode);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("TC-RBAC-005: 权限检查 - 无权限的用户，返回false")
        void testHasPermission_UserHasNoPermission_ReturnsFalse() {
            // Given
            String username = "normaluser";
            String permissionCode = "api:admin:delete";

            Role userRole = createRole("role_user", "user", "普通用户", "普通用户", true);
            Permission readPermission = createPermission("perm_001", "api:devices:read", "设备读取", "read");
            userRole.setPermissions(new HashSet<>(Arrays.asList(readPermission)));

            when(userRoleRepository.findRoleIdsByUsername(username)).thenReturn(Arrays.asList("role_user"));
            when(roleRepository.findAllById(anyList())).thenReturn(Arrays.asList(userRole));

            // When
            boolean result = rbacService.hasPermission(username, permissionCode);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("TC-RBAC-005: 权限检查 - 用户无角色，返回false")
        void testHasPermission_UserHasNoRoles_ReturnsFalse() {
            // Given
            String username = "newuser";
            String permissionCode = "api:devices:read";

            when(userRoleRepository.findRoleIdsByUsername(username)).thenReturn(Collections.emptyList());

            // When
            boolean result = rbacService.hasPermission(username, permissionCode);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("TC-RBAC-005: 权限检查 - 角色已禁用，不计算权限")
        void testHasPermission_RoleDisabled_ReturnsFalse() {
            // Given
            String username = "testuser";
            String permissionCode = "api:devices:read";

            Role disabledRole = createRole("role_disabled", "disabled", "禁用角色", "禁用角色", false);
            disabledRole.setEnabled(false);
            Permission permission = createPermission("perm_001", permissionCode, "设备读取", "read");
            disabledRole.setPermissions(new HashSet<>(Arrays.asList(permission)));

            when(userRoleRepository.findRoleIdsByUsername(username)).thenReturn(Arrays.asList("role_disabled"));
            when(roleRepository.findAllById(anyList())).thenReturn(Arrays.asList(disabledRole));

            // When
            boolean result = rbacService.hasPermission(username, permissionCode);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("TC-RBAC-005: 权限检查 - 权限已禁用，返回false")
        void testHasPermission_PermissionDisabled_ReturnsFalse() {
            // Given
            String username = "testuser";
            String permissionCode = "api:devices:read";

            Role role = createRole("role_test", "test", "测试角色", "测试角色", true);
            Permission disabledPermission = createPermission("perm_001", permissionCode, "设备读取", "read");
            disabledPermission.setEnabled(false);
            role.setPermissions(new HashSet<>(Arrays.asList(disabledPermission)));

            when(userRoleRepository.findRoleIdsByUsername(username)).thenReturn(Arrays.asList("role_test"));
            when(roleRepository.findAllById(anyList())).thenReturn(Arrays.asList(role));

            // When
            boolean result = rbacService.hasPermission(username, permissionCode);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("TC-RBAC-005: 角色检查 - 有角色返回true")
        void testHasRole_UserHasRole_ReturnsTrue() {
            // Given
            String username = "admin";
            String roleCode = "admin";

            Role adminRole = createRole("role_admin", roleCode, "管理员", "管理员", true);

            when(userRoleRepository.findRoleIdsByUsername(username)).thenReturn(Arrays.asList("role_admin"));
            when(roleRepository.findAllById(anyList())).thenReturn(Arrays.asList(adminRole));

            // When
            boolean result = rbacService.hasRole(username, roleCode);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("TC-RBAC-005: 角色检查 - 无角色返回false")
        void testHasRole_UserHasNoRole_ReturnsFalse() {
            // Given
            String username = "normaluser";
            String roleCode = "admin";

            Role userRole = createRole("role_user", "user", "普通用户", "普通用户", true);

            when(userRoleRepository.findRoleIdsByUsername(username)).thenReturn(Arrays.asList("role_user"));
            when(roleRepository.findAllById(anyList())).thenReturn(Arrays.asList(userRole));

            // When
            boolean result = rbacService.hasRole(username, roleCode);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("TC-RBAC-005: 获取用户所有权限 - 成功")
        void testGetUserPermissions_Success() {
            // Given
            String username = "admin";

            Role adminRole = createRole("role_admin", "admin", "管理员", "管理员", true);
            Permission perm1 = createPermission("perm_001", "api:devices:read", "设备读取", "read");
            Permission perm2 = createPermission("perm_002", "api:devices:create", "设备创建", "create");
            adminRole.setPermissions(new HashSet<>(Arrays.asList(perm1, perm2)));

            when(userRoleRepository.findRoleIdsByUsername(username)).thenReturn(Arrays.asList("role_admin"));
            when(roleRepository.findAllById(anyList())).thenReturn(Arrays.asList(adminRole));

            // When
            Set<Permission> result = rbacService.getUserPermissions(username);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
        }
    }

    // ==================== 其他测试 ====================

    @Nested
    @DisplayName("其他功能测试")
    class OtherFunctionTests {

        @Test
        @DisplayName("获取角色详情 - 成功")
        void testGetRoleById_Success() {
            // Given
            String roleId = "role_admin";
            Role role = createRole(roleId, "admin", "管理员", "管理员角色", true);

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

            // When
            Role result = rbacService.getRoleById(roleId);

            // Then
            assertNotNull(result);
            assertEquals(roleId, result.getId());
            assertEquals("admin", result.getCode());
        }

        @Test
        @DisplayName("获取角色详情 - 不存在")
        void testGetRoleById_NotFound() {
            // Given
            String roleId = "nonexistent";
            when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(RuntimeException.class, () -> rbacService.getRoleById(roleId));
        }

        @Test
        @DisplayName("获取所有角色 - 分页")
        void testGetAllRoles_Pagination() {
            // Given
            Role role1 = createRole("role_1", "role1", "角色1", "角色1", false);
            Role role2 = createRole("role_2", "role2", "角色2", "角色2", false);
            Page<Role> rolePage = new PageImpl<>(Arrays.asList(role1, role2));

            when(roleRepository.findAll(any(Pageable.class))).thenReturn(rolePage);

            // When
            Page<Role> result = rbacService.getAllRoles(PageRequest.of(0, 10));

            // Then
            assertNotNull(result);
            assertEquals(2, result.getContent().size());
        }

        @Test
        @DisplayName("获取启用的角色")
        void testGetEnabledRoles() {
            // Given
            Role role1 = createRole("role_1", "role1", "角色1", "角色1", false);
            Role role2 = createRole("role_2", "role2", "角色2", "角色2", false);

            when(roleRepository.findByEnabledTrue()).thenReturn(Arrays.asList(role1, role2));

            // When
            List<Role> result = rbacService.getEnabledRoles();

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("获取角色用户数")
        void testGetRoleUserCount() {
            // Given
            String roleId = "role_admin";
            when(userRoleRepository.countByRoleId(roleId)).thenReturn(5L);

            // When
            long count = rbacService.getRoleUserCount(roleId);

            // Then
            assertEquals(5L, count);
        }

        @Test
        @DisplayName("更新角色 - 成功")
        void testUpdateRole_Success() {
            // Given
            String roleId = "role_test";
            Role existingRole = createRole(roleId, "old_code", "旧名称", "旧描述", false);
            Role updateData = createRole(null, "new_code", "新名称", "新描述", true);

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
            when(roleRepository.existsByCode("new_code")).thenReturn(false);
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Role result = rbacService.updateRole(roleId, updateData);

            // Then
            assertEquals("new_code", result.getCode());
            assertEquals("新名称", result.getName());
            assertEquals("新描述", result.getDescription());
            assertTrue(result.isEnabled());
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