package com.dormpower.service;

import com.dormpower.ml.PowerPredictionAgent;
import com.dormpower.model.Device;
import com.dormpower.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * 自动节能执行服务
 * 
 * 核心功能：
 * 1. 根据预测结果自动执行节能策略
 * 2. 向排插发送控制命令
 * 3. 记录节能效果
 * 4. 动态调整策略
 * 
 * 性能优化：
 * 1. 使用原子类替代同步锁
 * 2. 限制缓存大小，防止内存溢出
 * 3. 使用StringBuilder拼接字符串
 * 4. 预分配集合容量
 */
@Service
public class AutoSavingService {

    @Autowired
    private PowerPredictionAgent predictionAgent;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private MqttService mqttService;

    @Autowired
    private SystemLogService systemLogService;

    @Autowired
    private NotificationService notificationService;

    private static final int MAX_CACHE_SIZE = 100;
    private static final int MAX_RECORDS_PER_ROOM = 50;

    private final Map<String, PowerPredictionAgent.PowerPrediction> roomPredictions = 
            new LinkedHashMap<String, PowerPredictionAgent.PowerPrediction>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, PowerPredictionAgent.PowerPrediction> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            };
    
    private final Map<String, List<PowerPredictionAgent.EnergySavingStrategy>> activeStrategies = 
            new LinkedHashMap<String, List<PowerPredictionAgent.EnergySavingStrategy>>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<PowerPredictionAgent.EnergySavingStrategy>> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            };
    
    private final Map<String, SavingStats> savingStatsMap = new ConcurrentHashMap<>();

    private final AtomicBoolean autoSavingEnabled = new AtomicBoolean(true);
    private volatile double dailyKwhThreshold = 2.0;

    /**
     * 节能统计 - 使用原子类优化并发
     */
    public static class SavingStats {
        public String roomId;
        public final DoubleAdder totalSavedKwh = new DoubleAdder();
        public final DoubleAdder todaySavedKwh = new DoubleAdder();
        public final AtomicInteger executedActions = new AtomicInteger(0);
        public volatile long lastExecutionTime;
        public final List<SavingRecord> records = Collections.synchronizedList(new ArrayList<>(MAX_RECORDS_PER_ROOM));
    }

    /**
     * 节能记录
     */
    public static class SavingRecord {
        public final long timestamp;
        public final String action;
        public final double savedKwh;
        public final String deviceId;

        public SavingRecord(long timestamp, String action, double savedKwh, String deviceId) {
            this.timestamp = timestamp;
            this.action = action;
            this.savedKwh = savedKwh;
            this.deviceId = deviceId;
        }
    }

    /**
     * 每小时预测用电量并生成策略
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyPrediction() {
        if (!autoSavingEnabled.get()) return;

        systemLogService.info("AUTO_SAVING", "开始每小时用电预测", "AutoSavingService");

        List<Device> allDevices = deviceRepository.findAll();
        Set<String> rooms = new HashSet<>(allDevices.size() / 2);
        
        for (Device d : allDevices) {
            if (d.getRoom() != null) {
                rooms.add(d.getRoom());
            }
        }

        for (String roomId : rooms) {
            try {
                PowerPredictionAgent.PowerPrediction prediction = predictionAgent.predictPower(roomId, 7);
                roomPredictions.put(roomId, prediction);

                List<PowerPredictionAgent.EnergySavingStrategy> strategies = 
                        predictionAgent.generateSavingStrategies(roomId, prediction);
                activeStrategies.put(roomId, strategies);

                if (prediction.predictedDailyKwh > dailyKwhThreshold) {
                    executeAutoSaving(roomId, prediction, strategies);
                }

                systemLogService.info("AUTO_SAVING", 
                        buildLogMessage(roomId, prediction.predictedDailyKwh, strategies.size()),
                        "AutoSavingService");
            } catch (Exception e) {
                systemLogService.error("AUTO_SAVING", 
                        "房间预测失败: " + roomId,
                        "AutoSavingService",
                        e.getMessage());
            }
        }
    }

    private String buildLogMessage(String roomId, double dailyKwh, int strategyCount) {
        StringBuilder sb = new StringBuilder(64);
        sb.append("房间 ").append(roomId)
          .append(" 预测日用电: ").append(String.format("%.2f", dailyKwh))
          .append(" kWh, 策略数: ").append(strategyCount);
        return sb.toString();
    }

    /**
     * 每15分钟检查并执行节能策略
     */
    @Scheduled(fixedRate = 900000)
    public void checkAndExecuteSaving() {
        if (!autoSavingEnabled.get()) return;

        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);

        for (Map.Entry<String, List<PowerPredictionAgent.EnergySavingStrategy>> entry : activeStrategies.entrySet()) {
            String roomId = entry.getKey();
            List<PowerPredictionAgent.EnergySavingStrategy> strategies = entry.getValue();

            if (strategies == null || strategies.isEmpty()) continue;

            for (PowerPredictionAgent.EnergySavingStrategy strategy : strategies) {
                if (!strategy.autoExecute) continue;

                if (shouldExecuteStrategy(strategy, currentHour, roomId)) {
                    executeStrategy(roomId, strategy);
                }
            }
        }
    }

    /**
     * 执行自动节能
     */
    private void executeAutoSaving(String roomId, 
                                    PowerPredictionAgent.PowerPrediction prediction,
                                    List<PowerPredictionAgent.EnergySavingStrategy> strategies) {
        
        systemLogService.info("AUTO_SAVING", 
                buildAutoSavingMessage(roomId, prediction),
                "AutoSavingService");

        notificationService.createSystemNotification(
                "系统管理员",
                "智能节能提醒",
                buildNotificationMessage(roomId, prediction)
        );
    }

    private String buildAutoSavingMessage(String roomId, PowerPredictionAgent.PowerPrediction prediction) {
        StringBuilder sb = new StringBuilder(64);
        sb.append("执行房间 ").append(roomId)
          .append(" 自动节能, 预计节省 ")
          .append(String.format("%.2f", prediction.savingsPotential * prediction.predictedDailyKwh))
          .append(" kWh");
        return sb.toString();
    }

    private String buildNotificationMessage(String roomId, PowerPredictionAgent.PowerPrediction prediction) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("房间 ").append(roomId)
          .append(" 预测今日用电 ").append(String.format("%.2f", prediction.predictedDailyKwh))
          .append(" kWh，已自动开启节能模式，预计节省 ")
          .append(String.format("%.1f", prediction.savingsPotential * 100))
          .append("%");
        return sb.toString();
    }

    /**
     * 判断是否应该执行策略
     */
    private boolean shouldExecuteStrategy(PowerPredictionAgent.EnergySavingStrategy strategy, 
                                          int currentHour, String roomId) {
        String condition = strategy.triggerCondition;
        
        if (condition == null || condition.isEmpty()) {
            return false;
        }

        if (condition.contains("hour ==")) {
            try {
                int triggerHour = Integer.parseInt(condition.split("hour == ")[1].split("[^0-9]")[0]);
                if (currentHour != triggerHour) return false;
            } catch (Exception e) {
                return false;
            }
        }

        if (condition.contains("hour >=")) {
            try {
                int startHour = Integer.parseInt(condition.split("hour >=")[1].split("&&")[0].trim());
                if (currentHour < startHour) return false;
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    /**
     * 执行节能策略
     */
    private void executeStrategy(String roomId, PowerPredictionAgent.EnergySavingStrategy strategy) {
        systemLogService.info("AUTO_SAVING", 
                "执行策略: " + strategy.name + " - " + strategy.description,
                "AutoSavingService");

        List<Device> devices = deviceRepository.findByRoom(roomId);
        int deviceCount = devices.size();
        double savingPerDevice = deviceCount > 0 ? strategy.estimatedSavingKwh / deviceCount : strategy.estimatedSavingKwh;
        
        for (String action : strategy.actions) {
            executeAction(roomId, devices, action, savingPerDevice);
        }

        SavingStats stats = savingStatsMap.computeIfAbsent(roomId, k -> {
            SavingStats s = new SavingStats();
            s.roomId = roomId;
            return s;
        });

        stats.totalSavedKwh.add(strategy.estimatedSavingKwh);
        stats.todaySavedKwh.add(strategy.estimatedSavingKwh);
        stats.executedActions.incrementAndGet();
        stats.lastExecutionTime = System.currentTimeMillis() / 1000;

        if (stats.records.size() < MAX_RECORDS_PER_ROOM) {
            stats.records.add(new SavingRecord(
                    System.currentTimeMillis() / 1000,
                    strategy.name,
                    strategy.estimatedSavingKwh,
                    null
            ));
        }
    }

    /**
     * 执行具体动作
     */
    private void executeAction(String roomId, List<Device> devices, String action, double savingKwh) {
        if (action.contains("关闭待机") || action.contains("关闭非必要")) {
            for (Device device : devices) {
                if (device.isOnline() && !isEssentialDevice(device)) {
                    sendPowerOffCommand(device.getId());
                    logSavingAction(roomId, device.getId(), "关闭待机设备", savingKwh);
                }
            }
        } else if (action.contains("限制功率")) {
            double limitPower = extractPowerFromAction(action);
            for (Device device : devices) {
                if (device.isOnline()) {
                    sendPowerLimitCommand(device.getId(), limitPower);
                    logSavingAction(roomId, device.getId(), "限制功率", savingKwh);
                }
            }
        } else if (action.contains("最低功耗") || action.contains("降低待机")) {
            for (Device device : devices) {
                if (device.isOnline()) {
                    sendLowPowerModeCommand(device.getId());
                    logSavingAction(roomId, device.getId(), "进入低功耗模式", savingKwh);
                }
            }
        } else if (action.contains("发送") || action.contains("通知")) {
            notificationService.createSystemNotification(
                    "room_" + roomId,
                    "节能提醒",
                    action
            );
        }
    }

    /**
     * 发送关机命令
     */
    private void sendPowerOffCommand(String deviceId) {
        try {
            StringBuilder sb = new StringBuilder(128);
            sb.append("{\"cmd\":\"power_off\",\"deviceId\":\"").append(deviceId)
              .append("\",\"timestamp\":").append(System.currentTimeMillis() / 1000).append("}");
            
            mqttService.sendMessage("device/" + deviceId + "/command", sb.toString());
            
            systemLogService.info("AUTO_SAVING", 
                    "发送关机命令: " + deviceId, 
                    "AutoSavingService");
        } catch (Exception e) {
            systemLogService.error("AUTO_SAVING", 
                    "发送命令失败: " + deviceId,
                    "AutoSavingService",
                    e.getMessage());
        }
    }

    /**
     * 发送功率限制命令
     */
    private void sendPowerLimitCommand(String deviceId, double limitWatts) {
        try {
            StringBuilder sb = new StringBuilder(128);
            sb.append("{\"cmd\":\"set_power_limit\",\"deviceId\":\"").append(deviceId)
              .append("\",\"limitWatts\":").append(limitWatts)
              .append(",\"timestamp\":").append(System.currentTimeMillis() / 1000).append("}");
            
            mqttService.sendMessage("device/" + deviceId + "/command", sb.toString());
            
            systemLogService.info("AUTO_SAVING", 
                    "发送功率限制命令: " + deviceId + " -> " + String.format("%.0f", limitWatts) + "W",
                    "AutoSavingService");
        } catch (Exception e) {
            systemLogService.error("AUTO_SAVING", 
                    "发送命令失败: " + deviceId,
                    "AutoSavingService",
                    e.getMessage());
        }
    }

    /**
     * 发送低功耗模式命令
     */
    private void sendLowPowerModeCommand(String deviceId) {
        try {
            StringBuilder sb = new StringBuilder(128);
            sb.append("{\"cmd\":\"set_eco_mode\",\"deviceId\":\"").append(deviceId)
              .append("\",\"timestamp\":").append(System.currentTimeMillis() / 1000).append("}");
            
            mqttService.sendMessage("device/" + deviceId + "/command", sb.toString());
            
            systemLogService.info("AUTO_SAVING", 
                    "发送低功耗模式命令: " + deviceId,
                    "AutoSavingService");
        } catch (Exception e) {
            systemLogService.error("AUTO_SAVING", 
                    "发送命令失败: " + deviceId,
                    "AutoSavingService",
                    e.getMessage());
        }
    }

    /**
     * 判断是否为必要设备
     */
    private boolean isEssentialDevice(Device device) {
        if (device.getName() == null) return false;
        String name = device.getName().toLowerCase();
        return name.contains("冰箱") || name.contains("路由") || name.contains("监控");
    }

    /**
     * 从动作描述中提取功率值
     */
    private double extractPowerFromAction(String action) {
        try {
            String[] parts = action.split("[^0-9.]");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    return Double.parseDouble(part);
                }
            }
        } catch (Exception e) {
            return 50.0;
        }
        return 50.0;
    }

    /**
     * 记录节能动作
     */
    private void logSavingAction(String roomId, String deviceId, String action, double savedKwh) {
        systemLogService.info("AUTO_SAVING", 
                buildSavingLog(roomId, deviceId, action, savedKwh),
                "AutoSavingService");
    }

    private String buildSavingLog(String roomId, String deviceId, String action, double savedKwh) {
        StringBuilder sb = new StringBuilder(64);
        sb.append("房间 ").append(roomId)
          .append(" 设备 ").append(deviceId)
          .append(": ").append(action)
          .append(", 节省 ").append(String.format("%.3f", savedKwh)).append(" kWh");
        return sb.toString();
    }

    /**
     * 获取房间预测
     */
    public PowerPredictionAgent.PowerPrediction getPrediction(String roomId) {
        return roomPredictions.get(roomId);
    }

    /**
     * 获取房间策略
     */
    public List<PowerPredictionAgent.EnergySavingStrategy> getStrategies(String roomId) {
        return activeStrategies.getOrDefault(roomId, Collections.emptyList());
    }

    /**
     * 获取或创建策略
     */
    public List<PowerPredictionAgent.EnergySavingStrategy> getOrCreateStrategies(String roomId) {
        List<PowerPredictionAgent.EnergySavingStrategy> strategies = activeStrategies.get(roomId);
        
        if (strategies == null || strategies.isEmpty()) {
            PowerPredictionAgent.PowerPrediction prediction = roomPredictions.get(roomId);
            if (prediction == null) {
                prediction = predictionAgent.predictPower(roomId, 7);
                roomPredictions.put(roomId, prediction);
            }
            strategies = predictionAgent.generateSavingStrategies(roomId, prediction);
            activeStrategies.put(roomId, strategies);
        }
        
        return strategies;
    }

    /**
     * 获取节能统计
     */
    public SavingStats getSavingStats(String roomId) {
        return savingStatsMap.get(roomId);
    }

    /**
     * 获取所有房间统计
     */
    public Map<String, SavingStats> getAllSavingStats() {
        return new HashMap<>(savingStatsMap);
    }

    /**
     * 手动触发预测
     */
    public PowerPredictionAgent.PowerPrediction manualPredict(String roomId, int days) {
        PowerPredictionAgent.PowerPrediction prediction = predictionAgent.predictPower(roomId, days);
        roomPredictions.put(roomId, prediction);
        return prediction;
    }

    /**
     * 手动执行策略
     */
    public void manualExecuteStrategy(String roomId, String strategyId) {
        List<PowerPredictionAgent.EnergySavingStrategy> strategies = activeStrategies.get(roomId);
        
        if (strategies == null || strategies.isEmpty()) {
            strategies = getOrCreateStrategies(roomId);
        }
        
        if (strategies != null) {
            for (PowerPredictionAgent.EnergySavingStrategy strategy : strategies) {
                if (strategy.strategyId.equals(strategyId)) {
                    executeStrategy(roomId, strategy);
                    return;
                }
            }
        }
    }

    /**
     * 设置自动节能开关
     */
    public void setAutoSavingEnabled(boolean enabled) {
        autoSavingEnabled.set(enabled);
        systemLogService.info("AUTO_SAVING", 
                "自动节能模式: " + (enabled ? "开启" : "关闭"),
                "AutoSavingService");
    }

    /**
     * 设置日用电阈值
     */
    public void setDailyKwhThreshold(double threshold) {
        this.dailyKwhThreshold = threshold;
    }

    /**
     * 获取自动节能状态
     */
    public boolean isAutoSavingEnabled() {
        return autoSavingEnabled.get();
    }
}
