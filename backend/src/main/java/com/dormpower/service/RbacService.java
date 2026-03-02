package com.dormpower.service;

import com.dormpower.model.Permission;
import com.dormpower.model.Resource;
import com.dormpower.model.Role;
import com.dormpower.model.UserRole;
import com.dormpower.repository.PermissionRepository;
import com.dormpower.repository.ResourceRepository;
import com.dormpower.repository.RoleRepository;
import com.dormpower.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RBAC服务
 */
@Service
public class RbacService {
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PermissionRepository permissionRepository;
    
    @Autowired
    private ResourceRepository resourceRepository;
    
    @Autowired
    private UserRoleRepository userRoleRepository;
    
    // ==================== 角色管理 ====================
    
    /**
     * 创建角色
     */
    public Role createRole(Role role) {
        if (roleRepository.existsByCode(role.getCode())) {
            throw new RuntimeException("Role code already exists: " + role.getCode());
        }
        role.setId("role_" + UUID.randomUUID().toString().substring(0, 8));
        role.setCreatedAt(System.currentTimeMillis() / 1000);
        role.setUpdatedAt(System.currentTimeMillis() / 1000);
        return roleRepository.save(role);
    }
    
    /**
     * 更新角色
     */
    public Role updateRole(String roleId, Role roleData) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
        
        if (!role.getCode().equals(roleData.getCode()) && 
            roleRepository.existsByCode(roleData.getCode())) {
            throw new RuntimeException("Role code already exists: " + roleData.getCode());
        }
        
        role.setCode(roleData.getCode());
        role.setName(roleData.getName());
        role.setDescription(roleData.getDescription());
        role.setEnabled(roleData.isEnabled());
        role.setUpdatedAt(System.currentTimeMillis() / 1000);
        
