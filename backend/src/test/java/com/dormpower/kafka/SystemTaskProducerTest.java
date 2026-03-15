package com.dormpower.kafka;

import com.dormpower.model.SystemTaskMessage;
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
 * SystemTaskProducer 单元测试
 */
@DisplayName("系统任务生产者测试")
class SystemTaskProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private SystemTaskProducer producer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        producer = new SystemTaskProducer();

        ReflectionTestUtils.setField(producer, "kafkaTemplate", kafkaTemplate);
        ReflectionTestUtils.setField(producer, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(producer, "systemTaskKafkaEnabled", true);
    }

    @Test
    @DisplayName("测试发送系统任务消息")
    void testSendSystemTask() throws Exception {
        SystemTaskMessage message = new SystemTaskMessage();
        message.setTaskType("BACKUP");
        message.setDescription("自动备份");

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"taskType\":\"BACKUP\"}");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));

        producer.sendSystemTask(message);

        verify(kafkaTemplate, times(1)).send(eq("dorm.system.task"), eq("BACKUP"), anyString());
    }

    @Test
    @DisplayName("测试发送备份任务")
    void testSendBackupTask() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"taskType\":\"BACKUP\"}");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));

        producer.sendBackupTask("自动备份", 30);

        verify(kafkaTemplate, times(1)).send(eq("dorm.system.task"), eq("BACKUP"), anyString());
    }

    @Test
    @DisplayName("测试发送清理任务")
    void testSendCleanupTask() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"taskType\":\"CLEANUP_LOGS\"}");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));

        producer.sendCleanupTask("LOGS", 30);

        verify(kafkaTemplate, times(1)).send(eq("dorm.system.task"), eq("CLEANUP_LOGS"), anyString());
    }

    @Test
    @DisplayName("测试发送指标收集任务")
    void testSendMetricsCollectionTask() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"taskType\":\"METRICS\"}");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));

        producer.sendMetricsCollectionTask();

        verify(kafkaTemplate, times(1)).send(eq("dorm.system.task"), eq("METRICS"), anyString());
    }

    @Test
    @DisplayName("Kafka 禁用时不发送消息")
    void testSendSystemTask_KafkaDisabled() {
        ReflectionTestUtils.setField(producer, "systemTaskKafkaEnabled", false);

        producer.sendSystemTask(new SystemTaskMessage());

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }
}