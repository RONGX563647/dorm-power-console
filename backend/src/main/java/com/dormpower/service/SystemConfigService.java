package com.dormpower.service;

import com.dormpower.model.SystemConfig;
import com.dormpower.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置服务类
 */
@Service
public class SystemConfigService {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    /**
     * 获取所有配置
     */
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    /**
     * 根据分类获取配置
     */
    public List<SystemConfig> getConfigsByCategory(String category) {
        return systemConfigRepository.findByCategory(category);
    }

    /**
     * 根据key获取配置值
     */
    public String getConfigValue(String key) {
        Optional<SystemConfig> config = systemConfigRepository.findByKey(key);
        return config.map(SystemConfig::getValue).orElse(null);
    }

    /**
     * 根据key获取配置值，带默认值
     */
    public String getConfigValue(String key, String defaultValue) {
        String value = getConfigValue(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取配置详情
     */
    public Optional<SystemConfig> getConfigByKey(String key) {
        return systemConfigRepository.findByKey(key);
    }

    /**
     * 创建或更新配置
     */
    public SystemConfig saveConfig(SystemConfig config) {
        return systemConfigRepository.save(config);
    }

    /**
     * 更新配置值
     */
    public SystemConfig updateConfigValue(String key, String value) {
        Optional<SystemConfig> existingConfig = systemConfigRepository.findByKey(key);
        if (existingConfig.isPresent()) {
            SystemConfig config = existingConfig.get();
            if (config.isEditable()) {
                config.setValue(value);
                return systemConfigRepository.save(config);
            } else {
                throw new RuntimeException("Config is not editable: " + key);
            }
        } else {
            throw new RuntimeException("Config not found: " + key);
        }
    }

    /**
     * 删除配置
     */
    public void deleteConfig(Long id) {
        systemConfigRepository.deleteById(id);
    }

    /**
     * 初始化默认配置
     */
    public void initDefaultConfigs() {
        // 系统配置
        saveConfigIfNotExists("system.name", "宿舍电源管理系统", "系统名称", "system", false);
        saveConfigIfNotExists("system.version", "1.0.0", "系统版本", "system", false);
        saveConfigIfNotExists("system.maintenance_mode", "false", "维护模式", "system", true);
        
        // 邮件配置
        saveConfigIfNotExists("email.enabled", "false", "邮件通知是否启用", "email", true);
        saveConfigIfNotExists("email.smtp.host", "", "SMTP服务器地址", "email", true);
        saveConfigIfNotExists("email.smtp.port", "587", "SMTP服务器端口", "email", true);
        saveConfigIfNotExists("email.smtp.username", "", "SMTP用户名", "email", true);
        saveConfigIfNotExists("email.smtp.password", "", "SMTP密码", "email", true);
        
        // 告警配置
        saveConfigIfNotExists("alert.enabled", "true", "告警功能是否启用", "alert", true);
        saveConfigIfNotExists("alert.email.enabled", "false", "邮件告警是否启用", "alert", true);
        saveConfigIfNotExists("alert.webhook.enabled", "false", "Webhook告警是否启用", "alert", true);
        
        // 数据备份配置
        saveConfigIfNotExists("backup.enabled", "true", "自动备份是否启用", "backup", true);
        saveConfigIfNotExists("backup.interval", "86400", "备份间隔（秒）", "backup", true);
        saveConfigIfNotExists("backup.retention_days", "30", "备份保留天数", "backup", true);
        
        // 监控配置
        saveConfigIfNotExists("monitor.enabled", "true", "监控功能是否启用", "monitor", true);
        saveConfigIfNotExists("monitor.metrics_retention_days", "7", "监控数据保留天数", "monitor", true);
    }

    private void saveConfigIfNotExists(String key, String value, String description, String category, boolean editable) {
        if (!systemConfigRepository.existsByKey(key)) {
            SystemConfig config = new SystemConfig();
            config.setKey(key);
            config.setValue(value);
            config.setDescription(description);
            config.setCategory(category);
            config.setEditable(editable);
            systemConfigRepository.save(config);
        }
    }
}
