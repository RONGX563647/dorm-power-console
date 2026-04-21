package com.dormpower.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * 
 * 配置三大核心异步链路:
 * 1. 设备遥测数据异步落库
 * 2. 告警消息异步推送
 * 3. 用电账单异步生成
 * 
 * @author dormpower team
 * @version 1.0
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 队列名称 ====================
    
    /**
     * 遥测数据队列
     */
    @Value("${rabbitmq.queue.telemetry:telemetry.data}")
    private String telemetryQueueName;
    
    /**
     * 告警消息队列
     */
    @Value("${rabbitmq.queue.alert:alert.message}")
    private String alertQueueName;
    
    /**
     * 账单生成队列
     */
    @Value("${rabbitmq.queue.bill:bill.generate}")
    private String billQueueName;
    
    // ==================== 交换机名称 ====================
    
    /**
     * 直接交换机
     */
    @Value("${rabbitmq.exchange.direct:dormpower.direct}")
    private String directExchange;
    
    // ==================== 路由键 ====================
    
    /**
     * 遥测数据路由键
     */
    @Value("${rabbitmq.routing.telemetry:telemetry}")
    private String telemetryRoutingKey;
    
    /**
     * 告警消息路由键
     */
    @Value("${rabbitmq.routing.alert:alert}")
    private String alertRoutingKey;
    
    /**
     * 账单生成路由键
     */
    @Value("${rabbitmq.routing.bill:bill}")
    private String billRoutingKey;

    // ==================== 交换机配置 ====================

    /**
     * 直接交换机 (Direct Exchange)
     * 
     * 用于点对点消息传递:
     * - 遥测数据 → 落库服务
     * - 告警消息 → 推送服务
     * - 账单生成 → 计算服务
     */
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(directExchange);
    }

    // ==================== 队列配置 ====================

    /**
     * 遥测数据队列
     * 
     * 特性:
     * - 持久化队列
     * - 不自动删除
     * - 支持消息确认
     */
    @Bean
    public Queue telemetryQueue() {
        return QueueBuilder.durable(telemetryQueueName)
            .withArgument("x-message-ttl", 60000)  // 消息 TTL: 60 秒
            .build();
    }

    /**
     * 告警消息队列
     * 
     * 特性:
     * - 高优先级队列
     * - 持久化
     * - 支持死信队列
     */
    @Bean
    public Queue alertQueue() {
        return QueueBuilder.durable(alertQueueName)
            .withArgument("x-max-priority", 10)  // 支持优先级
            .withArgument("x-message-ttl", 30000)  // 消息 TTL: 30 秒
            .build();
    }

    /**
     * 账单生成队列
     * 
     * 特性:
     * - 持久化队列
     * - 批量处理
     * - 延迟处理
     */
    @Bean
    public Queue billQueue() {
        return QueueBuilder.durable(billQueueName)
            .withArgument("x-message-ttl", 300000)  // 消息 TTL: 5 分钟
            .build();
    }

    // ==================== 绑定配置 ====================

    /**
     * 绑定遥测数据队列到直接交换机
     */
    @Bean
    public Binding telemetryBinding(Queue telemetryQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(telemetryQueue)
            .to(directExchange)
            .with(telemetryRoutingKey);
    }

    /**
     * 绑定告警消息队列到直接交换机
     */
    @Bean
    public Binding alertBinding(Queue alertQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(alertQueue)
            .to(directExchange)
            .with(alertRoutingKey);
    }

    /**
     * 绑定账单生成队列到直接交换机
     */
    @Bean
    public Binding billBinding(Queue billQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(billQueue)
            .to(directExchange)
            .with(billRoutingKey);
    }

    // ==================== 消息转换器 ====================

    /**
     * JSON 消息转换器
     * 
     * 将 Java 对象自动序列化为 JSON
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ==================== RabbitTemplate ====================

    /**
     * RabbitTemplate - 发送和接收消息的核心类
     * 
     * @param connectionFactory 连接工厂
     * @return 配置好的 RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        
        // 启用确认模式 (publisher confirms)
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // 消息发送失败，记录日志或重试
                System.err.println("消息发送失败：" + cause);
            }
        });
        
        // 启用返回模式 (mandatory)
        rabbitTemplate.setReturnsCallback(returned -> {
            System.err.println("消息未被路由：" + returned);
        });
        
        return rabbitTemplate;
    }
}
