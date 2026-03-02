package com.dormpower.ml;

import com.dormpower.model.Device;
import com.dormpower.model.DormRoom;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.DormRoomRepository;
import com.dormpower.service.CommandService;
import com.dormpower.service.NotificationService;
import com.dormpower.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能场景引擎
 * 
 * 核心功能：
 * 1. 自动生成智能场景
 * 2. 场景触发与执行
 * 3. 场景效果评估
 * 4. 自适应优化
 */
@Service
public class SmartSceneEngine {

    @Autowired
    private BehaviorLearningAgent behaviorLearningAgent;

    @Autowired
    private AnomalyDetectionAgent anomalyDetectionAgent;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DormRoomRepository dormRoomRepository;

    @Autowired
    private CommandService commandService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SystemLogService systemLogService;

    private final Map<String, SmartScene> activeScenes = new ConcurrentHashMap<>();
    private final Map<String, List<SceneExecutionRecord>> executionHistory = new ConcurrentHashMap<>();

    /**
     * 智能场景
     */
    public static class SmartScene {
        public String sceneId;
        public String name;
        public String description;
        public String roomId;
        public TriggerType triggerType;
        public String triggerCondition;
        public List<SceneAction> actions;
        public boolean enabled;
        public double confidence;
        public int executionCount;
        public double avgEffectiveness;
        public long createdAt;
        public long lastExecutedAt;
    }

    /**
     * 触发类型
     */
    public enum TriggerType {
        TIME,              // 定时触发
        POWER_THRESHOLD,   // 功率阈值触发
        ANOMALY,           // 异常触发
        BEHAVIOR_PATTERN,  // 行为模式触发
        MANUAL             // 手动触发
    }

    /**
     * 场景动作
     */
    public static class SceneAction {
        public String deviceId;
        public String action;
        public Map<String, Object> params;
        public int delay;
        public String description;
    }

    /**
     * 执行记录
     */
    public static class SceneExecutionRecord {
        public String sceneId;
        public long executedAt;
        public boolean success;
        public String triggerReason;
        public double powerBefore;
        public double powerAfter;
        public double effectiveness;
        public List<String> errors;
    }

    /**
     * 自动生成房间智能场景
     */
    public List<SmartScene> autoGenerateScenes(String roomId) {
        List<SmartScene> scenes = new ArrayList<>();

        List<BehaviorLearningAgent.SceneSuggestion> suggestions = 
                behaviorLearningAgent.generateSceneSuggestions(roomId, 7);

        for (BehaviorLearningAgent.SceneSuggestion suggestion : suggestions) {
            SmartScene scene = convertToScene(roomId, suggestion);
            if (scene != null) {
                scenes.add(scene);
                activeScenes.put(scene.sceneId, scene);
            }
        }

        scenes.addAll(generateAnomalyScenes(roomId));

        systemLogService.info("SMART_SCENE", 
                String.format("Generated %d smart scenes for room %s", scenes.size(), roomId),
                "SmartSceneEngine");

        return scenes;
    }

    /**
     * 转换建议为场景
     */
    private SmartScene convertToScene(String roomId, BehaviorLearningAgent.SceneSuggestion suggestion) {
        SmartScene scene = new SmartScene();
        scene.sceneId = "scene_" + UUID.randomUUID().toString().substring(0, 8);
        scene.name = suggestion.name;
        scene.description = suggestion.description;
        scene.roomId = roomId;
        scene.triggerType = TriggerType.valueOf(suggestion.triggerType);
        scene.triggerCondition = suggestion.triggerCondition;
        scene.actions = new ArrayList<>();
        scene.enabled = true;
        scene.confidence = suggestion.confidence;
        scene.executionCount = 0;
        scene.avgEffectiveness = 0;
        scene.createdAt = System.currentTimeMillis() / 1000;

        List<Device> devices = deviceRepository.findByRoom(roomId);
        for (String actionStr : suggestion.actions) {
            SceneAction action = parseAction(actionStr, devices);
            if (action != null) {
                scene.actions.add(action);
            }
        }

        return scene;
    }

    /**
     * 解析动作
     */
    private SceneAction parseAction(String actionStr, List<Device> devices) {
        SceneAction action = new SceneAction();
        action.params = new HashMap<>();
        action.description = actionStr;

        if (actionStr.contains("关闭") || actionStr.contains("断电")) {
            action.action = "power_off";
            if (!devices.isEmpty()) {
                action.deviceId = devices.get(0).getId();
            }
        } else if (actionStr.contains("开启") || actionStr.contains("通电")) {
            action.action = "power_on";
            if (!devices.isEmpty()) {
                action.deviceId = devices.get(0).getId();
            }
        } else if (actionStr.contains("通知")) {
            action.action = "notify";
            action.params.put("message", actionStr);
        } else if (actionStr.contains("降低")) {
            action.action = "reduce_power";
            action.params.put("percentage", 20);
        } else {
            action.action = "custom";
            action.params.put("command", actionStr);
        }

        return action;
    }

