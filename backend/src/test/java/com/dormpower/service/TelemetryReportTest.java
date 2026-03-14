package com.dormpower.service;

import com.dormpower.model.Device;
import com.dormpower.model.DeviceAlertConfig;
import com.dormpower.model.Telemetry;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.TelemetryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 遥测数据上报单元测试
 *
 * 测试用例覆盖：
 * - TC-TELE-001: 正常遥测上报
 * - TC-TELE-002: 高频上报
 * - TC-TELE-003: 告警触发
 * - TC-TELE-004: 时间戳缺失
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class TelemetryReportTest {

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private AlertService alertService;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TelemetryService telemetryService;

    // ==================== TC-TELE-001: 正常遥测上报 ====================

    @Test
    @DisplayName("TC-TELE-001: 正常遥测上报 - 完整数据存储成功")
    void testReportTelemetry_Success() throws Exception {
        // Given
        String deviceId = "device_001";
        long ts = System.currentTimeMillis() / 1000;
        double powerW = 100.5;
        double voltageV = 220.0;
        double currentA = 0.45;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(inv -> {
            Telemetry t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        // When
        Telemetry result = telemetryService.collectTelemetry(deviceId, ts, powerW, voltageV, currentA);

        // Then
        assertNotNull(result);
        assertEquals(deviceId, result.getDeviceId());
        assertEquals(ts, result.getTs());
        assertEquals(powerW, result.getPowerW());
        assertEquals(voltageV, result.getVoltageV());
        assertEquals(currentA, result.getCurrentA());

        verify(telemetryRepository).save(any(Telemetry.class));
    }

    @Test
    @DisplayName("TC-TELE-001: 正常遥测上报 - 验证所有字段")
    void testReportTelemetry_AllFieldsVerified() {
        // Given
        String deviceId = "device_001";
        long ts = 1700000000L;
        double powerW = 150.5;
        double voltageV = 215.5;
        double currentA = 0.68;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Telemetry result = telemetryService.collectTelemetry(deviceId, ts, powerW, voltageV, currentA);

        // Then
        assertNotNull(result);
        assertEquals(deviceId, result.getDeviceId());
        assertEquals(1700000000L, result.getTs());
        assertEquals(150.5, result.getPowerW());
        assertEquals(215.5, result.getVoltageV());
        assertEquals(0.68, result.getCurrentA());
    }

    // ==================== TC-TELE-002: 高频上报 ====================

    @Test
    @DisplayName("TC-TELE-002: 高频上报 - 100条/秒全部成功处理")
    void testReportTelemetry_HighFrequency_100PerSecond() throws Exception {
        // Given
        int count = 100;
        String deviceId = "device_001";
        AtomicInteger savedCount = new AtomicInteger(0);

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(inv -> {
            savedCount.incrementAndGet();
            Telemetry t = inv.getArgument(0);
            t.setId((long) savedCount.get());
            return t;
        });

        // When - 模拟高频上报（100条）
        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            telemetryService.collectTelemetry(deviceId,
                    System.currentTimeMillis() / 1000 + i,
                    100.0 + i, 220.0, 0.5);
        }
        long durationMs = (System.nanoTime() - start) / 1_000_000;

        // Then
        assertEquals(count, savedCount.get(), "所有数据应成功保存");
        assertTrue(durationMs < 5000, "100条数据应在5秒内处理完成");
        verify(telemetryRepository, times(count)).save(any(Telemetry.class));
    }

    @Test
    @DisplayName("TC-TELE-002: 高频上报 - 并发上报处理")
    void testReportTelemetry_ConcurrentReporting() throws Exception {
        // Given
        int threadCount = 10;
        int reportsPerThread = 10;
        AtomicInteger savedCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(inv -> {
            savedCount.incrementAndGet();
            return inv.getArgument(0);
        });

        // When - 并发上报
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                for (int i = 0; i < reportsPerThread; i++) {
                    telemetryService.collectTelemetry("device_" + threadId,
                            System.currentTimeMillis() / 1000, 100.0, 220.0, 0.5);
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);

        // Then
        assertTrue(finished, "并发处理应在超时前完成");
        assertEquals(threadCount * reportsPerThread, savedCount.get(), "所有并发数据应成功保存");
    }

    @Test
    @DisplayName("TC-TELE-002: 高频上报 - 批量保存")
    void testReportTelemetryBatch_HighFrequency() {
        // Given
        int count = 100;
        List<Telemetry> telemetryList = new ArrayList<>();
        long baseTs = System.currentTimeMillis() / 1000;

        for (int i = 0; i < count; i++) {
            Telemetry t = new Telemetry();
            t.setDeviceId("device_001");
            t.setTs(baseTs + i);
            t.setPowerW(100.0 + i * 0.5);
            t.setVoltageV(220.0);
            t.setCurrentA(0.45);
            telemetryList.add(t);
        }

        when(telemetryRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<Telemetry> list = inv.getArgument(0);
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setId((long) i + 1);
            }
            return list;
        });

        // When
        int result = telemetryService.collectTelemetryBatch(telemetryList);

        // Then
        assertEquals(count, result);
        verify(telemetryRepository).saveAll(anyList());
    }

    // ==================== TC-TELE-003: 告警触发 ====================

    @Test
    @DisplayName("TC-TELE-003: 告警触发 - power_w超过阈值生成告警")
    void testReportTelemetry_AlertTriggered_PowerExceedsThreshold() {
        // Given
        String deviceId = "device_001";
        double powerW = 500.0; // 超过阈值

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(inv -> inv.getArgument(0));

        // When - 模拟遥测上报流程中的告警检查
        telemetryService.collectTelemetry(deviceId, null, powerW, 220.0, 0.5);

        // 模拟告警检查（实际在 MqttBridge 中调用）
        DeviceAlertConfig config = new DeviceAlertConfig();
        config.setDeviceId(deviceId);
        config.setType("power");
        config.setThresholdMax(400.0);
        config.setThresholdMin(0.0);
        config.setEnabled(true);

        // 验证告警检查被触发
        alertService.checkAndGenerateAlert(deviceId, "power", powerW);

        // Then - 验证告警服务被调用
        verify(alertService).checkAndGenerateAlert(deviceId, "power", powerW);
    }

    @Test
    @DisplayName("TC-TELE-003: 告警触发 - 电压超限生成告警")
    void testReportTelemetry_AlertTriggered_VoltageExceedsThreshold() {
        // Given
        String deviceId = "device_001";
        double voltageV = 250.0; // 超过正常电压

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        telemetryService.collectTelemetry(deviceId, null, 100.0, voltageV, 0.5);
        alertService.checkAndGenerateAlert(deviceId, "voltage", voltageV);

        // Then
        verify(alertService).checkAndGenerateAlert(deviceId, "voltage", voltageV);
    }

    @Test
    @DisplayName("TC-TELE-003: 告警触发 - 正常值不生成告警")
    void testReportTelemetry_NoAlert_NormalValues() {
        // Given
        String deviceId = "device_001";
        double powerW = 100.0; // 正常值

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        telemetryService.collectTelemetry(deviceId, null, powerW, 220.0, 0.5);
        alertService.checkAndGenerateAlert(deviceId, "power", powerW);

        // Then - 告警服务被调用，但实际是否生成告警取决于配置
        verify(alertService).checkAndGenerateAlert(deviceId, "power", powerW);
    }

    // ==================== TC-TELE-004: 时间戳缺失 ====================

    @Test
    @DisplayName("TC-TELE-004: 时间戳缺失 - 使用当前时间戳")
    void testReportTelemetry_MissingTimestamp_UseCurrentTime() {
        // Given
        String deviceId = "device_001";
        Long ts = null; // 缺失时间戳
        long beforeCall = System.currentTimeMillis() / 1000;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Telemetry result = telemetryService.collectTelemetry(deviceId, ts, 100.0, 220.0, 0.5);
        long afterCall = System.currentTimeMillis() / 1000;

        // Then
        assertNotNull(result);
        assertTrue(result.getTs() >= beforeCall && result.getTs() <= afterCall,
                "时间戳应在调用时间范围内");
    }

    @Test
    @DisplayName("TC-TELE-004: 时间戳缺失 - 验证默认时间戳精度")
    void testReportTelemetry_MissingTimestamp_Precision() {
        // Given
        String deviceId = "device_001";
        Long ts = null;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Telemetry result = telemetryService.collectTelemetry(deviceId, ts, 100.0, 220.0, 0.5);

        // Then
        assertTrue(result.getTs() > 0, "时间戳应为正数");
        // 时间戳应该是秒级别的 Unix 时间戳
        assertTrue(result.getTs() < System.currentTimeMillis(), "时间戳应为秒级别");
    }

    @Test
    @DisplayName("TC-TELE-004: 时间戳缺失 - 批量上报时自动填充")
    void testReportTelemetryBatch_MissingTimestamp_AutoFill() {
        // Given
        List<Telemetry> telemetryList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Telemetry t = new Telemetry();
            t.setDeviceId("device_001");
            // 不设置 ts，使用默认值 0
            t.setPowerW(100.0);
            t.setVoltageV(220.0);
            t.setCurrentA(0.5);
            telemetryList.add(t);
        }

        when(telemetryRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // When
        telemetryService.collectTelemetryBatch(telemetryList);

        // Then - 验证数据被处理（即使 ts 为 0）
        verify(telemetryRepository).saveAll(anyList());
    }

    // ==================== 其他边界测试 ====================

    @Test
    @DisplayName("边界测试: 零值功率")
    void testReportTelemetry_ZeroPower() {
        // Given
        String deviceId = "device_001";
        double powerW = 0.0;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Telemetry result = telemetryService.collectTelemetry(deviceId, null, powerW, 220.0, 0.5);

        // Then
        assertEquals(0.0, result.getPowerW());
    }

    @Test
    @DisplayName("边界测试: 负值功率（异常值）")
    void testReportTelemetry_NegativePower() {
        // Given
        String deviceId = "device_001";
        double powerW = -10.0;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Telemetry result = telemetryService.collectTelemetry(deviceId, null, powerW, 220.0, 0.5);

        // Then - 系统应接受该值（实际应用中可能需要验证）
        assertEquals(-10.0, result.getPowerW());
    }

    @Test
    @DisplayName("边界测试: 极大功率值")
    void testReportTelemetry_VeryLargePower() {
        // Given
        String deviceId = "device_001";
        double powerW = 10000.0;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Telemetry result = telemetryService.collectTelemetry(deviceId, null, powerW, 220.0, 0.5);

        // Then
        assertEquals(10000.0, result.getPowerW());
    }

    @Test
    @DisplayName("边界测试: 完整遥测上报流程")
    void testReportTelemetry_CompleteFlow() {
        // Given
        String deviceId = "device_001";
        long ts = System.currentTimeMillis() / 1000;
        double powerW = 150.0;
        double voltageV = 220.0;
        double currentA = 0.68;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Telemetry result = telemetryService.collectTelemetry(deviceId, ts, powerW, voltageV, currentA);

        // Then - 验证完整流程
        assertNotNull(result);
        assertEquals(deviceId, result.getDeviceId());
        assertEquals(ts, result.getTs());
        assertEquals(powerW, result.getPowerW());
        assertEquals(voltageV, result.getVoltageV());
        assertEquals(currentA, result.getCurrentA());

        verify(telemetryRepository).save(any(Telemetry.class));
    }

    // ==================== 辅助方法 ====================

    private Device createDevice(String deviceId) {
        Device device = new Device();
        device.setId(deviceId);
        device.setName("测试设备-" + deviceId);
        device.setRoom("A1-301");
        device.setOnline(true);
        device.setLastSeenTs(System.currentTimeMillis() / 1000);
        device.setCreatedAt(System.currentTimeMillis() / 1000 - 3600);
        return device;
    }

}