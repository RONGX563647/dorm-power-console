package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 命令记录模型
 *
 * 表示设备控制命令记录实体，映射到数据库command_record表。
 * 记录所有下发的控制命令及其执行状态。
 *
 * @author dormpower team
 * @version 1.0
 */
@Entity
@Table(name = "command_record", indexes = {
        @Index(name = "idx_cmd_device_id", columnList = "deviceId"),
        @Index(name = "idx_cmd_state", columnList = "state"),
        @Index(name = "idx_cmd_state_expires", columnList = "state, expiresAt"),
        @Index(name = "idx_cmd_device_created", columnList = "deviceId, createdAt")
})
@Getter
@Setter
@NoArgsConstructor
public class CommandRecord {

    /** 命令唯一标识符，格式：cmd-{timestamp} */
    @Id
    private String cmdId;

    /** 目标设备ID */
    @NotNull
    private String deviceId;

    /** 插座编号（null表示控制所有插座） */
    private Integer socket;

    /** 命令类型（toggle、on、off等） */
    @NotNull
    private String action;

    /** 命令负载数据（JSON格式） */
    @NotNull
    private String payloadJson;

    /** 命令状态（pending、success、failed、timeout） */
    @NotNull
    private String state;

    /** 状态消息或错误信息 */
    @NotNull
    private String message;

    /** 创建时间戳（毫秒） */
    @NotNull
    private long createdAt;

    /** 更新时间戳（毫秒） */
    @NotNull
    private long updatedAt;

    /** 超时时间戳（毫秒） */
    @NotNull
    private long expiresAt;

    /** 执行时长（毫秒，仅成功/失败时有效） */
    private Integer durationMs;
}