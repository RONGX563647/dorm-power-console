package com.dormpower.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 命令确认消息模型
 *
 * 用于 Kafka 异步命令确认处理的传输对象
 *
 * @author dormpower team
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
public class CommandAckMessage {

    /** 设备 ID */
    private String deviceId;

    /** 命令 ID */
    private String cmdId;

    /** 状态：success, failed, pending */
    private String status;

    /** 消息 */
    private String message;

    /** 插座 ID */
    private Integer socket;

    /** 插座状态 */
    private String socketState;

    /** 时间戳 */
    private Long timestamp = System.currentTimeMillis();
}