package com.dormpower.controller;

import com.dormpower.ml.PowerPredictionAgent;
import com.dormpower.service.AutoSavingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 智能节能控制器
 * 
 * 提供用电预测和自动节能API：
 * 1. 用电量预测
 * 2. 节能策略生成
 * 3. 自动节能控制
 * 4. 节能效果统计
 */
@RestController
@RequestMapping("/api/saving")
@Tag(name = "智能节能", description = "用电预测与自动节能控制接口")
public class AutoSavingController {

    @Autowired
    private AutoSavingService autoSavingService;

    @Autowired
    private PowerPredictionAgent predictionAgent;

    // ==================== 用电预测API ====================

    @Operation(summary = "预测用电量", description = "预测房间每日/每周/每月用电量", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "预测完成")
    })
    @GetMapping("/predict/{roomId}")
    public ResponseEntity<PowerPredictionAgent.PowerPrediction> predictPower(
            @Parameter(description = "房间ID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "预测天数", example = "7")
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(autoSavingService.manualPredict(roomId, days));
    }

    @Operation(summary = "获取小时预测", description = "获取24小时用电预测", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/predict/{roomId}/hourly")
    public ResponseEntity<List<PowerPredictionAgent.HourlyPrediction>> getHourlyPrediction(
            @Parameter(description = "房间ID", required = true)
            @PathVariable String roomId) {
        PowerPredictionAgent.PowerPrediction prediction = autoSavingService.getPrediction(roomId);
        if (prediction != null) {
            return ResponseEntity.ok(prediction.hourlyPredictions);
        }
        return ResponseEntity.ok(List.of());
    }

    // ==================== 节能策略API ====================

    @Operation(summary = "生成节能策略", description = "根据预测生成节能策略", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "生成成功")
    })
    @GetMapping("/strategies/{roomId}")
    public ResponseEntity<List<PowerPredictionAgent.EnergySavingStrategy>> getStrategies(
            @Parameter(description = "房间ID", required = true)
            @PathVariable String roomId) {
        List<PowerPredictionAgent.EnergySavingStrategy> strategies = autoSavingService.getOrCreateStrategies(roomId);
        return ResponseEntity.ok(strategies);
    }

    @Operation(summary = "执行节能策略", description = "手动执行指定节能策略", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "执行成功")
    })
    @PostMapping("/strategies/{roomId}/execute/{strategyId}")
    public ResponseEntity<Map<String, Object>> executeStrategy(
            @Parameter(description = "房间ID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "策略ID", required = true)
            @PathVariable String strategyId) {
        autoSavingService.manualExecuteStrategy(roomId, strategyId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "roomId", roomId,
                "strategyId", strategyId,
                "message", "策略执行成功"
        ));
    }

    // ==================== 自动节能控制API ====================

    @Operation(summary = "获取自动节能状态", description = "获取自动节能开关状态", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/auto/status")
    public ResponseEntity<Map<String, Object>> getAutoSavingStatus() {
        return ResponseEntity.ok(Map.of(
                "enabled", autoSavingService.isAutoSavingEnabled(),
                "message", autoSavingService.isAutoSavingEnabled() ? "自动节能已开启" : "自动节能已关闭"
        ));
    }

    @Operation(summary = "开启/关闭自动节能", description = "控制自动节能开关", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "设置成功")
    })
    @PostMapping("/auto/toggle")
    public ResponseEntity<Map<String, Object>> toggleAutoSaving(
            @Parameter(description = "是否开启", required = true)
            @RequestParam boolean enabled) {
        autoSavingService.setAutoSavingEnabled(enabled);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "enabled", enabled,
                "message", enabled ? "自动节能已开启" : "自动节能已关闭"
        ));
    }

    @Operation(summary = "设置用电阈值", description = "设置触发自动节能的日用电量阈值", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "设置成功")
    })
    @PostMapping("/auto/threshold")
    public ResponseEntity<Map<String, Object>> setThreshold(
            @Parameter(description = "日用电量阈值(kWh)", required = true, example = "2.0")
            @RequestParam double threshold) {
        autoSavingService.setDailyKwhThreshold(threshold);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "threshold", threshold,
                "message", String.format("阈值已设置为 %.2f kWh", threshold)
        ));
    }

    // ==================== 节能统计API ====================

    @Operation(summary = "获取节能统计", description = "获取房间节能效果统计", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/stats/{roomId}")
    public ResponseEntity<AutoSavingService.SavingStats> getSavingStats(
            @Parameter(description = "房间ID", required = true)
            @PathVariable String roomId) {
        AutoSavingService.SavingStats stats = autoSavingService.getSavingStats(roomId);
        if (stats != null) {
            return ResponseEntity.ok(stats);
        }
        return ResponseEntity.ok(new AutoSavingService.SavingStats());
    }

    @Operation(summary = "获取所有房间节能统计", description = "获取所有房间节能效果汇总", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/stats/all")
    public ResponseEntity<Map<String, Object>> getAllSavingStats() {
        Map<String, AutoSavingService.SavingStats> allStats = autoSavingService.getAllSavingStats();
        
        double totalSaved = allStats.values().stream()
                .mapToDouble(s -> s.totalSavedKwh.sum())
                .sum();
        
        int totalActions = allStats.values().stream()
                .mapToInt(s -> s.executedActions.get())
                .sum();
        
        return ResponseEntity.ok(Map.of(
                "rooms", allStats,
                "summary", Map.of(
                        "totalRooms", allStats.size(),
                        "totalSavedKwh", totalSaved,
                        "totalActions", totalActions,
                        "message", String.format("累计节省 %.2f kWh，执行 %d 次节能操作", totalSaved, totalActions)
                )
        ));
    }

    // ==================== 综合分析API ====================

    @Operation(summary = "节能分析报告", description = "生成房间节能分析报告", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "生成成功")
    })
    @GetMapping("/report/{roomId}")
    public ResponseEntity<Map<String, Object>> generateReport(
            @Parameter(description = "房间ID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "分析天数", example = "7")
            @RequestParam(defaultValue = "7") int days) {
        
        PowerPredictionAgent.PowerPrediction prediction = autoSavingService.manualPredict(roomId, days);
        List<PowerPredictionAgent.EnergySavingStrategy> strategies = predictionAgent.generateSavingStrategies(roomId, prediction);
        AutoSavingService.SavingStats stats = autoSavingService.getSavingStats(roomId);

        return ResponseEntity.ok(Map.of(
                "roomId", roomId,
                "prediction", prediction,
                "strategies", strategies,
                "stats", stats != null ? stats : new AutoSavingService.SavingStats(),
                "recommendations", generateRecommendations(prediction, strategies)
        ));
    }

    /**
     * 生成建议
     */
    private List<String> generateRecommendations(PowerPredictionAgent.PowerPrediction prediction,
                                                  List<PowerPredictionAgent.EnergySavingStrategy> strategies) {
        List<String> recommendations = new java.util.ArrayList<>();
        
        if (prediction.predictedDailyKwh > 2.0) {
            recommendations.add("建议开启自动节能模式，预计可节省 " + 
                    String.format("%.2f kWh/天", prediction.predictedDailyKwh * prediction.savingsPotential));
        }
        
        if (prediction.savingsPotential > 0.3) {
            recommendations.add("检测到较大节能潜力，建议执行高峰时段限电策略");
        }
        
        long highPriorityCount = strategies.stream()
                .filter(s -> s.priority <= 2)
                .count();
        if (highPriorityCount > 0) {
            recommendations.add(String.format("有 %d 个高优先级节能策略待执行", highPriorityCount));
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("用电情况良好，继续保持");
        }
        
        return recommendations;
    }
}
