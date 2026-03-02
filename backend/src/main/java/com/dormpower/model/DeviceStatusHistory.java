package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * 设备状态历史模型
 * 用于记录设备状态的变更历史
 */
@Entity
@Table(name = "device_status_history")
public class DeviceStatusHistory {

    @Id
    private String id;

    @NotNull
    private String deviceId;

    @NotNull
    private boolean online;

    private double totalPowerW;

    private double voltageV;

    private double currentA;

    private String socketsJson;

    @NotNull
    private long ts;

    @NotNull
    private long createdAt;

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

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public double getTotalPowerW() {
        return totalPowerW;
    }

    public void setTotalPowerW(double totalPowerW) {
        this.totalPowerW = totalPowerW;
    }

    public double getVoltageV() {
        return voltageV;
    }

    public void setVoltageV(double voltageV) {
        this.voltageV = voltageV;
    }

    public double getCurrentA() {
        return currentA;
    }

    public void setCurrentA(double currentA) {
        this.currentA = currentA;
    }

    public String getSocketsJson() {
        return socketsJson;
    }

    public void setSocketsJson(String socketsJson) {
        this.socketsJson = socketsJson;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

}
