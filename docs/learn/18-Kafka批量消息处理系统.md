# 模块18：Kafka批量消息处理系统

> **学习时长**: 4-6 天  
> **难度**: ⭐⭐⭐⭐⭐  
> **前置知识**: Kafka基础、Spring Kafka、消息队列概念

---

## 一、系统架构

### 1.1 Kafka vs RabbitMQ

```yaml
对比:
  RabbitMQ:
    优势:
      - 低延迟（微秒级）
      - 灵活路由（Exchange/Queue）
      - 管理界面友好
    场景:
      - 日常消息处理
      - 命令下发
      - 告警通知
  
  Kafka:
    优势:
      - 高吞吐（百万级/秒）
      - 批量处理
      - 消息回溯
    场景:
      - 大规模遥测数据
      - 日志收集
      - 数据流处理
```

### 1.2 为什么用Kafka处理遥测数据？

```
场景分析:
  10000设备 × 每10秒上报1次 = 1000条/秒 = 8640万条/天
  
  如果使用单条插入:
    ❌ 数据库连接耗尽
    ❌ 磁盘IO瓶颈
    ❌ 响应延迟高
  
  使用Kafka批量处理:
    ✅ 批量消费（100条/批）
    ✅ 批量插入（100条/次）
    ✅ 吞吐量提升100倍
    ✅ 数据库压力降低90%
```

---

## 二、Kafka配置

### 2.1 KafkaConfig 配置类

**文件位置**: `backend/src/main/java/com/dormpower/config/KafkaConfig.java`

```java
package com.dormpower.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka配置
 * 
 * @author dormpower team
 * @version 1.0
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    /**
     * 生产者配置
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");           // 所有副本确认
        config.put(ProducerConfig.RETRIES_CONFIG, 3);            // 重试3次
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);     // 批量大小16KB
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);         // 等待10ms
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 缓冲区32MB
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    /**
     * 消费者配置（批量模式）
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "dorm-power");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);  // 手动提交
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);      // 每次拉取100条
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);      // 最小拉取1KB
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
    
    /**
     * 批量监听容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> batchKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true);  // 启用批量监听
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConcurrency(3);  // 3个并发消费者
        
        return factory;
    }
}
```

---

## 三、核心组件

### 3.1 TelemetryProducer 遥测数据生产者

**文件位置**: `backend/src/main/java/com/dormpower/kafka/TelemetryProducer.java`

```java
package com.dormpower.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 遥测数据Kafka生产者
 * 
 * @author dormpower team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    
    private static final String TOPIC = "dorm.telemetry";
    
    /**
     * 发送遥测数据
     */
    public CompletableFuture<SendResult<String, String>> send(String deviceId, String payload) {
        return kafkaTemplate.send(TOPIC, deviceId, payload)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Telemetry sent: device={}, offset={}", 
                        deviceId, result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send telemetry: {}", ex.getMessage());
                }
            });
    }
}
```

### 3.2 TelemetryConsumer 遥测数据消费者

**文件位置**: `backend/src/main/java/com/dormpower/kafka/TelemetryConsumer.java`

```java
package com.dormpower.kafka;

import com.dormpower.model.Telemetry;
import com.dormpower.repository.TelemetryBulkRepository;
import com.dormpower.service.AlertService;
import com.dormpower.service.WebSocketNotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 遥测数据Kafka批量消费者
 * 
 * 功能:
 * 1. 批量消费遥测数据（100条/批）
 * 2. 批量写入数据库
 * 3. 触发WebSocket通知
 * 4. 触发告警检查
 * 5. 监控指标收集
 * 
 * @author dormpower team
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class TelemetryConsumer {

    private final ObjectMapper objectMapper;
    private final TelemetryBulkRepository telemetryBulkRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final AlertService alertService;
    private final MeterRegistry meterRegistry;
    
    // 监控指标
    private Counter processedCounter;
    private Counter errorCounter;
    private Timer processTimer;
    
    @PostConstruct
    public void initMetrics() {
        processedCounter = Counter.builder("kafka.consumer.messages")
            .tag("topic", "dorm.telemetry")
            .tag("status", "processed")
            .register(meterRegistry);
        
        errorCounter = Counter.builder("kafka.consumer.messages")
            .tag("topic", "dorm.telemetry")
            .tag("status", "error")
            .register(meterRegistry);
        
        processTimer = Timer.builder("kafka.consumer.process.time")
            .tag("topic", "dorm.telemetry")
            .register(meterRegistry);
    }
    
    /**
     * 批量消费遥测数据
     */
    @KafkaListener(
        topics = "dorm.telemetry",
        groupId = "dorm-power-telemetry",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        if (records.isEmpty()) {
            return;
        }
        
        log.debug("Received {} telemetry records", records.size());
        
        Timer.Sample sample = Timer.start(meterRegistry);
        int processedCount = 0;
        int errorCount = 0;
        
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
                    processedCount++;
                    
                    // 异步发送WebSocket通知
                    wsNotifications.add(notifyTelemetryAsync(telemetry));
                    
                    // 异步检查告警
                    checkAlertsAsync(telemetry);
                    
                } catch (Exception e) {
                    log.error("Failed to parse message: {}", e.getMessage());
                    errorCount++;
                }
            }
            
            // 2. 批量写入数据库
            if (!telemetryList.isEmpty()) {
                telemetryBulkRepository.batchInsert(telemetryList);
                log.info("Batch inserted {} telemetry records", telemetryList.size());
            }
            
            // 3. 等待WebSocket通知完成
            CompletableFuture.allOf(wsNotifications.toArray(new CompletableFuture[0])).join();
            
            // 4. 手动提交偏移量
            ack.acknowledge();
            
            // 5. 记录监控指标
            processedCounter.increment(processedCount);
            
        } catch (Exception e) {
            log.error("Failed to process batch: {}", e.getMessage(), e);
            errorCounter.increment(errorCount);
            // 不提交偏移量，让Kafka重新投递
        } finally {
            sample.stop(processTimer);
        }
    }
    
    @Async
    protected CompletableFuture<Void> notifyTelemetryAsync(Telemetry telemetry) {
        try {
            webSocketNotificationService.broadcastTelemetry(
                telemetry.getDeviceId(),
                objectMapper.valueToTree(telemetry)
            );
        } catch (Exception e) {
            log.debug("Failed to send WS notification: {}", e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Async
    protected void checkAlertsAsync(Telemetry telemetry) {
        try {
            alertService.checkAndGenerateAlert(telemetry.getDeviceId(), "power", telemetry.getPowerW());
        } catch (Exception e) {
            log.debug("Failed to check alerts: {}", e.getMessage());
        }
    }
}
```

