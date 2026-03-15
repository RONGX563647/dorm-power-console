package com.dormpower.cache.bloom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisBloomFilterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisBloomFilter bloomFilter;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testPut_ShouldSetBits() {
        when(valueOperations.setBit(anyString(), anyLong(), anyBoolean())).thenReturn(false);

        bloomFilter.put("testFilter", "testKey");

        verify(valueOperations, atLeastOnce()).setBit(anyString(), anyLong(), eq(true));
    }

    @Test
    void testMightContain_WhenAllBitsSet_ReturnsTrue() {
        when(valueOperations.getBit(anyString(), anyLong())).thenReturn(true);

        boolean result = bloomFilter.mightContain("testFilter", "testKey");

        assertTrue(result);
    }

    @Test
    void testMightContain_WhenAnyBitNotSet_ReturnsFalse() {
        when(valueOperations.getBit(anyString(), anyLong())).thenReturn(false);

        boolean result = bloomFilter.mightContain("testFilter", "testKey");

        assertFalse(result);
    }

    @Test
    void testClear_ShouldDeleteKey() {
        bloomFilter.clear("testFilter");

        verify(stringRedisTemplate).delete("bloom:testFilter");
    }

    @Test
    void testPutAll_ShouldSetBitsForAllKeys() {
        when(valueOperations.setBit(anyString(), anyLong(), anyBoolean())).thenReturn(false);

        bloomFilter.putAll("testFilter", java.util.Arrays.asList("key1", "key2", "key3"));

        verify(valueOperations, atLeast(3)).setBit(anyString(), anyLong(), eq(true));
    }
}
