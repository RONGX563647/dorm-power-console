package com.dormpower.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetDevices() throws Exception {
        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("device-001"))
                .andExpect(jsonPath("$[0].name").value("宿舍101插座"))
                .andExpect(jsonPath("$[0].room").value("101"))
                .andExpect(jsonPath("$[0].online").value(true));
    }

    @Test
    public void testGetDeviceStatus() throws Exception {
        mockMvc.perform(get("/api/devices/device-001/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("device-001"))
                .andExpect(jsonPath("$.online").value(true))
                .andExpect(jsonPath("$.totalPowerW").value(120.5))
                .andExpect(jsonPath("$.voltageV").value(220.0))
                .andExpect(jsonPath("$.currentA").value(0.55));
    }

    @Test
    public void testSendCommand() throws Exception {
        String commandJson = "{\"action\": \"toggle\", \"socketId\": 1}";
        mockMvc.perform(post("/api/devices/device-001/commands")
                        .contentType("application/json")
                        .content(commandJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("device-001"))
                .andExpect(jsonPath("$.action").value("toggle"))
                .andExpect(jsonPath("$.socketId").value(1))
                .andExpect(jsonPath("$.status").value("pending"));
    }

}
