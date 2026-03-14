package com.dormpower.service;

import com.dormpower.model.DeviceAlert;
import com.dormpower.model.DeviceAlertConfig;
import com.dormpower.repository.DeviceAlertConfigRepository;
import com.dormpower.repository.DeviceAlertRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 告警触发单元测试
 *
 * 测试用例覆盖：
 * - TC-TRIG-001: 功率超限告警
 * - TC-TRIG-002: 电压超限告警
 * - TC-TRIG-003: 电流超限告警
 * - TC-TRIG-004: 配置禁用
 * - TC-TRIG-005: 重复告警
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTriggerTest {

    @Mock
    private DeviceAlertRepository deviceAlertRepository;

    @Mock
    private DeviceAlertConfigRepository deviceAlertConfigRepository;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @InjectMocks
    private AlertService alertService;

    private static final String DEVICE_ID = "device_001";
    private static final double POWER_THRESHOLD_MAX = 2000.0;
    private static final double POWER_THRESHOLD_MIN = 0.0;
    private static final double VOLTAGE_THRESHOLD_MAX = 240.0;
    private static final double VOLTAGE_THRESHOLD_MIN = 200.0;
    private static final double CURRENT_THRESHOLD_MAX = 10.0;
    private static final double CURRENT_THRESHOLD_MIN = 0.0;

    // ==================== TC-TRIG-001: 功率超限告警 ====================

    @Test
    @DisplayName("TC-TRIG-001: 功率超限告警 - 功率超过阈值上限，生成warning级别告警")
    void testPowerOverThreshold_GeneratesWarningAlert() {
        // Given
        DeviceAlertConfig config = createPowerConfig(true);
        double overThresholdValue = 2500.0; // 超过阈值 2000W

        when(deviceAlertConfigRepository.findByDeviceIdAndType(DEVICE_ID, "power")).thenReturn(config);
        when(deviceAlertRepository.findByDeviceIdAndTypeAndResolvedFalseOrderByTsDesc(DEVICE_ID, "power"))
                .thenReturn(Collections.emptyList());
        when(deviceAlertRepository.save(any(DeviceAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        alertService.checkAndGenerateAlert(DEVICE_ID, "power", overThresholdValue);

        // Then
        ArgumentCaptor<DeviceAlert> alertCaptor = ArgumentCaptor.forClass(DeviceAlert.class);
        verify(deviceAlertRepository).save(alertCaptor.capture());

        DeviceAlert savedAlert = alertCaptor.getValue();
        assertEquals("power", savedAlert.getType());
        assertEquals("warning", savedAlert.getLevel(), "功率超限应生成warning级别告警");
        assertTrue(savedAlert.getMessage().contains("Power exceeds threshold"));
        assertEquals(overThresholdValue, savedAlert.getActualValue());
        assertEquals(POWER_THRESHOLD_MAX, savedAlert.getThresholdValue());
        assertFalse(savedAlert.isResolved());

        // 验证WebSocket通知被发送
        verify(webSocketNotificationService).broadcastAlert(eq(DEVICE_ID), any(Map.class));
    }

    @Test
    @DisplayName("TC-TRIG-001: 功率超限告警 - 功率低于阈值下限，生成info级别告警")
    void testPowerBelowThreshold_GeneratesInfoAlert() {
        // Given
        DeviceAlertConfig config = createPowerConfig(true);
        config.setThresholdMin(100.0);
        double belowThresholdValue = 50.0;

        when(deviceAlertConfigRepository.findByDeviceIdAndType(DEVICE_ID, "power")).thenReturn(config);
        when(deviceAlertRepository.findByDeviceIdAndTypeAndResolvedFalseOrderByTsDesc(DEVICE_ID, "power"))
                .thenReturn(Collections.emptyList());
        when(deviceAlertRepository.save(any(DeviceAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        alertService.checkAndGenerateAlert(DEVICE_ID, "power", belowThresholdValue);

        // Then
        ArgumentCaptor<DeviceAlert> alertCaptor = ArgumentCaptor.forClass(DeviceAlert.class);
        verify(deviceAlertRepository).save(alertCaptor.capture());

        DeviceAlert savedAlert = alertCaptor.getValue();
        assertEquals("info", savedAlert.getLevel(), "功率低于阈值应生成info级别告警");
        assertTrue(savedAlert.getMessage().contains("Power below threshold"));
    }

    @Test
    @DisplayName("TC-TRIG-001: 功率超限告警 - 功率在正常范围内，不生成告警")
    void testPowerInNormalRange_NoAlertGenerated() {
        // Given
        DeviceAlertConfig config = createPowerConfig(true);
        double normalValue = 1500.0; // 在正常范围内

        when(deviceAlertConfigRepository.findByDeviceIdAndType(DEVICE_ID, "power")).thenReturn(config);
        when(deviceAlertRepository.findByDeviceIdAndTypeAndResolvedFalseOrderByTsDesc(DEVICE_ID, "power"))
                .thenReturn(Collections.emptyList());

        // When
        alertService.checkAndGenerateAlert(DEVICE_ID, "power", normalValue);

        // Then
        verify(deviceAlertRepository, never()).save(any());
        verify(webSocketNotificationService, never()).broadcastAlert(anyString(), any(Map.class));
    }

    // ==================== TC-TRIG-002: 电压超限告警 ====================

    @Test
    @DisplayName("TC-TRIG-002: 电压超限告警 - 电压超过阈值上限，生成error级别告警")
    void testVoltageOverThreshold_GeneratesErrorAlert() {
        // Given
        DeviceAlertConfig config = createVoltageConfig(true);
        double overThresholdValue = 250.0; // 超过阈值 240V

        when(deviceAlertConfigRepository.findByDeviceIdAndType(DEVICE_ID, "voltage")).thenReturn(config);
        when(deviceAlertRepository.findByDeviceIdAndTypeAndResolvedFalseOrderByTsDesc(DEVICE_ID, "voltage"))
                .thenReturn(Collections.emptyList());
        when(deviceAlertRepository.save(any(DeviceAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        alertService.checkAndGenerateAlert(DEVICE_ID, "voltage", overThresholdValue);

        // Then
        ArgumentCaptor<DeviceAlert> alertCaptor = ArgumentCaptor.forClass(DeviceAlert.class);
        verify(deviceAlertRepository).save(alertCaptor.capture());

        DeviceAlert savedAlert = alertCaptor.getValue();
        assertEquals("voltage", savedAlert.getType());
        assertEquals("error", savedAlert.getLevel(), "电压超限应生成error级别告警");
        assertTrue(savedAlert.getMessage().contains("Voltage exceeds threshold"));
        assertEquals(overThresholdValue, savedAlert.getActualValue());
        assertEquals(VOLTAGE_THRESHOLD_MAX, savedAlert.getThresholdValue());
    }

    @Test
    @DisplayName("TC-TRIG-002: 电压超限告警 - 电压低于阈值下限，生成error级别告警")
    void testVoltageBelowThreshold_GeneratesErrorAlert() {
        // Given
        DeviceAlertConfig config = createVoltageConfig(true);
        double belowThresholdValue = 180.0; // 低于阈值 200V

        when(deviceAlertConfigRepository.findByDeviceIdAndType(DEVICE_ID, "voltage")).thenReturn(config);
        when(deviceAlertRepository.findByDeviceIdAndTypeAndResolvedFalseOrderByTsDesc(DEVICE_ID, "voltage"))
                .thenReturn(Collections.emptyList());
        when(deviceAlertRepository.save(any(DeviceAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        alertService.checkAndGenerateAlert(DEVICE_ID, "voltage", belowThresholdValue);

        // Then
        ArgumentCaptor<DeviceAlert> alertCaptor = ArgumentCaptor.forClass(DeviceAlert.class);
        verify(deviceAlertRepository).save(alertCaptor.capture());

        DeviceAlert savedAlert = alertCaptor.getValue();
        assertEquals("error", savedAlert.getLevel(), "电压低于阈值应生成error级别告警");
        assertTrue(savedAlert.getMessage().contains("Voltage below threshold"));
        assertEquals(VOLTAGE_THRESHOLD_MIN, savedAlert.getThresholdValue());
    }

    @Test
    @DisplayName("TC-TRIG-002: 电压超限告警 - 电压在正常范围内，不生成告警")
    void testVoltageInNormalRange_NoAlertGenerated() {
        // Given
        DeviceAlertConfig config = createVoltageConfig(true);
        double normalValue = 220.0; // 在正常范围内

        when(deviceAlertConfigRepository.findByDeviceIdAndType(DEVICE_ID, "voltage")).thenReturn(config);
        when(deviceAlertRepository.findByDeviceIdAndTypeAndResolvedFalseOrderByTsDesc(DEVICE_ID, "voltage"))
                .thenReturn(Collections.emptyList());

        // When
        alertService.checkAndGenerateAlert(DEVICE_ID, "voltage", normalValue);

        // Then
        verify(deviceAlertRepository, never()).save(any());
    }

    // ==================== TC-TRIG-003: 电流超限告警 ====================

    @Test
    @DisplayName("TC-TRIG-003: 电流超限告警 - 电流超过阈值上限，生成warning级别告警")
    void testCurrentOverThreshold_GeneratesWarningAlert() {
        // Given
        DeviceAlertConfig config = createCurrentConfig(true);
        double overThresholdValue = 15.0; // 超过阈值 10A

        when(deviceAlertConfigRepository.findByDeviceIdAndType(DEVICE_ID, "current")).thenReturn(config);
        when(deviceAlertRepository.findByDeviceIdAndTypeAndResolvedFalseOrderByTsDesc(DEVICE_ID, "current"))
                .thenReturn(Collections.emptyList());
        when(deviceAlertRepository.save(any(DeviceAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        alertService.checkAndGenerateAlert(DEVICE_ID, "current", overThresholdValue);

        // Then
        ArgumentCaptor<DeviceAlert> alertCaptor = ArgumentCaptor.forClass(DeviceAlert.class);
        verify(deviceAlertRepository).save(alertCaptor.capture());

        DeviceAlert savedAlert = alertCaptor.getValue();
        assertEquals("current", savedAlert.getType());
        assertEquals("warning", savedAlert.getLevel(), "电流超限应生成warning级别告警");
        assertTrue(savedAlert.getMessage().contains("Current exceeds threshold"));
        assertEquals(overThresholdValue, savedAlert.getActualValue());
        assertEquals(CURRENT_THRESHOLD_MAX, savedAlert.getThresholdValue());
    }

    @Test
    @DisplayName("TC-TRIG-003: 电流超限告警 - 电流低于阈值下限，生成info级别告警")
    void testCurrentBelowThreshold_GeneratesInfoAlert() {
        // Given
        DeviceAlertConfig config = createCurrentConfig(true);
        config.setThresholdMin(1.0);
        double belowThresholdValue = 0.5;

        when(deviceAlertConfigRepository.findByDeviceIdAndType(DEVICE_ID, "current")).thenReturn(config);
        when(deviceAlertRepository.findByDeviceIdAndTypeAndResolvedFalseOrderByTsDesc(DEVICE_ID, "current"))
                .thenReturn(Collections.emptyList());
        when(deviceAlertRepository.save(any(DeviceAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        alertService.checkAndGenerateAlert(DEVICE_ID, "current", belowThresholdValue);

        // Then
        ArgumentCaptor<DeviceAlert> alertCaptor = ArgumentCaptor.forClass(DeviceAlert.class);
        verify(deviceAlertRepository).save(alertCaptor.capture());

        DeviceAlert savedAlert = alertCaptor.getValue();
        assertEquals("info", savedAlert.getLevel(), "电流低于阈值应生成info级别告警");
        assertTrue(savedAlert.getMessage().contains("Current below threshold"));
    }

    @Test
    @DisplayName("TC-TRIG-003: 电流超限告警 - 电流在正常范围内，不生成告警")
    void testCurrentInNormalRange_NoAlertGenerated() {
        // Given
        DeviceAlertConfig config = createCurrentConfig(true);
        double normalValue = 5.0; // 在正常范围内

        when(deviceAlertConfigRepository.findByDeviceIdAndType(DEVICE_ID, "current")).thenReturn(config);
        when(deviceAlertRepository.findByDeviceIdAndTypeAndResolvedFalseOrderByTsDesc(DEVICE_ID, "current"))
                .thenReturn(Collections.emptyList());

        // When
        alertService.checkAndGenerateAlert(DEVICE_ID, "current", normalValue);

        // Then
        verify(deviceAlertRepository, never()).save(any());
    }

    // ==================== TC-TRIG-004: 配置禁用 ====================

    @Test
    @DisplayName("TC-TRIG-004: 配置禁用 - enabled=false，不生成告警")
    void testConfigDisabled_NoAlertGenerated() {
        // Given
        DeviceAlertConfig config = createPowerConfig(false); // 禁用配置
        double overThresholdValue = 3000.0; // 超过阈值

        when(deviceAlertConfigRepository.findByDeviceIdAndType(DEVICE_ID, "power")).thenReturn(config);

        // When
        alertService.checkAndGenerateAlert(DEVICE_ID, "power", overThresholdValue);

        // Then
        verify(deviceAlertRepository, never()).save(any());
        verify(webSocketNotificationService, never()).broadcastAlert(anyString(), any(Map.class));
        // 不应检查重复告警
        verify(deviceAlertRepository, never()).findByDeviceIdAndTypeAndResolvedFalseOrderByTsDesc(any(), any());
    }

    @Test
    @DisplayName("TC-TRIG-004: 配置禁用 - 配置不存在，不生成告警")
    void testConfigNotFound_NoAlertGenerated() {
        // Given
        when(deviceAlertConfigRepository.findByDeviceIdAndType(DEVICE_ID, "power")).thenReturn(null);

        // When
        alertService.checkAndGenerateAlert(DEVICE_ID, "power", 3000.0);

        // Then
        verify(deviceAlertRepository, never()).save(any());
    }

    // ==================== TC-TRIG-005: 重复告警 ====================

    @Test
    @DisplayName("TC-TRIG-005: 重复告警 - 存在未解决的相同类型告警，不重复生成")
    void testDuplicateAlert_ExistingUnresolvedAlert_NoNewAlertGenerated() {
        // Given
        DeviceAlertConfig config = createPowerConfig(true);
        double overThresholdValue = 2500.0;

        // 模拟已存在未解决的告警
        DeviceAlert existingAlert = new DeviceAlert();
        existingAlert.setId("alert_existing");
        existingAlert.setDeviceId(DEVICE_ID);
        existingAlert.setType("power");
        existingAlert.setLevel("warning");
        existingAlert.setResolved(false);

        when(deviceAlertConfigRepository.findByDeviceIdAndType(DEVICE_ID, "power")).thenReturn(config);
        when(deviceAlertRepository.findByDeviceIdAndTypeAndResolvedFalseOrderByTsDesc(DEVICE_ID, "power"))
                .thenReturn(List.of(existingAlert));

        // When
        alertService.checkAndGenerateAlert(DEVICE_ID, "power", overThresholdValue);

        // Then
        verify(deviceAlertRepository, never()).save(any());
        verify(webSocketNotificationService, never()).broadcastAlert(anyString(), any(Map.class));
    }

    @Test
    @DisplayName("TC-TRIG-005: 重复告警 - 存在已解决的告警，可以生成新告警")
    void testDuplicateAlert_ExistingResolvedAlert_NewAlertGenerated() {
        // Given
        DeviceAlertConfig config = createPowerConfig(true);
        double overThresholdValue = 2500.0;

        when(deviceAlertConfigRepository.findByDeviceIdAndType(DEVICE_ID, "power")).thenReturn(config);
        // 没有未解决的告警
        when(deviceAlertRepository.findByDeviceIdAndTypeAndResolvedFalseOrderByTsDesc(DEVICE_ID, "power"))
                .thenReturn(Collections.emptyList());
        when(deviceAlertRepository.save(any(DeviceAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        alertService.checkAndGenerateAlert(DEVICE_ID, "power", overThresholdValue);

        // Then
        verify(deviceAlertRepository).save(any(DeviceAlert.class));
        verify(webSocketNotificationService).broadcastAlert(eq(DEVICE_ID), any(Map.class));
    }

    @Test
    @DisplayName("TC-TRIG-005: 重复告警 - 存在不同类型的未解决告警，可以生成新告警")
    void testDuplicateAlert_DifferentTypeAlert_NewAlertGenerated() {
        // Given
        DeviceAlertConfig config = createPowerConfig(true);
        double overThresholdValue = 2500.0;

        when(deviceAlertConfigRepository.findByDeviceIdAndType(DEVICE_ID, "power")).thenReturn(config);
        when(deviceAlertRepository.findByDeviceIdAndTypeAndResolvedFalseOrderByTsDesc(DEVICE_ID, "power"))
                .thenReturn(Collections.emptyList());
        when(deviceAlertRepository.save(any(DeviceAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        alertService.checkAndGenerateAlert(DEVICE_ID, "power", overThresholdValue);

        // Then
        verify(deviceAlertRepository).save(any(DeviceAlert.class));
    }

    // ==================== 辅助方法 ====================

    private DeviceAlertConfig createPowerConfig(boolean enabled) {
        DeviceAlertConfig config = new DeviceAlertConfig();
        config.setId("config_power_001");
        config.setDeviceId(DEVICE_ID);
        config.setType("power");
        config.setThresholdMin(POWER_THRESHOLD_MIN);
        config.setThresholdMax(POWER_THRESHOLD_MAX);
        config.setEnabled(enabled);
        config.setCreatedAt(System.currentTimeMillis() / 1000);
        config.setUpdatedAt(System.currentTimeMillis() / 1000);
        return config;
    }

    private DeviceAlertConfig createVoltageConfig(boolean enabled) {
        DeviceAlertConfig config = new DeviceAlertConfig();
        config.setId("config_voltage_001");
        config.setDeviceId(DEVICE_ID);
        config.setType("voltage");
        config.setThresholdMin(VOLTAGE_THRESHOLD_MIN);
        config.setThresholdMax(VOLTAGE_THRESHOLD_MAX);
        config.setEnabled(enabled);
        config.setCreatedAt(System.currentTimeMillis() / 1000);
        config.setUpdatedAt(System.currentTimeMillis() / 1000);
        return config;
    }

    private DeviceAlertConfig createCurrentConfig(boolean enabled) {
        DeviceAlertConfig config = new DeviceAlertConfig();
        config.setId("config_current_001");
        config.setDeviceId(DEVICE_ID);
        config.setType("current");
        config.setThresholdMin(CURRENT_THRESHOLD_MIN);
        config.setThresholdMax(CURRENT_THRESHOLD_MAX);
        config.setEnabled(enabled);
        config.setCreatedAt(System.currentTimeMillis() / 1000);
        config.setUpdatedAt(System.currentTimeMillis() / 1000);
        return config;
    }

}