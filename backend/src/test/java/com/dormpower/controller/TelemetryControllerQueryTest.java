package com.dormpower.controller;

import com.dormpower.config.TestSecurityConfig;
import com.dormpower.repository.TelemetryRepository;
import com.dormpower.service.TelemetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 遥测数据查询控制器单元测试
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
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class TelemetryControllerQueryTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TelemetryService telemetryService;

    @MockBean
    private TelemetryRepository telemetryRepository;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    // ==================== TC-QUERY-001: 查询60秒数据 ====================

    @Nested
    @DisplayName("TC-QUERY-001: 查询60秒数据")
    class Query60sDataTests {

        @Test
        @DisplayName("TC-QUERY-001: range=60s，返回60个数据点")
        void testGetTelemetry_60s_Returns60DataPoints() throws Exception {
            // Given
            String deviceId = "device_001";
            String range = "60s";
            List<Map<String, Object>> mockResult = createMockTelemetryResult(60);
            when(telemetryService.getTelemetry(deviceId, range)).thenReturn(mockResult);

            // When & Then
            mockMvc.perform(get("/api/telemetry")
                            .param("device", deviceId)
                            .param("range", range))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(60));
        }

        @Test
        @DisplayName("TC-QUERY-001: 60秒数据包含正确的字段格式")
        void testGetTelemetry_60s_CorrectFieldFormat() throws Exception {
            // Given
            String deviceId = "device_001";
            String range = "60s";
            List<Map<String, Object>> mockResult = createMockTelemetryResult(60);
            when(telemetryService.getTelemetry(deviceId, range)).thenReturn(mockResult);

            // When & Then
            mockMvc.perform(get("/api/telemetry")
                            .param("device", deviceId)
                            .param("range", range))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].ts").exists())
                    .andExpect(jsonPath("$[0].power_w").exists());
        }
    }

    // ==================== TC-QUERY-002: 查询24小时数据 ====================

    @Nested
    @DisplayName("TC-QUERY-002: 查询24小时数据")
    class Query24hDataTests {

        @Test
        @DisplayName("TC-QUERY-002: range=24h，返回96个数据点")
        void testGetTelemetry_24h_Returns96DataPoints() throws Exception {
            // Given
            String deviceId = "device_001";
            String range = "24h";
            List<Map<String, Object>> mockResult = createMockTelemetryResult(96);
            when(telemetryService.getTelemetry(deviceId, range)).thenReturn(mockResult);

            // When & Then
            mockMvc.perform(get("/api/telemetry")
                            .param("device", deviceId)
                            .param("range", range))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(96));
        }
    }

    // ==================== TC-QUERY-003: 查询7天数据 ====================

    @Nested
    @DisplayName("TC-QUERY-003: 查询7天数据")
    class Query7dDataTests {

        @Test
        @DisplayName("TC-QUERY-003: range=7d，返回168个数据点")
        void testGetTelemetry_7d_Returns168DataPoints() throws Exception {
            // Given
            String deviceId = "device_001";
            String range = "7d";
            List<Map<String, Object>> mockResult = createMockTelemetryResult(168);
            when(telemetryService.getTelemetry(deviceId, range)).thenReturn(mockResult);

            // When & Then
            mockMvc.perform(get("/api/telemetry")
                            .param("device", deviceId)
                            .param("range", range))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(168));
        }
    }

    // ==================== TC-QUERY-004: 无效时间范围 ====================

    @Nested
    @DisplayName("TC-QUERY-004: 无效时间范围")
    class InvalidRangeTests {

        @Test
        @DisplayName("TC-QUERY-004: range=invalid，返回400错误")
        void testGetTelemetry_InvalidRange_Returns400() throws Exception {
            // Given
            String deviceId = "device_001";
            String invalidRange = "invalid";

            // When & Then
            mockMvc.perform(get("/api/telemetry")
                            .param("device", deviceId)
                            .param("range", invalidRange))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-QUERY-004: range=1h（不支持的范围），返回400错误")
        void testGetTelemetry_UnsupportedRange_Returns400() throws Exception {
            // Given
            String deviceId = "device_001";
            String unsupportedRange = "1h";

            // When & Then
            mockMvc.perform(get("/api/telemetry")
                            .param("device", deviceId)
                            .param("range", unsupportedRange))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-QUERY-004: 缺少range参数，返回500错误")
        void testGetTelemetry_MissingRange_Returns500() throws Exception {
            // Given
            String deviceId = "device_001";

            // When & Then - 缺少必填参数时返回500
            mockMvc.perform(get("/api/telemetry")
                            .param("device", deviceId))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("TC-QUERY-004: 缺少device参数，返回500错误")
        void testGetTelemetry_MissingDevice_Returns500() throws Exception {
            // Given
            String range = "60s";

            // When & Then - 缺少必填参数时返回500
            mockMvc.perform(get("/api/telemetry")
                            .param("range", range))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ==================== TC-QUERY-005: 无数据设备 ====================

    @Nested
    @DisplayName("TC-QUERY-005: 无数据设备")
    class NoDataDeviceTests {

        @Test
        @DisplayName("TC-QUERY-005: 无数据设备查询60s，返回模拟数据（60个点）")
        void testGetTelemetry_NoData_60s_ReturnsMockData() throws Exception {
            // Given
            String deviceId = "device_no_data";
            String range = "60s";
            List<Map<String, Object>> mockResult = createMockTelemetryResult(60);
            when(telemetryService.getTelemetry(deviceId, range)).thenReturn(mockResult);

            // When & Then
            mockMvc.perform(get("/api/telemetry")
                            .param("device", deviceId)
                            .param("range", range))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(60));
        }

        @Test
        @DisplayName("TC-QUERY-005: 无数据设备查询24h，返回空列表")
        void testGetTelemetry_NoData_24h_ReturnsEmptyList() throws Exception {
            // Given
            String deviceId = "device_no_data";
            String range = "24h";
            when(telemetryService.getTelemetry(deviceId, range)).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/telemetry")
                            .param("device", deviceId)
                            .param("range", range))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("TC-QUERY-005: 无数据设备查询7d，返回空列表")
        void testGetTelemetry_NoData_7d_ReturnsEmptyList() throws Exception {
            // Given
            String deviceId = "device_no_data";
            String range = "7d";
            when(telemetryService.getTelemetry(deviceId, range)).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/telemetry")
                            .param("device", deviceId)
                            .param("range", range))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ==================== 其他接口测试 ====================

    @Nested
    @DisplayName("其他接口测试")
    class OtherEndpointTests {

        @Test
        @DisplayName("查询30天数据，返回120个数据点")
        void testGetTelemetry_30d_Returns120DataPoints() throws Exception {
            // Given
            String deviceId = "device_001";
            String range = "30d";
            List<Map<String, Object>> mockResult = createMockTelemetryResult(120);
            when(telemetryService.getTelemetry(deviceId, range)).thenReturn(mockResult);

            // When & Then
            mockMvc.perform(get("/api/telemetry")
                            .param("device", deviceId)
                            .param("range", range))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(120));
        }

        @Test
        @DisplayName("获取用电统计报表-成功")
        void testGetElectricityStatistics_Success() throws Exception {
            // Given
            String deviceId = "device_001";
            String period = "day";
            long start = 1700000000L;
            long end = 1700086400L;

            Map<String, Object> mockStats = new HashMap<>();
            mockStats.put("deviceId", deviceId);
            mockStats.put("period", period);
            mockStats.put("totalEnergyWh", 500.0);
            mockStats.put("averagePowerW", 100.0);
            mockStats.put("maxPowerW", 150.0);
            mockStats.put("minPowerW", 50.0);

            when(telemetryService.getElectricityStatistics(deviceId, period, start, end)).thenReturn(mockStats);

            // When & Then
            mockMvc.perform(get("/api/telemetry/statistics")
                            .param("device", deviceId)
                            .param("period", period)
                            .param("start", String.valueOf(start))
                            .param("end", String.valueOf(end)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deviceId").value(deviceId))
                    .andExpect(jsonPath("$.period").value(period))
                    .andExpect(jsonPath("$.totalEnergyWh").value(500.0));
        }

        @Test
        @DisplayName("获取用电统计报表-无效period参数")
        void testGetElectricityStatistics_InvalidPeriod_Returns400() throws Exception {
            // Given
            String deviceId = "device_001";
            String invalidPeriod = "invalid";
            long start = 1700000000L;
            long end = 1700086400L;

            // When & Then
            mockMvc.perform(get("/api/telemetry/statistics")
                            .param("device", deviceId)
                            .param("period", invalidPeriod)
                            .param("start", String.valueOf(start))
                            .param("end", String.valueOf(end)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("导出遥测数据-CSV格式")
        void testExportTelemetry_CsvFormat() throws Exception {
            // Given
            String deviceId = "device_001";
            String format = "csv";
            long start = 1700000000L;
            long end = 1700086400L;

            String csvContent = "timestamp,device_id,power_w,voltage_v,current_a\n" +
                    "1700000000,device_001,100.0,220.0,0.5\n";
            when(telemetryService.exportTelemetry(deviceId, format, start, end)).thenReturn(csvContent.getBytes());

            // When & Then
            mockMvc.perform(get("/api/telemetry/export")
                            .param("device", deviceId)
                            .param("format", format)
                            .param("start", String.valueOf(start))
                            .param("end", String.valueOf(end)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("导出遥测数据-无效格式")
        void testExportTelemetry_InvalidFormat_Returns400() throws Exception {
            // Given
            String deviceId = "device_001";
            String invalidFormat = "json";
            long start = 1700000000L;
            long end = 1700086400L;

            // When & Then
            mockMvc.perform(get("/api/telemetry/export")
                            .param("device", deviceId)
                            .param("format", invalidFormat)
                            .param("start", String.valueOf(start))
                            .param("end", String.valueOf(end)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== 辅助方法：创建测试数据 ====================

    /**
     * 创建模拟遥测数据结果
     */
    private List<Map<String, Object>> createMockTelemetryResult(int count) {
        List<Map<String, Object>> result = new ArrayList<>(count);
        long baseTs = System.currentTimeMillis() / 1000 - count;
        double basePower = 100.0;

        for (int i = 0; i < count; i++) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("ts", baseTs + i);
            dataPoint.put("power_w", basePower + Math.random() * 20 - 10);
            result.add(dataPoint);
        }

        return result;
    }
}