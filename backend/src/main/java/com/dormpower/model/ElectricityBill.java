package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 电费账单模型
 */
@Entity
@Table(name = "electricity_bills")
@Getter
@Setter
@NoArgsConstructor
public class ElectricityBill {

    @Id
    private String id;

    /** 房间ID */
    @NotNull
    private String roomId;

    /** 账单周期：2024-01 */
    @NotNull
    private String period;

    /** 总用电量（度） */
    @NotNull
    private double totalConsumption;

    /** 总金额（元） */
    @NotNull
    private double totalAmount;

    /** 峰时用电量 */
    private double peakConsumption;

    /** 峰时电费 */
    private double peakAmount;

    /** 谷时用电量 */
    private double valleyConsumption;

    /** 谷时电费 */
    private double valleyAmount;

    /** 平时用电量 */
    private double flatConsumption;

    /** 平时电费 */
    private double flatAmount;

    /** 账单状态：PENDING(待缴费)、PAID(已缴费)、OVERDUE(逾期) */
    @NotNull
    private String status;

    /** 缴费时间 */
    private long paidAt;

    /** 缴费方式：CASH(现金)、WECHAT(微信)、ALIPAY(支付宝) */
    private String paymentMethod;

    /** 交易流水号 */
    private String transactionId;

    /** 账单开始日期 */
    @NotNull
    private long startDate;

    /** 账单结束日期 */
    @NotNull
    private long endDate;

    @NotNull
    private long createdAt;

    private long updatedAt;
}