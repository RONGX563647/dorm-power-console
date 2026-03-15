package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 设备状态历史模型
 * 用于记录设备状态的变更历史
 */
@Entity
@Table(name = "device_status_history")
@Getter
@Setter
@NoArgsConstructor
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
}