package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 房间余额模型
 */
@Entity
@Table(name = "room_balances")
@Getter
@Setter
@NoArgsConstructor
public class RoomBalance {

    @Id
    private String id;

    /** 房间ID */
    @NotNull
    private String roomId;

    /** 当前余额 */
    @NotNull
    private double balance;

    /** 累计充值金额 */
    @NotNull
    private double totalRecharged;

    /** 累计消费金额 */
    @NotNull
    private double totalConsumed;

    /** 余额预警阈值 */
    private double warningThreshold;

    /** 是否已发送预警 */
    private boolean warningSent;

    /** 是否自动断电 */
    private boolean autoCutoff;

    /** 最后充值时间 */
    @NotNull
    private long lastRechargeAt;

    /** 最后消费时间 */
    @NotNull
    private long lastConsumptionAt;

    @NotNull
    private long createdAt;

    private long updatedAt;
}