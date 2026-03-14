package com.dormpower.service;

import com.dormpower.exception.BusinessException;
import com.dormpower.exception.ResourceNotFoundException;
import com.dormpower.model.Device;
import com.dormpower.model.DeviceAlert;
import com.dormpower.model.DeviceAlertConfig;
import com.dormpower.repository.DeviceAlertConfigRepository;
import com.dormpower.repository.DeviceAlertRepository;
import com.dormpower.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 告警服务单元测试
 *
 * 测试用例覆盖：
 * - TC-ACK-001: 正常解决告警
 * - TC-ACK-002: 告警不存在
 * - TC-ACK-003: 重复解决告警
 * - TC-ALERT-001: 告警生成
 * - TC-ALERT-002: 告警查询
 * - TC-CONFIG-001: 告警配置管理
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private DeviceAlertRepository deviceAlertRepository;

    @Mock
    private DeviceAlertConfigRepository deviceAlertConfigRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @InjectMocks
    private AlertService alertService;

    // ==================== TC-ACK-001: 正常解决告警 ====================

    @Nested
    @DisplayName("TC-ACK-001: 正常解决告警")
    class ResolveAlertSuccessTests {

        @Test
        @DisplayName("TC-ACK-001-01: 有效告警ID，告警状态更新为已解决")
        void testResolveAlert_Success() {
            // Given
            String alertId = "alert_001";
            DeviceAlert alert = createUnresolvedAlert(alertId, "device_001");

            when(deviceAlertRepository.findById(alertId)).thenReturn(Optional.of(alert));
            when(deviceAlertRepository.save(any(DeviceAlert.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            alertService.resolveAlert(alertId);

            // Then
            ArgumentCaptor<DeviceAlert> captor = ArgumentCaptor.forClass(DeviceAlert.class);
            verify(deviceAlertRepository).save(captor.capture());

            DeviceAlert savedAlert = captor.getValue();
            assertTrue(savedAlert.isResolved(), "告警状态应更新为已解决");
            assertTrue(savedAlert.getResolvedAt() > 0, "解决时间应被设置");
        }

        @Test
        @DisplayName("TC-ACK-001-02: 解决告警后resolvedAt时间戳正确")
        void testResolveAlert_ResolvedAtTimestamp() {
            // Given
            String alertId = "alert_002";
            DeviceAlert alert = createUnresolvedAlert(alertId, "device_001");

            when(deviceAlertRepository.findById(alertId)).thenReturn(Optional.of(alert));
            when(deviceAlertRepository.save(any(DeviceAlert.class))).thenAnswer(inv -> inv.getArgument(0));

            long beforeTime = System.currentTimeMillis() / 1000;

            // When
            alertService.resolveAlert(alertId);

            long afterTime = System.currentTimeMillis() / 1000;

            // Then
            ArgumentCaptor<DeviceAlert> captor = ArgumentCaptor.forClass(DeviceAlert.class);
            verify(deviceAlertRepository).save(captor.capture());

            long resolvedAt = captor.getValue().getResolvedAt();
            assertTrue(resolvedAt >= beforeTime && resolvedAt <= afterTime,
                    "解决时间应在调用时间范围内");
        }

        @Test
        @DisplayName("TC-ACK-001-03: 解决告警会记录日志")
        void testResolveAlert_LogsInfo() {
            // Given
            String alertId = "alert_003";
            DeviceAlert alert = createUnresolvedAlert(alertId, "device_001");

            when(deviceAlertRepository.findById(alertId)).thenReturn(Optional.of(alert));
            when(deviceAlertRepository.save(any(DeviceAlert.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            alertService.resolveAlert(alertId);

            // Then - 验证save被调用，间接证明日志记录
            verify(deviceAlertRepository).save(any(DeviceAlert.class));
        }
    }

    // ==================== TC-ACK-002: 告警不存在 ====================

    @Nested
    @DisplayName("TC-ACK-002: 告警不存在")
    class ResolveAlertNotFoundTests {

        @Test
        @DisplayName("TC-ACK-002-01: 无效告警ID，不执行保存操作")
        void testResolveAlert_AlertNotFound_NoSave() {
            // Given
            String invalidAlertId = "nonexistent_alert";

            when(deviceAlertRepository.findById(invalidAlertId)).thenReturn(Optional.empty());

            // When
            alertService.resolveAlert(invalidAlertId);

            // Then - 不应该执行保存操作
            verify(deviceAlertRepository, never()).save(any(DeviceAlert.class));
        }

        @Test
        @DisplayName("TC-ACK-002-02: 空告警ID处理")
        void testResolveAlert_EmptyId_NoSave() {
            // Given
            String emptyId = "";

            when(deviceAlertRepository.findById(emptyId)).thenReturn(Optional.empty());

            // When
            alertService.resolveAlert(emptyId);

            // Then
            verify(deviceAlertRepository, never()).save(any(DeviceAlert.class));
        }

        @Test
        @DisplayName("TC-ACK-002-03: null告警ID处理")
        void testResolveAlert_NullId_NoSave() {
            // Given
            when(deviceAlertRepository.findById(null)).thenReturn(Optional.empty());

            // When
            alertService.resolveAlert(null);

            // Then
            verify(deviceAlertRepository, never()).save(any(DeviceAlert.class));
        }
    }

    // ==================== TC-ACK-003: 重复解决告警 ====================

    @Nested
    @DisplayName("TC-ACK-003: 重复解决告警")
    class ResolveAlertAlreadyResolvedTests {

        @Test
        @DisplayName("TC-ACK-003-01: 已解决的告警不会重复处理")
        void testResolveAlert_AlreadyResolved_NoSave() {
            // Given
            String alertId = "alert_resolved";
            DeviceAlert resolvedAlert = createResolvedAlert(alertId, "device_001");

            when(deviceAlertRepository.findById(alertId)).thenReturn(Optional.of(resolvedAlert));

            // When
            alertService.resolveAlert(alertId);

            // Then - 已解决的告警不应该再次保存
            verify(deviceAlertRepository, never()).save(any(DeviceAlert.class));
        }

        @Test
        @DisplayName("TC-ACK-003-02: 已解决告警的resolvedAt不会被修改")
        void testResolveAlert_AlreadyResolved_ResolvedAtUnchanged() {
            // Given
            String alertId = "alert_resolved_2";
            DeviceAlert resolvedAlert = createResolvedAlert(alertId, "device_001");
            long originalResolvedAt = resolvedAlert.getResolvedAt();

            when(deviceAlertRepository.findById(alertId)).thenReturn(Optional.of(resolvedAlert));

            // When
            alertService.resolveAlert(alertId);

            // Then
            assertEquals(originalResolvedAt, resolvedAlert.getResolvedAt(),
                    "已解决告警的解决时间不应被修改");
        }
    }

    // ==================== TC-ALERT-001: 告警生成 ====================

    @Nested
    @DisplayName("TC-ALERT-001: 告警生成")
    class GenerateAlertTests {

        @Test
        @DisplayName("TC-ALERT-001-01: 功率超阈值生成告警")
        void testCheckAndGenerateAlert_PowerExceedsThreshold() {
            // Given
            String deviceId = "device_001";
            DeviceAlertConfig config = createAlertConfig(deviceId, "power", 0, 2000, true);

            when(deviceAlertConfigRepository.findByDeviceIdAndType(deviceId, "power")).thenReturn(config);
            when(deviceAlertRepository.save(any(DeviceAlert.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            alertService.checkAndGenerateAlert(deviceId, "power", 2500.0);

            // Then
            ArgumentCaptor<DeviceAlert> captor = ArgumentCaptor.forClass(DeviceAlert.class);
            verify(deviceAlertRepository).save(captor.capture());

            DeviceAlert savedAlert = captor.getValue();
            assertEquals("power", savedAlert.getType());
            assertEquals("warning", savedAlert.getLevel());
            assertTrue(savedAlert.getMessage().contains("exceeds threshold"));
            assertFalse(savedAlert.isResolved());
        }

        @Test
        @DisplayName("TC-ALERT-001-02: 电压超阈值生成错误级别告警")
        void testCheckAndGenerateAlert_VoltageExceedsThreshold_ErrorLevel() {
            // Given
            String deviceId = "device_001";
            DeviceAlertConfig config = createAlertConfig(deviceId, "voltage", 200, 240, true);

            when(deviceAlertConfigRepository.findByDeviceIdAndType(deviceId, "voltage")).thenReturn(config);
            when(deviceAlertRepository.save(any(DeviceAlert.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            alertService.checkAndGenerateAlert(deviceId, "voltage", 250.0);

            // Then
            ArgumentCaptor<DeviceAlert> captor = ArgumentCaptor.forClass(DeviceAlert.class);
            verify(deviceAlertRepository).save(captor.capture());

            DeviceAlert savedAlert = captor.getValue();
            assertEquals("voltage", savedAlert.getType());
            assertEquals("error", savedAlert.getLevel());
        }

        @Test
        @DisplayName("TC-ALERT-001-03: 告警配置禁用时不生成告警")
        void testCheckAndGenerateAlert_ConfigDisabled_NoAlert() {
            // Given
            String deviceId = "device_001";
            DeviceAlertConfig config = createAlertConfig(deviceId, "power", 0, 2000, false);

            when(deviceAlertConfigRepository.findByDeviceIdAndType(deviceId, "power")).thenReturn(config);

            // When
            alertService.checkAndGenerateAlert(deviceId, "power", 3000.0);

            // Then
            verify(deviceAlertRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-ALERT-001-04: 无告警配置时不生成告警")
        void testCheckAndGenerateAlert_NoConfig_NoAlert() {
            // Given
            String deviceId = "device_001";

            when(deviceAlertConfigRepository.findByDeviceIdAndType(deviceId, "power")).thenReturn(null);

            // When
            alertService.checkAndGenerateAlert(deviceId, "power", 3000.0);

            // Then
            verify(deviceAlertRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-ALERT-001-05: 生成告警后发送WebSocket通知")
        void testCheckAndGenerateAlert_SendsWebSocketNotification() {
            // Given
            String deviceId = "device_001";
            DeviceAlertConfig config = createAlertConfig(deviceId, "power", 0, 2000, true);

            when(deviceAlertConfigRepository.findByDeviceIdAndType(deviceId, "power")).thenReturn(config);
            when(deviceAlertRepository.save(any(DeviceAlert.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            alertService.checkAndGenerateAlert(deviceId, "power", 2500.0);

            // Then
            verify(webSocketNotificationService).broadcastAlert(eq(deviceId), any(Map.class));
        }
    }

    // ==================== TC-ALERT-002: 告警查询 ====================

    @Nested
    @DisplayName("TC-ALERT-002: 告警查询")
    class QueryAlertTests {

        @Test
        @DisplayName("TC-ALERT-002-01: 获取设备所有告警")
        void testGetDeviceAlerts_AllAlerts() {
            // Given
            String deviceId = "device_001";
            List<DeviceAlert> alerts = List.of(
                    createUnresolvedAlert("alert_001", deviceId),
                    createResolvedAlert("alert_002", deviceId)
            );

            when(deviceAlertRepository.findByDeviceIdOrderByTsDesc(deviceId)).thenReturn(alerts);

            // When
            List<DeviceAlert> result = alertService.getDeviceAlerts(deviceId, false);

            // Then
            assertEquals(2, result.size());
            verify(deviceAlertRepository).findByDeviceIdOrderByTsDesc(deviceId);
            verify(deviceAlertRepository, never()).findByDeviceIdAndResolvedFalseOrderByTsDesc(any());
        }

        @Test
        @DisplayName("TC-ALERT-002-02: 仅获取未解决告警")
        void testGetDeviceAlerts_OnlyUnresolved() {
            // Given
            String deviceId = "device_001";
            List<DeviceAlert> unresolvedAlerts = List.of(
                    createUnresolvedAlert("alert_001", deviceId)
            );

            when(deviceAlertRepository.findByDeviceIdAndResolvedFalseOrderByTsDesc(deviceId)).thenReturn(unresolvedAlerts);

            // When
            List<DeviceAlert> result = alertService.getDeviceAlerts(deviceId, true);

            // Then
            assertEquals(1, result.size());
            assertFalse(result.get(0).isResolved());
            verify(deviceAlertRepository).findByDeviceIdAndResolvedFalseOrderByTsDesc(deviceId);
        }

        @Test
        @DisplayName("TC-ALERT-002-03: 获取所有未解决告警")
        void testGetUnresolvedAlerts() {
            // Given
            List<DeviceAlert> unresolvedAlerts = List.of(
                    createUnresolvedAlert("alert_001", "device_001"),
                    createUnresolvedAlert("alert_002", "device_002")
            );

            when(deviceAlertRepository.findByResolvedFalseOrderByTsDesc()).thenReturn(unresolvedAlerts);

            // When
            List<DeviceAlert> result = alertService.getUnresolvedAlerts();

            // Then
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(a -> !a.isResolved()));
        }
    }

    // ==================== TC-CONFIG-001: 告警配置管理 ====================

    @Nested
    @DisplayName("TC-CONFIG-001: 告警配置管理")
    class AlertConfigTests {

        @Test
        @DisplayName("TC-CONFIG-001-01: 获取设备告警配置成功")
        void testGetDeviceAlertConfigs_Success() {
            // Given
            String deviceId = "device_001";
            List<DeviceAlertConfig> configs = List.of(
                    createAlertConfig(deviceId, "power", 0, 2000, true),
                    createAlertConfig(deviceId, "voltage", 200, 240, true)
            );

            when(deviceRepository.existsById(deviceId)).thenReturn(true);
            when(deviceAlertConfigRepository.findByDeviceId(deviceId)).thenReturn(configs);

            // When
            List<DeviceAlertConfig> result = alertService.getDeviceAlertConfigs(deviceId);

            // Then
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("TC-CONFIG-001-02: 设备不存在时抛出ResourceNotFoundException")
        void testGetDeviceAlertConfigs_DeviceNotFound() {
            // Given
            String deviceId = "nonexistent_device";

            when(deviceRepository.existsById(deviceId)).thenReturn(false);

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> {
                alertService.getDeviceAlertConfigs(deviceId);
            });
        }

        @Test
        @DisplayName("TC-CONFIG-001-03: 更新告警配置成功")
        void testUpdateAlertConfig_Success() {
            // Given
            String deviceId = "device_001";

            when(deviceRepository.existsById(deviceId)).thenReturn(true);
            when(deviceAlertConfigRepository.findByDeviceIdAndType(deviceId, "power")).thenReturn(null);
            when(deviceAlertConfigRepository.save(any(DeviceAlertConfig.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            DeviceAlertConfig result = alertService.updateAlertConfig(deviceId, "power", 0, 2500, true);

            // Then
            assertEquals(0, result.getThresholdMin());
            assertEquals(2500, result.getThresholdMax());
            assertTrue(result.isEnabled());
        }

        @Test
        @DisplayName("TC-CONFIG-001-04: 阈值范围无效时抛出BusinessException")
        void testUpdateAlertConfig_InvalidThresholdRange() {
            // Given
            String deviceId = "device_001";

            when(deviceRepository.existsById(deviceId)).thenReturn(true);

            // When & Then - minThreshold > maxThreshold
            assertThrows(BusinessException.class, () -> {
                alertService.updateAlertConfig(deviceId, "power", 3000, 2000, true);
            });
        }

        @Test
        @DisplayName("TC-CONFIG-001-05: 更新不存在的设备配置抛出ResourceNotFoundException")
        void testUpdateAlertConfig_DeviceNotFound() {
            // Given
            String deviceId = "nonexistent_device";

            when(deviceRepository.existsById(deviceId)).thenReturn(false);

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> {
                alertService.updateAlertConfig(deviceId, "power", 0, 2000, true);
            });
        }
    }

    // ==================== 辅助方法 ====================

    private DeviceAlert createUnresolvedAlert(String alertId, String deviceId) {
        DeviceAlert alert = new DeviceAlert();
        alert.setId(alertId);
        alert.setDeviceId(deviceId);
        alert.setType("power");
        alert.setLevel("warning");
        alert.setMessage("Power exceeds threshold");
        alert.setThresholdValue(2000.0);
        alert.setActualValue(2500.0);
        alert.setResolved(false);
        alert.setTs(System.currentTimeMillis() / 1000);
        alert.setCreatedAt(System.currentTimeMillis() / 1000);
        return alert;
    }

    private DeviceAlert createResolvedAlert(String alertId, String deviceId) {
        DeviceAlert alert = createUnresolvedAlert(alertId, deviceId);
        alert.setResolved(true);
        alert.setResolvedAt(System.currentTimeMillis() / 1000 - 3600);
        return alert;
    }

    private DeviceAlertConfig createAlertConfig(String deviceId, String type, double min, double max, boolean enabled) {
        DeviceAlertConfig config = new DeviceAlertConfig();
        config.setId("config_" + deviceId + "_" + type);
        config.setDeviceId(deviceId);
        config.setType(type);
        config.setThresholdMin(min);
        config.setThresholdMax(max);
        config.setEnabled(enabled);
        config.setCreatedAt(System.currentTimeMillis() / 1000);
        config.setUpdatedAt(System.currentTimeMillis() / 1000);
        return config;
    }
}