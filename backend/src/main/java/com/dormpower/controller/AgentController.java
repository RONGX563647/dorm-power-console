package com.dormpower.controller;

import com.dormpower.agent.IntentRecognizer;
import com.dormpower.agent.LLMService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI客服Agent控制器
 * 
 * 提供智能对话接口
 */
@RestController
@RequestMapping("/api/agent")
@Tag(name = "AI客服", description = "智能客服对话接口")
public class AgentController {

    private final LLMService llmService;
    private final IntentRecognizer intentRecognizer;
    private final Map<String, StringBuilder> conversationHistory;

    public AgentController(LLMService llmService, IntentRecognizer intentRecognizer) {
        this.llmService = llmService;
        this.intentRecognizer = intentRecognizer;
        this.conversationHistory = new ConcurrentHashMap<>();
    }

    @Operation(summary = "智能对话", description = "与AI客服进行对话")
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody Map<String, String> request) {
        
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "消息不能为空"
            ));
        }

        String actualUserId = userId != null ? userId : "anonymous";
        
        String response = llmService.chat(actualUserId, message);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "response", response,
                "userId", actualUserId
        ));
    }

    @Operation(summary = "意图识别", description = "识别用户消息意图")
    @PostMapping("/intent")
    public ResponseEntity<Map<String, Object>> recognizeIntent(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "消息不能为空"
            ));
        }

        IntentRecognizer.IntentResult result = intentRecognizer.recognize(message);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("intent", result.intent);
        response.put("confidence", result.confidence);
        response.put("entities", result.entities);
        response.put("needLLM", result.needLLM);
        if (result.apiEndpoint != null) {
            response.put("apiEndpoint", result.apiEndpoint);
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "快速问答", description = "快速匹配常见问题")
    @PostMapping("/quick")
    public ResponseEntity<Map<String, Object>> quickReply(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "消息不能为空"
            ));
        }

        String reply = IntentRecognizer.quickMatch(message);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "matched", reply != null,
                "response", reply != null ? reply : "未匹配到快速回复"
        ));
    }

    @Operation(summary = "健康检查", description = "检查Agent服务状态")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "AI Agent",
                "features", Map.of(
                        "intentRecognition", true,
                        "llmIntegration", true,
                        "quickMatch", true
                )
        ));
    }
}
