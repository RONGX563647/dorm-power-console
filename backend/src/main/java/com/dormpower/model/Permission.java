package com.dormpower.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * 权限实体
 * 
 * 细粒度权限控制:
 * - 精确到方法级别
 * - 支持资源类型限制
 * - 支持操作类型限制
 */
@Entity
@Table(name = "permission")
@Data
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;  // device:create, device:read, device:update, device:delete

    @Column(length = 20)
    private String resourceType;  // device, command, telemetry, user, report

    @Column(length = 20)
    private String operation;  // CREATE, READ, UPDATE, DELETE, EXPORT

    @Column(length = 200)
    private String description;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
