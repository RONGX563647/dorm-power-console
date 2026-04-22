# 模块17：MQTT桥接与IoT通信系统

> **学习时长**: 4-6 天  
> **难度**: ⭐⭐⭐⭐⭐  
> **前置知识**: 网络协议基础、MQTT协议、消息队列概念

---

## 一、系统架构

### 1.1 IoT通信架构

```
设备端                          服务端                          前端
┌─────────┐                  ┌──────────────┐               ┌─────────┐
│ IoT设备  │◄────MQTT────►│ MqttBridge   │               │ 浏览器  │
│ (ESP32)  │  发布/订阅     │              │               │         │
└─────────┘                  │  ├─────►│RabbitMQ    │               │         │
                            │  │           │◄──WebSocket──►│         │
                            │  ├─────►│Kafka       │               └─────────┘
                            └──────────────┘

数据流向:
1. 设备 → MQTT Broker → MqttBridge → RabbitMQ → 数据库
2. 数据库 → WebSocket → 浏览器（实时展示）
3. 浏览器 → REST API → RabbitMQ → MqttBridge → MQTT → 设备（控制命令）
```

### 1.2 MQTT协议基础

```yaml
MQTT核心概念:
  发布/订阅模式:
    - Publisher: 消息发布者（设备）
    - Subscriber: 消息订阅者（服务端）
    - Broker: 消息代理（Mosquitto）
    - Topic: 消息主题（路由）
  
  QoS等级:
    - QoS 0: 最多一次（可能丢失）
    - QoS 1: 至少一次（可能重复）
    - QoS 2: 恰好一次（最可靠）
  
  消息类型:
    - CONNECT: 客户端连接
    - PUBLISH: 发布消息
    - SUBSCRIBE: 订阅主题
    - UNSUBSCRIBE: 取消订阅
    - PINGREQ: 心跳请求
    - DISCONNECT: 断开连接
```

---

## 二、核心组件

### 2.1 MqttBridge MQTT桥接

**文件位置**: `backend/src/main/java/com/dormpower/mqtt/MqttBridge.java`

```java
package com.dormpower.mqtt;

import com.dormpower.config.MqttConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MQTT桥接服务
 * 
 * 功能:
 * 1. 连接MQTT Broker
 * 2. 订阅设备主题
 * 3. 转发消息到RabbitMQ/Kafka
 * 4. 下发命令到设备
 * 
 * 技术亮点:
 * - 自动重连机制
 * - 连接池管理
 * - 消息过滤与路由
 * - 异常处理与降级
 * 
 * @author dormpower team
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttBridge {

    private final MqttConfig mqttConfig;
    private final RabbitMQProducer rabbitMQProducer;
    private final ObjectMapper objectMapper;
    
    private MqttClient mqttClient;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    
    // 设备在线状态缓存
    private final Map<String, Boolean> deviceOnlineStatus = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        if (!mqttConfig.isEnabled()) {
            log.info("MQTT is disabled, skipping initialization");
            return;
        }
        
        try {
            connect();
            subscribeTopics();
        } catch (MqttException e) {
            log.error("Failed to initialize MQTT bridge", e);
        }
    }
    
    /**
     * 连接MQTT Broker
     */
    private void connect() throws MqttException {
        String brokerUrl = mqttConfig.getBrokerUrl();
        String clientId = mqttConfig.getClientId();
        
        // 创建MQTT客户端
        mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        
        // 配置连接选项
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(mqttConfig.getUsername());
        options.setPassword(mqttConfig.getPassword().toCharArray());
        options.setConnectionTimeout(10);          // 连接超时10秒
        options.setKeepAliveInterval(60);          // 心跳间隔60秒
        options.setAutomaticReconnect(true);       // 自动重连
        options.setCleanSession(false);            // 保留Session
        
        // 设置回调
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.error("MQTT connection lost: {}", cause.getMessage());
                connected.set(false);
            }
            
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                handleMessage(topic, message);
            }
            
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                log.debug("Message delivered: {}", token.getMessageId());
            }
        });
        
        // 连接
        mqttClient.connect(options);
        connected.set(true);
        
        log.info("MQTT connected to {}", brokerUrl);
    }
    
    /**
     * 订阅主题
     */
    private void subscribeTopics() throws MqttException {
        Map<String, String> topics = mqttConfig.getTopics();
        
        for (Map.Entry<String, String> entry : topics.entrySet()) {
            String topicKey = entry.getKey();
            String topicPattern = mqttConfig.getFullTopic(topicKey);
            
            // 订阅主题（QoS 1）
            mqttClient.subscribe(topicPattern + "/+", 1);
            
            log.info("Subscribed to topic: {}", topicPattern);
        }
    }
    
    /**
     * 处理接收到的消息
     */
    private void handleMessage(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            
            // 解析主题
            String[] parts = topic.split("/");
            if (parts.length < 3) {
                log.warn("Invalid topic format: {}", topic);
                return;
            }
            
            String topicType = parts[1];  // status, telemetry, event
            String deviceId = parts[2];   // 设备ID
            
            log.debug("Received message: topic={}, device={}, payload={}", 
                     topicType, deviceId, payload);
            
            // 根据主题类型路由
            switch (topicType) {
                case "telemetry":
                    handleTelemetry(deviceId, payload);
                    break;
                case "status":
                    handleDeviceStatus(deviceId, payload);
                    break;
                case "event":
                    handleDeviceEvent(deviceId, payload);
                    break;
                default:
                    log.warn("Unknown topic type: {}", topicType);
            }
            
        } catch (Exception e) {
            log.error("Failed to handle MQTT message: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理遥测数据
     */
    private void handleTelemetry(String deviceId, String payload) {
        try {
            // 转发到RabbitMQ
            rabbitMQProducer.sendTelemetry(deviceId, payload);
        } catch (Exception e) {
            log.error("Failed to forward telemetry: {}", e.getMessage());
        }
    }
    
    /**
     * 处理设备状态
     */
    private void handleDeviceStatus(String deviceId, String payload) {
        try {
            boolean online = payload.contains("online");
            deviceOnlineStatus.put(deviceId, online);
            
            rabbitMQProducer.sendDeviceStatus(deviceId, payload);
        } catch (Exception e) {
            log.error("Failed to handle device status: {}", e.getMessage());
        }
    }
    
    /**
     * 处理设备事件
     */
    private void handleDeviceEvent(String deviceId, String payload) {
        try {
            rabbitMQProducer.sendEvent(deviceId, payload);
        } catch (Exception e) {
            log.error("Failed to handle device event: {}", e.getMessage());
        }
    }
    
    /**
     * 下发命令到设备
     */
    public void sendCommand(String deviceId, String command) {
        try {
            String topic = mqttConfig.getDeviceTopic("cmd", deviceId);
            
            MqttMessage message = new MqttMessage(command.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);  // QoS 1 至少一次
            message.setRetained(false);
            
            mqttClient.publish(topic, message);
            
            log.info("Command sent: device={}, topic={}", deviceId, topic);
        } catch (MqttException e) {
            log.error("Failed to send command: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send command", e);
        }
    }
    
    @PreDestroy
    public void destroy() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
                log.info("MQTT disconnected");
            } catch (MqttException e) {
                log.error("Failed to disconnect MQTT", e);
            }
        }
    }
    
    public boolean isConnected() {
        return connected.get();
    }
}
```

