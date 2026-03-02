package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * 电价规则模型
 * 支持阶梯电价和时段电价
 */
@Entity
@Table(name = "electricity_price_rules")
public class ElectricityPriceRule {

    @Id
    private String id;

    @NotNull
    private String name; // 规则名称

    @NotNull
    private String type; // 规则类型：TIER(阶梯)、TIME(时段)、MIXED(混合)

    private String description; // 规则描述

    @NotNull
    private double basePrice; // 基础电价（元/度）

    private double tier1Price; // 第一阶梯电价
    private double tier1Limit; // 第一阶梯上限（度）

    private double tier2Price; // 第二阶梯电价
    private double tier2Limit; // 第二阶梯上限（度）

    private double tier3Price; // 第三阶梯电价

    private double peakPrice; // 峰时电价
    private double valleyPrice; // 谷时电价
    private double flatPrice; // 平时电价

    private int peakStartHour; // 峰时开始时间（0-23）
    private int peakEndHour; // 峰时结束时间（0-23）
    private int valleyStartHour; // 谷时开始时间（0-23）
    private int valleyEndHour; // 谷时结束时间（0-23）

    @NotNull
    private boolean enabled; // 是否启用

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public double getTier1Price() {
        return tier1Price;
    }

    public void setTier1Price(double tier1Price) {
        this.tier1Price = tier1Price;
    }

    public double getTier1Limit() {
        return tier1Limit;
    }

    public void setTier1Limit(double tier1Limit) {
        this.tier1Limit = tier1Limit;
    }

    public double getTier2Price() {
        return tier2Price;
    }

    public void setTier2Price(double tier2Price) {
        this.tier2Price = tier2Price;
    }

    public double getTier2Limit() {
        return tier2Limit;
    }

    public void setTier2Limit(double tier2Limit) {
        this.tier2Limit = tier2Limit;
    }

    public double getTier3Price() {
        return tier3Price;
    }

    public void setTier3Price(double tier3Price) {
        this.tier3Price = tier3Price;
    }

    public double getPeakPrice() {
        return peakPrice;
    }

    public void setPeakPrice(double peakPrice) {
        this.peakPrice = peakPrice;
    }

    public double getValleyPrice() {
        return valleyPrice;
    }

    public void setValleyPrice(double valleyPrice) {
        this.valleyPrice = valleyPrice;
    }

    public double getFlatPrice() {
        return flatPrice;
    }

    public void setFlatPrice(double flatPrice) {
        this.flatPrice = flatPrice;
    }

    public int getPeakStartHour() {
        return peakStartHour;
    }

    public void setPeakStartHour(int peakStartHour) {
        this.peakStartHour = peakStartHour;
    }

    public int getPeakEndHour() {
        return peakEndHour;
    }

    public void setPeakEndHour(int peakEndHour) {
        this.peakEndHour = peakEndHour;
    }

    public int getValleyStartHour() {
        return valleyStartHour;
    }

    public void setValleyStartHour(int valleyStartHour) {
        this.valleyStartHour = valleyStartHour;
    }

    public int getValleyEndHour() {
        return valleyEndHour;
    }

    public void setValleyEndHour(int valleyEndHour) {
        this.valleyEndHour = valleyEndHour;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
