package com.dormpower.service;

import com.dormpower.model.DeviceFirmware;
import com.dormpower.repository.DeviceFirmwareRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 设备固件服务
 */
@Service
public class DeviceFirmwareService {

    @Autowired
    private DeviceFirmwareRepository firmwareRepository;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private MqttService mqttService;

    @Autowired
    private SystemLogService systemLogService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 发起固件升级
     */
    @Transactional
    public DeviceFirmware initiateUpgrade(String deviceId, String version, String filePath,
                                           String checksum, long fileSize, String initiatedBy) {
        Optional<DeviceFirmware> existingUpgrade = firmwareRepository.findByDeviceIdAndStatus(deviceId, "PENDING");
        if (existingUpgrade.isPresent()) {
            throw new RuntimeException("Device already has a pending upgrade");
        }

        existingUpgrade = firmwareRepository.findByDeviceIdAndStatus(deviceId, "DOWNLOADING");
        if (existingUpgrade.isPresent()) {
            throw new RuntimeException("Device is currently upgrading");
        }

        DeviceFirmware latest = firmwareRepository.findFirstByDeviceIdOrderByCreatedAtDesc(deviceId).orElse(null);
        String previousVersion = latest != null ? latest.getVersion() : "unknown";

        DeviceFirmware firmware = new DeviceFirmware();
        firmware.setDeviceId(deviceId);
        firmware.setVersion(version);
        firmware.setPreviousVersion(previousVersion);
        firmware.setFilePath(filePath);
        firmware.setChecksum(checksum);
        firmware.setFileSize(fileSize);
        firmware.setStatus("PENDING");
        firmware.setInitiatedBy(initiatedBy);

        DeviceFirmware saved = firmwareRepository.save(firmware);

        systemLogService.info("FIRMWARE", 
            "Firmware upgrade initiated for device " + deviceId + " to version " + version, 
            initiatedBy);

        return saved;
    }

    /**
     * 开始下载固件
     */
    @Transactional
    public DeviceFirmware startDownload(Long firmwareId) {
        DeviceFirmware firmware = firmwareRepository.findById(firmwareId)
                .orElseThrow(() -> new RuntimeException("Firmware record not found"));

        if (!"PENDING".equals(firmware.getStatus())) {
            throw new RuntimeException("Firmware is not in pending status");
        }

        firmware.setStatus("DOWNLOADING");
        firmware.setStartedAt(System.currentTimeMillis() / 1000);
        firmware.setProgress(0);

        return firmwareRepository.save(firmware);
    }

    /**
     * 更新下载进度
     */
    @Transactional
    public DeviceFirmware updateProgress(Long firmwareId, int progress) {
        DeviceFirmware firmware = firmwareRepository.findById(firmwareId)
                .orElseThrow(() -> new RuntimeException("Firmware record not found"));

        firmware.setProgress(Math.min(100, Math.max(0, progress)));

        if (progress >= 100 && "DOWNLOADING".equals(firmware.getStatus())) {
            firmware.setStatus("INSTALLING");
        }

        return firmwareRepository.save(firmware);
    }

    /**
     * 完成升级
     */
    @Transactional
    public DeviceFirmware completeUpgrade(Long firmwareId, boolean success, String errorMessage) {
        DeviceFirmware firmware = firmwareRepository.findById(firmwareId)
                .orElseThrow(() -> new RuntimeException("Firmware record not found"));

        if (success) {
            firmware.setStatus("SUCCESS");
            firmware.setProgress(100);
            firmware.setCompletedAt(System.currentTimeMillis() / 1000);

            systemLogService.info("FIRMWARE", 
                "Firmware upgrade completed for device " + firmware.getDeviceId(), 
                firmware.getInitiatedBy());

            notificationService.createSystemNotification(
                "固件升级成功",
                "设备 " + firmware.getDeviceId() + " 已成功升级到版本 " + firmware.getVersion(),
                firmware.getInitiatedBy()
            );
        } else {
            firmware.setStatus("FAILED");
            firmware.setErrorMessage(errorMessage);
            firmware.setCompletedAt(System.currentTimeMillis() / 1000);

            systemLogService.error("FIRMWARE", 
                "Firmware upgrade failed for device " + firmware.getDeviceId() + ": " + errorMessage, 
                firmware.getInitiatedBy(),
                errorMessage);

            notificationService.createAlertNotification(
                "固件升级失败",
                "设备 " + firmware.getDeviceId() + " 升级失败: " + errorMessage,
                firmware.getInitiatedBy(),
                firmware.getDeviceId()
            );
        }

        return firmwareRepository.save(firmware);
    }

