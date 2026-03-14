package com.dormpower.service;

import com.dormpower.model.CommandRecord;
import com.dormpower.repository.CommandRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 命令历史查询服务单元测试
 *
 * 测试用例覆盖：
 * - TC-HIST-001: 查询设备命令历史
 * - TC-HIST-002: 查询无命令设备
 * - TC-HIST-003: 查询命令详情
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class CommandServiceHistoryTest {

    @Mock
    private CommandRecordRepository commandRecordRepository;

    private CommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new CommandService();
        org.springframework.test.util.ReflectionTestUtils.setField(commandService, "commandRecordRepository", commandRecordRepository);
    }

    // ==================== TC-HIST-001: 查询设备命令历史 ====================

    @Nested
    @DisplayName("TC-HIST-001: 查询设备命令历史")
    class GetCommandsByDeviceIdTests {

        @Test
        @DisplayName("TC-HIST-001: 有效设备ID，返回命令列表，按时间倒序")
        void testGetCommandsByDeviceId_Success() {
            // Given
            String deviceId = "device_001";
            long now = System.currentTimeMillis();

            CommandRecord cmd1 = createCommandRecord("cmd-" + (now - 3000), deviceId, "toggle", 1, "success", now - 3000);
            CommandRecord cmd2 = createCommandRecord("cmd-" + (now - 2000), deviceId, "on", 2, "success", now - 2000);
            CommandRecord cmd3 = createCommandRecord("cmd-" + (now - 1000), deviceId, "off", 1, "pending", now - 1000);

            // 按时间倒序返回
            when(commandRecordRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId))
                    .thenReturn(Arrays.asList(cmd3, cmd2, cmd1));

            // When
            List<CommandRecord> result = commandService.getCommandsByDeviceId(deviceId);

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());
            // 验证按时间倒序排列（最新的在前）
            assertEquals("cmd-" + (now - 1000), result.get(0).getCmdId());
            assertEquals("cmd-" + (now - 2000), result.get(1).getCmdId());
            assertEquals("cmd-" + (now - 3000), result.get(2).getCmdId());

            verify(commandRecordRepository).findByDeviceIdOrderByCreatedAtDesc(deviceId);
        }

        @Test
        @DisplayName("TC-HIST-001: 多个设备的命令历史隔离")
        void testGetCommandsByDeviceId_DifferentDevices() {
            // Given
            String deviceId1 = "device_001";
            String deviceId2 = "device_002";

            CommandRecord cmd1 = createCommandRecord("cmd-001", deviceId1, "toggle", 1, "success", System.currentTimeMillis());
            CommandRecord cmd2 = createCommandRecord("cmd-002", deviceId2, "on", 2, "success", System.currentTimeMillis());

            when(commandRecordRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId1))
                    .thenReturn(List.of(cmd1));
            when(commandRecordRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId2))
                    .thenReturn(List.of(cmd2));

            // When
            List<CommandRecord> result1 = commandService.getCommandsByDeviceId(deviceId1);
            List<CommandRecord> result2 = commandService.getCommandsByDeviceId(deviceId2);

            // Then
            assertEquals(1, result1.size());
            assertEquals(deviceId1, result1.get(0).getDeviceId());

            assertEquals(1, result2.size());
            assertEquals(deviceId2, result2.get(0).getDeviceId());
        }

        @Test
        @DisplayName("TC-HIST-001: 命令历史包含各种状态的命令")
        void testGetCommandsByDeviceId_VariousStates() {
            // Given
            String deviceId = "device_001";
            long now = System.currentTimeMillis();

            CommandRecord pendingCmd = createCommandRecord("cmd-pending", deviceId, "toggle", 1, "pending", now - 1000);
            CommandRecord successCmd = createCommandRecord("cmd-success", deviceId, "on", 2, "success", now - 2000);
            CommandRecord failedCmd = createCommandRecord("cmd-failed", deviceId, "off", 1, "failed", now - 3000);
            CommandRecord timeoutCmd = createCommandRecord("cmd-timeout", deviceId, "toggle", 2, "timeout", now - 4000);

            when(commandRecordRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId))
                    .thenReturn(Arrays.asList(pendingCmd, successCmd, failedCmd, timeoutCmd));

            // When
            List<CommandRecord> result = commandService.getCommandsByDeviceId(deviceId);

            // Then
            assertEquals(4, result.size());
            assertEquals("pending", result.get(0).getState());
            assertEquals("success", result.get(1).getState());
            assertEquals("failed", result.get(2).getState());
            assertEquals("timeout", result.get(3).getState());
        }
    }

    // ==================== TC-HIST-002: 查询无命令设备 ====================

    @Nested
    @DisplayName("TC-HIST-002: 查询无命令设备")
    class GetCommandsByDeviceIdEmptyTests {

        @Test
        @DisplayName("TC-HIST-002: 无命令记录的设备，返回空列表")
        void testGetCommandsByDeviceId_EmptyList() {
            // Given
            String deviceId = "device_no_commands";
            when(commandRecordRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId))
                    .thenReturn(Collections.emptyList());

            // When
            List<CommandRecord> result = commandService.getCommandsByDeviceId(deviceId);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(commandRecordRepository).findByDeviceIdOrderByCreatedAtDesc(deviceId);
        }

        @Test
        @DisplayName("TC-HIST-002: 新设备无命令历史")
        void testGetCommandsByDeviceId_NewDevice() {
            // Given
            String newDeviceId = "new_device_999";
            when(commandRecordRepository.findByDeviceIdOrderByCreatedAtDesc(newDeviceId))
                    .thenReturn(Collections.emptyList());

            // When
            List<CommandRecord> result = commandService.getCommandsByDeviceId(newDeviceId);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== TC-HIST-003: 查询命令详情 ====================

    @Nested
    @DisplayName("TC-HIST-003: 查询命令详情")
    class GetCommandStatusTests {

        @Test
        @DisplayName("TC-HIST-003: 有效cmdId，返回命令详细信息")
        void testGetCommandStatus_Success() {
            // Given
            String cmdId = "cmd-1234567890";
            CommandRecord commandRecord = createCommandRecord(cmdId, "device_001", "toggle", 1, "success", System.currentTimeMillis());
            commandRecord.setMessage("Command executed successfully");
            commandRecord.setDurationMs(150);

            when(commandRecordRepository.findById(cmdId)).thenReturn(Optional.of(commandRecord));

            // When
            Map<String, Object> result = commandService.getCommandStatus(cmdId);

            // Then
            assertNotNull(result);
            assertEquals(cmdId, result.get("cmdId"));
            assertEquals("success", result.get("state"));
            assertEquals("Command executed successfully", result.get("message"));
            assertEquals(150, result.get("durationMs"));
            assertNotNull(result.get("updatedAt"));

            verify(commandRecordRepository).findById(cmdId);
        }

        @Test
        @DisplayName("TC-HIST-003: 查询待处理命令详情")
        void testGetCommandStatus_PendingCommand() {
            // Given
            String cmdId = "cmd-pending-001";
            CommandRecord commandRecord = createCommandRecord(cmdId, "device_001", "toggle", 1, "pending", System.currentTimeMillis());

            when(commandRecordRepository.findById(cmdId)).thenReturn(Optional.of(commandRecord));

            // When
            Map<String, Object> result = commandService.getCommandStatus(cmdId);

            // Then
            assertNotNull(result);
            assertEquals(cmdId, result.get("cmdId"));
            assertEquals("pending", result.get("state"));
            assertNull(result.get("durationMs")); // 待处理命令没有durationMs
        }

        @Test
        @DisplayName("TC-HIST-003: 查询失败命令详情")
        void testGetCommandStatus_FailedCommand() {
            // Given
            String cmdId = "cmd-failed-001";
            CommandRecord commandRecord = createCommandRecord(cmdId, "device_001", "toggle", 1, "failed", System.currentTimeMillis());
            commandRecord.setMessage("Device offline");
            commandRecord.setDurationMs(30000);

            when(commandRecordRepository.findById(cmdId)).thenReturn(Optional.of(commandRecord));

            // When
            Map<String, Object> result = commandService.getCommandStatus(cmdId);

            // Then
            assertNotNull(result);
            assertEquals("failed", result.get("state"));
            assertEquals("Device offline", result.get("message"));
            assertEquals(30000, result.get("durationMs"));
        }

        @Test
        @DisplayName("TC-HIST-003: 查询超时命令详情")
        void testGetCommandStatus_TimeoutCommand() {
            // Given
            String cmdId = "cmd-timeout-001";
            CommandRecord commandRecord = createCommandRecord(cmdId, "device_001", "on", 2, "timeout", System.currentTimeMillis());
            commandRecord.setMessage("Command timed out");

            when(commandRecordRepository.findById(cmdId)).thenReturn(Optional.of(commandRecord));

            // When
            Map<String, Object> result = commandService.getCommandStatus(cmdId);

            // Then
            assertNotNull(result);
            assertEquals("timeout", result.get("state"));
            assertEquals("Command timed out", result.get("message"));
        }

        @Test
        @DisplayName("TC-HIST-003: 命令不存在，返回null")
        void testGetCommandStatus_NotFound() {
            // Given
            String cmdId = "cmd-nonexistent";
            when(commandRecordRepository.findById(cmdId)).thenReturn(Optional.empty());

            // When
            Map<String, Object> result = commandService.getCommandStatus(cmdId);

            // Then
            assertNull(result);

            verify(commandRecordRepository).findById(cmdId);
        }
    }

    // ==================== 辅助方法：创建测试数据 ====================

    /**
     * 创建命令记录测试实体
     */
    private CommandRecord createCommandRecord(String cmdId, String deviceId, String action, Integer socket, String state, long createdAt) {
        CommandRecord record = new CommandRecord();
        record.setCmdId(cmdId);
        record.setDeviceId(deviceId);
        record.setAction(action);
        record.setSocket(socket);
        record.setState(state);
        record.setPayloadJson("{\"action\":\"" + action + "\"}");
        record.setMessage("");
        record.setCreatedAt(createdAt);
        record.setUpdatedAt(createdAt);
        record.setExpiresAt(createdAt + 30000);
        return record;
    }
}