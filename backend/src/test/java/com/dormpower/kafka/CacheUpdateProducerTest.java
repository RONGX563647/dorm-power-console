package com.dormpower.kafka;

import com.dormpower.model.CacheUpdateMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CacheUpdateProducer单元测试
 */
@DisplayName("缓存更新生产者测试")
class CacheUpdateProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private CacheUpdateProducer producer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        producer = new CacheUpdateProducer();
        
        ReflectionTestUtils.setField(producer, "kafkaTemplate", kafkaTemplate);
        ReflectionTestUtils.setField(producer, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(producer, "topic", "cache.update");
    }

    @Test
    @DisplayName("测试发送缓存更新消息")
    void testSendCacheUpdate() throws Exception {
        String cacheName = "testCache";
        String key = "testKey";
        String value = "testValue";
        String operation = "PUT";

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"cacheName\":\"testCache\"}");

        producer.sendCacheUpdate(cacheName, key, value, operation);

        verify(kafkaTemplate, times(1)).send(eq("cache.update"), eq(key), anyString());
    }

    @Test
    @DisplayName("测试发送缓存清除消息")
    void testSendCacheEvict() throws Exception {
        String cacheName = "testCache";
        String key = "testKey";

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"cacheName\":\"testCache\"}");

        producer.sendCacheEvict(cacheName, key);

        verify(kafkaTemplate, times(1)).send(eq("cache.update"), eq(key), anyString());
    }

    @Test
    @DisplayName("测试发送缓存清空消息")
    void testSendCacheClear() throws Exception {
        String cacheName = "testCache";

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"cacheName\":\"testCache\"}");

        producer.sendCacheClear(cacheName);

        verify(kafkaTemplate, times(1)).send(eq("cache.update"), eq("ALL"), anyString());
    }
}
