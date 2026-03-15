package com.dormpower.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiLevelCacheNodeConsistencyTest {

    @Mock
    private Cache localCache;

    @Mock
    private Cache remoteCache;

    private MultiLevelCache multiLevelCache;

    @BeforeEach
    void setUp() {
        multiLevelCache = new MultiLevelCache("testCache", localCache, remoteCache);
    }

    @Test
    void testEvictLocal_ShouldOnlyEvictLocalCache() {
        multiLevelCache.evictLocal("testKey");

        verify(localCache).evict("testKey");
        verify(remoteCache, never()).evict(any());
    }

    @Test
    void testClearLocal_ShouldOnlyClearLocalCache() {
        multiLevelCache.clearLocal();

        verify(localCache).clear();
        verify(remoteCache, never()).clear();
    }

    @Test
    void testEvict_ShouldEvictBothCaches() {
        multiLevelCache.evict("testKey");

        verify(localCache).evict("testKey");
        verify(remoteCache).evict("testKey");
    }

    @Test
    void testClear_ShouldClearBothCaches() {
        multiLevelCache.clear();

        verify(localCache).clear();
        verify(remoteCache).clear();
    }

    @Test
    void testGetLocalCache_ReturnsLocalCache() {
        Cache result = multiLevelCache.getLocalCache();

        assertEquals(localCache, result);
    }

    @Test
    void testGetRemoteCache_ReturnsRemoteCache() {
        Cache result = multiLevelCache.getRemoteCache();

        assertEquals(remoteCache, result);
    }
}