    /**
     * 生成异常响应场景
     */
    private List<SmartScene> generateAnomalyScenes(String roomId) {
        List<SmartScene> scenes = new ArrayList<>();

        SmartScene overloadScene = new SmartScene();
        overloadScene.sceneId = "scene_overload_" + roomId;
        overloadScene.name = "过载保护场景";
        overloadScene.description = "当功率超过阈值时自动断电保护";
        overloadScene.roomId = roomId;
        overloadScene.triggerType = TriggerType.POWER_THRESHOLD;
        overloadScene.triggerCondition = "power > 2000W for 60s";
        overloadScene.actions = new ArrayList<>();
        overloadScene.enabled = true;
        overloadScene.confidence = 0.95;

        List<Device> devices = deviceRepository.findByRoom(roomId);
        for (Device device : devices) {
            SceneAction action = new SceneAction();
            action.deviceId = device.getId();
            action.action = "power_off";
            action.description = "关闭设备防止过载";
            overloadScene.actions.add(action);
        }

        SceneAction notifyAction = new SceneAction();
        notifyAction.action = "notify";
        notifyAction.params = new HashMap<>();
        notifyAction.params.put("message", "检测到过载风险，已自动断电保护");
        notifyAction.description = "发送过载通知";
        overloadScene.actions.add(notifyAction);

        scenes.add(overloadScene);
        activeScenes.put(overloadScene.sceneId, overloadScene);

        return scenes;
    }

    /**
     * 执行场景
     */
    public SceneExecutionRecord executeScene(String sceneId, String triggerReason) {
        SmartScene scene = activeScenes.get(sceneId);
        if (scene == null || !scene.enabled) {
            return null;
        }

        SceneExecutionRecord record = new SceneExecutionRecord();
        record.sceneId = sceneId;
        record.executedAt = System.currentTimeMillis() / 1000;
        record.triggerReason = triggerReason;
        record.errors = new ArrayList<>();

        record.powerBefore = getRoomCurrentPower(scene.roomId);

        boolean allSuccess = true;
        for (SceneAction action : scene.actions) {
            try {
                executeAction(action);
            } catch (Exception e) {
                allSuccess = false;
                record.errors.add("Action failed: " + action.description + " - " + e.getMessage());
            }
        }

        record.success = allSuccess;

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        record.powerAfter = getRoomCurrentPower(scene.roomId);
        record.effectiveness = calculateEffectiveness(record.powerBefore, record.powerAfter, scene);

        scene.executionCount++;
        scene.lastExecutedAt = record.executedAt;
        scene.avgEffectiveness = (scene.avgEffectiveness * (scene.executionCount - 1) + record.effectiveness)
                / scene.executionCount;

        executionHistory.computeIfAbsent(sceneId, k -> new ArrayList<>()).add(record);

        systemLogService.info("SMART_SCENE",
                String.format("Scene %s executed, effectiveness: %.2f", scene.name, record.effectiveness),
                "SmartSceneEngine");

        return record;
    }

