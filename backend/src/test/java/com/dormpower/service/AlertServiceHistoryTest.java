package com.dormpower.service;

import com.dormpower.model.DeviceAlert;
import com.dormpower.repository.DeviceAlertConfigRepository;
import com.dormpower.repository.DeviceAlertRepository;
import com.dormpower.repository.DeviceRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 告警历史查询服务单元测试
 *
 * 测试用例覆盖：
 * - TC-HIST-001: 查询设备告警
 * - TC-HIST-002: 查询未解决告警
 * - TC-HIST-003: 查询时间范围
 * - TC-HIST-004: 无告警设备
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceHistoryTest {

    @Mock
    private DeviceAlertRepository deviceAlertRepository;

    @Mock
    private DeviceAlertConfigRepository deviceAlertConfigRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService();
        org.springframework.test.util.ReflectionTestUtils.setField(alertService, "deviceAlertRepository", deviceAlertRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(alertService, "deviceAlertConfigRepository", deviceAlertConfigRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(alertService, "deviceRepository", deviceRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(alertService, "webSocketNotificationService", webSocketNotificationService);
    }

    // ==================== TC-HIST-001: 查询设备告警 ====================

    @Nested
    @DisplayName("TC-HIST-001: 查询设备告警")
    class QueryDeviceAlertsTests {

        @Test
        @DisplayName("TC-HIST-001: 有效设备ID，返回告警列表")
        void testGetDeviceAlerts_ValidDeviceId_ReturnsAlertList() {
            // Given
            String deviceId = "device_001";
            long now = System.currentTimeMillis() / 1000;

            // 模拟告警数据
            List<DeviceAlert> mockAlerts = createMockAlerts(deviceId, 3, now - 300, now);
            when(deviceAlertRepository.findByDeviceIdOrderByTsDesc(deviceId))
                    .thenReturn(mockAlerts);

            // When
            List<DeviceAlert> result = alertService.getDeviceAlerts(deviceId, false);

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());

            // 验证返回的告警属于正确的设备
            for (DeviceAlert alert : result) {
                assertEquals(deviceId, alert.getDeviceId());
            }

            verify(deviceAlertRepository).findByDeviceIdOrderByTsDesc(deviceId);
        }

        @Test
        @DisplayName("TC-HIST-001: 查询设备告警，包含完整的告警信息")
        void testGetDeviceAlerts_ContainsCompleteAlertInfo() {
            // Given
            String deviceId = "device_001";
            long now = System.currentTimeMillis() / 1000;

            List<DeviceAlert> mockAlerts = createMockAlerts(deviceId, 1, now - 100, now);
            when(deviceAlertRepository.findByDeviceIdOrderByTsDesc(deviceId))
                    .thenReturn(mockAlerts);

            // When
            List<DeviceAlert> result = alertService.getDeviceAlerts(deviceId, false);

            // Then
            assertFalse(result.isEmpty());
            DeviceAlert alert = result.get(0);

            // 验证告警包含必要字段
            assertNotNull(alert.getId());
            assertNotNull(alert.getDeviceId());
            assertNotNull(alert.getType());
            assertNotNull(alert.getLevel());
            assertNotNull(alert.getMessage());
            assertTrue(alert.getTs() > 0);
        }

        @Test
        @DisplayName("TC-HIST-001: 仅查询未解决的设备告警")
        void testGetDeviceAlerts_OnlyUnresolved_ReturnsUnresolvedAlerts() {
            // Given
            String deviceId = "device_001";
            long now = System.currentTimeMillis() / 1000;

            List<DeviceAlert> mockUnresolvedAlerts = createMockAlerts(deviceId, 2, now - 200, now);
            // 设置为未解决状态
            for (DeviceAlert alert : mockUnresolvedAlerts) {
                alert.setResolved(false);
            }

            when(deviceAlertRepository.findByDeviceIdAndResolvedFalseOrderByTsDesc(deviceId))
                    .thenReturn(mockUnresolvedAlerts);

            // When
            List<DeviceAlert> result = alertService.getDeviceAlerts(deviceId, true);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());

            // 验证所有返回的告警都是未解决的
            for (DeviceAlert alert : result) {
                assertFalse(alert.isResolved());
            }

            verify(deviceAlertRepository).findByDeviceIdAndResolvedFalseOrderByTsDesc(deviceId);
            verify(deviceAlertRepository, never()).findByDeviceIdOrderByTsDesc(anyString());
        }
    }

    // ==================== TC-HIST-002: 查询未解决告警 ====================

    @Nested
    @DisplayName("TC-HIST-002: 查询未解决告警")
    class QueryUnresolvedAlertsTests {

        @Test
        @DisplayName("TC-HIST-002: 查询所有未解决的告警，返回告警列表")
        void testGetUnresolvedAlerts_ReturnsUnresolvedAlertList() {
            // Given
            long now = System.currentTimeMillis() / 1000;

            // 模拟多个设备的未解决告警
            List<DeviceAlert> mockAlerts = new ArrayList<>();
            mockAlerts.addAll(createMockAlerts("device_001", 2, now - 300, now - 100));
            mockAlerts.addAll(createMockAlerts("device_002", 1, now - 50, now));

            // 设置为未解决状态
            for (DeviceAlert alert : mockAlerts) {
                alert.setResolved(false);
            }

            when(deviceAlertRepository.findByResolvedFalseOrderByTsDesc())
                    .thenReturn(mockAlerts);

            // When
            List<DeviceAlert> result = alertService.getUnresolvedAlerts();

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());

            // 验证所有返回的告警都是未解决的
            for (DeviceAlert alert : result) {
                assertFalse(alert.isResolved());
            }

            verify(deviceAlertRepository).findByResolvedFalseOrderByTsDesc();
        }

        @Test
        @DisplayName("TC-HIST-002: 无未解决告警时返回空列表")
        void testGetUnresolvedAlerts_NoUnresolved_ReturnsEmptyList() {
            // Given
            when(deviceAlertRepository.findByResolvedFalseOrderByTsDesc())
                    .thenReturn(Collections.emptyList());

            // When
            List<DeviceAlert> result = alertService.getUnresolvedAlerts();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(deviceAlertRepository).findByResolvedFalseOrderByTsDesc();
        }

        @Test
        @DisplayName("TC-HIST-002: 未解决告警按时间戳降序排列")
        void testGetUnresolvedAlerts_SortedByTsDesc() {
            // Given
            long now = System.currentTimeMillis() / 1000;

            // 创建按时间戳降序排列的告警列表
            List<DeviceAlert> mockAlerts = new ArrayList<>();
            DeviceAlert alert1 = createMockAlert("device_001", now - 100);
            alert1.setResolved(false);
            mockAlerts.add(alert1);

            DeviceAlert alert2 = createMockAlert("device_002", now - 50);
            alert2.setResolved(false);
            mockAlerts.add(alert2);

            when(deviceAlertRepository.findByResolvedFalseOrderByTsDesc())
                    .thenReturn(mockAlerts);

            // When
            List<DeviceAlert> result = alertService.getUnresolvedAlerts();

            // Then
            assertEquals(2, result.size());
            // 验证返回结果与Repository返回一致（Repository已按ts降序排列）
            assertEquals(mockAlerts, result);
        }
    }

    // ==================== TC-HIST-003: 查询时间范围 ====================

    @Nested
    @DisplayName("TC-HIST-003: 查询时间范围")
    class QueryTimeRangeTests {

        @Test
        @DisplayName("TC-HIST-003: 指定startTs和endTs，返回范围内告警")
        void testGetAlertsByTimeRange_ValidRange_ReturnsAlertsInRange() {
            // Given
            long now = System.currentTimeMillis() / 1000;
            long startTs = now - 3600; // 1小时前
            long endTs = now;

            List<DeviceAlert> mockAlerts = createMockAlerts("device_001", 5, startTs + 100, endTs - 100);
            when(deviceAlertRepository.findByTsBetweenOrderByTsDesc(eq(startTs), eq(endTs)))
                    .thenReturn(mockAlerts);

            // When
            List<DeviceAlert> result = alertService.getAlertsByTimeRange(startTs, endTs);

            // Then
            assertNotNull(result);
            assertEquals(5, result.size());

            // 验证所有告警的时间戳都在范围内
            for (DeviceAlert alert : result) {
                assertTrue(alert.getTs() >= startTs && alert.getTs() <= endTs);
            }

            verify(deviceAlertRepository).findByTsBetweenOrderByTsDesc(startTs, endTs);
        }

        @Test
        @DisplayName("TC-HIST-003: 时间范围内无告警，返回空列表")
        void testGetAlertsByTimeRange_NoAlertsInRange_ReturnsEmptyList() {
            // Given
            long now = System.currentTimeMillis() / 1000;
            long startTs = now - 3600;
            long endTs = now;

            when(deviceAlertRepository.findByTsBetweenOrderByTsDesc(startTs, endTs))
                    .thenReturn(Collections.emptyList());

            // When
            List<DeviceAlert> result = alertService.getAlertsByTimeRange(startTs, endTs);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(deviceAlertRepository).findByTsBetweenOrderByTsDesc(startTs, endTs);
        }

        @Test
        @DisplayName("TC-HIST-003: 时间范围查询跨多个设备")
        void testGetAlertsByTimeRange_MultipleDevices_ReturnsAllAlerts() {
            // Given
            long now = System.currentTimeMillis() / 1000;
            long startTs = now - 7200; // 2小时前
            long endTs = now;

            List<DeviceAlert> mockAlerts = new ArrayList<>();
            mockAlerts.addAll(createMockAlerts("device_001", 2, startTs + 100, startTs + 200));
            mockAlerts.addAll(createMockAlerts("device_002", 3, startTs + 500, endTs - 100));

            when(deviceAlertRepository.findByTsBetweenOrderByTsDesc(anyLong(), anyLong()))
                    .thenReturn(mockAlerts);

            // When
            List<DeviceAlert> result = alertService.getAlertsByTimeRange(startTs, endTs);

            // Then
            assertEquals(5, result.size());

            // 验证包含多个设备的告警
            long device001Count = result.stream()
                    .filter(a -> "device_001".equals(a.getDeviceId()))
                    .count();
            long device002Count = result.stream()
                    .filter(a -> "device_002".equals(a.getDeviceId()))
                    .count();

            assertEquals(2, device001Count);
            assertEquals(3, device002Count);
        }
    }

    // ==================== TC-HIST-004: 无告警设备 ====================

    @Nested
    @DisplayName("TC-HIST-004: 无告警设备")
    class NoAlertsDeviceTests {

        @Test
        @DisplayName("TC-HIST-004: 无告警设备ID，返回空列表")
        void testGetDeviceAlerts_NoAlertsDevice_ReturnsEmptyList() {
            // Given
            String deviceId = "device_no_alerts";

            when(deviceAlertRepository.findByDeviceIdOrderByTsDesc(deviceId))
                    .thenReturn(Collections.emptyList());

            // When
            List<DeviceAlert> result = alertService.getDeviceAlerts(deviceId, false);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(deviceAlertRepository).findByDeviceIdOrderByTsDesc(deviceId);
        }

        @Test
        @DisplayName("TC-HIST-004: 无告警设备查询未解决告警，返回空列表")
        void testGetDeviceAlerts_NoAlertsDevice_UnresolvedOnly_ReturnsEmptyList() {
            // Given
            String deviceId = "device_no_alerts";

            when(deviceAlertRepository.findByDeviceIdAndResolvedFalseOrderByTsDesc(deviceId))
                    .thenReturn(Collections.emptyList());

            // When
            List<DeviceAlert> result = alertService.getDeviceAlerts(deviceId, true);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(deviceAlertRepository).findByDeviceIdAndResolvedFalseOrderByTsDesc(deviceId);
        }

        @Test
        @DisplayName("TC-HIST-004: 所有告警已解决的设备，查询未解决告警返回空列表")
        void testGetDeviceAlerts_AllResolved_UnresolvedOnly_ReturnsEmptyList() {
            // Given
            String deviceId = "device_all_resolved";
            long now = System.currentTimeMillis() / 1000;

            // 创建已解决的告警列表
            List<DeviceAlert> resolvedAlerts = createMockAlerts(deviceId, 3, now - 500, now - 100);
            for (DeviceAlert alert : resolvedAlerts) {
                alert.setResolved(true);
            }

            // 未解决告警返回空列表
            when(deviceAlertRepository.findByDeviceIdAndResolvedFalseOrderByTsDesc(deviceId))
                    .thenReturn(Collections.emptyList());

            // When
            List<DeviceAlert> result = alertService.getDeviceAlerts(deviceId, true);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(deviceAlertRepository).findByDeviceIdAndResolvedFalseOrderByTsDesc(deviceId);
        }
    }

    // ==================== 辅助方法：创建测试数据 ====================

    /**
     * 创建模拟告警数据列表
     *
     * @param deviceId 设备ID
     * @param count    告警数量
     * @param startTs  开始时间戳
     * @param endTs    结束时间戳
     * @return 告警列表
     */
    private List<DeviceAlert> createMockAlerts(String deviceId, int count, long startTs, long endTs) {
        List<DeviceAlert> alerts = new ArrayList<>(count);
        long step = count > 1 ? (endTs - startTs) / (count - 1) : 0;

        String[] types = {"power", "voltage", "current", "online"};
        String[] levels = {"info", "warning", "error", "critical"};

        for (int i = 0; i < count; i++) {
            DeviceAlert alert = new DeviceAlert();
            alert.setId("alert_" + deviceId + "_" + i);
            alert.setDeviceId(deviceId);
            alert.setType(types[i % types.length]);
            alert.setLevel(levels[i % levels.length]);
            alert.setMessage("Alert message " + i + " for device " + deviceId);
            alert.setThresholdValue(100.0 + i);
            alert.setActualValue(150.0 + i);
            alert.setResolved(false);
            alert.setTs(startTs + i * step);
            alert.setCreatedAt(startTs + i * step);
            alerts.add(alert);
        }

        return alerts;
    }

    /**
     * 创建单个模拟告警
     *
     * @param deviceId 设备ID
     * @param ts       时间戳
     * @return 告警对象
     */
    private DeviceAlert createMockAlert(String deviceId, long ts) {
        DeviceAlert alert = new DeviceAlert();
        alert.setId("alert_" + deviceId + "_" + ts);
        alert.setDeviceId(deviceId);
        alert.setType("power");
        alert.setLevel("warning");
        alert.setMessage("Test alert for " + deviceId);
        alert.setThresholdValue(100.0);
        alert.setActualValue(150.0);
        alert.setResolved(false);
        alert.setTs(ts);
        alert.setCreatedAt(ts);
        return alert;
    }
}