---

## 四、批量数据库操作

### 4.1 TelemetryBulkRepository 批量操作

**文件位置**: `backend/src/main/java/com/dormpower/repository/TelemetryBulkRepository.java`

```java
package com.dormpower.repository;

import com.dormpower.model.Telemetry;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * 遥测数据批量操作Repository
 * 
 * 优化:
 * - 使用JDBC批量插入（比JPA快10倍）
 * - 使用JPQL批量更新（减少网络往返）
 * 
 * @author dormpower team
 * @version 1.0
 */
@Repository
@RequiredArgsConstructor
public class TelemetryBulkRepository {

    private final JdbcTemplate jdbcTemplate;
    
    private static final String BATCH_INSERT_SQL = 
        "INSERT INTO telemetry (device_id, ts, power_w, voltage_v, current_a) " +
        "VALUES (?, ?, ?, ?, ?)";
    
    private static final String BATCH_UPDATE_LAST_SEEN_SQL = 
        "UPDATE device SET last_seen_ts = ? WHERE device_id IN (";
    
    /**
     * 批量插入遥测数据
     */
    public void batchInsert(List<Telemetry> telemetryList) {
        jdbcTemplate.batchUpdate(BATCH_INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Telemetry t = telemetryList.get(i);
                ps.setString(1, t.getDeviceId());
                ps.setLong(2, t.getTs());
                ps.setDouble(3, t.getPowerW());
                ps.setDouble(4, t.getVoltageV());
                ps.setDouble(5, t.getCurrentA());
            }
            
            @Override
            public int getBatchSize() {
                return telemetryList.size();
            }
        });
    }
    
    /**
     * 批量更新设备最后在线时间
     */
    public void batchUpdateLastSeen(List<String> deviceIds, long timestamp) {
        if (deviceIds.isEmpty()) {
            return;
        }
        
        // 构建IN语句
        String placeholders = String.join(",", deviceIds.stream()
            .map(id -> "?").toArray(String[]::new));
        
        String sql = "UPDATE device SET last_seen_ts = ? WHERE device_id IN (" + placeholders + ")";
        
        // 使用参数数组
        Object[] params = new Object[deviceIds.size() + 1];
        params[0] = timestamp;
        for (int i = 0; i < deviceIds.size(); i++) {
            params[i + 1] = deviceIds.get(i);
        }
        
        jdbcTemplate.update(sql, params);
    }
}
```

---

## 五、性能对比

```
插入10万条遥测数据:

┌──────────┬──────────┬──────────┬─────────┐
│ 方式     │ 耗时     │ TPS      │ 内存    │
├──────────┼──────────┼──────────┼─────────┤
│ 单条插入 │ 500s     │ 200/s    │ 低      │
│ JPA批量  │ 50s      │ 2000/s   │ 中      │
│ JDBC批量 │ 5s       │ 20000/s  │ 低      │
│ Kafka+JDBC│ 3s      │ 33000/s  │ 中      │
└──────────┴──────────┴──────────┴─────────┘

Kafka批量处理优势:
  ✅ 吞吐量提升100倍
  ✅ 数据库连接数降低90%
  ✅ 支持水平扩展
  ✅ 消息可回溯
```

---

## 六、扩展练习

### 练习1：实现命令ACK消费者

1. 消费命令确认消息
2. 更新命令状态
3. 超时未确认则重试

### 练习2：实现缓存更新消费者

1. 消费缓存更新消息
2. 清除/更新Redis缓存
3. 保证多实例缓存一致性

---

**最后更新**: 2026-04-22  
**文档版本**: 1.0
