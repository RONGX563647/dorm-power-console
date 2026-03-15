package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 遥测数据模型
 */
@Entity
@Table(name = "telemetry", indexes = {
        @Index(name = "idx_telemetry_device_id", columnList = "deviceId"),
        @Index(name = "idx_telemetry_ts", columnList = "ts"),
        @Index(name = "idx_telemetry_device_ts", columnList = "deviceId, ts")
})
@Getter
@Setter
@NoArgsConstructor
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
}