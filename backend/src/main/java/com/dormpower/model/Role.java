package com.dormpower.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * 角色实体
 * 
 * RBAC 模型核心:
 * - 角色可以包含多个权限
 * - 用户可以拥有多个角色
 */
@Entity
@Table(name = "role")
@Data
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String name;  // ROLE_ADMIN, ROLE_OPERATOR, ROLE_VIEWER

    @Column(length = 100)
    private String description;

    @Column(name = "created_at")
    private Instant createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permission",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Permission> permissions = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
