package com.dormpower.config;

import com.dormpower.model.Device;
import com.dormpower.model.StripStatus;
import com.dormpower.model.Telemetry;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.StripStatusRepository;
import com.dormpower.repository.TelemetryRepository;
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

    @Autowired
    private TelemetryRepository telemetryRepository;

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

        // 初始化遥测数据
        initializeTelemetryData(now);
    }

    /**
     * 初始化遥测数据
     * @param now 当前时间戳
     */
    private void initializeTelemetryData(long now) {
        // 先删除现有的遥测数据，确保每次启动都重新生成
        telemetryRepository.deleteAll();

        // 为每个设备生成过去24小时的遥测数据，每10分钟一条
        String[] deviceIds = {"strip01", "strip02", "strip03"};
        double[][] baseValues = {
                {120.5, 220.0, 0.55},  // strip01
                {85.0, 220.0, 0.38},   // strip02
                {95.8, 220.0, 0.43}    // strip03
        };

        for (int i = 0; i < deviceIds.length; i++) {
            String deviceId = deviceIds[i];
            double basePower = baseValues[i][0];
            double baseVoltage = baseValues[i][1];
            double baseCurrent = baseValues[i][2];

            // 生成过去24小时的数据，每10分钟一条
            for (int j = 0; j < 144; j++) {  // 24小时 * 6 = 144个10分钟间隔
                long ts = now - (j * 600);  // 600秒 = 10分钟
                
                // 添加一些随机波动
                double powerW = basePower + (Math.random() * 10 - 5);
                double voltageV = baseVoltage + (Math.random() * 5 - 2.5);
                double currentA = baseCurrent + (Math.random() * 0.1 - 0.05);

                // 确保值不为负
                powerW = Math.max(0, powerW);
                voltageV = Math.max(0, voltageV);
                currentA = Math.max(0, currentA);

                Telemetry telemetry = new Telemetry();
                telemetry.setDeviceId(deviceId);
                telemetry.setTs(ts);
                telemetry.setPowerW(Math.round(powerW * 100.0) / 100.0);
                telemetry.setVoltageV(Math.round(voltageV * 100.0) / 100.0);
                telemetry.setCurrentA(Math.round(currentA * 100.0) / 100.0);

                telemetryRepository.save(telemetry);
            }

            // 生成最近60秒的数据，每1秒一条，确保测试时能获取到数据
            for (int j = 0; j < 60; j++) {
                long ts = now - j;  // 每1秒一条
                
                // 添加一些随机波动
                double powerW = basePower + (Math.random() * 5 - 2.5);
                double voltageV = baseVoltage + (Math.random() * 2 - 1);
                double currentA = baseCurrent + (Math.random() * 0.05 - 0.025);

                // 确保值不为负
                powerW = Math.max(0, powerW);
                voltageV = Math.max(0, voltageV);
                currentA = Math.max(0, currentA);

                Telemetry telemetry = new Telemetry();
                telemetry.setDeviceId(deviceId);
                telemetry.setTs(ts);
                telemetry.setPowerW(Math.round(powerW * 100.0) / 100.0);
                telemetry.setVoltageV(Math.round(voltageV * 100.0) / 100.0);
                telemetry.setCurrentA(Math.round(currentA * 100.0) / 100.0);

                telemetryRepository.save(telemetry);
            }
        }
    }

    /**
     * 标记超时的命令
     */
    private void markTimeouts() {
        commandService.markTimeouts();
    }

}
