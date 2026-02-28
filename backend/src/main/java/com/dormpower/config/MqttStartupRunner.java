package com.dormpower.config;

import com.dormpower.model.Device;
import com.dormpower.model.StripStatus;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.StripStatusRepository;
import com.dormpower.service.AuthService;
import com.dormpower.service.CommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用启动运行器
 * 在应用启动时初始化种子数据和管理员账户
 */
@Component
public class MqttStartupRunner implements ApplicationRunner {

    @Autowired
    private AuthService authService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private StripStatusRepository stripStatusRepository;

    @Autowired
    private CommandService commandService;

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("Initializing application data...");
        
        authService.ensureDefaultAdmin();
        System.out.println("Default admin account ensured");
        
        ensureSeedData();
        System.out.println("Seed data ensured");
        
        markTimeouts();
        System.out.println("Command timeouts marked");
    }

    /**
     * 确保种子数据存在
     */
    private void ensureSeedData() {
        if (deviceRepository.count() > 0) {
            return;
        }

        long now = System.currentTimeMillis() / 1000;

        Device device1 = new Device();
        device1.setId("strip01");
        device1.setName("宿舍101插座");
        device1.setRoom("A-302");
        device1.setOnline(true);
        device1.setLastSeenTs(now);
        device1.setCreatedAt(now);
        deviceRepository.save(device1);

        StripStatus status1 = new StripStatus();
        status1.setDeviceId("strip01");
        status1.setTs(now);
        status1.setOnline(true);
        status1.setTotalPowerW(120.5);
        status1.setVoltageV(220.0);
        status1.setCurrentA(0.55);
        status1.setSocketsJson("[{\"id\":1,\"on\":true,\"power_w\":60.2},{\"id\":2,\"on\":false,\"power_w\":0.0},{\"id\":3,\"on\":true,\"power_w\":60.3}]");
        stripStatusRepository.save(status1);

        Device device2 = new Device();
        device2.setId("strip02");
        device2.setName("宿舍102插座");
        device2.setRoom("A-303");
        device2.setOnline(false);
        device2.setLastSeenTs(now - 3600);
        device2.setCreatedAt(now);
        deviceRepository.save(device2);

        StripStatus status2 = new StripStatus();
        status2.setDeviceId("strip02");
        status2.setTs(now - 3600);
        status2.setOnline(false);
        status2.setTotalPowerW(85.0);
        status2.setVoltageV(220.0);
        status2.setCurrentA(0.0);
        status2.setSocketsJson("[{\"id\":1,\"on\":false,\"power_w\":0.0},{\"id\":2,\"on\":false,\"power_w\":0.0},{\"id\":3,\"on\":false,\"power_w\":0.0}]");
        stripStatusRepository.save(status2);

        Device device3 = new Device();
        device3.setId("strip03");
        device3.setName("宿舍103插座");
        device3.setRoom("A-304");
        device3.setOnline(true);
        device3.setLastSeenTs(now);
        device3.setCreatedAt(now);
        deviceRepository.save(device3);

        StripStatus status3 = new StripStatus();
        status3.setDeviceId("strip03");
        status3.setTs(now);
        status3.setOnline(true);
        status3.setTotalPowerW(95.8);
        status3.setVoltageV(220.0);
        status3.setCurrentA(0.43);
        status3.setSocketsJson("[{\"id\":1,\"on\":true,\"power_w\":45.0},{\"id\":2,\"on\":true,\"power_w\":50.8},{\"id\":3,\"on\":false,\"power_w\":0.0}]");
        stripStatusRepository.save(status3);
    }

    /**
     * 标记超时的命令
     */
    private void markTimeouts() {
        commandService.markTimeouts();
    }

}
