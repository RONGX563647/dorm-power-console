package com.dormpower.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户账户实体
 * 
 * 支持 RBAC 权限模型:
 * - 用户可以拥有多个角色
 * - 角色可以拥有多个权限
 * - 权限精确到方法级别
 */
@Entity
@Table(name = "user_account")
@Data
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String account;

    @Column(nullable = false)
    private String password;

    @Column(length = 50)
    private String username;

    @Column(length = 20)
    private String role;  // ADMIN, OPERATOR, VIEWER

    @Column(length = 20)
    private String status;  // ACTIVE, INACTIVE, LOCKED

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
