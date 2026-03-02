package com.dormpower.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * 大模型API调用服务
 * 
 * 支持多种大模型API：
 * 1. OpenAI API (GPT-3.5/4)
 * 2. 阿里云通义千问
 * 3. 百度文心一言
 * 4. 本地Ollama（可选）
 * 
 * 优化：单例HttpClient，连接复用
 */
@Service
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient;
    private final IntentRecognizer intentRecognizer;

    @Value("${llm.provider:openai}")
    private String provider;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.api-url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${llm.model:gpt-3.5-turbo}")
    private String model;

    @Value("${llm.max-tokens:500}")
    private int maxTokens;

    @Value("${llm.temperature:0.7}")
    private double temperature;

    @Value("${llm.timeout:30}")
    private int timeoutSeconds;

    private static final String SYSTEM_PROMPT = """
        你是宿舍用电管理系统的智能客服助手。
        
        职责：
        1. 解答用户关于用电、设备、账单的问题
        2. 帮助用户控制智能设备
        3. 提供节能建议
        4. 处理告警和异常情况
        
        规则：
        - 回答简洁明了，不超过200字
        - 涉及设备操作时，确认用户意图
        - 无法处理的问题，引导用户联系管理员
        - 使用友好、专业的语气
        """;

    public LLMService(IntentRecognizer intentRecognizer) {
        this.intentRecognizer = intentRecognizer;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    /**
     * 处理用户消息
     */
    public String chat(String userId, String message) {
        String quickReply = IntentRecognizer.quickMatch(message);
        if (quickReply != null) {
            return quickReply;
        }

        IntentRecognizer.IntentResult intent = intentRecognizer.recognize(message);

        if (!intent.needLLM && intent.apiEndpoint != null) {
            return handleLocalIntent(intent, message);
        }

        return callLLMAPI(message, intent);
    }

    /**
     * 处理本地意图
     */
    private String handleLocalIntent(IntentRecognizer.IntentResult intent, String message) {
        StringBuilder response = new StringBuilder();
        
        switch (intent.intent) {
            case "DEVICE_CONTROL":
                response.append(buildDeviceControlResponse(intent.entities));
                break;
            case "POWER_QUERY":
                response.append(buildPowerQueryResponse(intent.entities));
                break;
            case "DEVICE_STATUS":
                response.append(buildDeviceStatusResponse(intent.entities));
                break;
            case "BILL_QUERY":
                response.append(buildBillQueryResponse(intent.entities));
                break;
            case "ALARM_QUERY":
                response.append(buildAlarmQueryResponse(intent.entities));
                break;
            default:
                return callLLMAPI(message, intent);
        }

        return response.toString();
    }

    private String buildDeviceControlResponse(Map<String, String> entities) {
        String roomId = entities.getOrDefault("roomId", "您的房间");
        String device = entities.getOrDefault("DEVICE", "设备");
        String action = entities.getOrDefault("action", "操作");

        if ("on".equals(action)) {
            return String.format("好的，正在为您打开%s的%s，请稍候...", roomId, device);
        } else if ("off".equals(action)) {
            return String.format("好的，正在为您关闭%s的%s，请稍候...", roomId, device);
        }
        return String.format("我理解您想对%s的%s进行操作，请确认具体操作。", roomId, device);
    }

    private String buildPowerQueryResponse(Map<String, String> entities) {
        String roomId = entities.getOrDefault("roomId", "您的房间");
        String period = entities.getOrDefault("period", "今天");
        
        return String.format("正在查询%s%s的用电情况，请稍候...", roomId, getPeriodText(period));
    }

    private String buildDeviceStatusResponse(Map<String, String> entities) {
        String roomId = entities.getOrDefault("roomId", "您的房间");
        return String.format("正在查询%s的设备状态，请稍候...", roomId);
    }

    private String buildBillQueryResponse(Map<String, String> entities) {
        String roomId = entities.getOrDefault("roomId", "您的房间");
        return String.format("正在查询%s的电费账单，请稍候...", roomId);
    }

    private String buildAlarmQueryResponse(Map<String, String> entities) {
        return "正在查询告警信息，请稍候...";
    }

    private String getPeriodText(String period) {
        return switch (period) {
            case "today" -> "今天";
            case "yesterday" -> "昨天";
            case "week" -> "本周";
            case "month" -> "本月";
            default -> period;
        };
    }

    /**
     * 调用大模型API
     */
    private String callLLMAPI(String message, IntentRecognizer.IntentResult intent) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
            
            String contextPrompt = buildContextPrompt(message, intent);
            messages.add(Map.of("role", "user", "content", contextPrompt));
            
            requestBody.put("messages", messages);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseLLMResponse(response.body());
            } else {
                logger.error("LLM API error: {} - {}", response.statusCode(), response.body());
                return getFallbackResponse(intent);
            }

        } catch (Exception e) {
            logger.error("Failed to call LLM API", e);
            return getFallbackResponse(intent);
        }
    }

    /**
     * 构建上下文提示
     */
    private String buildContextPrompt(String message, IntentRecognizer.IntentResult intent) {
        StringBuilder prompt = new StringBuilder(message);
        
        if (intent.entities.containsKey("roomId")) {
            prompt.append("\n\n上下文信息：");
            prompt.append("\n房间号：").append(intent.entities.get("roomId"));
        }
        if (intent.intent != null && !"UNKNOWN".equals(intent.intent)) {
            prompt.append("\n用户意图：").append(intent.intent);
        }

        return prompt.toString();
    }

    /**
     * 解析大模型响应
     */
    private String parseLLMResponse(String responseBody) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) choice.get("message");
                return (String) message.get("content");
            }
        } catch (Exception e) {
            logger.error("Failed to parse LLM response", e);
        }
        return "抱歉，我暂时无法回答这个问题，请稍后再试或联系管理员。";
    }

    /**
     * 降级响应
     */
    private String getFallbackResponse(IntentRecognizer.IntentResult intent) {
        if (intent.intent != null) {
            return String.format("我理解您想%s，但系统暂时繁忙，请稍后再试。", 
                    getIntentDescription(intent.intent));
        }
        return "抱歉，系统暂时繁忙，请稍后再试或联系管理员。";
    }

    private String getIntentDescription(String intent) {
        return switch (intent) {
            case "DEVICE_CONTROL" -> "控制设备";
            case "POWER_QUERY" -> "查询用电";
            case "DEVICE_STATUS" -> "查询设备状态";
            case "BILL_QUERY" -> "查询账单";
            case "ALARM_QUERY" -> "查询告警";
            default -> "进行操作";
        };
    }

    /**
     * 流式响应（可选）
     */
    public void chatStream(String userId, String message, StreamCallback callback) {
        new Thread(() -> {
            String response = chat(userId, message);
            callback.onComplete(response);
        }).start();
    }

    public interface StreamCallback {
        void onComplete(String response);
    }
}
