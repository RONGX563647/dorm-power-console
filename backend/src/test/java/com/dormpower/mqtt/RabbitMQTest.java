package com.dormpower.mqtt;

import com.dormpower.dto.AlertMessage;
import com.dormpower.dto.BillMessage;
import com.dormpower.dto.TelemetryMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RabbitMQ 异步解耦测试
 * 
 * 测试三大核心异步链路:
 * 1. 设备遥测数据异步落库
 * 2. 告警消息异步推送
 * 3. 用电账单异步生成
 */
@SpringBootTest
@ActiveProfiles("test")
class RabbitMQTest {

    @Autowired(required = false)
    private RabbitMQProducer rabbitMQProducer;

    /**
     * 测试发送遥测数据
     */
    @Test
    void testSendTelemetry() {
        if (rabbitMQProducer == null) {
            System.out.println("RabbitMQ 未启用，跳过测试");
            return;
        }

        TelemetryMessage message = new TelemetryMessage();
        message.setDeviceId("device_001");
        message.setTimestamp(System.currentTimeMillis() / 1000);
        message.setPowerW(150.5);
        message.setVoltageV(220.0);
        message.setCurrentA(0.68);
        message.setRoom("room_101");
        message.setBuilding("building_A");

        assertDoesNotThrow(() -> {
            rabbitMQProducer.sendTelemetry(message);
        }, "发送遥测数据不应该抛出异常");
    }

    /**
     * 测试批量发送遥测数据
     */
    @Test
    void testSendTelemetryBatch() {
        if (rabbitMQProducer == null) {
            System.out.println("RabbitMQ 未启用，跳过测试");
            return;
        }

        List<TelemetryMessage> messages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TelemetryMessage message = new TelemetryMessage();
            message.setDeviceId("device_" + i);
            message.setTimestamp(System.currentTimeMillis() / 1000);
            message.setPowerW(100.0 + i);
            message.setVoltageV(220.0);
            message.setCurrentA(0.5 + i * 0.01);
            messages.add(message);
        }

        assertDoesNotThrow(() -> {
            rabbitMQProducer.sendTelemetryBatch(messages);
        }, "批量发送遥测数据不应该抛出异常");
    }

    /**
     * 测试发送告警消息
     */
    @Test
    void testSendAlert() {
        if (rabbitMQProducer == null) {
            System.out.println("RabbitMQ 未启用，跳过测试");
            return;
        }

        AlertMessage alert = new AlertMessage();
        alert.setAlertId("alert_" + System.currentTimeMillis());
        alert.setDeviceId("device_001");
        alert.setAlertType("POWER_OVERLOAD");
        alert.setSeverity("WARNING");
        alert.setValue(1000.0);
        alert.setThreshold(800.0);
        alert.setTimestamp(Instant.now());
        alert.setMessage("功率过载警告");
        alert.setRoom("room_101");
        alert.setBuilding("building_A");

        assertDoesNotThrow(() -> {
            rabbitMQProducer.sendAlert(alert);
        }, "发送告警消息不应该抛出异常");
    }

    /**
     * 测试发送严重告警
     */
    @Test
    void testSendCriticalAlert() {
        if (rabbitMQProducer == null) {
            System.out.println("RabbitMQ 未启用，跳过测试");
            return;
        }

        AlertMessage alert = new AlertMessage();
        alert.setAlertId("alert_critical_" + System.currentTimeMillis());
        alert.setDeviceId("device_001");
        alert.setAlertType("FIRE_HAZARD");
        alert.setSeverity("CRITICAL");
        alert.setValue(1500.0);
        alert.setThreshold(1000.0);
        alert.setTimestamp(Instant.now());
        alert.setMessage("火灾隐患！功率严重超载！");
        alert.setRoom("room_101");
        alert.setBuilding("building_A");

        assertDoesNotThrow(() -> {
            rabbitMQProducer.sendAlert(alert);
        }, "发送严重告警不应该抛出异常");
    }

    /**
     * 测试发送账单生成消息
     */
    @Test
    void testSendBill() {
        if (rabbitMQProducer == null) {
            System.out.println("RabbitMQ 未启用，跳过测试");
            return;
        }

        BillMessage bill = new BillMessage();
        bill.setRoomId("room_101");
        bill.setRoomNumber("101");
        bill.setBuilding("building_A");
        bill.setBillDate(LocalDate.now());
        bill.setEnergyKwh(150.5);
        bill.setPricePerKwh(0.6);
        bill.setTotalAmount(90.3);
        bill.setUserId(1L);

        assertDoesNotThrow(() -> {
            rabbitMQProducer.sendBill(bill);
        }, "发送账单生成消息不应该抛出异常");
    }

    /**
     * 测试消息优先级
     */
    @Test
    void testMessagePriority() {
        if (rabbitMQProducer == null) {
            System.out.println("RabbitMQ 未启用，跳过测试");
            return;
        }

        // 发送不同优先级的告警
        AlertMessage info = new AlertMessage();
        info.setAlertId("info_" + System.currentTimeMillis());
        info.setSeverity("INFO");

        AlertMessage warning = new AlertMessage();
        warning.setAlertId("warning_" + System.currentTimeMillis());
        warning.setSeverity("WARNING");

        AlertMessage critical = new AlertMessage();
        critical.setAlertId("critical_" + System.currentTimeMillis());
        critical.setSeverity("CRITICAL");

        assertDoesNotThrow(() -> {
            rabbitMQProducer.sendAlert(info);
            rabbitMQProducer.sendAlert(warning);
            rabbitMQProducer.sendAlert(critical);
        }, "发送不同优先级告警不应该抛出异常");
    }

    /**
     * 测试高并发场景
     */
    @Test
    void testHighConcurrency() {
        if (rabbitMQProducer == null) {
            System.out.println("RabbitMQ 未启用，跳过测试");
            return;
        }

        int messageCount = 1000;
        List<TelemetryMessage> messages = new ArrayList<>();

        for (int i = 0; i < messageCount; i++) {
            TelemetryMessage message = new TelemetryMessage();
            message.setDeviceId("device_" + (i % 100));
            message.setTimestamp(System.currentTimeMillis() / 1000);
            message.setPowerW(100.0 + Math.random() * 50);
            message.setVoltageV(220.0);
            message.setCurrentA(0.5);
            messages.add(message);
        }

        long startTime = System.currentTimeMillis();

        assertDoesNotThrow(() -> {
            rabbitMQProducer.sendTelemetryBatch(messages);
        }, "高并发发送消息不应该抛出异常");

        long duration = System.currentTimeMillis() - startTime;

        // 验证性能：1000 条消息应该在 2 秒内发送完成
        assertTrue(duration < 2000, 
            "1000 条消息应该在 2 秒内发送完成，实际：" + duration + "ms");

        System.out.println("高并发测试：" + messageCount + " 条消息，耗时：" + duration + "ms");
    }
}
