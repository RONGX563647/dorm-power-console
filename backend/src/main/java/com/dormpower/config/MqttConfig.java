package com.dormpower.config;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT配置类
 */
@Configuration
public class MqttConfig {

    @Value("${mqtt.enabled:false}")
    private boolean enabled;

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Value("${mqtt.topic-prefix:dorm}")
    private String topicPrefix;

    /**
     * 检查MQTT是否启用
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取主题前缀
     * @return 主题前缀
     */
    public String getTopicPrefix() {
        return topicPrefix;
    }

    /**
     * 构建主题
     * @param deviceId 设备ID
     * @param suffix 主题后缀
     * @return 完整主题
     */
    public String buildTopic(String deviceId, String suffix) {
        return topicPrefix + "/" + deviceId + "/" + suffix;
    }

    /**
     * 创建MQTT客户端实例
     * @return MqttClient实例，如果禁用或连接失败则返回null
     */
    public MqttClient createMqttClient() {
        if (!enabled) {
            System.out.println("MQTT is disabled via mqtt.enabled=false");
            return null;
        }

        try {
            MqttClient client = new MqttClient(brokerUrl, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            client.connect(options);
            System.out.println("MQTT connected to " + brokerUrl);
            return client;
        } catch (MqttException e) {
            System.err.println("Failed to connect to MQTT broker: " + e.getMessage());
            // 返回null，允许应用启动
            return null;
        }
    }

}
