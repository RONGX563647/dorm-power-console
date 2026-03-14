package com.dormpower.service;

import com.dormpower.exception.BusinessException;
import com.dormpower.exception.ResourceNotFoundException;
import com.dormpower.model.CommandRecord;
import com.dormpower.model.Device;
import com.dormpower.repository.CommandRecordRepository;
import com.dormpower.repository.DeviceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 命令冲突检测单元测试
 *
 * 测试用例覆盖：
 * - TC-CONF-001: 无冲突命令
 * - TC-CONF-002: 全局命令冲突
 * - TC-CONF-003: 插座命令冲突
 * - TC-CONF-004: 不同插座无冲突
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class CommandServiceConflictTest {

    @Mock
    private CommandRecordRepository commandRecordRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CommandService commandService;

    // ==================== TC-CONF-001: 无冲突命令 ====================

    @Test
    @DisplayName("TC-CONF-001: 无冲突命令 - 无待处理命令，正常创建")
    void testHasPendingConflict_NoConflict_NoPendingCommands() {
        // Given - 没有待处理的命令
        String deviceId = "device_001";
        Integer socket = 1;
        when(commandRecordRepository.findByDeviceIdAndState(deviceId, "pending"))
                .thenReturn(Collections.emptyList());

        // When
        boolean hasConflict = commandService.hasPendingConflict(deviceId, socket);

        // Then
        assertFalse(hasConflict);
        verify(commandRecordRepository).findByDeviceIdAndState(deviceId, "pending");
    }

    @Test
    @DisplayName("TC-CONF-001: 无冲突命令 - 发送命令成功")
    void testSendCommand_NoConflict_Success() {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId);
        Map<String, Object> request = Map.of("action", "toggle", "socket", 1);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(commandRecordRepository.findByDeviceIdAndState(deviceId, "pending"))
                .thenReturn(Collections.emptyList());
        when(commandRecordRepository.save(any(CommandRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Map<String, Object> result = commandService.sendCommand(deviceId, request);

        // Then
        assertNotNull(result);
        assertTrue((Boolean) result.get("ok"));
        assertNotNull(result.get("cmdId"));
        verify(commandRecordRepository).save(any(CommandRecord.class));
    }

    // ==================== TC-CONF-002: 全局命令冲突 ====================

    @Test
    @DisplayName("TC-CONF-002: 全局命令冲突 - 已有待处理全局命令")
    void testHasPendingConflict_GlobalCommandConflict() {
        // Given - 已有一个待处理的全局命令（socket=null）
        String deviceId = "device_001";
        Integer newSocket = null; // 新命令也是全局命令
        CommandRecord existingCmd = createCommandRecord(deviceId, null); // 全局命令
        when(commandRecordRepository.findByDeviceIdAndState(deviceId, "pending"))
                .thenReturn(List.of(existingCmd));

        // When
        boolean hasConflict = commandService.hasPendingConflict(deviceId, newSocket);

        // Then
        assertTrue(hasConflict);
    }

    @Test
    @DisplayName("TC-CONF-002: 全局命令冲突 - 返回409冲突")
    void testSendCommand_GlobalConflict_ThrowsBusinessException() {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId);
        Map<String, Object> request = Map.of("action", "toggle"); // 全局命令

        CommandRecord existingCmd = createCommandRecord(deviceId, null); // 已有全局命令
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(commandRecordRepository.findByDeviceIdAndState(deviceId, "pending"))
                .thenReturn(List.of(existingCmd));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commandService.sendCommand(deviceId, request)
        );

        assertTrue(exception.getMessage().contains("Pending command exists"));
    }

    @Test
    @DisplayName("TC-CONF-002: 全局命令冲突 - 插座命令与全局命令无冲突")
    void testHasPendingConflict_SocketVsGlobal_NoConflict() {
        // Given - 已有一个待处理的全局命令
        String deviceId = "device_001";
        Integer newSocket = 1; // 新命令是插座命令
        CommandRecord existingCmd = createCommandRecord(deviceId, null); // 全局命令
        when(commandRecordRepository.findByDeviceIdAndState(deviceId, "pending"))
                .thenReturn(List.of(existingCmd));

        // When
        boolean hasConflict = commandService.hasPendingConflict(deviceId, newSocket);

        // Then - 插座命令与全局命令不冲突
        assertFalse(hasConflict);
    }

    // ==================== TC-CONF-003: 插座命令冲突 ====================

    @Test
    @DisplayName("TC-CONF-003: 插座命令冲突 - 已有待处理同插座命令")
    void testHasPendingConflict_SocketCommandConflict() {
        // Given - 已有一个待处理的插座1命令
        String deviceId = "device_001";
        Integer newSocket = 1; // 新命令也是插座1
        CommandRecord existingCmd = createCommandRecord(deviceId, 1); // 插座1命令
        when(commandRecordRepository.findByDeviceIdAndState(deviceId, "pending"))
                .thenReturn(List.of(existingCmd));

        // When
        boolean hasConflict = commandService.hasPendingConflict(deviceId, newSocket);

        // Then
        assertTrue(hasConflict);
    }

    @Test
    @DisplayName("TC-CONF-003: 插座命令冲突 - 返回409冲突")
    void testSendCommand_SocketConflict_ThrowsBusinessException() {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId);
        Map<String, Object> request = Map.of("action", "toggle", "socket", 1);

        CommandRecord existingCmd = createCommandRecord(deviceId, 1); // 已有插座1命令
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(commandRecordRepository.findByDeviceIdAndState(deviceId, "pending"))
                .thenReturn(List.of(existingCmd));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> commandService.sendCommand(deviceId, request)
        );

        assertTrue(exception.getMessage().contains("Pending command exists"));
    }

    // ==================== TC-CONF-004: 不同插座无冲突 ====================

    @Test
    @DisplayName("TC-CONF-004: 不同插座无冲突 - 正常创建命令")
    void testHasPendingConflict_DifferentSocket_NoConflict() {
        // Given - 已有一个待处理的插座1命令
        String deviceId = "device_001";
        Integer newSocket = 2; // 新命令是插座2
        CommandRecord existingCmd = createCommandRecord(deviceId, 1); // 插座1命令
        when(commandRecordRepository.findByDeviceIdAndState(deviceId, "pending"))
                .thenReturn(List.of(existingCmd));

        // When
        boolean hasConflict = commandService.hasPendingConflict(deviceId, newSocket);

        // Then - 不同插座不冲突
        assertFalse(hasConflict);
    }

    @Test
    @DisplayName("TC-CONF-004: 不同插座无冲突 - 发送命令成功")
    void testSendCommand_DifferentSocket_Success() {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId);
        Map<String, Object> request = Map.of("action", "toggle", "socket", 2);

        CommandRecord existingCmd = createCommandRecord(deviceId, 1); // 已有插座1命令
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(commandRecordRepository.findByDeviceIdAndState(deviceId, "pending"))
                .thenReturn(List.of(existingCmd));
        when(commandRecordRepository.save(any(CommandRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Map<String, Object> result = commandService.sendCommand(deviceId, request);

        // Then
        assertNotNull(result);
        assertTrue((Boolean) result.get("ok"));
        assertNotNull(result.get("cmdId"));
        verify(commandRecordRepository).save(any(CommandRecord.class));
    }

    // ==================== 其他边界测试 ====================

    @Test
    @DisplayName("边界测试: 多个待处理命令，其中一个冲突")
    void testHasPendingConflict_MultiplePending_OneConflict() {
        // Given - 有多个待处理命令
        String deviceId = "device_001";
        Integer newSocket = 2;
        CommandRecord cmd1 = createCommandRecord(deviceId, 1); // 插座1
        CommandRecord cmd2 = createCommandRecord(deviceId, 2); // 插座2 - 冲突
        CommandRecord cmd3 = createCommandRecord(deviceId, 3); // 插座3
        when(commandRecordRepository.findByDeviceIdAndState(deviceId, "pending"))
                .thenReturn(List.of(cmd1, cmd2, cmd3));

        // When
        boolean hasConflict = commandService.hasPendingConflict(deviceId, newSocket);

        // Then
        assertTrue(hasConflict);
    }

    @Test
    @DisplayName("边界测试: 设备不存在")
    void testSendCommand_DeviceNotFound() {
        // Given
        String deviceId = "nonexistent_device";
        Map<String, Object> request = Map.of("action", "toggle");

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
                ResourceNotFoundException.class,
                () -> commandService.sendCommand(deviceId, request)
        );
    }

    @Test
    @DisplayName("边界测试: 全局命令与新全局命令冲突")
    void testHasPendingConflict_GlobalVsGlobal_Conflict() {
        // Given - 已有全局命令，新命令也是全局命令
        String deviceId = "device_001";
        CommandRecord existingGlobal = createCommandRecord(deviceId, null);
        when(commandRecordRepository.findByDeviceIdAndState(deviceId, "pending"))
                .thenReturn(List.of(existingGlobal));

        // When
        boolean hasConflict = commandService.hasPendingConflict(deviceId, null);

        // Then
        assertTrue(hasConflict);
    }

    @Test
    @DisplayName("边界测试: 插座命令与全局命令不冲突")
    void testSendCommand_SocketVsGlobal_Success() {
        // Given
        String deviceId = "device_001";
        Device device = createDevice(deviceId);
        Map<String, Object> request = Map.of("action", "toggle", "socket", 1);

        CommandRecord existingGlobal = createCommandRecord(deviceId, null); // 已有全局命令
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(commandRecordRepository.findByDeviceIdAndState(deviceId, "pending"))
                .thenReturn(List.of(existingGlobal));
        when(commandRecordRepository.save(any(CommandRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Map<String, Object> result = commandService.sendCommand(deviceId, request);

        // Then - 插座命令与全局命令不冲突，可以发送
        assertNotNull(result);
        assertTrue((Boolean) result.get("ok"));
    }

    // ==================== 辅助方法 ====================

    private Device createDevice(String deviceId) {
        Device device = new Device();
        device.setId(deviceId);
        device.setName("测试设备-" + deviceId);
        device.setRoom("A1-301");
        device.setOnline(true);
        device.setLastSeenTs(System.currentTimeMillis() / 1000);
        device.setCreatedAt(System.currentTimeMillis() / 1000 - 3600);
        return device;
    }

    private CommandRecord createCommandRecord(String deviceId, Integer socket) {
        long now = System.currentTimeMillis();
        CommandRecord record = new CommandRecord();
        record.setCmdId("cmd-" + now);
        record.setDeviceId(deviceId);
        record.setSocket(socket);
        record.setAction("toggle");
        record.setPayloadJson("{\"action\":\"toggle\"}");
        record.setState("pending");
        record.setMessage("");
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        record.setExpiresAt(now + 30000);
        return record;
    }

}