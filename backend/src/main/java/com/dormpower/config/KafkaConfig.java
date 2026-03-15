package com.dormpower.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 配置类
 *
 * 配置内容：
 * 1. 生产者配置（批量发送、压缩、幂等性）
 * 2. 消费者配置（批量消费、手动提交）
 * 3. 监听器容器工厂
 * 4. 错误处理器（重试 + 死信队列）
 * 5. Topic 自动创建（包括 DLT）
 *
 * v2.1 优化：
 * - 生产者幂等性配置，保证消息不重复
 * - 消费者错误处理，支持重试和死信队列
 * - 优化批量发送参数
 * - 自动创建 Topic 和死信队列
 *
 * 仅在 kafka.enabled=true 时启用
 *
 * @author dormpower team
 * @version 2.1
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class KafkaConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:dorm-power-consumer}")
    private String groupId;

    // ========== 生产者配置 ==========

    /**
     * 生产者工厂
     *
     * 优化配置：
     * - 启用幂等性，保证消息不重复
     * - 批量发送 + linger.ms 优化吞吐量
     * - LZ4 压缩减少网络传输
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // 批量发送配置
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 5);  // 等待 5ms 凑批

        // 可靠性配置
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);

        // 幂等性配置（重要：保证消息不重复）
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // 压缩配置
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");

        // 消息大小
        config.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1048576);

        logger.info("Kafka producer factory initialized with idempotence enabled");

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

        // 消费策略
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // 消费能力
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        logger.info("Kafka consumer factory initialized");

        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * 批量消费者监听器容器工厂
     *
     * 支持批量消费、手动提交偏移量、错误处理
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> batchKafkaListenerContainerFactory(
            KafkaTemplate<String, String> kafkaTemplate) {
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

        // 错误处理器：重试 3 次后发送到死信队列
        factory.setCommonErrorHandler(errorHandler(kafkaTemplate));

        logger.info("Batch Kafka listener container factory initialized with error handler");

        return factory;
    }

    /**
     * 单条消费者监听器容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> singleKafkaListenerContainerFactory(
            KafkaTemplate<String, String> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(3);
        factory.setAutoStartup(true);

        // 错误处理器
        factory.setCommonErrorHandler(errorHandler(kafkaTemplate));

        logger.info("Single Kafka listener container factory initialized with error handler");

        return factory;
    }

    /**
     * 错误处理器
     *
     * 处理策略：
     * 1. 重试 3 次，每次间隔 1 秒
     * 2. 重试失败后发送到死信队列（原 topic + .DLT）
     *
     * @param kafkaTemplate Kafka 模板
     * @return 错误处理器
     */
    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        // 死信队列发布器：失败消息发送到 topic.DLT
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
            (consumerRecord, exception) -> {
                String originalTopic = consumerRecord.topic();
                String dltTopic = originalTopic + ".DLT";
                logger.warn("Message failed after retries, sending to DLT: {} -> {}",
                    originalTopic, dltTopic);
                return new org.apache.kafka.common.TopicPartition(dltTopic, consumerRecord.partition());
            });

        // 重试策略：重试 3 次，每次间隔 1 秒
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));

        // 记录重试日志
        handler.setRetryListeners((record, ex, deliveryAttempt) -> {
            logger.warn("Retry attempt {} for topic {}, partition {}, offset {}: {}",
                deliveryAttempt, record.topic(), record.partition(), record.offset(), ex.getMessage());
        });

        logger.info("Kafka error handler initialized with DLT support");

        return handler;
    }
}