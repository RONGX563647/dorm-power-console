package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 设备分组关联模型
 * 用于设备和分组之间的多对多关系
 */
@Entity
@Table(name = "device_group_mappings")
@Getter
@Setter
@NoArgsConstructor
public class DeviceGroupMapping {

    @Id
    private String id;

    @NotNull
    private String deviceId;

    @NotNull
    private String groupId;

    @NotNull
    private long createdAt;
}