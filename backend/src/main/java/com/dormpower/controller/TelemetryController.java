package com.dormpower.controller;

import com.dormpower.service.TelemetryService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 遥测数据控制器
 * 
 * 提供设备遥测数据的查询和导出功能，包括：
 * - 遥测数据查询（支持多种时间范围）
 * - 用电统计报表生成
 * - 遥测数据导出（CSV格式）
 * 
 * 所有接口都需要Bearer Token认证
 * 
 * @author dormpower team
 * @version 1.0
 */
@RestController
@RequestMapping("/api")
@Tag(name = "遥测数据", description = "提供设备遥测数据查询和导出的RESTful API接口")
public class TelemetryController {

    // 遥测服务，处理遥测数据的业务逻辑
    @Autowired
    private TelemetryService telemetryService;

    /**
     * 获取遥测数据
     * 
     * 获取指定设备的功率遥测数据，支持多种时间范围查询。
     * 支持的时间范围：
     * - 60s：最近1分钟的数据
     * - 24h：最近24小时的数据
     * - 7d：最近7天的数据
     * - 30d：最近30天的数据
     * 
     * @param device 设备ID，唯一标识一个设备
     * @param range 时间范围，必须是60s、24h、7d或30d之一
     * @return 遥测数据列表，每个数据点包含时间戳和功率值
     * @throws BusinessException 当range参数无效时抛出
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
                       schema = @Schema(allowableValues = {"60s", "24h", "7d", "30d"}))
            @RequestParam String range) {
        // 验证时间范围参数是否合法
        if (!range.matches("^(60s|24h|7d|30d)$")) {
            throw new com.dormpower.exception.BusinessException("range is invalid, must be one of: 60s, 24h, 7d, 30d");
        }

        // 查询遥测数据
        List<Map<String, Object>> telemetryData = telemetryService.getTelemetry(device, range);
        return ResponseEntity.ok(telemetryData);
    }

    /**
     * 获取用电统计报表
     */
    @Operation(summary = "获取用电统计报表", 
               description = "生成指定设备的日/周/月/年用电统计报表", 
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @GetMapping("/telemetry/statistics")
    public ResponseEntity<?> getElectricityStatistics(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @RequestParam String device,
            @Parameter(description = "统计周期：day(日)、week(周)、month(月)、year(年)", 
                       required = true, 
                       example = "day",
                       schema = @Schema(allowableValues = {"day", "week", "month", "year"}))
            @RequestParam String period,
            @Parameter(description = "开始时间戳", required = true, example = "1700000000")
            @RequestParam long start,
            @Parameter(description = "结束时间戳", required = true, example = "1700086400")
            @RequestParam long end) {
        if (!period.matches("^(day|week|month|year)$")) {
            throw new com.dormpower.exception.BusinessException("period is invalid, must be one of: day, week, month, year");
        }

        Map<String, Object> statistics = telemetryService.getElectricityStatistics(device, period, start, end);
        return ResponseEntity.ok(statistics);
    }

    /**
     * 导出遥测数据
     */
    @Operation(summary = "导出遥测数据", 
               description = "导出指定设备的遥测数据，支持CSV格式", 
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "导出成功"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @GetMapping("/telemetry/export")
    public ResponseEntity<?> exportTelemetry(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @RequestParam String device,
            @Parameter(description = "导出格式：csv", 
                       required = true, 
                       example = "csv",
                       schema = @Schema(allowableValues = {"csv"}))
            @RequestParam String format,
            @Parameter(description = "开始时间戳", required = true, example = "1700000000")
            @RequestParam long start,
            @Parameter(description = "结束时间戳", required = true, example = "1700086400")
            @RequestParam long end) {
        if (!format.equals("csv")) {
            throw new com.dormpower.exception.BusinessException("format is invalid, only csv is supported");
        }

        byte[] data = telemetryService.exportTelemetry(device, format, start, end);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=telemetry_" + device + "_" + start + "_" + end + "." + format)
                .header("Content-Type", "text/csv")
                .body(data);
    }

}
