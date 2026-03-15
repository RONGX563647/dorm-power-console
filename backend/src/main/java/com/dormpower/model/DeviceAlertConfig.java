package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 设备告警配置模型
 * 用于存储设备的告警阈值设置
 */
@Entity
@Table(name = "device_alert_configs")
@Getter
@Setter
@NoArgsConstructor
public class DeviceAlertConfig {

    @Id
    private String id;

    @NotNull
    private String deviceId;

    /** 告警类型：power, voltage, current, online等 */
    @NotNull
    private String type;

    private double thresholdMin;

    private double thresholdMax;

    @NotNull
    private boolean enabled;

    @NotNull
    private long createdAt;

    @NotNull
    private long updatedAt;
}