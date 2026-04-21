package com.dormpower.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * 告警消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 告警 ID
     */
    private String alertId;

    /**
     * 设备 ID
     */
    private String deviceId;

    /**
     * 告警类型：POWER_OVERLOAD, VOLTAGE_ABNORMAL, CURRENT_ABNORMAL, OFFLINE
     */
    private String alertType;

    /**
     * 告警级别：INFO, WARNING, CRITICAL
     */
    private String severity;

    /**
     * 告警值
     */
    private double value;

    /**
     * 阈值
     */
    private double threshold;

    /**
     * 告警时间
     */
    private Instant timestamp;

    /**
     * 告警描述
     */
    private String message;

    /**
     * 房间号
     */
    private String room;

    /**
     * 楼栋号
     */
    private String building;
}
