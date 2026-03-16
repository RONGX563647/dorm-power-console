package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 设备分组模型
 */
@Entity
@Table(name = "device_groups")
@Getter
@Setter
@NoArgsConstructor
public class DeviceGroup {

    @Id
    private String id;

    @NotNull
    private String name;

    /** 分组类型：room, floor, building等 */
    @NotNull
    private String type;

    /** 父分组ID，用于层级结构 */
    @NotNull
    private String parentId;

    @NotNull
    private long createdAt;
}