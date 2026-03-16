package com.dormpower.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 设备固件实体
 */
@Entity
@Table(name = "device_firmware", indexes = {
    @Index(name = "idx_firmware_device", columnList = "deviceId"),
    @Index(name = "idx_firmware_version", columnList = "version")
})
@Getter
@Setter
@NoArgsConstructor
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
    private String status = "PENDING";

    private int progress = 0;

    @Column(length = 500)
    private String errorMessage;

    private String initiatedBy;

    private long startedAt;

    private long completedAt;

    private long createdAt = System.currentTimeMillis() / 1000;
}