package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * 房间余额模型
 */
@Entity
@Table(name = "room_balances")
public class RoomBalance {

    @Id
    private String id;

    @NotNull
    private String roomId; // 房间ID

    @NotNull
    private double balance; // 当前余额

    @NotNull
    private double totalRecharged; // 累计充值金额

    @NotNull
    private double totalConsumed; // 累计消费金额

    private double warningThreshold; // 余额预警阈值

    private boolean warningSent; // 是否已发送预警

    private boolean autoCutoff; // 是否自动断电

    @NotNull
    private long lastRechargeAt; // 最后充值时间

    @NotNull
    private long lastConsumptionAt; // 最后消费时间

    @NotNull
    private long createdAt;

    private long updatedAt;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double getTotalRecharged() {
        return totalRecharged;
    }

    public void setTotalRecharged(double totalRecharged) {
        this.totalRecharged = totalRecharged;
    }

    public double getTotalConsumed() {
        return totalConsumed;
    }

    public void setTotalConsumed(double totalConsumed) {
        this.totalConsumed = totalConsumed;
    }

    public double getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(double warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public boolean isWarningSent() {
        return warningSent;
    }

    public void setWarningSent(boolean warningSent) {
        this.warningSent = warningSent;
    }

    public boolean isAutoCutoff() {
        return autoCutoff;
    }

    public void setAutoCutoff(boolean autoCutoff) {
        this.autoCutoff = autoCutoff;
    }

    public long getLastRechargeAt() {
        return lastRechargeAt;
    }

    public void setLastRechargeAt(long lastRechargeAt) {
        this.lastRechargeAt = lastRechargeAt;
    }

    public long getLastConsumptionAt() {
        return lastConsumptionAt;
    }

    public void setLastConsumptionAt(long lastConsumptionAt) {
        this.lastConsumptionAt = lastConsumptionAt;
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
}
