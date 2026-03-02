package com.dormpower.service;

import com.dormpower.model.*;
import com.dormpower.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 自动断电控制服务
 */
@Service
public class PowerControlService {

    @Autowired
    private RoomBalanceRepository roomBalanceRepository;

    @Autowired
    private DormRoomRepository dormRoomRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ElectricityBillRepository billRepository;

    @Autowired
    private CommandService commandService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SystemLogService systemLogService;

    @Autowired
    private SystemConfigService systemConfigService;

    private static final double LOW_BALANCE = 10.0;

    /**
     * 检查并执行自动断电
     */
    @Scheduled(fixedRate = 300000)
    public void checkAndExecutePowerCutoff() {
        String autoCutoffEnabled = systemConfigService.getConfigValue("power.auto_cutoff_enabled", "false");
        if (!"true".equalsIgnoreCase(autoCutoffEnabled)) {
            return;
        }

        String thresholdStr = systemConfigService.getConfigValue("power.cutoff_threshold", "0");
        double threshold = Double.parseDouble(thresholdStr);

        List<RoomBalance> lowBalances = roomBalanceRepository.findByBalanceLessThan(threshold + 1);

        for (RoomBalance balance : lowBalances) {
            if (balance.getBalance() <= threshold) {
                executePowerCutoff(balance);
            } else if (balance.getBalance() < LOW_BALANCE && !balance.isWarningSent()) {
                sendLowBalanceWarning(balance);
            }
        }
    }

    /**
     * 执行断电操作
     */
    @Transactional
    public void executePowerCutoff(RoomBalance balance) {
        DormRoom room = dormRoomRepository.findById(balance.getRoomId()).orElse(null);
        if (room == null || room.getDeviceId() == null) {
            return;
        }

        Device device = deviceRepository.findById(room.getDeviceId()).orElse(null);
        if (device == null || !device.isOnline()) {
            return;
        }

        try {
            Map<String, Object> cmd = new HashMap<>();
            cmd.put("action", "power_off");
            cmd.put("socket", 0);
            commandService.sendCommand(room.getDeviceId(), cmd);

            balance.setAutoCutoff(true);
            balance.setUpdatedAt(System.currentTimeMillis() / 1000);
            roomBalanceRepository.save(balance);

            systemLogService.warn("POWER_CONTROL", 
                "Power cutoff executed for room " + room.getRoomNumber() + " due to low balance: " + balance.getBalance(),
                "PowerControlService");

            notificationService.createAlertNotification(
                "自动断电通知",
                "房间 " + room.getRoomNumber() + " 因余额不足已自动断电，当前余额: " + balance.getBalance() + " 元",
                "system",
                room.getDeviceId()
            );
        } catch (Exception e) {
            systemLogService.error("POWER_CONTROL",
                "Failed to execute power cutoff for room " + room.getRoomNumber(),
                "PowerControlService",
                e.getMessage());
        }
    }

    /**
     * 发送低余额预警
     */
    @Transactional
    public void sendLowBalanceWarning(RoomBalance balance) {
        DormRoom room = dormRoomRepository.findById(balance.getRoomId()).orElse(null);
        if (room == null) {
            return;
        }

        notificationService.createSystemNotification(
            "余额不足预警",
            "房间 " + room.getRoomNumber() + " 余额已不足 " + LOW_BALANCE + " 元，当前余额: " + balance.getBalance() + " 元，请及时充值",
            "HIGH"
        );

        balance.setWarningSent(true);
        roomBalanceRepository.save(balance);

        systemLogService.info("POWER_CONTROL",
            "Low balance warning sent for room " + room.getRoomNumber() + ", balance: " + balance.getBalance(),
            "PowerControlService");
    }

    /**
     * 手动断电
     */
    @Transactional
    public boolean manualPowerCutoff(String roomId, String operator) {
        RoomBalance balance = roomBalanceRepository.findByRoomId(roomId).orElse(null);
        if (balance == null) {
            throw new RuntimeException("Room balance not found: " + roomId);
        }

        DormRoom room = dormRoomRepository.findById(roomId).orElse(null);
        if (room == null || room.getDeviceId() == null) {
            throw new RuntimeException("Room has no associated device");
        }

        Device device = deviceRepository.findById(room.getDeviceId()).orElse(null);
        if (device == null) {
            throw new RuntimeException("Device not found");
        }

        try {
            Map<String, Object> cmd = new HashMap<>();
            cmd.put("action", "power_off");
            cmd.put("socket", 0);
            commandService.sendCommand(room.getDeviceId(), cmd);

            balance.setAutoCutoff(true);
            balance.setUpdatedAt(System.currentTimeMillis() / 1000);
            roomBalanceRepository.save(balance);

            systemLogService.info("POWER_CONTROL",
                "Manual power cutoff executed for room " + room.getRoomNumber() + " by " + operator,
                operator);

            return true;
        } catch (Exception e) {
            systemLogService.error("POWER_CONTROL",
                "Failed to execute manual power cutoff",
                operator,
                e.getMessage());
            return false;
        }
    }

