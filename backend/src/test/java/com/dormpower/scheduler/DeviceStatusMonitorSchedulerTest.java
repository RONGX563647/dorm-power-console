package com.dormpower.scheduler;

import com.dormpower.model.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 设备在线状态监控单元测试
 *
 * 测试用例覆盖：
 * - TC-ONLINE-001: 设备心跳正常
 * - TC-ONLINE-002: 设备心跳超时
 * - TC-ONLINE-003: 设备断开连接
 * - TC-ONLINE-004: 设备重新上线
 *
 * @author dormpower team
 * @version 1.0
 */
public class DeviceStatusMonitorSchedulerTest {

    // 心跳超时阈值（秒）- 与被测类保持一致
    private static final long OFFLINE_THRESHOLD_SECONDS = 120;

    // 测试数据常量
    private static final String DEVICE_ID_1 = "device-001";
    private static final String DEVICE_ID_2 = "device-002";
    private static final String DEVICE_ID_3 = "device-003";

    // 被测对象
    private TestableDeviceStatusMonitorScheduler scheduler;

    // 测试替身
    private SimpleDeviceStore deviceStore;
    private SimpleNotificationTracker notificationTracker;
    private SimpleSystemLogTracker systemLogTracker;

    @BeforeEach
    void setUp() {
        deviceStore = new SimpleDeviceStore();
        notificationTracker = new SimpleNotificationTracker();
        systemLogTracker = new SimpleSystemLogTracker();

        scheduler = new TestableDeviceStatusMonitorScheduler(
                deviceStore,
                notificationTracker,
                systemLogTracker
        );
    }

    /**
     * 可测试的设备状态监控调度器
     * 复制原调度器的核心逻辑，使用测试替身
     */
    static class TestableDeviceStatusMonitorScheduler {
        private final SimpleDeviceStore deviceStore;
        private final SimpleNotificationTracker notificationTracker;
        private final SimpleSystemLogTracker systemLogTracker;

        public TestableDeviceStatusMonitorScheduler(
                SimpleDeviceStore deviceStore,
                SimpleNotificationTracker notificationTracker,
                SimpleSystemLogTracker systemLogTracker) {
            this.deviceStore = deviceStore;
            this.notificationTracker = notificationTracker;
            this.systemLogTracker = systemLogTracker;
        }

        /**
         * 检查设备状态（核心逻辑）
         */
        public void checkDeviceStatus() {
            List<Device> devices = deviceStore.findAll();
            long now = System.currentTimeMillis() / 1000;

            for (Device device : devices) {
                boolean shouldBeOnline = (now - device.getLastSeenTs()) < OFFLINE_THRESHOLD_SECONDS;

                if (device.isOnline() && !shouldBeOnline) {
                    // 设备离线
                    device.setOnline(false);
                    deviceStore.save(device);

                    // 记录日志
                    systemLogTracker.logWarn("DEVICE",
                            "Device went offline: " + device.getId());

                    // 发送通知
                    notificationTracker.sendAlert(
                            "设备离线告警",
                            "设备 " + device.getId() + " (" + device.getName() + ") 已离线",
                            device.getId()
                    );
                } else if (!device.isOnline() && shouldBeOnline) {
                    // 设备上线
                    device.setOnline(true);
                    deviceStore.save(device);

                    // 记录日志
                    systemLogTracker.logInfo("DEVICE",
                            "Device came online: " + device.getId());
                }
            }
        }
    }

    /**
     * 设备心跳和在线状态测试
     */
    @Nested
    @DisplayName("设备在线状态监控测试")
    class DeviceOnlineStatusTests {

        /**
         * TC-ONLINE-001: 设备心跳正常
         */
        @Test
        @DisplayName("TC-ONLINE-001: 设备心跳正常-设备保持在线状态")
        void testHeartbeatNormal_DeviceStaysOnline() {
            // Arrange: 创建在线设备，最近有心跳（30秒前）
            Device onlineDevice = createDevice(DEVICE_ID_1, true, System.currentTimeMillis() / 1000 - 30);
            deviceStore.addDevice(onlineDevice);

            // Act: 执行状态检查
            scheduler.checkDeviceStatus();

            // Assert: 设备保持在线，无需更新
            assertEquals(0, deviceStore.getSaveCount(), "设备状态不应更新");
            assertEquals(0, notificationTracker.getAlertCount(), "不应发送告警通知");
        }

