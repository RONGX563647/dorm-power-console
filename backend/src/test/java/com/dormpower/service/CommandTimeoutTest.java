package com.dormpower.service;

import com.dormpower.model.CommandRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 命令超时单元测试
 *
 * 测试用例覆盖：
 * - TC-TIMEOUT-001: 命令正常响应
 * - TC-TIMEOUT-002: 命令超时
 * - TC-TIMEOUT-003: 批量超时检测
 *
 * @author dormpower team
 * @version 1.0
 */
public class CommandTimeoutTest {

    // 测试数据常量
    private static final String CMD_ID_1 = "cmd-1234567890";
    private static final String CMD_ID_2 = "cmd-1234567891";
    private static final String CMD_ID_3 = "cmd-1234567892";
    private static final String DEVICE_ID_1 = "device-001";
    private static final String DEVICE_ID_2 = "device-002";
    private static final String DEVICE_ID_3 = "device-003";

    // 被测对象
    private TestableCommandService commandService;

    // 测试替身
    private SimpleCommandRecordStore commandRecordStore;
    private SimpleDeviceStore deviceStore;

    @BeforeEach
    void setUp() {
        commandRecordStore = new SimpleCommandRecordStore();
        deviceStore = new SimpleDeviceStore();
        commandService = new TestableCommandService(commandRecordStore);
    }

    /**
     * 可测试的命令服务
     * 复制原服务的核心逻辑，使用测试替身
     */
    static class TestableCommandService {
        private static final String STATE_PENDING = "pending";
        private static final String STATE_SUCCESS = "success";
        private static final String STATE_FAILED = "failed";
        private static final String STATE_TIMEOUT = "timeout";

        private final SimpleCommandRecordStore commandRecordStore;

        public TestableCommandService(SimpleCommandRecordStore commandRecordStore) {
            this.commandRecordStore = commandRecordStore;
        }

        /**
         * 更新命令状态
         */
        public void updateCommandState(String cmdId, String state, String message) {
            CommandRecord cmd = commandRecordStore.findById(cmdId);
            if (cmd != null) {
                long now = System.currentTimeMillis();
                cmd.setState(state);
                cmd.setMessage(message != null ? message : "");
                cmd.setUpdatedAt(now);

                if (STATE_SUCCESS.equals(state) || STATE_FAILED.equals(state)) {
                    cmd.setDurationMs((int) (now - cmd.getCreatedAt()));
                }

                commandRecordStore.save(cmd);
            }
        }

        /**
         * 标记超时命令
         */
        public void markTimeouts() {
            long now = System.currentTimeMillis();
            List<CommandRecord> pendingCommands = commandRecordStore.findByState(STATE_PENDING);

            for (CommandRecord cmd : pendingCommands) {
                if (cmd.getExpiresAt() < now) {
                    cmd.setState(STATE_TIMEOUT);
                    cmd.setMessage("Command timed out");
                    cmd.setUpdatedAt(now);
                    commandRecordStore.save(cmd);
                }
            }
        }

        /**
         * 获取命令状态
         */
        public Map<String, Object> getCommandStatus(String cmdId) {
            CommandRecord cmd = commandRecordStore.findById(cmdId);
            if (cmd == null) {
                return null;
            }

            Map<String, Object> status = new HashMap<>();
            status.put("cmdId", cmd.getCmdId());
            status.put("state", cmd.getState());
            status.put("message", cmd.getMessage());
            status.put("updatedAt", cmd.getUpdatedAt());

            Integer duration = cmd.getDurationMs();
            if (duration != null) {
                status.put("durationMs", duration);
            }

            return status;
        }
    }

    /**
     * 命令超时测试
     */
    @Nested
    @DisplayName("命令超时测试")
    class CommandTimeoutTests {

