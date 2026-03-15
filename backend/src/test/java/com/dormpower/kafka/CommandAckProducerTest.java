package com.dormpower.kafka;

import com.dormpower.model.CommandAckMessage;
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
 * CommandAckProducer 单元测试
 */
@DisplayName("命令确认生产者测试")
class CommandAckProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private CommandAckProducer producer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        producer = new CommandAckProducer();

        ReflectionTestUtils.setField(producer, "kafkaTemplate", kafkaTemplate);
        ReflectionTestUtils.setField(producer, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(producer, "commandKafkaEnabled", true);
    }

    @Test
    @DisplayName("测试发送命令确认消息")
    void testSendCommandAck() throws Exception {
        CommandAckMessage message = new CommandAckMessage();
        message.setDeviceId("device_001");
        message.setCmdId("cmd_001");
        message.setStatus("success");

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"cmdId\":\"cmd_001\"}");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));

        producer.sendCommandAck(message);

        verify(kafkaTemplate, times(1)).send(eq("dorm.command.ack"), eq("device_001"), anyString());
    }

    @Test
    @DisplayName("测试简化版命令确认发送")
    void testSendCommandAck_Simple() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"cmdId\":\"cmd_001\"}");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));

        producer.sendCommandAck("device_001", "cmd_001", "success", "执行成功");

        verify(kafkaTemplate, times(1)).send(eq("dorm.command.ack"), eq("device_001"), anyString());
    }

    @Test
    @DisplayName("Kafka 禁用时不发送消息")
    void testSendCommandAck_KafkaDisabled() {
        ReflectionTestUtils.setField(producer, "commandKafkaEnabled", false);

        producer.sendCommandAck(new CommandAckMessage());

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }
}