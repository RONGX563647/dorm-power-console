package com.dormpower.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * 用户角色关联实体
 */
@Entity
@Table(name = "user_roles", indexes = {
    @Index(name = "idx_user_roles_user", columnList = "username"),
    @Index(name = "idx_user_roles_role", columnList = "role_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_role", columnNames = {"username", "role_id"})
})
@IdClass(UserRoleId.class)
public class UserRole {
    
    @Id
    private String username;
    
    @Id
    private String roleId;
    
    @NotNull
    private long assignedAt;
    
    private String assignedBy;
    
    public UserRole() {
        this.assignedAt = System.currentTimeMillis() / 1000;
    }
    
    public UserRole(String username, String roleId) {
        this.username = username;
        this.roleId = roleId;
        this.assignedAt = System.currentTimeMillis() / 1000;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getRoleId() {
        return roleId;
    }
    
    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }
    
    public long getAssignedAt() {
        return assignedAt;
    }
    
    public void setAssignedAt(long assignedAt) {
        this.assignedAt = assignedAt;
    }
    
    public String getAssignedBy() {
        return assignedBy;
    }
    
    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }
}
