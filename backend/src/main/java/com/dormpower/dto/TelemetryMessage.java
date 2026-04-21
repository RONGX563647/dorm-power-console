package com.dormpower.mqtt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 遥测数据消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备 ID
     */
    private String deviceId;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 功率 (瓦)
     */
    private double powerW;

    /**
     * 电压 (伏)
     */
    private double voltageV;

    /**
     * 电流 (安)
     */
    private double currentA;

    /**
     * 房间号
     */
    private String room;

    /**
     * 楼栋号
     */
    private String building;
}
