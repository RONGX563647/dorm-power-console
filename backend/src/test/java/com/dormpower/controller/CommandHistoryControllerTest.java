package com.dormpower.controller;

import com.dormpower.config.TestSecurityConfig;
import com.dormpower.model.CommandRecord;
import com.dormpower.repository.CommandRecordRepository;
import com.dormpower.service.CommandService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 命令历史查询控制器单元测试
 *
 * 测试用例覆盖：
 * - TC-HIST-001: 查询设备命令历史
 * - TC-HIST-002: 查询无命令设备
 * - TC-HIST-003: 查询命令详情
 *
 * @author dormpower team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class CommandHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommandService commandService;

    @MockBean
    private CommandRecordRepository commandRecordRepository;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    // ==================== TC-HIST-001: 查询设备命令历史 ====================

    @Nested
    @DisplayName("TC-HIST-001: 查询设备命令历史")
    class GetDeviceCommandHistoryTests {

        @Test
        @DisplayName("TC-HIST-001: 有效设备ID，返回命令列表，按时间倒序")
        void testGetDeviceCommandHistory_Success() throws Exception {
            // Given
            String deviceId = "device_001";
            long now = System.currentTimeMillis();

            CommandRecord cmd1 = createCommandRecord("cmd-" + (now - 3000), deviceId, "toggle", 1, "success", now - 3000);
            CommandRecord cmd2 = createCommandRecord("cmd-" + (now - 2000), deviceId, "on", 2, "success", now - 2000);
            CommandRecord cmd3 = createCommandRecord("cmd-" + (now - 1000), deviceId, "off", 1, "pending", now - 1000);

            List<CommandRecord> commandList = Arrays.asList(cmd3, cmd2, cmd1);
            when(commandService.getCommandsByDeviceId(deviceId)).thenReturn(commandList);

            // When & Then
            mockMvc.perform(get("/api/commands/device/{deviceId}", deviceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].cmdId").value("cmd-" + (now - 1000)))
                    .andExpect(jsonPath("$[0].state").value("pending"))
                    .andExpect(jsonPath("$[1].state").value("success"))
                    .andExpect(jsonPath("$[2].state").value("success"));
        }

        @Test
        @DisplayName("TC-HIST-001: 命令历史包含完整字段")
        void testGetDeviceCommandHistory_CompleteFields() throws Exception {
            // Given
            String deviceId = "device_001";
            CommandRecord cmd = createCommandRecord("cmd-001", deviceId, "toggle", 1, "success", System.currentTimeMillis());
            cmd.setMessage("Command executed successfully");
            cmd.setDurationMs(150);

            when(commandService.getCommandsByDeviceId(deviceId)).thenReturn(List.of(cmd));

            // When & Then
            mockMvc.perform(get("/api/commands/device/{deviceId}", deviceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].cmdId").value("cmd-001"))
                    .andExpect(jsonPath("$[0].deviceId").value(deviceId))
                    .andExpect(jsonPath("$[0].action").value("toggle"))
                    .andExpect(jsonPath("$[0].socket").value(1))
                    .andExpect(jsonPath("$[0].state").value("success"))
                    .andExpect(jsonPath("$[0].message").value("Command executed successfully"));
        }
    }

    // ==================== TC-HIST-002: 查询无命令设备 ====================

    @Nested
    @DisplayName("TC-HIST-002: 查询无命令设备")
    class GetDeviceCommandHistoryEmptyTests {

        @Test
        @DisplayName("TC-HIST-002: 无命令记录的设备，返回空列表")
        void testGetDeviceCommandHistory_EmptyList() throws Exception {
            // Given
            String deviceId = "device_no_commands";
            when(commandService.getCommandsByDeviceId(deviceId)).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/commands/device/{deviceId}", deviceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("TC-HIST-002: 新设备无命令历史，返回空列表")
        void testGetDeviceCommandHistory_NewDevice() throws Exception {
            // Given
            String newDeviceId = "new_device_999";
            when(commandService.getCommandsByDeviceId(newDeviceId)).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/commands/device/{deviceId}", newDeviceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ==================== TC-HIST-003: 查询命令详情 ====================

    @Nested
    @DisplayName("TC-HIST-003: 查询命令详情")
    class GetCommandDetailTests {

        @Test
        @DisplayName("TC-HIST-003: 有效cmdId，返回命令详细信息")
        void testGetCommandDetail_Success() throws Exception {
            // Given
            String cmdId = "cmd-1234567890";
            Map<String, Object> status = new HashMap<>();
            status.put("cmdId", cmdId);
            status.put("state", "success");
            status.put("message", "Command executed successfully");
            status.put("updatedAt", System.currentTimeMillis());
            status.put("durationMs", 150);

            when(commandService.getCommandStatus(cmdId)).thenReturn(status);

            // When & Then
            mockMvc.perform(get("/api/commands/{cmdId}", cmdId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cmdId").value(cmdId))
                    .andExpect(jsonPath("$.state").value("success"))
                    .andExpect(jsonPath("$.message").value("Command executed successfully"))
                    .andExpect(jsonPath("$.durationMs").value(150));
        }

        @Test
        @DisplayName("TC-HIST-003: 查询待处理命令详情")
        void testGetCommandDetail_PendingCommand() throws Exception {
            // Given
            String cmdId = "cmd-pending-001";
            Map<String, Object> status = new HashMap<>();
            status.put("cmdId", cmdId);
            status.put("state", "pending");
            status.put("message", "");
            status.put("updatedAt", System.currentTimeMillis());

            when(commandService.getCommandStatus(cmdId)).thenReturn(status);

            // When & Then
            mockMvc.perform(get("/api/commands/{cmdId}", cmdId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cmdId").value(cmdId))
                    .andExpect(jsonPath("$.state").value("pending"));
        }

        @Test
        @DisplayName("TC-HIST-003: 查询失败命令详情")
        void testGetCommandDetail_FailedCommand() throws Exception {
            // Given
            String cmdId = "cmd-failed-001";
            Map<String, Object> status = new HashMap<>();
            status.put("cmdId", cmdId);
            status.put("state", "failed");
            status.put("message", "Device offline");
            status.put("updatedAt", System.currentTimeMillis());
            status.put("durationMs", 30000);

            when(commandService.getCommandStatus(cmdId)).thenReturn(status);

            // When & Then
            mockMvc.perform(get("/api/commands/{cmdId}", cmdId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.state").value("failed"))
                    .andExpect(jsonPath("$.message").value("Device offline"));
        }

        @Test
        @DisplayName("TC-HIST-003: 命令不存在，返回404")
        void testGetCommandDetail_NotFound() throws Exception {
            // Given
            String cmdId = "cmd-nonexistent";
            when(commandService.getCommandStatus(cmdId)).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/api/commands/{cmdId}", cmdId))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== 其他接口测试：/api/cmd/{cmdId} ====================

    @Nested
    @DisplayName("其他接口测试")
    class OtherEndpointTests {

        @Test
        @DisplayName("通过/api/cmd/{cmdId}查询命令状态-成功")
        void testGetCommandStatus_Success() throws Exception {
            // Given
            String cmdId = "cmd-123456";
            Map<String, Object> status = new HashMap<>();
            status.put("cmdId", cmdId);
            status.put("state", "success");
            status.put("message", "OK");
            status.put("updatedAt", System.currentTimeMillis());

            when(commandService.getCommandStatus(cmdId)).thenReturn(status);

            // When & Then
            mockMvc.perform(get("/api/cmd/{cmdId}", cmdId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cmdId").value(cmdId))
                    .andExpect(jsonPath("$.state").value("success"));
        }

        @Test
        @DisplayName("通过/api/cmd/{cmdId}查询命令状态-不存在")
        void testGetCommandStatus_NotFound() throws Exception {
            // Given
            String cmdId = "cmd-nonexistent";
            when(commandService.getCommandStatus(cmdId)).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/api/cmd/{cmdId}", cmdId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.ok").value(false))
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("cmd not found"));
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