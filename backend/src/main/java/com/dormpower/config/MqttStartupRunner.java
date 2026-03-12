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

    /**
     * 认证服务接口
     */
    @Autowired
    private AuthService authService;
    /**
     * 设备数据访问接口
     */

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
        
        // 种子数据初始化已移除
        markTimeouts();
        System.out.println("Command timeouts marked");
    }

    // 种子数据初始化方法已移除

    // 遥测数据初始化方法已移除

    /**
     * 标记超时的命令
     */
    private void markTimeouts() {
        commandService.markTimeouts();
    }

}
