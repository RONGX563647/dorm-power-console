package com.dormpower.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 操作审计日志实体
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_username", columnList = "username"),
    @Index(name = "idx_audit_ts", columnList = "ts"),
    @Index(name = "idx_audit_module", columnList = "module"),
    @Index(name = "idx_audit_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String username;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String module;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 200)
    private String target;

    @Column(length = 100)
    private String targetType;

    @Column(length = 100)
    private String targetId;

    @Column(length = 100)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String requestMethod;

    @Column(columnDefinition = "TEXT")
    private String requestUrl;

    @Column(columnDefinition = "TEXT")
    private String requestParams;

    @Column(columnDefinition = "TEXT")
    private String requestBody;

    @Column(columnDefinition = "TEXT")
    private String responseData;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 500)
    private String message;

    private long ts;

    private long duration;

    private String traceId;

    /**
     * 默认构造函数，初始化时间戳
     */
    public AuditLog(String username, String module, String action, String status) {
        this.ts = System.currentTimeMillis() / 1000;
        this.username = username;
        this.module = module;
        this.action = action;
        this.status = status;
    }
}