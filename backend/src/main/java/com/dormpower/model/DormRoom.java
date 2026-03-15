package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 宿舍房间模型
 */
@Entity
@Table(name = "dorm_rooms")
@Getter
@Setter
@NoArgsConstructor
public class DormRoom {

    @Id
    private String id;

    /** 楼栋ID */
    @NotNull
    private String buildingId;

    /** 楼层 */
    @NotNull
    private int floor;

    /** 房间号 */
    @NotNull
    private String roomNumber;

    /** 房间类型：SINGLE(单人间)、DOUBLE(双人间)、QUAD(四人间) */
    private String roomType;

    /** 容纳人数 */
    private int capacity;

    /** 当前入住人数 */
    private int currentOccupants;

    /** 用电配额（度/月） */
    private double electricityQuota;

    /** 关联的设备ID */
    private String deviceId;

    /** 应用的电价规则ID */
    private String priceRuleId;

    /** 状态：VACANT(空闲)、OCCUPIED(已入住)、MAINTENANCE(维修中) */
    @NotNull
    private String status;

    /** 备注 */
    private String remark;

    /** 是否启用 */
    @NotNull
    private boolean enabled;

    @NotNull
    private long createdAt;

    private long updatedAt;
}