        /**
         * TC-TIMEOUT-001: 命令正常响应
         *
         * 测试场景：命令正常响应
         * 输入：30秒内响应
         * 预期输出：状态更新为success
         * 优先级：P1
         */
        @Test
        @DisplayName("TC-TIMEOUT-001: 命令正常响应-状态更新为success")
        void testCommandNormalResponse_StatusUpdatedToSuccess() {
            // Arrange: 创建待处理命令（刚创建，未超时）
            long now = System.currentTimeMillis();
            CommandRecord pendingCmd = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "pending", now, now + 30000);
            commandRecordStore.save(pendingCmd);

            // Act: 模拟命令在30秒内成功响应
            commandService.updateCommandState(CMD_ID_1, "success", "Command executed successfully");

            // Assert: 验证状态更新为success
            CommandRecord updatedCmd = commandRecordStore.findById(CMD_ID_1);
            assertNotNull(updatedCmd, "命令应存在");
            assertEquals("success", updatedCmd.getState(), "状态应为success");
            assertEquals("Command executed successfully", updatedCmd.getMessage(), "消息应正确");
            assertNotNull(updatedCmd.getDurationMs(), "执行时长应被设置");
            assertTrue(updatedCmd.getDurationMs() >= 0, "执行时长应大于等于0");
        }

        /**
         * TC-TIMEOUT-002: 命令超时
         *
         * 测试场景：命令超时
         * 输入：30秒未响应
         * 预期输出：状态更新为timeout
         * 优先级：P1
         */
        @Test
        @DisplayName("TC-TIMEOUT-002: 命令超时-状态更新为timeout")
        void testCommandTimeout_StatusUpdatedToTimeout() {
            // Arrange: 创建已超时的待处理命令（31秒前创建）
            long now = System.currentTimeMillis();
            long createdAt = now - 31000; // 31秒前创建
            long expiresAt = createdAt + 30000; // 1秒前过期
            CommandRecord timedOutCmd = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "pending", createdAt, expiresAt);
            commandRecordStore.save(timedOutCmd);

            // Act: 执行超时检测
            commandService.markTimeouts();

