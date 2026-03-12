package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * 命令记录模型
 * 
 * 表示设备控制命令记录实体，映射到数据库command_record表。
 * 记录所有下发的控制命令及其执行状态。
 * 
 * 字段说明：
 * - cmdId：命令唯一标识符
 * - deviceId：目标设备ID
 * - socket：插座编号（null表示控制所有插座）
 * - action：命令类型（toggle、on、off等）
 * - payloadJson：命令负载数据（JSON格式）
 * - state：命令状态（pending、success、failed、timeout）
 * - message：状态消息或错误信息
 * - createdAt：创建时间戳（毫秒）
 * - updatedAt：更新时间戳（毫秒）
 * - expiresAt：超时时间戳（毫秒）
 * - durationMs：执行时长（毫秒，仅成功/失败时有效）
 * 
 * 索引说明：
 * - idx_cmd_device_id：按设备ID查询
 * - idx_cmd_state：按状态查询
 * - idx_cmd_state_expires：查询待处理超时命令
 * - idx_cmd_device_created：按设备和创建时间查询
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
public class CommandRecord {

    // 命令唯一标识符，格式：cmd-{timestamp}
    @Id
    private String cmdId;

    // 目标设备ID
    @NotNull
    private String deviceId;

    // 插座编号（null表示控制所有插座）
    private Integer socket;

    // 命令类型（toggle、on、off等）
    @NotNull
    private String action;

    // 命令负载数据（JSON格式）
    @NotNull
    private String payloadJson;

    // 命令状态（pending、success、failed、timeout）
    @NotNull
    private String state;

    // 状态消息或错误信息
    @NotNull
    private String message;

    // 创建时间戳（毫秒）
    @NotNull
    private long createdAt;

    // 更新时间戳（毫秒）
    @NotNull
    private long updatedAt;

    // 超时时间戳（毫秒）
    @NotNull
    private long expiresAt;

    // 执行时长（毫秒，仅成功/失败时有效）
    private Integer durationMs;

    // Getters and setters
    public String getCmdId() {
        return cmdId;
    }

    public void setCmdId(String cmdId) {
        this.cmdId = cmdId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getSocket() {
        return socket;
    }

    public void setSocket(Integer socket) {
        this.socket = socket;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }

}