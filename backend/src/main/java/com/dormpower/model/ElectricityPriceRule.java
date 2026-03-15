package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 电价规则模型
 * 支持阶梯电价和时段电价
 */
@Entity
@Table(name = "electricity_price_rules")
@Getter
@Setter
@NoArgsConstructor
public class ElectricityPriceRule {

    @Id
    private String id;

    /** 规则名称 */
    @NotNull
    private String name;

    /** 规则类型：TIER(阶梯)、TIME(时段)、MIXED(混合) */
    @NotNull
    private String type;

    /** 规则描述 */
    private String description;

    /** 基础电价（元/度） */
    @NotNull
    private double basePrice;

    /** 第一阶梯电价 */
    private double tier1Price;

    /** 第一阶梯上限（度） */
    private double tier1Limit;

    /** 第二阶梯电价 */
    private double tier2Price;

    /** 第二阶梯上限（度） */
    private double tier2Limit;

    /** 第三阶梯电价 */
    private double tier3Price;

    /** 峰时电价 */
    private double peakPrice;

    /** 谷时电价 */
    private double valleyPrice;

    /** 平时电价 */
    private double flatPrice;

    /** 峰时开始时间（0-23） */
    private int peakStartHour;

    /** 峰时结束时间（0-23） */
    private int peakEndHour;

    /** 谷时开始时间（0-23） */
    private int valleyStartHour;

    /** 谷时结束时间（0-23） */
    private int valleyEndHour;

    /** 是否启用 */
    @NotNull
    private boolean enabled;

    @NotNull
    private long createdAt;

    private long updatedAt;
}