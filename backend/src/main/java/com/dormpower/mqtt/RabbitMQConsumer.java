package com.dormpower.mqtt;

import com.dormpower.dto.AlertMessage;
import com.dormpower.dto.BillMessage;
import com.dormpower.dto.TelemetryMessage;
import com.dormpower.service.AlertService;
import com.dormpower.service.BillService;
import com.dormpower.service.TelemetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 消息消费者
 * 
 * 三大核心异步链路消费者:
 * 1. 遥测数据异步落库
 * 2. 告警消息异步推送
 * 3. 账单异步生成
 * 
 * @author dormpower team
 * @version 1.0
 */
@Component
public class RabbitMQConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConsumer.class);

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private BillService billService;

    /**
     * 消费遥测数据消息
     * 
     * 处理流程:
     * 1. 接收遥测数据消息
     * 2. 批量写入数据库 (100 条/批)
     * 3. 检查是否触发告警
     * 4. 更新设备实时功率
     * 
     * @param message 遥测数据消息
     */
    @RabbitListener(queues = "${rabbitmq.queue.telemetry:telemetry.data}")
    @RabbitHandler
    public void consumeTelemetry(@Payload TelemetryMessage message) {
        try {
            logger.debug("接收遥测数据：deviceId={}, timestamp={}, power={}W", 
                message.getDeviceId(), message.getTimestamp(), message.getPowerW());
            
            // 保存遥测数据到数据库
            telemetryService.saveTelemetry(message);
            
            // 检查是否触发告警
            alertService.checkAndGenerateAlert(
                message.getDeviceId(),
                message.getPowerW(),
                message.getVoltageV(),
                message.getCurrentA()
            );
            
            logger.info("遥测数据处理完成：{}", message.getDeviceId());
            
        } catch (Exception e) {
            logger.error("处理遥测数据失败：deviceId={}", message.getDeviceId(), e);
            // 可以重新入队或记录到死信队列
        }
    }

    /**
     * 消费告警消息
     * 
     * 处理流程:
     * 1. 接收告警消息
     * 2. 保存告警记录到数据库
     * 3. 通过 WebSocket 推送给前端
     * 4. 发送短信/邮件通知 (可选)
     * 
     * @param message 告警消息
     */
    @RabbitListener(queues = "${rabbitmq.queue.alert:alert.message}")
    @RabbitHandler
    public void consumeAlert(@Payload AlertMessage message) {
        try {
            logger.info("接收告警消息：alertId={}, type={}, severity={}", 
                message.getAlertId(), message.getAlertType(), message.getSeverity());
            
            // 保存告警记录
            alertService.saveAlert(message);
            
            // 通过 WebSocket 推送告警
            alertService.pushAlertToWebSocket(message);
            
            // 发送短信/邮件通知 (严重告警)
            if ("CRITICAL".equals(message.getSeverity())) {
                alertService.sendSmsNotification(message);
                alertService.sendEmailNotification(message);
            }
            
            logger.info("告警处理完成：{}", message.getAlertId());
            
        } catch (Exception e) {
            logger.error("处理告警消息失败：alertId={}", message.getAlertId(), e);
        }
    }

    /**
     * 消费账单生成消息
     * 
     * 处理流程:
     * 1. 接收账单生成消息
     * 2. 查询用电数据
     * 3. 计算电费
     * 4. 生成账单记录
     * 5. 发送账单通知
     * 
     * @param message 账单消息
     */
    @RabbitListener(queues = "${rabbitmq.queue.bill:bill.generate}")
    @RabbitHandler
    public void consumeBill(@Payload BillMessage message) {
        try {
            logger.info("接收账单生成消息：roomId={}, date={}", 
                message.getRoomId(), message.getBillDate());
            
            // 生成账单
            billService.generateBill(message);
            
            // 发送账单通知
            billService.sendBillNotification(message);
            
            logger.info("账单生成完成：roomId={}, amount={}元", 
                message.getRoomId(), message.getTotalAmount());
            
        } catch (Exception e) {
            logger.error("生成账单失败：roomId={}", message.getRoomId(), e);
        }
    }

    /**
     * 批量消费遥测数据 (可选优化)
     * 
     * 使用场景：设备批量上报数据时
     * 
     * @param messages 遥测数据列表
     */
    // @RabbitListener(queues = "${rabbitmq.queue.telemetry:telemetry.data}")
    // public void consumeTelemetryBatch(@Payload List<TelemetryMessage> messages) {
    //     logger.info("批量接收遥测数据：{} 条", messages.size());
    //     
    //     // 批量写入数据库
    //     telemetryService.batchSaveTelemetry(messages);
    //     
    //     logger.info("批量处理完成：{} 条", messages.size());
    // }
}
