package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 充值记录模型
 */
@Entity
@Table(name = "recharge_records")
@Getter
@Setter
@NoArgsConstructor
public class RechargeRecord {

    @Id
    private String id;

    /** 房间ID */
    @NotNull
    private String roomId;

    /** 充值金额 */
    @NotNull
    private double amount;

    /** 充值前余额 */
    @NotNull
    private double balanceBefore;

    /** 充值后余额 */
    @NotNull
    private double balanceAfter;

    /** 支付方式：CASH、WECHAT、ALIPAY */
    @NotNull
    private String paymentMethod;

    /** 第三方交易号 */
    private String transactionId;

    /** 状态：SUCCESS、FAILED、PENDING */
    @NotNull
    private String status;

    /** 操作员 */
    private String operator;

    /** 备注 */
    private String remark;

    @NotNull
    private long createdAt;
}