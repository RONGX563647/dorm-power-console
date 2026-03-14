package com.dormpower.service;

import com.dormpower.model.Device;
import com.dormpower.model.DeviceStatusHistory;
import com.dormpower.model.StripStatus;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.DeviceStatusHistoryRepository;
import com.dormpower.repository.StripStatusRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 设备状态上报单元测试
 *
 * 测试用例覆盖：
 * - TC-STATUS-001: 正常状态上报
 * - TC-STATUS-002: 部分字段缺失
 * - TC-STATUS-003: Kafka不可用降级
 * - TC-STATUS-004: 设备离线上报
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class DeviceStatusServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private StripStatusRepository stripStatusRepository;

    @Mock
    private DeviceStatusHistoryRepository deviceStatusHistoryRepository;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DeviceStatusService deviceStatusService;

    // ==================== TC-STATUS-001: 正常状态上报 ====================

    @Test
    @DisplayName("TC-STATUS-001: 正常状态上报 - 状态存储成功，WebSocket推送")
    void testProcessStatusReport_Success() throws Exception {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, true);
        String json = "{\"online\":true,\"total_power_w\":100.5,\"voltage_v\":220.0,\"current_a\":0.45,\"sockets\":[{\"id\":1,\"on\":true}]}";
        JsonNode payload = objectMapper.readTree(json);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(stripStatusRepository.findByDeviceId(deviceId)).thenReturn(null);
        when(stripStatusRepository.save(any(StripStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceStatusHistoryRepository.save(any(DeviceStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        StripStatus result = deviceStatusService.processStatusReport(deviceId, payload);

        // Then
        assertNotNull(result);
        assertTrue(result.isOnline());
        assertEquals(100.5, result.getTotalPowerW());
        assertEquals(220.0, result.getVoltageV());
        assertEquals(0.45, result.getCurrentA());
        assertNotNull(result.getSocketsJson());
        assertTrue(result.getSocketsJson().length() > 2); // 不是空数组

        verify(deviceRepository).save(any(Device.class));
        verify(stripStatusRepository).save(any(StripStatus.class));
        verify(deviceStatusHistoryRepository).save(any(DeviceStatusHistory.class));
        verify(webSocketNotificationService).broadcastDeviceStatus(eq(deviceId), any(JsonNode.class));
    }

    @Test
    @DisplayName("TC-STATUS-001: 正常状态上报 - 更新现有插座状态")
    void testProcessStatusReport_UpdateExisting() throws Exception {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, true);
        StripStatus existingStatus = new StripStatus();
        existingStatus.setDeviceId(deviceId);
        existingStatus.setOnline(false);
        existingStatus.setTotalPowerW(50.0);

        String json = "{\"online\":true,\"total_power_w\":150.0,\"voltage_v\":215.0,\"current_a\":0.7}";
        JsonNode payload = objectMapper.readTree(json);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(stripStatusRepository.findByDeviceId(deviceId)).thenReturn(existingStatus);
        when(stripStatusRepository.save(any(StripStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceStatusHistoryRepository.save(any(DeviceStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        StripStatus result = deviceStatusService.processStatusReport(deviceId, payload);

        // Then
        assertNotNull(result);
        assertTrue(result.isOnline());
        assertEquals(150.0, result.getTotalPowerW());
        assertEquals(215.0, result.getVoltageV());
    }

    // ==================== TC-STATUS-002: 部分字段缺失 ====================

    @Test
    @DisplayName("TC-STATUS-002: 部分字段缺失 - 缺少voltage_v，使用默认值220V")
    void testProcessStatusReport_MissingVoltage_DefaultValue() throws Exception {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, true);
        String json = "{\"online\":true,\"total_power_w\":100.0,\"current_a\":0.5}";
        JsonNode payload = objectMapper.readTree(json);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(stripStatusRepository.findByDeviceId(deviceId)).thenReturn(null);
        when(stripStatusRepository.save(any(StripStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceStatusHistoryRepository.save(any(DeviceStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        StripStatus result = deviceStatusService.processStatusReport(deviceId, payload);

        // Then
        assertNotNull(result);
        assertEquals(DeviceStatusService.DEFAULT_VOLTAGE_V, result.getVoltageV());
        assertEquals(220.0, result.getVoltageV());
    }

    @Test
    @DisplayName("TC-STATUS-002: 部分字段缺失 - 缺少current_a")
    void testProcessStatusReport_MissingCurrent_DefaultValue() throws Exception {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, true);
        String json = "{\"online\":true,\"total_power_w\":100.0,\"voltage_v\":220.0}";
        JsonNode payload = objectMapper.readTree(json);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(stripStatusRepository.findByDeviceId(deviceId)).thenReturn(null);
        when(stripStatusRepository.save(any(StripStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceStatusHistoryRepository.save(any(DeviceStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        StripStatus result = deviceStatusService.processStatusReport(deviceId, payload);

        // Then
        assertEquals(DeviceStatusService.DEFAULT_CURRENT_A, result.getCurrentA());
        assertEquals(0.0, result.getCurrentA());
    }

    @Test
    @DisplayName("TC-STATUS-002: 部分字段缺失 - 缺少total_power_w")
    void testProcessStatusReport_MissingPower_DefaultValue() throws Exception {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, true);
        String json = "{\"online\":true,\"voltage_v\":220.0,\"current_a\":0.5}";
        JsonNode payload = objectMapper.readTree(json);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(stripStatusRepository.findByDeviceId(deviceId)).thenReturn(null);
        when(stripStatusRepository.save(any(StripStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceStatusHistoryRepository.save(any(DeviceStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        StripStatus result = deviceStatusService.processStatusReport(deviceId, payload);

        // Then
        assertEquals(DeviceStatusService.DEFAULT_POWER_W, result.getTotalPowerW());
        assertEquals(0.0, result.getTotalPowerW());
    }

    @Test
    @DisplayName("TC-STATUS-002: 部分字段缺失 - 缺少online字段，默认true")
    void testProcessStatusReport_MissingOnline_DefaultTrue() throws Exception {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, false);
        String json = "{\"total_power_w\":100.0,\"voltage_v\":220.0,\"current_a\":0.5}";
        JsonNode payload = objectMapper.readTree(json);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(stripStatusRepository.findByDeviceId(deviceId)).thenReturn(null);
        when(stripStatusRepository.save(any(StripStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceStatusHistoryRepository.save(any(DeviceStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        StripStatus result = deviceStatusService.processStatusReport(deviceId, payload);

        // Then
        assertTrue(result.isOnline(), "缺少online字段时应默认为true");
    }

    @Test
    @DisplayName("TC-STATUS-002: 部分字段缺失 - 缺少sockets字段")
    void testProcessStatusReport_MissingSockets_DefaultEmptyArray() throws Exception {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, true);
        String json = "{\"online\":true,\"voltage_v\":220.0}";
        JsonNode payload = objectMapper.readTree(json);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(stripStatusRepository.findByDeviceId(deviceId)).thenReturn(null);
        when(stripStatusRepository.save(any(StripStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceStatusHistoryRepository.save(any(DeviceStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        StripStatus result = deviceStatusService.processStatusReport(deviceId, payload);

        // Then
        assertEquals(DeviceStatusService.DEFAULT_SOCKETS_JSON, result.getSocketsJson());
    }

    // ==================== TC-STATUS-003: Kafka不可用降级 ====================

    @Test
    @DisplayName("TC-STATUS-003: Kafka不可用 - 降级直接处理")
    void testProcessStatusDirect_KafkaFallback_Success() throws Exception {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, false);
        String json = "{\"online\":true,\"total_power_w\":100.0,\"voltage_v\":220.0,\"current_a\":0.5}";
        JsonNode payload = objectMapper.readTree(json);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(stripStatusRepository.save(any(StripStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceStatusHistoryRepository.save(any(DeviceStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // When - 直接调用 processStatusDirect（模拟 Kafka 不可用时的降级路径）
        StripStatus result = deviceStatusService.processStatusDirect(
                deviceId, true, 100.0, 220.0, 0.5, "[]", payload);

        // Then
        assertNotNull(result);
        assertTrue(result.isOnline());
        assertEquals(100.0, result.getTotalPowerW());
        verify(webSocketNotificationService).broadcastDeviceStatus(eq(deviceId), any(JsonNode.class));
    }

    @Test
    @DisplayName("TC-STATUS-003: Kafka不可用 - 空payload时不发送WebSocket通知")
    void testProcessStatusDirect_NullPayload_NoWebSocket() {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, true);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(stripStatusRepository.save(any(StripStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceStatusHistoryRepository.save(any(DeviceStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        StripStatus result = deviceStatusService.processStatusDirect(
                deviceId, true, 100.0, 220.0, 0.5, "[]", null);

        // Then
        assertNotNull(result);
        verify(webSocketNotificationService, never()).broadcastDeviceStatus(any(), any());
    }

    // ==================== TC-STATUS-004: 设备离线上报 ====================

    @Test
    @DisplayName("TC-STATUS-004: 设备离线上报 - online=false，更新设备离线状态")
    void testProcessStatusReport_DeviceOffline() throws Exception {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, true); // 当前在线
        String json = "{\"online\":false,\"total_power_w\":0,\"voltage_v\":220.0,\"current_a\":0}";
        JsonNode payload = objectMapper.readTree(json);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(stripStatusRepository.findByDeviceId(deviceId)).thenReturn(null);
        when(stripStatusRepository.save(any(StripStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceStatusHistoryRepository.save(any(DeviceStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);

        // When
        StripStatus result = deviceStatusService.processStatusReport(deviceId, payload);

        // Then
        assertNotNull(result);
        assertFalse(result.isOnline(), "插座状态应为离线");

        verify(deviceRepository).save(deviceCaptor.capture());
        Device savedDevice = deviceCaptor.getValue();
        assertFalse(savedDevice.isOnline(), "设备状态应更新为离线");
    }

    @Test
    @DisplayName("TC-STATUS-004: 设备离线上报 - 专用离线处理方法")
    void testProcessOfflineReport_Success() {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, true);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        boolean result = deviceStatusService.processOfflineReport(deviceId);

        // Then
        assertTrue(result);
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    @DisplayName("TC-STATUS-004: 设备离线上报 - 设备不存在")
    void testProcessOfflineReport_DeviceNotFound() {
        // Given
        String deviceId = "nonexistent_device";
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        // When
        boolean result = deviceStatusService.processOfflineReport(deviceId);

        // Then
        assertFalse(result);
        verify(deviceRepository, never()).save(any(Device.class));
    }

    // ==================== 其他边界测试 ====================

    @Test
    @DisplayName("边界测试: 自定义WebSocket回调")
    void testProcessStatusReport_CustomCallback() throws Exception {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, true);
        String json = "{\"online\":true}";
        JsonNode payload = objectMapper.readTree(json);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(stripStatusRepository.save(any(StripStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceStatusHistoryRepository.save(any(DeviceStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // 自定义回调
        StringBuilder callbackInvoked = new StringBuilder();
        Consumer<JsonNode> callback = node -> callbackInvoked.append("called");

        // When
        deviceStatusService.processStatusReport(deviceId, payload, callback);

        // Then - 应使用自定义回调而不是默认的WebSocket通知
        assertEquals("called", callbackInvoked.toString());
        verify(webSocketNotificationService, never()).broadcastDeviceStatus(any(), any());
    }

    @Test
    @DisplayName("边界测试: 状态历史记录")
    void testProcessStatusReport_HistorySaved() throws Exception {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, true);
        String json = "{\"online\":true,\"total_power_w\":100.0,\"voltage_v\":220.0,\"current_a\":0.5}";
        JsonNode payload = objectMapper.readTree(json);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(stripStatusRepository.save(any(StripStatus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deviceStatusHistoryRepository.save(any(DeviceStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        deviceStatusService.processStatusReport(deviceId, payload);

        // Then - 验证历史记录被保存
        ArgumentCaptor<DeviceStatusHistory> historyCaptor = ArgumentCaptor.forClass(DeviceStatusHistory.class);
        verify(deviceStatusHistoryRepository).save(historyCaptor.capture());

        DeviceStatusHistory history = historyCaptor.getValue();
        assertEquals(deviceId, history.getDeviceId());
        assertTrue(history.isOnline());
        assertEquals(100.0, history.getTotalPowerW());
    }

    // ==================== 辅助方法 ====================

    private Device createDevice(String deviceId, boolean online) {
        Device device = new Device();
        device.setId(deviceId);
        device.setName("测试设备-" + deviceId);
        device.setRoom("A1-301");
        device.setOnline(online);
        device.setLastSeenTs(System.currentTimeMillis() / 1000);
        device.setCreatedAt(System.currentTimeMillis() / 1000 - 3600);
        return device;
    }

}