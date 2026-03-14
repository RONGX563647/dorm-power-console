package com.dormpower.service;

import com.dormpower.exception.BusinessException;
import com.dormpower.exception.ResourceNotFoundException;
import com.dormpower.model.DeviceAlert;
import com.dormpower.model.DeviceAlertConfig;
import com.dormpower.repository.DeviceAlertRepository;
import com.dormpower.repository.DeviceAlertConfigRepository;
import com.dormpower.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 告警服务
 */
@Service
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    @Autowired
    private DeviceAlertRepository deviceAlertRepository;

    @Autowired
    private DeviceAlertConfigRepository deviceAlertConfigRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private WebSocketNotificationService webSocketNotificationService;

    /**
     * 检查并生成告警
     */
    public void checkAndGenerateAlert(String deviceId, String type, double value) {
        DeviceAlertConfig config = deviceAlertConfigRepository.findByDeviceIdAndType(deviceId, type);
        if (config == null || !config.isEnabled()) {
            return;
        }

        // TC-TRIG-005: 检查是否存在未解决的相同类型告警，避免重复生成
        List<DeviceAlert> existingAlerts = deviceAlertRepository
                .findByDeviceIdAndTypeAndResolvedFalseOrderByTsDesc(deviceId, type);
        if (!existingAlerts.isEmpty()) {
            logger.debug("Alert already exists for device {} type {}, skipping", deviceId, type);
            return;
        }

        long now = System.currentTimeMillis() / 1000;
        String level = "info";
        String message = "";

        if (type.equals("power")) {
            if (value > config.getThresholdMax()) {
                level = "warning";
                message = "Power exceeds threshold: " + value + "W (max: " + config.getThresholdMax() + "W)";
            } else if (value < config.getThresholdMin()) {
                level = "info";
                message = "Power below threshold: " + value + "W (min: " + config.getThresholdMin() + "W)";
            }
        } else if (type.equals("voltage")) {
            if (value > config.getThresholdMax()) {
                level = "error";
                message = "Voltage exceeds threshold: " + value + "V (max: " + config.getThresholdMax() + "V)";
            } else if (value < config.getThresholdMin()) {
                level = "error";
                message = "Voltage below threshold: " + value + "V (min: " + config.getThresholdMin() + "V)";
            }
        } else if (type.equals("current")) {
            if (value > config.getThresholdMax()) {
                level = "warning";
                message = "Current exceeds threshold: " + value + "A (max: " + config.getThresholdMax() + "A)";
            } else if (value < config.getThresholdMin()) {
                level = "info";
                message = "Current below threshold: " + value + "A (min: " + config.getThresholdMin() + "A)";
            }
        }

        if (!message.isEmpty()) {
            DeviceAlert alert = new DeviceAlert();
            alert.setId("alert_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000));
            alert.setDeviceId(deviceId);
            alert.setType(type);
            alert.setLevel(level);
            alert.setMessage(message);
            alert.setThresholdValue(type.equals("power") || type.equals("current") ? config.getThresholdMax() : 
                                  (value > config.getThresholdMax() ? config.getThresholdMax() : config.getThresholdMin()));
            alert.setActualValue(value);
            alert.setResolved(false);
            alert.setTs(now);
            alert.setCreatedAt(now);
            
            deviceAlertRepository.save(alert);
            logger.info("Generated alert: {}", message);
            
            // 发送WebSocket通知
            webSocketNotificationService.broadcastAlert(deviceId, Map.of(
                "type", type,
                "level", level,
                "message", message,
                "value", value,
                "threshold", alert.getThresholdValue(),
                "ts", now
            ));
        }
    }

    /**
     * 获取设备的告警列表
     */
    public List<DeviceAlert> getDeviceAlerts(String deviceId, boolean onlyUnresolved) {
        if (onlyUnresolved) {
            return deviceAlertRepository.findByDeviceIdAndResolvedFalseOrderByTsDesc(deviceId);
        } else {
            return deviceAlertRepository.findByDeviceIdOrderByTsDesc(deviceId);
        }
    }

    /**
     * 获取所有未解决的告警
     */
    public List<DeviceAlert> getUnresolvedAlerts() {
        return deviceAlertRepository.findByResolvedFalseOrderByTsDesc();
    }

    /**
     * 获取时间范围内的告警
     *
     * @param startTs 开始时间戳（秒）
     * @param endTs   结束时间戳（秒）
     * @return 告警列表
     */
    public List<DeviceAlert> getAlertsByTimeRange(long startTs, long endTs) {
        return deviceAlertRepository.findByTsBetweenOrderByTsDesc(startTs, endTs);
    }

    /**
     * 解决告警
     */
    public void resolveAlert(String alertId) {
        DeviceAlert alert = deviceAlertRepository.findById(alertId).orElse(null);
        if (alert != null && !alert.isResolved()) {
            alert.setResolved(true);
            alert.setResolvedAt(System.currentTimeMillis() / 1000);
            deviceAlertRepository.save(alert);
            logger.info("Resolved alert: {}", alertId);
        }
    }

    /**
     * 获取设备的告警配置
     *
     * @param deviceId 设备ID
     * @return 告警配置列表
     * @throws ResourceNotFoundException 设备不存在时抛出
     */
    public List<DeviceAlertConfig> getDeviceAlertConfigs(String deviceId) {
        // 验证设备是否存在
        if (!deviceRepository.existsById(deviceId)) {
            throw new ResourceNotFoundException("Device not found: " + deviceId);
        }
        return deviceAlertConfigRepository.findByDeviceId(deviceId);
    }

    /**
     * 获取所有启用的告警配置
     */
    @Cacheable(value = "alertConfigs", key = "'enabled'")
    public List<DeviceAlertConfig> getAllAlertConfigs() {
        return deviceAlertConfigRepository.findByEnabledTrue();
    }

    /**
     * 更新设备的告警配置
     *
     * @param deviceId 设备ID
     * @param type 告警类型
     * @param minThreshold 最小阈值
     * @param maxThreshold 最大阈值
     * @param enabled 是否启用
     * @return 更新后的告警配置
     * @throws ResourceNotFoundException 设备不存在时抛出
     * @throws BusinessException 阈值范围无效时抛出
     */
    @CacheEvict(value = "alertConfigs", allEntries = true)
    public DeviceAlertConfig updateAlertConfig(String deviceId, String type, double minThreshold, double maxThreshold, boolean enabled) {
        // 验证设备是否存在
        if (!deviceRepository.existsById(deviceId)) {
            throw new ResourceNotFoundException("Device not found: " + deviceId);
        }

        // 验证阈值范围
        if (minThreshold > maxThreshold) {
            throw new BusinessException("Invalid threshold range: minThreshold (" + minThreshold + ") cannot be greater than maxThreshold (" + maxThreshold + ")");
        }

        DeviceAlertConfig config = deviceAlertConfigRepository.findByDeviceIdAndType(deviceId, type);
        long now = System.currentTimeMillis() / 1000;

        if (config == null) {
            config = new DeviceAlertConfig();
            config.setId("config_" + UUID.randomUUID().toString().substring(0, 8));
            config.setDeviceId(deviceId);
            config.setType(type);
            config.setCreatedAt(now);
        }

        config.setThresholdMin(minThreshold);
        config.setThresholdMax(maxThreshold);
        config.setEnabled(enabled);
        config.setUpdatedAt(now);

        return deviceAlertConfigRepository.save(config);
    }

    /**
     * 删除告警配置
     */
    @CacheEvict(value = "alertConfigs", allEntries = true)
    public void deleteAlertConfig(String configId) {
        deviceAlertConfigRepository.deleteById(configId);
    }

}
