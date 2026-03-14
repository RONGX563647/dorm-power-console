package com.dormpower.service;

import com.dormpower.model.Telemetry;
import com.dormpower.repository.TelemetryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * 遥测数据采集单元测试
 *
 * 测试用例覆盖：
 * - TC-TELE-001: 正常采集遥测数据
 * - TC-TELE-002: 采集数据字段缺失
 * - TC-TELE-003: Kafka不可用时直接存储
 * - TC-TELE-004: 高频数据采集
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class TelemetryServiceCollectTest {

    @Mock
    private TelemetryRepository telemetryRepository;

    @InjectMocks
    private TelemetryService telemetryService;

    // ==================== TC-TELE-001: 正常采集遥测数据 ====================

    @Test
    @DisplayName("TC-TELE-001: 正常采集遥测数据 - 数据存储成功")
    void testCollectTelemetry_Success() {
        // Given
        String deviceId = "device_001";
        long ts = System.currentTimeMillis() / 1000;
        double powerW = 100.5;
        double voltageV = 220.0;
        double currentA = 0.45;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(invocation -> {
            Telemetry t = invocation.getArgument(0);
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
    @DisplayName("TC-TELE-001: 正常采集遥测数据 - 所有字段完整")
    void testCollectTelemetry_AllFieldsPresent() {
        // Given
        String deviceId = "device_002";
        long ts = 1700000000L;
        double powerW = 150.0;
        double voltageV = 215.5;
        double currentA = 0.68;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(invocation -> {
            Telemetry t = invocation.getArgument(0);
            t.setId(2L);
            return t;
        });

        // When
        Telemetry result = telemetryService.collectTelemetry(deviceId, ts, powerW, voltageV, currentA);

        // Then
        assertNotNull(result);
        assertEquals(215.5, result.getVoltageV());
        assertEquals(0.68, result.getCurrentA());
        assertEquals(150.0, result.getPowerW());
    }

    // ==================== TC-TELE-002: 采集数据字段缺失 ====================

    @Test
    @DisplayName("TC-TELE-002: 采集数据字段缺失 - 缺少voltageV字段，使用默认值220V")
    void testCollectTelemetry_MissingVoltageV_DefaultValue() {
        // Given
        String deviceId = "device_001";
        long ts = System.currentTimeMillis() / 1000;
        Double voltageV = null; // 缺失
        double powerW = 100.0;
        double currentA = 0.45;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(invocation -> {
            Telemetry t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        // When
        Telemetry result = telemetryService.collectTelemetry(deviceId, ts, powerW, voltageV, currentA);

        // Then
        assertNotNull(result);
        assertEquals(TelemetryService.DEFAULT_VOLTAGE_V, result.getVoltageV());
        assertEquals(220.0, result.getVoltageV());
        verify(telemetryRepository).save(any(Telemetry.class));
    }

    @Test
    @DisplayName("TC-TELE-002: 采集数据字段缺失 - 缺少currentA字段")
    void testCollectTelemetry_MissingCurrentA_DefaultValue() {
        // Given
        String deviceId = "device_001";
        Double currentA = null; // 缺失
        long ts = System.currentTimeMillis() / 1000;
        double powerW = 100.0;
        double voltageV = 220.0;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Telemetry result = telemetryService.collectTelemetry(deviceId, ts, powerW, voltageV, currentA);

        // Then
        assertNotNull(result);
        assertEquals(TelemetryService.DEFAULT_CURRENT_A, result.getCurrentA());
        assertEquals(0.0, result.getCurrentA());
    }

    @Test
    @DisplayName("TC-TELE-002: 采集数据字段缺失 - 缺少powerW字段")
    void testCollectTelemetry_MissingPowerW_DefaultValue() {
        // Given
        String deviceId = "device_001";
        Double powerW = null; // 缺失
        long ts = System.currentTimeMillis() / 1000;
        double voltageV = 220.0;
        double currentA = 0.5;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Telemetry result = telemetryService.collectTelemetry(deviceId, ts, powerW, voltageV, currentA);

        // Then
        assertNotNull(result);
        assertEquals(TelemetryService.DEFAULT_POWER_W, result.getPowerW());
        assertEquals(0.0, result.getPowerW());
    }

    @Test
    @DisplayName("TC-TELE-002: 采集数据字段缺失 - 缺少时间戳，使用当前时间")
    void testCollectTelemetry_MissingTs_UseCurrentTime() {
        // Given
        String deviceId = "device_001";
        Long ts = null; // 缺失
        double powerW = 100.0;
        double voltageV = 220.0;
        double currentA = 0.5;
        long beforeCall = System.currentTimeMillis() / 1000;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Telemetry result = telemetryService.collectTelemetry(deviceId, ts, powerW, voltageV, currentA);
        long afterCall = System.currentTimeMillis() / 1000;

        // Then
        assertNotNull(result);
        assertTrue(result.getTs() >= beforeCall && result.getTs() <= afterCall);
    }

    @Test
    @DisplayName("TC-TELE-002: 采集数据字段缺失 - 多个字段缺失")
    void testCollectTelemetry_MultipleFieldsMissing() {
        // Given
        String deviceId = "device_001";
        Double powerW = null;
        Double voltageV = null;
        Double currentA = null;
        Long ts = null;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Telemetry result = telemetryService.collectTelemetry(deviceId, ts, powerW, voltageV, currentA);

        // Then
        assertNotNull(result);
        assertEquals(TelemetryService.DEFAULT_VOLTAGE_V, result.getVoltageV());
        assertEquals(TelemetryService.DEFAULT_CURRENT_A, result.getCurrentA());
        assertEquals(TelemetryService.DEFAULT_POWER_W, result.getPowerW());
        assertTrue(result.getTs() > 0);
    }

    // ==================== TC-TELE-003: Kafka不可用时直接存储 ====================

    @Test
    @DisplayName("TC-TELE-003: Kafka不可用时 - 直接存储到数据库")
    void testCollectTelemetry_KafkaDisabled_DirectStorage() {
        // Given - 即使Kafka不可用，TelemetryService也应能直接存储数据
        String deviceId = "device_001";
        long ts = System.currentTimeMillis() / 1000;
        double powerW = 100.0;
        double voltageV = 220.0;
        double currentA = 0.45;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(invocation -> {
            Telemetry t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        // When - 直接调用collectTelemetry，不经过Kafka
        Telemetry result = telemetryService.collectTelemetry(deviceId, ts, powerW, voltageV, currentA);

        // Then - 数据应该成功存储
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(deviceId, result.getDeviceId());
        verify(telemetryRepository).save(any(Telemetry.class));
    }

    // ==================== TC-TELE-004: 高频数据采集 ====================

    @Test
    @DisplayName("TC-TELE-004: 高频数据采集 - 100条全部成功存储")
    void testCollectTelemetryBatch_HighFrequency_Success() {
        // Given - 模拟100条数据
        int count = 100;
        List<Telemetry> telemetryList = new ArrayList<>(count);
        long baseTs = System.currentTimeMillis() / 1000;

        for (int i = 0; i < count; i++) {
            Telemetry t = new Telemetry();
            t.setDeviceId("device_001");
            t.setTs(baseTs + i);
            t.setPowerW(100.0 + i * 0.5);
            t.setVoltageV(220.0);
            t.setCurrentA(0.45 + i * 0.01);
            telemetryList.add(t);
        }

        when(telemetryRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Telemetry> list = invocation.getArgument(0);
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setId((long) i + 1);
            }
            return list;
        });

        // When
        int savedCount = telemetryService.collectTelemetryBatch(telemetryList);

        // Then
        assertEquals(count, savedCount);
        verify(telemetryRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("TC-TELE-004: 高频数据采集 - 批量保存性能验证")
    void testCollectTelemetryBatch_Performance() {
        // Given - 100条数据
        List<Telemetry> telemetryList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Telemetry t = new Telemetry();
            t.setDeviceId("device_" + (i % 10));
            t.setTs(System.currentTimeMillis() / 1000 + i);
            t.setPowerW(100.0);
            t.setVoltageV(220.0);
            t.setCurrentA(0.5);
            telemetryList.add(t);
        }

        when(telemetryRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        long start = System.nanoTime();
        int result = telemetryService.collectTelemetryBatch(telemetryList);
        long durationMs = (System.nanoTime() - start) / 1_000_000;

        // Then
        assertEquals(100, result);
        assertTrue(durationMs < 1000, "批量保存应该在1秒内完成");
    }

    @Test
    @DisplayName("TC-TELE-004: 高频数据采集 - 空列表处理")
    void testCollectTelemetryBatch_EmptyList() {
        // Given
        List<Telemetry> emptyList = new ArrayList<>();

        // When
        int result = telemetryService.collectTelemetryBatch(emptyList);

        // Then
        assertEquals(0, result);
        verify(telemetryRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("TC-TELE-004: 高频数据采集 - null列表处理")
    void testCollectTelemetryBatch_NullList() {
        // When
        int result = telemetryService.collectTelemetryBatch(null);

        // Then
        assertEquals(0, result);
        verify(telemetryRepository, never()).saveAll(anyList());
    }

    // ==================== 其他边界测试 ====================

    @Test
    @DisplayName("边界测试: 电压为0时使用默认值")
    void testCollectTelemetryBatch_ZeroVoltage_DefaultValue() {
        // Given
        List<Telemetry> telemetryList = new ArrayList<>();
        Telemetry t = new Telemetry();
        t.setDeviceId("device_001");
        t.setTs(System.currentTimeMillis() / 1000);
        t.setPowerW(100.0);
        t.setVoltageV(0); // 电压为0
        t.setCurrentA(0.5);
        telemetryList.add(t);

        when(telemetryRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        telemetryService.collectTelemetryBatch(telemetryList);

        // Then - 电压为0时应该被替换为默认值
        assertEquals(TelemetryService.DEFAULT_VOLTAGE_V, t.getVoltageV());
    }

    @Test
    @DisplayName("边界测试: 负数功率值")
    void testCollectTelemetry_NegativePower() {
        // Given - 功率理论上不应为负，但系统应能处理
        String deviceId = "device_001";
        double negativePower = -10.0;

        when(telemetryRepository.save(any(Telemetry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Telemetry result = telemetryService.collectTelemetry(deviceId, null, negativePower, 220.0, 0.5);

        // Then - 系统应接受该值（实际应用中可能需要验证）
        assertNotNull(result);
        assertEquals(-10.0, result.getPowerW());
    }

}