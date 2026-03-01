package com.dormpower.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 */
@RestController
@RequestMapping("/health")
@Tag(name = "健康检查", description = "服务健康状态检查接口")
public class HealthController {

    /**
     * 健康检查接口
     */
    @Operation(summary = "健康检查", description = "检查服务是否正常运行")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "服务正常")
    })
    @GetMapping
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "dorm-power-backend");
        response.put("version", "1.0.0");
        return response;
    }

}