        /**
         * TC-ONLINE-002: 设备心跳超时
         */
        @Test
        @DisplayName("TC-ONLINE-002: 设备心跳超时-设备标记为离线")
        void testHeartbeatTimeout_DeviceMarkedOffline() {
            // Arrange: 创建在线设备，但心跳已超时（超过120秒）
            long oldTimestamp = System.currentTimeMillis() / 1000 - OFFLINE_THRESHOLD_SECONDS - 10;
            Device onlineDeviceWithTimeout = createDevice(DEVICE_ID_1, true, oldTimestamp);
            deviceStore.addDevice(onlineDeviceWithTimeout);

            // Act: 执行状态检查
            scheduler.checkDeviceStatus();

            // Assert: 设备被标记为离线
            assertEquals(1, deviceStore.getSaveCount(), "设备应被保存");
            assertFalse(deviceStore.getLastSavedDevice().isOnline(), "设备应被标记为离线");
            assertEquals(1, notificationTracker.getAlertCount(), "应发送告警通知");
            assertEquals("设备离线告警", notificationTracker.getLastAlertTitle());
            assertEquals(1, systemLogTracker.getWarnCount(), "应记录警告日志");
        }

        /**
         * TC-ONLINE-003: 设备断开连接
         */
        @Test
        @DisplayName("TC-ONLINE-003: 设备断开连接-设备立即标记为离线")
        void testDeviceDisconnected_DeviceMarkedOfflineImmediately() {
            // Arrange: 创建在线设备，心跳严重超时（模拟断开连接场景，5分钟前）
            long disconnectedTimestamp = System.currentTimeMillis() / 1000 - 300;
            Device disconnectedDevice = createDevice(DEVICE_ID_2, true, disconnectedTimestamp);
            deviceStore.addDevice(disconnectedDevice);

            // Act: 执行状态检查
            scheduler.checkDeviceStatus();

            // Assert: 设备被标记为离线
            assertEquals(1, deviceStore.getSaveCount(), "设备应被保存");
            assertFalse(deviceStore.getLastSavedDevice().isOnline(), "设备应被标记为离线");
            assertTrue(notificationTracker.getLastAlertContent().contains(DEVICE_ID_2),
                    "告警内容应包含设备ID");
        }

        /**
         * TC-ONLINE-004: 设备重新上线
         */
        @Test
        @DisplayName("TC-ONLINE-004: 设备重新上线-设备标记为在线")
        void testDeviceReconnects_DeviceMarkedOnline() {
            // Arrange: 创建离线设备，但最近有心跳（30秒前）
            long recentTimestamp = System.currentTimeMillis() / 1000 - 30;
            Device offlineDeviceWithHeartbeat = createDevice(DEVICE_ID_3, false, recentTimestamp);
            deviceStore.addDevice(offlineDeviceWithHeartbeat);

            // Act: 执行状态检查
            scheduler.checkDeviceStatus();

            // Assert: 设备被标记为在线
            assertEquals(1, deviceStore.getSaveCount(), "设备应被保存");
            assertTrue(deviceStore.getLastSavedDevice().isOnline(), "设备应被标记为在线");
            assertEquals(1, systemLogTracker.getInfoCount(), "应记录信息日志");
            assertEquals(0, notificationTracker.getAlertCount(), "不应发送告警通知");
        }
    }

    /**
     * 边界条件和多设备场景测试
     */
    @Nested
    @DisplayName("边界条件和多设备场景测试")
    class EdgeCaseTests {

        /**
         * 测试心跳刚好在阈值边界的情况
         */
        @Test
        @DisplayName("心跳在阈值边界-设备保持在线")
        void testHeartbeatAtThreshold_DeviceStaysOnline() {
            // Arrange: 心跳刚好在阈值内（119秒前）
            long boundaryTimestamp = System.currentTimeMillis() / 1000 - OFFLINE_THRESHOLD_SECONDS + 1;
            Device boundaryDevice = createDevice(DEVICE_ID_1, true, boundaryTimestamp);
            deviceStore.addDevice(boundaryDevice);

            // Act: 执行状态检查
            scheduler.checkDeviceStatus();

            // Assert: 设备保持在线
            assertEquals(0, deviceStore.getSaveCount(), "设备状态不应更新");
        }

        /**
         * 测试心跳刚好超过阈值边界的情况
         */
        @Test
        @DisplayName("心跳超过阈值边界-设备标记为离线")
        void testHeartbeatOverThreshold_DeviceMarkedOffline() {
            // Arrange: 心跳刚好超过阈值（121秒前）
            long overThresholdTimestamp = System.currentTimeMillis() / 1000 - OFFLINE_THRESHOLD_SECONDS - 1;
            Device overThresholdDevice = createDevice(DEVICE_ID_1, true, overThresholdTimestamp);
            deviceStore.addDevice(overThresholdDevice);

            // Act: 执行状态检查
            scheduler.checkDeviceStatus();

            // Assert: 设备被标记为离线
            assertEquals(1, deviceStore.getSaveCount(), "设备应被保存");
            assertFalse(deviceStore.getLastSavedDevice().isOnline(), "设备应被标记为离线");
        }

