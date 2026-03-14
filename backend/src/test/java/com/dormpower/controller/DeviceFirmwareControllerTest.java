package com.dormpower.controller;

import com.dormpower.config.TestSecurityConfig;
import com.dormpower.model.DeviceFirmware;
import com.dormpower.repository.DeviceFirmwareRepository;
import com.dormpower.service.DeviceFirmwareService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 设备固件控制器单元测试
 *
 * 测试用例覆盖：
 * - TC-FW-001: 发起固件升级
 * - TC-FW-002: 重复发起升级
 * - TC-FW-003: 更新升级进度
 * - TC-FW-004: 完成升级成功
 * - TC-FW-005: 完成升级失败
 * - TC-FW-006: 查询固件历史
 *
 * @author dormpower team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class DeviceFirmwareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceFirmwareService firmwareService;

    @MockBean
    private DeviceFirmwareRepository firmwareRepository;

    private DeviceFirmware testFirmware;

    @BeforeEach
    void setUp() {
        testFirmware = createTestFirmware();
    }

    // ==================== TC-FW-001: 发起固件升级 ====================

    @Nested
    @DisplayName("TC-FW-001: 发起固件升级")
    class InitiateUpgradeTests {

        @Test
        @DisplayName("TC-FW-001: 有效参数，返回200，状态为PENDING")
        void testInitiateUpgrade_Success() throws Exception {
            // Given
            when(firmwareService.initiateUpgrade(anyString(), anyString(), anyString(), anyString(), anyLong(), anyString()))
                    .thenReturn(testFirmware);

            // When & Then
            mockMvc.perform(post("/api/firmware/upgrade")
                            .param("deviceId", "strip01")
                            .param("version", "1.2.0")
                            .param("filePath", "/firmware/strip_v1.2.0.bin")
                            .param("checksum", "a1b2c3d4e5f6")
                            .param("fileSize", "102400")
                            .param("initiatedBy", "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deviceId").value("strip01"))
                    .andExpect(jsonPath("$.version").value("1.2.0"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("TC-FW-001: 缺少必填参数deviceId，返回500")
        void testInitiateUpgrade_MissingDeviceId() throws Exception {
            // When & Then - 缺少必填参数时Spring会抛出异常，返回500
            mockMvc.perform(post("/api/firmware/upgrade")
                            .param("version", "1.2.0")
                            .param("filePath", "/firmware/strip_v1.2.0.bin"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("TC-FW-001: 使用默认参数值")
        void testInitiateUpgrade_DefaultValues() throws Exception {
            // Given
            DeviceFirmware firmware = createTestFirmware();
            firmware.setInitiatedBy("system");
            firmware.setFileSize(0);
            when(firmwareService.initiateUpgrade(anyString(), anyString(), anyString(), anyString(), anyLong(), anyString()))
                    .thenReturn(firmware);

            // When & Then
            mockMvc.perform(post("/api/firmware/upgrade")
                            .param("deviceId", "strip01")
                            .param("version", "1.2.0")
                            .param("filePath", "/firmware/strip_v1.2.0.bin"))
                    .andExpect(status().isOk());
        }
    }

    // ==================== TC-FW-002: 重复发起升级 ====================

    @Nested
    @DisplayName("TC-FW-002: 重复发起升级")
    class DuplicateUpgradeTests {

        @Test
        @DisplayName("TC-FW-002: 已有待处理升级，返回400，提示已有待处理升级")
        void testInitiateUpgrade_AlreadyPending() throws Exception {
            // Given
            when(firmwareService.initiateUpgrade(anyString(), anyString(), anyString(), anyString(), anyLong(), anyString()))
                    .thenThrow(new RuntimeException("Device already has a pending upgrade"));

            // When & Then
            mockMvc.perform(post("/api/firmware/upgrade")
                            .param("deviceId", "strip01")
                            .param("version", "1.3.0")
                            .param("filePath", "/firmware/strip_v1.3.0.bin")
                            .param("checksum", "checksum")
                            .param("fileSize", "1024")
                            .param("initiatedBy", "admin"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Device already has a pending upgrade"));
        }
    }

    // ==================== TC-FW-003: 更新升级进度 ====================

    @Nested
    @DisplayName("TC-FW-003: 更新升级进度")
    class UpdateProgressTests {

        @Test
        @DisplayName("TC-FW-003: progress=50，返回200，进度更新成功")
        void testUpdateProgress_Success() throws Exception {
            // Given
            DeviceFirmware firmware = createTestFirmware();
            firmware.setProgress(50);
            firmware.setStatus("DOWNLOADING");
            when(firmwareService.updateProgress(1L, 50)).thenReturn(firmware);

            // When & Then
            mockMvc.perform(put("/api/firmware/{firmwareId}/progress", 1L)
                            .param("progress", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.progress").value(50))
                    .andExpect(jsonPath("$.status").value("DOWNLOADING"));
        }

        @Test
        @DisplayName("TC-FW-003: 进度为100，状态变为INSTALLING")
        void testUpdateProgress_Progress100() throws Exception {
            // Given
            DeviceFirmware firmware = createTestFirmware();
            firmware.setProgress(100);
            firmware.setStatus("INSTALLING");
            when(firmwareService.updateProgress(1L, 100)).thenReturn(firmware);

            // When & Then
            mockMvc.perform(put("/api/firmware/{firmwareId}/progress", 1L)
                            .param("progress", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.progress").value(100))
                    .andExpect(jsonPath("$.status").value("INSTALLING"));
        }

        @Test
        @DisplayName("TC-FW-003: 固件记录不存在，返回400")
        void testUpdateProgress_NotFound() throws Exception {
            // Given
            when(firmwareService.updateProgress(anyLong(), anyInt()))
                    .thenThrow(new RuntimeException("Firmware record not found"));

            // When & Then
            mockMvc.perform(put("/api/firmware/{firmwareId}/progress", 999L)
                            .param("progress", "50"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Firmware record not found"));
        }
    }

    // ==================== TC-FW-004: 完成升级成功 ====================

    @Nested
    @DisplayName("TC-FW-004: 完成升级成功")
    class CompleteUpgradeSuccessTests {

        @Test
        @DisplayName("TC-FW-004: success=true，状态更新为SUCCESS")
        void testCompleteUpgrade_Success() throws Exception {
            // Given
            DeviceFirmware firmware = createTestFirmware();
            firmware.setStatus("SUCCESS");
            firmware.setProgress(100);
            when(firmwareService.completeUpgrade(1L, true, null)).thenReturn(firmware);

            // When & Then
            mockMvc.perform(post("/api/firmware/{firmwareId}/complete", 1L)
                            .param("success", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.progress").value(100));
        }

        @Test
        @DisplayName("TC-FW-004: 完成升级时记录完成时间")
        void testCompleteUpgrade_RecordCompletedAt() throws Exception {
            // Given
            DeviceFirmware firmware = createTestFirmware();
            firmware.setStatus("SUCCESS");
            firmware.setCompletedAt(System.currentTimeMillis() / 1000);
            when(firmwareService.completeUpgrade(1L, true, null)).thenReturn(firmware);

            // When & Then
            mockMvc.perform(post("/api/firmware/{firmwareId}/complete", 1L)
                            .param("success", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completedAt").exists());
        }
    }

    // ==================== TC-FW-005: 完成升级失败 ====================

    @Nested
    @DisplayName("TC-FW-005: 完成升级失败")
    class CompleteUpgradeFailedTests {

        @Test
        @DisplayName("TC-FW-005: success=false，状态更新为FAILED，记录错误信息")
        void testCompleteUpgrade_Failed() throws Exception {
            // Given
            DeviceFirmware firmware = createTestFirmware();
            firmware.setStatus("FAILED");
            firmware.setErrorMessage("Download failed: connection timeout");
            when(firmwareService.completeUpgrade(1L, false, "Download failed: connection timeout"))
                    .thenReturn(firmware);

            // When & Then
            mockMvc.perform(post("/api/firmware/{firmwareId}/complete", 1L)
                            .param("success", "false")
                            .param("errorMessage", "Download failed: connection timeout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FAILED"))
                    .andExpect(jsonPath("$.errorMessage").value("Download failed: connection timeout"));
        }

        @Test
        @DisplayName("TC-FW-005: 固件记录不存在，返回400")
        void testCompleteUpgrade_NotFound() throws Exception {
            // Given
            when(firmwareService.completeUpgrade(anyLong(), anyBoolean(), anyString()))
                    .thenThrow(new RuntimeException("Firmware record not found"));

            // When & Then
            mockMvc.perform(post("/api/firmware/{firmwareId}/complete", 999L)
                            .param("success", "false")
                            .param("errorMessage", "test error"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Firmware record not found"));
        }
    }

    // ==================== TC-FW-006: 查询固件历史 ====================

    @Nested
    @DisplayName("TC-FW-006: 查询固件历史")
    class GetFirmwareHistoryTests {

        @Test
        @DisplayName("TC-FW-006: 有效设备ID，返回升级历史列表")
        void testGetDeviceFirmwareHistory_Success() throws Exception {
            // Given
            DeviceFirmware firmware1 = createTestFirmware();
            firmware1.setId(1L);
            firmware1.setVersion("1.0.0");
            firmware1.setStatus("SUCCESS");

            DeviceFirmware firmware2 = createTestFirmware();
            firmware2.setId(2L);
            firmware2.setVersion("1.1.0");
            firmware2.setStatus("SUCCESS");

            DeviceFirmware firmware3 = createTestFirmware();
            firmware3.setId(3L);
            firmware3.setVersion("1.2.0");
            firmware3.setStatus("PENDING");

            when(firmwareService.getDeviceFirmwareHistory("strip01"))
                    .thenReturn(Arrays.asList(firmware3, firmware2, firmware1));

            // When & Then
            mockMvc.perform(get("/api/firmware/device/{deviceId}", "strip01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].version").value("1.2.0"))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("TC-FW-006: 设备无升级历史，返回空列表")
        void testGetDeviceFirmwareHistory_Empty() throws Exception {
            // Given
            when(firmwareService.getDeviceFirmwareHistory("new_device"))
                    .thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/firmware/device/{deviceId}", "new_device"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ==================== 其他接口测试 ====================

    @Nested
    @DisplayName("其他接口测试")
    class OtherEndpointTests {

        @Test
        @DisplayName("获取固件详情-存在")
        void testGetFirmwareById_Found() throws Exception {
            // Given
            when(firmwareService.getFirmwareById(1L)).thenReturn(Optional.of(testFirmware));

            // When & Then
            mockMvc.perform(get("/api/firmware/{firmwareId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deviceId").value("strip01"))
                    .andExpect(jsonPath("$.version").value("1.2.0"));
        }

        @Test
        @DisplayName("获取固件详情-不存在")
        void testGetFirmwareById_NotFound() throws Exception {
            // Given
            when(firmwareService.getFirmwareById(999L)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/firmware/{firmwareId}", 999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("获取当前固件版本-存在")
        void testGetCurrentFirmware_Found() throws Exception {
            // Given
            DeviceFirmware currentFirmware = createTestFirmware();
            currentFirmware.setStatus("SUCCESS");
            when(firmwareService.getCurrentFirmware("strip01")).thenReturn(Optional.of(currentFirmware));

            // When & Then
            mockMvc.perform(get("/api/firmware/device/{deviceId}/current", "strip01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.version").value("1.2.0"));
        }

        @Test
        @DisplayName("获取当前固件版本-不存在")
        void testGetCurrentFirmware_NotFound() throws Exception {
            // Given
            when(firmwareService.getCurrentFirmware("strip01")).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/firmware/device/{deviceId}/current", "strip01"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("获取待处理升级列表")
        void testGetPendingUpgrades() throws Exception {
            // Given
            DeviceFirmware pending = createTestFirmware();
            pending.setStatus("PENDING");
            when(firmwareService.getPendingUpgrades()).thenReturn(Arrays.asList(pending));

            // When & Then
            mockMvc.perform(get("/api/firmware/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("获取进行中升级列表")
        void testGetActiveUpgrades() throws Exception {
            // Given
            DeviceFirmware downloading = createTestFirmware();
            downloading.setStatus("DOWNLOADING");
            when(firmwareService.getActiveUpgrades()).thenReturn(Arrays.asList(downloading));

            // When & Then
            mockMvc.perform(get("/api/firmware/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].status").value("DOWNLOADING"));
        }

        @Test
        @DisplayName("发送升级命令成功")
        void testSendUpgradeCommand_Success() throws Exception {
            // Given
            when(firmwareService.sendUpgradeCommand(1L)).thenReturn(true);

            // When & Then
            mockMvc.perform(post("/api/firmware/{firmwareId}/send", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Upgrade command sent"));
        }

        @Test
        @DisplayName("发送升级命令失败")
        void testSendUpgradeCommand_Failed() throws Exception {
            // Given
            when(firmwareService.sendUpgradeCommand(1L)).thenReturn(false);

            // When & Then
            mockMvc.perform(post("/api/firmware/{firmwareId}/send", 1L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Failed to send upgrade command"));
        }

        @Test
        @DisplayName("取消升级成功")
        void testCancelUpgrade_Success() throws Exception {
            // Given
            DeviceFirmware cancelled = createTestFirmware();
            cancelled.setStatus("FAILED");
            cancelled.setErrorMessage("Cancelled: User requested");
            when(firmwareService.cancelUpgrade(1L, "User requested")).thenReturn(cancelled);

            // When & Then
            mockMvc.perform(post("/api/firmware/{firmwareId}/cancel", 1L)
                            .param("reason", "User requested"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FAILED"))
                    .andExpect(jsonPath("$.errorMessage").value("Cancelled: User requested"));
        }
    }

    // ==================== 辅助方法：创建测试数据 ====================

    /**
     * 创建测试固件实体
     */
    private DeviceFirmware createTestFirmware() {
        DeviceFirmware firmware = new DeviceFirmware();
        firmware.setId(1L);
        firmware.setDeviceId("strip01");
        firmware.setVersion("1.2.0");
        firmware.setPreviousVersion("1.1.0");
        firmware.setFilePath("/firmware/strip_v1.2.0.bin");
        firmware.setChecksum("a1b2c3d4e5f6");
        firmware.setFileSize(102400);
        firmware.setStatus("PENDING");
        firmware.setInitiatedBy("admin");
        return firmware;
    }
}