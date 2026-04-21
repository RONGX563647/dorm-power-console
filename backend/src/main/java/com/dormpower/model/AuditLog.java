package com.dormpower.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * 审计日志实体
 * 
 * 记录所有关键操作:
 * - 操作人
 * - 操作类型
 * - 操作资源
 * - 操作时间
 * - 操作结果
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_resource", columnList = "resource"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "action", length = 50, nullable = false)
    private String action;  // LOGIN, LOGOUT, DEVICE_CONTROL, DATA_EXPORT, etc.

    @Column(name = "resource", length = 200)
    private String resource;  // device:001, user:123, etc.

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "params", columnDefinition = "TEXT")
    private String params;  // JSON 格式存储请求参数

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;  // JSON 格式存储响应结果

    @Column(name = "status", length = 20)
    private String status;  // SUCCESS, FAILED

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "duration_ms")
    private Long durationMs;  // 操作耗时 (毫秒)

    @PrePersist
    protected void onCreate() {
        timestamp = Instant.now();
    }
}
