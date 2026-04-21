package com.dormpower.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 分布式 JWT 令牌黑名单测试
 */
@SpringBootTest
@ActiveProfiles("test")
class TokenBlacklistTest {

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private String testToken;

    @BeforeEach
    void setUp() {
        // 创建一个测试 token
        testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
    }

    @Test
    void testAddToBlacklist() {
        // 添加 token 到黑名单
        tokenBlacklist.addToBlacklist(testToken);
        
        // 验证 token 在黑名单中
        assertTrue(tokenBlacklist.isBlacklisted(testToken), "Token 应该在黑名单中");
    }

    @Test
    void testIsBlacklisted_NotInBlacklist() {
        // 验证未加入黑名单的 token
        assertFalse(tokenBlacklist.isBlacklisted(testToken), "Token 不应该在黑名单中");
    }

    @Test
    void testRemoveFromBlacklist() {
        // 添加并移除
        tokenBlacklist.addToBlacklist(testToken);
        assertTrue(tokenBlacklist.isBlacklisted(testToken));
        
        tokenBlacklist.removeFromBlacklist(testToken);
        assertFalse(tokenBlacklist.isBlacklisted(testToken), "Token 应该已从黑名单移除");
    }

    @Test
    void testMultipleTokens() {
        // 测试多个 token
        String token1 = "token1";
        String token2 = "token2";
        String token3 = "token3";
        
        tokenBlacklist.addToBlacklist(token1);
        tokenBlacklist.addToBlacklist(token2);
        
        assertTrue(tokenBlacklist.isBlacklisted(token1));
        assertTrue(tokenBlacklist.isBlacklisted(token2));
        assertFalse(tokenBlacklist.isBlacklisted(token3));
    }

    @Test
    void testRedisKeyFormat() {
        // 验证 Redis key 格式 (如果 Redis 可用)
        if (redisTemplate != null) {
            tokenBlacklist.addToBlacklist(testToken);
            
            String expectedHash = String.valueOf(testToken.hashCode());
            String expectedKey = "token:blacklist:" + expectedHash;
            
            Boolean exists = redisTemplate.hasKey(expectedKey);
            assertTrue(exists != null && exists, "Redis 中应该存在黑名单 key");
        }
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        // 测试并发访问
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                String token = "concurrent_token_" + index;
                tokenBlacklist.addToBlacklist(token);
                assertTrue(tokenBlacklist.isBlacklisted(token));
            });
            threads[i].start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证所有 token 都在黑名单中
        for (int i = 0; i < threadCount; i++) {
            assertTrue(tokenBlacklist.isBlacklisted("concurrent_token_" + i));
        }
    }
}
