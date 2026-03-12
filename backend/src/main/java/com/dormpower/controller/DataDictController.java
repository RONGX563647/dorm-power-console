package com.dormpower.controller;

import com.dormpower.model.DataDict;
import com.dormpower.service.DataDictService;
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
 * 数据字典控制器
 */
@RestController
@RequestMapping("/api/dict")
@Tag(name = "数据字典", description = "数据字典管理接口")
public class DataDictController {

    @Autowired
    private DataDictService dataDictService;

    @Operation(summary = "获取字典类型列表", description = "获取所有字典类型", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/types")
    public ResponseEntity<List<String>> getDictTypes() {
        return ResponseEntity.ok(dataDictService.getAllDictTypes());
    }

    @Operation(summary = "获取字典项列表", description = "根据类型获取字典项列表", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/type/{dictType}")
    public ResponseEntity<List<DataDict>> getDictsByType(
            @Parameter(description = "字典类型", required = true, example = "BILL_STATUS")
            @PathVariable String dictType) {
        return ResponseEntity.ok(dataDictService.getEnabledDictsByType(dictType));
    }

    @Operation(summary = "获取字典树形结构", description = "根据类型获取字典树形结构", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/tree/{dictType}")
    public ResponseEntity<List<Map<String, Object>>> getDictTree(
            @Parameter(description = "字典类型", required = true, example = "DEVICE_STATUS")
            @PathVariable String dictType) {
        return ResponseEntity.ok(dataDictService.getDictTree(dictType));
    }

    @Operation(summary = "分页查询字典项", description = "分页查询指定类型的字典项", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/page")
    public ResponseEntity<Page<DataDict>> getDictsPage(
            @Parameter(description = "字典类型", required = true, example = "BILL_STATUS")
            @RequestParam String dictType,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(dataDictService.getDictsByType(dictType, pageable));
    }

    @Operation(summary = "根据编码获取字典项", description = "根据字典编码获取字典项详情", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "字典项不存在")
    })
    @GetMapping("/code/{dictCode}")
    public ResponseEntity<?> getDictByCode(
            @Parameter(description = "字典编码", required = true, example = "PENDING")
            @PathVariable String dictCode) {
        return dataDictService.getDictByCode(dictCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "创建字典项", description = "创建新的字典项", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping
    public ResponseEntity<?> createDict(@RequestBody DataDict dict) {
        try {
            return ResponseEntity.ok(dataDictService.createDict(dict));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "批量创建字典项", description = "批量创建字典项", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    @PostMapping("/batch")
    public ResponseEntity<List<DataDict>> batchCreateDict(@RequestBody List<DataDict> dicts) {
        return ResponseEntity.ok(dataDictService.batchCreateDict(dicts));
    }

    @Operation(summary = "更新字典项", description = "更新字典项信息", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "字典项不存在")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDict(
            @Parameter(description = "字典ID", required = true)
            @PathVariable Long id,
            @RequestBody DataDict dict) {
        try {
            return ResponseEntity.ok(dataDictService.updateDict(id, dict));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "删除字典项", description = "删除指定的字典项", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "无法删除系统字典")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDict(
            @Parameter(description = "字典ID", required = true)
            @PathVariable Long id) {
        try {
            dataDictService.deleteDict(id);
            return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "获取字典标签", description = "根据类型和编码获取字典标签", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/label")
    public ResponseEntity<Map<String, String>> getDictLabel(
            @Parameter(description = "字典类型", required = true, example = "BILL_STATUS")
            @RequestParam String dictType,
            @Parameter(description = "字典编码", required = true, example = "PENDING")
            @RequestParam String dictCode) {
        String label = dataDictService.getDictLabel(dictType, dictCode);
        return ResponseEntity.ok(Map.of("label", label));
    }

    @Operation(summary = "初始化系统字典", description = "初始化系统预置字典数据", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "初始化成功")
    })
    @PostMapping("/init")
    public ResponseEntity<Map<String, String>> initSystemDicts() {
        dataDictService.initSystemDicts();
        return ResponseEntity.ok(Map.of("message", "System dictionaries initialized"));
    }
}
