package com.dormpower.service;

import com.dormpower.dto.MqttSimulatorRequest;
import com.dormpower.dto.MqttSimulatorResponse;
import com.dormpower.dto.MqttSimulatorStatus;
import com.dormpower.dto.MqttSimulatorTaskSummary;
import com.dormpower.model.Device;
import com.dormpower.model.StripStatus;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.StripStatusRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * MQTT设备模拟器服务 - 优化版本
 * 增强功能：
 * 1. 详细的性能监控指标
 * 2. 消息类型选择（状态/遥测/混合）
 * 3. 详细的统计数据
 * 4. 实时性能指标（CPU、内存）
 * 5. 更好的错误处理和日志
 */
@Service
public class MqttSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(MqttSimulatorService.class);
    private static final int MAX_SIMULATOR_DEVICES = 1_000_000;
    private static final int MAX_DEVICES_PER_CYCLE = 10_000;
    private static final int SUMMARY_MONITORING_THRESHOLD = 50_000;
    private static final long FAST_POLLING_MS = 2_000L;
    private static final long MEDIUM_POLLING_MS = 5_000L;
    private static final long SLOW_POLLING_MS = 10_000L;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private StripStatusRepository stripStatusRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // 使用固定线程池，限制并发数，避免资源耗尽
    private final ExecutorService executorService = Executors.newFixedThreadPool(
        Math.min(10, Runtime.getRuntime().availableProcessors())
    );

    // 使用ConcurrentHashMap保证线程安全
    private final Map<String, SimulatorTask> simulatorTasks = new ConcurrentHashMap<>();
    private final AtomicInteger taskIdGenerator = new AtomicInteger(0);

    // 保留历史任务（最多100个）
    private final List<MqttSimulatorStatus> historyTasks = new CopyOnWriteArrayList<>();

    /**
     * 启动MQTT模拟器
     */
    public MqttSimulatorResponse startSimulator(MqttSimulatorRequest request) {
        try {
            validateRequest(request);

            String taskId = "simulator_" + taskIdGenerator.incrementAndGet() + "_" + System.currentTimeMillis();
            SimulatorTask task = new SimulatorTask(taskId, request);
            simulatorTasks.put(taskId, task);

            // 提交任务到线程池
            executorService.submit(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    log.error("模拟器任务执行异常: taskId={}, error={}", taskId, e.getMessage(), e);
                    task.setStatus("ERROR");
                } finally {
                    // 任务完成后移除（保留一段时间供查询）
                    simulatorTasks.remove(taskId);
                    // 添加到历史记录
                    saveToHistory(task);
                }
            });

            log.info("MQTT模拟器任务已启动: taskId={}, devices={}, duration={}s, interval={}s",
                    taskId, request.getDevices(), request.getDuration(), request.getInterval());

            return MqttSimulatorResponse.builder()
                    .taskId(taskId)
                    .status("STARTED")
                    .message("MQTT模拟器已启动，设备数: " + request.getDevices())
                    .build();
        } catch (Exception e) {
            log.error("启动模拟器失败: {}", e.getMessage(), e);
            return MqttSimulatorResponse.builder()
                    .taskId("")
                    .status("ERROR")
                    .message("启动失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 停止MQTT模拟器
     */
    public MqttSimulatorResponse stopSimulator(String taskId) {
        SimulatorTask task = simulatorTasks.get(taskId);
        if (task == null) {
            // 检查是否是已完成的任务
            Optional<MqttSimulatorStatus> historyTask = historyTasks.stream()
                    .filter(t -> t.getTaskId().equals(taskId))
                    .findFirst();

            if (historyTask.isPresent()) {
                return MqttSimulatorResponse.builder()
                        .taskId(taskId)
                        .status(historyTask.get().getStatus())
                        .message("任务已" + historyTask.get().getStatus())
                        .build();
            }

            return MqttSimulatorResponse.builder()
                    .taskId(taskId)
                    .status("NOT_FOUND")
                    .message("模拟器任务不存在")
                    .build();
        }

        task.stop();

        log.info("MQTT模拟器任务已停止: taskId={}", taskId);

        return MqttSimulatorResponse.builder()
                .taskId(taskId)
                .status("STOPPED")
                .message("MQTT模拟器已停止")
                .build();
    }

    /**
     * 获取模拟器状态
     */
    public MqttSimulatorStatus getSimulatorStatus(String taskId) {
        SimulatorTask task = simulatorTasks.get(taskId);
        if (task != null) {
            return task.getStatusInfo();
        }

        // 检查历史任务
        return historyTasks.stream()
                .filter(t -> t.getTaskId().equals(taskId))
                .findFirst()
                .orElseGet(() -> MqttSimulatorStatus.builder()
                        .taskId(taskId)
                        .status("NOT_FOUND")
                        .message("模拟器任务不存在")
                        .build());
    }

    /**
     * 获取所有模拟器任务（包括运行中和历史）
     */
    public List<MqttSimulatorStatus> getAllSimulatorTasks() {
        List<MqttSimulatorStatus> tasks = new ArrayList<>();

        // 添加运行中的任务
        for (SimulatorTask task : simulatorTasks.values()) {
            tasks.add(task.getStatusInfo());
        }

        // 添加历史任务（最近20个）
        int historySize = Math.min(20, historyTasks.size());
        for (int i = historyTasks.size() - 1; i >= historyTasks.size() - historySize && i >= 0; i--) {
            tasks.add(historyTasks.get(i));
        }

        return tasks;
    }

    /**
     * 获取所有模拟器任务摘要
     */
    public List<MqttSimulatorTaskSummary> getAllSimulatorTaskSummaries() {
        return getAllSimulatorTasks().stream()
                .map(task -> MqttSimulatorTaskSummary.builder()
                        .taskId(task.getTaskId())
                        .status(task.getStatus())
                        .devices(task.getDevices())
                        .totalMessages(task.getTotalMessages())
                        .successRate(task.getSuccessRate())
                        .runtime(task.getRuntime())
                        .summaryOnly(task.isSummaryOnly())
                        .monitoringMode(task.getMonitoringMode())
                        .lastUpdateTime(task.getLastUpdateTime())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 获取历史任务统计
     */
    public Map<String, Object> getHistoryStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 总任务数
        stats.put("totalTasks", historyTasks.size());

        // 按状态统计
        Map<String, Long> statusCount = historyTasks.stream()
                .collect(Collectors.groupingBy(MqttSimulatorStatus::getStatus, Collectors.counting()));
        stats.put("statusCount", statusCount);

        // 总设备数
        long totalDevices = historyTasks.stream().mapToLong(MqttSimulatorStatus::getDevices).sum();
        stats.put("totalDevices", totalDevices);

        // 总消息数
        long totalMessages = historyTasks.stream().mapToLong(MqttSimulatorStatus::getTotalMessages).sum();
        stats.put("totalMessages", totalMessages);

        // 平均成功率
        double avgSuccessRate = historyTasks.stream()
                .filter(t -> t.getSuccessRate() > 0)
                .mapToDouble(MqttSimulatorStatus::getSuccessRate)
                .average()
                .orElse(0.0);
        stats.put("avgSuccessRate", String.format("%.2f%%", avgSuccessRate));

        return stats;
    }

    /**
     * 验证请求参数
     */
    private void validateRequest(MqttSimulatorRequest request) {
        if (request.getDevices() <= 0 || request.getDevices() > MAX_SIMULATOR_DEVICES) {
            throw new IllegalArgumentException("设备数量必须在 1-1000000 之间");
        }
        if (request.getDuration() <= 0 || request.getDuration() > 86400) {
            throw new IllegalArgumentException("持续时间必须在 1-86400 秒之间");
        }
        if (request.getInterval() < 0.01 || request.getInterval() > 60) {
            throw new IllegalArgumentException("发送间隔必须在 0.01-60 秒之间");
        }
        if (request.getOnlineRate() < 0 || request.getOnlineRate() > 1) {
            throw new IllegalArgumentException("在线率必须在 0-1 之间");
        }
    }

    /**
     * 保存任务到历史记录
     */
    private synchronized void saveToHistory(SimulatorTask task) {
        MqttSimulatorStatus status = task.getStatusInfo();
        historyTasks.add(status);

        // 限制历史记录数量
        if (historyTasks.size() > 100) {
            historyTasks.remove(0);
        }
    }

    /**
     * 清理过期的历史记录（每天执行）
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupHistory() {
        synchronized (historyTasks) {
            if (historyTasks.size() > 50) {
                int removeCount = historyTasks.size() - 50;
                for (int i = 0; i < removeCount; i++) {
                    historyTasks.remove(0);
                }
                log.info("已清理 {} 条历史记录", removeCount);
            }
        }
    }

    /**
     * 模拟器任务类 - 优化版本
     */
    private class SimulatorTask implements Runnable {
        private final String taskId;
        private final int devices;
        private final int duration;
        private final double interval;
        private final String messageType;
        private final boolean enableDetailedMonitoring;
        private final double minPower;
        private final double maxPower;
        private final double minVoltage;
        private final double maxVoltage;
        private final double onlineRate;
        private final String deviceNamePrefix;
        private final int roomStart;
        private final int roomEnd;
        private final boolean enableHeartbeat;
        private final int heartbeatInterval;
        private final String deviceIdPrefix;

        private volatile boolean running = true;
        private volatile String status = "RUNNING";
        private final AtomicInteger totalMessages = new AtomicInteger(0);
        private final AtomicInteger successMessages = new AtomicInteger(0);
        private final AtomicInteger errorMessages = new AtomicInteger(0);

        // 性能监控
        private final AtomicLong startTime = new AtomicLong(0);
        private final AtomicLong endTime = new AtomicLong(0);
        private final AtomicLong lastSendTime = new AtomicLong(0);
        private final AtomicLong totalSendTime = new AtomicLong(0);
        private final AtomicInteger sendCount = new AtomicInteger(0);
        private final AtomicLong maxSendInterval = new AtomicLong(0);
        private final AtomicLong minSendInterval = new AtomicLong(Long.MAX_VALUE);

        // 统计数据
        private final AtomicLong totalPower = new AtomicLong(0);
        private final AtomicLong maxPowerObserved = new AtomicLong(0);
        private final AtomicLong minPowerObserved = new AtomicLong(Long.MAX_VALUE);
        private final AtomicInteger lastCycleOnlineDevices = new AtomicInteger(0);
        private final AtomicInteger lastCycleProcessedDevices = new AtomicInteger(0);

        // 详细监控数据
        private final Map<String, Object> detailedMetrics = new ConcurrentHashMap<>();

        private boolean useSummaryMonitoring() {
            return devices > SUMMARY_MONITORING_THRESHOLD;
        }

        private long getRecommendedPollingIntervalMs() {
            if (devices > 200_000) {
                return SLOW_POLLING_MS;
            }
            if (devices > SUMMARY_MONITORING_THRESHOLD) {
                return MEDIUM_POLLING_MS;
            }
            return FAST_POLLING_MS;
        }

        private int getDevicesPerCycle() {
            return Math.min(devices, MAX_DEVICES_PER_CYCLE);
        }

        public SimulatorTask(String taskId, MqttSimulatorRequest request) {
            this.taskId = taskId;
            this.devices = request.getDevices();
            this.duration = request.getDuration();
            this.interval = request.getInterval();
            this.messageType = request.getMessageType() != null ? request.getMessageType() : "MIXED";
            this.enableDetailedMonitoring = request.isEnableDetailedMonitoring();
            this.minPower = request.getMinPower();
            this.maxPower = request.getMaxPower();
            this.minVoltage = request.getMinVoltage();
            this.maxVoltage = request.getMaxVoltage();
            this.onlineRate = request.getOnlineRate();
            this.deviceNamePrefix = request.getDeviceNamePrefix() != null ? request.getDeviceNamePrefix() : "模拟设备";
            this.roomStart = request.getRoomStart();
            this.roomEnd = request.getRoomEnd();
            this.enableHeartbeat = request.isEnableHeartbeat();
            this.heartbeatInterval = request.getHeartbeatInterval();
            this.deviceIdPrefix = "sim_" + taskId.substring(0, Math.min(10, taskId.length())) + "_";
        }

        private String buildDeviceId(int index) {
            return deviceIdPrefix + String.format("%07d", index);
        }

        /**
         * 创建设备（如果不存在）
         */
        private Device createDevice(String deviceId, int index) {
            try {
                Device device = deviceRepository.findById(deviceId).orElseGet(Device::new);
                device.setId(deviceId);
                device.setName(deviceNamePrefix + " " + index);
                device.setRoom("A-" + (roomStart + (index % (roomEnd - roomStart + 1))));
                device.setOnline(true);
                device.setLastSeenTs(System.currentTimeMillis() / 1000);
                device.setCreatedAt(System.currentTimeMillis() / 1000);
                deviceRepository.save(device);

                // 创建或更新设备状态
                StripStatus stripStatus = stripStatusRepository.findByDeviceId(deviceId);
                if (stripStatus == null) {
                    stripStatus = new StripStatus();
                    stripStatus.setDeviceId(deviceId);
                }
                stripStatus.setTs(System.currentTimeMillis() / 1000);
                stripStatus.setOnline(true);
                stripStatus.setTotalPowerW(0);
                stripStatus.setVoltageV((minVoltage + maxVoltage) / 2);
                stripStatus.setCurrentA(0);
                stripStatus.setSocketsJson(generateInitialSockets());
                stripStatusRepository.save(stripStatus);

                if (log.isDebugEnabled()) {
                    log.debug("创建/更新模拟设备: {}", deviceId);
                }
                return device;
            } catch (Exception e) {
                log.error("创建设备失败: deviceId={}, error={}", deviceId, e.getMessage());
                errorMessages.incrementAndGet();
                return null;
            }
        }

        private Device getOrCreateDevice(String deviceId, int index) {
            Device device = deviceRepository.findById(deviceId).orElse(null);
            if (device != null) {
                return device;
            }
            return createDevice(deviceId, index);
        }

        /**
         * 生成初始插座状态
         */
        private String generateInitialSockets() {
            try {
                List<Map<String, Object>> sockets = new ArrayList<>();
                for (int i = 1; i <= 4; i++) {
                    Map<String, Object> socket = new HashMap<>();
                    socket.put("id", "socket_" + i);
                    socket.put("on", false);
                    socket.put("power_w", 0.0);
                    sockets.add(socket);
                }
                return objectMapper.writeValueAsString(sockets);
            } catch (JsonProcessingException e) {
                log.error("生成插座状态失败: {}", e.getMessage());
                return "[]";
            }
        }

        /**
         * 更新设备心跳
         */
        private void updateDeviceHeartbeat(String deviceId, int index) {
            try {
                Device device = getOrCreateDevice(deviceId, index);
                if (device != null) {
                    device.setLastSeenTs(System.currentTimeMillis() / 1000);
                    deviceRepository.save(device);
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("更新设备心跳失败: deviceId={}, error={}", deviceId, e.getMessage());
                }
            }
        }

        /**
         * 发送状态消息
         */
        private boolean sendStatus(String deviceId, int index) {
            try {
                Device device = getOrCreateDevice(deviceId, index);
                if (device == null) {
                    return false;
                }

                boolean online = Math.random() < onlineRate;
                device.setOnline(online);
                device.setLastSeenTs(System.currentTimeMillis() / 1000);

                deviceRepository.save(device);

                // 更新设备状态
                StripStatus stripStatus = stripStatusRepository.findByDeviceId(deviceId);
                if (stripStatus != null) {
                    stripStatus.setTs(System.currentTimeMillis() / 1000);
                    stripStatus.setOnline(online);
                    stripStatusRepository.save(stripStatus);
                }

                successMessages.incrementAndGet();
                return online;
            } catch (Exception e) {
                log.error("发送状态消息失败: deviceId={}, error={}", deviceId, e.getMessage());
                errorMessages.incrementAndGet();
                return false;
            }
        }

        /**
         * 发送遥测数据
         */
        private boolean sendTelemetry(String deviceId, int index) {
            try {
                Device device = getOrCreateDevice(deviceId, index);
                if (device == null) {
                    return false;
                }

                device.setOnline(true);
                device.setLastSeenTs(System.currentTimeMillis() / 1000);
                deviceRepository.save(device);

                // 生成随机遥测数据
                double power = minPower + Math.random() * (maxPower - minPower);
                double voltage = minVoltage + Math.random() * (maxVoltage - minVoltage);
                double current = power / voltage;

                // 更新统计数据
                totalPower.addAndGet((long) power);
                maxPowerObserved.set(Math.max(maxPowerObserved.get(), (long) power));
                minPowerObserved.set(Math.min(minPowerObserved.get(), (long) power));

                // 更新设备状态
                StripStatus stripStatus = stripStatusRepository.findByDeviceId(deviceId);
                if (stripStatus != null) {
                    stripStatus.setTs(System.currentTimeMillis() / 1000);
                    stripStatus.setOnline(true);
                    stripStatus.setTotalPowerW(power);
                    stripStatus.setVoltageV(voltage);
                    stripStatus.setCurrentA(current);

                    // 生成随机插座状态
                    List<Map<String, Object>> sockets = new ArrayList<>();
                    for (int i = 1; i <= 4; i++) {
                        Map<String, Object> socket = new HashMap<>();
                        boolean on = Math.random() > 0.5;
                        double socketPower = on ? (power / 4 * (0.8 + Math.random() * 0.4)) : 0.0;
                        socket.put("id", "socket_" + i);
                        socket.put("on", on);
                        socket.put("power_w", socketPower);
                        sockets.add(socket);
                    }
                    stripStatus.setSocketsJson(objectMapper.writeValueAsString(sockets));
                    stripStatusRepository.save(stripStatus);
                }

                successMessages.incrementAndGet();
                return true;
            } catch (Exception e) {
                log.error("发送遥测数据失败: deviceId={}, error={}", deviceId, e.getMessage());
                errorMessages.incrementAndGet();
                return false;
            }
        }

        /**
         * 记录发送时间
         */
        private void recordSendTime() {
            long now = System.currentTimeMillis();
            if (lastSendTime.get() > 0) {
                long interval = now - lastSendTime.get();
                totalSendTime.addAndGet(interval);
                sendCount.incrementAndGet();

                if (interval > maxSendInterval.get()) {
                    maxSendInterval.set(interval);
                }
                if (interval < minSendInterval.get()) {
                    minSendInterval.set(interval);
                }
            }
            lastSendTime.set(now);
        }

        @Override
        public void run() {
            try {
                startTime.set(System.currentTimeMillis());
                log.info("MQTT模拟器任务开始运行: taskId={}, devices={}, duration={}s, interval={}s",
                        taskId, devices, duration, interval);

                long endTimeMillis = startTime.get() + (duration * 1000L);
                int heartbeatCounter = 0;
                int cycleIndex = 0;

                // 模拟发送消息
                while (running && System.currentTimeMillis() < endTimeMillis) {
                    int devicesThisCycle = getDevicesPerCycle();
                    int startIndex = devices > 0
                            ? (int) (((long) cycleIndex * devicesThisCycle) % devices)
                            : 0;
                    int cycleOnlineDevices = 0;
                    lastCycleOnlineDevices.set(0);
                    lastCycleProcessedDevices.set(0);

                    for (int offset = 0; offset < devicesThisCycle; offset++) {
                        if (!running) break;

                        int deviceIndex = (startIndex + offset) % devices;
                        String deviceId = buildDeviceId(deviceIndex);
                        boolean online;

                        // 根据消息类型发送
                        if ("STATUS".equals(messageType)) {
                            online = sendStatus(deviceId, deviceIndex);
                        } else if ("TELEMETRY".equals(messageType)) {
                            online = sendTelemetry(deviceId, deviceIndex);
                        } else { // MIXED
                            if (Math.random() > 0.5) {
                                online = sendStatus(deviceId, deviceIndex);
                            } else {
                                online = sendTelemetry(deviceId, deviceIndex);
                            }
                        }

                        if (online) {
                            cycleOnlineDevices++;
                        }
                        lastCycleOnlineDevices.set(cycleOnlineDevices);
                        lastCycleProcessedDevices.set(offset + 1);

                        recordSendTime();
                        totalMessages.incrementAndGet();

                        // 启用心跳机制
                        if (enableHeartbeat && heartbeatInterval > 0 && heartbeatCounter % heartbeatInterval == 0) {
                            updateDeviceHeartbeat(deviceId, deviceIndex);
                        }
                    }

                    heartbeatCounter++;
                    cycleIndex++;

                    // 收集详细监控数据
                    if (enableDetailedMonitoring && !useSummaryMonitoring()) {
                        collectDetailedMetrics();
                    }

                    Thread.sleep((long) (interval * 1000));
                }

                this.endTime.set(System.currentTimeMillis());
                status = "COMPLETED";

                log.info("MQTT模拟器任务完成: taskId={}, messages={}, success={}, errors={}, successRate={}%",
                        taskId, totalMessages.get(), successMessages.get(), errorMessages.get(),
                        getSuccessRate());

            } catch (InterruptedException e) {
                log.info("MQTT模拟器任务被中断: taskId={}", taskId);
                status = "STOPPED";
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("MQTT模拟器任务异常: taskId={}, error={}", taskId, e.getMessage(), e);
                status = "ERROR";
            }
        }

        /**
         * 收集详细监控数据
         */
        private void collectDetailedMetrics() {
            detailedMetrics.put("timestamp", System.currentTimeMillis());
            detailedMetrics.put("cpuLoad", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
            detailedMetrics.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());
            detailedMetrics.put("heapMemory", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
            detailedMetrics.put("devicesPerCycle", getDevicesPerCycle());
        }

        public void stop() {
            running = false;
            status = "STOPPED";
            endTime.set(System.currentTimeMillis());
            log.info("MQTT模拟器任务已停止: taskId={}", taskId);
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public MqttSimulatorStatus getStatusInfo() {
            double successRate = totalMessages.get() > 0
                ? ((double) successMessages.get() / totalMessages.get()) * 100
                : 0.0;

            double avgSendIntervalVal = sendCount.get() > 0
                ? (double) totalSendTime.get() / sendCount.get()
                : 0.0;

            double onlineRateVal = lastCycleProcessedDevices.get() > 0
                ? ((double) lastCycleOnlineDevices.get() / lastCycleProcessedDevices.get()) * 100
                : 0.0;

            double avgPowerVal = totalMessages.get() > 0
                ? (double) totalPower.get() / totalMessages.get()
                : 0.0;

            long runtimeVal = endTime.get() > 0
                ? endTime.get() - startTime.get()
                : System.currentTimeMillis() - startTime.get();

            double cpuUsage = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
            long memoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
                boolean summaryOnly = useSummaryMonitoring();
                boolean detailedMonitoringEnabled = enableDetailedMonitoring && !summaryOnly;
                String monitoringMode = summaryOnly ? "SUMMARY" : "REALTIME";
                String statusMessage = summaryOnly
                    ? "任务运行中（摘要监控模式，已自动降级以避免大规模设备压垮实时监控）"
                    : "任务运行中";

            return MqttSimulatorStatus.builder()
                    .taskId(taskId)
                    .status(status)
                    .devices(devices)
                    .duration(duration)
                    .interval(interval)
                    .totalMessages(totalMessages.get())
                    .successMessages(successMessages.get())
                    .errorMessages(errorMessages.get())
                    .messageType(messageType)
                    .successRate(successRate)
                    .avgSendInterval(avgSendIntervalVal)
                    .maxSendInterval(maxSendInterval.get())
                    .minSendInterval(minSendInterval.get() == Long.MAX_VALUE ? 0 : minSendInterval.get())
                    .runtime(runtimeVal)
                    .startTime(startTime.get())
                    .endTime(endTime.get())
                    .onlineRate(onlineRateVal)
                    .avgPower(avgPowerVal)
                    .maxPower(maxPowerObserved.get())
                    .minPower(minPowerObserved.get() == Long.MAX_VALUE ? 0 : minPowerObserved.get())
                    .cpuUsage(cpuUsage)
                    .memoryUsage(memoryUsage)
                    .message(statusMessage)
                    .lastUpdateTime(System.currentTimeMillis())
                    .enableDetailedMonitoring(detailedMonitoringEnabled)
                    .detailedMetrics(detailedMonitoringEnabled ? detailedMetrics.toString() : "{}")
                    .devicesPerCycle(getDevicesPerCycle())
                    .monitoringMode(monitoringMode)
                    .recommendedPollingIntervalMs(getRecommendedPollingIntervalMs())
                    .summaryOnly(summaryOnly)
                    .build();
        }

        private double getSuccessRate() {
            return totalMessages.get() > 0
                ? ((double) successMessages.get() / totalMessages.get()) * 100
                : 0.0;
        }

    }
}