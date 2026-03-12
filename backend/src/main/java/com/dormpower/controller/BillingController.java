package com.dormpower.controller;

import com.dormpower.annotation.AuditLog;
import com.dormpower.model.ElectricityBill;
import com.dormpower.model.ElectricityPriceRule;
import com.dormpower.model.RechargeRecord;
import com.dormpower.model.RoomBalance;
import com.dormpower.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 计费管理控制器
 */
@RestController
@RequestMapping("/api/billing")
@Tag(name = "计费管理", description = "电费计价、账单、充值管理接口")
public class BillingController {

    @Autowired
    private BillingService billingService;

    /**
     * 获取所有电价规则
     */
    @Operation(summary = "获取电价规则列表", description = "获取所有电费计价规则", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/price-rules")
    public ResponseEntity<?> getAllPriceRules() {
        List<ElectricityPriceRule> rules = billingService.getAllPriceRules();
        return ResponseEntity.ok(rules);
    }

    /**
     * 创建电价规则
     */
    @Operation(summary = "创建电价规则", description = "创建新的电费计价规则", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "创建失败")
    })
    @AuditLog(value = "创建电价规则", type = "BILLING")
    @PostMapping("/price-rules")
    public ResponseEntity<?> createPriceRule(
            @Parameter(description = "电价规则信息", required = true)
            @RequestBody ElectricityPriceRule rule) {
        try {
            ElectricityPriceRule created = billingService.createPriceRule(rule);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to create price rule: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 更新电价规则
     */
    @Operation(summary = "更新电价规则", description = "更新指定的电价规则", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "规则不存在")
    })
    @AuditLog(value = "更新电价规则", type = "BILLING")
    @PutMapping("/price-rules/{id}")
    public ResponseEntity<?> updatePriceRule(
            @Parameter(description = "规则ID", required = true) @PathVariable String id,
            @Parameter(description = "电价规则信息", required = true) @RequestBody ElectricityPriceRule rule) {
        try {
            ElectricityPriceRule updated = billingService.updatePriceRule(id, rule);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to update price rule: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 删除电价规则
     */
    @Operation(summary = "删除电价规则", description = "删除指定的电价规则", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "规则不存在")
    })
    @AuditLog(value = "删除电价规则", type = "BILLING")
    @DeleteMapping("/price-rules/{id}")
    public ResponseEntity<?> deletePriceRule(
            @Parameter(description = "规则ID", required = true) @PathVariable String id) {
        try {
            billingService.deletePriceRule(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Price rule deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to delete price rule: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 生成账单
     */
    @Operation(summary = "生成月度账单", description = "为指定房间生成月度电费账单", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "生成成功"),
            @ApiResponse(responseCode = "400", description = "生成失败")
    })
    @AuditLog(value = "生成电费账单", type = "BILLING")
    @PostMapping("/bills/generate")
    public ResponseEntity<?> generateBill(
            @Parameter(description = "房间ID", required = true) @RequestParam String roomId,
            @Parameter(description = "账单周期(YYYY-MM)", required = true) @RequestParam String period) {
        try {
            ElectricityBill bill = billingService.generateMonthlyBill(roomId, period);
            return ResponseEntity.ok(bill);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to generate bill: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取房间账单列表
     */
    @Operation(summary = "获取房间账单", description = "获取指定房间的所有账单", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/bills")
    public ResponseEntity<?> getRoomBills(
            @Parameter(description = "房间ID", required = true) @RequestParam String roomId,
            @Parameter(description = "页码", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") int size) {
        Page<ElectricityBill> bills = billingService.getRoomBills(roomId, PageRequest.of(page, size));
        return ResponseEntity.ok(bills);
    }

    /**
     * 获取待缴费账单
     */
    @Operation(summary = "获取待缴费账单", description = "获取所有待缴费的账单", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/bills/pending")
    public ResponseEntity<?> getPendingBills() {
        List<ElectricityBill> bills = billingService.getPendingBills();
        return ResponseEntity.ok(bills);
    }

    /**
     * 缴费
     */
    @Operation(summary = "缴纳电费", description = "为指定账单缴费", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "缴费成功"),
            @ApiResponse(responseCode = "400", description = "缴费失败")
    })
    @AuditLog(value = "缴纳电费", type = "BILLING")
    @PostMapping("/bills/{billId}/pay")
    public ResponseEntity<?> payBill(
            @Parameter(description = "账单ID", required = true) @PathVariable String billId,
            @Parameter(description = "支付方式", required = true) @RequestParam String paymentMethod,
            @Parameter(description = "操作员") @RequestParam(required = false) String operator) {
        try {
            ElectricityBill bill = billingService.payBill(billId, paymentMethod, operator);
            return ResponseEntity.ok(bill);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to pay bill: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 充值
     */
    @Operation(summary = "余额充值", description = "为指定房间充值电费", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "充值成功"),
            @ApiResponse(responseCode = "400", description = "充值失败")
    })
    @AuditLog(value = "电费充值", type = "BILLING")
    @PostMapping("/recharge")
    public ResponseEntity<?> recharge(
            @Parameter(description = "房间ID", required = true) @RequestParam String roomId,
            @Parameter(description = "充值金额", required = true) @RequestParam double amount,
            @Parameter(description = "支付方式", required = true) @RequestParam String paymentMethod,
            @Parameter(description = "操作员") @RequestParam(required = false) String operator) {
        try {
            RechargeRecord record = billingService.recharge(roomId, amount, paymentMethod, operator);
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to recharge: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取充值记录
     */
    @Operation(summary = "获取充值记录", description = "获取指定房间的充值记录", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/recharge-records")
    public ResponseEntity<?> getRechargeRecords(
            @Parameter(description = "房间ID", required = true) @RequestParam String roomId,
            @Parameter(description = "页码", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") int size) {
        Page<RechargeRecord> records = billingService.getRechargeRecords(roomId, PageRequest.of(page, size));
        return ResponseEntity.ok(records);
    }

    /**
     * 获取房间余额
     */
    @Operation(summary = "获取房间余额", description = "获取指定房间的电费余额", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/balance/{roomId}")
    public ResponseEntity<?> getRoomBalance(
            @Parameter(description = "房间ID", required = true) @PathVariable String roomId) {
        RoomBalance balance = billingService.getRoomBalance(roomId);
        return ResponseEntity.ok(balance);
    }

    /**
     * 获取余额不足的房间
     */
    @Operation(summary = "获取余额不足房间", description = "获取余额低于阈值的房间列表", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/low-balance")
    public ResponseEntity<?> getLowBalanceRooms(
            @Parameter(description = "阈值", example = "10.0") @RequestParam(defaultValue = "10.0") double threshold) {
        List<RoomBalance> balances = billingService.getLowBalanceRooms(threshold);
        return ResponseEntity.ok(balances);
    }
}
