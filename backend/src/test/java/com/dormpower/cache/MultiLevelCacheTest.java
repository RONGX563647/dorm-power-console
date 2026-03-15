package com.dormpower.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.Cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MultiLevelCache单元测试
 */
@DisplayName("多级缓存测试")
class MultiLevelCacheTest {

    @Mock
    private Cache localCache;

    @Mock
    private Cache remoteCache;

    private MultiLevelCache multiLevelCache;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        multiLevelCache = new MultiLevelCache("testCache", localCache, remoteCache);
    }

    @Test
    @DisplayName("测试L1缓存命中")
    void testLocalCacheHit() {
        String key = "testKey";
        String value = "testValue";

        Cache.ValueWrapper wrapper = mock(Cache.ValueWrapper.class);
        when(wrapper.get()).thenReturn(value);
        when(localCache.get(key)).thenReturn(wrapper);

        Cache.ValueWrapper result = multiLevelCache.get(key);

        assertNotNull(result);
        assertEquals(value, result.get());
        verify(localCache, times(1)).get(key);
        verify(remoteCache, never()).get(key);
    }

    @Test
    @DisplayName("测试L2缓存命中并回填L1")
    void testRemoteCacheHitAndBackfill() {
        String key = "testKey";
        String value = "testValue";

        Cache.ValueWrapper wrapper = mock(Cache.ValueWrapper.class);
        when(wrapper.get()).thenReturn(value);
        when(localCache.get(key)).thenReturn(null);
        when(remoteCache.get(key)).thenReturn(wrapper);

        Cache.ValueWrapper result = multiLevelCache.get(key);

        assertNotNull(result);
        assertEquals(value, result.get());
        verify(localCache, times(1)).get(key);
        verify(remoteCache, times(1)).get(key);
        verify(localCache, times(1)).put(key, value);
    }

    @Test
    @DisplayName("测试缓存未命中")
    void testCacheMiss() {
        String key = "testKey";

        when(localCache.get(key)).thenReturn(null);
        when(remoteCache.get(key)).thenReturn(null);

        Cache.ValueWrapper result = multiLevelCache.get(key);

        assertNull(result);
        verify(localCache, times(1)).get(key);
        verify(remoteCache, times(1)).get(key);
    }

    @Test
    @DisplayName("测试缓存写入")
    void testCachePut() {
        String key = "testKey";
        String value = "testValue";

        multiLevelCache.put(key, value);

        verify(localCache, times(1)).put(key, value);
        verify(remoteCache, times(1)).put(key, value);
    }

    @Test
    @DisplayName("测试缓存清除")
    void testCacheEvict() {
        String key = "testKey";

        multiLevelCache.evict(key);

        verify(localCache, times(1)).evict(key);
        verify(remoteCache, times(1)).evict(key);
    }

    @Test
    @DisplayName("测试缓存清空")
    void testCacheClear() {
        multiLevelCache.clear();

        verify(localCache, times(1)).clear();
        verify(remoteCache, times(1)).clear();
    }

    @Test
    @DisplayName("测试获取缓存名称")
    void testGetName() {
        assertEquals("testCache", multiLevelCache.getName());
    }

    @Test
    @DisplayName("测试获取原生缓存")
    void testGetNativeCache() {
        assertEquals(multiLevelCache, multiLevelCache.getNativeCache());
    }
}
