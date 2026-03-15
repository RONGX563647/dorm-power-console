package com.dormpower.kafka;

import com.dormpower.model.NotificationMessage;
import com.dormpower.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * NotificationConsumer 单元测试
 */
@DisplayName("通知消息消费者测试")
class NotificationConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private Acknowledgment acknowledgment;

    private NotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumer = new NotificationConsumer();

        ReflectionTestUtils.setField(consumer, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(consumer, "notificationRepository", notificationRepository);
        // 使用 SimpleMeterRegistry 而不是 mock
        ReflectionTestUtils.setField(consumer, "meterRegistry", new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("空消息列表直接返回")
    void testConsumeNotificationBatch_EmptyList() {
        List<ConsumerRecord<String, String>> records = new ArrayList<>();

        consumer.consumeNotificationBatch(records, acknowledgment);

        verify(notificationRepository, never()).saveAll(anyList());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("正常批量消费通知消息")
    void testConsumeNotificationBatch_Success() throws Exception {
        List<ConsumerRecord<String, String>> records = new ArrayList<>();
        String messageJson = "{\"title\":\"测试\",\"content\":\"内容\",\"type\":\"SYSTEM\",\"priority\":\"NORMAL\"}";
        records.add(new ConsumerRecord<>("dorm.notification", 0, 0, "user1", messageJson));

        NotificationMessage mockMessage = new NotificationMessage();
        mockMessage.setTitle("测试");
        mockMessage.setContent("内容");
        mockMessage.setType("SYSTEM");

        when(objectMapper.readValue(anyString(), eq(NotificationMessage.class))).thenReturn(mockMessage);
        when(notificationRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

        consumer.consumeNotificationBatch(records, acknowledgment);

        verify(notificationRepository, times(1)).saveAll(anyList());
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("消息解析失败不影响其他消息")
    void testConsumeNotificationBatch_ParseError() throws Exception {
        List<ConsumerRecord<String, String>> records = new ArrayList<>();
        records.add(new ConsumerRecord<>("dorm.notification", 0, 0, "user1", "invalid json"));
        records.add(new ConsumerRecord<>("dorm.notification", 0, 1, "user2", "{\"title\":\"test\"}"));

        when(objectMapper.readValue(eq("invalid json"), eq(NotificationMessage.class)))
            .thenThrow(new RuntimeException("Parse error"));
        when(objectMapper.readValue(eq("{\"title\":\"test\"}"), eq(NotificationMessage.class)))
            .thenReturn(new NotificationMessage());

        consumer.consumeNotificationBatch(records, acknowledgment);

        verify(notificationRepository, times(1)).saveAll(anyList());
        verify(acknowledgment, times(1)).acknowledge();
    }
}