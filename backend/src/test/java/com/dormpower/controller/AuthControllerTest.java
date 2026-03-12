package com.dormpower.controller;

import com.dormpower.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testLoginSuccess() throws Exception {
        String loginJson = "{\"account\": \"admin\", \"password\": \"admin123\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.user.role").value("admin"))
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    public void testLoginFailure() throws Exception {
        String loginJson = "{\"account\": \"admin\", \"password\": \"wrongpassword\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testRegister() throws Exception {
        String registerJson = "{\"username\": \"testuser\", \"email\": \"test@example.com\", \"password\": \"password123\"}";
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.role").value("user"))
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    public void testForgotPassword() throws Exception {
        String forgotPasswordJson = "{\"email\": \"admin@dorm.local\"}";
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType("application/json")
                        .content(forgotPasswordJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset email sent"));
    }

    @Test
    public void testLogout() throws Exception {
        // 先登录获取token
        String loginJson = "{\"account\": \"admin\", \"password\": \"admin123\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // 登出（这里简化测试，实际应该提取token并在请求头中发送）
        mockMvc.perform(post("/api/auth/logout")
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

}
