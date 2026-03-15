package com.dormpower.kafka;

import com.dormpower.model.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * NotificationProducer 单元测试
 */
@DisplayName("通知消息生产者测试")
class NotificationProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private NotificationProducer producer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        producer = new NotificationProducer();

        ReflectionTestUtils.setField(producer, "kafkaTemplate", kafkaTemplate);
        ReflectionTestUtils.setField(producer, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(producer, "notificationKafkaEnabled", true);
    }

    @Test
    @DisplayName("测试发送通知消息")
    void testSendNotification() throws Exception {
        NotificationMessage message = new NotificationMessage();
        message.setTitle("测试通知");
        message.setContent("测试内容");
        message.setType("SYSTEM");
        message.setUsername("testUser");

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"title\":\"测试通知\"}");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));

        producer.sendNotification(message);

        verify(kafkaTemplate, times(1)).send(eq("dorm.notification"), anyString(), anyString());
    }

    @Test
    @DisplayName("测试发送告警通知")
    void testSendAlertNotification() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"ALERT\"}");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));

        producer.sendAlertNotification("告警标题", "告警内容", "user1", "device1");

        verify(kafkaTemplate, times(1)).send(eq("dorm.notification"), eq("user1"), anyString());
    }

    @Test
    @DisplayName("测试发送系统通知")
    void testSendSystemNotification() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"SYSTEM\"}");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));

        producer.sendSystemNotification("系统通知", "通知内容", "HIGH");

        verify(kafkaTemplate, times(1)).send(eq("dorm.notification"), eq("system"), anyString());
    }

    @Test
    @DisplayName("测试发送邮件通知")
    void testSendEmailNotification() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"EMAIL\"}");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));

        producer.sendEmailNotification("邮件标题", "邮件内容", "user1");

        verify(kafkaTemplate, times(1)).send(eq("dorm.notification"), eq("user1"), anyString());
    }

    @Test
    @DisplayName("Kafka 禁用时不发送消息")
    void testSendNotification_KafkaDisabled() {
        ReflectionTestUtils.setField(producer, "notificationKafkaEnabled", false);

        producer.sendNotification(new NotificationMessage());

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }
}