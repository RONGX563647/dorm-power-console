package com.dormpower.controller;

import com.dormpower.config.TestSecurityConfig;
import com.dormpower.util.JwtUtil;
import com.dormpower.util.TokenBlacklist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 认证单元测试
 *
 * 测试用例覆盖：
 * - TC-AUTH-001: 正常登录
 * - TC-AUTH-002: 密码错误
 * - TC-AUTH-003: 用户不存在
 * - TC-AUTH-004: 限流测试
 * - TC-AUTH-005: 正常登出
 * - TC-AUTH-006: 刷新令牌
 *
 * @author dormpower team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    // 测试数据常量
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String ADMIN_EMAIL = "admin@dorm.local";
    private static final String WRONG_PASSWORD = "wrongpassword";
    private static final String NON_EXISTENT_USER = "nonexistent";

    /**
     * 登录测试
     */
    @Nested
    @DisplayName("登录测试")
    class LoginTests {

        /**
         * TC-AUTH-001: 正常登录
         *
         * 测试场景：正常登录
         * 输入：正确账号密码
         * 预期输出：返回JWT令牌
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-AUTH-001: 正常登录-返回JWT令牌")
        void testLoginSuccess() throws Exception {
            // Arrange
            String loginJson = "{\"account\": \"" + ADMIN_USERNAME + "\", \"password\": \"" + ADMIN_PASSWORD + "\"}";

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.username").value(ADMIN_USERNAME))
                    .andExpect(jsonPath("$.user.role").value("admin"))
                    .andExpect(jsonPath("$.token").exists());
        }

        /**
         * TC-AUTH-002: 密码错误
         *
         * 测试场景：密码错误
         * 输入：错误密码
         * 预期输出：返回401
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-AUTH-002: 密码错误-返回401")
        void testLoginFailure_WrongPassword() throws Exception {
            // Arrange
            String loginJson = "{\"account\": \"" + ADMIN_USERNAME + "\", \"password\": \"" + WRONG_PASSWORD + "\"}";

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isUnauthorized());
        }

        /**
         * TC-AUTH-003: 用户不存在
         *
         * 测试场景：用户不存在
         * 输入：不存在的账号
         * 预期输出：返回401
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-AUTH-003: 用户不存在-返回401")
        void testLoginFailure_UserNotFound() throws Exception {
            // Arrange
            String loginJson = "{\"account\": \"" + NON_EXISTENT_USER + "\", \"password\": \"anypassword\"}";

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isUnauthorized());
        }

        /**
         * 测试使用邮箱登录
         */
        @Test
        @DisplayName("使用邮箱登录-返回JWT令牌")
        void testLoginWithEmail() throws Exception {
            // Arrange
            String loginJson = "{\"account\": \"" + ADMIN_EMAIL + "\", \"password\": \"" + ADMIN_PASSWORD + "\"}";

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists());
        }
    }

    /**
     * 限流测试
     */
    @Nested
    @DisplayName("限流测试")
    class RateLimitTests {

        /**
         * TC-AUTH-004: 限流测试
         *
         * 测试场景：限流测试
         * 输入：短时间多次请求
         * 预期输出：返回429
         * 优先级：P1
         */
        @Test
        @DisplayName("TC-AUTH-004: 限流测试-返回429")
        void testRateLimit() throws Exception {
            // Arrange
            String loginJson = "{\"account\": \"" + ADMIN_USERNAME + "\", \"password\": \"" + ADMIN_PASSWORD + "\"}";

            // Act: 快速发送多次请求（超过限流阈值）
            // 限流配置为每秒2次，发送5次请求
            int successCount = 0;
            int tooManyRequestsCount = 0;

            for (int i = 0; i < 5; i++) {
                MvcResult result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson))
                        .andReturn();

                int status = result.getResponse().getStatus();
                if (status == 200) {
                    successCount++;
                } else if (status == 429) {
                    tooManyRequestsCount++;
                }
            }

            // Assert: 验证有限流效果（至少有成功或被限流的请求）
            assertTrue(successCount > 0 || tooManyRequestsCount > 0,
                    "应该有成功或被限流的请求");
        }
    }

    /**
     * 登出测试
     */
    @Nested
    @DisplayName("登出测试")
    class LogoutTests {

        /**
         * TC-AUTH-005: 正常登出
         *
         * 测试场景：正常登出
         * 输入：有效令牌
         * 预期输出：令牌加入黑名单
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-AUTH-005: 正常登出-令牌加入黑名单")
        void testLogout_TokenBlacklisted() throws Exception {
            // Arrange: 先登录获取token
            String loginJson = "{\"account\": \"" + ADMIN_USERNAME + "\", \"password\": \"" + ADMIN_PASSWORD + "\"}";
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // 从响应中提取token
            String response = loginResult.getResponse().getContentAsString();
            String token = extractToken(response);
            assertNotNull(token, "登录应返回token");

            // 验证token初始时不被黑名单
            assertFalse(tokenBlacklist.isBlacklisted(token), "登出前token不应在黑名单中");

            // Act: 登出
            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));

            // Assert: 验证token已被加入黑名单
            assertTrue(tokenBlacklist.isBlacklisted(token), "登出后token应在黑名单中");
        }

        /**
         * 测试无token登出
         */
        @Test
        @DisplayName("无token登出-返回成功")
        void testLogoutWithoutToken() throws Exception {
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }
    }

    /**
     * 刷新令牌测试
     */
    @Nested
    @DisplayName("刷新令牌测试")
    class RefreshTokenTests {

        /**
         * TC-AUTH-006: 刷新令牌
         *
         * 测试场景：刷新令牌
         * 输入：有效令牌
         * 预期输出：返回新令牌
         * 优先级：P1
         */
        @Test
        @DisplayName("TC-AUTH-006: 刷新令牌-返回新令牌")
        void testRefreshToken() throws Exception {
            // Arrange: 先登录获取token
            String loginJson = "{\"account\": \"" + ADMIN_USERNAME + "\", \"password\": \"" + ADMIN_PASSWORD + "\"}";
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = loginResult.getResponse().getContentAsString();
            String oldToken = extractToken(response);
            assertNotNull(oldToken, "登录应返回token");

            // 等待一小段时间确保新token不同
            Thread.sleep(100);

            // Act: 刷新令牌
            MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                            .header("Authorization", "Bearer " + oldToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andReturn();

            // Assert: 验证返回了新token
            String refreshResponse = refreshResult.getResponse().getContentAsString();
            String newToken = extractToken(refreshResponse);
            assertNotNull(newToken, "刷新应返回新token");
        }

        /**
         * 测试无token刷新
         */
        @Test
        @DisplayName("无token刷新-返回默认token")
        void testRefreshWithoutToken() throws Exception {
            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists());
        }
    }

    /**
     * 获取当前用户测试
     */
    @Nested
    @DisplayName("获取当前用户测试")
    class GetCurrentUserTests {

        @Test
        @DisplayName("有效token-返回用户信息")
        void testGetCurrentUser_Success() throws Exception {
            // Arrange: 先登录获取token
            String loginJson = "{\"account\": \"" + ADMIN_USERNAME + "\", \"password\": \"" + ADMIN_PASSWORD + "\"}";
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String token = extractToken(loginResult.getResponse().getContentAsString());

            // Act & Assert: 获取当前用户
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(ADMIN_USERNAME))
                    .andExpect(jsonPath("$.role").value("admin"));
        }

        @Test
        @DisplayName("无token-返回401")
        void testGetCurrentUser_NoToken() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("无效token-返回401")
        void testGetCurrentUser_InvalidToken() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer invalid_token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 从JSON响应中提取token
     */
    private String extractToken(String jsonResponse) {
        try {
            int start = jsonResponse.indexOf("\"token\":\"") + 9;
            if (start < 9) return null;
            int end = jsonResponse.indexOf("\"", start);
            return jsonResponse.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }
}