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
 * 
 * 提供设备管理的业务逻辑，包括：
 * - 设备列表查询（带缓存）
 * - 设备状态查询（带缓存）
 * - 设备详情查询
 * - 设备状态更新
 * - 设备添加和删除
 * 
 * 使用Spring Cache进行缓存管理，提高查询性能。
 * 所有操作都记录日志，便于问题排查。
 * 
 * @author dormpower team
 * @version 1.0
 */
@Service
public class DeviceService {

    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);

    // 设备数据访问对象
    @Autowired
    private DeviceRepository deviceRepository;

    /**
     * 获取设备列表
     * 
     * 查询所有注册的设备信息，包括设备ID、名称、房间号和在线状态。
     * 使用Spring Cache进行缓存，当结果为null或空列表时不缓存。
     * 缓存key为"all"，在设备添加或删除时会清除缓存。
     * 
     * @return 设备列表，每个设备包含id、name、room、online和lastSeen字段
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
     * 获取设备详情（带缓存）
     * @param deviceId 设备ID
     * @return 设备详情
     */
    @Cacheable(value = "deviceDetail", key = "#deviceId")
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
     * 更新设备状态（清除设备相关缓存）
     * @param deviceId 设备ID
     * @param online 在线状态
     */
    @CacheEvict(value = {"devices", "deviceStatus", "deviceDetail", "deviceOnline"}, allEntries = true)
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
     * 注册设备
     *
     * 设备首次连接时自动注册，检查设备是否已存在。
     * 新注册设备默认为离线状态。
     *
     * @param id 设备ID
     * @param name 设备名称
     * @param room 所在房间号
     * @return 注册成功的设备
     * @throws BusinessException 当设备ID已存在时抛出
     */
    @CacheEvict(value = "devices", allEntries = true)
    public Device registerDevice(String id, String name, String room) {
        logger.debug("注册设备: {}", id);

        // 检查设备是否已存在
        if (deviceRepository.existsById(id)) {
            logger.warn("设备已存在: {}", id);
            throw new com.dormpower.exception.BusinessException("设备ID已存在: " + id);
        }

        // 创建设备实体
        Device device = new Device();
        device.setId(id);
        device.setName(name);
        device.setRoom(room);
        device.setOnline(false);
        device.setCreatedAt(System.currentTimeMillis() / 1000);
        device.setLastSeenTs(System.currentTimeMillis() / 1000);

        Device savedDevice = deviceRepository.save(device);
        logger.info("设备注册成功: {}", savedDevice.getId());
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

    // ==================== 设备在线状态管理 ====================

    /** 设备离线阈值（秒）- 超过此时间无心跳则视为离线 */
    public static final long OFFLINE_THRESHOLD_SECONDS = 60;

    /**
     * 处理设备心跳（清除设备相关缓存）
     *
     * 更新设备的最后心跳时间，并将设备标记为在线。
     * 心跳消息表示设备正常工作。
     *
     * @param deviceId 设备ID
     * @return 更新后的设备，如果设备不存在返回null
     */
    @CacheEvict(value = {"devices", "deviceStatus", "deviceDetail", "deviceOnline"}, allEntries = true)
    public Device processHeartbeat(String deviceId) {
        logger.debug("处理设备心跳: {}", deviceId);
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            logger.warn("设备不存在，无法处理心跳: {}", deviceId);
            return null;
        }

        long now = System.currentTimeMillis() / 1000;
        device.setLastSeenTs(now);
        device.setOnline(true);
        Device savedDevice = deviceRepository.save(device);
        logger.info("设备心跳处理成功: {} -> 在线", deviceId);
        return savedDevice;
    }

    /**
     * 标记设备离线（清除设备相关缓存）
     *
     * 用于处理LWT（Last Will and Testament）消息，设备断开连接时立即标记为离线。
     *
     * @param deviceId 设备ID
     * @return 是否成功标记离线
     */
    @CacheEvict(value = {"devices", "deviceStatus", "deviceDetail", "deviceOnline"}, allEntries = true)
    public boolean markDeviceOffline(String deviceId) {
        logger.debug("标记设备离线: {}", deviceId);
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            logger.warn("设备不存在，无法标记离线: {}", deviceId);
            return false;
        }

        device.setOnline(false);
        deviceRepository.save(device);
        logger.info("设备已标记离线: {}", deviceId);
        return true;
    }

    /**
     * 检查并更新设备在线状态（清除设备相关缓存）
     *
     * 根据最后心跳时间判断设备是否在线。
     * 如果超过 OFFLINE_THRESHOLD_SECONDS 秒无心跳，则标记为离线。
     *
     * @param deviceId 设备ID
     * @return 设备当前是否在线
     */
    @CacheEvict(value = {"devices", "deviceStatus", "deviceDetail", "deviceOnline"}, allEntries = true)
    public boolean checkAndUpdateOnlineStatus(String deviceId) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            return false;
        }

        long now = System.currentTimeMillis() / 1000;
        long lastSeenTs = device.getLastSeenTs();
        boolean shouldBeOnline = (now - lastSeenTs) < OFFLINE_THRESHOLD_SECONDS;

        if (device.isOnline() != shouldBeOnline) {
            device.setOnline(shouldBeOnline);
            deviceRepository.save(device);
            logger.info("设备在线状态更新: {} -> {}", deviceId, shouldBeOnline ? "在线" : "离线");
        }

        return shouldBeOnline;
    }

    /**
     * 检查设备是否在线（基于心跳时间，带缓存）
     *
     * @param deviceId 设备ID
     * @return 设备是否在线
     */
    @Cacheable(value = "deviceOnline", key = "#deviceId")
    public boolean isDeviceOnline(String deviceId) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            return false;
        }

        long now = System.currentTimeMillis() / 1000;
        return device.isOnline() && (now - device.getLastSeenTs()) < OFFLINE_THRESHOLD_SECONDS;
    }

}
