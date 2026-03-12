package com.dormpower.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 配置类
 *
 * 配置内容：
 * 1. 生产者配置（批量发送、压缩）
 * 2. 消费者配置（批量消费、手动提交）
 * 3. 监听器容器工厂
 *
 * 仅在 kafka.enabled=true 时启用
 *
 * @author dormpower team
 * @version 1.0
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:dorm-power-consumer}")
    private String groupId;

    // ========== 生产者配置 ==========

    /**
     * 生产者工厂
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 批量发送大小
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        // 缓冲区大小
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        // 确认模式：所有副本确认
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        // 重试次数
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        // 压缩类型
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        // 消息最大大小
        config.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1048576);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Kafka 模板
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ========== 消费者配置 ==========

    /**
     * 消费者工厂
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // 从最早的消息开始消费
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // 禁用自动提交
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // 单次拉取最大记录数
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        // 拉取间隔
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        // 会话超时
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        // 心跳间隔
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * 批量消费者监听器容器工厂
     *
     * 支持批量消费和手动提交偏移量
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> batchKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // 启用批量消费
        factory.setBatchListener(true);
        // 手动提交偏移量
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        // 并发数
        factory.setConcurrency(3);
        // 自动启动
        factory.setAutoStartup(true);

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
        factory.setAutoStartup(true);

        return factory;
    }
}