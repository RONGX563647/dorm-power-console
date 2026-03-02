package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * 宿舍房间模型
 */
@Entity
@Table(name = "dorm_rooms")
public class DormRoom {

    @Id
    private String id;

    @NotNull
    private String buildingId; // 楼栋ID

    @NotNull
    private int floor; // 楼层

    @NotNull
    private String roomNumber; // 房间号

    private String roomType; // 房间类型：SINGLE(单人间)、DOUBLE(双人间)、QUAD(四人间)

    private int capacity; // 容纳人数

    private int currentOccupants; // 当前入住人数

    private double electricityQuota; // 用电配额（度/月）

    private String deviceId; // 关联的设备ID

    private String priceRuleId; // 应用的电价规则ID

    @NotNull
    private String status; // 状态：VACANT(空闲)、OCCUPIED(已入住)、MAINTENANCE(维修中)

    private String remark; // 备注

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

    public String getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(String buildingId) {
        this.buildingId = buildingId;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getCurrentOccupants() {
        return currentOccupants;
    }

    public void setCurrentOccupants(int currentOccupants) {
        this.currentOccupants = currentOccupants;
    }

    public double getElectricityQuota() {
        return electricityQuota;
    }

    public void setElectricityQuota(double electricityQuota) {
        this.electricityQuota = electricityQuota;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPriceRuleId() {
        return priceRuleId;
    }

    public void setPriceRuleId(String priceRuleId) {
        this.priceRuleId = priceRuleId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