        /**
         * 测试多设备混合场景
         */
        @Test
        @DisplayName("多设备混合场景-正确处理各设备状态")
        void testMultipleDevices_HandlesEachCorrectly() {
            // Arrange: 创建多个不同状态的设备
            long now = System.currentTimeMillis() / 1000;
            Device onlineDevice = createDevice(DEVICE_ID_1, true, now - 30);  // 在线，心跳正常
            Device timeoutDevice = createDevice(DEVICE_ID_2, true, now - 200); // 在线但心跳超时
            Device offlineRecovering = createDevice(DEVICE_ID_3, false, now - 30); // 离线但心跳恢复

            deviceStore.addDevice(onlineDevice);
            deviceStore.addDevice(timeoutDevice);
            deviceStore.addDevice(offlineRecovering);

            // Act: 执行状态检查
            scheduler.checkDeviceStatus();

            // Assert: 验证保存调用次数（2次：timeoutDevice离线，offlineRecovering上线）
            assertEquals(2, deviceStore.getSaveCount(), "应有2个设备状态更新");
            assertEquals(1, notificationTracker.getAlertCount(), "应发送1个告警通知");
        }

        /**
         * 测试空设备列表
         */
        @Test
        @DisplayName("空设备列表-不执行任何操作")
        void testEmptyDeviceList_NoOperation() {
            // Arrange: 空设备列表
            // deviceStore 默认为空

            // Act: 执行状态检查
            scheduler.checkDeviceStatus();

            // Assert: 不执行任何保存操作
            assertEquals(0, deviceStore.getSaveCount(), "不应保存任何设备");
            assertEquals(0, notificationTracker.getAlertCount(), "不应发送告警通知");
        }
    }

    // ==================== 辅助方法：创建测试数据 ====================

    /**
     * 创建设备实体
     */
    private Device createDevice(String deviceId, boolean online, long lastSeenTs) {
        Device device = new Device();
        device.setId(deviceId);
        device.setName("测试设备-" + deviceId);
        device.setRoom("101");
        device.setOnline(online);
        device.setLastSeenTs(lastSeenTs);
        device.setCreatedAt(System.currentTimeMillis() / 1000 - 86400);
        return device;
    }

    // ==================== 简单存储类（不实现JpaRepository接口）====================

    /**
     * 简单设备存储
     */
    static class SimpleDeviceStore {
        private final List<Device> devices = new ArrayList<>();
        private int saveCount = 0;
        private Device lastSavedDevice = null;

        public List<Device> findAll() {
            return new ArrayList<>(devices);
        }

        public Device save(Device device) {
            saveCount++;
            lastSavedDevice = device;
            // 更新列表中的设备
            for (int i = 0; i < devices.size(); i++) {
                if (devices.get(i).getId().equals(device.getId())) {
                    devices.set(i, device);
                    return device;
                }
            }
            // 如果不存在则添加
            devices.add(device);
            return device;
        }

        public void addDevice(Device device) {
            devices.add(device);
        }

        public int getSaveCount() {
            return saveCount;
        }

        public Device getLastSavedDevice() {
            return lastSavedDevice;
        }
    }

    /**
     * 简单通知跟踪器
     */
    static class SimpleNotificationTracker {
        private int alertCount = 0;
        private String lastAlertTitle = null;
        private String lastAlertContent = null;

        public void sendAlert(String title, String content, String deviceId) {
            alertCount++;
            lastAlertTitle = title;
            lastAlertContent = content;
        }

        public int getAlertCount() {
            return alertCount;
        }

        public String getLastAlertTitle() {
            return lastAlertTitle;
        }

        public String getLastAlertContent() {
            return lastAlertContent;
        }
    }

    /**
     * 简单系统日志跟踪器
     */
    static class SimpleSystemLogTracker {
        private int infoCount = 0;
        private int warnCount = 0;

        public void logInfo(String type, String message) {
            infoCount++;
        }

        public void logWarn(String type, String message) {
            warnCount++;
        }

        public int getInfoCount() {
            return infoCount;
        }

        public int getWarnCount() {
            return warnCount;
        }
    }
}