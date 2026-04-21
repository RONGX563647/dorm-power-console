# 宿舍电源管理系统 - Kafka 使用文档

## 目录
- [1. 项目为什么使用 Kafka](#1-项目为什么使用-kafka)
- [2. 项目如何使用 Kafka](#2-项目如何使用-kafka)
- [3. 核心功能实现](#3-核心功能实现)
- [4. 消息流转架构](#4-消息流转架构)
- [5. 面试要点总结](#5-面试要点总结)

---

## 1. 项目为什么使用 Kafka

### 1.1 业务需求分析

宿舍电源管理系统是一个典型的 IoT（物联网）应用场景，具有以下数据处理需求：

| 业务需求 | 数据特点 | 传统方案问题 | Kafka 解决方案 |
|----------|----------|--------------|----------------|
| **高频遥测数据** | 设备每秒上报多条数据 | 数据库压力大 | 削峰填谷，批量处理 |
| **实时推送要求** | 毫秒级延迟 | 同步处理延迟高 | 解耦处理，异步推送 |
| **数据持久化** | 需要存储历史数据 | 直接写库性能差 | 先写 Kafka，再持久化 |
| **告警处理** | 需要实时检测 | 阻塞主流程 | 独立消费者处理 |
| **多消费者** | WebSocket、存储、告警 | 重复消费 | 分组订阅，各自处理 |

### 1.2 技术选型对比

| 对比维度 | Kafka | RabbitMQ | 直接数据库 |
|----------|-------|----------|------------|
| **吞吐量** | 百万级/秒 | 十万级/秒 | 万级/秒 |
| **消息持久化** | ✅ | ✅ | ✅ |
| **消息回溯** | ✅ | ✅ | ❌ |
| **消费者组** | ✅ | ✅ | ❌ |
| **消息堆积** | ✅ | 有限 | ❌ |
| **时序性** | 分区保证 | 队列保证 | 应用保证 |
| **适用场景** | 大数据、流处理 | 企业级消息 | 简单存储 |

### 1.3 Kafka 在项目中的核心价值

#### 1.3.1 削峰填谷

```
┌─────────────────────────────────────────────────────────────┐
│                      削峰填谷架构                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  设备上报（峰值：10000条/秒）                                │
│         │                                                   │
│         ▼                                                   │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                    Kafka 消息队列                       ││
│  │  吞吐量：100000条/秒                                   ││
│  │  消息堆积：可存储7天数据                               ││
│  └──────────────────────────┬──────────────────────────────┘│
│                             │                               │
│         ┌───────────────────┼───────────────────┐          │
│         ▼                   ▼                   ▼          │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │  WebSocket  │    │   数据库     │    │    告警     │  │
│  │  推送服务   │    │   存储服务   │    │   检测服务   │  │
│  │  100条/秒   │    │  500条/秒   │    │  1000条/秒  │  │
│  └──────────────┘    └──────────────┘    └──────────────┘  │
│                                                             │
│  效果：峰值期间消息堆积，谷值期间消费                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 1.3.2 服务解耦

```
┌─────────────────────────────────────────────────────────────┐
│                      服务解耦架构                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              MQTT 消息接收服务                        │   │
│  │  职责：接收设备上报数据                              │   │
│  │  输出：发送到 Kafka                                 │   │
│  └──────────────────────┬──────────────────────────────┘   │
│                         │                                    │
│                         ▼                                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Kafka Topic                            │   │
│  │  - dorm.telemetry: 遥测数据                        │   │
│  │  - dorm.device.status: 设备状态                   │   │
│  │  - dorm.alert: 告警数据                           │   │
│  └──────────────────────┬──────────────────────────────┘   │
│                         │                                    │
│         ┌───────────────┼───────────────┐                   │
│         ▼               ▼               ▼                   │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐      │
│  │ Telemetry   │ │   Status    │ │   Alert     │      │
│  │ Consumer    │ │  Consumer   │ │  Consumer   │      │
│  └──────────────┘ └──────────────┘ └──────────────┘      │
│         │               │               │                   │
│         ▼               ▼               ▼                   │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐      │
│  │ 批量写入DB   │ │ 更新设备状态 │ │  生成告警   │      │
│  └──────────────┘ └──────────────┘ └──────────────┘      │
│                                                             │
│  优势：新增消费者无需修改生产端                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 项目如何使用 Kafka

### 2.1 核心组件

#### 2.1.1 Kafka 配置类

**文件**：[KafkaConfig.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/config/KafkaConfig.java)

```java
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // ========== 生产者配置 ==========
    
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // 批量发送配置
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);           // 批量大小
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);    // 缓冲区大小
        
        // 可靠性配置
        config.put(ProducerConfig.ACKS_CONFIG, "all");                 // 所有副本确认
        config.put(ProducerConfig.RETRIES_CONFIG, 3);                 // 重试次数
        
        // 压缩配置
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");     // LZ4 压缩
        
        // 消息大小
        config.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1048576); // 1MB
        
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ========== 消费者配置 ==========
    
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "dorm-power-consumer");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        // 消费策略
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");   // 从最早开始消费
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);       // 手动提交
        
        // 消费能力
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);           // 单次拉取数量
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);   // 拉取间隔
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);      // 会话超时
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);   // 心跳间隔
        
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * 批量消费者监听器容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> batchKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // 批量消费
        factory.setBatchListener(true);
        
        // 手动提交偏移量
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // 并发数
        factory.setConcurrency(3);
        
        return factory;
    }

    /**
     * 单条消费者监听器容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> singleKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(3);
        
        return factory;
    }
}
```

**设计要点**：
1. **条件装配**：`@ConditionalOnProperty` 确保 Kafka 可用时才启用
2. **生产者优化**：批量发送、LZ4 压缩、全部副本确认
3. **消费者优化**：批量消费、手动提交、并发处理
4. **多种工厂**：支持批量和单条消费模式

### 2.2 消息生产者

**文件**：[TelemetryProducer.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/kafka/TelemetryProducer.java)

```java
@Component
public class TelemetryProducer {

    private static final String TELEMETRY_TOPIC = "dorm.telemetry";
    private static final String STATUS_TOPIC = "dorm.device.status";
    private static final String ALERT_TOPIC = "dorm.alert";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${kafka.enabled:true}")
    private boolean kafkaEnabled;

    /**
     * 发送遥测数据
     */
    public void sendTelemetry(String deviceId, long ts, double powerW, double voltageV, double currentA) {
        if (!kafkaEnabled) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("ts", ts);
        data.put("powerW", powerW);
        data.put("voltageV", voltageV);
        data.put("currentA", currentA);
        data.put("timestamp", System.currentTimeMillis());

        sendAsync(TELEMETRY_TOPIC, deviceId, data);
    }

    /**
     * 发送设备状态数据
     */
    public void sendDeviceStatus(String deviceId, boolean online, double totalPowerW,
                                  double voltageV, double currentA, String socketsJson) {
        if (!kafkaEnabled) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("online", online);
        data.put("totalPowerW", totalPowerW);
        data.put("voltageV", voltageV);
        data.put("currentA", currentA);
        data.put("socketsJson", socketsJson);
        data.put("timestamp", System.currentTimeMillis());

        sendAsync(STATUS_TOPIC, deviceId, data);
    }

    /**
     * 发送告警数据
     */
    public void sendAlert(String deviceId, String alertType, double alertValue, double threshold) {
        if (!kafkaEnabled) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("alertType", alertType);
        data.put("alertValue", alertValue);
        data.put("threshold", threshold);
        data.put("timestamp", System.currentTimeMillis());

        sendAsync(ALERT_TOPIC, deviceId, data);
    }

    /**
     * 异步发送消息
     */
    private void sendAsync(String topic, String key, Map<String, Object> data) {
        try {
            String message = objectMapper.writeValueAsString(data);
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(topic, key, message);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send message to Kafka: topic={}, key={}, error={}",
                            topic, key, ex.getMessage());
                } else {
                    logger.debug("Message sent to Kafka: topic={}, key={}, partition={}",
                            topic, key, result.getRecordMetadata().partition());
                }
            });
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize message: {}", e.getMessage());
        }
    }
}
```

**设计要点**：
1. **Topic 分流**：遥测数据、状态数据、告警数据分别发送到不同 Topic
2. **异步发送**：不阻塞 MQTT 消息处理
3. **Key 分区**：使用 deviceId 作为 Key，保证同一设备消息有序
4. **开关控制**：可通过配置启用/禁用 Kafka

### 2.3 消息消费者

**文件**：[TelemetryConsumer.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/kafka/TelemetryConsumer.java)

```java
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class TelemetryConsumer {

    /**
     * 批量消费遥测数据
     */
    @KafkaListener(
        topics = "dorm.telemetry",
        groupId = "dorm-power-telemetry",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeTelemetryBatch(List<ConsumerRecord<String, String>> records, 
                                     Acknowledgment acknowledgment) {
        if (records.isEmpty()) {
            return;
        }

        logger.debug("Received {} telemetry records", records.size());

        try {
            // 1. 解析消息
            List<Telemetry> telemetryList = new ArrayList<>(records.size());
            List<CompletableFuture<Void>> wsNotifications = new ArrayList<>();

            for (ConsumerRecord<String, String> record : records) {
                try {
                    JsonNode json = objectMapper.readTree(record.value());

                    Telemetry telemetry = new Telemetry();
                    telemetry.setDeviceId(json.get("deviceId").asText());
                    telemetry.setTs(json.get("ts").asLong());
                    telemetry.setPowerW(json.get("powerW").asDouble());
                    telemetry.setVoltageV(json.get("voltageV").asDouble());
                    telemetry.setCurrentA(json.get("currentA").asDouble());

                    telemetryList.add(telemetry);

                    // 异步发送 WebSocket 通知
                    wsNotifications.add(notifyTelemetryAsync(telemetry));

                    // 异步检查告警
                    checkAlertsAsync(telemetry);

                } catch (Exception e) {
                    logger.error("Failed to parse telemetry message: {}", e.getMessage());
                }
            }

            // 2. 批量写入数据库
            if (!telemetryList.isEmpty()) {
                telemetryBulkRepository.batchInsert(telemetryList);
                logger.info("Batch inserted {} telemetry records", telemetryList.size());
            }

            // 3. 更新设备最后在线时间
            updateDeviceLastSeen(telemetryList);

            // 4. 等待 WebSocket 通知完成
            CompletableFuture.allOf(wsNotifications.toArray(new CompletableFuture[0])).join();

            // 5. 手动提交偏移量
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process telemetry batch: {}", e.getMessage(), e);
            // 不提交偏移量，让 Kafka 重新投递
        }
    }

    /**
     * 批量消费设备状态数据
     */
    @KafkaListener(
        topics = "dorm.device.status",
        groupId = "dorm-power-status",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeStatusBatch(List<ConsumerRecord<String, String>> records, 
                                   Acknowledgment acknowledgment) {
        // 处理设备状态...
    }

    /**
     * 批量消费告警数据
     */
    @KafkaListener(
        topics = "dorm.alert",
        groupId = "dorm-power-alert",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeAlertBatch(List<ConsumerRecord<String, String>> records, 
                                  Acknowledgment acknowledgment) {
        // 处理告警数据...
    }

    /**
     * 异步发送 WebSocket 通知
     */
    @Async
    protected CompletableFuture<Void> notifyTelemetryAsync(Telemetry telemetry) {
        try {
            webSocketNotificationService.broadcastTelemetry(
                telemetry.getDeviceId(),
                objectMapper.valueToTree(Map.of(
                    "ts", telemetry.getTs(),
                    "powerW", telemetry.getPowerW(),
                    "voltageV", telemetry.getVoltageV(),
                    "currentA", telemetry.getCurrentA()
                ))
            );
        } catch (Exception e) {
            logger.debug("Failed to send WebSocket notification: {}", e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步检查告警
     */
    @Async
    protected void checkAlertsAsync(Telemetry telemetry) {
        try {
            alertService.checkAndGenerateAlert(telemetry.getDeviceId(), "power", telemetry.getPowerW());
            alertService.checkAndGenerateAlert(telemetry.getDeviceId(), "voltage", telemetry.getVoltageV());
            alertService.checkAndGenerateAlert(telemetry.getDeviceId(), "current", telemetry.getCurrentA());
        } catch (Exception e) {
            logger.debug("Failed to check alerts: {}", e.getMessage());
        }
    }
}
```

**设计要点**：
1. **批量消费**：一次拉取多条消息，提高吞吐量
2. **批量入库**：使用批量插入，减少数据库 IO
3. **异步处理**：WebSocket 通知和告警检查异步执行
4. **手动提交**：处理完成后手动提交偏移量，保证消息不丢失
5. **错误处理**：解析失败不影响其他消息处理

---

## 3. 核心功能实现

### 3.1 消息 Topic 设计

| Topic 名称 | 消息类型 | Key | 消费者组 | 处理逻辑 |
|------------|----------|-----|----------|----------|
| `dorm.telemetry` | 遥测数据 | deviceId | dorm-power-telemetry | 批量入库、WebSocket推送、告警检查 |
| `dorm.device.status` | 设备状态 | deviceId | dorm-power-status | 更新设备状态、WebSocket推送 |
| `dorm.alert` | 告警数据 | deviceId | dorm-power-alert | 生成告警记录、通知 |

### 3.2 消息格式

```json
// 遥测数据
{
    "deviceId": "device_001",
    "ts": 1640000000,
    "powerW": 150.5,
    "voltageV": 220.0,
    "currentA": 0.68,
    "timestamp": 1640000000000
}

// 设备状态
{
    "deviceId": "device_001",
    "online": true,
    "totalPowerW": 150.5,
    "voltageV": 220.0,
    "currentA": 0.68,
    "socketsJson": "[{\"id\":1,\"on\":true}]",
    "timestamp": 1640000000000
}

// 告警数据
{
    "deviceId": "device_001",
    "alertType": "power",
    "alertValue": 2500.0,
    "threshold": 2000.0,
    "timestamp": 1640000000000
}
```

---

## 4. 消息流转架构

### 4.1 完整消息流转

```
┌─────────────────────────────────────────────────────────────┐
│                      完整消息流转                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 设备上报数据                                           │
│     │                                                       │
│     │ MQTT: dorm/{room}/{device}/telemetry                 │
│     │                                                       │
│     ▼                                                       │
│  2. MqttBridge 接收消息                                    │
│     │                                                       │
│     │ - 解析 MQTT 消息                                      │
│     │ - 调用 TelemetryProducer 发送到 Kafka                 │
│     │                                                       │
│     ▼                                                       │
│  3. Kafka Topic                                            │
│     │                                                       │
│     │ 3个 Topic:                                           │
│     │ - dorm.telemetry (遥测数据)                         │
│     │ - dorm.device.status (设备状态)                     │
│     │ - dorm.alert (告警数据)                              │
│     │                                                       │
│     ▼                                                       │
│  4. 消费者处理                                             │
│     │                                                       │
│     ├──> TelemetryConsumer (遥测数据)                       │
│     │     ├─ 批量写入数据库                                │
│     │     ├─ 异步发送 WebSocket 通知                      │
│     │     └─ 异步检查告警                                 │
│     │                                                       │
│     ├──> StatusConsumer (设备状态)                         │
│     │     ├─ 更新设备状态                                  │
│     │     └─ WebSocket 推送                               │
│     │                                                       │
│     └──> AlertConsumer (告警数据)                          │
│           └─ 生成告警记录                                  │
│                                                             │
│  5. 前端接收                                               │
│     │                                                       │
│     │ WebSocket 推送                                        │
│     │                                                       │
│     ▼                                                       │
│  6. 界面更新                                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 消息处理流程

```
┌─────────────────────────────────────────────────────────────┐
│                    消息处理流程                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Kafka Consumer                                            │
│     │                                                       │
│     │ 1. 拉取消息 (poll)                                   │
│     │                                                       │
│     ▼                                                       │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  批量解析                                               ││
│  │  - 解析 JSON                                            ││
│  │  - 转换为实体对象                                       ││
│  │  - 错误跳过，继续处理                                    ││
│  └─────────────────────────────────────────────────────────┘│
│     │                                                       │
│     │ 并行处理                                              │
│     ▼                                                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐│
│  │ 批量写入数据库 │  │ WebSocket 推送  │  │ 告警检查   ││
│  │                 │  │                 │  │             ││
│  │ JDBC Batch      │  │ @Async 异步     │  │ @Async 异步 ││
│  │ Insert          │  │ 通知前端        │  │ 检测阈值   ││
│  └─────────────────┘  └─────────────────┘  └─────────────┘│
│     │                                                       │
│     │ 等待所有异步任务完成                                  │
│     ▼                                                       │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ 手动提交偏移量 (acknowledge)                           ││
│  └─────────────────────────────────────────────────────────┘│
│     │                                                       │
│     │ 处理失败：不提交，Kafka 重新投递                       │
│     │ 处理成功：提交，Kafka 记录消费位置                    │
│     │                                                       │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. 面试要点总结

### 5.1 项目实战问题

#### Q1: 你们项目为什么使用 Kafka？

**答案**：

我们项目是一个 IoT 宿舍电源管理系统，使用 Kafka 主要有以下几个原因：

1. **高吞吐量需求**：
   - 设备每秒上报多条遥测数据
   - 峰值可达 10000 条/秒
   - Kafka 可支持百万级吞吐量

2. **削峰填谷**：
   - 设备上报不均匀
   - Kafka 可缓冲消息，平衡负载

3. **服务解耦**：
   - MQTT 接收服务与业务处理解耦
   - 新增消费者无需修改生产端

4. **异步处理**：
   - 消息持久化、WebSocket 推送、告警检查异步处理
   - 不阻塞主流程

#### Q2: 你们项目中 Kafka 用在哪些场景？

**答案**：

1. **遥测数据处理**：
   - 接收 MQTT 转发的遥测数据
   - 批量写入数据库
   - 触发 WebSocket 推送

2. **设备状态更新**：
   - 接收设备状态数据
   - 更新设备在线状态
   - WebSocket 通知前端

3. **告警数据处理**：
   - 接收告警数据
   - 生成告警记录
   - 发送通知

#### Q3: 你们如何保证消息不丢失？

**答案**：

1. **生产者端**：
   ```java
   // 确认模式：所有副本确认
   config.put(ProducerConfig.ACKS_CONFIG, "all");
   
   // 重试次数
   config.put(ProducerConfig.RETRIES_CONFIG, 3);
   ```

2. **消费者端**：
   ```java
   // 手动提交偏移量
   factory.getContainerProperties().setAckMode(
       ContainerProperties.AckMode.MANUAL_IMMEDIATE
   );
   
   // 处理成功后提交
   acknowledgment.acknowledge();
   ```

3. **失败处理**：
   - 处理失败不提交，Kafka 会重新投递
   - 日志记录失败消息

#### Q4: 你们如何保证消息顺序？

**答案**：

1. **使用 Key 分区**：
   - 使用 deviceId 作为消息 Key
   - 相同 deviceId 的消息发送到同一分区
   - 同一分区内消息有序

2. **单 Topic 设计**：
   - 按数据类型分为不同 Topic
   - 同一设备的数据在同分区有序

#### Q5: 你们如何处理高并发？

**答案**：

1. **批量消费**：
   ```java
   // 一次拉取多条消息
   factory.setBatchListener(true);
   config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
   ```

2. **批量入库**：
   ```java
   // JDBC 批量插入
   telemetryBulkRepository.batchInsert(telemetryList);
   ```

3. **异步处理**：
   ```java
   // WebSocket 通知和告警检查异步执行
   @Async
   protected CompletableFuture<Void> notifyTelemetryAsync(...)
   ```

4. **并发消费**：
   ```java
   // 设置并发数
   factory.setConcurrency(3);
   ```

#### Q6: Kafka 消息积压怎么办？

**答案**：

1. **增加消费者**：
   - 增加消费者组内消费者数量
   - 注意：不能超过分区数量

2. **优化消费者性能**：
   - 减少单次处理时间
   - 使用批量处理

3. **增加分区**：
   - 增加 Topic 分区数量
   - 重新分配分区

4. **消息过期策略**：
   - 设置合理过期时间
   - 过期消息可丢弃

### 5.2 技术深度问题

#### Q7: 你们如何设计 Kafka Topic？

**答案**：

我们按数据类型设计 Topic：

1. **dorm.telemetry**：遥测数据
   - 数据量大，需要高吞吐
   - 批量消费、批量入库

2. **dorm.device.status**：设备状态
   - 状态变化时发送
   - 更新设备状态

3. **dorm.alert**：告警数据
   - 告警时发送
   - 生成告警记录

**设计原则**：
- 按业务类型分离
- 相同 Key 的消息在同一分区
- 便于独立扩展

#### Q8: Kafka 和 MQTT 的关系？

**答案**：

在项目中，Kafka 和 MQTT 承担不同角色：

| 对比 | MQTT | Kafka |
|------|------|-------|
| **协议层** | 应用层协议 | 应用层协议 |
| **通信模式** | 发布/订阅 | 发布/订阅 |
| **使用场景** | 设备 <-> 服务器 | 服务器 <-> 后端服务 |
| **吞吐量** | 中等 | 极高 |
| **消息持久化** | 可选 | 必须 |
| **消费者模式** | 推送 | 拉取 |

**项目中的使用**：
```
IoT设备 --MQTT--> 后端服务 --Kafka--> 消费者
```

#### Q9: 你们如何保证消息不重复消费？

**答案**：

1. **幂等性处理**：
   - 数据库使用唯一键约束
   - 重复消息会插入失败

2. **手动提交**：
   - 处理完成后手动提交
   - 失败时不提交，Kafka 会重新投递

3. **去重逻辑**：
   - 根据 deviceId + ts 去重
   - 相同数据不重复处理

#### Q10: Kafka 性能优化有哪些？

**答案**：

1. **生产者优化**：
   - 批量发送：`batch.size=16384`
   - 压缩：`compression.type=lz4`
   - 异步发送：不阻塞

2. **消费者优化**：
   - 批量消费：`batch.listener=true`
   - 手动提交：避免自动提交丢失
   - 并发消费：`concurrency=3`

3. **Broker 优化**：
   - 增加分区数
   - 合理设置副本因子
   - 调整日志保留时间

---

## 总结

本文档详细介绍了宿舍电源管理系统中 Kafka 的使用：

1. **为什么用**：高吞吐量需求、削峰填谷、服务解耦、异步处理
2. **怎么用**：Topic 设计、消息格式、生产者、消费者配置
3. **面试要点**：项目实战问题、技术深度问题

通过实际项目实践，深入理解 Kafka 的应用场景和实现原理，为面试和实际开发提供参考。

---

**文档版本**：v1.0  
**编写日期**：2026年3月14日  
**作者**：DormPower Team
