package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * 定时任务模型
 * 用于存储设备的定时开关任务
 */
@Entity
@Table(name = "scheduled_tasks")
public class ScheduledTask {

    @Id
    private String id;

    @NotNull
    private String deviceId;

    @NotNull
    private String type; // 任务类型：power_on, power_off, socket_on, socket_off

    private int socketId; // 插座ID，仅在socket_*类型时有效

    @NotNull
    private long scheduledTime; // 计划执行时间戳

    @NotNull
    private String cronExpression; // cron表达式，用于重复任务

    @NotNull
    private boolean enabled; // 是否启用

    @NotNull
    private boolean recurring; // 是否重复执行

    @NotNull
    private long createdAt;

    @NotNull
    private long updatedAt;

    private long lastExecutedAt; // 最后执行时间

    private String lastStatus; // 最后执行状态

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getSocketId() {
        return socketId;
    }

    public void setSocketId(int socketId) {
        this.socketId = socketId;
    }

    public long getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
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

    public long getLastExecutedAt() {
        return lastExecutedAt;
    }

    public void setLastExecutedAt(long lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
    }

}