    /**
     * 恢复供电
     */
    @Transactional
    public boolean restorePower(String roomId, String operator) {
        RoomBalance balance = roomBalanceRepository.findByRoomId(roomId).orElse(null);
        if (balance == null) {
            throw new RuntimeException("Room balance not found: " + roomId);
        }

        DormRoom room = dormRoomRepository.findById(roomId).orElse(null);
        if (room == null || room.getDeviceId() == null) {
            throw new RuntimeException("Room has no associated device");
        }

        try {
            Map<String, Object> cmd = new HashMap<>();
            cmd.put("action", "power_on");
            cmd.put("socket", 0);
            commandService.sendCommand(room.getDeviceId(), cmd);

            balance.setAutoCutoff(false);
            balance.setWarningSent(false);
            balance.setUpdatedAt(System.currentTimeMillis() / 1000);
            roomBalanceRepository.save(balance);

            systemLogService.info("POWER_CONTROL",
                "Power restored for room " + room.getRoomNumber() + " by " + operator,
                operator);

            notificationService.createSystemNotification(
                "恢复供电通知",
                "房间 " + room.getRoomNumber() + " 已恢复供电",
                "NORMAL"
            );

            return true;
        } catch (Exception e) {
            systemLogService.error("POWER_CONTROL",
                "Failed to restore power",
                operator,
                e.getMessage());
            return false;
        }
    }

    /**
     * 获取断电状态
     */
    public Map<String, Object> getPowerCutoffStatus(String roomId) {
        Map<String, Object> result = new HashMap<>();

        RoomBalance balance = roomBalanceRepository.findByRoomId(roomId).orElse(null);
        if (balance == null) {
            result.put("exists", false);
            return result;
        }

        result.put("exists", true);
        result.put("roomId", roomId);
        result.put("balance", balance.getBalance());
        result.put("powerCutoff", balance.isAutoCutoff());
        result.put("warningSent", balance.isWarningSent());

        return result;
    }

    /**
     * 获取所有断电房间列表
     */
    public List<Map<String, Object>> getAllCutoffRooms() {
        List<Map<String, Object>> result = new ArrayList<>();

        List<RoomBalance> allBalances = roomBalanceRepository.findAll();
        for (RoomBalance balance : allBalances) {
            if (balance.isAutoCutoff()) {
                Map<String, Object> roomInfo = new HashMap<>();
                roomInfo.put("roomId", balance.getRoomId());
                roomInfo.put("balance", balance.getBalance());

                DormRoom room = dormRoomRepository.findById(balance.getRoomId()).orElse(null);
                if (room != null) {
                    roomInfo.put("roomNumber", room.getRoomNumber());
                    roomInfo.put("deviceId", room.getDeviceId());
                }

                result.add(roomInfo);
            }
        }

        return result;
    }

    /**
     * 检查欠费房间
     */
    public List<Map<String, Object>> getOverdueRooms() {
        List<Map<String, Object>> result = new ArrayList<>();

        List<ElectricityBill> pendingBills = billRepository.findByStatusOrderByPeriodDesc("PENDING");
        Map<String, Double> roomDebt = new HashMap<>();

        for (ElectricityBill bill : pendingBills) {
            String roomId = bill.getRoomId();
            double amount = bill.getTotalAmount();
            roomDebt.merge(roomId, amount, Double::sum);
        }

        for (Map.Entry<String, Double> entry : roomDebt.entrySet()) {
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("roomId", entry.getKey());
            roomInfo.put("totalDebt", entry.getValue());

            RoomBalance balance = roomBalanceRepository.findByRoomId(entry.getKey()).orElse(null);
            if (balance != null) {
                roomInfo.put("currentBalance", balance.getBalance());
                roomInfo.put("powerCutoff", balance.isAutoCutoff());
            }

            DormRoom room = dormRoomRepository.findById(entry.getKey()).orElse(null);
            if (room != null) {
                roomInfo.put("roomNumber", room.getRoomNumber());
            }

            result.add(roomInfo);
        }

        return result;
    }
}
