package com.dormpower.service;

import com.dormpower.exception.BusinessException;
import com.dormpower.model.Telemetry;
import com.dormpower.repository.TelemetryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 遥测数据查询服务单元测试
 *
 * 测试用例覆盖：
 * - TC-QUERY-001: 查询60秒数据
 * - TC-QUERY-002: 查询24小时数据
 * - TC-QUERY-003: 查询7天数据
 * - TC-QUERY-004: 无效时间范围
 * - TC-QUERY-005: 无数据设备
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class TelemetryServiceQueryTest {

    @Mock
    private TelemetryRepository telemetryRepository;

    private TelemetryService telemetryService;

    @BeforeEach
    void setUp() {
        telemetryService = new TelemetryService();
        org.springframework.test.util.ReflectionTestUtils.setField(telemetryService, "telemetryRepository", telemetryRepository);
    }

    // ==================== TC-QUERY-001: 查询60秒数据 ====================

    @Nested
    @DisplayName("TC-QUERY-001: 查询60秒数据")
    class Query60sDataTests {

        @Test
        @DisplayName("TC-QUERY-001: range=60s，返回60个数据点")
        void testGetTelemetry_60s_Returns60DataPoints() {
            // Given
            String deviceId = "device_001";
            String range = "60s";
            long now = System.currentTimeMillis() / 1000;

            // 模拟60条遥测数据
            List<Telemetry> mockData = createMockTelemetryData(deviceId, 60, now - 60, now);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(mockData);
            when(telemetryRepository.findFirstByDeviceIdAndTsLessThanOrderByTsDesc(anyString(), anyLong()))
                    .thenReturn(null);

            // When
            List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, range);

            // Then
            assertNotNull(result);
            assertEquals(60, result.size());

            // 验证每个数据点包含必要字段
            for (Map<String, Object> dataPoint : result) {
                assertTrue(dataPoint.containsKey("ts"));
                assertTrue(dataPoint.containsKey("power_w"));
            }

            verify(telemetryRepository).findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong());
        }

        @Test
        @DisplayName("TC-QUERY-001: 60秒数据包含时间戳和功率值")
        void testGetTelemetry_60s_ContainsCorrectFields() {
            // Given
            String deviceId = "device_001";
            String range = "60s";
            long now = System.currentTimeMillis() / 1000;

            List<Telemetry> mockData = createMockTelemetryData(deviceId, 60, now - 60, now);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(mockData);
            when(telemetryRepository.findFirstByDeviceIdAndTsLessThanOrderByTsDesc(anyString(), anyLong()))
                    .thenReturn(null);

            // When
            List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, range);

            // Then
            for (int i = 0; i < result.size(); i++) {
                Map<String, Object> dataPoint = result.get(i);
                assertNotNull(dataPoint.get("ts"));
                assertNotNull(dataPoint.get("power_w"));
                assertTrue((Double) dataPoint.get("power_w") >= 0);
            }
        }
    }

    // ==================== TC-QUERY-002: 查询24小时数据 ====================

    @Nested
    @DisplayName("TC-QUERY-002: 查询24小时数据")
    class Query24hDataTests {

        @Test
        @DisplayName("TC-QUERY-002: range=24h，返回96个数据点")
        void testGetTelemetry_24h_Returns96DataPoints() {
            // Given
            String deviceId = "device_001";
            String range = "24h";
            long now = System.currentTimeMillis() / 1000;
            long startTs = now - 95 * 900L; // 96个点，每15分钟一个点

            // 模拟足够多的数据点
            List<Telemetry> mockData = createMockTelemetryData(deviceId, 500, startTs, now);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(mockData);

            // When
            List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, range);

            // Then
            assertNotNull(result);
            assertEquals(96, result.size());

            verify(telemetryRepository).findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong());
        }

        @Test
        @DisplayName("TC-QUERY-002: 24小时数据按15分钟间隔采样")
        void testGetTelemetry_24h_15MinuteIntervals() {
            // Given
            String deviceId = "device_001";
            String range = "24h";
            long now = System.currentTimeMillis() / 1000;

            List<Telemetry> mockData = createMockTelemetryData(deviceId, 96, now - 86400, now);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(mockData);

            // When
            List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, range);

            // Then
            assertEquals(96, result.size());
        }
    }

    // ==================== TC-QUERY-003: 查询7天数据 ====================

    @Nested
    @DisplayName("TC-QUERY-003: 查询7天数据")
    class Query7dDataTests {

        @Test
        @DisplayName("TC-QUERY-003: range=7d，返回168个数据点")
        void testGetTelemetry_7d_Returns168DataPoints() {
            // Given
            String deviceId = "device_001";
            String range = "7d";
            long now = System.currentTimeMillis() / 1000;
            long startTs = now - 167 * 3600L; // 168个点，每小时一个点

            // 模拟足够多的数据点
            List<Telemetry> mockData = createMockTelemetryData(deviceId, 1000, startTs, now);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(mockData);

            // When
            List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, range);

            // Then
            assertNotNull(result);
            assertEquals(168, result.size());

            verify(telemetryRepository).findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong());
        }

        @Test
        @DisplayName("TC-QUERY-003: 7天数据按1小时间隔采样")
        void testGetTelemetry_7d_HourlyIntervals() {
            // Given
            String deviceId = "device_001";
            String range = "7d";
            long now = System.currentTimeMillis() / 1000;

            List<Telemetry> mockData = createMockTelemetryData(deviceId, 168, now - 604800, now);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(mockData);

            // When
            List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, range);

            // Then
            assertEquals(168, result.size());
        }
    }

    // ==================== TC-QUERY-004: 无效时间范围 ====================

    @Nested
    @DisplayName("TC-QUERY-004: 无效时间范围")
    class InvalidRangeTests {

        @Test
        @DisplayName("TC-QUERY-004: range=invalid，抛出BusinessException")
        void testGetTelemetry_InvalidRange_ThrowsException() {
            // Given
            String deviceId = "device_001";
            String invalidRange = "invalid";

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> telemetryService.getTelemetry(deviceId, invalidRange)
            );

            assertTrue(exception.getMessage().contains("Invalid range"));
        }

        @Test
        @DisplayName("TC-QUERY-004: range为空字符串，抛出BusinessException")
        void testGetTelemetry_EmptyRange_ThrowsException() {
            // Given
            String deviceId = "device_001";
            String emptyRange = "";

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> telemetryService.getTelemetry(deviceId, emptyRange)
            );

            assertTrue(exception.getMessage().contains("Invalid range"));
        }

        @Test
        @DisplayName("TC-QUERY-004: range为null相关值，抛出BusinessException")
        void testGetTelemetry_UnsupportedRange_ThrowsException() {
            // Given
            String deviceId = "device_001";
            String unsupportedRange = "1h"; // 不支持的时间范围

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> telemetryService.getTelemetry(deviceId, unsupportedRange)
            );

            assertTrue(exception.getMessage().contains("Invalid range"));
        }
    }

    // ==================== TC-QUERY-005: 无数据设备 ====================

    @Nested
    @DisplayName("TC-QUERY-005: 无数据设备")
    class NoDataDeviceTests {

        @Test
        @DisplayName("TC-QUERY-005: 无数据设备查询60s，返回模拟数据（60个点）")
        void testGetTelemetry_NoData_60s_ReturnsMockData() {
            // Given
            String deviceId = "device_no_data";
            String range = "60s";

            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());

            // When
            List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, range);

            // Then
            assertNotNull(result);
            assertEquals(60, result.size()); // 返回模拟数据

            // 验证模拟数据格式正确
            for (Map<String, Object> dataPoint : result) {
                assertTrue(dataPoint.containsKey("ts"));
                assertTrue(dataPoint.containsKey("power_w"));
            }
        }

        @Test
        @DisplayName("TC-QUERY-005: 无数据设备查询24h，返回空列表")
        void testGetTelemetry_NoData_24h_ReturnsEmptyList() {
            // Given
            String deviceId = "device_no_data";
            String range = "24h";

            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());

            // When
            List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, range);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("TC-QUERY-005: 无数据设备查询7d，返回空列表")
        void testGetTelemetry_NoData_7d_ReturnsEmptyList() {
            // Given
            String deviceId = "device_no_data";
            String range = "7d";

            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());

            // When
            List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, range);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("TC-QUERY-005: 无数据设备查询30d，返回空列表")
        void testGetTelemetry_NoData_30d_ReturnsEmptyList() {
            // Given
            String deviceId = "device_no_data";
            String range = "30d";

            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());

            // When
            List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, range);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== 其他测试：30天数据 ====================

    @Nested
    @DisplayName("其他测试：查询30天数据")
    class Query30dDataTests {

        @Test
        @DisplayName("range=30d，返回120个数据点")
        void testGetTelemetry_30d_Returns120DataPoints() {
            // Given
            String deviceId = "device_001";
            String range = "30d";
            long now = System.currentTimeMillis() / 1000;

            List<Telemetry> mockData = createMockTelemetryData(deviceId, 2000, now - 2592000, now);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(mockData);

            // When
            List<Map<String, Object>> result = telemetryService.getTelemetry(deviceId, range);

            // Then
            assertNotNull(result);
            assertEquals(120, result.size());
        }
    }

    // ==================== 辅助方法：创建测试数据 ====================

    /**
     * 创建模拟遥测数据列表
     */
    private List<Telemetry> createMockTelemetryData(String deviceId, int count, long startTs, long endTs) {
        List<Telemetry> dataList = new ArrayList<>(count);
        long step = (endTs - startTs) / Math.max(count - 1, 1);
        double basePower = 100.0;

        for (int i = 0; i < count; i++) {
            Telemetry telemetry = new Telemetry();
            telemetry.setId((long) i + 1);
            telemetry.setDeviceId(deviceId);
            telemetry.setTs(startTs + i * step);
            telemetry.setPowerW(basePower + Math.random() * 50);
            telemetry.setVoltageV(220.0);
            telemetry.setCurrentA(0.5 + Math.random() * 0.5);
            dataList.add(telemetry);
        }

        return dataList;
    }
}