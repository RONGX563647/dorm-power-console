package com.dormpower.controller;

import com.dormpower.service.AiReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * AI分析报告控制器
 */
@RestController
@RequestMapping("/api/rooms")
public class AiReportController {

    @Autowired
    private AiReportService aiReportService;

    /**
     * 获取AI分析报告
     * @param roomId 房间ID
     * @param period 时间范围
     * @return AI分析报告
     */
    @GetMapping("/{roomId}/ai_report")
    public ResponseEntity<?> getAiReport(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "7d") String period) {
        try {
            if (!period.equals("7d") && !period.equals("30d")) {
                Map<String, Object> error = new HashMap<>();
                error.put("ok", false);
                error.put("code", "BAD_REQUEST");
                error.put("message", "period is invalid");
                return ResponseEntity.badRequest().body(error);
            }

            Map<String, Object> report = aiReportService.getAiReport(roomId, period);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("code", "INTERNAL_ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

}
