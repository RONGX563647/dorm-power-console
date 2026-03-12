package com.dormpower.service;

import com.dormpower.model.Device;
import com.dormpower.model.Telemetry;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.TelemetryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 智能Agent数据生成服务
 * 为测试和演示生成模拟遥测数据
 */
@Service
public class AgentDataGenerator {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TelemetryRepository telemetryRepository;

    private final Random random = new Random(42);

    /**
     * 为设备生成模拟遥测数据
     */
    public int generateTelemetryData(String deviceId, int hours) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            return 0;
        }

        long now = System.currentTimeMillis() / 1000;
        long startTs = now - hours * 3600L;
        
        int pointsPerHour = 60;
        int totalPoints = hours * pointsPerHour;
        
        List<Telemetry> telemetryList = new ArrayList<>();
        
        double basePower = 30 + random.nextDouble() * 30;
        
        for (int i = 0; i < totalPoints; i++) {
            long ts = startTs + (i * 3600L / pointsPerHour);
            
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(ts * 1000);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            
            double hourFactor = getHourFactor(hour);
            double weekendFactor = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) ? 1.2 : 1.0;
            
            double noise = (random.nextDouble() - 0.5) * 15;
            double power = basePower * hourFactor * weekendFactor + noise;
            power = Math.max(0, power);
            
            if (random.nextDouble() < 0.01) {
                power += 50 + random.nextDouble() * 100;
            }
            
            if (random.nextDouble() < 0.005) {
                power = Math.max(0, power - 30);
            }
            
            Telemetry t = new Telemetry();
            t.setDeviceId(deviceId);
            t.setTs(ts);
            t.setPowerW(Math.round(power * 10.0) / 10.0);
            t.setVoltageV(220 + (random.nextDouble() - 0.5) * 5);
            t.setCurrentA(t.getPowerW() / t.getVoltageV());
            
            telemetryList.add(t);
        }
        
        telemetryRepository.saveAll(telemetryList);
        
        return telemetryList.size();
    }

    /**
     * 为房间所有设备生成数据
     */
    public Map<String, Object> generateRoomData(String roomId, int hours) {
        List<Device> devices = deviceRepository.findByRoom(roomId);
        
        if (devices.isEmpty()) {
            devices = deviceRepository.findAll();
        }
        
        Map<String, Integer> results = new HashMap<>();
        int total = 0;
        
        for (Device device : devices) {
            int count = generateTelemetryData(device.getId(), hours);
            results.put(device.getId(), count);
            total += count;
        }
        
        return Map.of(
                "roomId", roomId,
                "devicesProcessed", devices.size(),
                "totalPoints", total,
                "details", results
        );
    }

    /**
     * 为所有设备生成数据
     */
    public Map<String, Object> generateAllDevicesData(int hours) {
        List<Device> devices = deviceRepository.findAll();
        
        Map<String, Integer> results = new HashMap<>();
        int total = 0;
        
        for (Device device : devices) {
            int count = generateTelemetryData(device.getId(), hours);
            results.put(device.getId(), count);
            total += count;
        }
        
        return Map.of(
                "devicesProcessed", devices.size(),
                "totalPoints", total,
                "hours", hours,
                "details", results
        );
    }

    /**
     * 获取小时因子（模拟真实用电模式）
     * 典型宿舍用电模式：
     * - 0-6点: 深夜睡眠，用电最低
     * - 6-8点: 起床洗漱，用电上升
     * - 8-12点: 上课时间，宿舍用电低
     * - 12-14点: 午休时间，用电中等
     * - 14-18点: 下午上课，用电较低
     * - 18-23点: 晚间活动，用电最高
     * - 23-24点: 准备睡觉，用电下降
     */
    private double getHourFactor(int hour) {
        if (hour >= 0 && hour < 6) {
            return 0.2 + random.nextDouble() * 0.1;
        } else if (hour >= 6 && hour < 8) {
            return 0.6 + random.nextDouble() * 0.2;
        } else if (hour >= 8 && hour < 12) {
            return 0.3 + random.nextDouble() * 0.2;
        } else if (hour >= 12 && hour < 14) {
            return 0.7 + random.nextDouble() * 0.2;
        } else if (hour >= 14 && hour < 18) {
            return 0.4 + random.nextDouble() * 0.2;
        } else if (hour >= 18 && hour < 23) {
            return 1.0 + random.nextDouble() * 0.3;
        } else {
            return 0.5 + random.nextDouble() * 0.2;
        }
    }

    /**
     * 清除设备的遥测数据
     */
    public void clearTelemetryData(String deviceId) {
        long now = System.currentTimeMillis() / 1000;
        long startTs = now - 86400 * 30;
        List<Telemetry> data = telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(deviceId, startTs, now);
        telemetryRepository.deleteAll(data);
    }
}
