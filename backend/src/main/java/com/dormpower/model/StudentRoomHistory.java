package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * 学生入住历史记录模型
 * 记录学生的入住和退宿历史
 */
@Entity
@Table(name = "student_room_history")
public class StudentRoomHistory {

    @Id
    private String id;

    @NotNull
    private String studentId; // 学生ID

    @NotNull
    private String roomId; // 房间ID

    @NotNull
    private long checkInDate; // 入住日期

    private long checkOutDate; // 退宿日期

    @NotNull
    private String status; // 状态：ACTIVE(在住)、CHECKED_OUT(已退宿)

    private String checkInReason; // 入住原因

    private String checkOutReason; // 退宿原因

    private String operator; // 操作员

    private double electricityUsage; // 期间用电量

    private double electricityCost; // 期间电费

    private String remark; // 备注

    @NotNull
    private long createdAt;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public long getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(long checkInDate) {
        this.checkInDate = checkInDate;
    }

    public long getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(long checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCheckInReason() {
        return checkInReason;
    }

    public void setCheckInReason(String checkInReason) {
        this.checkInReason = checkInReason;
    }

    public String getCheckOutReason() {
        return checkOutReason;
    }

    public void setCheckOutReason(String checkOutReason) {
        this.checkOutReason = checkOutReason;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public double getElectricityUsage() {
        return electricityUsage;
    }

    public void setElectricityUsage(double electricityUsage) {
        this.electricityUsage = electricityUsage;
    }

    public double getElectricityCost() {
        return electricityCost;
    }

    public void setElectricityCost(double electricityCost) {
        this.electricityCost = electricityCost;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
