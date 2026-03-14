package com.dormpower.controller;

import com.dormpower.config.TestCacheConfig;
import com.dormpower.config.TestSecurityConfig;
import com.dormpower.model.DeviceAlertConfig;
import com.dormpower.repository.DeviceAlertConfigRepository;
import com.dormpower.repository.DeviceAlertRepository;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.service.WebSocketNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 告警配置单元测试
 *
 * 测试用例覆盖：
 * - TC-CFG-001: 获取告警配置 - 有效设备ID
 * - TC-CFG-002: 更新告警配置 - 有效配置数据
 * - TC-CFG-003: 设备不存在 - 无效设备ID
 * - TC-CFG-004: 阈值范围错误 - thresholdMin > thresholdMax
 *
 * @author dormpower team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestCacheConfig.class})
public class AlertConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceRepository deviceRepository;

    @MockBean
    private DeviceAlertConfigRepository deviceAlertConfigRepository;

    @MockBean
    private DeviceAlertRepository deviceAlertRepository;

    @MockBean
    private WebSocketNotificationService webSocketNotificationService;

    @Autowired
    private ObjectMapper objectMapper;

    // 测试数据常量
    private static final String VALID_DEVICE_ID = "device-001";
    private static final String INVALID_DEVICE_ID = "device-999";
    private static final String POWER_TYPE = "power";
    private static final String VOLTAGE_TYPE = "voltage";

    /**
     * 告警配置测试用例
     */
    @Nested
    @DisplayName("告警配置测试")
    class AlertConfigTests {

        /**
         * 初始化测试数据
         */
        @BeforeEach
        void setUp() {
            // 设置设备存在
            when(deviceRepository.existsById(VALID_DEVICE_ID)).thenReturn(true);
            when(deviceRepository.existsById(INVALID_DEVICE_ID)).thenReturn(false);

            // 设置告警配置列表
            List<DeviceAlertConfig> configs = createAlertConfigs();
            when(deviceAlertConfigRepository.findByDeviceId(VALID_DEVICE_ID)).thenReturn(configs);

            // 设置保存配置时的行为
            when(deviceAlertConfigRepository.save(any(DeviceAlertConfig.class))).thenAnswer(invocation -> {
                DeviceAlertConfig config = invocation.getArgument(0);
                return config;
            });

            // 设置根据设备ID和类型查找配置
            when(deviceAlertConfigRepository.findByDeviceIdAndType(VALID_DEVICE_ID, POWER_TYPE))
                    .thenReturn(createPowerAlertConfig());
            when(deviceAlertConfigRepository.findByDeviceIdAndType(VALID_DEVICE_ID, VOLTAGE_TYPE))
                    .thenReturn(createVoltageAlertConfig());
        }

        /**
         * TC-CFG-001: 获取告警配置
         *
         * 测试场景：获取告警配置
         * 输入：有效设备ID
         * 预期输出：返回配置列表
         * 优先级：P1
         */
        @Test
        @DisplayName("TC-CFG-001: 获取告警配置-有效设备ID返回配置列表")
        void testGetAlertConfigsWithValidDeviceId() throws Exception {
            mockMvc.perform(get("/api/alerts/config/{deviceId}", VALID_DEVICE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].deviceId").value(VALID_DEVICE_ID))
                    .andExpect(jsonPath("$[0].type").value(POWER_TYPE))
                    .andExpect(jsonPath("$[0].thresholdMin").value(10.0))
                    .andExpect(jsonPath("$[0].thresholdMax").value(2000.0))
                    .andExpect(jsonPath("$[0].enabled").value(true))
                    .andExpect(jsonPath("$[1].type").value(VOLTAGE_TYPE));
        }

        /**
         * TC-CFG-002: 更新告警配置
         *
         * 测试场景：更新告警配置
         * 输入：有效配置数据
         * 预期输出：配置更新成功
         * 优先级：P1
         */
        @Test
        @DisplayName("TC-CFG-002: 更新告警配置-有效配置数据更新成功")
        void testUpdateAlertConfigWithValidData() throws Exception {
            // 构造请求体
            AlertConfigRequest request = new AlertConfigRequest();
            request.setType(POWER_TYPE);
            request.setThresholdMin(20.0);
            request.setThresholdMax(2500.0);
            request.setEnabled(true);

            mockMvc.perform(put("/api/alerts/config/{deviceId}", VALID_DEVICE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deviceId").value(VALID_DEVICE_ID))
                    .andExpect(jsonPath("$.type").value(POWER_TYPE))
                    .andExpect(jsonPath("$.thresholdMin").value(20.0))
                    .andExpect(jsonPath("$.thresholdMax").value(2500.0))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        /**
         * TC-CFG-003: 设备不存在
         *
         * 测试场景：设备不存在
         * 输入：无效设备ID
         * 预期输出：返回404错误
         * 优先级：P1
         */
        @Test
        @DisplayName("TC-CFG-003: 设备不存在-返回404错误")
        void testGetAlertConfigsWithInvalidDeviceId() throws Exception {
            mockMvc.perform(get("/api/alerts/config/{deviceId}", INVALID_DEVICE_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists());
        }

        /**
         * TC-CFG-004: 阈值范围错误
         *
         * 测试场景：阈值范围错误
         * 输入：thresholdMin > thresholdMax
         * 预期输出：返回400错误
         * 优先级：P1
         */
        @Test
        @DisplayName("TC-CFG-004: 阈值范围错误-返回400错误")
        void testUpdateAlertConfigWithInvalidThresholdRange() throws Exception {
            // 构造请求体：thresholdMin > thresholdMax
            AlertConfigRequest request = new AlertConfigRequest();
            request.setType(POWER_TYPE);
            request.setThresholdMin(3000.0);  // 大于thresholdMax
            request.setThresholdMax(2000.0);
            request.setEnabled(true);

            mockMvc.perform(put("/api/alerts/config/{deviceId}", VALID_DEVICE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    // ==================== 辅助方法：创建测试数据 ====================

    /**
     * 创建告警配置列表
     */
    private List<DeviceAlertConfig> createAlertConfigs() {
        List<DeviceAlertConfig> configs = new ArrayList<>();
        configs.add(createPowerAlertConfig());
        configs.add(createVoltageAlertConfig());
        return configs;
    }

    /**
     * 创建功率告警配置
     */
    private DeviceAlertConfig createPowerAlertConfig() {
        DeviceAlertConfig config = new DeviceAlertConfig();
        config.setId("config-001");
        config.setDeviceId(VALID_DEVICE_ID);
        config.setType(POWER_TYPE);
        config.setThresholdMin(10.0);
        config.setThresholdMax(2000.0);
        config.setEnabled(true);
        config.setCreatedAt(System.currentTimeMillis() / 1000 - 86400);
        config.setUpdatedAt(System.currentTimeMillis() / 1000);
        return config;
    }

    /**
     * 创建电压告警配置
     */
    private DeviceAlertConfig createVoltageAlertConfig() {
        DeviceAlertConfig config = new DeviceAlertConfig();
        config.setId("config-002");
        config.setDeviceId(VALID_DEVICE_ID);
        config.setType(VOLTAGE_TYPE);
        config.setThresholdMin(198.0);
        config.setThresholdMax(242.0);
        config.setEnabled(true);
        config.setCreatedAt(System.currentTimeMillis() / 1000 - 86400);
        config.setUpdatedAt(System.currentTimeMillis() / 1000);
        return config;
    }

    /**
     * 告警配置请求DTO（与AlertController中的内部类一致）
     */
    public static class AlertConfigRequest {
        private String type;
        private double thresholdMin;
        private double thresholdMax;
        private boolean enabled;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public double getThresholdMin() {
            return thresholdMin;
        }

        public void setThresholdMin(double thresholdMin) {
            this.thresholdMin = thresholdMin;
        }

        public double getThresholdMax() {
            return thresholdMax;
        }

        public void setThresholdMax(double thresholdMax) {
            this.thresholdMax = thresholdMax;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}