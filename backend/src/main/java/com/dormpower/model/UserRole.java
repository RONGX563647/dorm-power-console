package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Getter
@Setter
@NoArgsConstructor
public class UserRole {

    @Id
    private String username;

    @Id
    private String roleId;

    @NotNull
    private long assignedAt = System.currentTimeMillis() / 1000;

    private String assignedBy;

    /**
     * 便捷构造函数
     */
    public UserRole(String username, String roleId) {
        this.username = username;
        this.roleId = roleId;
        this.assignedAt = System.currentTimeMillis() / 1000;
    }
}