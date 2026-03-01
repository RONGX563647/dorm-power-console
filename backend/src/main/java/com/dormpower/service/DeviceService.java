package com.dormpower.service;

import com.dormpower.exception.ResourceNotFoundException;
import com.dormpower.model.Device;
import com.dormpower.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备服务
 */
@Service
public class DeviceService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);

    @Autowired
    private DeviceRepository deviceRepository;

    /**
     * 获取设备列表
     * @return 设备列表
     */
    @Cacheable(value = "devices", unless = "#result == null || #result.isEmpty()")
    public List<Map<String, Object>> getDevices() {
        logger.debug("获取设备列表");
        List<Device> devices = deviceRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Device device : devices) {
            Map<String, Object> deviceMap = new HashMap<>();
            deviceMap.put("id", device.getId());
            deviceMap.put("name", device.getName());
            deviceMap.put("room", device.getRoom());
            deviceMap.put("online", device.isOnline());
            deviceMap.put("lastSeen", device.getLastSeenTs());
            result.add(deviceMap);
        }
        
        logger.info("获取设备列表成功，共{}个设备", result.size());
        return result;
    }

    /**
     * 获取设备状态
     * @param deviceId 设备ID
     * @return 设备状态
     */
    @Cacheable(value = "deviceStatus", key = "#deviceId")
    public Map<String, Object> getDeviceStatus(String deviceId) {
        logger.debug("获取设备状态: {}", deviceId);
        Device device = deviceRepository.findById(deviceId).orElse(null);
        
        Map<String, Object> status = new HashMap<>();
        status.put("deviceId", deviceId);
        
        if (device != null) {
            status.put("online", device.isOnline());
            status.put("lastSeen", device.getLastSeenTs());
            status.put("name", device.getName());
            status.put("room", device.getRoom());
        } else {
            status.put("online", false);
            status.put("lastSeen", 0L);
            logger.warn("设备不存在: {}", deviceId);
        }
        
        return status;
    }

    /**
     * 获取设备详情
     * @param deviceId 设备ID
     * @return 设备详情
     */
    public Map<String, Object> getDeviceDetail(String deviceId) {
        logger.debug("获取设备详情: {}", deviceId);
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("设备不存在: " + deviceId));
        
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", device.getId());
        detail.put("name", device.getName());
        detail.put("room", device.getRoom());
        detail.put("online", device.isOnline());
        detail.put("lastSeen", device.getLastSeenTs());
        detail.put("createdAt", device.getCreatedAt());
        
        return detail;
    }

    /**
     * 更新设备状态
     * @param deviceId 设备ID
     * @param online 在线状态
     */
    @CacheEvict(value = {"devices", "deviceStatus"}, allEntries = true)
    public void updateDeviceStatus(String deviceId, boolean online) {
        logger.debug("更新设备状态: {} -> {}", deviceId, online);
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device != null) {
            device.setOnline(online);
            device.setLastSeenTs(System.currentTimeMillis());
            deviceRepository.save(device);
            logger.info("设备状态已更新: {} -> {}", deviceId, online);
        } else {
            logger.warn("设备不存在，无法更新状态: {}", deviceId);
        }
    }

    /**
     * 添加设备
     * @param device 设备
     * @return 添加的设备
     */
    @CacheEvict(value = "devices", allEntries = true)
    public Device addDevice(Device device) {
        logger.debug("添加设备: {}", device.getId());
        device.setCreatedAt(System.currentTimeMillis());
        device.setLastSeenTs(System.currentTimeMillis());
        Device savedDevice = deviceRepository.save(device);
        logger.info("设备添加成功: {}", savedDevice.getId());
        return savedDevice;
    }

    /**
     * 删除设备
     * @param deviceId 设备ID
     */
    @CacheEvict(value = {"devices", "deviceStatus"}, allEntries = true)
    public void deleteDevice(String deviceId) {
        logger.debug("删除设备: {}", deviceId);
        if (!deviceRepository.existsById(deviceId)) {
            throw new ResourceNotFoundException("设备不存在: " + deviceId);
        }
        deviceRepository.deleteById(deviceId);
        logger.info("设备删除成功: {}", deviceId);
    }

}
