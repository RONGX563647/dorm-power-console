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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 遥测数据采样单元测试
 *
 * 测试用例覆盖：
 * - TC-SAMPLE-001: 60秒槽位填充
 * - TC-SAMPLE-002: 24小时采样
 * - TC-SAMPLE-003: 数据点不足
 * - TC-SAMPLE-004: 空数据槽位
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class TelemetryServiceSampleTest {

    @Mock
    private TelemetryRepository telemetryRepository;

    @InjectMocks
    private TelemetryService telemetryService;

    // ==================== TC-SAMPLE-001: 60秒槽位填充 ====================

    @Test
    @DisplayName("TC-SAMPLE-001: 60秒槽位填充 - 100条原始数据生成60个均匀分布数据点")
    void testGetTelemetry_60sRange_100PointsTo60Slots() {
        // Given
        String deviceId = "device_001";
        long now = System.currentTimeMillis() / 1000;
        long startTs = now - 59; // 60秒范围的起始时间

        // 创建100条原始数据，分布在60秒内
        List<Telemetry> rows = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Telemetry t = new Telemetry();
            t.setDeviceId(deviceId);
            t.setTs(startTs + (i * 60L / 100)); // 均匀分布在60秒内
            t.setPowerW(100.0 + i);
            t.setVoltageV(220.0);
            t.setCurrentA(0.5);
            rows.add(t);
        }

        when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                .thenReturn(rows);
        when(telemetryRepository.findFirstByDeviceIdAndTsLessThanOrderByTsDesc(eq(deviceId), anyLong()))
                .thenReturn(null);

        // When
        List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, "60s");

        // Then
        assertEquals(60, result.size(), "应返回60个数据点");

        // 验证时间戳是均匀分布的
        for (int i = 0; i < result.size() - 1; i++) {
            long ts1 = (Long) result.get(i).get("ts");
            long ts2 = (Long) result.get(i + 1).get("ts");
            assertEquals(1, ts2 - ts1, "相邻槽位时间差应为1秒");
        }

        // 验证每个数据点都有功率值
        for (Map<String, Object> point : result) {
            assertTrue(point.containsKey("power_w"), "每个数据点应包含功率值");
            assertTrue((Double) point.get("power_w") >= 0, "功率值应非负");
        }
    }

    @Test
    @DisplayName("TC-SAMPLE-001: 60秒槽位填充 - 验证槽位时间戳")
    void testGetTelemetry_60sRange_SlotTimestamps() {
        // Given
        String deviceId = "device_001";
        long now = System.currentTimeMillis() / 1000;

        List<Telemetry> rows = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            Telemetry t = new Telemetry();
            t.setDeviceId(deviceId);
            t.setTs(now - 59 + i);
            t.setPowerW(100.0 + i);
            rows.add(t);
        }

        when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                .thenReturn(rows);
        when(telemetryRepository.findFirstByDeviceIdAndTsLessThanOrderByTsDesc(eq(deviceId), anyLong()))
                .thenReturn(null);

        // When
        List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, "60s");

        // Then
        assertEquals(60, result.size());
        // 验证时间戳从 startTs 开始
        long firstTs = (Long) result.get(0).get("ts");
        long lastTs = (Long) result.get(59).get("ts");
        assertEquals(59, lastTs - firstTs, "时间范围应为59秒（60个点）");
    }

    // ==================== TC-SAMPLE-002: 24小时采样 ====================

    @Test
    @DisplayName("TC-SAMPLE-002: 24小时采样 - 10000条原始数据生成96个采样数据点")
    void testGetTelemetry_24hRange_10000PointsTo96Slots() {
        // Given
        String deviceId = "device_001";
        long now = System.currentTimeMillis() / 1000;
        long startTs = now - 86400 + 1; // 24小时范围

        // 创建10000条原始数据，分布在24小时内
        List<Telemetry> rows = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            Telemetry t = new Telemetry();
            t.setDeviceId(deviceId);
            t.setTs(startTs + (i * 86400L / 10000));
            t.setPowerW(100.0 + (i % 100));
            t.setVoltageV(220.0);
            t.setCurrentA(0.5);
            rows.add(t);
        }

        when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                .thenReturn(rows);

        // When
        List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, "24h");

        // Then
        assertEquals(96, result.size(), "应返回96个数据点");

        // 验证每个数据点都有功率值
        for (Map<String, Object> point : result) {
            assertTrue(point.containsKey("power_w"));
        }
    }

    @Test
    @DisplayName("TC-SAMPLE-002: 24小时采样 - 采样间隔验证")
    void testGetTelemetry_24hRange_SamplingInterval() {
        // Given
        String deviceId = "device_001";

        // 创建足够多的数据点
        List<Telemetry> rows = new ArrayList<>();
        long baseTs = System.currentTimeMillis() / 1000 - 86400;
        for (int i = 0; i < 1000; i++) {
            Telemetry t = new Telemetry();
            t.setDeviceId(deviceId);
            t.setTs(baseTs + i * 90); // 每90秒一个数据点
            t.setPowerW(100.0);
            rows.add(t);
        }

        when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                .thenReturn(rows);

        // When
        List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, "24h");

        // Then
        assertEquals(96, result.size());
    }

    // ==================== TC-SAMPLE-003: 数据点不足 ====================

    @Test
    @DisplayName("TC-SAMPLE-003: 数据点不足 - 50条原始数据目标96点，返回50个数据点")
    void testGetTelemetry_24hRange_LessThanTarget_ReturnsAll() {
        // Given
        String deviceId = "device_001";
        long now = System.currentTimeMillis() / 1000;
        long startTs = now - 86400 + 1;

        // 只创建50条数据，少于目标96点
        List<Telemetry> rows = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Telemetry t = new Telemetry();
            t.setDeviceId(deviceId);
            t.setTs(startTs + i * 1800); // 每30分钟一个点
            t.setPowerW(100.0 + i);
            rows.add(t);
        }

        when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                .thenReturn(rows);

        // When
        List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, "24h");

        // Then
        assertEquals(50, result.size(), "数据点不足时返回实际数据点数");

        // 验证返回的是原始数据
        for (int i = 0; i < result.size(); i++) {
            assertEquals(rows.get(i).getTs(), result.get(i).get("ts"));
        }
    }

    @Test
    @DisplayName("TC-SAMPLE-003: 数据点不足 - 7天范围只有100条数据")
    void testGetTelemetry_7dRange_LessThanTarget() {
        // Given
        String deviceId = "device_001";
        long now = System.currentTimeMillis() / 1000;
        long startTs = now - 604800 + 1; // 7天范围

        // 只创建100条数据，少于目标168点
        List<Telemetry> rows = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Telemetry t = new Telemetry();
            t.setDeviceId(deviceId);
            t.setTs(startTs + i * 6048);
            t.setPowerW(100.0 + i);
            rows.add(t);
        }

        when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                .thenReturn(rows);

        // When
        List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, "7d");

        // Then
        assertEquals(100, result.size(), "数据点不足时返回实际数据点数");
    }

    // ==================== TC-SAMPLE-004: 空数据槽位 ====================

    @Test
    @DisplayName("TC-SAMPLE-004: 空数据槽位 - 部分槽位无数据，使用前一数据填充")
    void testGetTelemetry_60sRange_PartialSlots_FilledCorrectly() {
        // Given
        String deviceId = "device_001";

        // 使用 anyLong() 来匹配任意时间范围查询
        // 创建数据，只在偶数秒有数据
        List<Telemetry> rows = new ArrayList<>();
        long baseTs = System.currentTimeMillis() / 1000 - 30;
        for (int i = 0; i < 30; i++) {
            Telemetry t = new Telemetry();
            t.setDeviceId(deviceId);
            t.setTs(baseTs + i * 2); // 每2秒一个数据
            t.setPowerW(100.0 + i);
            rows.add(t);
        }

        when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                .thenReturn(rows);
        // 提供历史数据以填充起始槽位
        Telemetry historyData = new Telemetry();
        historyData.setPowerW(50.0);
        when(telemetryRepository.findFirstByDeviceIdAndTsLessThanOrderByTsDesc(eq(deviceId), anyLong()))
                .thenReturn(historyData);

        // When
        List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, "60s");

        // Then
        assertEquals(60, result.size());

        // 验证每个槽位都有数据
        for (int i = 0; i < 60; i++) {
            double power = (Double) result.get(i).get("power_w");
            assertTrue(power >= 50.0, "功率值应大于等于50，实际值: " + power);
        }
    }

    @Test
    @DisplayName("TC-SAMPLE-004: 空数据槽位 - 间隙数据填充")
    void testGetTelemetry_60sRange_GapInData_FilledCorrectly() {
        // Given
        String deviceId = "device_001";

        // 创建数据，有两段数据，中间有间隙
        List<Telemetry> rows = new ArrayList<>();
        long baseTs = System.currentTimeMillis() / 1000 - 50;

        // 第一段：0-19秒的数据
        for (int i = 0; i < 20; i++) {
            Telemetry t = new Telemetry();
            t.setDeviceId(deviceId);
            t.setTs(baseTs + i);
            t.setPowerW(100.0 + i);
            rows.add(t);
        }
        // 中间20秒没有数据（间隙）
        // 第二段：40-59秒的数据
        for (int i = 40; i < 60; i++) {
            Telemetry t = new Telemetry();
            t.setDeviceId(deviceId);
            t.setTs(baseTs + i);
            t.setPowerW(200.0 + i);
            rows.add(t);
        }

        when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                .thenReturn(rows);
        when(telemetryRepository.findFirstByDeviceIdAndTsLessThanOrderByTsDesc(eq(deviceId), anyLong()))
                .thenReturn(null);

        // When
        List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, "60s");

        // Then
        assertEquals(60, result.size());

        // 验证数据被正确填充（每个槽位都有功率值）
        for (int i = 0; i < 60; i++) {
            double power = (Double) result.get(i).get("power_w");
            assertTrue(power >= 0, "每个槽位应有功率值");
        }

        // 验证间隙后的数据正确（第40个槽位开始应该是200+）
        double powerAt40 = (Double) result.get(59).get("power_w");
        assertTrue(powerAt40 >= 200, "间隙后的数据应该正确");
    }

    @Test
    @DisplayName("TC-SAMPLE-004: 空数据槽位 - 使用历史数据填充起始槽位")
    void testGetTelemetry_60sRange_WithHistoryData_StartSlotFilled() {
        // Given
        String deviceId = "device_001";
        long now = System.currentTimeMillis() / 1000;
        long startTs = now - 59;

        // 数据从第10秒开始
        List<Telemetry> rows = new ArrayList<>();
        for (int i = 10; i < 60; i++) {
            Telemetry t = new Telemetry();
            t.setDeviceId(deviceId);
            t.setTs(startTs + i);
            t.setPowerW(100.0 + i);
            rows.add(t);
        }

        // 历史数据（startTs之前的最后一个数据）
        Telemetry historyData = new Telemetry();
        historyData.setDeviceId(deviceId);
        historyData.setTs(startTs - 10);
        historyData.setPowerW(88.8);

        when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                .thenReturn(rows);
        when(telemetryRepository.findFirstByDeviceIdAndTsLessThanOrderByTsDesc(eq(deviceId), anyLong()))
                .thenReturn(historyData);

        // When
        List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, "60s");

        // Then
        assertEquals(60, result.size());

        // 验证前10秒使用历史数据填充
        for (int i = 0; i < 10; i++) {
            double power = (Double) result.get(i).get("power_w");
            assertEquals(88.8, power, 0.001, "起始槽位应使用历史数据填充");
        }
    }

    @Test
    @DisplayName("TC-SAMPLE-004: 空数据槽位 - 无数据时返回模拟数据")
    void testGetTelemetry_60sRange_NoData_ReturnsMockData() {
        // Given
        String deviceId = "device_001";

        // 没有时间范围内的数据
        when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                .thenReturn(new ArrayList<>());

        // When
        List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, "60s");

        // Then - 代码在无数据时返回模拟数据（60个点，功率约100W）
        assertEquals(60, result.size());

        // 验证模拟数据的基本特征
        for (Map<String, Object> point : result) {
            assertTrue(point.containsKey("ts"));
            assertTrue(point.containsKey("power_w"));
            double power = (Double) point.get("power_w");
            // 模拟数据的功率在 90-110 范围内（100 ± 10）
            assertTrue(power >= 90 && power <= 110, "模拟数据功率应在合理范围内");
        }
    }

    // ==================== 其他边界测试 ====================

    @Test
    @DisplayName("边界测试: 无效range参数")
    void testGetTelemetry_InvalidRange_ThrowsException() {
        // Given
        String deviceId = "device_001";
        String invalidRange = "invalid";

        // When & Then
        assertThrows(com.dormpower.exception.BusinessException.class,
                () -> telemetryService.getTelemetry(deviceId, invalidRange));
    }

    @Test
    @DisplayName("边界测试: 30天采样")
    void testGetTelemetry_30dRange() {
        // Given
        String deviceId = "device_001";

        List<Telemetry> rows = new ArrayList<>();
        long baseTs = System.currentTimeMillis() / 1000 - 2592000;
        for (int i = 0; i < 500; i++) {
            Telemetry t = new Telemetry();
            t.setDeviceId(deviceId);
            t.setTs(baseTs + i * 5200);
            t.setPowerW(100.0);
            rows.add(t);
        }

        when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                .thenReturn(rows);

        // When
        List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, "30d");

        // Then
        assertEquals(120, result.size(), "30天范围应返回120个数据点");
    }

    @Test
    @DisplayName("边界测试: 功率值精度验证")
    void testGetTelemetry_PowerPrecision() {
        // Given
        String deviceId = "device_001";
        long now = System.currentTimeMillis() / 1000;
        long startTs = now - 59;

        // 创建带小数精度的功率数据
        List<Telemetry> rows = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            Telemetry t = new Telemetry();
            t.setDeviceId(deviceId);
            t.setTs(startTs + i);
            t.setPowerW(100.123456789 + i * 0.001);
            rows.add(t);
        }

        when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                .thenReturn(rows);
        when(telemetryRepository.findFirstByDeviceIdAndTsLessThanOrderByTsDesc(eq(deviceId), anyLong()))
                .thenReturn(null);

        // When
        List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, "60s");

        // Then
        assertEquals(60, result.size());

        // 验证功率值被正确舍入到3位小数
        for (Map<String, Object> point : result) {
            double power = (Double) point.get("power_w");
            // 验证最多3位小数
            double rounded = Math.round(power * 1000.0) / 1000.0;
            assertEquals(rounded, power, 0.0001);
        }
    }

}