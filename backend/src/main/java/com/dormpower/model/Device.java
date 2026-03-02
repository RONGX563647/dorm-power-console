package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

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
public class Device {

    // 设备唯一标识符
    @Id
    private String id;

    // 设备名称
    @NotNull
    private String name;

    // 所在房间号
    @NotNull
    private String room;

    // 在线状态：true-在线，false-离线
    @NotNull
    private boolean online;

    // 最后心跳时间戳（Unix时间戳，秒）
    @NotNull
    private long lastSeenTs;

    // 创建时间戳（Unix时间戳，秒）
    @NotNull
    private long createdAt;

    // Getters and setters
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

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getLastSeenTs() {
        return lastSeenTs;
    }

    public void setLastSeenTs(long lastSeenTs) {
        this.lastSeenTs = lastSeenTs;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

}