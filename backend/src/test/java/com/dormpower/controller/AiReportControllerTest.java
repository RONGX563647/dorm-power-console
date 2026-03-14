package com.dormpower.controller;

import com.dormpower.config.TestSecurityConfig;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.TelemetryRepository;
import com.dormpower.service.AiReportService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI分析报告控制器单元测试
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
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AiReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiReportService aiReportService;

    @MockBean
    private DeviceRepository deviceRepository;

    @MockBean
    private TelemetryRepository telemetryRepository;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    // ==================== TC-AI-001: 正常生成报告 ====================

    @Nested
    @DisplayName("TC-AI-001: 正常生成报告")
    class NormalReportTests {

        @Test
        @DisplayName("TC-AI-001: 有效房间ID和数据，返回完整分析报告")
        void testGetAiReport_Success_ReturnsCompleteReport() throws Exception {
            // Given
            String roomId = "A1-301";
            String period = "7d";

            Map<String, Object> mockReport = createCompleteReport(roomId);
            when(aiReportService.getAiReport(roomId, period)).thenReturn(mockReport);

            // When & Then
            mockMvc.perform(get("/api/rooms/{roomId}/ai_report", roomId)
                            .param("period", period))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.room_id").value(roomId))
                    .andExpect(jsonPath("$.summary").exists())
                    .andExpect(jsonPath("$.anomalies").isArray())
                    .andExpect(jsonPath("$.recommendations").isArray())
                    .andExpect(jsonPath("$.generated_at").exists())
                    .andExpect(jsonPath("$.power_stats").exists());
        }

        @Test
        @DisplayName("TC-AI-001: 默认使用7天周期")
        void testGetAiReport_DefaultPeriod_ReturnsReport() throws Exception {
            // Given
            String roomId = "A1-301";

            Map<String, Object> mockReport = createCompleteReport(roomId);
            when(aiReportService.getAiReport(roomId, "7d")).thenReturn(mockReport);

            // When & Then - 不传period参数，使用默认值7d
            mockMvc.perform(get("/api/rooms/{roomId}/ai_report", roomId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.room_id").value(roomId));
        }

        @Test
        @DisplayName("TC-AI-001: 30天周期生成报告")
        void testGetAiReport_30DayPeriod_ReturnsReport() throws Exception {
            // Given
            String roomId = "A1-301";
            String period = "30d";

            Map<String, Object> mockReport = createCompleteReport(roomId);
            when(aiReportService.getAiReport(roomId, period)).thenReturn(mockReport);

            // When & Then
            mockMvc.perform(get("/api/rooms/{roomId}/ai_report", roomId)
                            .param("period", period))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.room_id").value(roomId));
        }

        @Test
        @DisplayName("TC-AI-001: 报告包含功率统计数据")
        void testGetAiReport_ContainsPowerStats() throws Exception {
            // Given
            String roomId = "A1-301";
            String period = "7d";

            Map<String, Object> mockReport = createCompleteReport(roomId);
            when(aiReportService.getAiReport(roomId, period)).thenReturn(mockReport);

            // When & Then
            mockMvc.perform(get("/api/rooms/{roomId}/ai_report", roomId)
                            .param("period", period))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.power_stats.avg_power_w").value(100.0))
                    .andExpect(jsonPath("$.power_stats.peak_power_w").value(150.0))
                    .andExpect(jsonPath("$.power_stats.total_kwh").exists());
        }
    }

    // ==================== TC-AI-002: 无设备房间 ====================

    @Nested
    @DisplayName("TC-AI-002: 无设备房间")
    class NoDeviceRoomTests {

        @Test
        @DisplayName("TC-AI-002: 无设备房间ID，返回无设备提示")
        void testGetAiReport_NoDevice_ReturnsNoDeviceMessage() throws Exception {
            // Given
            String roomId = "A1-999";
            String period = "7d";

            Map<String, Object> mockReport = createNoDeviceReport(roomId);
            when(aiReportService.getAiReport(roomId, period)).thenReturn(mockReport);

            // When & Then
            mockMvc.perform(get("/api/rooms/{roomId}/ai_report", roomId)
                            .param("period", period))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.room_id").value(roomId))
                    .andExpect(jsonPath("$.summary").value("No device data in this room yet."))
                    .andExpect(jsonPath("$.anomalies[0]").value("No analyzable sample found."))
                    .andExpect(jsonPath("$.recommendations[0]").value("Ensure devices upload status and telemetry periodically."));
        }

        @Test
        @DisplayName("TC-AI-002: 无设备房间返回空的功率统计")
        void testGetAiReport_NoDevice_ReturnsEmptyPowerStats() throws Exception {
            // Given
            String roomId = "A1-999";
            String period = "7d";

            Map<String, Object> mockReport = createNoDeviceReport(roomId);
            when(aiReportService.getAiReport(roomId, period)).thenReturn(mockReport);

            // When & Then
            mockMvc.perform(get("/api/rooms/{roomId}/ai_report", roomId)
                            .param("period", period))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.power_stats.avg_power_w").value(0.0))
                    .andExpect(jsonPath("$.power_stats.peak_power_w").value(0.0));
        }
    }

    // ==================== TC-AI-003: 无数据设备 ====================

    @Nested
    @DisplayName("TC-AI-003: 无数据设备")
    class NoDataDeviceTests {

        @Test
        @DisplayName("TC-AI-003: 有设备无数据，返回无数据提示")
        void testGetAiReport_NoData_ReturnsNoDataMessage() throws Exception {
            // Given
            String roomId = "A1-301";
            String period = "7d";

            Map<String, Object> mockReport = createNoDataReport(roomId);
            when(aiReportService.getAiReport(roomId, period)).thenReturn(mockReport);

            // When & Then
            mockMvc.perform(get("/api/rooms/{roomId}/ai_report", roomId)
                            .param("period", period))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.room_id").value(roomId))
                    .andExpect(jsonPath("$.summary").value("Devices are online but telemetry coverage is insufficient."))
                    .andExpect(jsonPath("$.anomalies[0]").value("Not enough telemetry points in selected period."))
                    .andExpect(jsonPath("$.recommendations[0]").value("Increase telemetry frequency to every 1-5 seconds."));
        }

        @Test
        @DisplayName("TC-AI-003: 无数据设备返回空的功率统计")
        void testGetAiReport_NoData_ReturnsEmptyPowerStats() throws Exception {
            // Given
            String roomId = "A1-301";
            String period = "7d";

            Map<String, Object> mockReport = createNoDataReport(roomId);
            when(aiReportService.getAiReport(roomId, period)).thenReturn(mockReport);

            // When & Then
            mockMvc.perform(get("/api/rooms/{roomId}/ai_report", roomId)
                            .param("period", period))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.power_stats.avg_power_w").value(0.0))
                    .andExpect(jsonPath("$.power_stats.peak_power_w").value(0.0));
        }
    }

    // ==================== TC-AI-004: 高功率异常 ====================

    @Nested
    @DisplayName("TC-AI-004: 高功率异常")
    class HighPowerAnomalyTests {

        @Test
        @DisplayName("TC-AI-004: 峰值功率>200W，包含异常提示")
        void testGetAiReport_HighPeakPower_ContainsAnomaly() throws Exception {
            // Given
            String roomId = "A1-301";
            String period = "7d";

            Map<String, Object> mockReport = createHighPowerReport(roomId, 250.0, 100.0);
            when(aiReportService.getAiReport(roomId, period)).thenReturn(mockReport);

            // When & Then
            mockMvc.perform(get("/api/rooms/{roomId}/ai_report", roomId)
                            .param("period", period))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.anomalies").isArray())
                    .andExpect(jsonPath("$.anomalies[0]").value(org.hamcrest.Matchers.containsString("Peak power")));
        }

        @Test
        @DisplayName("TC-AI-004: 高功率报告包含节能建议")
        void testGetAiReport_HighPower_ContainsRecommendations() throws Exception {
            // Given
            String roomId = "A1-301";
            String period = "7d";

            Map<String, Object> mockReport = createHighPowerReport(roomId, 220.0, 120.0);
            when(aiReportService.getAiReport(roomId, period)).thenReturn(mockReport);

            // When & Then
            mockMvc.perform(get("/api/rooms/{roomId}/ai_report", roomId)
                            .param("period", period))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recommendations").isArray())
                    .andExpect(jsonPath("$.recommendations.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        }

        @Test
        @DisplayName("TC-AI-004: 正常功率报告无峰值异常")
        void testGetAiReport_NormalPower_NoPeakAnomaly() throws Exception {
            // Given
            String roomId = "A1-301";
            String period = "7d";

            Map<String, Object> mockReport = createCompleteReport(roomId);
            when(aiReportService.getAiReport(roomId, period)).thenReturn(mockReport);

            // When & Then
            mockMvc.perform(get("/api/rooms/{roomId}/ai_report", roomId)
                            .param("period", period))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.power_stats.peak_power_w").value(150.0));
        }
    }

    // ==================== 辅助方法：创建测试数据 ====================

    /**
     * 创建完整的AI报告
     */
    private Map<String, Object> createCompleteReport(String roomId) {
        Map<String, Object> report = new HashMap<>();
        report.put("room_id", roomId);
        report.put("summary", "Average power is about 100.0W, peak is about 150.0W.");
        report.put("anomalies", new ArrayList<>());
        report.put("recommendations", List.of(
                "Enable auto off for low-priority sockets after 00:30.",
                "Set alerts for periods above baseline by 20%."
        ));
        report.put("generated_at", System.currentTimeMillis() / 1000);

        Map<String, Object> powerStats = new HashMap<>();
        powerStats.put("avg_power_w", 100.0);
        powerStats.put("peak_power_w", 150.0);
        powerStats.put("peak_time", "2024-01-15T10:30:00Z");
        powerStats.put("total_kwh", 12.5);
        report.put("power_stats", powerStats);

        return report;
    }

    /**
     * 创建无设备报告
     */
    private Map<String, Object> createNoDeviceReport(String roomId) {
        Map<String, Object> report = new HashMap<>();
        report.put("room_id", roomId);
        report.put("summary", "No device data in this room yet.");
        report.put("anomalies", List.of("No analyzable sample found."));
        report.put("recommendations", List.of("Ensure devices upload status and telemetry periodically."));
        report.put("generated_at", System.currentTimeMillis() / 1000);

        Map<String, Object> powerStats = new HashMap<>();
        powerStats.put("avg_power_w", 0.0);
        powerStats.put("peak_power_w", 0.0);
        powerStats.put("peak_time", "");
        powerStats.put("total_kwh", 0.0);
        report.put("power_stats", powerStats);

        return report;
    }

    /**
     * 创建无数据报告
     */
    private Map<String, Object> createNoDataReport(String roomId) {
        Map<String, Object> report = new HashMap<>();
        report.put("room_id", roomId);
        report.put("summary", "Devices are online but telemetry coverage is insufficient.");
        report.put("anomalies", List.of("Not enough telemetry points in selected period."));
        report.put("recommendations", List.of("Increase telemetry frequency to every 1-5 seconds."));
        report.put("generated_at", System.currentTimeMillis() / 1000);

        Map<String, Object> powerStats = new HashMap<>();
        powerStats.put("avg_power_w", 0.0);
        powerStats.put("peak_power_w", 0.0);
        powerStats.put("peak_time", "");
        powerStats.put("total_kwh", 0.0);
        report.put("power_stats", powerStats);

        return report;
    }

    /**
     * 创建高功率报告
     */
    private Map<String, Object> createHighPowerReport(String roomId, double peakPower, double avgPower) {
        Map<String, Object> report = new HashMap<>();
        report.put("room_id", roomId);
        report.put("summary", String.format("Average power is about %.1fW, peak is about %.1fW.", avgPower, peakPower));
        report.put("anomalies", List.of(
                String.format("Peak power reached %.1fW. Check high-load periods.", peakPower)
        ));
        report.put("recommendations", List.of(
                "Enable auto off for low-priority sockets after 00:30.",
                "Set alerts for periods above baseline by 20%.",
                "Consider scheduling high-power devices during off-peak hours."
        ));
        report.put("generated_at", System.currentTimeMillis() / 1000);

        Map<String, Object> powerStats = new HashMap<>();
        powerStats.put("avg_power_w", avgPower);
        powerStats.put("peak_power_w", peakPower);
        powerStats.put("peak_time", "2024-01-15T10:30:00Z");
        powerStats.put("total_kwh", 15.0);
        report.put("power_stats", powerStats);

        return report;
    }
}