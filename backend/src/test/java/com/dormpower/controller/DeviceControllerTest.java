package com.dormpower.controller;

import com.dormpower.config.TestSecurityConfig;
import com.dormpower.model.Device;
import com.dormpower.model.StripStatus;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.StripStatusRepository;
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

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 设备状态查询单元测试
 *
 * 测试用例覆盖：
 * - TC-STATUS-001: 查询在线设备状态
 * - TC-STATUS-002: 查询离线设备状态
 * - TC-STATUS-003: 查询不存在设备
 * - TC-STATUS-004: 插座状态解析异常
 *
 * @author dormpower team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceRepository deviceRepository;

    @MockBean
    private StripStatusRepository stripStatusRepository;

    
    // 测试数据常量
    private static final String ONLINE_DEVICE_ID = "device-001";
    private static final String OFFLINE_DEVICE_ID = "device-002";
    private static final String NON_EXISTENT_DEVICE_ID = "device-999";
    private static final String MALFORMED_JSON_DEVICE_ID = "device-003";

    /**
     * 设备状态查询测试用例
     */
    @Nested
    @DisplayName("设备状态查询测试")
    class DeviceStatusQueryTests {

        /**
         * 初始化测试数据
         */
        @BeforeEach
        void setUp() {
            // 设置在线设备数据
            Device onlineDevice = createOnlineDevice();
            when(deviceRepository.findById(ONLINE_DEVICE_ID)).thenReturn(Optional.of(onlineDevice));

            StripStatus onlineStatus = createOnlineStripStatus();
            when(stripStatusRepository.findByDeviceId(ONLINE_DEVICE_ID)).thenReturn(onlineStatus);

            // 设置离线设备数据
            Device offlineDevice = createOfflineDevice();
            when(deviceRepository.findById(OFFLINE_DEVICE_ID)).thenReturn(Optional.of(offlineDevice));

            StripStatus offlineStatus = createOfflineStripStatus();
            when(stripStatusRepository.findByDeviceId(OFFLINE_DEVICE_ID)).thenReturn(offlineStatus);

            // 设置不存在设备
            when(deviceRepository.findById(NON_EXISTENT_DEVICE_ID)).thenReturn(Optional.empty());
            when(stripStatusRepository.findByDeviceId(NON_EXISTENT_DEVICE_ID)).thenReturn(null);

            // 设置格式错误JSON设备
            Device malformedDevice = createOnlineDevice();
            malformedDevice.setId(MALFORMED_JSON_DEVICE_ID);
            when(deviceRepository.findById(MALFORMED_JSON_DEVICE_ID)).thenReturn(Optional.of(malformedDevice));

            StripStatus malformedStatus = createMalformedJsonStripStatus();
            when(stripStatusRepository.findByDeviceId(MALFORMED_JSON_DEVICE_ID)).thenReturn(malformedStatus);
        }

        /**
         * TC-STATUS-001: 查询在线设备状态
         *
         * 测试场景：查询在线设备状态
         * 输入：有效设备ID
         * 预期输出：返回完整状态信息，online=true
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-STATUS-001: 查询在线设备状态-返回完整状态信息")
        void testGetOnlineDeviceStatus() throws Exception {
            mockMvc.perform(get("/api/devices/{deviceId}/status", ONLINE_DEVICE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.online").value(true))
                    .andExpect(jsonPath("$.total_power_w").value(120.5))
                    .andExpect(jsonPath("$.voltage_v").value(220.0))
                    .andExpect(jsonPath("$.current_a").value(0.55))
                    .andExpect(jsonPath("$.sockets").isArray())
                    .andExpect(jsonPath("$.sockets[0].id").value(1))
                    .andExpect(jsonPath("$.sockets[0].on").value(true))
                    .andExpect(jsonPath("$.sockets[0].power_w").value(50.0));
        }

        /**
         * TC-STATUS-002: 查询离线设备状态
         *
         * 测试场景：查询离线设备状态
         * 输入：离线设备ID
         * 预期输出：返回状态信息，online=false
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-STATUS-002: 查询离线设备状态-返回online=false")
        void testGetOfflineDeviceStatus() throws Exception {
            mockMvc.perform(get("/api/devices/{deviceId}/status", OFFLINE_DEVICE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.online").value(false))
                    .andExpect(jsonPath("$.total_power_w").value(0.0))
                    .andExpect(jsonPath("$.voltage_v").value(0.0))
                    .andExpect(jsonPath("$.current_a").value(0.0));
        }

        /**
         * TC-STATUS-003: 查询不存在设备
         *
         * 测试场景：查询不存在设备
         * 输入：无效设备ID
         * 预期输出：返回404
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-STATUS-003: 查询不存在设备-返回404")
        void testGetNonExistentDeviceStatus() throws Exception {
            mockMvc.perform(get("/api/devices/{deviceId}/status", NON_EXISTENT_DEVICE_ID))
                    .andExpect(status().isNotFound());
        }

        /**
         * TC-STATUS-004: 插座状态解析异常
         *
         * 测试场景：插座状态解析异常
         * 输入：socketsJson格式错误
         * 预期输出：返回空插座列表
         * 优先级：P1
         */
        @Test
        @DisplayName("TC-STATUS-004: 插座状态解析异常-返回空插座列表")
        void testMalformedSocketsJson() throws Exception {
            mockMvc.perform(get("/api/devices/{deviceId}/status", MALFORMED_JSON_DEVICE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.online").value(true))
                    .andExpect(jsonPath("$.sockets").isArray())
                    .andExpect(jsonPath("$.sockets").isEmpty());
        }
    }

    /**
     * 设备列表查询测试用例
     */
    @Nested
    @DisplayName("设备列表查询测试")
    class DeviceListTests {

        @Test
        @DisplayName("获取设备列表成功")
        void testGetDevices() throws Exception {
            // 设置设备列表数据
            Device device1 = createOnlineDevice();
            Device device2 = createOfflineDevice();
            when(deviceRepository.findAll()).thenReturn(java.util.List.of(device1, device2));

            mockMvc.perform(get("/api/devices"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    // ==================== 辅助方法：创建测试数据 ====================

    /**
     * 创建在线设备实体
     */
    private Device createOnlineDevice() {
        Device device = new Device();
        device.setId(ONLINE_DEVICE_ID);
        device.setName("宿舍101插座");
        device.setRoom("101");
        device.setOnline(true);
        device.setLastSeenTs(System.currentTimeMillis() / 1000);
        device.setCreatedAt(System.currentTimeMillis() / 1000 - 86400);
        return device;
    }

    /**
     * 创建离线设备实体
     */
    private Device createOfflineDevice() {
        Device device = new Device();
        device.setId(OFFLINE_DEVICE_ID);
        device.setName("宿舍102插座");
        device.setRoom("102");
        device.setOnline(false);
        device.setLastSeenTs(System.currentTimeMillis() / 1000 - 3600);
        device.setCreatedAt(System.currentTimeMillis() / 1000 - 86400);
        return device;
    }

    /**
     * 创建在线插座状态实体
     */
    private StripStatus createOnlineStripStatus() {
        StripStatus status = new StripStatus();
        status.setDeviceId(ONLINE_DEVICE_ID);
        status.setTs(System.currentTimeMillis() / 1000);
        status.setOnline(true);
        status.setTotalPowerW(120.5);
        status.setVoltageV(220.0);
        status.setCurrentA(0.55);
        // 正常的插座JSON数据
        status.setSocketsJson("[{\"id\":1,\"on\":true,\"power_w\":50.0},{\"id\":2,\"on\":false,\"power_w\":0.0}]");
        return status;
    }

    /**
     * 创建离线插座状态实体
     */
    private StripStatus createOfflineStripStatus() {
        StripStatus status = new StripStatus();
        status.setDeviceId(OFFLINE_DEVICE_ID);
        status.setTs(System.currentTimeMillis() / 1000 - 3600);
        status.setOnline(false);
        status.setTotalPowerW(0.0);
        status.setVoltageV(0.0);
        status.setCurrentA(0.0);
        status.setSocketsJson("[]");
        return status;
    }

    /**
     * 创建格式错误的插座状态实体
     */
    private StripStatus createMalformedJsonStripStatus() {
        StripStatus status = new StripStatus();
        status.setDeviceId(MALFORMED_JSON_DEVICE_ID);
        status.setTs(System.currentTimeMillis() / 1000);
        status.setOnline(true);
        status.setTotalPowerW(100.0);
        status.setVoltageV(220.0);
        status.setCurrentA(0.45);
        // 格式错误的JSON
        status.setSocketsJson("{invalid json format}");
        return status;
    }
}