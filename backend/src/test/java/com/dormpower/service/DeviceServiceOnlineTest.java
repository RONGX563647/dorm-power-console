package com.dormpower.service;

import com.dormpower.model.Device;
import com.dormpower.repository.DeviceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 设备在线检测服务单元测试
 *
 * 测试用例覆盖：
 * - TC-ONLINE-001: 设备心跳正常
 * - TC-ONLINE-002: 设备心跳超时（60秒无心跳）
 * - TC-ONLINE-003: 设备断开连接（LWT消息）
 * - TC-ONLINE-004: 设备重新上线
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class DeviceServiceOnlineTest {

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceService deviceService;

    // ==================== TC-ONLINE-001: 设备心跳正常 ====================

    @Test
    @DisplayName("TC-ONLINE-001: 设备心跳正常 - 心跳消息，设备保持在线状态")
    void testProcessHeartbeat_Success() {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, true, System.currentTimeMillis() / 1000);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Device result = deviceService.processHeartbeat(deviceId);

        // Then
        assertNotNull(result);
        assertTrue(result.isOnline());
        assertTrue(result.getLastSeenTs() > 0);
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    @DisplayName("TC-ONLINE-001: 设备心跳正常 - 离线设备收到心跳后变为在线")
    void testProcessHeartbeat_OfflineToOnline() {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, false, System.currentTimeMillis() / 1000 - 120);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Device result = deviceService.processHeartbeat(deviceId);

        // Then
        assertNotNull(result);
        assertTrue(result.isOnline());
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    @DisplayName("TC-ONLINE-001: 设备心跳正常 - 设备不存在")
    void testProcessHeartbeat_DeviceNotFound() {
        // Given
        String deviceId = "non_existent_device";
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        // When
        Device result = deviceService.processHeartbeat(deviceId);

        // Then
        assertNull(result);
        verify(deviceRepository, never()).save(any(Device.class));
    }

    // ==================== TC-ONLINE-002: 设备心跳超时 ====================

    @Test
    @DisplayName("TC-ONLINE-002: 设备心跳超时 - 60秒无心跳，设备标记为离线")
    void testCheckAndUpdateOnlineStatus_Timeout() {
        // Given - 设备标记为在线，但心跳超时（61秒前）
        String deviceId = "device_001";
        long oldTimestamp = System.currentTimeMillis() / 1000 - 61;
        Device device = createDevice(deviceId, true, oldTimestamp);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        boolean isOnline = deviceService.checkAndUpdateOnlineStatus(deviceId);

        // Then
        assertFalse(isOnline);
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    @DisplayName("TC-ONLINE-002: 设备心跳超时 - 恰好60秒边界测试")
    void testCheckAndUpdateOnlineStatus_Exactly60Seconds() {
        // Given - 设备标记为在线，心跳恰好60秒前（边界情况）
        String deviceId = "device_001";
        long timestamp = System.currentTimeMillis() / 1000 - 60;
        Device device = createDevice(deviceId, true, timestamp);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        // When
        boolean isOnline = deviceService.checkAndUpdateOnlineStatus(deviceId);

        // Then - 60秒边界，应该离线（因为条件是 < OFFLINE_THRESHOLD_SECONDS）
        assertFalse(isOnline);
    }

    @Test
    @DisplayName("TC-ONLINE-002: 设备心跳超时 - 59秒前有心跳，仍然在线")
    void testCheckAndUpdateOnlineStatus_WithinTimeout() {
        // Given - 设备标记为在线，心跳59秒前
        String deviceId = "device_001";
        long timestamp = System.currentTimeMillis() / 1000 - 59;
        Device device = createDevice(deviceId, true, timestamp);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        // When
        boolean isOnline = deviceService.checkAndUpdateOnlineStatus(deviceId);

        // Then
        assertTrue(isOnline);
        // 状态未变化，不需要保存
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    @DisplayName("TC-ONLINE-002: 设备心跳超时 - 设备不存在")
    void testCheckAndUpdateOnlineStatus_DeviceNotFound() {
        // Given
        String deviceId = "non_existent_device";
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        // When
        boolean isOnline = deviceService.checkAndUpdateOnlineStatus(deviceId);

        // Then
        assertFalse(isOnline);
    }

    // ==================== TC-ONLINE-003: 设备断开连接（LWT消息） ====================

    @Test
    @DisplayName("TC-ONLINE-003: 设备断开连接 - LWT消息，设备立即标记为离线")
    void testMarkDeviceOffline_Success() {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, true, System.currentTimeMillis() / 1000);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        boolean result = deviceService.markDeviceOffline(deviceId);

        // Then
        assertTrue(result);
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    @DisplayName("TC-ONLINE-003: 设备断开连接 - 离线设备再次标记离线")
    void testMarkDeviceOffline_AlreadyOffline() {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId, false, System.currentTimeMillis() / 1000 - 30);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        boolean result = deviceService.markDeviceOffline(deviceId);

        // Then
        assertTrue(result);
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    @DisplayName("TC-ONLINE-003: 设备断开连接 - 设备不存在")
    void testMarkDeviceOffline_DeviceNotFound() {
        // Given
        String deviceId = "non_existent_device";
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        // When
        boolean result = deviceService.markDeviceOffline(deviceId);

        // Then
        assertFalse(result);
        verify(deviceRepository, never()).save(any(Device.class));
    }

    // ==================== TC-ONLINE-004: 设备重新上线 ====================

    @Test
    @DisplayName("TC-ONLINE-004: 设备重新上线 - 心跳恢复，设备标记为在线")
    void testDeviceBackOnline() {
        // Given - 设备离线，然后收到心跳
        String deviceId = "device_001";
        Device device = createDevice(deviceId, false, System.currentTimeMillis() / 1000 - 120);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When - 处理心跳
        Device result = deviceService.processHeartbeat(deviceId);

        // Then
        assertNotNull(result);
        assertTrue(result.isOnline());
        assertTrue(result.getLastSeenTs() > System.currentTimeMillis() / 1000 - 10);
    }

    @Test
    @DisplayName("TC-ONLINE-004: 设备重新上线 - 检查isDeviceOnline方法")
    void testIsDeviceOnline_BackOnline() {
        // Given - 设备刚收到心跳
        String deviceId = "device_001";
        Device device = createDevice(deviceId, true, System.currentTimeMillis() / 1000);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        // When
        boolean isOnline = deviceService.isDeviceOnline(deviceId);

        // Then
        assertTrue(isOnline);
    }

    @Test
    @DisplayName("TC-ONLINE-004: 设备重新上线 - 离线设备心跳超时")
    void testIsDeviceOnline_Timeout() {
        // Given - 设备心跳超时
        String deviceId = "device_001";
        long oldTimestamp = System.currentTimeMillis() / 1000 - 70;
        Device device = createDevice(deviceId, true, oldTimestamp);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        // When
        boolean isOnline = deviceService.isDeviceOnline(deviceId);

        // Then - 虽然数据库中online=true，但心跳超时，应该返回false
        assertFalse(isOnline);
    }

    @Test
    @DisplayName("TC-ONLINE-004: 设备重新上线 - 设备不存在")
    void testIsDeviceOnline_DeviceNotFound() {
        // Given
        String deviceId = "non_existent_device";
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        // When
        boolean isOnline = deviceService.isDeviceOnline(deviceId);

        // Then
        assertFalse(isOnline);
    }

    // ==================== 辅助方法 ====================

    private Device createDevice(String deviceId, boolean online, long lastSeenTs) {
        Device device = new Device();
        device.setId(deviceId);
        device.setName("测试设备-" + deviceId);
        device.setRoom("A1-301");
        device.setOnline(online);
        device.setLastSeenTs(lastSeenTs);
        device.setCreatedAt(System.currentTimeMillis() / 1000 - 3600);
        return device;
    }

}