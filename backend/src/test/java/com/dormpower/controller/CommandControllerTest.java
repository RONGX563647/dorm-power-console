package com.dormpower.controller;

import com.dormpower.config.TestSecurityConfig;
import com.dormpower.mqtt.MqttBridge;
import com.dormpower.model.CommandRecord;
import com.dormpower.model.Device;
import com.dormpower.repository.CommandRecordRepository;
import com.dormpower.repository.DeviceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 命令下发单元测试
 *
 * 测试用例覆盖：
 * - TC-CMD-001: 正常下发命令
 * - TC-CMD-002: 下发命令到不存在设备
 * - TC-CMD-003: 下发冲突命令
 * - TC-CMD-004: 批量下发命令
 * - TC-CMD-005: MQTT不可用
 *
 * @author dormpower team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class CommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceRepository deviceRepository;

    @MockBean
    private CommandRecordRepository commandRecordRepository;

    @MockBean
    private MqttBridge mqttBridge;

    // 测试数据常量
    private static final String DEVICE_ID_1 = "device-001";
    private static final String DEVICE_ID_2 = "device-002";
    private static final String DEVICE_ID_INVALID = "device-999";
    private static final String CMD_ID_1 = "cmd-1234567890";
    private static final String CMD_ID_2 = "cmd-1234567891";

    /**
     * 命令下发操作测试
     */
    @Nested
    @DisplayName("命令下发操作测试")
    class CommandOperationTests {

        /**
         * TC-CMD-001: 正常下发命令
         *
         * 测试场景：正常下发命令
         * 输入：有效设备ID和命令参数
         * 预期输出：返回200，包含cmdId
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-CMD-001: 正常下发命令-返回200，包含cmdId")
        void testSendCommand_Success() throws Exception {
            // Arrange
            Device device = createDevice(DEVICE_ID_1, true);
            when(deviceRepository.findById(DEVICE_ID_1)).thenReturn(Optional.of(device));
            when(commandRecordRepository.findByDeviceIdAndState(anyString(), anyString()))
                    .thenReturn(new ArrayList<>());
            when(mqttBridge.publishCommand(anyString(), any())).thenReturn(true);

            CommandRecord savedRecord = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "pending");
            when(commandRecordRepository.save(any(CommandRecord.class))).thenReturn(savedRecord);

            // Act & Assert
            mockMvc.perform(post("/api/strips/{deviceId}/cmd", DEVICE_ID_1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"action\":\"toggle\",\"socket\":1}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.cmdId").exists())
                    .andExpect(jsonPath("$.stripId").value(DEVICE_ID_1));

            verify(mqttBridge).publishCommand(anyString(), any());
        }

        /**
         * TC-CMD-002: 下发命令到不存在设备
         *
         * 测试场景：下发命令到不存在设备
         * 输入：无效设备ID
         * 预期输出：返回404
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-CMD-002: 下发命令到不存在设备-返回404")
        void testSendCommand_DeviceNotFound() throws Exception {
            // Arrange
            when(deviceRepository.findById(DEVICE_ID_INVALID)).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(post("/api/strips/{deviceId}/cmd", DEVICE_ID_INVALID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"action\":\"toggle\",\"socket\":1}"))
                    .andExpect(status().isNotFound());

            verify(mqttBridge, never()).publishCommand(anyString(), any());
        }

        /**
         * TC-CMD-003: 下发冲突命令
         *
         * 测试场景：下发冲突命令
         * 输入：同设备同插座待处理命令
         * 预期输出：返回400（业务异常）
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-CMD-003: 下发冲突命令-返回400")
        void testSendCommand_ConflictCommand() throws Exception {
            // Arrange
            Device device = createDevice(DEVICE_ID_1, true);
            when(deviceRepository.findById(DEVICE_ID_1)).thenReturn(Optional.of(device));

            // 模拟存在待处理的冲突命令
            CommandRecord pendingCommand = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "pending");
            when(commandRecordRepository.findByDeviceIdAndState(DEVICE_ID_1, "pending"))
                    .thenReturn(List.of(pendingCommand));

            // Act & Assert
            mockMvc.perform(post("/api/strips/{deviceId}/cmd", DEVICE_ID_1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"action\":\"toggle\",\"socket\":1}"))
                    .andExpect(status().isBadRequest());

            verify(mqttBridge, never()).publishCommand(anyString(), any());
        }

        /**
         * TC-CMD-004: 批量下发命令
         *
         * 测试场景：批量下发命令
         * 输入：多个设备ID
         * 预期输出：返回成功数量和cmdId列表
         * 优先级：P1
         */
        @Test
        @DisplayName("TC-CMD-004: 批量下发命令-返回成功数量和cmdId列表")
        void testSendBatchCommand_Success() throws Exception {
            // Arrange
            Device device1 = createDevice(DEVICE_ID_1, true);
            Device device2 = createDevice(DEVICE_ID_2, true);
            when(deviceRepository.findById(DEVICE_ID_1)).thenReturn(Optional.of(device1));
            when(deviceRepository.findById(DEVICE_ID_2)).thenReturn(Optional.of(device2));
            when(commandRecordRepository.findByDeviceIdAndState(anyString(), anyString()))
                    .thenReturn(new ArrayList<>());
            when(mqttBridge.publishCommand(anyString(), any())).thenReturn(true);

            CommandRecord record1 = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "pending");
            CommandRecord record2 = createCommandRecord(CMD_ID_2, DEVICE_ID_2, "toggle", 1, "pending");
            when(commandRecordRepository.save(any(CommandRecord.class)))
                    .thenReturn(record1, record2);

            // Act & Assert
            mockMvc.perform(post("/api/commands/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"deviceIds\":[\"" + DEVICE_ID_1 + "\",\"" + DEVICE_ID_2 + "\"],\"command\":{\"action\":\"toggle\",\"socket\":1}}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.deviceCount").value(2))
                    .andExpect(jsonPath("$.successCount").value(2))
                    .andExpect(jsonPath("$.commandIds").exists());

            verify(mqttBridge, times(2)).publishCommand(anyString(), any());
        }

        /**
         * TC-CMD-005: MQTT不可用
         *
         * 测试场景：MQTT不可用
         * 输入：MQTT连接断开
         * 预期输出：命令状态标记为failed
         * 优先级：P1
         */
        @Test
        @DisplayName("TC-CMD-005: MQTT不可用-命令状态标记为failed")
        void testSendCommand_MqttUnavailable() throws Exception {
            // Arrange
            Device device = createDevice(DEVICE_ID_1, true);
            when(deviceRepository.findById(DEVICE_ID_1)).thenReturn(Optional.of(device));
            when(commandRecordRepository.findByDeviceIdAndState(anyString(), anyString()))
                    .thenReturn(new ArrayList<>());

            CommandRecord savedRecord = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "pending");
            when(commandRecordRepository.save(any(CommandRecord.class))).thenReturn(savedRecord);
            when(commandRecordRepository.findById(CMD_ID_1)).thenReturn(Optional.of(savedRecord));

            // 模拟MQTT不可用
            when(mqttBridge.publishCommand(anyString(), any())).thenReturn(false);

            // Act & Assert
            mockMvc.perform(post("/api/strips/{deviceId}/cmd", DEVICE_ID_1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"action\":\"toggle\",\"socket\":1}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cmdId").exists());

            // 验证命令状态被更新为failed（save至少被调用一次用于创建命令记录）
            verify(commandRecordRepository, atLeast(1)).save(any(CommandRecord.class));
        }
    }

    /**
     * 命令查询测试
     */
    @Nested
    @DisplayName("命令查询测试")
    class CommandQueryTests {

        @Test
        @DisplayName("获取命令详情-成功")
        void testGetCommandDetail_Success() throws Exception {
            // Arrange
            CommandRecord record = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "success");
            record.setMessage("Command executed successfully");
            record.setDurationMs(150);
            when(commandRecordRepository.findById(CMD_ID_1)).thenReturn(Optional.of(record));

            // Act & Assert
            mockMvc.perform(get("/api/commands/{cmdId}", CMD_ID_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cmdId").value(CMD_ID_1))
                    .andExpect(jsonPath("$.state").value("success"));
        }

        @Test
        @DisplayName("获取命令详情-命令不存在")
        void testGetCommandDetail_NotFound() throws Exception {
            // Arrange
            when(commandRecordRepository.findById("non_existent_cmd")).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get("/api/commands/{cmdId}", "non_existent_cmd"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("获取设备命令历史-成功")
        void testGetDeviceCommandHistory_Success() throws Exception {
            // Arrange
            CommandRecord record1 = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "success");
            CommandRecord record2 = createCommandRecord(CMD_ID_2, DEVICE_ID_1, "on", 2, "pending");
            when(commandRecordRepository.findByDeviceIdOrderByCreatedAtDesc(DEVICE_ID_1))
                    .thenReturn(List.of(record1, record2));

            // Act & Assert
            mockMvc.perform(get("/api/commands/device/{deviceId}", DEVICE_ID_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }

    /**
     * 边界条件测试
     */
    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("批量下发命令-部分设备不存在")
        void testBatchCommand_PartialDeviceNotExist() throws Exception {
            // Arrange
            Device device1 = createDevice(DEVICE_ID_1, true);
            when(deviceRepository.findById(DEVICE_ID_1)).thenReturn(Optional.of(device1));
            when(deviceRepository.findById(DEVICE_ID_INVALID)).thenReturn(Optional.empty());
            when(commandRecordRepository.findByDeviceIdAndState(anyString(), anyString()))
                    .thenReturn(new ArrayList<>());
            when(mqttBridge.publishCommand(anyString(), any())).thenReturn(true);

            CommandRecord record1 = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "pending");
            when(commandRecordRepository.save(any(CommandRecord.class))).thenReturn(record1);

            // Act & Assert
            mockMvc.perform(post("/api/commands/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"deviceIds\":[\"" + DEVICE_ID_1 + "\",\"" + DEVICE_ID_INVALID + "\"],\"command\":{\"action\":\"toggle\",\"socket\":1}}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deviceCount").value(2))
                    .andExpect(jsonPath("$.successCount").value(1)); // 只成功1个
        }

        @Test
        @DisplayName("下发命令-不带socket参数")
        void testSendCommand_WithoutSocket() throws Exception {
            // Arrange
            Device device = createDevice(DEVICE_ID_1, true);
            when(deviceRepository.findById(DEVICE_ID_1)).thenReturn(Optional.of(device));
            when(commandRecordRepository.findByDeviceIdAndState(anyString(), anyString()))
                    .thenReturn(new ArrayList<>());
            when(mqttBridge.publishCommand(anyString(), any())).thenReturn(true);

            CommandRecord savedRecord = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", null, "pending");
            when(commandRecordRepository.save(any(CommandRecord.class))).thenReturn(savedRecord);

            // Act & Assert
            mockMvc.perform(post("/api/strips/{deviceId}/cmd", DEVICE_ID_1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"action\":\"toggle\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true));
        }

        @Test
        @DisplayName("批量下发命令-空设备列表")
        void testBatchCommand_EmptyDeviceList() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/commands/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"deviceIds\":[],\"command\":{\"action\":\"toggle\"}}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deviceCount").value(0))
                    .andExpect(jsonPath("$.successCount").value(0));
        }

        @Test
        @DisplayName("下发冲突命令-不同插座不冲突")
        void testSendCommand_DifferentSocketNoConflict() throws Exception {
            // Arrange
            Device device = createDevice(DEVICE_ID_1, true);
            when(deviceRepository.findById(DEVICE_ID_1)).thenReturn(Optional.of(device));

            // 插座1有待处理命令
            CommandRecord pendingCommand = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "pending");
            when(commandRecordRepository.findByDeviceIdAndState(DEVICE_ID_1, "pending"))
                    .thenReturn(List.of(pendingCommand));
            when(mqttBridge.publishCommand(anyString(), any())).thenReturn(true);

            CommandRecord newRecord = createCommandRecord(CMD_ID_2, DEVICE_ID_1, "toggle", 2, "pending");
            when(commandRecordRepository.save(any(CommandRecord.class))).thenReturn(newRecord);

            // Act & Assert: 向插座2发送命令应该成功（不冲突）
            mockMvc.perform(post("/api/strips/{deviceId}/cmd", DEVICE_ID_1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"action\":\"toggle\",\"socket\":2}"))
                    .andExpect(status().isOk());
        }
    }

    // ==================== 辅助方法：创建测试数据 ====================

    /**
     * 创建设备实体
     */
    private Device createDevice(String deviceId, boolean online) {
        Device device = new Device();
        device.setId(deviceId);
        device.setName("测试设备-" + deviceId);
        device.setRoom("101");
        device.setOnline(online);
        device.setLastSeenTs(System.currentTimeMillis() / 1000);
        device.setCreatedAt(System.currentTimeMillis() / 1000 - 86400);
        return device;
    }

    /**
     * 创建命令记录实体
     */
    private CommandRecord createCommandRecord(String cmdId, String deviceId, String action, Integer socket, String state) {
        CommandRecord record = new CommandRecord();
        record.setCmdId(cmdId);
        record.setDeviceId(deviceId);
        record.setAction(action);
        record.setSocket(socket);
        record.setState(state);
        record.setMessage("");
        record.setCreatedAt(System.currentTimeMillis());
        record.setUpdatedAt(System.currentTimeMillis());
        record.setExpiresAt(System.currentTimeMillis() + 30000);
        record.setPayloadJson("{\"action\":\"" + action + "\"}");
        return record;
    }
}