    /**
     * 执行动作
     */
    private void executeAction(SceneAction action) {
        if (action.delay > 0) {
            try {
                Thread.sleep(action.delay * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        switch (action.action) {
            case "power_off", "power_on" -> {
                if (action.deviceId != null) {
                    Map<String, Object> cmd = new HashMap<>();
                    cmd.put("action", action.action);
                    cmd.put("socket", 0);
                    commandService.sendCommand(action.deviceId, cmd);
                }
            }
            case "notify" -> {
                String message = (String) action.params.getOrDefault("message", "场景通知");
                notificationService.createSystemNotification("智能场景通知", message, "NORMAL");
            }
            default -> systemLogService.info("SMART_SCENE",
                    "Custom action: " + action.description, "SmartSceneEngine");
        }
    }

    /**
     * 获取房间当前功率
     */
    private double getRoomCurrentPower(String roomId) {
        List<Device> devices = deviceRepository.findByRoom(roomId);
        double totalPower = 0;

        for (Device device : devices) {
            if (device.isOnline()) {
                totalPower += 50;
            }
        }

        return totalPower;
    }

    /**
     * 计算场景效果
     */
    private double calculateEffectiveness(double powerBefore, double powerAfter, SmartScene scene) {
        if (powerBefore <= 0) return 0;

        double powerReduction = powerBefore - powerAfter;
        double effectiveness = powerReduction / powerBefore;

        if (scene.triggerType == TriggerType.ANOMALY) {
            effectiveness = powerAfter < 50 ? 1.0 : 0.5;
        }

        return Math.max(0, Math.min(1, effectiveness));
    }

    /**
     * 定时检查场景触发条件
     */
    @Scheduled(fixedRate = 60000)
    public void checkSceneTriggers() {
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        for (SmartScene scene : activeScenes.values()) {
            if (!scene.enabled) continue;

            boolean shouldTrigger = false;
            String triggerReason = "";

            if (scene.triggerType == TriggerType.TIME) {
                String[] parts = scene.triggerCondition.split(":");
                if (parts.length == 2) {
                    int triggerHour = Integer.parseInt(parts[0]);
                    int triggerMinute = Integer.parseInt(parts[1]);
                    if (currentHour == triggerHour && currentMinute == triggerMinute) {
                        shouldTrigger = true;
                        triggerReason = "定时触发";
                    }
                }
            } else if (scene.triggerType == TriggerType.POWER_THRESHOLD) {
                double currentPower = getRoomCurrentPower(scene.roomId);
                if (currentPower > 2000) {
                    shouldTrigger = true;
                    triggerReason = String.format("功率超限: %.1fW", currentPower);
                }
            } else if (scene.triggerType == TriggerType.ANOMALY) {
                Map<String, Object> roomAnomalies = anomalyDetectionAgent.detectRoomAnomalies(scene.roomId);
                double riskScore = (Double) roomAnomalies.getOrDefault("riskScore", 0.0);
                if (riskScore > 60) {
                    shouldTrigger = true;
                    triggerReason = String.format("异常风险: %.1f", riskScore);
                }
            }

            if (shouldTrigger) {
                executeScene(scene.sceneId, triggerReason);
            }
        }
    }

    /**
     * 获取房间活跃场景
     */
    public List<SmartScene> getRoomScenes(String roomId) {
        List<SmartScene> roomScenes = new ArrayList<>();
        for (SmartScene scene : activeScenes.values()) {
            if (roomId.equals(scene.roomId)) {
                roomScenes.add(scene);
            }
        }
        return roomScenes;
    }

    /**
     * 启用/禁用场景
     */
    public boolean toggleScene(String sceneId, boolean enabled) {
        SmartScene scene = activeScenes.get(sceneId);
        if (scene == null) return false;
        scene.enabled = enabled;
        return true;
    }

    /**
     * 删除场景
     */
    public boolean deleteScene(String sceneId) {
        return activeScenes.remove(sceneId) != null;
    }

    /**
     * 获取场景执行历史
     */
    public List<SceneExecutionRecord> getExecutionHistory(String sceneId, int limit) {
        List<SceneExecutionRecord> history = executionHistory.getOrDefault(sceneId, new ArrayList<>());
        return history.stream()
                .sorted((a, b) -> Long.compare(b.executedAt, a.executedAt))
                .limit(limit)
                .toList();
    }

    /**
     * 优化场景
     */
    public void optimizeScene(String sceneId) {
        SmartScene scene = activeScenes.get(sceneId);
        if (scene == null || scene.executionCount < 5) return;

        List<SceneExecutionRecord> history = executionHistory.getOrDefault(sceneId, new ArrayList<>());

        double avgEffectiveness = history.stream()
                .mapToDouble(r -> r.effectiveness)
                .average()
                .orElse(0);

        if (avgEffectiveness < 0.3) {
            scene.enabled = false;
            systemLogService.warn("SMART_SCENE",
                    String.format("Scene %s disabled due to low effectiveness: %.2f", scene.name, avgEffectiveness),
                    "SmartSceneEngine");
        } else if (avgEffectiveness > 0.7) {
            scene.confidence = Math.min(1.0, scene.confidence + 0.1);
        }
    }

    /**
     * 获取场景统计
     */
    public Map<String, Object> getSceneStatistics(String sceneId) {
        SmartScene scene = activeScenes.get(sceneId);
        if (scene == null) return Map.of("exists", false);

        List<SceneExecutionRecord> history = executionHistory.getOrDefault(sceneId, new ArrayList<>());

        Map<String, Object> stats = new HashMap<>();
        stats.put("exists", true);
        stats.put("sceneId", sceneId);
        stats.put("name", scene.name);
        stats.put("enabled", scene.enabled);
        stats.put("confidence", scene.confidence);
        stats.put("executionCount", scene.executionCount);
        stats.put("avgEffectiveness", scene.avgEffectiveness);
        stats.put("recentExecutions", history.stream()
                .sorted((a, b) -> Long.compare(b.executedAt, a.executedAt))
                .limit(10)
                .toList());

        return stats;
    }
}
