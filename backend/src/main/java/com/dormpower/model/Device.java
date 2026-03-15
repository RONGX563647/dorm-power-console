package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 设备模型
 *
 * 表示智能插座设备实体，映射到数据库devices表。
 * 包含设备的基本信息和在线状态。
 *
 * 字段说明：
 * - id：设备唯一标识符
 * - name：设备名称
 * - room：所在房间号
 * - online：在线状态
 * - lastSeenTs：最后心跳时间戳（秒）
 * - createdAt：创建时间戳（秒）
 *
 * @author dormpower team
 * @version 1.0
 */
@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
public class Device {

    /** 设备唯一标识符 */
    @Id
    private String id;

    /** 设备名称 */
    @NotNull
    private String name;

    /** 所在房间号 */
    @NotNull
    private String room;

    /** 在线状态：true-在线，false-离线 */
    @NotNull
    private boolean online;

    /** 最后心跳时间戳（Unix时间戳，秒） */
    @NotNull
    private long lastSeenTs;

    /** 创建时间戳（Unix时间戳，秒） */
    @NotNull
    private long createdAt;
}