            // Assert: 验证状态更新为timeout
            CommandRecord updatedCmd = commandRecordStore.findById(CMD_ID_1);
            assertNotNull(updatedCmd, "命令应存在");
            assertEquals("timeout", updatedCmd.getState(), "状态应为timeout");
            assertEquals("Command timed out", updatedCmd.getMessage(), "消息应为超时");
            assertTrue(updatedCmd.getUpdatedAt() > createdAt, "更新时间应更新");
        }

        /**
         * TC-TIMEOUT-003: 批量超时检测
         *
         * 测试场景：批量超时检测
         * 输入：多个超时命令
         * 预期输出：全部标记为timeout
         * 优先级：P2
         */
        @Test
        @DisplayName("TC-TIMEOUT-003: 批量超时检测-全部标记为timeout")
        void testBatchTimeoutDetection_AllMarkedAsTimeout() {
            // Arrange: 创建多个已超时的待处理命令
            long now = System.currentTimeMillis();

            // 命令1: 31秒前创建，已超时
            CommandRecord cmd1 = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "pending",
                    now - 31000, now - 1000);
            commandRecordStore.save(cmd1);

            // 命令2: 60秒前创建，已超时
            CommandRecord cmd2 = createCommandRecord(CMD_ID_2, DEVICE_ID_2, "on", 2, "pending",
                    now - 60000, now - 30000);
            commandRecordStore.save(cmd2);

            // 命令3: 45秒前创建，已超时
            CommandRecord cmd3 = createCommandRecord(CMD_ID_3, DEVICE_ID_3, "off", 3, "pending",
                    now - 45000, now - 15000);
            commandRecordStore.save(cmd3);

            // Act: 执行批量超时检测
            commandService.markTimeouts();

            // Assert: 验证所有超时命令都被标记为timeout
            assertEquals("timeout", commandRecordStore.findById(CMD_ID_1).getState(), "命令1应为timeout");
            assertEquals("timeout", commandRecordStore.findById(CMD_ID_2).getState(), "命令2应为timeout");
            assertEquals("timeout", commandRecordStore.findById(CMD_ID_3).getState(), "命令3应为timeout");

            // 验证消息都被设置为超时消息
            assertEquals("Command timed out", commandRecordStore.findById(CMD_ID_1).getMessage());
            assertEquals("Command timed out", commandRecordStore.findById(CMD_ID_2).getMessage());
            assertEquals("Command timed out", commandRecordStore.findById(CMD_ID_3).getMessage());
        }
    }

    /**
     * 边界条件测试
     */
    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        /**
         * 测试未超时的命令不会被标记
         */
        @Test
        @DisplayName("未超时命令-保持pending状态")
        void testNotExpiredCommand_StaysPending() {
            // Arrange: 创建未超时的待处理命令
            long now = System.currentTimeMillis();
            CommandRecord pendingCmd = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "pending",
                    now, now + 30000);
            commandRecordStore.save(pendingCmd);

            // Act: 执行超时检测
            commandService.markTimeouts();

            // Assert: 命令仍为pending状态
            assertEquals("pending", commandRecordStore.findById(CMD_ID_1).getState(), "未超时命令应保持pending");
        }

        /**
         * 测试已成功的命令不会被超时
         */
        @Test
        @DisplayName("已成功命令-不受超时检测影响")
        void testSuccessfulCommand_NotAffectedByTimeout() {
            // Arrange: 创建已成功的命令（即使是创建很久以前的）
            long now = System.currentTimeMillis();
            CommandRecord successCmd = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "success",
                    now - 60000, now - 30000);
            successCmd.setMessage("Command executed successfully");
            commandRecordStore.save(successCmd);

            // Act: 执行超时检测
            commandService.markTimeouts();

            // Assert: 成功命令不受影响
            assertEquals("success", commandRecordStore.findById(CMD_ID_1).getState(), "成功命令状态不应改变");
            assertEquals("Command executed successfully", commandRecordStore.findById(CMD_ID_1).getMessage());
        }

        /**
         * 测试已失败的命令不会被超时
         */
        @Test
        @DisplayName("已失败命令-不受超时检测影响")
        void testFailedCommand_NotAffectedByTimeout() {
            // Arrange: 创建已失败的命令
            long now = System.currentTimeMillis();
            CommandRecord failedCmd = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "failed",
                    now - 60000, now - 30000);
            failedCmd.setMessage("Device not responding");
            commandRecordStore.save(failedCmd);

            // Act: 执行超时检测
            commandService.markTimeouts();

            // Assert: 失败命令不受影响
            assertEquals("failed", commandRecordStore.findById(CMD_ID_1).getState(), "失败命令状态不应改变");
            assertEquals("Device not responding", commandRecordStore.findById(CMD_ID_1).getMessage());
        }

        /**
         * 测试混合场景：部分超时，部分未超时
         */
        @Test
        @DisplayName("混合场景-只标记超时命令")
        void testMixedScenario_OnlyTimeoutCommandsMarked() {
            // Arrange
            long now = System.currentTimeMillis();

            // 超时命令
            CommandRecord timedOutCmd = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "pending",
                    now - 35000, now - 5000);
            commandRecordStore.save(timedOutCmd);

            // 未超时命令
            CommandRecord pendingCmd = createCommandRecord(CMD_ID_2, DEVICE_ID_2, "toggle", 2, "pending",
                    now, now + 30000);
            commandRecordStore.save(pendingCmd);

            // 已成功命令
            CommandRecord successCmd = createCommandRecord(CMD_ID_3, DEVICE_ID_3, "toggle", 3, "success",
                    now - 10000, now + 20000);
            successCmd.setMessage("OK");
            commandRecordStore.save(successCmd);

            // Act: 执行超时检测
            commandService.markTimeouts();

            // Assert
            assertEquals("timeout", commandRecordStore.findById(CMD_ID_1).getState(), "超时命令应被标记");
            assertEquals("pending", commandRecordStore.findById(CMD_ID_2).getState(), "未超时命令应保持pending");
            assertEquals("success", commandRecordStore.findById(CMD_ID_3).getState(), "成功命令不受影响");
        }

        /**
         * 测试边界值：刚好超时
         */
        @Test
        @DisplayName("边界值-刚好超时")
        void testBoundary_JustExpired() {
            // Arrange: 创建刚好超时的命令（expiresAt < now）
            long now = System.currentTimeMillis();
            CommandRecord justExpiredCmd = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "pending",
                    now - 30001, now - 1); // 过期时间比当前时间早1毫秒
            commandRecordStore.save(justExpiredCmd);

            // Act: 执行超时检测
            commandService.markTimeouts();

            // Assert: 应被标记为超时
            assertEquals("timeout", commandRecordStore.findById(CMD_ID_1).getState(), "刚好超时的命令应被标记");
        }

        /**
         * 测试边界值：刚好未超时
         */
        @Test
        @DisplayName("边界值-刚好未超时")
        void testBoundary_JustNotExpired() {
            // Arrange: 创建刚好未超时的命令（expiresAt > now）
            long now = System.currentTimeMillis();
            CommandRecord justNotExpiredCmd = createCommandRecord(CMD_ID_1, DEVICE_ID_1, "toggle", 1, "pending",
                    now - 29000, now + 1000); // 过期时间比当前时间晚1毫秒
            commandRecordStore.save(justNotExpiredCmd);

            // Act: 执行超时检测
            commandService.markTimeouts();

            // Assert: 应保持pending
            assertEquals("pending", commandRecordStore.findById(CMD_ID_1).getState(), "刚好未超时的命令应保持pending");
        }

        /**
         * 测试空命令列表
         */
        @Test
        @DisplayName("空命令列表-无操作")
        void testEmptyCommandList_NoOperation() {
            // Arrange: 不添加任何命令

            // Act: 执行超时检测
            commandService.markTimeouts();

            // Assert: 不抛出异常即通过
        }
    }

    // ==================== 辅助方法：创建测试数据 ====================

    /**
     * 创建命令记录实体
     */
    private CommandRecord createCommandRecord(String cmdId, String deviceId, String action, Integer socket,
                                               String state, long createdAt, long expiresAt) {
        CommandRecord record = new CommandRecord();
        record.setCmdId(cmdId);
        record.setDeviceId(deviceId);
        record.setAction(action);
        record.setSocket(socket);
        record.setState(state);
        record.setMessage("");
        record.setCreatedAt(createdAt);
        record.setUpdatedAt(createdAt);
        record.setExpiresAt(expiresAt);
        record.setPayloadJson("{\"action\":\"" + action + "\"}");
        return record;
    }

    // ==================== 简单存储类 ====================

    /**
     * 简单命令记录存储
     */
    static class SimpleCommandRecordStore {
        private final Map<String, CommandRecord> records = new HashMap<>();

        public CommandRecord save(CommandRecord record) {
            records.put(record.getCmdId(), record);
            return record;
        }

        public CommandRecord findById(String cmdId) {
            return records.get(cmdId);
        }

        public List<CommandRecord> findByState(String state) {
            return records.values().stream()
                    .filter(r -> state.equals(r.getState()))
                    .toList();
        }

        public void clear() {
            records.clear();
        }
    }

    /**
     * 简单设备存储
     */
    static class SimpleDeviceStore {
        private final Map<String, com.dormpower.model.Device> devices = new HashMap<>();

        public com.dormpower.model.Device save(com.dormpower.model.Device device) {
            devices.put(device.getId(), device);
            return device;
        }

        public com.dormpower.model.Device findById(String deviceId) {
            return devices.get(deviceId);
        }
    }
}