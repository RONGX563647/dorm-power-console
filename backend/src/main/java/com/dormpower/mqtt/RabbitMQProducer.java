package com.dormpower.mqtt;

import com.dormpower.config.RabbitMQConfig;
import com.dormpower.dto.AlertMessage;
import com.dormpower.dto.BillMessage;
import com.dormpower.dto.TelemetryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ 消息生产者
 * 
 * 三大核心异步链路:
 * 1. 设备遥测数据异步落库
 * 2. 告警消息异步推送
 * 3. 用电账单异步生成
 * 
 * @author dormpower team
 * @version 1.0
 */
@Service
public class RabbitMQProducer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.direct:dormpower.direct}")
    private String directExchange;

    @Value("${rabbitmq.routing.telemetry:telemetry}")
    private String telemetryRoutingKey;

    @Value("${rabbitmq.routing.alert:alert}")
    private String alertRoutingKey;

    @Value("${rabbitmq.routing.bill:bill}")
    private String billRoutingKey;

    /**
     * 发送遥测数据到消息队列
     * 
     * 使用场景:
     * - MQTT 接收设备遥测数据后，异步写入数据库
     * - 削峰填谷，避免数据库 IO 瓶颈
     * 
     * @param telemetryMessage 遥测数据消息
     */
    public void sendTelemetry(TelemetryMessage telemetryMessage) {
        try {
            logger.debug("发送遥测数据：deviceId={}, power={}W", 
                telemetryMessage.getDeviceId(), telemetryMessage.getPowerW());
            
            rabbitTemplate.convertAndSend(directExchange, telemetryRoutingKey, telemetryMessage);
            
            logger.info("遥测数据发送成功：{}", telemetryMessage.getDeviceId());
            
        } catch (Exception e) {
            logger.error("发送遥测数据失败：{}", telemetryMessage.getDeviceId(), e);
            // TODO: 可以添加重试机制或降级处理
        }
    }

    /**
     * 发送告警消息到消息队列
     * 
     * 使用场景:
     * - 检测到设备异常 (功率过载、电压异常等)
     * - 异步推送告警通知给前端和用户
     * 
     * @param alertMessage 告警消息
     */
    public void sendAlert(AlertMessage alertMessage) {
        try {
            logger.info("发送告警消息：type={}, severity={}, deviceId={}", 
                alertMessage.getAlertType(), alertMessage.getSeverity(), alertMessage.getDeviceId());
            
            // 设置消息优先级
            int priority = getPriority(alertMessage.getSeverity());
            
            rabbitTemplate.convertAndSend(directExchange, alertRoutingKey, 
                alertMessage, message -> {
                    message.getMessageProperties().setPriority(priority);
                    return message;
                });
            
            logger.info("告警消息发送成功：{}", alertMessage.getAlertId());
            
        } catch (Exception e) {
            logger.error("发送告警消息失败：{}", alertMessage.getAlertId(), e);
        }
    }

    /**
     * 发送账单生成消息到消息队列
     * 
     * 使用场景:
     * - 定时任务触发每日/每月账单生成
     * - 异步计算用电量和电费
     * 
     * @param billMessage 账单消息
     */
    public void sendBill(BillMessage billMessage) {
        try {
            logger.info("发送账单生成消息：roomId={}, date={}, amount={}元", 
                billMessage.getRoomId(), billMessage.getBillDate(), billMessage.getTotalAmount());
            
            rabbitTemplate.convertAndSend(directExchange, billRoutingKey, billMessage);
            
            logger.info("账单生成消息发送成功：{}", billMessage.getRoomId());
            
        } catch (Exception e) {
            logger.error("发送账单生成消息失败：{}", billMessage.getRoomId(), e);
        }
    }

    /**
     * 根据告警级别获取优先级
     * 
     * @param severity 告警级别
     * @return 优先级 (0-10, 10 最高)
     */
    private int getPriority(String severity) {
        if (severity == null) {
            return 5;  // 默认优先级
        }
        
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> 10;  // 严重告警，最高优先级
            case "ERROR" -> 8;
            case "WARNING" -> 6;
            case "INFO" -> 4;
            default -> 5;
        };
    }

    /**
     * 批量发送遥测数据
     * 
     * @param telemetryMessages 遥测数据列表
     */
    public void sendTelemetryBatch(java.util.List<TelemetryMessage> telemetryMessages) {
        logger.info("批量发送遥测数据：{} 条", telemetryMessages.size());
        
        for (TelemetryMessage message : telemetryMessages) {
            sendTelemetry(message);
        }
        
        logger.info("批量发送完成：{} 条", telemetryMessages.size());
    }
}
