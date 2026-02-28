package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * 遥测数据模型
 */
@Entity
@Table(name = "telemetry", indexes = {
        @Index(name = "idx_telemetry_device_id", columnList = "deviceId"),
        @Index(name = "idx_telemetry_ts", columnList = "ts")
})
public class Telemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String deviceId;

    @NotNull
    private long ts;

    @NotNull
    private double powerW;

    @NotNull
    private double voltageV;

    @NotNull
    private double currentA;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public double getPowerW() {
        return powerW;
    }

    public void setPowerW(double powerW) {
        this.powerW = powerW;
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

}