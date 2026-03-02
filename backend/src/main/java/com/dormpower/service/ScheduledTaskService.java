package com.dormpower.service;

import com.dormpower.model.ScheduledTask;
import com.dormpower.repository.ScheduledTaskRepository;
import com.dormpower.mqtt.MqttBridge;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 定时任务服务
 */
@Service
public class ScheduledTaskService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);

    @Autowired
    private ScheduledTaskRepository scheduledTaskRepository;

    @Autowired
    private MqttBridge mqttBridge;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 创建定时任务
     */
    public ScheduledTask createTask(String deviceId, String type, int socketId, long scheduledTime, String cronExpression, boolean recurring) {
        ScheduledTask task = new ScheduledTask();
        task.setId("task_" + UUID.randomUUID().toString().substring(0, 8));
        task.setDeviceId(deviceId);
        task.setType(type);
        task.setSocketId(socketId);
        task.setScheduledTime(scheduledTime);
        task.setCronExpression(cronExpression);
        task.setEnabled(true);
        task.setRecurring(recurring);
        task.setCreatedAt(System.currentTimeMillis() / 1000);
        task.setUpdatedAt(System.currentTimeMillis() / 1000);
        
        return scheduledTaskRepository.save(task);
    }

    /**
     * 获取设备的定时任务列表
     */
    public List<ScheduledTask> getDeviceTasks(String deviceId) {
        return scheduledTaskRepository.findByDeviceIdOrderByScheduledTimeAsc(deviceId);
    }

    /**
     * 获取所有启用的定时任务
     */
    public List<ScheduledTask> getAllEnabledTasks() {
        return scheduledTaskRepository.findByEnabledTrueOrderByScheduledTimeAsc();
    }

    /**
     * 更新定时任务
     */
    public ScheduledTask updateTask(String taskId, Map<String, Object> updates) {
        ScheduledTask task = scheduledTaskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        
        if (updates.containsKey("type")) {
            task.setType((String) updates.get("type"));
        }
        if (updates.containsKey("socketId")) {
            task.setSocketId((int) updates.get("socketId"));
        }
        if (updates.containsKey("scheduledTime")) {
            task.setScheduledTime((long) updates.get("scheduledTime"));
        }
        if (updates.containsKey("cronExpression")) {
            task.setCronExpression((String) updates.get("cronExpression"));
        }
        if (updates.containsKey("enabled")) {
            task.setEnabled((boolean) updates.get("enabled"));
        }
        if (updates.containsKey("recurring")) {
            task.setRecurring((boolean) updates.get("recurring"));
        }
        
        task.setUpdatedAt(System.currentTimeMillis() / 1000);
        return scheduledTaskRepository.save(task);
    }

    /**
     * 删除定时任务
     */
    public void deleteTask(String taskId) {
        scheduledTaskRepository.deleteById(taskId);
    }

    /**
     * 启用/禁用定时任务
     */
    public ScheduledTask toggleTask(String taskId, boolean enabled) {
        ScheduledTask task = scheduledTaskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        task.setEnabled(enabled);
        task.setUpdatedAt(System.currentTimeMillis() / 1000);
        return scheduledTaskRepository.save(task);
    }

    /**
     * 执行定时任务
     */
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void executeScheduledTasks() {
        long now = System.currentTimeMillis() / 1000;
        List<ScheduledTask> tasks = scheduledTaskRepository.findByEnabledTrueAndScheduledTimeLessThanEqualOrderByScheduledTimeAsc(now);
        
        for (ScheduledTask task : tasks) {
            try {
                executeTask(task);
                
                // 如果是重复任务，计算下次执行时间
                if (task.isRecurring()) {
                    // 这里简化处理，实际应该使用cron表达式计算下次执行时间
                    // 暂时设置为24小时后
                    task.setScheduledTime(now + 86400);
                    task.setLastExecutedAt(now);
                    task.setLastStatus("success");
                    scheduledTaskRepository.save(task);
                } else {
                    // 非重复任务，执行后禁用
                    task.setEnabled(false);
                    task.setLastExecutedAt(now);
                    task.setLastStatus("success");
                    scheduledTaskRepository.save(task);
                }
            } catch (Exception e) {
                logger.error("Failed to execute task {}: {}", task.getId(), e.getMessage());
                task.setLastExecutedAt(now);
                task.setLastStatus("failed: " + e.getMessage());
                scheduledTaskRepository.save(task);
            }
        }
    }

    /**
     * 执行单个任务
     */
    private void executeTask(ScheduledTask task) throws Exception {
        logger.info("Executing task {}: {} for device {}", task.getId(), task.getType(), task.getDeviceId());
        
        Map<String, Object> command = new HashMap<>();
        command.put("cmdId", "task_" + System.currentTimeMillis());
        
        switch (task.getType()) {
            case "power_on":
                command.put("action", "power_on");
                break;
            case "power_off":
                command.put("action", "power_off");
                break;
            case "socket_on":
                command.put("action", "socket_on");
                command.put("socket", task.getSocketId());
                break;
            case "socket_off":
                command.put("action", "socket_off");
                command.put("socket", task.getSocketId());
                break;
            default:
                throw new RuntimeException("Unknown task type: " + task.getType());
        }
        
        boolean success = mqttBridge.publishCommand(task.getDeviceId(), command);
        if (!success) {
            throw new RuntimeException("Failed to publish command");
        }
    }

}
