package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 设备告警模型
 * 用于存储设备异常告警信息
 */
@Entity
@Table(name = "device_alerts")
@Getter
@Setter
@NoArgsConstructor
public class DeviceAlert {

    @Id
    private String id;

    @NotNull
    private String deviceId;

    /** 告警类型：power, voltage, current, online等 */
    @NotNull
    private String type;

    /** 告警级别：info, warning, error, critical */
    @NotNull
    private String level;

    @NotNull
    private String message;

    private double thresholdValue;

    private double actualValue;

    @NotNull
    private boolean resolved;

    @NotNull
    private long ts;

    @NotNull
    private long createdAt;

    private long resolvedAt;
}