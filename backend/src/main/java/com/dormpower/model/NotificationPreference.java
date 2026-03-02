package com.dormpower.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 用户通知偏好设置实体
 * 存储用户对各类通知的偏好设置
 */
@Entity
@Table(name = "notification_preferences", indexes = {
    @Index(name = "idx_notification_pref_user", columnList = "username", unique = true)
})
public class NotificationPreference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(nullable = false, unique = true)
    private String username;
    
    @NotNull
    private boolean emailEnabled;
    
    @NotNull
    private boolean systemEnabled;
    
    @NotNull
    private boolean alertEnabled;
    
    @NotNull
    private boolean billingEnabled;
    
    @NotNull
    private boolean maintenanceEnabled;
    
    @NotNull
    private boolean quietHoursEnabled;
    
    private String quietHoursStart;
    
    private String quietHoursEnd;
    
    @NotNull
    private String alertLevel;
    
    @NotNull
    private long createdAt;
    
    @NotNull
    private long updatedAt;
    
    public NotificationPreference() {
        this.emailEnabled = true;
        this.systemEnabled = true;
        this.alertEnabled = true;
        this.billingEnabled = true;
        this.maintenanceEnabled = true;
        this.quietHoursEnabled = false;
        this.quietHoursStart = "22:00";
        this.quietHoursEnd = "08:00";
        this.alertLevel = "warning";
        this.createdAt = System.currentTimeMillis() / 1000;
        this.updatedAt = System.currentTimeMillis() / 1000;
    }
    
    public NotificationPreference(String username) {
        this();
        this.username = username;
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
    
    public boolean isEmailEnabled() {
        return emailEnabled;
    }
    
    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }
    
    public boolean isSystemEnabled() {
        return systemEnabled;
    }
    
    public void setSystemEnabled(boolean systemEnabled) {
        this.systemEnabled = systemEnabled;
    }
    
    public boolean isAlertEnabled() {
        return alertEnabled;
    }
    
    public void setAlertEnabled(boolean alertEnabled) {
        this.alertEnabled = alertEnabled;
    }
    
    public boolean isBillingEnabled() {
        return billingEnabled;
    }
    
    public void setBillingEnabled(boolean billingEnabled) {
        this.billingEnabled = billingEnabled;
    }
    
    public boolean isMaintenanceEnabled() {
        return maintenanceEnabled;
    }
    
    public void setMaintenanceEnabled(boolean maintenanceEnabled) {
        this.maintenanceEnabled = maintenanceEnabled;
    }
    
    public boolean isQuietHoursEnabled() {
        return quietHoursEnabled;
    }
    
    public void setQuietHoursEnabled(boolean quietHoursEnabled) {
        this.quietHoursEnabled = quietHoursEnabled;
    }
    
    public String getQuietHoursStart() {
        return quietHoursStart;
    }
    
    public void setQuietHoursStart(String quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }
    
    public String getQuietHoursEnd() {
        return quietHoursEnd;
    }
    
    public void setQuietHoursEnd(String quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }
    
    public String getAlertLevel() {
        return alertLevel;
    }
    
    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
