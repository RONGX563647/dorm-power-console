package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 学生入住历史记录模型
 * 记录学生的入住和退宿历史
 */
@Entity
@Table(name = "student_room_history")
@Getter
@Setter
@NoArgsConstructor
public class StudentRoomHistory {

    @Id
    private String id;

    /** 学生ID */
    @NotNull
    private String studentId;

    /** 房间ID */
    @NotNull
    private String roomId;

    /** 入住日期 */
    @NotNull
    private long checkInDate;

    /** 退宿日期 */
    private long checkOutDate;

    /** 状态：ACTIVE(在住)、CHECKED_OUT(已退宿) */
    @NotNull
    private String status;

    /** 入住原因 */
    private String checkInReason;

    /** 退宿原因 */
    private String checkOutReason;

    /** 操作员 */
    private String operator;

    /** 期间用电量 */
    private double electricityUsage;

    /** 期间电费 */
    private double electricityCost;

    /** 备注 */
    private String remark;

    @NotNull
    private long createdAt;
}