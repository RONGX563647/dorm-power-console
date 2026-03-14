package com.dormpower.service;

import com.dormpower.model.CommandRecord;
import com.dormpower.repository.CommandRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 命令状态查询单元测试
 *
 * 测试用例覆盖：
 * - TC-STATUS-001: 查询pending状态命令
 * - TC-STATUS-002: 查询success状态命令
 * - TC-STATUS-003: 查询不存在命令
 * - TC-STATUS-004: 查询timeout状态命令
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class CommandServiceStatusTest {

    @Mock
    private CommandRecordRepository commandRecordRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CommandService commandService;

    // ==================== TC-STATUS-001: 查询pending状态命令 ====================

    @Test
    @DisplayName("TC-STATUS-001: 查询pending状态命令 - 返回state=pending")
    void testGetCommandStatus_Pending() {
        // Given
        String cmdId = "cmd-1700000000000";
        CommandRecord commandRecord = createCommandRecord(cmdId, "pending", null);
        when(commandRecordRepository.findById(cmdId)).thenReturn(Optional.of(commandRecord));

        // When
        Map<String, Object> status = commandService.getCommandStatus(cmdId);

        // Then
        assertNotNull(status);
        assertEquals(cmdId, status.get("cmdId"));
        assertEquals("pending", status.get("state"));
        assertNull(status.get("durationMs")); // pending状态没有durationMs
        verify(commandRecordRepository).findById(cmdId);
    }

    @Test
    @DisplayName("TC-STATUS-001: 查询pending状态命令 - 包含message字段")
    void testGetCommandStatus_Pending_WithMessage() {
        // Given
        String cmdId = "cmd-1700000000001";
        CommandRecord commandRecord = createCommandRecord(cmdId, "pending", null);
        commandRecord.setMessage("Waiting for device ack");
        when(commandRecordRepository.findById(cmdId)).thenReturn(Optional.of(commandRecord));

        // When
        Map<String, Object> status = commandService.getCommandStatus(cmdId);

        // Then
        assertNotNull(status);
        assertEquals("pending", status.get("state"));
        assertEquals("Waiting for device ack", status.get("message"));
    }

    // ==================== TC-STATUS-002: 查询success状态命令 ====================

    @Test
    @DisplayName("TC-STATUS-002: 查询success状态命令 - 返回state=success和durationMs")
    void testGetCommandStatus_Success() {
        // Given
        String cmdId = "cmd-1700000000002";
        CommandRecord commandRecord = createCommandRecord(cmdId, "success", 150);
        when(commandRecordRepository.findById(cmdId)).thenReturn(Optional.of(commandRecord));

        // When
        Map<String, Object> status = commandService.getCommandStatus(cmdId);

        // Then
        assertNotNull(status);
        assertEquals(cmdId, status.get("cmdId"));
        assertEquals("success", status.get("state"));
        assertEquals(150, status.get("durationMs"));
        verify(commandRecordRepository).findById(cmdId);
    }

    @Test
    @DisplayName("TC-STATUS-002: 查询success状态命令 - 包含执行成功消息")
    void testGetCommandStatus_Success_WithMessage() {
        // Given
        String cmdId = "cmd-1700000000003";
        CommandRecord commandRecord = createCommandRecord(cmdId, "success", 200);
        commandRecord.setMessage("Command executed successfully");
        when(commandRecordRepository.findById(cmdId)).thenReturn(Optional.of(commandRecord));

        // When
        Map<String, Object> status = commandService.getCommandStatus(cmdId);

        // Then
        assertNotNull(status);
        assertEquals("success", status.get("state"));
        assertEquals(200, status.get("durationMs"));
        assertEquals("Command executed successfully", status.get("message"));
    }

    @Test
    @DisplayName("TC-STATUS-002: 查询failed状态命令 - 返回state=failed和durationMs")
    void testGetCommandStatus_Failed() {
        // Given
        String cmdId = "cmd-1700000000004";
        CommandRecord commandRecord = createCommandRecord(cmdId, "failed", 80);
        commandRecord.setMessage("Device returned error");
        when(commandRecordRepository.findById(cmdId)).thenReturn(Optional.of(commandRecord));

        // When
        Map<String, Object> status = commandService.getCommandStatus(cmdId);

        // Then
        assertNotNull(status);
        assertEquals("failed", status.get("state"));
        assertEquals(80, status.get("durationMs"));
        assertEquals("Device returned error", status.get("message"));
    }

    // ==================== TC-STATUS-003: 查询不存在命令 ====================

    @Test
    @DisplayName("TC-STATUS-003: 查询不存在命令 - 返回null")
    void testGetCommandStatus_NotFound() {
        // Given
        String cmdId = "cmd-nonexistent";
        when(commandRecordRepository.findById(cmdId)).thenReturn(Optional.empty());

        // When
        Map<String, Object> status = commandService.getCommandStatus(cmdId);

        // Then
        assertNull(status);
        verify(commandRecordRepository).findById(cmdId);
    }

    @Test
    @DisplayName("TC-STATUS-003: 查询不存在命令 - 空cmdId")
    void testGetCommandStatus_EmptyCmdId() {
        // Given
        String cmdId = "";
        when(commandRecordRepository.findById(cmdId)).thenReturn(Optional.empty());

        // When
        Map<String, Object> status = commandService.getCommandStatus(cmdId);

        // Then
        assertNull(status);
    }

    // ==================== TC-STATUS-004: 查询timeout状态命令 ====================

    @Test
    @DisplayName("TC-STATUS-004: 查询timeout状态命令 - 返回state=timeout")
    void testGetCommandStatus_Timeout() {
        // Given
        String cmdId = "cmd-1700000000005";
        CommandRecord commandRecord = createCommandRecord(cmdId, "timeout", null);
        commandRecord.setMessage("Command timed out");
        when(commandRecordRepository.findById(cmdId)).thenReturn(Optional.of(commandRecord));

        // When
        Map<String, Object> status = commandService.getCommandStatus(cmdId);

        // Then
        assertNotNull(status);
        assertEquals(cmdId, status.get("cmdId"));
        assertEquals("timeout", status.get("state"));
        assertEquals("Command timed out", status.get("message"));
        assertNull(status.get("durationMs")); // timeout状态通常没有durationMs
        verify(commandRecordRepository).findById(cmdId);
    }

    @Test
    @DisplayName("TC-STATUS-004: 查询timeout状态命令 - 超时消息验证")
    void testGetCommandStatus_Timeout_Message() {
        // Given
        String cmdId = "cmd-1700000000006";
        CommandRecord commandRecord = createCommandRecord(cmdId, "timeout", null);
        commandRecord.setMessage("Command timed out after 30 seconds");
        commandRecord.setExpiresAt(System.currentTimeMillis() - 1000); // 已过期
        when(commandRecordRepository.findById(cmdId)).thenReturn(Optional.of(commandRecord));

        // When
        Map<String, Object> status = commandService.getCommandStatus(cmdId);

        // Then
        assertNotNull(status);
        assertEquals("timeout", status.get("state"));
        assertTrue(status.get("message").toString().contains("timed out"));
    }

    // ==================== 其他边界测试 ====================

    @Test
    @DisplayName("状态查询 - 验证返回的字段完整性")
    void testGetCommandStatus_AllFields() {
        // Given
        String cmdId = "cmd-1700000000007";
        long now = System.currentTimeMillis();
        CommandRecord commandRecord = new CommandRecord();
        commandRecord.setCmdId(cmdId);
        commandRecord.setDeviceId("device_001");
        commandRecord.setAction("toggle");
        commandRecord.setState("success");
        commandRecord.setMessage("OK");
        commandRecord.setCreatedAt(now - 100);
        commandRecord.setUpdatedAt(now);
        commandRecord.setExpiresAt(now + 30000);
        commandRecord.setDurationMs(100);
        when(commandRecordRepository.findById(cmdId)).thenReturn(Optional.of(commandRecord));

        // When
        Map<String, Object> status = commandService.getCommandStatus(cmdId);

        // Then
        assertNotNull(status);
        assertEquals(cmdId, status.get("cmdId"));
        assertEquals("success", status.get("state"));
        assertEquals("OK", status.get("message"));
        assertEquals(now, status.get("updatedAt"));
        assertEquals(100, status.get("durationMs"));
    }

    @Test
    @DisplayName("状态查询 - durationMs为null时不包含在返回中")
    void testGetCommandStatus_NoDurationMs() {
        // Given
        String cmdId = "cmd-1700000000008";
        CommandRecord commandRecord = createCommandRecord(cmdId, "pending", null);
        when(commandRecordRepository.findById(cmdId)).thenReturn(Optional.of(commandRecord));

        // When
        Map<String, Object> status = commandService.getCommandStatus(cmdId);

        // Then
        assertNotNull(status);
        assertNull(status.get("durationMs"));
    }

    // ==================== 辅助方法 ====================

    private CommandRecord createCommandRecord(String cmdId, String state, Integer durationMs) {
        long now = System.currentTimeMillis();
        CommandRecord record = new CommandRecord();
        record.setCmdId(cmdId);
        record.setDeviceId("device_001");
        record.setAction("toggle");
        record.setPayloadJson("{\"action\":\"toggle\"}");
        record.setState(state);
        record.setMessage("");
        record.setCreatedAt(now - 1000);
        record.setUpdatedAt(now);
        record.setExpiresAt(now + 30000);
        record.setDurationMs(durationMs);
        return record;
    }

}