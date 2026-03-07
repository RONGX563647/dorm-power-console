package com.dormpower.dto;

/**
 * MQTT模拟器请求DTO
 */
public class MqttSimulatorRequest {
    /**
     * 设备名称前缀
     */
    private String deviceNamePrefix;
    
    /**
     * 设备数量
     */
    private int devices;
    
    /**
     * 持续时间(秒)
     */
    private int duration;
    
    /**
     * 发送间隔(秒)
     */
    private double interval;
    
    /**
     * 最小功率(瓦)
     */
    private double minPower;
    
    /**
     * 最大功率(瓦)
     */
    private double maxPower;
    
    /**
     * 消息类型（STATUS/TELEMETRY/MIXED）
     */
    private String messageType;
    
    /**
     * 是否启用详细监控
     */
    private boolean enableDetailedMonitoring;
    
    /**
     * 在线率
     */
    private double onlineRate;
    
    /**
     * 房间起始编号
     */
    private int roomStart;
    
    /**
     * 房间结束编号
     */
    private int roomEnd;
    
    /**
     * 是否启用心跳
     */
    private boolean enableHeartbeat;
    
    /**
     * 心跳间隔(秒)
     */
    private int heartbeatInterval;
    
    /**
     * Broker URL
     */
    private String brokerUrl;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 主题前缀
     */
    private String topicPrefix;
    
    /**
     * 最小电压
     */
    private double minVoltage;
    
    /**
     * 最大电压
     */
    private double maxVoltage;

    // 构造方法
    public MqttSimulatorRequest() {
    }

    public MqttSimulatorRequest(String deviceNamePrefix, int devices, int duration, double interval, 
                               double minPower, double maxPower, String messageType, 
                               boolean enableDetailedMonitoring, double onlineRate, 
                               int roomStart, int roomEnd, boolean enableHeartbeat, 
                               int heartbeatInterval, String brokerUrl, String username, 
                               String password, String topicPrefix, double minVoltage, 
                               double maxVoltage) {
        this.deviceNamePrefix = deviceNamePrefix;
        this.devices = devices;
        this.duration = duration;
        this.interval = interval;
        this.minPower = minPower;
        this.maxPower = maxPower;
        this.messageType = messageType;
        this.enableDetailedMonitoring = enableDetailedMonitoring;
        this.onlineRate = onlineRate;
        this.roomStart = roomStart;
        this.roomEnd = roomEnd;
        this.enableHeartbeat = enableHeartbeat;
        this.heartbeatInterval = heartbeatInterval;
        this.brokerUrl = brokerUrl;
        this.username = username;
        this.password = password;
        this.topicPrefix = topicPrefix;
        this.minVoltage = minVoltage;
        this.maxVoltage = maxVoltage;
    }

    // Getter和Setter方法
    public String getDeviceNamePrefix() {
        return deviceNamePrefix;
    }

    public void setDeviceNamePrefix(String deviceNamePrefix) {
        this.deviceNamePrefix = deviceNamePrefix;
    }

    public int getDevices() {
        return devices;
    }

    public void setDevices(int devices) {
        this.devices = devices;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public double getInterval() {
        return interval;
    }

    public void setInterval(double interval) {
        this.interval = interval;
    }

    public double getMinPower() {
        return minPower;
    }

    public void setMinPower(double minPower) {
        this.minPower = minPower;
    }

    public double getMaxPower() {
        return maxPower;
    }

    public void setMaxPower(double maxPower) {
        this.maxPower = maxPower;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public boolean isEnableDetailedMonitoring() {
        return enableDetailedMonitoring;
    }

    public void setEnableDetailedMonitoring(boolean enableDetailedMonitoring) {
        this.enableDetailedMonitoring = enableDetailedMonitoring;
    }

    public double getOnlineRate() {
        return onlineRate;
    }

    public void setOnlineRate(double onlineRate) {
        this.onlineRate = onlineRate;
    }

    public int getRoomStart() {
        return roomStart;
    }

    public void setRoomStart(int roomStart) {
        this.roomStart = roomStart;
    }

    public int getRoomEnd() {
        return roomEnd;
    }

    public void setRoomEnd(int roomEnd) {
        this.roomEnd = roomEnd;
    }

    public boolean isEnableHeartbeat() {
        return enableHeartbeat;
    }

    public void setEnableHeartbeat(boolean enableHeartbeat) {
        this.enableHeartbeat = enableHeartbeat;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public void setTopicPrefix(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }

    public double getMinVoltage() {
        return minVoltage;
    }

    public void setMinVoltage(double minVoltage) {
        this.minVoltage = minVoltage;
    }

    public double getMaxVoltage() {
        return maxVoltage;
    }

    public void setMaxVoltage(double maxVoltage) {
        this.maxVoltage = maxVoltage;
    }

    // Builder类
    public static class Builder {
        private String deviceNamePrefix;
        private int devices;
        private int duration;
        private double interval;
        private double minPower;
        private double maxPower;
        private String messageType;
        private boolean enableDetailedMonitoring;
        private double onlineRate;
        private int roomStart;
        private int roomEnd;
        private boolean enableHeartbeat;
        private int heartbeatInterval;
        private String brokerUrl;
        private String username;
        private String password;
        private String topicPrefix;
        private double minVoltage;
        private double maxVoltage;

        public Builder deviceNamePrefix(String deviceNamePrefix) {
            this.deviceNamePrefix = deviceNamePrefix;
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

        public Builder minPower(double minPower) {
            this.minPower = minPower;
            return this;
        }

        public Builder maxPower(double maxPower) {
            this.maxPower = maxPower;
            return this;
        }

        public Builder messageType(String messageType) {
            this.messageType = messageType;
            return this;
        }

        public Builder enableDetailedMonitoring(boolean enableDetailedMonitoring) {
            this.enableDetailedMonitoring = enableDetailedMonitoring;
            return this;
        }

        public Builder onlineRate(double onlineRate) {
            this.onlineRate = onlineRate;
            return this;
        }

        public Builder roomStart(int roomStart) {
            this.roomStart = roomStart;
            return this;
        }

        public Builder roomEnd(int roomEnd) {
            this.roomEnd = roomEnd;
            return this;
        }

        public Builder enableHeartbeat(boolean enableHeartbeat) {
            this.enableHeartbeat = enableHeartbeat;
            return this;
        }

        public Builder heartbeatInterval(int heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
            return this;
        }

        public Builder brokerUrl(String brokerUrl) {
            this.brokerUrl = brokerUrl;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder topicPrefix(String topicPrefix) {
            this.topicPrefix = topicPrefix;
            return this;
        }

        public Builder minVoltage(double minVoltage) {
            this.minVoltage = minVoltage;
            return this;
        }

        public Builder maxVoltage(double maxVoltage) {
            this.maxVoltage = maxVoltage;
            return this;
        }

        public MqttSimulatorRequest build() {
            return new MqttSimulatorRequest(
                deviceNamePrefix, devices, duration, interval, minPower, maxPower, messageType, 
                enableDetailedMonitoring, onlineRate, roomStart, roomEnd, enableHeartbeat, 
                heartbeatInterval, brokerUrl, username, password, topicPrefix, minVoltage, maxVoltage
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}