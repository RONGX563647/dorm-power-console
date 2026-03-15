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
 * 登录日志实体
 */
@Entity
@Table(name = "login_log", indexes = {
    @Index(name = "idx_login_username", columnList = "username"),
    @Index(name = "idx_login_ts", columnList = "loginTs"),
    @Index(name = "idx_login_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String username;

    @Column(length = 50)
    private String loginType;

    @Column(length = 50)
    private String loginMethod;

    @Column(length = 100)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 100)
    private String browser;

    @Column(length = 100)
    private String os;

    @Column(length = 200)
    private String location;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 500)
    private String message;

    private long loginTs = System.currentTimeMillis() / 1000;

    private Long logoutTs;

    private Long duration;

    private String sessionId;

    private String deviceId;

    /**
     * 便捷构造函数
     */
    public LoginLog(String username, String status) {
        this.loginTs = System.currentTimeMillis() / 1000;
        this.username = username;
        this.status = status;
    }
}