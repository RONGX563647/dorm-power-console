package com.dormpower.controller;

import com.dormpower.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testQuickReply() throws Exception {
        String requestJson = "{\"message\": \"你好\"}";
        mockMvc.perform(post("/api/agent/quick")
                        .contentType("application/json")
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").exists())
                .andExpect(jsonPath("$.response").exists());
    }

    @Test
    public void testRecognizeIntent() throws Exception {
        String requestJson = "{\"message\": \"我的房间用电情况如何\"}";
        mockMvc.perform(post("/api/agent/intent")
                        .contentType("application/json")
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").exists())
                .andExpect(jsonPath("$.confidence").exists())
                .andExpect(jsonPath("$.entities").exists());
    }

    @Test
    public void testChat() throws Exception {
        String requestJson = "{\"message\": \"A-101房间的设备状态如何\"}";
        mockMvc.perform(post("/api/agent/chat")
                        .contentType("application/json")
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").exists());
    }

    @Test
    public void testChatWithRoomId() throws Exception {
        String requestJson = "{\"message\": \"房间A-101的用电量是多少\"}";
        mockMvc.perform(post("/api/agent/chat")
                        .contentType("application/json")
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").exists());
    }

    @Test
    public void testChatWithoutRoomId() throws Exception {
        String requestJson = "{\"message\": \"我的房间用电情况如何\"}";
        mockMvc.perform(post("/api/agent/chat")
                        .contentType("application/json")
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").exists());
    }

}