package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * 设备模型
 */
@Entity
@Table(name = "devices")
public class Device {

    @Id
    private String id;

    @NotNull
    private String name;

    @NotNull
    private String room;

    @NotNull
    private boolean online;

    @NotNull
    private long lastSeenTs;

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