package com.dormpower.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录日志实体
 */
@Entity
@Table(name = "login_log", indexes = {
    @Index(name = "idx_login_username", columnList = "username"),
    @Index(name = "idx_login_ts", columnList = "loginTs"),
    @Index(name = "idx_login_status", columnList = "status")
})
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

    private long loginTs;

    private Long logoutTs;

    private Long duration;

    private String sessionId;

    private String deviceId;

    public LoginLog() {
        this.loginTs = System.currentTimeMillis() / 1000;
    }

    public LoginLog(String username, String status) {
        this();
        this.username = username;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public String getLoginMethod() {
        return loginMethod;
    }

    public void setLoginMethod(String loginMethod) {
        this.loginMethod = loginMethod;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getLoginTs() {
        return loginTs;
    }

    public void setLoginTs(long loginTs) {
        this.loginTs = loginTs;
    }

    public Long getLogoutTs() {
        return logoutTs;
    }

    public void setLogoutTs(Long logoutTs) {
        this.logoutTs = logoutTs;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
