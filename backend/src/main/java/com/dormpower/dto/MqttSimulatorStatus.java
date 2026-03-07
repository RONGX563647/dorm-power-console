package com.dormpower.dto;

/**
 * MQTT模拟器详细状态DTO
 */
public class MqttSimulatorStatus {
    private String taskId;
    private String status;
    private int devices;
    private int duration;
    private double interval;
    private int totalMessages;
    private int errorMessages;
    private int successMessages;
    private String messageType;
    private double successRate;
    private double avgSendInterval;
    private double maxSendInterval;
    private double minSendInterval;
    private long runtime;
    private long startTime;
    private long endTime;
    private double onlineRate;
    private double avgPower;
    private double maxPower;
    private double minPower;
    private double cpuUsage;
    private long memoryUsage;
    private String message;
    private long lastUpdateTime;
    private boolean enableDetailedMonitoring;
    private String detailedMetrics;
    private int devicesPerCycle;
    private String monitoringMode;
    private long recommendedPollingIntervalMs;
    private boolean summaryOnly;

    public MqttSimulatorStatus() {}

    public static class Builder {
        private String taskId;
        private String status;
        private int devices;
        private int duration;
        private double interval;
        private int totalMessages;
        private int errorMessages;
        private int successMessages;
        private String messageType;
        private double successRate;
        private double avgSendInterval;
        private double maxSendInterval;
        private double minSendInterval;
        private long runtime;
        private long startTime;
        private long endTime;
        private double onlineRate;
        private double avgPower;
        private double maxPower;
        private double minPower;
        private double cpuUsage;
        private long memoryUsage;
        private String message;
        private long lastUpdateTime;
        private boolean enableDetailedMonitoring;
        private String detailedMetrics;
        private int devicesPerCycle;
        private String monitoringMode;
        private long recommendedPollingIntervalMs;
        private boolean summaryOnly;

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder devices(int devices) {
            this.devices = devices;
            return this;
        }

        public Builder duration(int duration) {
            this.duration = duration;
            return this;
        }

        public Builder interval(double interval) {
            this.interval = interval;
            return this;
        }

        public Builder totalMessages(int totalMessages) {
            this.totalMessages = totalMessages;
            return this;
        }

        public Builder errorMessages(int errorMessages) {
            this.errorMessages = errorMessages;
            return this;
        }

        public Builder successMessages(int successMessages) {
            this.successMessages = successMessages;
            return this;
        }

        public Builder messageType(String messageType) {
            this.messageType = messageType;
            return this;
        }

        public Builder successRate(double successRate) {
            this.successRate = successRate;
            return this;
        }

        public Builder avgSendInterval(double avgSendInterval) {
            this.avgSendInterval = avgSendInterval;
            return this;
        }

        public Builder maxSendInterval(double maxSendInterval) {
            this.maxSendInterval = maxSendInterval;
            return this;
        }

        public Builder minSendInterval(double minSendInterval) {
            this.minSendInterval = minSendInterval;
            return this;
        }

