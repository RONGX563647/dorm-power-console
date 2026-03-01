package com.dormpower.controller;

import com.dormpower.service.TelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 遥测数据控制器
 */
@RestController
@RequestMapping("/api")
@Tag(name = "遥测数据", description = "设备功率数据查询接口")
public class TelemetryController {

    @Autowired
    private TelemetryService telemetryService;

    /**
     * 获取遥测数据
     */
    @Operation(summary = "获取遥测数据", 
               description = "获取指定设备的功率遥测数据，支持多种时间范围查询", 
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/telemetry")
    public ResponseEntity<?> getTelemetry(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @RequestParam String device,
            @Parameter(description = "时间范围：60s(1分钟)、24h(24小时)、7d(7天)、30d(30天)", 
                       required = true, 
                       example = "24h",
                       allowableValues = {"60s", "24h", "7d", "30d"})
            @RequestParam String range) {
        try {
            if (!range.matches("^(60s|24h|7d|30d)$")) {
                Map<String, Object> error = new HashMap<>();
                error.put("ok", false);
                error.put("code", "BAD_REQUEST");
                error.put("message", "range is invalid, must be one of: 60s, 24h, 7d, 30d");
                return ResponseEntity.badRequest().body(error);
            }

            List<Map<String, Object>> telemetryData = telemetryService.getTelemetry(device, range);
            return ResponseEntity.ok(telemetryData);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("code", "INTERNAL_ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

}
