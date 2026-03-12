package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * 插座状态模型
 */
@Entity
@Table(name = "strip_status")
public class StripStatus {

    @Id
    private String deviceId;

    @NotNull
    private long ts;

    @NotNull
    private boolean online;

    @NotNull
    private double totalPowerW;

    @NotNull
    private double voltageV;

    @NotNull
    private double currentA;

    @NotNull
    private String socketsJson;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
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

}
