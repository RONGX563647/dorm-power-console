package com.dormpower.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * IP访问控制实体
 */
@Entity
@Table(name = "ip_access_control", indexes = {
    @Index(name = "idx_ip_address", columnList = "ipAddress", unique = true),
    @Index(name = "idx_ip_type", columnList = "type")
})
public class IpAccessControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String ipAddress;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String type;

    @Column(length = 200)
    private String description;

    private boolean enabled = true;

    private long expiresAt;

    private String createdBy;

    private long createdAt;

    private long updatedAt;

    public IpAccessControl() {
        long now = System.currentTimeMillis() / 1000;
        this.createdAt = now;
        this.updatedAt = now;
        this.expiresAt = 0;
    }

    public IpAccessControl(String ipAddress, String type) {
        this();
        this.ipAddress = ipAddress;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