        return roleRepository.save(role);
    }
    
    /**
     * 删除角色
     */
    @Transactional
    public void deleteRole(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
        
        if (role.isSystem()) {
            throw new RuntimeException("Cannot delete system role: " + role.getCode());
        }
        
        // 删除用户角色关联
        userRoleRepository.deleteByRoleId(roleId);
        
        // 删除角色
        roleRepository.delete(role);
    }
    
    /**
     * 获取角色详情
     */
    public Role getRoleById(String roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
    }
    
    /**
     * 根据code获取角色
     */
    public Role getRoleByCode(String code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Role not found: " + code));
    }
    
    /**
     * 获取所有角色
     */
    public Page<Role> getAllRoles(Pageable pageable) {
        return roleRepository.findAll(pageable);
    }
    
    /**
     * 获取启用的角色
     */
    public List<Role> getEnabledRoles() {
        return roleRepository.findByEnabledTrue();
    }
    
    /**
     * 为角色分配权限
     */
    @Transactional
    public Role assignPermissions(String roleId, List<String> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
        
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(permissionIds));
        role.setPermissions(permissions);
        role.setUpdatedAt(System.currentTimeMillis() / 1000);
        
        return roleRepository.save(role);
    }
    
    /**
     * 移除角色权限
     */
    @Transactional
    public Role removePermissions(String roleId, List<String> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
        
        Set<Permission> permissions = role.getPermissions();
        if (permissions != null) {
            permissions.removeIf(p -> permissionIds.contains(p.getId()));
            role.setPermissions(permissions);
            role.setUpdatedAt(System.currentTimeMillis() / 1000);
        }
        
        return roleRepository.save(role);
    }
    
    /**
     * 获取角色的权限
     */
    public Set<Permission> getRolePermissions(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
        return role.getPermissions() != null ? role.getPermissions() : new HashSet<>();
    }
    
    // ==================== 权限管理 ====================
    
    /**
     * 创建权限
     */
    public Permission createPermission(Permission permission) {
        if (permissionRepository.existsByCode(permission.getCode())) {
            throw new RuntimeException("Permission code already exists: " + permission.getCode());
        }
        
        Resource resource = resourceRepository.findById(permission.getResource().getId())
                .orElseThrow(() -> new RuntimeException("Resource not found: " + permission.getResource().getId()));
        
        permission.setId("perm_" + UUID.randomUUID().toString().substring(0, 8));
        permission.setResource(resource);
        permission.setCreatedAt(System.currentTimeMillis() / 1000);
        permission.setUpdatedAt(System.currentTimeMillis() / 1000);
        
        return permissionRepository.save(permission);
    }
    
    /**
     * 更新权限
     */
    public Permission updatePermission(String permissionId, Permission permissionData) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + permissionId));
        
        if (!permission.getCode().equals(permissionData.getCode()) && 
            permissionRepository.existsByCode(permissionData.getCode())) {
            throw new RuntimeException("Permission code already exists: " + permissionData.getCode());
        }
        
        permission.setCode(permissionData.getCode());
        permission.setName(permissionData.getName());
        permission.setDescription(permissionData.getDescription());
        permission.setAction(permissionData.getAction());
        permission.setEnabled(permissionData.isEnabled());
        permission.setUpdatedAt(System.currentTimeMillis() / 1000);
        
        if (permissionData.getResource() != null && permissionData.getResource().getId() != null) {
            Resource resource = resourceRepository.findById(permissionData.getResource().getId())
                    .orElseThrow(() -> new RuntimeException("Resource not found"));
            permission.setResource(resource);
        }
        
        return permissionRepository.save(permission);
    }
    
    /**
     * 删除权限
     */
    @Transactional
    public void deletePermission(String permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + permissionId));
        permissionRepository.delete(permission);
    }
    
    /**
     * 获取权限详情
     */
    public Permission getPermissionById(String permissionId) {
        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + permissionId));
    }
    
    /**
     * 获取所有权限
     */
    public Page<Permission> getAllPermissions(Pageable pageable) {
        return permissionRepository.findAll(pageable);
    }
    
    /**
     * 获取资源的权限
     */
    public List<Permission> getPermissionsByResource(String resourceId) {
        return permissionRepository.findByResourceId(resourceId);
    }
    
    // ==================== 资源管理 ====================
    
    /**
     * 创建资源
     */
    public Resource createResource(Resource resource) {
        if (resourceRepository.existsByCode(resource.getCode())) {
            throw new RuntimeException("Resource code already exists: " + resource.getCode());
        }
        
        resource.setId("res_" + UUID.randomUUID().toString().substring(0, 8));
        resource.setCreatedAt(System.currentTimeMillis() / 1000);
        resource.setUpdatedAt(System.currentTimeMillis() / 1000);
        
        return resourceRepository.save(resource);
    }
    
    /**
     * 更新资源
     */
    public Resource updateResource(String resourceId, Resource resourceData) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found: " + resourceId));
        
        if (!resource.getCode().equals(resourceData.getCode()) && 
            resourceRepository.existsByCode(resourceData.getCode())) {
            throw new RuntimeException("Resource code already exists: " + resourceData.getCode());
        }
        
        resource.setCode(resourceData.getCode());
        resource.setName(resourceData.getName());
        resource.setDescription(resourceData.getDescription());
        resource.setType(resourceData.getType());
        resource.setUrl(resourceData.getUrl());
        resource.setMethod(resourceData.getMethod());
        resource.setParentId(resourceData.getParentId());
        resource.setSortOrder(resourceData.getSortOrder());
        resource.setEnabled(resourceData.isEnabled());
        resource.setUpdatedAt(System.currentTimeMillis() / 1000);
        
        return resourceRepository.save(resource);
    }
    
    /**
     * 删除资源
     */
    @Transactional
    public void deleteResource(String resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found: " + resourceId));
        
        // 删除关联权限
        List<Permission> permissions = permissionRepository.findByResourceId(resourceId);
        permissionRepository.deleteAll(permissions);
        
        resourceRepository.delete(resource);
    }
    
    /**
     * 获取资源详情
     */
    public Resource getResourceById(String resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found: " + resourceId));
    }
    
    /**
     * 获取所有资源
     */
    public Page<Resource> getAllResources(Pageable pageable) {
        return resourceRepository.findAll(pageable);
    }
    
    /**
     * 获取根资源
     */
    public List<Resource> getRootResources() {
        return resourceRepository.findRootResources();
    }
    
    /**
     * 获取子资源
     */
    public List<Resource> getChildResources(Long parentId) {
        return resourceRepository.findByParentIdOrderBySortOrder(parentId);
    }
    
    /**
     * 获取资源树
     */
    public List<Map<String, Object>> getResourceTree() {
        List<Resource> rootResources = resourceRepository.findRootResources();
        return buildResourceTree(rootResources);
    }
    
    private List<Map<String, Object>> buildResourceTree(List<Resource> resources) {
        List<Map<String, Object>> tree = new ArrayList<>();
        for (Resource resource : resources) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", resource.getId());
            node.put("code", resource.getCode());
            node.put("name", resource.getName());
            node.put("type", resource.getType());
            node.put("url", resource.getUrl());
            node.put("method", resource.getMethod());
            node.put("enabled", resource.isEnabled());
            
            // 使用数字ID查找子资源
            try {
                // 尝试从资源ID中提取数字部分作为parentId
                String numericPart = resource.getId().replaceAll("[^0-9]", "");
                if (!numericPart.isEmpty()) {
                    Long parentId = Long.parseLong(numericPart);
                    List<Resource> children = resourceRepository.findByParentIdOrderBySortOrder(parentId);
                    if (!children.isEmpty()) {
                        node.put("children", buildResourceTree(children));
                    }
                }
            } catch (NumberFormatException e) {
                // 忽略解析错误
            }
            
            tree.add(node);
        }
        return tree;
    }
    
    // ==================== 用户角色管理 ====================
    
    /**
     * 为用户分配角色
     */
    @Transactional
    public void assignRolesToUser(String username, List<String> roleIds, String assignedBy) {
        for (String roleId : roleIds) {
            if (!userRoleRepository.existsByUsernameAndRoleId(username, roleId)) {
                UserRole userRole = new UserRole(username, roleId);
                userRole.setAssignedBy(assignedBy);
                userRoleRepository.save(userRole);
            }
        }
    }
    
    /**
     * 移除用户角色
     */
    @Transactional
    public void removeRolesFromUser(String username, List<String> roleIds) {
        for (String roleId : roleIds) {
            userRoleRepository.deleteByUsernameAndRoleId(username, roleId);
        }
    }
    
    /**
     * 设置用户角色（替换原有角色）
     */
    @Transactional
    public void setUserRoles(String username, List<String> roleIds, String assignedBy) {
        // 删除原有角色
        userRoleRepository.deleteByUsername(username);
        
        // 分配新角色
        assignRolesToUser(username, roleIds, assignedBy);
    }
    
    /**
     * 获取用户的角色
     */
    public List<Role> getUserRoles(String username) {
        List<String> roleIds = userRoleRepository.findRoleIdsByUsername(username);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        return roleRepository.findAllById(roleIds);
    }
    
    /**
     * 获取用户的所有权限
     */
    public Set<Permission> getUserPermissions(String username) {
        List<Role> roles = getUserRoles(username);
        Set<Permission> permissions = new HashSet<>();
        
        for (Role role : roles) {
            if (role.isEnabled() && role.getPermissions() != null) {
                permissions.addAll(role.getPermissions());
            }
        }
        
        return permissions;
    }
    
    /**
     * 检查用户是否有指定权限
     */
    public boolean hasPermission(String username, String permissionCode) {
        Set<Permission> permissions = getUserPermissions(username);
        return permissions.stream()
                .anyMatch(p -> p.getCode().equals(permissionCode) && p.isEnabled());
    }
    
    /**
     * 检查用户是否有指定角色
     */
    public boolean hasRole(String username, String roleCode) {
        List<Role> roles = getUserRoles(username);
        return roles.stream()
                .anyMatch(r -> r.getCode().equals(roleCode) && r.isEnabled());
    }
    
    /**
     * 获取角色的用户数
     */
    public long getRoleUserCount(String roleId) {
        return userRoleRepository.countByRoleId(roleId);
    }
    
    // ==================== 初始化 ====================
    
    /**
     * 初始化默认RBAC数据
     */
    @Transactional
    public void initDefaultRbacData() {
        // 创建资源
        initResources();
        
        // 创建权限
        initPermissions();
        
        // 创建角色
        initRoles();
    }
    
    private void initResources() {
        if (resourceRepository.count() > 0) return;
        
        // API资源
        createResourceIfNotExists("api:devices", "设备管理", "API", "/api/devices", "ALL");
        createResourceIfNotExists("api:users", "用户管理", "API", "/api/users", "ALL");
        createResourceIfNotExists("api:roles", "角色管理", "API", "/api/roles", "ALL");
        createResourceIfNotExists("api:permissions", "权限管理", "API", "/api/permissions", "ALL");
        createResourceIfNotExists("api:resources", "资源管理", "API", "/api/resources", "ALL");
        createResourceIfNotExists("api:billing", "计费管理", "API", "/api/billing", "ALL");
        createResourceIfNotExists("api:dorm", "宿舍管理", "API", "/api/dorm", "ALL");
        createResourceIfNotExists("api:telemetry", "遥测数据", "API", "/api/telemetry", "ALL");
        createResourceIfNotExists("api:commands", "命令控制", "API", "/api/commands", "ALL");
        createResourceIfNotExists("api:alerts", "告警管理", "API", "/api/alerts", "ALL");
        createResourceIfNotExists("api:students", "学生管理", "API", "/api/students", "ALL");
        createResourceIfNotExists("api:admin", "系统管理", "API", "/api/admin", "ALL");
    }
    
    private void createResourceIfNotExists(String code, String name, String type, String url, String method) {
        if (!resourceRepository.existsByCode(code)) {
            Resource resource = new Resource();
            resource.setId("res_" + code.replace(":", "_"));
            resource.setCode(code);
            resource.setName(name);
            resource.setType(type);
            resource.setUrl(url);
            resource.setMethod(method);
            resource.setParentId(0L);
            resourceRepository.save(resource);
        }
    }
    
    private void initPermissions() {
        if (permissionRepository.count() > 0) return;
        
        List<Resource> resources = resourceRepository.findByEnabledTrue();
        String[] actions = {"create", "read", "update", "delete", "list"};
        
        for (Resource resource : resources) {
            for (String action : actions) {
                String code = resource.getCode() + ":" + action;
                if (!permissionRepository.existsByCode(code)) {
                    Permission permission = new Permission();
                    permission.setId("perm_" + code.replace(":", "_"));
                    permission.setCode(code);
                    permission.setName(resource.getName() + " - " + action);
                    permission.setResource(resource);
                    permission.setAction(action);
                    permissionRepository.save(permission);
                }
            }
        }
    }
    
    private void initRoles() {
        if (roleRepository.count() > 0) return;
        
        // 管理员角色
        Role adminRole = new Role();
        adminRole.setId("role_admin");
        adminRole.setCode("admin");
        adminRole.setName("管理员");
        adminRole.setDescription("系统管理员，拥有所有权限");
        adminRole.setSystem(true);
        roleRepository.save(adminRole);
        
        // 普通用户角色
        Role userRole = new Role();
        userRole.setId("role_user");
        userRole.setCode("user");
        userRole.setName("普通用户");
        userRole.setDescription("普通用户，拥有基本操作权限");
        userRole.setSystem(true);
        roleRepository.save(userRole);
        
        // 宿管角色
        Role dormManagerRole = new Role();
        dormManagerRole.setId("role_dorm_manager");
        dormManagerRole.setCode("dorm_manager");
        dormManagerRole.setName("宿管");
        dormManagerRole.setDescription("宿舍管理员，管理宿舍和学生");
        dormManagerRole.setSystem(true);
        roleRepository.save(dormManagerRole);
        
        // 财务角色
        Role financeRole = new Role();
        financeRole.setId("role_finance");
        financeRole.setCode("finance");
        financeRole.setName("财务");
        financeRole.setDescription("财务管理员，管理计费和充值");
        financeRole.setSystem(true);
        roleRepository.save(financeRole);
        
        // 为管理员分配所有权限
        List<Permission> allPermissions = permissionRepository.findByEnabledTrue();
        adminRole.setPermissions(new HashSet<>(allPermissions));
        roleRepository.save(adminRole);
        
        // 为普通用户分配基本权限
        List<String> userPermissionCodes = Arrays.asList(
            "api:devices:read", "api:devices:list",
            "api:telemetry:read", "api:telemetry:list",
            "api:billing:read", "api:billing:list"
        );
        List<Permission> userPermissions = permissionRepository.findByCodes(userPermissionCodes);
        userRole.setPermissions(new HashSet<>(userPermissions));
        roleRepository.save(userRole);
    }
}
