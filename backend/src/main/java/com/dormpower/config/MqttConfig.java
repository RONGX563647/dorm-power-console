package com.dormpower.config;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT配置类
 */
/**
 * MQTT配置类，用于配置和管理MQTT连接参数和客户端实例
 */
@Configuration
public class MqttConfig {

    // MQTT是否启用的配置项，默认为false
    @Value("${mqtt.enabled:false}")
    private boolean enabled;

    // MQTT代理服务器的URL
    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    // MQTT客户端的唯一标识符
    @Value("${mqtt.client-id}")
    private String clientId;

    // MQTT连接的用户名
    @Value("${mqtt.username}")
    private String username;

    // MQTT连接的密码
    @Value("${mqtt.password}")
    private String password;

    // MQTT主题的前缀，默认为"dorm"
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
            // 创建MQTT客户端实例
            MqttClient client = new MqttClient(brokerUrl, clientId);
            // 创建MQTT连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            
            // 设置认证信息
            if (username != null && !username.isEmpty() && !"admin".equals(username)) {
                options.setUserName(username);
                options.setPassword(password.toCharArray());
                System.out.println("MQTT connecting with authentication");
            } else {
                System.out.println("MQTT connecting without authentication");
            }
            

            
            // 设置连接参数
            options.setAutomaticReconnect(true); // 自动重连
            options.setCleanSession(true);       // 清除会话
            options.setConnectionTimeout(10);    // 连接超时时间(秒)
            options.setKeepAliveInterval(60);    // 心跳间隔(秒)
            // 连接MQTT代理服务器
            client.connect(options);
            System.out.println("MQTT connected to " + brokerUrl);
            return client;
        } catch (MqttException e) {
            System.err.println("Failed to connect to MQTT broker: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

}
