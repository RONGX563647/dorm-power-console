package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * 电费账单模型
 */
@Entity
@Table(name = "electricity_bills")
public class ElectricityBill {

    @Id
    private String id;

    @NotNull
    private String roomId; // 房间ID

    @NotNull
    private String period; // 账单周期：2024-01

    @NotNull
    private double totalConsumption; // 总用电量（度）

    @NotNull
    private double totalAmount; // 总金额（元）

    private double peakConsumption; // 峰时用电量
    private double peakAmount; // 峰时电费

    private double valleyConsumption; // 谷时用电量
    private double valleyAmount; // 谷时电费

    private double flatConsumption; // 平时用电量
    private double flatAmount; // 平时电费

    @NotNull
    private String status; // 账单状态：PENDING(待缴费)、PAID(已缴费)、OVERDUE(逾期)

    private long paidAt; // 缴费时间

    private String paymentMethod; // 缴费方式：CASH(现金)、WECHAT(微信)、ALIPAY(支付宝)

    private String transactionId; // 交易流水号

    @NotNull
    private long startDate; // 账单开始日期

    @NotNull
    private long endDate; // 账单结束日期

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

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public double getTotalConsumption() {
        return totalConsumption;
    }

    public void setTotalConsumption(double totalConsumption) {
        this.totalConsumption = totalConsumption;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getPeakConsumption() {
        return peakConsumption;
    }

    public void setPeakConsumption(double peakConsumption) {
        this.peakConsumption = peakConsumption;
    }

    public double getPeakAmount() {
        return peakAmount;
    }

    public void setPeakAmount(double peakAmount) {
        this.peakAmount = peakAmount;
    }

    public double getValleyConsumption() {
        return valleyConsumption;
    }

    public void setValleyConsumption(double valleyConsumption) {
        this.valleyConsumption = valleyConsumption;
    }

    public double getValleyAmount() {
        return valleyAmount;
    }

    public void setValleyAmount(double valleyAmount) {
        this.valleyAmount = valleyAmount;
    }

    public double getFlatConsumption() {
        return flatConsumption;
    }

    public void setFlatConsumption(double flatConsumption) {
        this.flatConsumption = flatConsumption;
    }

    public double getFlatAmount() {
        return flatAmount;
    }

    public void setFlatAmount(double flatAmount) {
        this.flatAmount = flatAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(long paidAt) {
        this.paidAt = paidAt;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
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
