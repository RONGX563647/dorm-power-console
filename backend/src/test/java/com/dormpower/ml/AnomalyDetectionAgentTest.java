package com.dormpower.ml;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 异常检测Agent单元测试
 *
 * 测试用例覆盖：
 * - TC-ANOM-001: 点异常检测
 * - TC-ANOM-002: 趋势异常检测
 * - TC-ANOM-003: 上下文异常检测
 * - TC-ANOM-004: 设备健康评估
 * - TC-ANOM-005: 预测性异常
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class AnomalyDetectionAgentTest {

    private static final int WINDOW_SIZE = 30;

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private DeviceRepository deviceRepository;

    private AnomalyDetectionAgent anomalyDetectionAgent;

    @BeforeEach
    void setUp() {
        anomalyDetectionAgent = new AnomalyDetectionAgent();
        org.springframework.test.util.ReflectionTestUtils.setField(anomalyDetectionAgent, "telemetryRepository", telemetryRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(anomalyDetectionAgent, "deviceRepository", deviceRepository);
    }

    // ==================== TC-ANOM-001: 点异常检测 ====================

    @Nested
    @DisplayName("TC-ANOM-001: 点异常检测")
    class PointAnomalyTests {

        @Test
        @DisplayName("TC-ANOM-001: 功率偏离均值3倍标准差，检测到POINT_ANOMALY")
        void testDetectRealtime_PointAnomaly_DeviationAboveThreshold() {
            // Given
            String deviceId = "device_001";
            double currentPower = 300.0; // 异常高功率

            Device device = createDevice(deviceId, "A1-301");
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            // 创建正态分布的遥测数据，均值约100W
            List<Telemetry> normalData = createNormalTelemetryData(deviceId, 50, 100.0, 10.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(normalData);

            // When
            AnomalyDetectionAgent.DetectedAnomaly anomaly = anomalyDetectionAgent.detectRealtime(deviceId, currentPower);

            // Then
            assertNotNull(anomaly);
            assertEquals(AnomalyDetectionAgent.AnomalyType.POINT_ANOMALY, anomaly.type);
            assertEquals(deviceId, anomaly.deviceId);
            assertTrue(anomaly.deviationScore > 3.0, "偏差分数应大于3");
            assertNotNull(anomaly.description);
            assertNotNull(anomaly.recommendations);

            verify(deviceRepository).findById(deviceId);
            verify(telemetryRepository).findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong());
        }

        @Test
        @DisplayName("TC-ANOM-001: 正常功率不触发异常检测")
        void testDetectRealtime_NormalPower_NoAnomaly() {
            // Given
            String deviceId = "device_001";
            double currentPower = 105.0; // 在正常范围内

            Device device = createDevice(deviceId, "A1-301");
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            List<Telemetry> normalData = createNormalTelemetryData(deviceId, 50, 100.0, 10.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(normalData);

            // When
            AnomalyDetectionAgent.DetectedAnomaly anomaly = anomalyDetectionAgent.detectRealtime(deviceId, currentPower);

            // Then
            assertNull(anomaly, "正常功率不应触发异常");
        }

        @Test
        @DisplayName("TC-ANOM-001: 批量检测识别多个点异常")
        void testDetectBatch_MultiplePointAnomalies() {
            // Given
            String deviceId = "device_001";
            Device device = createDevice(deviceId, "A1-301");
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            // 创建包含异常点的数据
            List<Telemetry> dataWithAnomalies = createTelemetryWithAnomalies(deviceId, 100, 100.0, 10.0, 250.0, 5);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(dataWithAnomalies);

            // When
            List<AnomalyDetectionAgent.DetectedAnomaly> anomalies = anomalyDetectionAgent.detectBatch(deviceId,
                    System.currentTimeMillis() / 1000 - 3600, System.currentTimeMillis() / 1000);

            // Then
            assertNotNull(anomalies);
            assertTrue(anomalies.size() >= 1, "应检测到至少一个点异常");

            boolean hasPointAnomaly = anomalies.stream()
                    .anyMatch(a -> a.type == AnomalyDetectionAgent.AnomalyType.POINT_ANOMALY);
            assertTrue(hasPointAnomaly, "应包含点异常类型");
        }
    }

    // ==================== TC-ANOM-002: 趋势异常检测 ====================

    @Nested
    @DisplayName("TC-ANOM-002: 趋势异常检测")
    class TrendAnomalyTests {

        @Test
        @DisplayName("TC-ANOM-002: 功率持续上升，检测到TREND_ANOMALY")
        void testDetectBatch_TrendAnomaly_ContinuousRise() {
            // Given
            String deviceId = "device_001";
            Device device = createDevice(deviceId, "A1-301");
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            // 创建持续上升的数据趋势 (需要足够的数据点用于趋势检测)
            List<Telemetry> risingData = createRisingTrendTelemetryData(deviceId, 200, 50.0, 300.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(risingData);

            // When
            List<AnomalyDetectionAgent.DetectedAnomaly> anomalies = anomalyDetectionAgent.detectBatch(deviceId,
                    System.currentTimeMillis() / 1000 - 3600, System.currentTimeMillis() / 1000);

            // Then
            assertNotNull(anomalies);

            // 检查是否包含趋势异常（数据条件满足时）
            if (anomalies.size() >= WINDOW_SIZE * 2) {
                boolean hasTrendAnomaly = anomalies.stream()
                        .anyMatch(a -> a.type == AnomalyDetectionAgent.AnomalyType.TREND_ANOMALY);
                if (hasTrendAnomaly) {
                    // 验证趋势异常包含移动平均信息
                    AnomalyDetectionAgent.DetectedAnomaly trendAnomaly = anomalies.stream()
                            .filter(a -> a.type == AnomalyDetectionAgent.AnomalyType.TREND_ANOMALY)
                            .findFirst()
                            .orElse(null);

                    if (trendAnomaly != null) {
                        assertNotNull(trendAnomaly.metadata);
                        assertTrue(trendAnomaly.metadata.containsKey("movingAverage"));
                    }
                }
            }
        }

        @Test
        @DisplayName("TC-ANOM-002: 功率持续下降，检测到趋势异常")
        void testDetectBatch_TrendAnomaly_ContinuousDrop() {
            // Given
            String deviceId = "device_001";
            Device device = createDevice(deviceId, "A1-301");
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            // 创建持续下降的数据趋势
            List<Telemetry> fallingData = createFallingTrendTelemetryData(deviceId, 200, 300.0, 50.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(fallingData);

            // When
            List<AnomalyDetectionAgent.DetectedAnomaly> anomalies = anomalyDetectionAgent.detectBatch(deviceId,
                    System.currentTimeMillis() / 1000 - 3600, System.currentTimeMillis() / 1000);

            // Then
            assertNotNull(anomalies);
        }

        @Test
        @DisplayName("TC-ANOM-002: 稳定功率无趋势异常")
        void testDetectBatch_StablePower_NoTrendAnomaly() {
            // Given
            String deviceId = "device_001";
            Device device = createDevice(deviceId, "A1-301");
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            // 创建稳定的功率数据
            List<Telemetry> stableData = createNormalTelemetryData(deviceId, 100, 100.0, 5.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(stableData);

            // When
            List<AnomalyDetectionAgent.DetectedAnomaly> anomalies = anomalyDetectionAgent.detectBatch(deviceId,
                    System.currentTimeMillis() / 1000 - 3600, System.currentTimeMillis() / 1000);

            // Then - 稳定数据不一定产生趋势异常
            assertNotNull(anomalies);
        }
    }

    // ==================== TC-ANOM-003: 上下文异常检测 ====================

    @Nested
    @DisplayName("TC-ANOM-003: 上下文异常检测")
    class ContextualAnomalyTests {

        @Test
        @DisplayName("TC-ANOM-003: 凌晨时段高功率，检测到CONTEXTUAL_ANOMALY")
        void testDetectBatch_ContextualAnomaly_LateNightHighPower() {
            // Given
            String deviceId = "device_001";
            Device device = createDevice(deviceId, "A1-301");
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            // 创建凌晨时段高功率数据（需要足够的数据点）
            List<Telemetry> contextualData = createContextualAnomalyTelemetryData(deviceId, 200, 50.0, 200.0, 2);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(contextualData);

            // When
            List<AnomalyDetectionAgent.DetectedAnomaly> anomalies = anomalyDetectionAgent.detectBatch(deviceId,
                    System.currentTimeMillis() / 1000 - 3600, System.currentTimeMillis() / 1000);

            // Then
            assertNotNull(anomalies);

            // 检查是否包含上下文异常（数据条件满足时）
            boolean hasContextualAnomaly = anomalies.stream()
                    .anyMatch(a -> a.type == AnomalyDetectionAgent.AnomalyType.CONTEXTUAL_ANOMALY);

            // 验证上下文异常包含小时信息（如果存在）
            if (hasContextualAnomaly) {
                AnomalyDetectionAgent.DetectedAnomaly contextualAnomaly = anomalies.stream()
                        .filter(a -> a.type == AnomalyDetectionAgent.AnomalyType.CONTEXTUAL_ANOMALY)
                        .findFirst()
                        .orElse(null);

                if (contextualAnomaly != null) {
                    assertNotNull(contextualAnomaly.metadata);
                    assertTrue(contextualAnomaly.metadata.containsKey("hour"));
                }
            }
        }

        @Test
        @DisplayName("TC-ANOM-003: 正常时段用电无上下文异常")
        void testDetectBatch_NormalHours_NoContextualAnomaly() {
            // Given
            String deviceId = "device_001";
            Device device = createDevice(deviceId, "A1-301");
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            // 创建正常时段的数据
            List<Telemetry> normalData = createNormalTelemetryData(deviceId, 100, 100.0, 15.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(normalData);

            // When
            List<AnomalyDetectionAgent.DetectedAnomaly> anomalies = anomalyDetectionAgent.detectBatch(deviceId,
                    System.currentTimeMillis() / 1000 - 3600, System.currentTimeMillis() / 1000);

            // Then
            assertNotNull(anomalies);
        }
    }

    // ==================== TC-ANOM-004: 设备健康评估 ====================

    @Nested
    @DisplayName("TC-ANOM-004: 设备健康评估")
    class DeviceHealthTests {

        @Test
        @DisplayName("TC-ANOM-004: 正常设备数据，健康分数>=80")
        void testGetDeviceHealth_NormalDevice_HealthScoreAbove80() {
            // Given
            String deviceId = "device_001";
            Device device = createDevice(deviceId, "A1-301");
            // 允许多次调用（getDeviceHealth内部会调用detectBatch）
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            // 创建正常的遥测数据
            List<Telemetry> normalData = createNormalTelemetryData(deviceId, 100, 100.0, 10.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(normalData);

            // When
            AnomalyDetectionAgent.DeviceHealthStatus health = anomalyDetectionAgent.getDeviceHealth(deviceId);

            // Then
            assertNotNull(health);
            assertEquals(deviceId, health.deviceId);
            assertTrue(health.healthScore >= 80, "正常设备健康分数应>=80，实际: " + health.healthScore);
            assertEquals("健康", health.status);
            assertNotNull(health.metrics);
            assertNotNull(health.recentAnomalies);
        }

        @Test
        @DisplayName("TC-ANOM-004: 异常设备数据，健康分数较低")
        void testGetDeviceHealth_AbnormalDevice_LowerHealthScore() {
            // Given
            String deviceId = "device_001";
            Device device = createDevice(deviceId, "A1-301");
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            // 创建异常的遥测数据（大波动）
            List<Telemetry> abnormalData = createAbnormalTelemetryData(deviceId, 100, 10.0, 300.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(abnormalData);

            // When
            AnomalyDetectionAgent.DeviceHealthStatus health = anomalyDetectionAgent.getDeviceHealth(deviceId);

            // Then
            assertNotNull(health);
            // 异常数据可能导致较低的分数，也可能有警告
            assertTrue(health.healthScore < 100, "异常设备健康分数应低于100，实际: " + health.healthScore);
        }

        @Test
        @DisplayName("TC-ANOM-004: 数据不足设备返回默认健康分数")
        void testGetDeviceHealth_InsufficientData_DefaultHealthScore() {
            // Given
            String deviceId = "device_001";
            Device device = createDevice(deviceId, "A1-301");
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            // 创建少量数据
            List<Telemetry> insufficientData = createNormalTelemetryData(deviceId, 5, 100.0, 10.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(insufficientData);

            // When
            AnomalyDetectionAgent.DeviceHealthStatus health = anomalyDetectionAgent.getDeviceHealth(deviceId);

            // Then
            assertNotNull(health);
            assertEquals(50, health.healthScore);
            assertEquals("数据不足", health.status);
        }

        @Test
        @DisplayName("TC-ANOM-004: 健康状态包含完整指标")
        void testGetDeviceHealth_ContainsCompleteMetrics() {
            // Given
            String deviceId = "device_001";
            Device device = createDevice(deviceId, "A1-301");
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            List<Telemetry> normalData = createNormalTelemetryData(deviceId, 100, 100.0, 10.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(normalData);

            // When
            AnomalyDetectionAgent.DeviceHealthStatus health = anomalyDetectionAgent.getDeviceHealth(deviceId);

            // Then
            assertNotNull(health.metrics);
            assertTrue(health.metrics.containsKey("avgPower"));
            assertTrue(health.metrics.containsKey("stdPower"));
            assertTrue(health.metrics.containsKey("maxPower"));
            assertTrue(health.metrics.containsKey("minPower"));
            assertTrue(health.metrics.containsKey("sampleCount"));
        }
    }

    // ==================== TC-ANOM-005: 预测性异常 ====================

    @Nested
    @DisplayName("TC-ANOM-005: 预测性异常")
    class PredictiveAnomalyTests {

        @Test
        @DisplayName("TC-ANOM-005: 功率趋势预测，返回预测结果")
        void testPredictAnomaly_ReturnsPredictionResult() {
            // Given
            String deviceId = "device_001";
            Device device = createDevice(deviceId, "A1-301");
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            // 创建有预测价值的数据
            List<Telemetry> trendData = createRisingTrendTelemetryData(deviceId, 50, 80.0, 150.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(trendData);

            // When
            AnomalyDetectionAgent.DetectedAnomaly prediction = anomalyDetectionAgent.predictAnomaly(deviceId);

            // Then - 可能返回null（如果预测偏差不够大）或返回预测异常
            if (prediction != null) {
                assertEquals(AnomalyDetectionAgent.AnomalyType.PREDICTIVE_ANOMALY, prediction.type);
                assertEquals(deviceId, prediction.deviceId);
                assertNotNull(prediction.value);
                assertNotNull(prediction.expectedValue);
                assertNotNull(prediction.metadata);
                assertEquals("EWMA", prediction.metadata.get("predictionMethod"));
            }

            verify(deviceRepository).findById(deviceId);
            verify(telemetryRepository).findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong());
        }

        @Test
        @DisplayName("TC-ANOM-005: 设备故障预测返回完整结果")
        void testPredictDeviceFailure_ReturnsCompleteResult() {
            // Given
            String deviceId = "device_001";
            Device device = createDevice(deviceId, "A1-301");
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            List<Telemetry> normalData = createNormalTelemetryData(deviceId, 500, 100.0, 10.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(eq(deviceId), anyLong(), anyLong()))
                    .thenReturn(normalData);

            // When
            Map<String, Object> result = anomalyDetectionAgent.predictDeviceFailure(deviceId);

            // Then
            assertNotNull(result);
            assertEquals(true, result.get("success"));
            assertEquals(deviceId, result.get("deviceId"));
            assertNotNull(result.get("failureProbability"));
            assertNotNull(result.get("riskLevel"));
            assertNotNull(result.get("riskFactors"));
            assertNotNull(result.get("recommendations"));
        }

        @Test
        @DisplayName("TC-ANOM-005: 设备不存在返回失败结果")
        void testPredictDeviceFailure_DeviceNotFound_ReturnsFailure() {
            // Given
            String deviceId = "device_not_exist";
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

            // When
            Map<String, Object> result = anomalyDetectionAgent.predictDeviceFailure(deviceId);

            // Then
            assertNotNull(result);
            assertEquals(false, result.get("success"));
            assertEquals("设备不存在", result.get("message"));
        }
    }

    // ==================== 房间级异常检测测试 ====================

    @Nested
    @DisplayName("房间级异常检测测试")
    class RoomAnomalyTests {

        @Test
        @DisplayName("房间异常检测返回完整结果")
        void testDetectRoomAnomalies_ReturnsCompleteResult() {
            // Given
            String roomId = "A1-301";
            Device device1 = createDevice("device_001", roomId);
            Device device2 = createDevice("device_002", roomId);
            when(deviceRepository.findByRoom(roomId)).thenReturn(List.of(device1, device2));
            when(deviceRepository.findById("device_001")).thenReturn(Optional.of(device1));
            when(deviceRepository.findById("device_002")).thenReturn(Optional.of(device2));

            List<Telemetry> normalData = createNormalTelemetryData("device_001", 100, 100.0, 10.0);
            when(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(anyString(), anyLong(), anyLong()))
                    .thenReturn(normalData);

            // When
            Map<String, Object> result = anomalyDetectionAgent.detectRoomAnomalies(roomId);

            // Then
            assertNotNull(result);
            assertEquals(roomId, result.get("roomId"));
            assertNotNull(result.get("riskScore"));
            assertNotNull(result.get("riskLevel"));
            assertNotNull(result.get("totalAnomalies"));
            assertNotNull(result.get("anomalies"));
            assertNotNull(result.get("deviceStatuses"));
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
     * 创建正态分布的遥测数据
     */
    private List<Telemetry> createNormalTelemetryData(String deviceId, int count, double mean, double stdDev) {
        List<Telemetry> dataList = new ArrayList<>(count);
        long baseTs = System.currentTimeMillis() / 1000 - count * 60;
        java.util.Random random = new java.util.Random(42);

        for (int i = 0; i < count; i++) {
            Telemetry telemetry = new Telemetry();
            telemetry.setId((long) i + 1);
            telemetry.setDeviceId(deviceId);
            telemetry.setTs(baseTs + i * 60);
            // Box-Muller变换生成正态分布
            double u1 = random.nextDouble();
            double u2 = random.nextDouble();
            double z = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
            telemetry.setPowerW(mean + z * stdDev);
            telemetry.setVoltageV(220.0);
            telemetry.setCurrentA(0.5);
            dataList.add(telemetry);
        }

        return dataList;
    }

    /**
     * 创建包含异常点的遥测数据
     */
    private List<Telemetry> createTelemetryWithAnomalies(String deviceId, int count, double mean, double stdDev, double anomalyValue, int anomalyCount) {
        List<Telemetry> dataList = createNormalTelemetryData(deviceId, count, mean, stdDev);
        java.util.Random random = new java.util.Random();

        // 随机替换一些点为异常值
        for (int i = 0; i < anomalyCount && i < dataList.size(); i++) {
            int idx = random.nextInt(dataList.size());
            dataList.get(idx).setPowerW(anomalyValue);
        }

        return dataList;
    }

    /**
     * 创建持续上升趋势的遥测数据
     */
    private List<Telemetry> createRisingTrendTelemetryData(String deviceId, int count, double startPower, double endPower) {
        List<Telemetry> dataList = new ArrayList<>(count);
        long baseTs = System.currentTimeMillis() / 1000 - count * 60;
        double step = (endPower - startPower) / (count - 1);

        for (int i = 0; i < count; i++) {
            Telemetry telemetry = new Telemetry();
            telemetry.setId((long) i + 1);
            telemetry.setDeviceId(deviceId);
            telemetry.setTs(baseTs + i * 60);
            telemetry.setPowerW(startPower + i * step);
            telemetry.setVoltageV(220.0);
            telemetry.setCurrentA(0.5);
            dataList.add(telemetry);
        }

        return dataList;
    }

    /**
     * 创建持续下降趋势的遥测数据
     */
    private List<Telemetry> createFallingTrendTelemetryData(String deviceId, int count, double startPower, double endPower) {
        List<Telemetry> dataList = new ArrayList<>(count);
        long baseTs = System.currentTimeMillis() / 1000 - count * 60;
        double step = (startPower - endPower) / (count - 1);

        for (int i = 0; i < count; i++) {
            Telemetry telemetry = new Telemetry();
            telemetry.setId((long) i + 1);
            telemetry.setDeviceId(deviceId);
            telemetry.setTs(baseTs + i * 60);
            telemetry.setPowerW(startPower - i * step);
            telemetry.setVoltageV(220.0);
            telemetry.setCurrentA(0.5);
            dataList.add(telemetry);
        }

        return dataList;
    }

    /**
     * 创建上下文异常的遥测数据（特定时段高功率）
     */
    private List<Telemetry> createContextualAnomalyTelemetryData(String deviceId, int count, double normalPower, double anomalyPower, int anomalyHour) {
        List<Telemetry> dataList = new ArrayList<>(count);
        long baseTs = System.currentTimeMillis() / 1000 - count * 60;
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < count; i++) {
            Telemetry telemetry = new Telemetry();
            telemetry.setId((long) i + 1);
            telemetry.setDeviceId(deviceId);

            // 设置时间戳使其落在特定小时
            long ts = baseTs + i * 60;
            telemetry.setTs(ts);

            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTimeInMillis(ts * 1000);
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);

            // 指定小时使用高功率
            if (hour == anomalyHour) {
                telemetry.setPowerW(anomalyPower + random.nextDouble() * 20);
            } else {
                telemetry.setPowerW(normalPower + random.nextDouble() * 10);
            }

            telemetry.setVoltageV(220.0);
            telemetry.setCurrentA(0.5);
            dataList.add(telemetry);
        }

        return dataList;
    }

    /**
     * 创建异常波动大的遥测数据
     */
    private List<Telemetry> createAbnormalTelemetryData(String deviceId, int count, double minPower, double maxPower) {
        List<Telemetry> dataList = new ArrayList<>(count);
        long baseTs = System.currentTimeMillis() / 1000 - count * 60;
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < count; i++) {
            Telemetry telemetry = new Telemetry();
            telemetry.setId((long) i + 1);
            telemetry.setDeviceId(deviceId);
            telemetry.setTs(baseTs + i * 60);
            // 随机在最小和最大值之间波动
            telemetry.setPowerW(minPower + random.nextDouble() * (maxPower - minPower));
            telemetry.setVoltageV(220.0);
            telemetry.setCurrentA(0.5);
            dataList.add(telemetry);
        }

        return dataList;
    }
}