package com.dormpower.controller;

import com.dormpower.model.CollectionRecord;
import com.dormpower.service.CollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 催缴管理控制器
 */
@RestController
@RequestMapping("/api/collections")
@Tag(name = "催缴管理", description = "电费催缴管理接口")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    @Operation(summary = "创建催缴记录", description = "为指定账单创建催缴记录", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "404", description = "账单不存在")
    })
    @PostMapping
    public ResponseEntity<?> createCollectionRecord(
            @Parameter(description = "账单ID", required = true, example = "bill_12345678")
            @RequestParam String billId,
            @Parameter(description = "催缴类型", required = true, example = "REMINDER")
            @RequestParam String type,
            @Parameter(description = "通知渠道", required = true, example = "EMAIL")
            @RequestParam String channel) {
        try {
            return ResponseEntity.ok(collectionService.createCollectionRecord(billId, type, channel));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "获取催缴记录列表", description = "分页获取所有催缴记录", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public ResponseEntity<Page<CollectionRecord>> getCollectionRecords(
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(collectionService.getCollectionRecords(pageable));
    }

    @Operation(summary = "获取房间催缴记录", description = "获取指定房间的催缴记录", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<CollectionRecord>> getRoomCollectionRecords(
            @Parameter(description = "房间ID", required = true, example = "room_001")
            @PathVariable String roomId) {
        return ResponseEntity.ok(collectionService.getRoomCollectionRecords(roomId));
    }

    @Operation(summary = "获取账单催缴记录", description = "获取指定账单的催缴记录", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/bill/{billId}")
    public ResponseEntity<List<CollectionRecord>> getBillCollectionRecords(
            @Parameter(description = "账单ID", required = true, example = "bill_12345678")
            @PathVariable String billId) {
        return ResponseEntity.ok(collectionService.getBillCollectionRecords(billId));
    }

    @Operation(summary = "清理过期记录", description = "清理指定天数之前的催缴记录", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "清理成功")
    })
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Long>> cleanupOldRecords(
            @Parameter(description = "保留天数", required = true, example = "90")
            @RequestParam int retentionDays) {
        long deleted = collectionService.cleanupOldRecords(retentionDays);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}
