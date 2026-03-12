package com.dormpower.service;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import java.util.function.BiConsumer;

/**
 * MQTT服务
 */
@Service
public class MqttService {

    private MqttClient mqttClient;
    private BiConsumer<String, String> messageHandler;

    /**
     * 发送MQTT消息
     * @param topic 主题
     * @param message 消息内容
     * @throws MqttException MQTT异常
     */
    public void sendMessage(String topic, String message) throws MqttException {
        if (mqttClient != null && mqttClient.isConnected()) {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);
            mqttClient.publish(topic, mqttMessage);
        } else {
            System.err.println("MQTT client is not initialized or not connected");
        }
    }

    /**
     * 订阅MQTT主题
     * @param topic 主题
     * @throws MqttException MQTT异常
     */
    public void subscribe(String topic) throws MqttException {
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.subscribe(topic, (topic1, message) -> {
                String payload = new String(message.getPayload());
                System.out.println("Received message on topic " + topic1 + ": " + payload);
                if (messageHandler != null) {
                    messageHandler.accept(topic1, payload);
                }
            });
        } else {
            System.err.println("MQTT client is not initialized or not connected");
        }
    }

    /**
     * 设置MQTT客户端
     * @param mqttClient MqttClient实例
     */
    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
        if (mqttClient != null) {
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("MQTT connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    System.out.println("Message arrived on topic " + topic + ": " + payload);
                    if (messageHandler != null) {
                        messageHandler.accept(topic, payload);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message delivery complete");
                }
            });
        }
    }

    /**
     * 设置消息处理器
     * @param handler 消息处理器
     */
    public void setMessageHandler(BiConsumer<String, String> handler) {
        this.messageHandler = handler;
    }

    /**
     * 检查MQTT客户端是否已连接
     * @return 是否已连接
     */
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    /**
     * 断开MQTT连接
     * @throws MqttException MQTT异常
     */
    public void disconnect() throws MqttException {
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.disconnect();
        }
    }

}