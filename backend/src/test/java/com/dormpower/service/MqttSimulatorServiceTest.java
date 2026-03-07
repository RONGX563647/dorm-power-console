package com.dormpower.service;

import com.dormpower.dto.MqttSimulatorRequest;
import com.dormpower.dto.MqttSimulatorResponse;
import com.dormpower.dto.MqttSimulatorStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MQTT模拟器服务测试
 */
@SpringBootTest
public class MqttSimulatorServiceTest {

    @Autowired
    private MqttSimulatorService mqttSimulatorService;

    @Test
    public void testStartSimulator() throws InterruptedException {
        // 创建模拟器请求
        MqttSimulatorRequest request = MqttSimulatorRequest.builder()
                .devices(5)
                .duration(5)
                .interval(0.5)
                .build();

        // 启动模拟器
        MqttSimulatorResponse response = mqttSimulatorService.startSimulator(request);
        assertNotNull(response);
        assertEquals("STARTED", response.getStatus());
        assertNotNull(response.getTaskId());

        // 等待模拟器运行一段时间
        TimeUnit.SECONDS.sleep(2);

        // 获取模拟器状态
        MqttSimulatorStatus status = mqttSimulatorService.getSimulatorStatus(response.getTaskId());
        assertNotNull(status);
        assertEquals("RUNNING", status.getStatus());
        assertEquals(5, status.getDevices());
        assertEquals(5, status.getDuration());
        assertEquals(0.5, status.getInterval());

        // 停止模拟器
        MqttSimulatorResponse stopResponse = mqttSimulatorService.stopSimulator(response.getTaskId());
        assertNotNull(stopResponse);
        assertEquals("STOPPED", stopResponse.getStatus());

        // 验证模拟器已停止
        MqttSimulatorStatus stoppedStatus = mqttSimulatorService.getSimulatorStatus(response.getTaskId());
        assertEquals("NOT_FOUND", stoppedStatus.getStatus());
    }

    @Test
    public void testGetAllSimulatorTasks() {
        // 创建并启动两个模拟器
        MqttSimulatorRequest request1 = MqttSimulatorRequest.builder()
                .devices(3)
                .duration(3)
                .interval(1)
                .build();

        MqttSimulatorRequest request2 = MqttSimulatorRequest.builder()
                .devices(2)
                .duration(2)
                .interval(0.5)
                .build();

        MqttSimulatorResponse response1 = mqttSimulatorService.startSimulator(request1);
        MqttSimulatorResponse response2 = mqttSimulatorService.startSimulator(request2);

        // 获取所有模拟器任务
        List<MqttSimulatorStatus> tasks = mqttSimulatorService.getAllSimulatorTasks();
        assertNotNull(tasks);
        assertTrue(tasks.size() >= 2);

        // 停止模拟器
        mqttSimulatorService.stopSimulator(response1.getTaskId());
        mqttSimulatorService.stopSimulator(response2.getTaskId());
    }

    @Test
    public void testStopNonExistentSimulator() {
        // 尝试停止不存在的模拟器
        MqttSimulatorResponse response = mqttSimulatorService.stopSimulator("non_existent_task");
        assertNotNull(response);
        assertEquals("NOT_FOUND", response.getStatus());
    }

    @Test
    public void testGetNonExistentSimulatorStatus() {
        // 尝试获取不存在的模拟器状态
        MqttSimulatorStatus status = mqttSimulatorService.getSimulatorStatus("non_existent_task");
        assertNotNull(status);
        assertEquals("NOT_FOUND", status.getStatus());
    }
}
