package com.dormpower.service;

import com.dormpower.model.DeviceAlert;
import com.dormpower.model.DeviceAlertConfig;
import com.dormpower.repository.DeviceAlertRepository;
import com.dormpower.repository.DeviceAlertConfigRepository;
import com.dormpower.service.WebSocketNotificationService;
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
    private WebSocketNotificationService webSocketNotificationService;

    /**
     * 检查并生成告警
     */
    public void checkAndGenerateAlert(String deviceId, String type, double value) {
        DeviceAlertConfig config = deviceAlertConfigRepository.findByDeviceIdAndType(deviceId, type);
        if (config == null || !config.isEnabled()) {
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
     */
    public List<DeviceAlertConfig> getDeviceAlertConfigs(String deviceId) {
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
     */
    @CacheEvict(value = "alertConfigs", allEntries = true)
    public DeviceAlertConfig updateAlertConfig(String deviceId, String type, double minThreshold, double maxThreshold, boolean enabled) {
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