    /**
     * 取消升级
     */
    @Transactional
    public DeviceFirmware cancelUpgrade(Long firmwareId, String reason) {
        DeviceFirmware firmware = firmwareRepository.findById(firmwareId)
                .orElseThrow(() -> new RuntimeException("Firmware record not found"));

        if ("SUCCESS".equals(firmware.getStatus()) || "FAILED".equals(firmware.getStatus())) {
            throw new RuntimeException("Cannot cancel completed upgrade");
        }

        firmware.setStatus("FAILED");
        firmware.setErrorMessage("Cancelled: " + reason);
        firmware.setCompletedAt(System.currentTimeMillis() / 1000);

        systemLogService.warn("FIRMWARE", 
            "Firmware upgrade cancelled for device " + firmware.getDeviceId() + ": " + reason, 
            "System");

        return firmwareRepository.save(firmware);
    }

    /**
     * 获取设备的固件历史
     */
    public List<DeviceFirmware> getDeviceFirmwareHistory(String deviceId) {
        return firmwareRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId);
    }

    /**
     * 获取设备当前固件版本
     */
    public Optional<DeviceFirmware> getCurrentFirmware(String deviceId) {
        return firmwareRepository.findFirstByDeviceIdOrderByCreatedAtDesc(deviceId)
                .filter(f -> "SUCCESS".equals(f.getStatus()));
    }

    /**
     * 获取待处理的升级任务
     */
    public List<DeviceFirmware> getPendingUpgrades() {
        return firmwareRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    /**
     * 获取进行中的升级任务
     */
    public List<DeviceFirmware> getActiveUpgrades() {
        List<DeviceFirmware> downloading = firmwareRepository.findByStatusOrderByCreatedAtDesc("DOWNLOADING");
        List<DeviceFirmware> installing = firmwareRepository.findByStatusOrderByCreatedAtDesc("INSTALLING");
        downloading.addAll(installing);
        return downloading;
    }

    /**
     * 定时检查升级超时
     */
    @Scheduled(fixedRate = 300000)
    public void checkUpgradeTimeout() {
        long timeout = System.currentTimeMillis() / 1000 - 3600;

        List<DeviceFirmware> downloading = firmwareRepository.findByStatusOrderByCreatedAtDesc("DOWNLOADING");
        for (DeviceFirmware firmware : downloading) {
            if (firmware.getStartedAt() > 0 && firmware.getStartedAt() < timeout) {
                firmware.setStatus("FAILED");
                firmware.setErrorMessage("Upgrade timeout");
                firmware.setCompletedAt(System.currentTimeMillis() / 1000);
                firmwareRepository.save(firmware);

                systemLogService.error("FIRMWARE", 
                    "Firmware upgrade timeout for device " + firmware.getDeviceId(), 
                    "System",
                    "Upgrade timeout");
            }
        }

        List<DeviceFirmware> installing = firmwareRepository.findByStatusOrderByCreatedAtDesc("INSTALLING");
        for (DeviceFirmware firmware : installing) {
            if (firmware.getStartedAt() > 0 && firmware.getStartedAt() < timeout) {
                firmware.setStatus("FAILED");
                firmware.setErrorMessage("Installation timeout");
                firmware.setCompletedAt(System.currentTimeMillis() / 1000);
                firmwareRepository.save(firmware);

                systemLogService.error("FIRMWARE", 
                    "Firmware installation timeout for device " + firmware.getDeviceId(), 
                    "System",
                    "Installation timeout");
            }
        }
    }

    /**
     * 获取固件详情
     */
    public Optional<DeviceFirmware> getFirmwareById(Long id) {
        return firmwareRepository.findById(id);
    }

    /**
     * 发送升级命令到设备
     */
    public boolean sendUpgradeCommand(Long firmwareId) {
        DeviceFirmware firmware = firmwareRepository.findById(firmwareId)
                .orElseThrow(() -> new RuntimeException("Firmware record not found"));

        try {
            String topic = "device/" + firmware.getDeviceId() + "/ota";
            String payload = String.format(
                "{\"action\":\"upgrade\",\"version\":\"%s\",\"url\":\"%s\",\"checksum\":\"%s\"}",
                firmware.getVersion(),
                firmware.getFilePath(),
                firmware.getChecksum()
            );

            mqttService.sendMessage(topic, payload);
            startDownload(firmwareId);
            return true;
        } catch (Exception e) {
            systemLogService.error("FIRMWARE", 
                "Failed to send upgrade command: " + e.getMessage(), 
                "System",
                e.getMessage());
            return false;
        }
    }
}