        public Builder runtime(long runtime) {
            this.runtime = runtime;
            return this;
        }

        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(long endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder onlineRate(double onlineRate) {
            this.onlineRate = onlineRate;
            return this;
        }

        public Builder avgPower(double avgPower) {
            this.avgPower = avgPower;
            return this;
        }

        public Builder maxPower(double maxPower) {
            this.maxPower = maxPower;
            return this;
        }

        public Builder minPower(double minPower) {
            this.minPower = minPower;
            return this;
        }

        public Builder cpuUsage(double cpuUsage) {
            this.cpuUsage = cpuUsage;
            return this;
        }

        public Builder memoryUsage(long memoryUsage) {
            this.memoryUsage = memoryUsage;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder lastUpdateTime(long lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
            return this;
        }

        public Builder enableDetailedMonitoring(boolean enableDetailedMonitoring) {
            this.enableDetailedMonitoring = enableDetailedMonitoring;
            return this;
        }

        public Builder detailedMetrics(String detailedMetrics) {
            this.detailedMetrics = detailedMetrics;
            return this;
        }

        public Builder devicesPerCycle(int devicesPerCycle) {
            this.devicesPerCycle = devicesPerCycle;
            return this;
        }

        public Builder monitoringMode(String monitoringMode) {
            this.monitoringMode = monitoringMode;
            return this;
        }

        public Builder recommendedPollingIntervalMs(long recommendedPollingIntervalMs) {
            this.recommendedPollingIntervalMs = recommendedPollingIntervalMs;
            return this;
        }

        public Builder summaryOnly(boolean summaryOnly) {
            this.summaryOnly = summaryOnly;
            return this;
        }

        public MqttSimulatorStatus build() {
            MqttSimulatorStatus status = new MqttSimulatorStatus();
            status.taskId = this.taskId;
            status.status = this.status;
            status.devices = this.devices;
            status.duration = this.duration;
            status.interval = this.interval;
            status.totalMessages = this.totalMessages;
            status.errorMessages = this.errorMessages;
            status.successMessages = this.successMessages;
            status.messageType = this.messageType;
            status.successRate = this.successRate;
            status.avgSendInterval = this.avgSendInterval;
            status.maxSendInterval = this.maxSendInterval;
            status.minSendInterval = this.minSendInterval;
            status.runtime = this.runtime;
            status.startTime = this.startTime;
            status.endTime = this.endTime;
            status.onlineRate = this.onlineRate;
            status.avgPower = this.avgPower;
            status.maxPower = this.maxPower;
            status.minPower = this.minPower;
            status.cpuUsage = this.cpuUsage;
            status.memoryUsage = this.memoryUsage;
            status.message = this.message;
            status.lastUpdateTime = this.lastUpdateTime;
            status.enableDetailedMonitoring = this.enableDetailedMonitoring;
            status.detailedMetrics = this.detailedMetrics;
            status.devicesPerCycle = this.devicesPerCycle;
            status.monitoringMode = this.monitoringMode;
            status.recommendedPollingIntervalMs = this.recommendedPollingIntervalMs;
            status.summaryOnly = this.summaryOnly;
            return status;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getTaskId() { return taskId; }
    public String getStatus() { return status; }
    public int getDevices() { return devices; }
    public int getDuration() { return duration; }
    public double getInterval() { return interval; }
    public int getTotalMessages() { return totalMessages; }
    public int getErrorMessages() { return errorMessages; }
    public int getSuccessMessages() { return successMessages; }
    public String getMessageType() { return messageType; }
    public double getSuccessRate() { return successRate; }
    public double getAvgSendInterval() { return avgSendInterval; }
    public double getMaxSendInterval() { return maxSendInterval; }
    public double getMinSendInterval() { return minSendInterval; }
    public long getRuntime() { return runtime; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public double getOnlineRate() { return onlineRate; }
    public double getAvgPower() { return avgPower; }
    public double getMaxPower() { return maxPower; }
    public double getMinPower() { return minPower; }
    public double getCpuUsage() { return cpuUsage; }
    public long getMemoryUsage() { return memoryUsage; }
    public String getMessage() { return message; }
    public long getLastUpdateTime() { return lastUpdateTime; }
    public boolean isEnableDetailedMonitoring() { return enableDetailedMonitoring; }
    public String getDetailedMetrics() { return detailedMetrics; }
    public int getDevicesPerCycle() { return devicesPerCycle; }
    public String getMonitoringMode() { return monitoringMode; }
    public long getRecommendedPollingIntervalMs() { return recommendedPollingIntervalMs; }
    public boolean isSummaryOnly() { return summaryOnly; }

    // Setters
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public void setStatus(String status) { this.status = status; }
    public void setDevices(int devices) { this.devices = devices; }
    public void setDuration(int duration) { this.duration = duration; }
    public void setInterval(double interval) { this.interval = interval; }
    public void setTotalMessages(int totalMessages) { this.totalMessages = totalMessages; }
    public void setErrorMessages(int errorMessages) { this.errorMessages = errorMessages; }
    public void setSuccessMessages(int successMessages) { this.successMessages = successMessages; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }
    public void setAvgSendInterval(double avgSendInterval) { this.avgSendInterval = avgSendInterval; }
    public void setMaxSendInterval(double maxSendInterval) { this.maxSendInterval = maxSendInterval; }
    public void setMinSendInterval(double minSendInterval) { this.minSendInterval = minSendInterval; }
    public void setRuntime(long runtime) { this.runtime = runtime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public void setOnlineRate(double onlineRate) { this.onlineRate = onlineRate; }
    public void setAvgPower(double avgPower) { this.avgPower = avgPower; }
    public void setMaxPower(double maxPower) { this.maxPower = maxPower; }
    public void setMinPower(double minPower) { this.minPower = minPower; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
    public void setMemoryUsage(long memoryUsage) { this.memoryUsage = memoryUsage; }
    public void setMessage(String message) { this.message = message; }
    public void setLastUpdateTime(long lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
    public void setEnableDetailedMonitoring(boolean enableDetailedMonitoring) { this.enableDetailedMonitoring = enableDetailedMonitoring; }
    public void setDetailedMetrics(String detailedMetrics) { this.detailedMetrics = detailedMetrics; }
    public void setDevicesPerCycle(int devicesPerCycle) { this.devicesPerCycle = devicesPerCycle; }
    public void setMonitoringMode(String monitoringMode) { this.monitoringMode = monitoringMode; }
    public void setRecommendedPollingIntervalMs(long recommendedPollingIntervalMs) { this.recommendedPollingIntervalMs = recommendedPollingIntervalMs; }
    public void setSummaryOnly(boolean summaryOnly) { this.summaryOnly = summaryOnly; }
}