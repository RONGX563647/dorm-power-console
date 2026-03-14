package com.dormpower.service;

import com.dormpower.model.Device;
import com.dormpower.model.DeviceStatusHistory;
import com.dormpower.model.StripStatus;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.DeviceStatusHistoryRepository;
import com.dormpower.repository.StripStatusRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * 设备状态服务
 *
 * 处理设备状态上报，包括：
 * - 设备在线状态更新
 * - 插座状态存储
 * - WebSocket 通知
 * - 状态历史记录
 *
 * 支持降级处理：Kafka 不可用时直接处理
 *
 * @author dormpower team
 * @version 1.0
 */
@Service
public class DeviceStatusService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceStatusService.class);

    /** 默认电压值（V） */
    public static final double DEFAULT_VOLTAGE_V = 220.0;
    /** 默认电流值（A） */
    public static final double DEFAULT_CURRENT_A = 0.0;
    /** 默认功率值（W） */
    public static final double DEFAULT_POWER_W = 0.0;
    /** 默认插座状态 JSON */
    public static final String DEFAULT_SOCKETS_JSON = "[]";

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private StripStatusRepository stripStatusRepository;

    @Autowired
    private DeviceStatusHistoryRepository deviceStatusHistoryRepository;

    @Autowired
    private WebSocketNotificationService webSocketNotificationService;

    /**
     * 处理设备状态上报
     *
     * @param deviceId 设备ID
     * @param payload 状态数据（JSON）
     * @return 处理后的插座状态
     */
    public StripStatus processStatusReport(String deviceId, JsonNode payload) {
        return processStatusReport(deviceId, payload, null);
    }

    /**
     * 处理设备状态上报（支持 WebSocket 回调）
     *
     * @param deviceId 设备ID
     * @param payload 状态数据（JSON）
     * @param wsCallback WebSocket 回调（可为 null）
     * @return 处理后的插座状态
     */
    public StripStatus processStatusReport(String deviceId, JsonNode payload,
                                            Consumer<JsonNode> wsCallback) {
        logger.debug("处理设备状态上报: deviceId={}", deviceId);

        long now = System.currentTimeMillis() / 1000;

        // 解析状态数据，缺失字段使用默认值
        boolean online = payload.has("online") ? payload.get("online").asBoolean() : true;
        double totalPowerW = payload.has("total_power_w") ? payload.get("total_power_w").asDouble() : DEFAULT_POWER_W;
        double voltageV = payload.has("voltage_v") ? payload.get("voltage_v").asDouble() : DEFAULT_VOLTAGE_V;
        double currentA = payload.has("current_a") ? payload.get("current_a").asDouble() : DEFAULT_CURRENT_A;
        String socketsJson = payload.has("sockets") ? payload.get("sockets").toString() : DEFAULT_SOCKETS_JSON;

        // 更新设备在线状态
        updateDeviceOnlineStatus(deviceId, online, now);

        // 保存插座状态
        StripStatus status = saveStripStatus(deviceId, online, totalPowerW, voltageV, currentA, socketsJson, now);

        // 记录状态历史
        saveStatusHistory(deviceId, online, totalPowerW, voltageV, currentA, socketsJson, now);

        // WebSocket 通知
        if (wsCallback != null) {
            wsCallback.accept(payload);
        } else {
            webSocketNotificationService.broadcastDeviceStatus(deviceId, payload);
        }

        logger.info("设备状态上报处理完成: deviceId={}, online={}", deviceId, online);
        return status;
    }

    /**
     * 处理设备离线上报
     *
     * @param deviceId 设备ID
     * @return 是否成功处理
     */
    public boolean processOfflineReport(String deviceId) {
        logger.debug("处理设备离线上报: deviceId={}", deviceId);

        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            logger.warn("设备不存在，无法处理离线上报: {}", deviceId);
            return false;
        }

        long now = System.currentTimeMillis() / 1000;
        device.setOnline(false);
        device.setLastSeenTs(now);
        deviceRepository.save(device);

        logger.info("设备离线状态已更新: {}", deviceId);
        return true;
    }

    /**
     * 直接处理状态上报（Kafka 降级模式）
     *
     * @param deviceId 设备ID
     * @param online 是否在线
     * @param totalPowerW 总功率
     * @param voltageV 电压
     * @param currentA 电流
     * @param socketsJson 插座状态 JSON
     * @param payload 原始 JSON 数据（用于 WebSocket）
     * @return 处理后的插座状态
     */
    public StripStatus processStatusDirect(String deviceId, boolean online,
                                            double totalPowerW, double voltageV, double currentA,
                                            String socketsJson, JsonNode payload) {
        logger.debug("直接处理设备状态: deviceId={}", deviceId);

        long now = System.currentTimeMillis() / 1000;

        // 更新设备在线状态
        updateDeviceOnlineStatus(deviceId, online, now);

        // 保存插座状态
        StripStatus status = saveStripStatus(deviceId, online, totalPowerW, voltageV, currentA, socketsJson, now);

        // 记录状态历史
        saveStatusHistory(deviceId, online, totalPowerW, voltageV, currentA, socketsJson, now);

        // WebSocket 通知
        if (payload != null) {
            webSocketNotificationService.broadcastDeviceStatus(deviceId, payload);
        }

        return status;
    }

    private void updateDeviceOnlineStatus(String deviceId, boolean online, long now) {
        deviceRepository.findById(deviceId).ifPresent(device -> {
            device.setOnline(online);
            device.setLastSeenTs(now);
            deviceRepository.save(device);
        });
    }

    private StripStatus saveStripStatus(String deviceId, boolean online, double totalPowerW,
                                         double voltageV, double currentA, String socketsJson, long now) {
        StripStatus status = stripStatusRepository.findByDeviceId(deviceId);
        if (status == null) {
            status = new StripStatus();
            status.setDeviceId(deviceId);
        }

        status.setTs(now);
        status.setOnline(online);
        status.setTotalPowerW(totalPowerW);
        status.setVoltageV(voltageV);
        status.setCurrentA(currentA);
        status.setSocketsJson(socketsJson);

        return stripStatusRepository.save(status);
    }

    private void saveStatusHistory(String deviceId, boolean online, double totalPowerW,
                                    double voltageV, double currentA, String socketsJson, long now) {
        DeviceStatusHistory history = new DeviceStatusHistory();
        history.setId("history_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000));
        history.setDeviceId(deviceId);
        history.setOnline(online);
        history.setTotalPowerW(totalPowerW);
        history.setVoltageV(voltageV);
        history.setCurrentA(currentA);
        history.setSocketsJson(socketsJson);
        history.setTs(now);
        history.setCreatedAt(now);

        deviceStatusHistoryRepository.save(history);
    }

}