package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 定时任务模型
 * 用于存储设备的定时开关任务
 */
@Entity
@Table(name = "scheduled_tasks")
@Getter
@Setter
@NoArgsConstructor
public class ScheduledTask {

    @Id
    private String id;

    @NotNull
    private String deviceId;

    /** 任务类型：power_on, power_off, socket_on, socket_off */
    @NotNull
    private String type;

    /** 插座ID，仅在socket_*类型时有效 */
    private int socketId;

    /** 计划执行时间戳 */
    @NotNull
    private long scheduledTime;

    /** cron表达式，用于重复任务 */
    @NotNull
    private String cronExpression;

    /** 是否启用 */
    @NotNull
    private boolean enabled;

    /** 是否重复执行 */
    @NotNull
    private boolean recurring;

    @NotNull
    private long createdAt;

    @NotNull
    private long updatedAt;

    /** 最后执行时间 */
    private long lastExecutedAt;

    /** 最后执行状态 */
    private String lastStatus;
}