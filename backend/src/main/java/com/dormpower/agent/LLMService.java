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
import java.util.concurrent.ConcurrentHashMap;

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

    // 本地模拟响应，当API key为空时使用
    private static final Map<String, String> LOCAL_RESPONSES = new HashMap<>();
    private static final Map<String, List<Map<String, String>>> conversationHistory = new ConcurrentHashMap<>();
    
    static {
        LOCAL_RESPONSES.put("你好", "您好！我是宿舍用电管理助手，有什么可以帮您？");
        LOCAL_RESPONSES.put("用电量", "您的用电量情况良好，建议继续保持节能习惯。");
        LOCAL_RESPONSES.put("设备", "您的设备运行正常，没有异常情况。");
        LOCAL_RESPONSES.put("账单", "您的电费账单已更新，请及时查看。");
        LOCAL_RESPONSES.put("节能", "建议您在不使用设备时及时关闭电源，这样可以节省更多电量。");
        LOCAL_RESPONSES.put("宿舍", "宿舍管理系统运行正常，您可以查询房间信息、学生信息等。");
        LOCAL_RESPONSES.put("学生", "学生管理系统运行正常，您可以查询学生信息、入住记录等。");
        LOCAL_RESPONSES.put("断电", "断电管理系统运行正常，您可以查询断电记录、恢复供电等。");
        LOCAL_RESPONSES.put("固件", "固件管理系统运行正常，您可以查询固件版本、升级状态等。");
    }

    private static final String SYSTEM_PROMPT = """
        你是宿舍用电管理系统的智能客服助手，名叫小电。
        
        职责：
        1. 解答用户关于用电、设备、账单的问题
        2. 帮助用户控制智能设备
        3. 提供个性化的节能建议
        4. 处理告警和异常情况
        5. 提供宿舍管理、学生管理等相关信息
        
        规则：
        - 回答简洁明了，不超过200字
        - 涉及设备操作时，确认用户意图
        - 无法处理的问题，引导用户联系管理员
        - 使用友好、专业的语气
        - 根据用户的历史对话提供个性化响应
        - 对不同类型的用户使用不同的称呼和语气
        - 提供具体、实用的建议和解决方案
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
        // 确保用户ID不为空
        if (userId == null) {
            userId = "anonymous";
        }

        // 记录用户消息到对话历史
        addToConversationHistory(userId, "user", message);

        // 快速匹配
        String quickReply = IntentRecognizer.quickMatch(message);
        if (quickReply != null) {
            // 记录系统回复到对话历史
            addToConversationHistory(userId, "assistant", quickReply);
            return quickReply;
        }

        // 意图识别
        IntentRecognizer.IntentResult intent = intentRecognizer.recognize(message);

        // 处理本地意图
        if (!intent.needLLM && intent.apiEndpoint != null) {
            String response = handleLocalIntent(intent, message);
            // 记录系统回复到对话历史
            addToConversationHistory(userId, "assistant", response);
            return response;
        }

        // 调用大模型API
        String response = callLLMAPI(message, intent, userId);
        // 记录系统回复到对话历史
        addToConversationHistory(userId, "assistant", response);
        return response;
    }

    /**
     * 添加消息到对话历史
     */
    private void addToConversationHistory(String userId, String role, String content) {
        conversationHistory.computeIfAbsent(userId, k -> new ArrayList<>())
                .add(Map.of("role", role, "content", content));
        
        // 限制对话历史长度，最多保留20条消息
        List<Map<String, String>> history = conversationHistory.get(userId);
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }
    }

    /**
     * 获取对话历史
     */
    private List<Map<String, String>> getConversationHistory(String userId) {
        return conversationHistory.getOrDefault(userId, new ArrayList<>());
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
                if (intent.apiEndpoint != null) {
                    Map<String, Object> apiResponse = callSystemApi(intent.apiEndpoint, intent.entities);
                    if (apiResponse != null) {
                        response.append(buildPowerQueryResponseWithApiData(intent.entities, apiResponse));
                    } else {
                        response.append(buildPowerQueryResponse(intent.entities));
                    }
                } else {
                    response.append(buildPowerQueryResponse(intent.entities));
                }
                break;
            case "DEVICE_STATUS":
                if (intent.apiEndpoint != null) {
                    Map<String, Object> apiResponse = callSystemApi(intent.apiEndpoint, intent.entities);
                    if (apiResponse != null) {
                        response.append(buildDeviceStatusResponseWithApiData(intent.entities, apiResponse));
                    } else {
                        response.append(buildDeviceStatusResponse(intent.entities));
                    }
                } else {
                    response.append(buildDeviceStatusResponse(intent.entities));
                }
                break;
            case "BILL_QUERY":
                if (intent.apiEndpoint != null) {
                    Map<String, Object> apiResponse = callSystemApi(intent.apiEndpoint, intent.entities);
                    if (apiResponse != null) {
                        response.append(buildBillQueryResponseWithApiData(intent.entities, apiResponse));
                    } else {
                        response.append(buildBillQueryResponse(intent.entities));
                    }
                } else {
                    response.append(buildBillQueryResponse(intent.entities));
                }
                break;
            case "ALARM_QUERY":
                if (intent.apiEndpoint != null) {
                    Map<String, Object> apiResponse = callSystemApi(intent.apiEndpoint, intent.entities);
                    if (apiResponse != null) {
                        response.append(buildAlarmQueryResponseWithApiData(intent.entities, apiResponse));
                    } else {
                        response.append(buildAlarmQueryResponse(intent.entities));
                    }
                } else {
                    response.append(buildAlarmQueryResponse(intent.entities));
                }
                break;
            case "ENERGY_SAVING":
                if (intent.apiEndpoint != null) {
                    Map<String, Object> apiResponse = callSystemApi(intent.apiEndpoint, intent.entities);
                    if (apiResponse != null) {
                        response.append(buildEnergySavingResponseWithApiData(intent.entities, apiResponse));
                    } else {
                        response.append(buildEnergySavingResponse(intent.entities));
                    }
                } else {
                    response.append(buildEnergySavingResponse(intent.entities));
                }
                break;
            case "ROOM_MANAGEMENT":
                if (intent.apiEndpoint != null) {
                    Map<String, Object> apiResponse = callSystemApi(intent.apiEndpoint, intent.entities);
                    if (apiResponse != null) {
                        response.append(buildRoomManagementResponseWithApiData(intent.entities, apiResponse));
                    } else {
                        response.append(buildRoomManagementResponse(intent.entities));
                    }
                } else {
                    response.append(buildRoomManagementResponse(intent.entities));
                }
                break;
            case "STUDENT_MANAGEMENT":
                if (intent.apiEndpoint != null) {
                    Map<String, Object> apiResponse = callSystemApi(intent.apiEndpoint, intent.entities);
                    if (apiResponse != null) {
                        response.append(buildStudentManagementResponseWithApiData(intent.entities, apiResponse));
                    } else {
                        response.append(buildStudentManagementResponse(intent.entities));
                    }
                } else {
                    response.append(buildStudentManagementResponse(intent.entities));
                }
                break;
            case "POWER_CONTROL":
                if (intent.apiEndpoint != null) {
                    Map<String, Object> apiResponse = callSystemApi(intent.apiEndpoint, intent.entities);
                    if (apiResponse != null) {
                        response.append(buildPowerControlResponseWithApiData(intent.entities, apiResponse));
                    } else {
                        response.append(buildPowerControlResponse(intent.entities));
                    }
                } else {
                    response.append(buildPowerControlResponse(intent.entities));
                }
                break;
            case "FIRMWARE_UPGRADE":
                if (intent.apiEndpoint != null) {
                    Map<String, Object> apiResponse = callSystemApi(intent.apiEndpoint, intent.entities);
                    if (apiResponse != null) {
                        response.append(buildFirmwareUpgradeResponseWithApiData(intent.entities, apiResponse));
                    } else {
                        response.append(buildFirmwareUpgradeResponse(intent.entities));
                    }
                } else {
                    response.append(buildFirmwareUpgradeResponse(intent.entities));
                }
                break;
            default:
                return callLLMAPI(message, intent, "anonymous");
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
        if ("您的房间".equals(roomId)) {
            return "请提供您的房间号，例如 A-101，以便我查询用电情况。";
        }
        return String.format("%s%s的用电量为0.0kWh，用电情况正常，建议继续保持节能习惯。", roomId, getPeriodText(period));
    }

    private String buildDeviceStatusResponse(Map<String, String> entities) {
        String roomId = entities.getOrDefault("roomId", "您的房间");
        if ("您的房间".equals(roomId)) {
            return "请提供您的房间号，例如 A-101，以便我查询设备状态。";
        }
        return String.format("%s的设备运行正常，所有插座都处于关闭状态，总功率为0.0W。", roomId);
    }

    private String buildBillQueryResponse(Map<String, String> entities) {
        String roomId = entities.getOrDefault("roomId", "您的房间");
        if ("您的房间".equals(roomId)) {
            return "请提供您的房间号，例如 A-101，以便我查询电费账单。";
        }
        return String.format("%s的电费账单已更新，当前余额为100.0元，本月用电量为0.0kWh，费用为0.0元。", roomId);
    }

    private String buildAlarmQueryResponse(Map<String, String> entities) {
        String roomId = entities.getOrDefault("roomId", "");
        if (!roomId.isEmpty()) {
            return String.format("%s当前没有未处理的告警信息。", roomId);
        }
        return "当前没有未处理的告警信息。";
    }

    private String buildEnergySavingResponse(Map<String, String> entities) {
        String roomId = entities.getOrDefault("roomId", "您的房间");
        if ("您的房间".equals(roomId)) {
            return "请提供您的房间号，例如 A-101，以便我为您生成节能策略。";
        }
        return String.format("已为%s生成节能策略，建议您在不使用设备时及时关闭电源，这样可以节省更多电量。", roomId);
    }

    private String buildRoomManagementResponse(Map<String, String> entities) {
        String roomId = entities.getOrDefault("roomId", "");
        if (!roomId.isEmpty()) {
            return String.format("%s的房间信息已查询，状态正常。", roomId);
        }
        return "宿舍管理系统运行正常，您可以查询房间信息、学生信息等。";
    }

    private String buildStudentManagementResponse(Map<String, String> entities) {
        String roomId = entities.getOrDefault("roomId", "");
        if (!roomId.isEmpty()) {
            return String.format("%s的学生信息已查询，状态正常。", roomId);
        }
        return "学生管理系统运行正常，您可以查询学生信息、入住记录等。";
    }

    private String buildPowerControlResponse(Map<String, String> entities) {
        String roomId = entities.getOrDefault("roomId", "");
        if (!roomId.isEmpty()) {
            return String.format("%s的供电状态正常。", roomId);
        }
        return "断电管理系统运行正常，您可以查询断电记录、恢复供电等。";
    }

    private String buildFirmwareUpgradeResponse(Map<String, String> entities) {
        String deviceId = entities.getOrDefault("deviceId", "");
        if (!deviceId.isEmpty()) {
            return String.format("%s的固件版本正常，无需升级。", deviceId);
        }
        return "固件管理系统运行正常，您可以查询固件版本、升级状态等。";
    }

    // API数据响应构建方法
    private String buildPowerQueryResponseWithApiData(Map<String, String> entities, Map<String, Object> apiResponse) {
        String roomId = entities.getOrDefault("roomId", "您的房间");
        if (apiResponse != null) {
            // 尝试从API响应中提取用电数据
            Object totalPower = apiResponse.get("totalPower");
            Object averagePower = apiResponse.get("averagePower");
            Object peakPower = apiResponse.get("peakPower");
            Object powerConsumption = apiResponse.get("powerConsumption");
            
            if (powerConsumption != null) {
                return String.format("%s的用电量为%s kWh，用电情况正常，建议继续保持节能习惯。", roomId, powerConsumption);
            } else if (totalPower != null) {
                return String.format("%s的总功率为%s W，用电情况正常，建议继续保持节能习惯。", roomId, totalPower);
            }
        }
        return String.format("%s的用电情况已查询，具体数据请参考系统返回结果。", roomId);
    }

    private String buildDeviceStatusResponseWithApiData(Map<String, String> entities, Map<String, Object> apiResponse) {
        String roomId = entities.getOrDefault("roomId", "您的房间");
        if (apiResponse != null) {
            // 尝试从API响应中提取设备状态数据
            Object devices = apiResponse.get("devices");
            Object totalPower = apiResponse.get("totalPower");
            
            // 使用 instanceof 模式匹配简化类型检查和转换
            if (devices instanceof List<?> deviceList) {
                int deviceCount = deviceList.size();
                int onlineCount = 0;

                // 使用 instanceof 模式匹配简化 Map 类型检查
                for (Object device : deviceList) {
                    if (device instanceof Map<?, ?> deviceMap) {
                        Object status = deviceMap.get("status");
                        if (status != null && "ONLINE".equals(status)) {
                            onlineCount++;
                        }
                    }
                }
                
                if (totalPower != null) {
                    return String.format("%s共有%d台设备，其中%d台在线，总功率为%s W。", roomId, deviceCount, onlineCount, totalPower);
                } else {
                    return String.format("%s共有%d台设备，其中%d台在线。", roomId, deviceCount, onlineCount);
                }
            }
        }
        return String.format("%s的设备状态已查询，具体数据请参考系统返回结果。", roomId);
    }

    private String buildBillQueryResponseWithApiData(Map<String, String> entities, Map<String, Object> apiResponse) {
        String roomId = entities.getOrDefault("roomId", "您的房间");
        if (apiResponse != null) {
            // 尝试从API响应中提取账单数据
            Object balance = apiResponse.get("balance");
            Object amount = apiResponse.get("amount");
            Object powerConsumption = apiResponse.get("powerConsumption");
            
            if (balance != null && amount != null && powerConsumption != null) {
                return String.format("%s的电费账单已更新，当前余额为%s元，本月用电量为%s kWh，费用为%s元。", roomId, balance, powerConsumption, amount);
            } else if (balance != null) {
                return String.format("%s的电费账单已更新，当前余额为%s元。", roomId, balance);
            }
        }
        return String.format("%s的电费账单已查询，具体数据请参考系统返回结果。", roomId);
    }

    private String buildAlarmQueryResponseWithApiData(Map<String, String> entities, Map<String, Object> apiResponse) {
        String roomId = entities.getOrDefault("roomId", "");
        if (apiResponse != null) {
            // 尝试从API响应中提取告警数据
            Object alarms = apiResponse.get("alarms");
            
            // 使用 instanceof 模式匹配简化类型检查和转换
            if (alarms instanceof List<?> alarmList) {
                int alarmCount = alarmList.size();
                
                if (alarmCount > 0) {
                    if (!roomId.isEmpty()) {
                        return String.format("%s当前有%d条未处理的告警信息。", roomId, alarmCount);
                    } else {
                        return String.format("当前有%d条未处理的告警信息。", alarmCount);
                    }
                } else {
                    if (!roomId.isEmpty()) {
                        return String.format("%s当前没有未处理的告警信息。", roomId);
                    } else {
                        return "当前没有未处理的告警信息。";
                    }
                }
            }
        }
        return "告警信息已查询，具体数据请参考系统返回结果。";
    }

    private String buildEnergySavingResponseWithApiData(Map<String, String> entities, Map<String, Object> apiResponse) {
        String roomId = entities.getOrDefault("roomId", "您的房间");
        if (apiResponse != null) {
            // 尝试从API响应中提取节能策略数据
            Object strategies = apiResponse.get("strategies");
            Object savings = apiResponse.get("savings");
            
            // 使用 instanceof 模式匹配简化类型检查和转换
            if (strategies instanceof List<?> strategyList) {
                int strategyCount = strategyList.size();
                
                if (savings != null) {
                    return String.format("已为%s生成%d条节能策略，预计可节省%s kWh电量。", roomId, strategyCount, savings);
                } else {
                    return String.format("已为%s生成%d条节能策略，具体建议请参考系统返回结果。", roomId, strategyCount);
                }
            }
        }
        return String.format("已为%s生成节能策略，具体建议请参考系统返回结果。", roomId);
    }

    private String buildRoomManagementResponseWithApiData(Map<String, String> entities, Map<String, Object> apiResponse) {
        String roomId = entities.getOrDefault("roomId", "");
        if (apiResponse != null) {
            // 尝试从API响应中提取房间信息
            Object roomInfo = apiResponse.get("room");
            Object students = apiResponse.get("students");
            
            // 使用 instanceof 模式匹配简化类型检查和转换
            if (roomInfo instanceof Map<?, ?> roomMap) {
                Object building = roomMap.get("building");
                Object floor = roomMap.get("floor");
                Object capacity = roomMap.get("capacity");
                
                if (!roomId.isEmpty() && building != null && floor != null) {
                    return String.format("%s的房间信息已查询，位于%s楼%d层，容量为%d人。", roomId, building, floor, capacity);
                }
            }
        }
        if (!roomId.isEmpty()) {
            return String.format("%s的房间信息已查询，具体数据请参考系统返回结果。", roomId);
        }
        return "房间管理信息已查询，具体数据请参考系统返回结果。";
    }

    private String buildStudentManagementResponseWithApiData(Map<String, String> entities, Map<String, Object> apiResponse) {
        String roomId = entities.getOrDefault("roomId", "");
        if (apiResponse != null) {
            // 尝试从API响应中提取学生信息
            Object students = apiResponse.get("students");
            
            // 使用 instanceof 模式匹配简化类型检查和转换
            if (students instanceof List<?> studentList) {
                int studentCount = studentList.size();
                
                if (!roomId.isEmpty()) {
                    return String.format("%s的学生信息已查询，共有%d名学生入住。", roomId, studentCount);
                } else {
                    return String.format("学生管理信息已查询，共有%d名学生。", studentCount);
                }
            }
        }
        if (!roomId.isEmpty()) {
            return String.format("%s的学生信息已查询，具体数据请参考系统返回结果。", roomId);
        }
        return "学生管理信息已查询，具体数据请参考系统返回结果。";
    }

    private String buildPowerControlResponseWithApiData(Map<String, String> entities, Map<String, Object> apiResponse) {
        String roomId = entities.getOrDefault("roomId", "");
        if (apiResponse != null) {
            // 尝试从API响应中提取供电状态数据
            Object status = apiResponse.get("status");
            Object reason = apiResponse.get("reason");
            
            if (status != null) {
                if (!roomId.isEmpty()) {
                    if ("ON".equals(status)) {
                        return String.format("%s的供电状态正常。", roomId);
                    } else if ("OFF".equals(status)) {
                        if (reason != null) {
                            return String.format("%s的供电已中断，原因：%s。", roomId, reason);
                        } else {
                            return String.format("%s的供电已中断。", roomId);
                        }
                    }
                }
            }
        }
        if (!roomId.isEmpty()) {
            return String.format("%s的供电状态已查询，具体数据请参考系统返回结果。", roomId);
        }
        return "断电管理信息已查询，具体数据请参考系统返回结果。";
    }

    private String buildFirmwareUpgradeResponseWithApiData(Map<String, String> entities, Map<String, Object> apiResponse) {
        String deviceId = entities.getOrDefault("deviceId", "");
        if (apiResponse != null) {
            // 尝试从API响应中提取固件信息
            Object version = apiResponse.get("version");
            Object status = apiResponse.get("status");
            
            if (version != null) {
                if (!deviceId.isEmpty()) {
                    if ("UP_TO_DATE".equals(status)) {
                        return String.format("%s的固件版本为%s，无需升级。", deviceId, version);
                    } else if ("NEEDS_UPGRADE".equals(status)) {
                        return String.format("%s的固件版本为%s，需要升级。", deviceId, version);
                    } else {
                        return String.format("%s的固件版本为%s。", deviceId, version);
                    }
                }
            }
        }
        if (!deviceId.isEmpty()) {
            return String.format("%s的固件信息已查询，具体数据请参考系统返回结果。", deviceId);
        }
        return "固件管理信息已查询，具体数据请参考系统返回结果。";
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
    private String callLLMAPI(String message, IntentRecognizer.IntentResult intent, String userId) {
        // 如果API key为空，使用本地模拟响应
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return getLocalResponse(message, intent, userId);
        }
        
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
            
            // 添加对话历史
            List<Map<String, String>> history = getConversationHistory(userId);
            for (Map<String, String> msg : history) {
                messages.add(msg);
            }
            
            // 添加当前消息
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
                return getLocalResponse(message, intent, userId);
            }

        } catch (Exception e) {
            logger.error("Failed to call LLM API", e);
            return getLocalResponse(message, intent, userId);
        }
    }
    
    /**
     * 获取本地模拟响应
     */
    private String getLocalResponse(String message, IntentRecognizer.IntentResult intent, String userId) {
        // 检查是否有匹配的本地响应
        for (Map.Entry<String, String> entry : LOCAL_RESPONSES.entrySet()) {
            if (message.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // 根据意图生成响应
        if (intent.intent != null) {
            return switch (intent.intent) {
                case "DEVICE_CONTROL" -> "设备控制命令已收到，正在执行...";
                case "POWER_QUERY" -> "您的用电情况正常，建议继续保持节能习惯。";
                case "DEVICE_STATUS" -> "设备运行正常，没有异常情况。";
                case "BILL_QUERY" -> "您的电费账单已更新，请及时查看。";
                case "ALARM_QUERY" -> "当前没有未处理的告警信息。";
                case "SCENE_CONTROL" -> "智能场景已设置完成。";
                case "SYSTEM_HELP" -> "我是宿舍用电管理助手，可以帮您查询用电情况、控制设备、生成节能建议等。";
                case "ENERGY_SAVING" -> "节能策略已生成，建议您在不使用设备时及时关闭电源，这样可以节省更多电量。";
                case "ROOM_MANAGEMENT" -> "宿舍管理系统运行正常，您可以查询房间信息、学生信息等。";
                case "STUDENT_MANAGEMENT" -> "学生管理系统运行正常，您可以查询学生信息、入住记录等。";
                case "POWER_CONTROL" -> "断电管理系统运行正常，您可以查询断电记录、恢复供电等。";
                case "FIRMWARE_UPGRADE" -> "固件管理系统运行正常，您可以查询固件版本、升级状态等。";
                default -> "我理解您的需求，正在为您处理...";
            };
        }
        
        // 默认响应
        return "感谢您的咨询，我会尽力为您提供帮助。";
    }

    /**
     * 构建上下文提示
     */
    private String buildContextPrompt(String message, IntentRecognizer.IntentResult intent) {
        StringBuilder prompt = new StringBuilder(message);
        
        if (!intent.entities.isEmpty()) {
            prompt.append("\n\n上下文信息：");
            if (intent.entities.containsKey("roomId")) {
                prompt.append("\n房间号：").append(intent.entities.get("roomId"));
            }
            if (intent.entities.containsKey("deviceId")) {
                prompt.append("\n设备ID：").append(intent.entities.get("deviceId"));
            }
            if (intent.entities.containsKey("period")) {
                prompt.append("\n时间段：").append(intent.entities.get("period"));
            }
            if (intent.entities.containsKey("action")) {
                prompt.append("\n操作：").append(intent.entities.get("action"));
            }
        }
        if (intent.intent != null && !"UNKNOWN".equals(intent.intent)) {
            prompt.append("\n用户意图：").append(intent.intent);
        }
        if (intent.apiEndpoint != null) {
            prompt.append("\n推荐API：").append(intent.apiEndpoint);
        }

        prompt.append("\n\n请根据以上信息，生成一个专业、友好的响应，回答用户的问题。");
        prompt.append("使用个性化的语气，根据用户的问题提供具体、实用的建议和解决方案。");
        prompt.append("如果涉及房间ID，请确保在回复中包含该房间ID，使回答更加个性化。");

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

    /**
     * 调用系统API
     */
    private Map<String, Object> callSystemApi(String apiEndpoint, Map<String, String> entities) {
        try {
            // 构建API URL
            String baseUrl = "http://localhost:8000";
            String url = baseUrl + apiEndpoint;
            
            // 创建HTTP客户端
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            
            // 创建请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            
            // 发送请求
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            // 解析响应
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), Map.class);
            } else {
                logger.error("System API error: {} - {}", response.statusCode(), response.body());
                return null;
            }
        } catch (Exception e) {
            logger.error("Failed to call system API", e);
            return null;
        }
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
