package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * 设备告警配置模型
 * 用于存储设备的告警阈值设置
 */
@Entity
@Table(name = "device_alert_configs")
public class DeviceAlertConfig {

    @Id
    private String id;

    @NotNull
    private String deviceId;

    @NotNull
    private String type; // 告警类型：power, voltage, current, online等

    private double thresholdMin;

    private double thresholdMax;

    @NotNull
    private boolean enabled;

    @NotNull
    private long createdAt;

    @NotNull
    private long updatedAt;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getThresholdMin() {
        return thresholdMin;
    }

    public void setThresholdMin(double thresholdMin) {
        this.thresholdMin = thresholdMin;
    }

    public double getThresholdMax() {
        return thresholdMax;
    }

    public void setThresholdMax(double thresholdMax) {
        this.thresholdMax = thresholdMax;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
