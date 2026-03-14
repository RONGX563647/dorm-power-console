package com.dormpower.service;

import com.dormpower.model.DeviceFirmware;
import com.dormpower.repository.DeviceFirmwareRepository;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 设备固件服务单元测试
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
@ExtendWith(MockitoExtension.class)
class DeviceFirmwareServiceTest {

    @Mock
    private DeviceFirmwareRepository firmwareRepository;

    @Mock
    private MqttService mqttService;

    @Mock
    private SystemLogService systemLogService;

    @Mock
    private NotificationService notificationService;

    private DeviceFirmwareService firmwareService;

    private DeviceFirmware testFirmware;

    @BeforeEach
    void setUp() {
        // 手动创建服务实例并注入mock
        firmwareService = new DeviceFirmwareService();
        org.springframework.test.util.ReflectionTestUtils.setField(firmwareService, "firmwareRepository", firmwareRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(firmwareService, "mqttService", mqttService);
        org.springframework.test.util.ReflectionTestUtils.setField(firmwareService, "systemLogService", systemLogService);
        org.springframework.test.util.ReflectionTestUtils.setField(firmwareService, "notificationService", notificationService);

        testFirmware = new DeviceFirmware();
        testFirmware.setId(1L);
        testFirmware.setDeviceId("strip01");
        testFirmware.setVersion("1.2.0");
        testFirmware.setPreviousVersion("1.1.0");
        testFirmware.setFilePath("/firmware/strip_v1.2.0.bin");
        testFirmware.setChecksum("a1b2c3d4e5f6");
        testFirmware.setFileSize(102400);
        testFirmware.setStatus("PENDING");
        testFirmware.setInitiatedBy("admin");
    }

    // ==================== TC-FW-001: 发起固件升级 ====================

    @Nested
    @DisplayName("TC-FW-001: 发起固件升级")
    class InitiateUpgradeTests {

        @Test
        @DisplayName("TC-FW-001: 有效参数，返回200，状态为PENDING")
        void testInitiateUpgrade_Success() {
            // Given
            String deviceId = "strip01";
            String version = "1.2.0";
            String filePath = "/firmware/strip_v1.2.0.bin";
            String checksum = "a1b2c3d4e5f6";
            long fileSize = 102400;
            String initiatedBy = "admin";

            when(firmwareRepository.findByDeviceIdAndStatus(deviceId, "PENDING"))
                    .thenReturn(Optional.empty());
            when(firmwareRepository.findByDeviceIdAndStatus(deviceId, "DOWNLOADING"))
                    .thenReturn(Optional.empty());
            when(firmwareRepository.findFirstByDeviceIdOrderByCreatedAtDesc(deviceId))
                    .thenReturn(Optional.empty());
            when(firmwareRepository.save(any(DeviceFirmware.class))).thenAnswer(invocation -> {
                DeviceFirmware firmware = invocation.getArgument(0);
                firmware.setId(1L);
                return firmware;
            });

            // When
            DeviceFirmware result = firmwareService.initiateUpgrade(
                    deviceId, version, filePath, checksum, fileSize, initiatedBy);

            // Then
            assertNotNull(result);
            assertEquals(deviceId, result.getDeviceId());
            assertEquals(version, result.getVersion());
            assertEquals(filePath, result.getFilePath());
            assertEquals("PENDING", result.getStatus());
            assertEquals(initiatedBy, result.getInitiatedBy());

            verify(firmwareRepository).findByDeviceIdAndStatus(deviceId, "PENDING");
            verify(firmwareRepository).findByDeviceIdAndStatus(deviceId, "DOWNLOADING");
            verify(firmwareRepository).save(any(DeviceFirmware.class));
            verify(systemLogService).info(eq("FIRMWARE"), anyString(), eq(initiatedBy));
        }

        @Test
        @DisplayName("TC-FW-001: 发起升级时自动获取上一版本号")
        void testInitiateUpgrade_WithPreviousVersion() {
            // Given
            DeviceFirmware previousFirmware = new DeviceFirmware();
            previousFirmware.setVersion("1.1.0");
            previousFirmware.setStatus("SUCCESS");

            when(firmwareRepository.findByDeviceIdAndStatus(anyString(), eq("PENDING")))
                    .thenReturn(Optional.empty());
            when(firmwareRepository.findByDeviceIdAndStatus(anyString(), eq("DOWNLOADING")))
                    .thenReturn(Optional.empty());
            when(firmwareRepository.findFirstByDeviceIdOrderByCreatedAtDesc("strip01"))
                    .thenReturn(Optional.of(previousFirmware));
            when(firmwareRepository.save(any(DeviceFirmware.class))).thenAnswer(invocation -> {
                DeviceFirmware firmware = invocation.getArgument(0);
                firmware.setId(2L);
                return firmware;
            });

            // When
            DeviceFirmware result = firmwareService.initiateUpgrade(
                    "strip01", "1.2.0", "/firmware/test.bin", "checksum", 1024, "admin");

            // Then
            assertEquals("1.1.0", result.getPreviousVersion());
        }
    }

    // ==================== TC-FW-002: 重复发起升级 ====================

    @Nested
    @DisplayName("TC-FW-002: 重复发起升级")
    class DuplicateUpgradeTests {

        @Test
        @DisplayName("TC-FW-002: 已有待处理升级，返回400，提示已有待处理升级")
        void testInitiateUpgrade_AlreadyPending() {
            // Given
            String deviceId = "strip01";
            DeviceFirmware existingFirmware = new DeviceFirmware();
            existingFirmware.setId(1L);
            existingFirmware.setDeviceId(deviceId);
            existingFirmware.setStatus("PENDING");

            when(firmwareRepository.findByDeviceIdAndStatus(deviceId, "PENDING"))
                    .thenReturn(Optional.of(existingFirmware));

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> firmwareService.initiateUpgrade(
                            deviceId, "1.3.0", "/firmware/test.bin", "checksum", 1024, "admin")
            );

            assertTrue(exception.getMessage().contains("pending upgrade"));
            verify(firmwareRepository).findByDeviceIdAndStatus(deviceId, "PENDING");
            verify(firmwareRepository, never()).save(any(DeviceFirmware.class));
        }

        @Test
        @DisplayName("TC-FW-002: 设备正在升级中，返回400")
        void testInitiateUpgrade_CurrentlyUpgrading() {
            // Given
            String deviceId = "strip01";
            DeviceFirmware existingFirmware = new DeviceFirmware();
            existingFirmware.setId(1L);
            existingFirmware.setDeviceId(deviceId);
            existingFirmware.setStatus("DOWNLOADING");

            when(firmwareRepository.findByDeviceIdAndStatus(deviceId, "PENDING"))
                    .thenReturn(Optional.empty());
            when(firmwareRepository.findByDeviceIdAndStatus(deviceId, "DOWNLOADING"))
                    .thenReturn(Optional.of(existingFirmware));

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> firmwareService.initiateUpgrade(
                            deviceId, "1.3.0", "/firmware/test.bin", "checksum", 1024, "admin")
            );

            assertTrue(exception.getMessage().contains("currently upgrading"));
            verify(firmwareRepository, never()).save(any(DeviceFirmware.class));
        }
    }

    // ==================== TC-FW-003: 更新升级进度 ====================

    @Nested
    @DisplayName("TC-FW-003: 更新升级进度")
    class UpdateProgressTests {

        @Test
        @DisplayName("TC-FW-003: progress=50，返回200，进度更新成功")
        void testUpdateProgress_Success() {
            // Given
            Long firmwareId = 1L;
            int progress = 50;
            testFirmware.setStatus("DOWNLOADING");

            when(firmwareRepository.findById(firmwareId)).thenReturn(Optional.of(testFirmware));
            when(firmwareRepository.save(any(DeviceFirmware.class))).thenAnswer(invocation -> {
                DeviceFirmware firmware = invocation.getArgument(0);
                return firmware;
            });

            // When
            DeviceFirmware result = firmwareService.updateProgress(firmwareId, progress);

            // Then
            assertEquals(50, result.getProgress());
            assertEquals("DOWNLOADING", result.getStatus());

            verify(firmwareRepository).findById(firmwareId);
            verify(firmwareRepository).save(any(DeviceFirmware.class));
        }

        @Test
        @DisplayName("TC-FW-003: 进度达到100时状态变更为INSTALLING")
        void testUpdateProgress_Progress100_StatusChangesToInstalling() {
            // Given
            Long firmwareId = 1L;
            testFirmware.setStatus("DOWNLOADING");

            when(firmwareRepository.findById(firmwareId)).thenReturn(Optional.of(testFirmware));
            when(firmwareRepository.save(any(DeviceFirmware.class))).thenAnswer(invocation -> {
                DeviceFirmware firmware = invocation.getArgument(0);
                return firmware;
            });

            // When
            DeviceFirmware result = firmwareService.updateProgress(firmwareId, 100);

            // Then
            assertEquals(100, result.getProgress());
            assertEquals("INSTALLING", result.getStatus());
        }

        @Test
        @DisplayName("TC-FW-003: 进度超过100时限制为100")
        void testUpdateProgress_ProgressOver100_CappedAt100() {
            // Given
            Long firmwareId = 1L;
            testFirmware.setStatus("DOWNLOADING");

            when(firmwareRepository.findById(firmwareId)).thenReturn(Optional.of(testFirmware));
            when(firmwareRepository.save(any(DeviceFirmware.class))).thenAnswer(invocation -> {
                DeviceFirmware firmware = invocation.getArgument(0);
                return firmware;
            });

            // When
            DeviceFirmware result = firmwareService.updateProgress(firmwareId, 150);

            // Then
            assertEquals(100, result.getProgress());
        }

        @Test
        @DisplayName("TC-FW-003: 进度为负数时限制为0")
        void testUpdateProgress_ProgressNegative_CappedAtZero() {
            // Given
            Long firmwareId = 1L;
            testFirmware.setStatus("DOWNLOADING");

            when(firmwareRepository.findById(firmwareId)).thenReturn(Optional.of(testFirmware));
            when(firmwareRepository.save(any(DeviceFirmware.class))).thenAnswer(invocation -> {
                DeviceFirmware firmware = invocation.getArgument(0);
                return firmware;
            });

            // When
            DeviceFirmware result = firmwareService.updateProgress(firmwareId, -10);

            // Then
            assertEquals(0, result.getProgress());
        }

        @Test
        @DisplayName("TC-FW-003: 固件记录不存在，抛出异常")
        void testUpdateProgress_NotFound() {
            // Given
            Long firmwareId = 999L;
            when(firmwareRepository.findById(firmwareId)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> firmwareService.updateProgress(firmwareId, 50)
            );

            assertTrue(exception.getMessage().contains("not found"));
        }
    }

    // ==================== TC-FW-004: 完成升级成功 ====================

    @Nested
    @DisplayName("TC-FW-004: 完成升级成功")
    class CompleteUpgradeSuccessTests {

        @Test
        @DisplayName("TC-FW-004: success=true，状态更新为SUCCESS")
        void testCompleteUpgrade_Success() {
            // Given
            Long firmwareId = 1L;
            testFirmware.setStatus("INSTALLING");

            when(firmwareRepository.findById(firmwareId)).thenReturn(Optional.of(testFirmware));
            when(firmwareRepository.save(any(DeviceFirmware.class))).thenAnswer(invocation -> {
                DeviceFirmware firmware = invocation.getArgument(0);
                return firmware;
            });

            // When
            DeviceFirmware result = firmwareService.completeUpgrade(firmwareId, true, null);

            // Then
            assertEquals("SUCCESS", result.getStatus());
            assertEquals(100, result.getProgress());
            assertTrue(result.getCompletedAt() > 0);
            assertNull(result.getErrorMessage());

            verify(firmwareRepository).findById(firmwareId);
            verify(firmwareRepository).save(any(DeviceFirmware.class));
            verify(systemLogService).info(eq("FIRMWARE"), anyString(), anyString());
            verify(notificationService).createSystemNotification(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("TC-FW-004: 完成升级后记录日志并发送通知")
        void testCompleteUpgrade_Success_LoggingAndNotification() {
            // Given
            testFirmware.setInitiatedBy("admin");
            testFirmware.setStatus("INSTALLING");

            when(firmwareRepository.findById(1L)).thenReturn(Optional.of(testFirmware));
            when(firmwareRepository.save(any(DeviceFirmware.class))).thenReturn(testFirmware);

            // When
            firmwareService.completeUpgrade(1L, true, null);

            // Then
            verify(systemLogService).info(
                    eq("FIRMWARE"),
                    contains("completed"),
                    eq("admin")
            );
            verify(notificationService).createSystemNotification(
                    eq("固件升级成功"),
                    contains("成功升级"),
                    eq("admin")
            );
        }
    }

    // ==================== TC-FW-005: 完成升级失败 ====================

    @Nested
    @DisplayName("TC-FW-005: 完成升级失败")
    class CompleteUpgradeFailedTests {

        @Test
        @DisplayName("TC-FW-005: success=false，状态更新为FAILED，记录错误信息")
        void testCompleteUpgrade_Failed() {
            // Given
            Long firmwareId = 1L;
            String errorMessage = "Download failed: connection timeout";
            testFirmware.setStatus("DOWNLOADING");

            when(firmwareRepository.findById(firmwareId)).thenReturn(Optional.of(testFirmware));
            when(firmwareRepository.save(any(DeviceFirmware.class))).thenAnswer(invocation -> {
                DeviceFirmware firmware = invocation.getArgument(0);
                return firmware;
            });

            // When
            DeviceFirmware result = firmwareService.completeUpgrade(firmwareId, false, errorMessage);

            // Then
            assertEquals("FAILED", result.getStatus());
            assertEquals(errorMessage, result.getErrorMessage());
            assertTrue(result.getCompletedAt() > 0);

            verify(firmwareRepository).findById(firmwareId);
            verify(firmwareRepository).save(any(DeviceFirmware.class));
        }

        @Test
        @DisplayName("TC-FW-005: 升级失败后记录错误日志并发送告警通知")
        void testCompleteUpgrade_Failed_LoggingAndNotification() {
            // Given
            String errorMessage = "Installation failed: checksum mismatch";
            testFirmware.setInitiatedBy("admin");
            testFirmware.setStatus("INSTALLING");

            when(firmwareRepository.findById(1L)).thenReturn(Optional.of(testFirmware));
            when(firmwareRepository.save(any(DeviceFirmware.class))).thenReturn(testFirmware);

            // When
            firmwareService.completeUpgrade(1L, false, errorMessage);

            // Then
            verify(systemLogService).error(
                    eq("FIRMWARE"),
                    contains("failed"),
                    eq("admin"),
                    eq(errorMessage)
            );
            verify(notificationService).createAlertNotification(
                    eq("固件升级失败"),
                    contains(errorMessage),
                    eq("admin"),
                    eq("strip01")
            );
        }

        @Test
        @DisplayName("TC-FW-005: 固件记录不存在，抛出异常")
        void testCompleteUpgrade_NotFound() {
            // Given
            Long firmwareId = 999L;
            when(firmwareRepository.findById(firmwareId)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> firmwareService.completeUpgrade(firmwareId, false, "error")
            );

            assertTrue(exception.getMessage().contains("not found"));
        }
    }

    // ==================== TC-FW-006: 查询固件历史 ====================

    @Nested
    @DisplayName("TC-FW-006: 查询固件历史")
    class GetFirmwareHistoryTests {

        @Test
        @DisplayName("TC-FW-006: 有效设备ID，返回升级历史列表")
        void testGetDeviceFirmwareHistory_Success() {
            // Given
            String deviceId = "strip01";

            DeviceFirmware firmware1 = new DeviceFirmware();
            firmware1.setId(1L);
            firmware1.setDeviceId(deviceId);
            firmware1.setVersion("1.0.0");
            firmware1.setStatus("SUCCESS");

            DeviceFirmware firmware2 = new DeviceFirmware();
            firmware2.setId(2L);
            firmware2.setDeviceId(deviceId);
            firmware2.setVersion("1.1.0");
            firmware2.setStatus("SUCCESS");

            DeviceFirmware firmware3 = new DeviceFirmware();
            firmware3.setId(3L);
            firmware3.setDeviceId(deviceId);
            firmware3.setVersion("1.2.0");
            firmware3.setStatus("PENDING");

            when(firmwareRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId))
                    .thenReturn(Arrays.asList(firmware3, firmware2, firmware1));

            // When
            var result = firmwareService.getDeviceFirmwareHistory(deviceId);

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());

            verify(firmwareRepository).findByDeviceIdOrderByCreatedAtDesc(deviceId);
        }

        @Test
        @DisplayName("TC-FW-006: 设备无升级历史，返回空列表")
        void testGetDeviceFirmwareHistory_Empty() {
            // Given
            String deviceId = "new_device";
            when(firmwareRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId))
                    .thenReturn(Collections.emptyList());

            // When
            var result = firmwareService.getDeviceFirmwareHistory(deviceId);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("TC-FW-006: 获取当前固件版本")
        void testGetCurrentFirmware_Success() {
            // Given
            String deviceId = "strip01";
            DeviceFirmware latestSuccess = new DeviceFirmware();
            latestSuccess.setId(2L);
            latestSuccess.setDeviceId(deviceId);
            latestSuccess.setVersion("1.1.0");
            latestSuccess.setStatus("SUCCESS");

            when(firmwareRepository.findFirstByDeviceIdOrderByCreatedAtDesc(deviceId))
                    .thenReturn(Optional.of(latestSuccess));

            // When
            var result = firmwareService.getCurrentFirmware(deviceId);

            // Then
            assertTrue(result.isPresent());
            assertEquals("1.1.0", result.get().getVersion());
        }

        @Test
        @DisplayName("TC-FW-006: 最新固件非成功状态，不返回当前版本")
        void testGetCurrentFirmware_NoSuccessfulFirmware() {
            // Given
            String deviceId = "strip01";
            DeviceFirmware pendingFirmware = new DeviceFirmware();
            pendingFirmware.setId(1L);
            pendingFirmware.setDeviceId(deviceId);
            pendingFirmware.setVersion("1.2.0");
            pendingFirmware.setStatus("PENDING");

            when(firmwareRepository.findFirstByDeviceIdOrderByCreatedAtDesc(deviceId))
                    .thenReturn(Optional.of(pendingFirmware));

            // When
            var result = firmwareService.getCurrentFirmware(deviceId);

            // Then
            assertFalse(result.isPresent());
        }
    }

    // ==================== 其他辅助测试 ====================

    @Nested
    @DisplayName("其他功能测试")
    class OtherTests {

        @Test
        @DisplayName("获取待处理升级列表")
        void testGetPendingUpgrades() {
            // Given
            DeviceFirmware pending1 = new DeviceFirmware();
            pending1.setStatus("PENDING");

            when(firmwareRepository.findByStatusOrderByCreatedAtDesc("PENDING"))
                    .thenReturn(Arrays.asList(pending1));

            // When
            var result = firmwareService.getPendingUpgrades();

            // Then
            assertEquals(1, result.size());
            verify(firmwareRepository).findByStatusOrderByCreatedAtDesc("PENDING");
        }

        @Test
        @DisplayName("取消升级成功")
        void testCancelUpgrade_Success() {
            // Given
            testFirmware.setStatus("DOWNLOADING");

            when(firmwareRepository.findById(1L)).thenReturn(Optional.of(testFirmware));
            when(firmwareRepository.save(any(DeviceFirmware.class))).thenReturn(testFirmware);

            // When
            DeviceFirmware result = firmwareService.cancelUpgrade(1L, "User requested");

            // Then
            assertEquals("FAILED", result.getStatus());
            assertTrue(result.getErrorMessage().contains("Cancelled"));
            assertTrue(result.getErrorMessage().contains("User requested"));
        }

        @Test
        @DisplayName("取消已完成的升级，抛出异常")
        void testCancelUpgrade_AlreadyCompleted() {
            // Given
            testFirmware.setStatus("SUCCESS");

            when(firmwareRepository.findById(1L)).thenReturn(Optional.of(testFirmware));

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> firmwareService.cancelUpgrade(1L, "User requested")
            );

            assertTrue(exception.getMessage().contains("Cannot cancel completed"));
        }

        @Test
        @DisplayName("发送升级命令成功")
        void testSendUpgradeCommand_Success() throws MqttException {
            // Given
            when(firmwareRepository.findById(1L)).thenReturn(Optional.of(testFirmware));
            when(firmwareRepository.save(any(DeviceFirmware.class))).thenReturn(testFirmware);
            doNothing().when(mqttService).sendMessage(anyString(), anyString());

            // When
            boolean result = firmwareService.sendUpgradeCommand(1L);

            // Then
            assertTrue(result);
            verify(mqttService).sendMessage(contains("strip01"), contains("upgrade"));
        }

        @Test
        @DisplayName("发送升级命令失败-MQTT异常")
        void testSendUpgradeCommand_MqttException() throws MqttException {
            // Given
            when(firmwareRepository.findById(1L)).thenReturn(Optional.of(testFirmware));
            doThrow(new MqttException(1)).when(mqttService).sendMessage(anyString(), anyString());

            // When
            boolean result = firmwareService.sendUpgradeCommand(1L);

            // Then
            assertFalse(result);
            verify(systemLogService).error(eq("FIRMWARE"), anyString(), anyString(), anyString());
        }
    }
}