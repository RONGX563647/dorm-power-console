package com.dormpower.controller;

import com.dormpower.service.TelemetryService;
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
public class TelemetryController {

    @Autowired
    private TelemetryService telemetryService;

    /**
     * 获取遥测数据
     * @param device 设备ID
     * @param range 时间范围
     * @return 遥测数据列表
     */
    @GetMapping("/telemetry")
    public ResponseEntity<?> getTelemetry(
            @RequestParam String device,
            @RequestParam String range) {
        try {
            if (!range.matches("^(60s|24h|7d|30d)$")) {
                Map<String, Object> error = new HashMap<>();
                error.put("ok", false);
                error.put("code", "BAD_REQUEST");
                error.put("message", "range is invalid");
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
