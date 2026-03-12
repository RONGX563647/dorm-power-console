package com.dormpower.dto;

/**
 * MQTT模拟器响应DTO
 */
public class MqttSimulatorResponse {
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 消息
     */
    private String message;

    // 构造方法
    public MqttSimulatorResponse() {
    }

    public MqttSimulatorResponse(String taskId, String status, String message) {
        this.taskId = taskId;
        this.status = status;
        this.message = message;
    }

    // Getter和Setter方法
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Builder类
    public static class Builder {
        private String taskId;
        private String status;
        private String message;

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public MqttSimulatorResponse build() {
            return new MqttSimulatorResponse(taskId, status, message);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
