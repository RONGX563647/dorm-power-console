package com.dormpower.kafka;

import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.StripStatusRepository;
import com.dormpower.repository.TelemetryBulkRepository;
import com.dormpower.service.AlertService;
import com.dormpower.service.WebSocketNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * TelemetryConsumer 单元测试
 */
@DisplayName("遥测数据消费者测试")
class TelemetryConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TelemetryBulkRepository telemetryBulkRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private StripStatusRepository stripStatusRepository;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @Mock
    private AlertService alertService;

    @Mock
    private Acknowledgment acknowledgment;

    private TelemetryConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumer = new TelemetryConsumer();

        ReflectionTestUtils.setField(consumer, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(consumer, "telemetryBulkRepository", telemetryBulkRepository);
        ReflectionTestUtils.setField(consumer, "deviceRepository", deviceRepository);
        ReflectionTestUtils.setField(consumer, "stripStatusRepository", stripStatusRepository);
        ReflectionTestUtils.setField(consumer, "webSocketNotificationService", webSocketNotificationService);
        ReflectionTestUtils.setField(consumer, "alertService", alertService);
        // 使用 SimpleMeterRegistry 而不是 mock
        ReflectionTestUtils.setField(consumer, "meterRegistry", new SimpleMeterRegistry());

        // Mock ObjectMapper 行为
        try {
            when(objectMapper.valueToTree(any())).thenReturn(new ObjectMapper().createObjectNode());
        } catch (Exception e) {
            // ignore
        }
    }

    @Nested
    @DisplayName("遥测数据消费测试")
    class TelemetryBatchTests {

        @Test
        @DisplayName("空消息列表直接返回")
        void testConsumeTelemetryBatch_EmptyList() {
            List<ConsumerRecord<String, String>> records = new ArrayList<>();

            consumer.consumeTelemetryBatch(records, acknowledgment);

            verify(telemetryBulkRepository, never()).batchInsert(anyList());
            verify(acknowledgment, never()).acknowledge();
        }

        @Test
        @DisplayName("正常批量消费遥测数据")
        void testConsumeTelemetryBatch_Success() throws Exception {
            List<ConsumerRecord<String, String>> records = new ArrayList<>();
            String messageJson = "{\"deviceId\":\"device_001\",\"ts\":1640000000,\"powerW\":150.5,\"voltageV\":220.0,\"currentA\":0.68}";
            records.add(new ConsumerRecord<>("dorm.telemetry", 0, 0, "device_001", messageJson));

            when(objectMapper.readTree(anyString())).thenReturn(
                new ObjectMapper().readTree(messageJson)
            );

            consumer.consumeTelemetryBatch(records, acknowledgment);

            verify(telemetryBulkRepository, times(1)).batchInsert(anyList());
            verify(acknowledgment, times(1)).acknowledge();
        }
    }

    @Nested
    @DisplayName("设备状态消费测试")
    class DeviceStatusBatchTests {

        @Test
        @DisplayName("空消息列表直接返回")
        void testConsumeStatusBatch_EmptyList() {
            List<ConsumerRecord<String, String>> records = new ArrayList<>();

            consumer.consumeStatusBatch(records, acknowledgment);

            verify(deviceRepository, never()).saveAll(any());
            verify(stripStatusRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("告警数据消费测试")
    class AlertBatchTests {

        @Test
        @DisplayName("空消息列表直接返回")
        void testConsumeAlertBatch_EmptyList() {
            List<ConsumerRecord<String, String>> records = new ArrayList<>();

            consumer.consumeAlertBatch(records, acknowledgment);

            verify(alertService, never()).checkAndGenerateAlert(anyString(), anyString(), anyDouble());
        }
    }
}