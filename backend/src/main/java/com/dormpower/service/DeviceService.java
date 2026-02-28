package com.dormpower.service;

import com.dormpower.exception.ResourceNotFoundException;
import com.dormpower.model.Device;
import com.dormpower.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private DeviceRepository deviceRepository;

    /**
     * 获取设备列表
     * @return 设备列表
     */
    public List<Map<String, Object>> getDevices() {
        List<Device> devices = deviceRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 转换为响应格式
        for (Device device : devices) {
            Map<String, Object> deviceMap = new HashMap<>();
            deviceMap.put("id", device.getId());
            deviceMap.put("name", device.getName());
            deviceMap.put("room", device.getRoom());
            deviceMap.put("online", device.isOnline());
            deviceMap.put("lastSeen", device.getLastSeenTs());
            result.add(deviceMap);
        }
        
        // 如果没有数据，返回模拟数据
        if (result.isEmpty()) {
            Map<String, Object> device1 = new HashMap<>();
            device1.put("id", "device-001");
            device1.put("name", "宿舍101插座");
            device1.put("room", "101");
            device1.put("online", true);
            device1.put("lastSeen", System.currentTimeMillis());
            result.add(device1);

            Map<String, Object> device2 = new HashMap<>();
            device2.put("id", "device-002");
            device2.put("name", "宿舍102插座");
            device2.put("room", "102");
            device2.put("online", false);
            device2.put("lastSeen", System.currentTimeMillis() - 3600000);
            result.add(device2);
        }
        
        return result;
    }

    /**
     * 获取设备状态
     * @param deviceId 设备ID
     * @return 设备状态
     */
    public Map<String, Object> getDeviceStatus(String deviceId) {
        // 查询设备
        Device device = deviceRepository.findById(deviceId).orElse(null);
        
        Map<String, Object> status = new HashMap<>();
        status.put("deviceId", deviceId);
        
        if (device != null) {
            status.put("online", device.isOnline());
            status.put("lastSeen", device.getLastSeenTs());
        } else {
            status.put("online", false);
            status.put("lastSeen", 0L);
        }
        
        // 返回模拟的详细状态
        status.put("totalPowerW", 120.5);
        status.put("voltageV", 220.0);
        status.put("currentA", 0.55);
        status.put("sockets", List.of(
                Map.of("id", 1, "status", "on", "powerW", 60.2),
                Map.of("id", 2, "status", "off", "powerW", 0.0),
                Map.of("id", 3, "status", "on", "powerW", 60.3)
        ));
        
        return status;
    }

    /**
     * 获取设备详情
     * @param deviceId 设备ID
     * @return 设备详情
     */
    public Map<String, Object> getDeviceDetail(String deviceId) {
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
    public void updateDeviceStatus(String deviceId, boolean online) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device != null) {
            device.setOnline(online);
            device.setLastSeenTs(System.currentTimeMillis());
            deviceRepository.save(device);
        }
    }

    /**
     * 添加设备
     * @param device 设备
     * @return 添加的设备
     */
    public Device addDevice(Device device) {
        device.setCreatedAt(System.currentTimeMillis());
        device.setLastSeenTs(System.currentTimeMillis());
        return deviceRepository.save(device);
    }

    /**
     * 删除设备
     * @param deviceId 设备ID
     */
    public void deleteDevice(String deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new ResourceNotFoundException("设备不存在: " + deviceId);
        }
        deviceRepository.deleteById(deviceId);
    }

}