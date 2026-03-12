package com.dormpower.controller;

import com.dormpower.model.MessageTemplate;
import com.dormpower.service.MessageTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息模板控制器
 */
@RestController
@RequestMapping("/api/message-templates")
@Tag(name = "消息模板", description = "消息模板管理接口")
public class MessageTemplateController {

    @Autowired
    private MessageTemplateService templateService;

    @Operation(summary = "创建消息模板", description = "创建新的消息模板", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "模板编码已存在")
    })
    @PostMapping
    public ResponseEntity<?> createTemplate(@RequestBody MessageTemplate template) {
        try {
            return ResponseEntity.ok(templateService.createTemplate(template));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "更新消息模板", description = "更新消息模板内容", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "模板不存在")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTemplate(
            @Parameter(description = "模板ID", required = true)
            @PathVariable Long id,
            @RequestBody MessageTemplate template) {
        try {
            return ResponseEntity.ok(templateService.updateTemplate(id, template));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "删除消息模板", description = "删除指定的消息模板", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "无法删除系统模板")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(
            @Parameter(description = "模板ID", required = true)
            @PathVariable Long id) {
        try {
            templateService.deleteTemplate(id);
            return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "根据编码获取模板", description = "根据模板编码获取模板详情", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "模板不存在")
    })
    @GetMapping("/code/{templateCode}")
    public ResponseEntity<?> getTemplateByCode(
            @Parameter(description = "模板编码", required = true, example = "BILL_DUE_REMINDER")
            @PathVariable String templateCode) {
        return templateService.getTemplateByCode(templateCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "根据类型获取模板列表", description = "获取指定类型的所有模板", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/type/{type}")
    public ResponseEntity<List<MessageTemplate>> getTemplatesByType(
            @Parameter(description = "模板类型", required = true, example = "BILLING")
            @PathVariable String type) {
        return ResponseEntity.ok(templateService.getTemplatesByType(type));
    }

    @Operation(summary = "根据渠道获取模板列表", description = "获取指定渠道的所有模板", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/channel/{channel}")
    public ResponseEntity<List<MessageTemplate>> getTemplatesByChannel(
            @Parameter(description = "渠道", required = true, example = "EMAIL")
            @PathVariable String channel) {
        return ResponseEntity.ok(templateService.getTemplatesByChannel(channel));
    }

    @Operation(summary = "获取所有启用的模板", description = "获取所有启用状态的消息模板", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/enabled")
    public ResponseEntity<List<MessageTemplate>> getAllEnabledTemplates() {
        return ResponseEntity.ok(templateService.getAllEnabledTemplates());
    }

    @Operation(summary = "渲染模板内容", description = "使用变量渲染模板内容", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "渲染成功"),
            @ApiResponse(responseCode = "404", description = "模板不存在")
    })
    @PostMapping("/render/{templateCode}")
    public ResponseEntity<?> renderTemplate(
            @Parameter(description = "模板编码", required = true, example = "BILL_DUE_REMINDER")
            @PathVariable String templateCode,
            @RequestBody Map<String, Object> variables) {
        try {
            String content = templateService.renderTemplate(templateCode, variables);
            String subject = templateService.renderSubject(templateCode, variables);
            String htmlContent = templateService.renderHtmlContent(templateCode, variables);

            Map<String, String> result = new HashMap<>();
            result.put("subject", subject);
            result.put("content", content);
            result.put("htmlContent", htmlContent);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "提取模板变量", description = "从模板内容中提取变量列表", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "提取成功")
    })
    @PostMapping("/extract-variables")
    public ResponseEntity<List<String>> extractVariables(
            @Parameter(description = "模板内容", required = true)
            @RequestBody String content) {
        return ResponseEntity.ok(templateService.extractVariables(content));
    }

    @Operation(summary = "初始化系统模板", description = "初始化系统预置消息模板", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "初始化成功")
    })
    @PostMapping("/init")
    public ResponseEntity<Map<String, String>> initSystemTemplates() {
        templateService.initSystemTemplates();
        return ResponseEntity.ok(Map.of("message", "System templates initialized"));
    }
}
