package com.dormpower.scheduler;

import com.dormpower.model.Device;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.service.NotificationService;
import com.dormpower.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 设备在线状态监控定时任务
 */
@Component
public class DeviceStatusMonitorScheduler {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SystemLogService systemLogService;

    private static final long OFFLINE_THRESHOLD_SECONDS = 120; // 2分钟无响应视为离线

    /**
     * 每30秒检查一次设备在线状态
     */
    @Scheduled(fixedRate = 30000)
    public void checkDeviceStatus() {
        List<Device> devices = deviceRepository.findAll();
        long now = System.currentTimeMillis() / 1000;

        for (Device device : devices) {
            boolean shouldBeOnline = (now - device.getLastSeenTs()) < OFFLINE_THRESHOLD_SECONDS;

            if (device.isOnline() && !shouldBeOnline) {
                // 设备离线
                device.setOnline(false);
                deviceRepository.save(device);

                // 记录日志
                systemLogService.warn("DEVICE", 
                    "Device went offline: " + device.getId(), 
                    "DeviceMonitor");

                // 发送通知
                notificationService.createAlertNotification(
                    "设备离线告警",
                    "设备 " + device.getId() + " (" + device.getName() + ") 已离线",
                    null,
                    device.getId()
                );
            } else if (!device.isOnline() && shouldBeOnline) {
                // 设备上线
                device.setOnline(true);
                deviceRepository.save(device);

                // 记录日志
                systemLogService.info("DEVICE", 
                    "Device came online: " + device.getId(), 
                    "DeviceMonitor");
            }
        }
    }
}
