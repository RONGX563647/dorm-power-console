package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 插座状态模型
 */
@Entity
@Table(name = "strip_status")
@Getter
@Setter
@NoArgsConstructor
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
}