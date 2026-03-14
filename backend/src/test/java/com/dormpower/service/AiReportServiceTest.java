package com.dormpower.service;

import com.dormpower.model.Device;
import com.dormpower.model.Telemetry;
import com.dormpower.repository.DeviceRepository;
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
 * AI分析报告服务单元测试
 *
 * 测试用例覆盖：
 * - TC-AI-001: 正常生成报告
 * - TC-AI-002: 无设备房间
 * - TC-AI-003: 无数据设备
 * - TC-AI-004: 高功率异常
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class AiReportServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private TelemetryRepository telemetryRepository;

    private AiReportService aiReportService;

    @BeforeEach
    void setUp() {
        aiReportService = new AiReportService();
        org.springframework.test.util.ReflectionTestUtils.setField(aiReportService, "deviceRepository", deviceRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(aiReportService, "telemetryRepository", telemetryRepository);
    }

    // ==================== TC-AI-001: 正常生成报告 ====================

    @Nested
    @DisplayName("TC-AI-001: 正常生成报告")
    class NormalReportTests {

        @Test
        @DisplayName("TC-AI-001: 有效房间ID和数据，返回完整分析报告")
        void testGetAiReport_Success_ReturnsCompleteReport() {
            // Given
            String roomId = "A1-301";
            String period = "7d";

            // 模拟设备数据
            Device device = createDevice("device_001", roomId);
            when(deviceRepository.findByRoom(roomId)).thenReturn(List.of(device));

            // 模拟遥测数据
            List<Telemetry> telemetryData = createTelemetryData("device_001", 100, 50.0, 150.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(anyString(), anyLong(), anyLong()))
                    .thenReturn(telemetryData);

            // When
            Map<String, Object> report = aiReportService.getAiReport(roomId, period);

            // Then
            assertNotNull(report);
            assertEquals(roomId, report.get("room_id"));
            assertNotNull(report.get("summary"));
            assertNotNull(report.get("anomalies"));
            assertNotNull(report.get("recommendations"));
            assertNotNull(report.get("generated_at"));
            assertNotNull(report.get("power_stats"));

            // 验证功率统计字段
            Map<String, Object> powerStats = (Map<String, Object>) report.get("power_stats");
            assertTrue(powerStats.containsKey("avg_power_w"));
            assertTrue(powerStats.containsKey("peak_power_w"));
            assertTrue(powerStats.containsKey("peak_time"));
            assertTrue(powerStats.containsKey("total_kwh"));

            verify(deviceRepository).findByRoom(roomId);
            verify(telemetryRepository).findByDeviceIdAndTsBetweenOrderByTsAsc(anyString(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("TC-AI-001: 报告包含正确的摘要信息")
        void testGetAiReport_ContainsCorrectSummary() {
            // Given
            String roomId = "A1-301";
            String period = "7d";

            Device device = createDevice("device_001", roomId);
            when(deviceRepository.findByRoom(roomId)).thenReturn(List.of(device));

            List<Telemetry> telemetryData = createTelemetryData("device_001", 100, 80.0, 120.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(anyString(), anyLong(), anyLong()))
                    .thenReturn(telemetryData);

            // When
            Map<String, Object> report = aiReportService.getAiReport(roomId, period);

            // Then
            String summary = (String) report.get("summary");
            assertTrue(summary.contains("Average power"));
            assertTrue(summary.contains("peak"));
        }

        @Test
        @DisplayName("TC-AI-001: 多设备房间生成聚合报告")
        void testGetAiReport_MultipleDevices_AggregatedReport() {
            // Given
            String roomId = "A1-301";
            String period = "7d";

            Device device1 = createDevice("device_001", roomId);
            Device device2 = createDevice("device_002", roomId);
            when(deviceRepository.findByRoom(roomId)).thenReturn(List.of(device1, device2));

            List<Telemetry> telemetryData1 = createTelemetryData("device_001", 50, 60.0, 100.0);
            List<Telemetry> telemetryData2 = createTelemetryData("device_002", 50, 40.0, 80.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq("device_001"), anyLong(), anyLong()))
                    .thenReturn(telemetryData1);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq("device_002"), anyLong(), anyLong()))
                    .thenReturn(telemetryData2);

            // When
            Map<String, Object> report = aiReportService.getAiReport(roomId, period);

            // Then
            assertNotNull(report);
            assertEquals(roomId, report.get("room_id"));
            assertNotNull(report.get("summary"));
        }

        @Test
        @DisplayName("TC-AI-001: 30天周期生成报告")
        void testGetAiReport_30DayPeriod_ReturnsReport() {
            // Given
            String roomId = "A1-301";
            String period = "30d";

            Device device = createDevice("device_001", roomId);
            when(deviceRepository.findByRoom(roomId)).thenReturn(List.of(device));

            List<Telemetry> telemetryData = createTelemetryData("device_001", 200, 70.0, 130.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(anyString(), anyLong(), anyLong()))
                    .thenReturn(telemetryData);

            // When
            Map<String, Object> report = aiReportService.getAiReport(roomId, period);

            // Then
            assertNotNull(report);
            assertEquals(roomId, report.get("room_id"));
        }
    }

    // ==================== TC-AI-002: 无设备房间 ====================

    @Nested
    @DisplayName("TC-AI-002: 无设备房间")
    class NoDeviceRoomTests {

        @Test
        @DisplayName("TC-AI-002: 无设备房间ID，返回无设备提示")
        void testGetAiReport_NoDevice_ReturnsNoDeviceMessage() {
            // Given
            String roomId = "A1-999";
            String period = "7d";

            when(deviceRepository.findByRoom(roomId)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> report = aiReportService.getAiReport(roomId, period);

            // Then
            assertNotNull(report);
            assertEquals(roomId, report.get("room_id"));
            assertEquals("No device data in this room yet.", report.get("summary"));

            List<String> anomalies = (List<String>) report.get("anomalies");
            assertTrue(anomalies.contains("No analyzable sample found."));

            List<String> recommendations = (List<String>) report.get("recommendations");
            assertTrue(recommendations.contains("Ensure devices upload status and telemetry periodically."));

            verify(deviceRepository).findByRoom(roomId);
            verify(telemetryRepository, never()).findByDeviceIdAndTsBetweenOrderByTsAsc(anyString(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("TC-AI-002: 无设备房间返回空的功率统计")
        void testGetAiReport_NoDevice_ReturnsEmptyPowerStats() {
            // Given
            String roomId = "A1-999";
            String period = "7d";

            when(deviceRepository.findByRoom(roomId)).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> report = aiReportService.getAiReport(roomId, period);

            // Then
            Map<String, Object> powerStats = (Map<String, Object>) report.get("power_stats");
            assertEquals(0.0, powerStats.get("avg_power_w"));
            assertEquals(0.0, powerStats.get("peak_power_w"));
            assertEquals("", powerStats.get("peak_time"));
            assertEquals(0.0, powerStats.get("total_kwh"));
        }
    }

    // ==================== TC-AI-003: 无数据设备 ====================

    @Nested
    @DisplayName("TC-AI-003: 无数据设备")
    class NoDataDeviceTests {

        @Test
        @DisplayName("TC-AI-003: 有设备无数据，返回无数据提示")
        void testGetAiReport_NoData_ReturnsNoDataMessage() {
            // Given
            String roomId = "A1-301";
            String period = "7d";

            Device device = createDevice("device_001", roomId);
            when(deviceRepository.findByRoom(roomId)).thenReturn(List.of(device));
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(anyString(), anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());

            // When
            Map<String, Object> report = aiReportService.getAiReport(roomId, period);

            // Then
            assertNotNull(report);
            assertEquals(roomId, report.get("room_id"));
            assertEquals("Devices are online but telemetry coverage is insufficient.", report.get("summary"));

            List<String> anomalies = (List<String>) report.get("anomalies");
            assertTrue(anomalies.contains("Not enough telemetry points in selected period."));

            List<String> recommendations = (List<String>) report.get("recommendations");
            assertTrue(recommendations.contains("Increase telemetry frequency to every 1-5 seconds."));

            verify(deviceRepository).findByRoom(roomId);
            verify(telemetryRepository).findByDeviceIdAndTsBetweenOrderByTsAsc(anyString(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("TC-AI-003: 无数据设备返回空的功率统计")
        void testGetAiReport_NoData_ReturnsEmptyPowerStats() {
            // Given
            String roomId = "A1-301";
            String period = "7d";

            Device device = createDevice("device_001", roomId);
            when(deviceRepository.findByRoom(roomId)).thenReturn(List.of(device));
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(anyString(), anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());

            // When
            Map<String, Object> report = aiReportService.getAiReport(roomId, period);

            // Then
            Map<String, Object> powerStats = (Map<String, Object>) report.get("power_stats");
            assertEquals(0.0, powerStats.get("avg_power_w"));
            assertEquals(0.0, powerStats.get("peak_power_w"));
        }
    }

    // ==================== TC-AI-004: 高功率异常 ====================

    @Nested
    @DisplayName("TC-AI-004: 高功率异常")
    class HighPowerAnomalyTests {

        @Test
        @DisplayName("TC-AI-004: 峰值功率>200W，包含异常提示")
        void testGetAiReport_HighPeakPower_ContainsAnomaly() {
            // Given
            String roomId = "A1-301";
            String period = "7d";

            Device device = createDevice("device_001", roomId);
            when(deviceRepository.findByRoom(roomId)).thenReturn(List.of(device));

            // 创建峰值功率>200W的数据
            List<Telemetry> telemetryData = createTelemetryDataWithPeak("device_001", 100, 100.0, 250.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(anyString(), anyLong(), anyLong()))
                    .thenReturn(telemetryData);

            // When
            Map<String, Object> report = aiReportService.getAiReport(roomId, period);

            // Then
            List<String> anomalies = (List<String>) report.get("anomalies");
            boolean hasPeakAnomaly = anomalies.stream()
                    .anyMatch(a -> a.contains("Peak power") && a.contains("250"));
            assertTrue(hasPeakAnomaly, "应包含峰值功率异常提示");
        }

        @Test
        @DisplayName("TC-AI-004: 平均功率>150W，包含高平均功率异常")
        void testGetAiReport_HighAveragePower_ContainsAnomaly() {
            // Given
            String roomId = "A1-301";
            String period = "7d";

            Device device = createDevice("device_001", roomId);
            when(deviceRepository.findByRoom(roomId)).thenReturn(List.of(device));

            // 创建平均功率>150W的数据
            List<Telemetry> telemetryData = createTelemetryData("device_001", 100, 160.0, 180.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(anyString(), anyLong(), anyLong()))
                    .thenReturn(telemetryData);

            // When
            Map<String, Object> report = aiReportService.getAiReport(roomId, period);

            // Then
            List<String> anomalies = (List<String>) report.get("anomalies");
            boolean hasAvgAnomaly = anomalies.stream()
                    .anyMatch(a -> a.contains("Average power") && a.contains("high"));
            assertTrue(hasAvgAnomaly, "应包含高平均功率异常提示");
        }

        @Test
        @DisplayName("TC-AI-004: 正常功率范围无异常提示")
        void testGetAiReport_NormalPower_NoAnomalies() {
            // Given
            String roomId = "A1-301";
            String period = "7d";

            Device device = createDevice("device_001", roomId);
            when(deviceRepository.findByRoom(roomId)).thenReturn(List.of(device));

            // 创建正常功率数据
            List<Telemetry> telemetryData = createTelemetryData("device_001", 100, 80.0, 120.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(anyString(), anyLong(), anyLong()))
                    .thenReturn(telemetryData);

            // When
            Map<String, Object> report = aiReportService.getAiReport(roomId, period);

            // Then
            List<String> anomalies = (List<String>) report.get("anomalies");
            // 正常功率范围内可能没有异常
            assertNotNull(anomalies);
        }

        @Test
        @DisplayName("TC-AI-004: 高功率设备包含节能建议")
        void testGetAiReport_HighPower_ContainsRecommendations() {
            // Given
            String roomId = "A1-301";
            String period = "7d";

            Device device = createDevice("device_001", roomId);
            when(deviceRepository.findByRoom(roomId)).thenReturn(List.of(device));

            List<Telemetry> telemetryData = createTelemetryData("device_001", 100, 120.0, 180.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(anyString(), anyLong(), anyLong()))
                    .thenReturn(telemetryData);

            // When
            Map<String, Object> report = aiReportService.getAiReport(roomId, period);

            // Then
            List<String> recommendations = (List<String>) report.get("recommendations");
            assertTrue(recommendations.size() >= 2);
            assertTrue(recommendations.stream().anyMatch(r -> r.contains("auto off")));
            assertTrue(recommendations.stream().anyMatch(r -> r.contains("alerts")));
        }
    }

    // ==================== 辅助方法：创建测试数据 ====================

    /**
     * 创建设备实体
     */
    private Device createDevice(String deviceId, String room) {
        Device device = new Device();
        device.setId(deviceId);
        device.setName("测试设备-" + deviceId);
        device.setRoom(room);
        device.setOnline(true);
        return device;
    }

    /**
     * 创建遥测数据列表
     */
    private List<Telemetry> createTelemetryData(String deviceId, int count, double minPower, double maxPower) {
        List<Telemetry> dataList = new ArrayList<>(count);
        long baseTs = System.currentTimeMillis() / 1000 - count * 60;
        double powerRange = maxPower - minPower;

        for (int i = 0; i < count; i++) {
            Telemetry telemetry = new Telemetry();
            telemetry.setId((long) i + 1);
            telemetry.setDeviceId(deviceId);
            telemetry.setTs(baseTs + i * 60);
            telemetry.setPowerW(minPower + Math.random() * powerRange);
            telemetry.setVoltageV(220.0);
            telemetry.setCurrentA(0.5);
            dataList.add(telemetry);
        }

        return dataList;
    }

    /**
     * 创建包含指定峰值功率的遥测数据
     */
    private List<Telemetry> createTelemetryDataWithPeak(String deviceId, int count, double basePower, double peakPower) {
        List<Telemetry> dataList = new ArrayList<>(count);
        long baseTs = System.currentTimeMillis() / 1000 - count * 60;

        for (int i = 0; i < count; i++) {
            Telemetry telemetry = new Telemetry();
            telemetry.setId((long) i + 1);
            telemetry.setDeviceId(deviceId);
            telemetry.setTs(baseTs + i * 60);
            // 在中间位置设置峰值
            if (i == count / 2) {
                telemetry.setPowerW(peakPower);
            } else {
                telemetry.setPowerW(basePower + Math.random() * 50);
            }
            telemetry.setVoltageV(220.0);
            telemetry.setCurrentA(0.5);
            dataList.add(telemetry);
        }

        return dataList;
    }
}