package com.dormpower.dto;

/**
 * MQTT模拟器任务列表摘要DTO
 */
public class MqttSimulatorTaskSummary {
    private String taskId;
    private String status;
    private int devices;
    private int totalMessages;
    private double successRate;
    private long runtime;
    private boolean summaryOnly;
    private String monitoringMode;
    private long lastUpdateTime;

    public MqttSimulatorTaskSummary() {}

    public static class Builder {
        private String taskId;
        private String status;
        private int devices;
        private int totalMessages;
        private double successRate;
        private long runtime;
        private boolean summaryOnly;
        private String monitoringMode;
        private long lastUpdateTime;

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

        public Builder totalMessages(int totalMessages) {
            this.totalMessages = totalMessages;
            return this;
        }

        public Builder successRate(double successRate) {
            this.successRate = successRate;
            return this;
        }

        public Builder runtime(long runtime) {
            this.runtime = runtime;
            return this;
        }

        public Builder summaryOnly(boolean summaryOnly) {
            this.summaryOnly = summaryOnly;
            return this;
        }

        public Builder monitoringMode(String monitoringMode) {
            this.monitoringMode = monitoringMode;
            return this;
        }

        public Builder lastUpdateTime(long lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
            return this;
        }

        public MqttSimulatorTaskSummary build() {
            MqttSimulatorTaskSummary summary = new MqttSimulatorTaskSummary();
            summary.taskId = this.taskId;
            summary.status = this.status;
            summary.devices = this.devices;
            summary.totalMessages = this.totalMessages;
            summary.successRate = this.successRate;
            summary.runtime = this.runtime;
            summary.summaryOnly = this.summaryOnly;
            summary.monitoringMode = this.monitoringMode;
            summary.lastUpdateTime = this.lastUpdateTime;
            return summary;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getDevices() {
        return devices;
    }

    public void setDevices(int devices) {
        this.devices = devices;
    }

    public int getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(int totalMessages) {
        this.totalMessages = totalMessages;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public long getRuntime() {
        return runtime;
    }

    public void setRuntime(long runtime) {
        this.runtime = runtime;
    }

    public boolean isSummaryOnly() {
        return summaryOnly;
    }

    public void setSummaryOnly(boolean summaryOnly) {
        this.summaryOnly = summaryOnly;
    }

    public String getMonitoringMode() {
        return monitoringMode;
    }

    public void setMonitoringMode(String monitoringMode) {
        this.monitoringMode = monitoringMode;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}