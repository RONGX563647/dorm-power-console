package com.dormpower.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * 设备固件实体
 */
@Entity
@Table(name = "device_firmware", indexes = {
    @Index(name = "idx_firmware_device", columnList = "deviceId"),
    @Index(name = "idx_firmware_version", columnList = "version")
})
public class DeviceFirmware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String deviceId;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String version;

    @Column(length = 50)
    private String previousVersion;

    @Column(length = 200)
    private String filePath;

    @Column(length = 500)
    private String description;

    @Column(length = 64)
    private String checksum;

    private long fileSize;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String status;

    private int progress = 0;

    @Column(length = 500)
    private String errorMessage;

    private String initiatedBy;

    private long startedAt;

    private long completedAt;

    private long createdAt;

    public DeviceFirmware() {
        this.createdAt = System.currentTimeMillis() / 1000;
        this.status = "PENDING";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPreviousVersion() {
        return previousVersion;
    }

    public void setPreviousVersion(String previousVersion) {
        this.previousVersion = previousVersion;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
