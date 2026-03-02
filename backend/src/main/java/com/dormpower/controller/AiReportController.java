package com.dormpower.controller;

import com.dormpower.service.AiReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "AI分析报告", description = "智能用电分析报告接口")
public class AiReportController {

    @Autowired
    private AiReportService aiReportService;

    /**
     * 获取AI分析报告
     */
    @Operation(summary = "获取AI分析报告", 
               description = "获取指定房间的智能用电分析报告，包含用电趋势、异常检测和节能建议", 
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/{roomId}/ai_report")
    public ResponseEntity<?> getAiReport(
            @Parameter(description = "房间ID", required = true, example = "room_001")
            @PathVariable String roomId,
            @Parameter(description = "时间范围：7d(7天)、30d(30天)", 
                       required = false, 
                       example = "7d",
                       schema = @Schema(allowableValues = {"7d", "30d"}))
            @RequestParam(defaultValue = "7d") String period) {
        if (!period.equals("7d") && !period.equals("30d")) {
            throw new com.dormpower.exception.BusinessException("period is invalid, must be one of: 7d, 30d");
        }

        Map<String, Object> report = aiReportService.getAiReport(roomId, period);
        return ResponseEntity.ok(report);
    }

}