### 2.2 RabbitMQProducer 消息生产者

**文件位置**: `backend/src/main/java/com/dormpower/mqtt/RabbitMQProducer.java`

```java
package com.dormpower.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ消息生产者
 * 
 * 功能:
 * 1. 将MQTT消息转发到RabbitMQ
 * 2. 支持不同消息类型路由
 * 
 * @author dormpower team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 发送遥测数据
     */
    public void sendTelemetry(String deviceId, String payload) {
        rabbitTemplate.convertAndSend(
            "dormpower.direct",      // 交换机
            "telemetry",             // 路由键
            payload                  // 消息
        );
        
        log.debug("Telemetry sent to RabbitMQ: device={}", deviceId);
    }
    
    /**
     * 发送设备状态
     */
    public void sendDeviceStatus(String deviceId, String payload) {
        rabbitTemplate.convertAndSend(
            "dormpower.direct",
            "device.status",
            payload
        );
    }
    
    /**
     * 发送设备事件
     */
    public void sendEvent(String deviceId, String payload) {
        rabbitTemplate.convertAndSend(
            "dormpower.direct",
            "device.event",
            payload
        );
    }
}
```

---

## 三、MQTT主题设计

### 3.1 主题结构

```yaml
主题命名规范:
  格式: dorm/{type}/{deviceId}
  
  主题列表:
    - dorm/telemetry/{deviceId}:   遥测数据（电压、电流、功率）
    - dorm/status/{deviceId}:      设备状态（在线/离线）
    - dorm/cmd/{deviceId}:         命令下发（开关、配置）
    - dorm/ack/{deviceId}:         命令确认
    - dorm/event/{deviceId}:       设备事件（告警、故障）
  
  示例:
    - dorm/telemetry/device_001
    - dorm/status/A1-301-plug
    - dorm/cmd/device_002
```

### 3.2 消息格式

```json
// 遥测数据
{
  "deviceId": "device_001",
  "ts": 1642234567,
  "voltageV": 220.5,
  "currentA": 5.12,
  "powerW": 1128.6,
  "energyKwh": 125.34
}

// 设备状态
{
  "deviceId": "device_001",
  "online": true,
  "ts": 1642234567
}

// 命令下发
{
  "cmd": "switch",
  "action": "on",
  "timestamp": 1642234567
}
```

---

## 四、扩展练习

### 练习1：实现命令确认机制

1. 设备收到命令后发送ACK
2. 服务端记录命令状态
3. 超时未确认则重试

### 练习2：实现QoS优化

1. 根据消息类型选择QoS等级
2. 遥测数据使用QoS 0
3. 命令下发使用QoS 1

---

**最后更新**: 2026-04-22  
**文档版本**: 1.0
